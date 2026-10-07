package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.calling.CallActionReceiver
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CipherNotificationManager {

    private const val TAG = "CipherNotification"

    const val CHANNEL_MESSAGES = "cipherlink_messages_v1"
    const val CHANNEL_GROUP_MESSAGES = "cipherlink_groups_v1"
    const val CHANNEL_CALLS = "cipherlink_calls_v2"
    const val CHANNEL_MISSED_CALLS = "cipherlink_missed_calls_v1"
    const val CHANNEL_UPDATES = "cipherlink_updates_v1"
    const val CHANNEL_SECURITY = "cipherlink_security_v1"

    private const val BASE_MSG_NOTIFICATION_ID = 3000
    private const val BASE_GROUP_MSG_NOTIFICATION_ID = 3500
    private const val BASE_MISSED_CALL_NOTIFICATION_ID = 4000
    private const val BASE_UPDATE_NOTIFICATION_ID = 5000
    private const val BASE_SECURITY_NOTIFICATION_ID = 6000
    private const val NOTIFICATION_ID_CALL = 2001

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        createNotificationChannels(context)
        isInitialized = true
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Direct Messages Channel
        val msgChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            "Direct Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming private messages in CipheLink"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }

        // 2. Group Messages Channel
        val groupChannel = NotificationChannel(
            CHANNEL_GROUP_MESSAGES,
            "Group Chats",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for new messages in groups"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            setShowBadge(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }

        // 3. Incoming Calls Channel
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val callChannel = NotificationChannel(
            CHANNEL_CALLS,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority incoming voice and video call alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 800, 1000, 800)
            setSound(ringtoneUri, audioAttr)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        // 4. Missed Calls Channel
        val missedCallChannel = NotificationChannel(
            CHANNEL_MISSED_CALLS,
            "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for missed audio and video calls"
            enableVibration(true)
            setShowBadge(true)
        }

        // 5. Group & System Updates Channel
        val updateChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "App & Group Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Important announcements, group invitations, and updates"
            setShowBadge(false)
        }

        // 6. Security Channel
        val securityChannel = NotificationChannel(
            CHANNEL_SECURITY,
            "Security & Account",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical account, login, and security alerts"
            enableVibration(true)
            setShowBadge(true)
        }

        nm.createNotificationChannels(
            listOf(
                msgChannel,
                groupChannel,
                callChannel,
                missedCallChannel,
                updateChannel,
                securityChannel
            )
        )
        Log.i(TAG, "All notification channels registered successfully")
    }

    fun showMessageNotification(
        context: Context,
        conversationId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        isLockedChat: Boolean = false
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val displayText = if (isLockedChat) {
            "New encrypted message (Locked Chat)"
        } else {
            messageText
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "CHAT_DETAIL")
            putExtra("EXTRA_CONVERSATION_ID", conversationId)
            putExtra("EXTRA_SENDER_ID", senderId)
        }

        val requestCode = (conversationId.hashCode() and 0x7FFFFFFF)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = BASE_MSG_NOTIFICATION_ID + (conversationId.hashCode() % 1000).let { if (it < 0) -it else it }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isLockedChat) "CipheLink" else senderName)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(if (isLockedChat) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        nm.notify(notificationId, notification)
    }

    fun showGroupMessageNotification(
        context: Context,
        conversationId: String,
        groupTitle: String,
        senderName: String,
        messageText: String
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val displayText = "$senderName: $messageText"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "CHAT_DETAIL")
            putExtra("EXTRA_CONVERSATION_ID", conversationId)
        }

        val requestCode = (conversationId.hashCode() and 0x7FFFFFFF) + 200
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = BASE_GROUP_MSG_NOTIFICATION_ID + (conversationId.hashCode() % 1000).let { if (it < 0) -it else it }

        val notification = NotificationCompat.Builder(context, CHANNEL_GROUP_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(groupTitle)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(notificationId, notification)
    }

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        isVideo: Boolean
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("CALL_ACTION", "SHOW_INCOMING_CALL")
            putExtra("CALL_ID", callId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE_CALL
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ACCEPT_CALL
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            1003,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callTypeLabel = if (isVideo) "Video" else "Voice"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Incoming $callTypeLabel Call")
            .setContentText("$callerName is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_launcher_foreground, "Decline", declinePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Accept", acceptPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID_CALL, notification)
    }

    fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerId: String,
        callerName: String,
        isVideo: Boolean
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Tap to open calls tab
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "CALLS")
            putExtra("EXTRA_CALL_ID", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BASE_MISSED_CALL_NOTIFICATION_ID + (callId.hashCode() and 0x7FFFFFFF) % 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Call back immediately
        val callBackIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "START_CALL")
            putExtra("EXTRA_CONTACT_ID", callerId)
            putExtra("EXTRA_CALL_TYPE", if (isVideo) "VIDEO" else "AUDIO")
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            BASE_MISSED_CALL_NOTIFICATION_ID + (callId.hashCode() and 0x7FFFFFFF) % 1000 + 1,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callTypeStr = if (isVideo) "video" else "voice"
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Missed $callTypeStr call")
            .setContentText("You missed a $callTypeStr call from $callerName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Call Back", callBackPendingIntent)
            .build()

        val notificationId = BASE_MISSED_CALL_NOTIFICATION_ID + (callId.hashCode() and 0x7FFFFFFF) % 1000
        nm.notify(notificationId, notification)
    }

    fun showGroupInviteNotification(
        context: Context,
        groupId: String,
        groupTitle: String,
        inviterName: String
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "CHAT_DETAIL")
            putExtra("EXTRA_CONVERSATION_ID", groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BASE_UPDATE_NOTIFICATION_ID + (groupId.hashCode() and 0x7FFFFFFF) % 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = "$inviterName invited you to join \"$groupTitle\""
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Group Invitation")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = BASE_UPDATE_NOTIFICATION_ID + (groupId.hashCode() and 0x7FFFFFFF) % 1000
        nm.notify(notificationId, notification)
    }

    fun showSecurityAlertNotification(
        context: Context,
        title: String,
        message: String
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", "SETTINGS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BASE_SECURITY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(BASE_SECURITY_NOTIFICATION_ID, notification)
    }

    fun showAppUpdateNotification(
        context: Context,
        title: String,
        message: String,
        targetDestination: String = "DASHBOARD"
    ) {
        initialize(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NAVIGATE_DESTINATION", targetDestination)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            BASE_UPDATE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(BASE_UPDATE_NOTIFICATION_ID, notification)
    }

    fun clearNotificationsForConversation(context: Context, conversationId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notificationId = BASE_MSG_NOTIFICATION_ID + (conversationId.hashCode() % 1000).let { if (it < 0) -it else it }
        val groupNotificationId = BASE_GROUP_MSG_NOTIFICATION_ID + (conversationId.hashCode() % 1000).let { if (it < 0) -it else it }
        nm.cancel(notificationId)
        nm.cancel(groupNotificationId)
    }

    fun clearCallNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.cancel(NOTIFICATION_ID_CALL)
    }

    fun syncFcmTokenToFirestore(token: String, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        if (token.isBlank()) return

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId).set(
                    mapOf(
                        "fcmToken" to token,
                        "fcmUpdatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                Log.i(TAG, "Successfully synced FCM token to user document for $userId")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync FCM token to Firestore", e)
            }
        }
    }
}
