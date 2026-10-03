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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * Efficient, targeted Firestore queries using prefix bounds and index equality
     * instead of downloading the whole users collection.
     */
    suspend fun searchUsers(query: String): List<User> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val authUser = Firebase.auth.currentUser ?: return emptyList()
        val currentUid = authUser.uid
        val results = mutableMapOf<String, User>()

        try {
            val cleanHandle = q.removePrefix("@").lowercase()

            // 1. Search by username prefix
            val byUsername = db.collection("users")
                .orderBy("username")
                .startAt(cleanHandle)
                .endAt(cleanHandle + "\uf8ff")
                .limit(15)
                .get()
                .await()

            byUsername.documents.forEach { doc ->
                val fu = doc.toObject(FirestoreUser::class.java)
                if (fu != null && fu.userId.isNotBlank() && fu.userId != currentUid) {
                    results[fu.userId] = mapFirestoreUserToModel(fu)
                }
            }

            // 2. Search by displayName prefix if needed
            if (results.size < 10) {
                val byName = db.collection("users")
                    .orderBy("displayName")
                    .startAt(q)
                    .endAt(q + "\uf8ff")
                    .limit(15)
                    .get()
                    .await()

                byName.documents.forEach { doc ->
                    val fu = doc.toObject(FirestoreUser::class.java)
                    if (fu != null && fu.userId.isNotBlank() && fu.userId != currentUid) {
                        results[fu.userId] = mapFirestoreUserToModel(fu)
                    }
                }
            }

            // 3. Search by exact phone number
            if (q.any { it.isDigit() }) {
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

    fun addContact(name: String, phoneOrHandle: String) {
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "C" }
        val handleClean = phoneOrHandle.removePrefix("@").replace(" ", "_")
        val newUserId = "usr_${System.currentTimeMillis() % 100000}"

        val newUser = User(
            id = newUserId,
            name = name,
            handle = handleClean,
            phone = if (phoneOrHandle.startsWith("+") || phoneOrHandle.any { it.isDigit() }) phoneOrHandle else "+1 (555) 000-0000",
            avatarInitials = initials,
            avatarColorHex = 0xFF00F0FF,
            statusMessage = "Available",
            isOnline = true,
            lastSeenText = "Online"
        )

        _contacts.value = _contacts.value + newUser

        val authUser = Firebase.auth.currentUser ?: return
        scope.launch {
            try {
                val data = hashMapOf(
                    "userId" to newUser.id,
                    "displayName" to newUser.name,
                    "username" to newUser.handle,
                    "profilePhoto" to newUser.avatarInitials,
                    "phoneNumber" to newUser.phone,
                    "onlineStatus" to "online",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastSeen" to FieldValue.serverTimestamp()
                )
                db.collection("users")
                    .document(authUser.uid)
                    .collection("contacts")
                    .document(newUser.id)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("UserRepository", "Failed to cache contact remotely: ${e.message}")
            }
        }
    }

    private fun mapFirestoreUserToModel(fu: FirestoreUser): User {
        val isOnline = fu.onlineStatus == "online"
        val lastSeenStr = formatLastSeen(fu.lastSeen, isOnline)

        return User(
            id = fu.userId,
            name = fu.displayName.ifEmpty { "User ${fu.userId.take(4)}" },
            handle = fu.username.ifEmpty { "user_${fu.userId.take(4)}" },
            phone = fu.phoneNumber,
            email = fu.email,
            avatarInitials = fu.profilePhoto.ifEmpty { fu.displayName.take(2).uppercase().ifEmpty { "US" } },
            avatarColorHex = if (fu.userId.hashCode() % 2 == 0) 0xFF00F0FF else 0xFF00E699,
            statusMessage = if (isOnline) "Available" else "Offline",
            isOnline = isOnline,
            lastSeenText = lastSeenStr
        )
    }

    private fun formatLastSeen(timestamp: Timestamp?, isOnline: Boolean): String {
        if (isOnline) return "Online"
        if (timestamp == null) return "Active recently"
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
