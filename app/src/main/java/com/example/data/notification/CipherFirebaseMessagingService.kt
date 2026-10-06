package com.example.data.notification

import android.util.Log
import com.example.data.CipherAppContainer
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CipherFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CipherFCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Registration Token generated: $token")
        CipherNotificationManager.syncFcmTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "message"
            when (type) {
                "message", "chat" -> {
                    val conversationId = data["conversationId"] ?: ""
                    val senderId = data["senderId"] ?: ""
                    val senderName = data["senderName"] ?: "Contact"
                    val messageText = data["messageText"] ?: data["content"] ?: "New message"

                    val isLocked = try {
                        CipherAppContainer.chatSecurityManager.isChatLocked(conversationId)
                    } catch (e: Exception) {
                        false
                    }

                    CipherNotificationManager.showMessageNotification(
                        context = applicationContext,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        messageText = messageText,
                        isLockedChat = isLocked
                    )
                }
                "missed_call" -> {
                    val callId = data["callId"] ?: ""
                    val callerId = data["callerId"] ?: ""
                    val callerName = data["callerName"] ?: "Contact"
                    val isVideo = data["callType"]?.equals("VIDEO", ignoreCase = true) == true

                    CipherNotificationManager.showMissedCallNotification(
                        context = applicationContext,
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        isVideo = isVideo
                    )
                }
                "update", "announcement" -> {
                    val title = data["title"] ?: "CipheLink Update"
                    val body = data["body"] ?: data["message"] ?: "Important app announcement"
                    CipherNotificationManager.showAppUpdateNotification(
                        context = applicationContext,
                        title = title,
                        message = body
                    )
                }
            }
        } else {
            remoteMessage.notification?.let { notif ->
                CipherNotificationManager.showAppUpdateNotification(
                    context = applicationContext,
                    title = notif.title ?: "CipheLink",
                    message = notif.body ?: "New notification"
                )
            }
        }
    }
}
