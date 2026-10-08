package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.FirestoreUser
import com.example.model.User
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }
    private val db = FirebaseFirestore.getInstance(databaseId)

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    private var usersListener: ListenerRegistration? = null

    init {
        scope.launch {
            authRepository.currentUser.collect { currentUser ->
                val authUser = Firebase.auth.currentUser
                if (currentUser != null && authUser != null) {
                    attachUsersListener(currentUser.id)
                } else {
                    usersListener?.remove()
                    usersListener = null
                    _contacts.value = emptyList()
                }
            }
        }
    }

    private fun attachUsersListener(currentUserId: String?) {
        usersListener?.remove()
        val authUser = Firebase.auth.currentUser
        if (authUser == null) {
            // Zero-trust gate: do not query users directory if unauthenticated
            return
        }
        val path = "users"
        usersListener = db.collection(path)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val fu = doc.toObject(FirestoreUser::class.java)
                            fu?.let { mapFirestoreUserToModel(it) }
                        } catch (e: Exception) {
                            Log.e("UserRepository", "Error deserializing user doc", e)
                            null
                        }
                    }.filter { it.id != currentUserId } // Hide self from peer directory

                    _contacts.value = list
                }
            }
    }

    fun getContactById(id: String): User? = _contacts.value.find { it.id == id }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val fu = snapshot.toObject(FirestoreUser::class.java)
                    val user = fu?.let { mapFirestoreUserToModel(it) }
                    trySend(user)
                } catch (e: Exception) {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Searches real registered users in Firebase by display name, username, or phone number.
     * Returns real profile information and avoids fake/local data.
     */
    suspend fun searchUsers(query: String): List<User> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val authUser = Firebase.auth.currentUser ?: return emptyList()
        val currentUid = authUser.uid
        val results = mutableMapOf<String, User>()

        val cleanDigits = q.filter { it.isDigit() }
        val cleanQuery = q.lowercase()

        try {
            // 1. Fetch registered users from Firestore
            val snapshot = db.collection("users")
                .limit(50)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val fu = doc.toObject(FirestoreUser::class.java)
                if (fu != null && fu.userId.isNotBlank() && fu.userId != currentUid) {
                    val nameMatch = fu.displayName.lowercase().contains(cleanQuery)
                    val usernameMatch = fu.username.lowercase().contains(cleanQuery.removePrefix("@"))
                    val phoneDigits = fu.phoneNumber.filter { it.isDigit() }
                    val phoneMatch = cleanDigits.length >= 3 && phoneDigits.contains(cleanDigits)

                    if (nameMatch || usernameMatch || phoneMatch) {
                        results[fu.userId] = mapFirestoreUserToModel(fu)
                    }
                }
            }

            // 2. Direct lookup by exact or prefix phone number if searching with digits
            if (cleanDigits.length >= 4 && results.size < 5) {
                val byPhone = db.collection("users")
                    .whereEqualTo("phoneNumber", q)
                    .limit(5)
                    .get()
                    .await()
                byPhone.documents.forEach { doc ->
                    val fu = doc.toObject(FirestoreUser::class.java)
                    if (fu != null && fu.userId.isNotBlank() && fu.userId != currentUid) {
                        results[fu.userId] = mapFirestoreUserToModel(fu)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "searchUsers error: ${e.message}", e)
        }

        return results.values.toList()
    }

    fun addContact(user: User) {
        if (_contacts.value.none { it.id == user.id }) {
            _contacts.value = _contacts.value + user
        }

        val authUser = Firebase.auth.currentUser ?: return
        scope.launch {
            try {
                val data = hashMapOf(
                    "userId" to user.id,
                    "displayName" to user.name,
                    "username" to user.handle,
                    "profilePhoto" to user.photoUrl.ifEmpty { user.avatarInitials },
                    "phoneNumber" to user.phone,
                    "onlineStatus" to if (user.isOnline) "online" else "offline",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastSeen" to FieldValue.serverTimestamp()
                )
                db.collection("users")
                    .document(authUser.uid)
                    .collection("contacts")
                    .document(user.id)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to cache contact remotely: ${e.message}")
            }
        }
    }

    private fun mapFirestoreUserToModel(fu: FirestoreUser): User {
        val now = System.currentTimeMillis()
        val lastSeenMs = fu.lastSeen?.toDate()?.time ?: 0L
        val diffMs = now - lastSeenMs
        // Accurate presence: genuine online only if status is online AND heartbeat within 120 seconds
        val isOnline = fu.onlineStatus == "online" && (lastSeenMs <= 0 || diffMs < 120_000L)
        val lastSeenStr = formatLastSeen(fu.lastSeen, isOnline)

        val isPhotoUrl = fu.profilePhoto.startsWith("http://") ||
                fu.profilePhoto.startsWith("https://") ||
                fu.profilePhoto.startsWith("content://")
        val photoUrl = if (isPhotoUrl) fu.profilePhoto else ""
        val initials = if (!isPhotoUrl && fu.profilePhoto.isNotBlank()) {
            fu.profilePhoto.take(2).uppercase()
        } else {
            fu.displayName.take(2).uppercase().ifEmpty { "U" }
        }

        return User(
            id = fu.userId,
            name = fu.displayName.ifEmpty { "User ${fu.userId.take(4)}" },
            handle = fu.username.ifEmpty { "user_${fu.userId.take(4)}" },
            phone = fu.phoneNumber,
            email = fu.email,
            photoUrl = photoUrl,
            avatarInitials = initials,
            avatarColorHex = if (fu.userId.hashCode() % 2 == 0) 0xFF00F0FF else 0xFF00E699,
            statusMessage = if (isOnline) "Available" else "Offline",
            isOnline = isOnline,
            lastSeenText = lastSeenStr
        )
    }

    private fun formatLastSeen(timestamp: Timestamp?, isOnline: Boolean): String {
        if (isOnline) return "Online"
        if (timestamp == null) return "Offline"
        val diffMs = System.currentTimeMillis() - timestamp.toDate().time
        val minutes = diffMs / (1000 * 60)
        return when {
            minutes < 1 -> "Last seen just now"
            minutes < 60 -> "Last seen ${minutes}m ago"
            minutes < 1440 -> "Last seen ${minutes / 60}h ago"
            else -> "Last seen recently"
        }
    }
}
