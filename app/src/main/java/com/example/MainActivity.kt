package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.CipherAppContainer
import com.example.data.notification.CipherNotificationManager
import com.example.ui.navigation.CipherNavHost
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_NOTIFICATIONS = 1010
    }

    private var pendingNotificationIntent by androidx.compose.runtime.mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationIntent = intent
        CipherAppContainer.initialize(this)
        CipherNotificationManager.initialize(this)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleCallIntent(intent)
        syncFcmToken()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBgDark
                ) {
                    CipherNavHost(
                        intent = pendingNotificationIntent,
                        onIntentHandled = { pendingNotificationIntent = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationIntent = intent
        handleCallIntent(intent)
    }

    private fun handleCallIntent(intent: Intent?) {
        val action = intent?.getStringExtra("CALL_ACTION") ?: return
        Log.i(TAG, "Handling call intent action: $action")
        when (action) {
            "ACCEPT_CALL" -> {
                try {
                    CipherAppContainer.callingService.answerIncomingCall()
                } catch (e: Exception) {
                    Log.w(TAG, "Error answering call from intent", e)
                }
            }
            "DECLINE_CALL" -> {
                try {
                    CipherAppContainer.callingService.declineIncomingCall()
                } catch (e: Exception) {
                    Log.w(TAG, "Error declining call from intent", e)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }
    }

    private fun syncFcmToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        CipherNotificationManager.syncFcmTokenToFirestore(token)
                    }
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize FCM token fetch", e)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            CipherAppContainer.authRepository.updatePresence(true)
        } catch (e: Exception) {
            // Ignore if container not yet ready
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            CipherAppContainer.authRepository.updatePresence(false)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            CipherAppContainer.authRepository.updatePresence(false)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
