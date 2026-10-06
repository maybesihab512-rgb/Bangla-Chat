package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatSecurityManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ChatSecurityManager"
        private const val PREFS_NAME = "cipherlink_chat_locks_v1"
    }

    private val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }
    private val db = FirebaseFirestore.getInstance(databaseId)

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lockedConversations = MutableStateFlow<Set<String>>(emptySet())
    val lockedConversations: StateFlow<Set<String>> = _lockedConversations.asStateFlow()

    // Transient in-memory unlocked conversations for the current app session
    private val unlockedInSession = mutableSetOf<String>()

    init {
        loadLocalLockedChats()
    }

    private val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    private fun lockKey(userId: String): String = "locked_convs_$userId"

    fun loadLocalLockedChats() {
        val uid = currentUserId
        if (uid.isBlank()) return
        val saved = prefs.getStringSet(lockKey(uid), emptySet()) ?: emptySet()
        _lockedConversations.value = saved
    }

    fun syncFromFirestore(userId: String) {
        if (userId.isBlank()) return
        scope.launch {
            try {
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("chat_locks")
                    .get()
                    .await()

                val remoteIds = snapshot.documents.map { it.id }.toSet()
                val merged = _lockedConversations.value + remoteIds
                _lockedConversations.value = merged
                prefs.edit().putStringSet(lockKey(userId), merged).apply()
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing chat locks from Firestore: ${e.message}")
            }
        }
    }

    fun isChatLocked(conversationId: String): Boolean {
        return _lockedConversations.value.contains(conversationId)
    }

    fun isChatUnlockedInSession(conversationId: String): Boolean {
        if (!isChatLocked(conversationId)) return true
        return unlockedInSession.contains(conversationId)
    }

    fun markUnlockedForSession(conversationId: String) {
        unlockedInSession.add(conversationId)
    }

    fun lockConversation(conversationId: String) {
        val uid = currentUserId
        val updated = _lockedConversations.value + conversationId
        _lockedConversations.value = updated
        unlockedInSession.remove(conversationId)

        if (uid.isNotBlank()) {
            prefs.edit().putStringSet(lockKey(uid), updated).apply()
            scope.launch {
                try {
                    db.collection("users")
                        .document(uid)
                        .collection("chat_locks")
                        .document(conversationId)
                        .set(mapOf("lockedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist chat lock to Firestore: ${e.message}")
                }
            }
        }
    }

    fun unlockConversation(conversationId: String) {
        val uid = currentUserId
        val updated = _lockedConversations.value - conversationId
        _lockedConversations.value = updated
        unlockedInSession.remove(conversationId)

        if (uid.isNotBlank()) {
            prefs.edit().putStringSet(lockKey(uid), updated).apply()
            scope.launch {
                try {
                    db.collection("users")
                        .document(uid)
                        .collection("chat_locks")
                        .document(conversationId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove chat lock from Firestore: ${e.message}")
                }
            }
        }
    }

    fun authenticateToOpenChat(
        activity: FragmentActivity,
        conversationTitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS && canAuth != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            // Hardware doesn't support or error -> Fallback allow
            Log.i(TAG, "Biometric unavailable ($canAuth), granting session unlock fallback")
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Chat")
            .setSubtitle("Confirm identity to open \"$conversationTitle\"")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            // Below API 30, DEVICE_CREDENTIAL cannot be combined with negative button
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        val promptInfo = promptInfoBuilder.build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Please try again.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking BiometricPrompt", e)
            onError(e.localizedMessage ?: "Biometric error")
        }
    }
}
