package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.FirestoreUser
import com.example.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthStatus {
    object Idle : AuthStatus()
    object Loading : AuthStatus()
    data class OtpSent(val phoneNumber: String, val confirmationCode: String = "482910") : AuthStatus()
    data class Success(val user: User) : AuthStatus()
    data class NeedsProfileSetup(val phone: String = "", val email: String = "") : AuthStatus()
    data class Error(val message: String) : AuthStatus()
}

class AuthRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }
    private val db = FirebaseFirestore.getInstance(databaseId)
    private val auth: FirebaseAuth = Firebase.auth

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    init {
        val initialUser = auth.currentUser
        if (initialUser != null) {
            val user = mapFirebaseUser(initialUser)
            _currentUser.value = user
            _authStatus.value = AuthStatus.Success(user)
            syncUserProfileToFirestore(user)
            fetchRemoteProfileAndMerge(user.id)
            updatePresence(true)
        } else {
            _currentUser.value = null
            _authStatus.value = AuthStatus.Idle
        }

        auth.addAuthStateListener { firebaseAuth ->
            val fUser = firebaseAuth.currentUser
            if (fUser != null) {
                val user = mapFirebaseUser(fUser)
                _currentUser.value = user
                _authStatus.value = AuthStatus.Success(user)
                syncUserProfileToFirestore(user)
                fetchRemoteProfileAndMerge(user.id)
                updatePresence(true)
            } else {
                updatePresence(false)
                _currentUser.value = null
                _authStatus.value = AuthStatus.Idle
            }
        }
    }

    private fun fetchRemoteProfileAndMerge(uid: String) {
        scope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val fu = doc.toObject(FirestoreUser::class.java)
                    if (fu != null) {
                        val current = _currentUser.value ?: return@launch
                        val savedPhoto = fu.profilePhoto
                        val isPhotoUrl = savedPhoto.startsWith("http://") || savedPhoto.startsWith("https://") || savedPhoto.startsWith("content://")
                        val merged = current.copy(
                            name = fu.displayName.ifBlank { current.name },
                            handle = fu.username.ifBlank { current.handle },
                            phone = fu.phoneNumber.ifBlank { current.phone },
                            photoUrl = if (isPhotoUrl) savedPhoto else current.photoUrl,
                            avatarInitials = if (!isPhotoUrl && savedPhoto.isNotBlank()) savedPhoto else current.avatarInitials
                        )
                        _currentUser.value = merged
                    }
                }
            } catch (e: Exception) {
                Log.w("Auth", "Could not fetch existing remote profile", e)
            }
        }
    }

    fun mapFirebaseUser(firebaseUser: FirebaseUser): User {
        val email = firebaseUser.email ?: ""
        val emailPrefix = if (email.isNotEmpty()) email.substringBefore("@") else "User"
        val name = firebaseUser.displayName?.ifBlank { emailPrefix } ?: emailPrefix
        val handle = if (email.isNotEmpty()) emailPrefix.replace(".", "_") else "user_${firebaseUser.uid.take(4)}"
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "U" }
        val photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        return User(
            id = firebaseUser.uid,
            name = name,
            handle = handle,
            email = email,
            phone = firebaseUser.phoneNumber ?: "",
            photoUrl = photoUrl,
            avatarInitials = initials.uppercase(),
            avatarColorHex = if (firebaseUser.uid.hashCode() % 2 == 0) 0xFF00F0FF else 0xFF00E699,
            statusMessage = "Available",
            isOnline = true,
            lastSeenText = "Online",
            e2eeFingerprint = "7F4A:9C2E:11D8:6E90:4B21:AA33"
        )
    }

    fun syncUserProfileToFirestore(user: User) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid != user.id) return // Zero-trust check: must be owner of own profile
        scope.launch {
            try {
                val userDoc = db.collection("users").document(user.id)
                val data = hashMapOf(
                    "userId" to user.id,
                    "displayName" to user.name,
                    "username" to user.handle,
                    "profilePhoto" to user.photoUrl.ifEmpty { user.avatarInitials },
                    "phoneNumber" to user.phone,
                    "email" to user.email,
                    "onlineStatus" to if (user.isOnline) "online" else "offline",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastSeen" to FieldValue.serverTimestamp()
                )
                userDoc.set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/${user.id}")
            }
        }
    }

    fun attemptAutoSignIn(activity: Activity, onComplete: () -> Unit = {}) {
        if (auth.currentUser != null) {
            onComplete()
            return
        }
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onComplete()
                }
            } catch (e: Exception) {
                // Silent auto sign-in ignored if no saved accounts
            }
        }
    }

    fun continueWithGoogle(activity: Activity, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        _authStatus.value = AuthStatus.Loading
        val credentialManager = CredentialManager.create(activity)
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            null
        }

        if (clientId != null && clientId.isNotBlank()) {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(activity, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        val firebaseUser = authResult.user

                        if (firebaseUser != null) {
                            val user = mapFirebaseUser(firebaseUser)
                            _currentUser.value = user
                            _authStatus.value = AuthStatus.Success(user)
                            syncUserProfileToFirestore(user)
                            onComplete()
                            return@launch
                        }
                    }
                    _authStatus.value = AuthStatus.Idle
                } catch (e: GetCredentialCancellationException) {
                    Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
                    _authStatus.value = AuthStatus.Idle
                } catch (e: Exception) {
                    Log.e("Auth", "Google Sign-In failed", e)
                    val msg = "Sign-in failed. Please try again."
                    _authStatus.value = AuthStatus.Error(msg)
                    onError(msg)
                }
            }
        } else {
            val msg = "Google Sign-In is currently unavailable. Please try again."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
        }
    }

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun requestPhoneOtp(
        activity: Activity? = null,
        phoneNumber: String,
        onCodeSent: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.length < 6) {
            val msg = "Please enter a valid phone number with country code."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
            return
        }

        _authStatus.value = AuthStatus.Loading
        if (activity == null) {
            val msg = "Unable to start verification. Please try again."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
            return
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    scope.launch {
                        try {
                            val authResult = auth.signInWithCredential(credential).await()
                            val fUser = authResult.user
                            if (fUser != null) {
                                val user = mapFirebaseUser(fUser).copy(phone = cleanPhone)
                                _currentUser.value = user
                                _authStatus.value = AuthStatus.Success(user)
                                syncUserProfileToFirestore(user)
                            }
                        } catch (e: Exception) {
                            Log.e("Auth", "Auto phone verification sign-in failed", e)
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("Auth", "Phone verification failed", e)
                    val rawMsg = e.localizedMessage ?: ""
                    val msg = if (rawMsg.contains("operation is not allowed", ignoreCase = true) || rawMsg.contains("disabled", ignoreCase = true)) {
                        "Phone sign-in is currently unavailable. Please continue with Google."
                    } else if (rawMsg.contains("reCAPTCHA", ignoreCase = true) || rawMsg.contains("Integrity", ignoreCase = true) || rawMsg.contains("17006", ignoreCase = true)) {
                        "Verification could not be completed on this device. Please continue with Google."
                    } else {
                        "Phone verification failed. Please try again or continue with Google."
                    }
                    _authStatus.value = AuthStatus.Error(msg)
                    onError(msg)
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    resendToken = token
                    _authStatus.value = AuthStatus.OtpSent(cleanPhone)
                    onCodeSent()
                }
            })
            .build()

        try {
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.e("Auth", "Error launching verifyPhoneNumber", e)
            val msg = "Failed to send verification code. Please try again."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
        }
    }

    fun verifyOtp(
        code: String,
        phoneNumber: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        if (code.length != 6) {
            val msg = "Please enter the complete 6-digit verification code."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
            return false
        }

        val vId = verificationId
        if (vId == null) {
            val msg = "Verification code expired. Please request a new code."
            _authStatus.value = AuthStatus.Error(msg)
            onError(msg)
            return false
        }

        _authStatus.value = AuthStatus.Loading
        scope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(vId, code)
                val authResult = auth.signInWithCredential(credential).await()
                val fUser = authResult.user
                if (fUser != null) {
                    val user = mapFirebaseUser(fUser).copy(phone = phoneNumber)
                    _currentUser.value = user
                    _authStatus.value = AuthStatus.Success(user)
                    syncUserProfileToFirestore(user)
                    onSuccess()
                } else {
                    val msg = "Something went wrong. Please try again."
                    _authStatus.value = AuthStatus.Error(msg)
                    onError(msg)
                }
            } catch (e: Exception) {
                Log.e("Auth", "OTP verification failed", e)
                val msg = "Invalid verification code. Please check and try again."
                _authStatus.value = AuthStatus.Error(msg)
                onError(msg)
            }
        }
        return true
    }

    fun completeProfile(name: String, handle: String, status: String, phone: String = "", photoUrl: String = "") {
        val current = _currentUser.value ?: return
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "U" }
        val updated = current.copy(
            name = name,
            handle = handle.removePrefix("@"),
            statusMessage = status,
            phone = phone.ifEmpty { current.phone },
            photoUrl = photoUrl.ifEmpty { current.photoUrl },
            avatarInitials = initials.uppercase()
        )
        _currentUser.value = updated
        _authStatus.value = AuthStatus.Success(updated)
        syncUserProfileToFirestore(updated)
    }

    suspend fun updateProfile(
        displayName: String,
        phoneNumber: String,
        photoUri: Uri?,
        statusMessage: String
    ): Result<User> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("User not logged in"))
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("No current user"))

        return try {
            var uploadedPhotoUrl = current.photoUrl
            if (photoUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profile_photos")
                    .child(currentUid)
                    .child("${System.currentTimeMillis()}.jpg")
                val uploadTask = storageRef.putFile(photoUri).await()
                uploadedPhotoUrl = uploadTask.storage.downloadUrl.await().toString()
            }

            val cleanName = displayName.trim().ifEmpty { current.name }
            val cleanPhone = phoneNumber.trim()
            val initials = cleanName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "U" }

            val updatedUser = current.copy(
                name = cleanName,
                phone = cleanPhone,
                photoUrl = uploadedPhotoUrl,
                avatarInitials = initials.uppercase(),
                statusMessage = statusMessage.trim().ifEmpty { "Available" }
            )

            // Update Firebase Auth profile
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .apply {
                        if (uploadedPhotoUrl.isNotBlank()) {
                            setPhotoUri(Uri.parse(uploadedPhotoUrl))
                        }
                    }
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
            } catch (e: Exception) {
                Log.w("Auth", "Failed updating firebase auth profile", e)
            }

            // Update Firestore user doc
            val data = hashMapOf(
                "userId" to currentUid,
                "displayName" to cleanName,
                "username" to updatedUser.handle,
                "profilePhoto" to uploadedPhotoUrl.ifEmpty { initials.uppercase() },
                "phoneNumber" to cleanPhone,
                "email" to updatedUser.email,
                "onlineStatus" to if (updatedUser.isOnline) "online" else "offline",
                "statusMessage" to updatedUser.statusMessage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(currentUid).set(data, SetOptions.merge()).await()

            _currentUser.value = updatedUser
            _authStatus.value = AuthStatus.Success(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Log.e("Auth", "Error updating profile", e)
            Result.failure(e)
        }
    }

    private var presenceHeartbeatJob: Job? = null

    fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        presenceHeartbeatJob?.cancel()

        if (isOnline) {
            // Write online immediately with server timestamp
            scope.launch {
                try {
                    db.collection("users").document(uid).update(
                        "onlineStatus", "online",
                        "lastSeen", FieldValue.serverTimestamp()
                    ).await()
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }

            // Start heartbeat job updating lastSeen every 30 seconds
            presenceHeartbeatJob = scope.launch {
                while (isActive) {
                    delay(30_000)
                    try {
                        db.collection("users").document(uid).update(
                            "onlineStatus", "online",
                            "lastSeen", FieldValue.serverTimestamp()
                        ).await()
                    } catch (e: Exception) {
                        // Ignore transient network errors
                    }
                }
            }
        } else {
            // Write offline immediately
            scope.launch {
                try {
                    db.collection("users").document(uid).update(
                        "onlineStatus", "offline",
                        "lastSeen", FieldValue.serverTimestamp()
                    ).await()
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }
        }
    }

    fun logOut(activity: Activity? = null) {
        scope.launch {
            try {
                auth.signOut()
                if (activity != null) {
                    val credentialManager = CredentialManager.create(activity)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
            } catch (e: Exception) {
                Log.e("Auth", "Error signing out", e)
            }
        }
        _currentUser.value = null
        _authStatus.value = AuthStatus.Idle
    }
}
