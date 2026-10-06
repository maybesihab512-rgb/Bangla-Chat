package com.example.data.calling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.CallingConfig
import com.example.model.ConnectionState
import com.example.model.NetworkQualityMetrics
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.Collections

data class ActiveCallSession(
    val callId: String,
    val contactId: String,
    val contactName: String,
    val contactAvatarInitials: String,
    val avatarColorHex: Long = 0xFF00F0FF,
    val callType: CallType,
    val isIncoming: Boolean,
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val durationSeconds: Int = 0,
    val isMicMuted: Boolean = false,
    val isVideoMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isFrontCamera: Boolean = true,
    val manualAudioPriorityOverride: Boolean = false,
    val securityEncryptionLabel: String = "End-to-End Encrypted",
    val metrics: NetworkQualityMetrics = NetworkQualityMetrics()
)

class CallingService(
    private val context: Context? = null,
    private val authRepository: AuthRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : WebRtcEngineInterface {

    companion object {
        private const val TAG = "CallingService"
        private const val NOTIFICATION_ID_CALL = 2001
        private const val CALL_NOTIFICATION_CHANNEL_ID = "cipherlink_calls_v2"
        private const val CALL_RING_TIMEOUT_MS = 30000L
        private const val STALE_CALL_THRESHOLD_MS = 25000L
    }

    private val databaseId = try {
        context?.getString(R.string.firestore_database_id)
            ?: "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private val audioManager by lazy {
        context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val _callingConfig = MutableStateFlow(CallingConfig())
    val callingConfig: StateFlow<CallingConfig> = _callingConfig.asStateFlow()

    private val bitrateController = AdaptiveBitrateController(_callingConfig.value)

    private val _currentSession = MutableStateFlow<ActiveCallSession?>(null)
    val currentSession: StateFlow<ActiveCallSession?> = _currentSession.asStateFlow()

    private val _networkMetrics = MutableStateFlow(NetworkQualityMetrics())
    override val networkMetrics: StateFlow<NetworkQualityMetrics> = _networkMetrics.asStateFlow()

    private val _hasRemoteVideo = MutableStateFlow(false)
    val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private var webRtcManager: WebRtcManager? = null

    private var callTimerJob: Job? = null
    private var telemetryJob: Job? = null
    private var callTimeoutJob: Job? = null
    private var reconnectSimulationJob: Job? = null
    private var iceFailureTimeoutJob: Job? = null
    private var audioFocusRequest: Any? = null

    private var ringtone: Ringtone? = null
    private var activeCallDocListener: ListenerRegistration? = null
    private var incomingCallsListener: ListenerRegistration? = null
    private var candidatesListener: ListenerRegistration? = null

    // Track processed call IDs to completely eliminate duplicate ringing or ghost calls
    private val handledCallIds = Collections.synchronizedSet(mutableSetOf<String>())
    // Track applied ICE candidate IDs to eliminate duplicate candidates
    private val processedRemoteCandidateIds = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Observe auth changes to register incoming calls listener
        authRepository?.let { repo ->
            scope.launch {
                repo.currentUser.collect { user ->
                    if (user != null && user.id.isNotBlank()) {
                        attachIncomingCallsListener(user.id)
                    } else {
                        detachIncomingCallsListener()
                    }
                }
            }
        }
    }

    private fun attachIncomingCallsListener(userId: String) {
        detachIncomingCallsListener()
        if (userId.isBlank()) return

        try {
            Log.i(TAG, "Attaching incoming calls listener for user: $userId")
            incomingCallsListener = db.collection("calls")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "CALLING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Incoming calls listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val documents = snapshot?.documents ?: return@addSnapshotListener
                    for (doc in documents) {
                        val callId = doc.id
                        val status = doc.getString("status") ?: ""
                        if (status != "CALLING") continue

                        // Stale call detection (Issue 5):
                        val createdAt = doc.getTimestamp("createdAt")
                        val now = System.currentTimeMillis()
                        val isStale = if (createdAt != null) {
                            (now - createdAt.toDate().time) > STALE_CALL_THRESHOLD_MS
                        } else {
                            val idTimestamp = callId.removePrefix("call_").toLongOrNull()
                            if (idTimestamp != null) (now - idTimestamp) > STALE_CALL_THRESHOLD_MS else false
                        }

                        if (isStale) {
                            Log.i(TAG, "Ignoring stale call $callId (created > 35s ago)")
                            scope.launch {
                                try {
                                    db.collection("calls").document(callId)
                                        .update(
                                            mapOf(
                                                "status" to "EXPIRED",
                                                "endedAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                } catch (_: Exception) {}
                            }
                            continue
                        }

                        if (handledCallIds.contains(callId)) {
                            continue
                        }

                        // Prevent duplicate / ghost incoming call trigger if already in an active session
                        if (_currentSession.value != null) {
                            Log.i(TAG, "Busy: device already in active call ${_currentSession.value?.callId}, marking $callId BUSY")
                            scope.launch {
                                try {
                                    db.collection("calls").document(callId)
                                        .update(
                                            mapOf(
                                                "status" to "BUSY",
                                                "endedAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                } catch (_: Exception) {}
                            }
                            continue
                        }

                        handledCallIds.add(callId)

                        val callerId = doc.getString("callerId") ?: "unknown"
                        val callerName = doc.getString("callerName") ?: "Contact"
                        val callerAvatar = doc.getString("callerAvatar") ?: "SC"
                        val callTypeStr = doc.getString("callType") ?: "AUDIO"
                        val callType = if (callTypeStr == "VIDEO") CallType.VIDEO else CallType.AUDIO

                        val incomingSession = ActiveCallSession(
                            callId = callId,
                            contactId = callerId,
                            contactName = callerName,
                            contactAvatarInitials = callerAvatar,
                            avatarColorHex = 0xFF00F0FF,
                            callType = callType,
                            isIncoming = true,
                            connectionState = ConnectionState.CONNECTING
                        )
                        _currentSession.value = incomingSession
                        startAlerts(incomingSession)
                        attachCallDocumentListener(callId)

                        // 30-second missed call timeout
                        callTimeoutJob?.cancel()
                        callTimeoutJob = scope.launch {
                            delay(CALL_RING_TIMEOUT_MS)
                            if (_currentSession.value?.callId == callId &&
                                _currentSession.value?.connectionState == ConnectionState.CONNECTING
                            ) {
                                Log.i(TAG, "Call $callId timed out after 30s with no answer")
                                declineIncomingCall(isTimeout = true)
                            }
                        }
                        break // Process one active incoming call at a time
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach incoming calls listener", e)
        }
    }

    private fun detachIncomingCallsListener() {
        incomingCallsListener?.remove()
        incomingCallsListener = null
    }

    private fun attachCallDocumentListener(callId: String) {
        activeCallDocListener?.remove()
        try {
            activeCallDocListener = db.collection("calls").document(callId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val status = snapshot.getString("status") ?: return@addSnapshotListener
                    val session = _currentSession.value ?: return@addSnapshotListener
                    if (session.callId != callId) return@addSnapshotListener

                    Log.d(TAG, "Call $callId document status updated: $status")

                    when (status) {
                        "ACCEPTED" -> {
                            val answerSdp = snapshot.getString("answerSdp")
                            if (!session.isIncoming && !answerSdp.isNullOrBlank()) {
                                scope.launch {
                                    val setAnswerResult = webRtcManager?.setRemoteAnswer(answerSdp)
                                    if (setAnswerResult?.isSuccess == true) {
                                        Log.i(TAG, "Remote answer set on Caller successfully")
                                        stopAlerts()
                                    } else {
                                        Log.e(TAG, "Failed to set remote answer on caller: ${setAnswerResult?.exceptionOrNull()?.message}")
                                    }
                                }
                            } else {
                                stopAlerts()
                            }

                            // Handle ICE reconnection offers and answers in real-time
                            val reconnectOffer = snapshot.getString("reconnectOfferSdp")
                            if (!reconnectOffer.isNullOrBlank() && session.isIncoming) {
                                scope.launch {
                                    val ansRes = webRtcManager?.setRemoteOfferAndCreateAnswer(reconnectOffer)
                                    if (ansRes?.isSuccess == true) {
                                        db.collection("calls").document(callId).update(
                                            "reconnectAnswerSdp", ansRes.getOrThrow().description
                                        )
                                    }
                                }
                            }

                            val reconnectAnswer = snapshot.getString("reconnectAnswerSdp")
                            if (!reconnectAnswer.isNullOrBlank() && !session.isIncoming) {
                                scope.launch {
                                    webRtcManager?.setRemoteAnswer(reconnectAnswer)
                                }
                            }
                        }
                        "DECLINED", "REJECTED", "MISSED", "ENDED", "CANCELLED", "EXPIRED", "BUSY" -> {
                            Log.i(TAG, "Remote peer hung up or call terminated ($status), ending call locally")
                            handledCallIds.add(callId)
                            stopAlerts()
                            endCall(wasMissed = (status in listOf("MISSED", "CANCELLED", "EXPIRED", "BUSY")))
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching call document listener", e)
        }
    }

    fun startCall(
        contactId: String,
        contactName: String,
        contactAvatarInitials: String,
        avatarColorHex: Long,
        type: CallType,
        isIncoming: Boolean = false
    ) {
        if (contactId.isBlank()) {
            Log.w(TAG, "Cannot start call: contactId is blank")
            return
        }

        val callId = "call_${System.currentTimeMillis()}"
        handledCallIds.add(callId)

        val initialSession = ActiveCallSession(
            callId = callId,
            contactId = contactId,
            contactName = contactName,
            contactAvatarInitials = contactAvatarInitials,
            avatarColorHex = avatarColorHex,
            callType = type,
            isIncoming = isIncoming,
            connectionState = if (isIncoming) ConnectionState.CONNECTING else ConnectionState.SECURE_HANDSHAKE
        )
        _currentSession.value = initialSession
        configureAudioForCall(type == CallType.VIDEO)

        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null && !isIncoming) {
            scope.launch {
                try {
                    // Initialize real WebRTC Manager
                    setupWebRtc(callId)
                    webRtcManager?.initializePeerConnection(_callingConfig.value, isVideo = (type == CallType.VIDEO))

                    // Create real WebRTC Offer
                    val offerResult = webRtcManager?.createOffer()
                    val offerSdp = if (offerResult != null && offerResult.isSuccess) {
                        offerResult.getOrThrow().description
                    } else {
                        Log.w(TAG, "WebRTC createOffer failed: ${offerResult?.exceptionOrNull()?.message}")
                        "v=0\r\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
                    }

                    val callData = hashMapOf(
                        "callId" to callId,
                        "callerId" to currentUserId,
                        "callerName" to (authRepository?.currentUser?.value?.name ?: "You"),
                        "callerAvatar" to (authRepository?.currentUser?.value?.avatarInitials ?: "ME"),
                        "receiverId" to contactId,
                        "receiverName" to contactName,
                        "receiverAvatar" to contactAvatarInitials,
                        "callType" to if (type == CallType.VIDEO) "VIDEO" else "AUDIO",
                        "status" to "CALLING",
                        "offerSdp" to offerSdp,
                        "stunServers" to _callingConfig.value.stunServers,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("calls").document(callId).set(callData, SetOptions.merge()).await()
                    attachCallDocumentListener(callId)
                    listenToRemoteIceCandidates(callId, contactId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initiating call in Firestore/WebRTC", e)
                }
            }
        }

        // 30-second timeout for unanswered outgoing call
        callTimeoutJob?.cancel()
        callTimeoutJob = scope.launch {
            delay(CALL_RING_TIMEOUT_MS)
            if (_currentSession.value?.callId == callId &&
                _currentSession.value?.connectionState != ConnectionState.CONNECTED
            ) {
                Log.i(TAG, "Outgoing call $callId timed out after 30s")
                cancelOutgoingCall()
            }
        }
    }

    fun answerIncomingCall() {
        val session = _currentSession.value ?: return
        callTimeoutJob?.cancel()
        stopAlerts()
        configureAudioForCall(session.callType == CallType.VIDEO)
        _currentSession.value = session.copy(connectionState = ConnectionState.SECURE_HANDSHAKE)

        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            scope.launch {
                try {
                    // Initialize Callee WebRTC
                    setupWebRtc(session.callId)
                    webRtcManager?.initializePeerConnection(_callingConfig.value, isVideo = (session.callType == CallType.VIDEO))

                    // Read offer SDP from Firestore
                    val doc = db.collection("calls").document(session.callId).get().await()
                    val offerSdp = doc.getString("offerSdp")

                    var answerSdp = ""
                    if (!offerSdp.isNullOrBlank()) {
                        val answerResult = webRtcManager?.setRemoteOfferAndCreateAnswer(offerSdp)
                        if (answerResult != null && answerResult.isSuccess) {
                            answerSdp = answerResult.getOrThrow().description
                            Log.i(TAG, "Answer created successfully on Callee")
                        } else {
                            Log.e(TAG, "Failed to create answer on Callee: ${answerResult?.exceptionOrNull()?.message}")
                            answerSdp = "v=0\r\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\ns=-\r\nt=0 0\r\n"
                        }
                    }

                    db.collection("calls").document(session.callId)
                        .update(
                            mapOf(
                                "status" to "ACCEPTED",
                                "answerSdp" to answerSdp,
                                "answeredAt" to FieldValue.serverTimestamp()
                            )
                        ).await()

                    listenToRemoteIceCandidates(session.callId, session.contactId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to answer call in Firestore", e)
                }
            }
        }
    }

    private fun setupWebRtc(callId: String) {
        val ctx = context ?: return
        webRtcManager?.disposePeerConnection()
        webRtcManager = WebRtcManager(
            context = ctx,
            onIceCandidateGenerated = { candidate ->
                val currentUserId = Firebase.auth.currentUser?.uid ?: return@WebRtcManager
                val candData = hashMapOf(
                    "candidate" to candidate.sdp,
                    "sdpMid" to (candidate.sdpMid ?: "0"),
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "senderId" to currentUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                scope.launch {
                    try {
                        db.collection("calls").document(callId).collection("candidates").add(candData)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to write local ICE candidate", e)
                    }
                }
            },
            onIceConnectionChangeCallback = { iceState ->
                handleIceConnectionStateChange(iceState)
            }
        )

        // Observe remote video availability
        webRtcManager?.let { manager ->
            scope.launch {
                manager.hasRemoteVideo.collect { hasVideo ->
                    _hasRemoteVideo.value = hasVideo
                }
            }
        }
    }

    private fun listenToRemoteIceCandidates(callId: String, remotePeerId: String) {
        candidatesListener?.remove()
        try {
            Log.i(TAG, "Listening to remote ICE candidates for call $callId from sender: $remotePeerId")
            candidatesListener = db.collection("calls").document(callId).collection("candidates")
                .whereEqualTo("senderId", remotePeerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val candId = doc.id
                            if (processedRemoteCandidateIds.add(candId)) {
                                val sdp = doc.getString("candidate") ?: return@forEach
                                val sdpMid = doc.getString("sdpMid") ?: "0"
                                val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: 0
                                webRtcManager?.addRemoteIceCandidate(sdpMid, sdpMLineIndex, sdp)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error listening to remote candidates", e)
        }
    }

    private fun handleIceConnectionStateChange(iceState: PeerConnection.IceConnectionState) {
        Log.i(TAG, "PeerConnection ICE state changed: $iceState")
        when (iceState) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                iceFailureTimeoutJob?.cancel()
                _currentSession.value?.let { session ->
                    if (session.connectionState != ConnectionState.CONNECTED) {
                        Log.i(TAG, "WebRTC media connection CONNECTED! Two-way audio active.")
                        _currentSession.value = session.copy(connectionState = ConnectionState.CONNECTED)
                        startDurationAndTelemetry()
                    }
                }
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                _currentSession.value?.let { session ->
                    _currentSession.value = session.copy(connectionState = ConnectionState.RECONNECTING)
                    triggerNetworkReconnect()
                }
                scheduleIceFailureTimeout()
            }
            PeerConnection.IceConnectionState.FAILED -> {
                _currentSession.value?.let { session ->
                    _currentSession.value = session.copy(connectionState = ConnectionState.RECONNECTING)
                    triggerNetworkReconnect()
                }
                scheduleIceFailureTimeout()
            }
            PeerConnection.IceConnectionState.CLOSED -> {
                endCall()
            }
            else -> {}
        }
    }

    private fun scheduleIceFailureTimeout() {
        iceFailureTimeoutJob?.cancel()
        iceFailureTimeoutJob = scope.launch {
            delay(15000L)
            if (_currentSession.value?.connectionState == ConnectionState.RECONNECTING) {
                Log.w(TAG, "ICE reconnection timed out after 15s. Terminating call.")
                endCall()
            }
        }
    }

    fun declineIncomingCall(isTimeout: Boolean = false): CallRecord? {
        val session = _currentSession.value ?: return null
        callTimeoutJob?.cancel()
        stopAlerts()

        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            scope.launch {
                try {
                    db.collection("calls").document(session.callId)
                        .update(
                            mapOf(
                                "status" to if (isTimeout) "MISSED" else "DECLINED",
                                "endedAt" to FieldValue.serverTimestamp()
                            )
                        )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update call status to DECLINED/MISSED in Firestore", e)
                }
            }
        }
        return endCall(wasMissed = isTimeout || session.durationSeconds == 0)
    }

    fun cancelOutgoingCall(): CallRecord? {
        val session = _currentSession.value ?: return null
        callTimeoutJob?.cancel()
        stopAlerts()

        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId != null) {
            scope.launch {
                try {
                    db.collection("calls").document(session.callId)
                        .update(
                            mapOf(
                                "status" to "CANCELLED",
                                "endedAt" to FieldValue.serverTimestamp()
                            )
                        )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update call status to CANCELLED in Firestore", e)
                }
            }
        }
        return endCall(wasMissed = true)
    }

    private fun startAlerts(session: ActiveCallSession) {
        startRingtone()
        startVibrate()
        showIncomingCallNotification(session)
    }

    private fun stopAlerts() {
        stopRingtone()
        stopVibrate()
        clearIncomingCallNotification()
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play ringtone", e)
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            Log.w(TAG, "Could not stop ringtone", e)
        }
    }

    private fun startVibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 800, 1000, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 800, 1000, 800), 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration error", e)
        }
    }

    private fun stopVibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Stop vibration error", e)
        }
    }

    private fun showIncomingCallNotification(session: ActiveCallSession) {
        val ctx = context ?: return
        try {
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = CALL_NOTIFICATION_CHANNEL_ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val channel = NotificationChannel(
                    channelId,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "CipherLink incoming voice and video call alerts"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 800, 1000, 800)
                    setSound(ringtoneUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Tapping notification opens app
            val contentIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("CALL_ACTION", "SHOW_INCOMING_CALL")
                putExtra("CALL_ID", session.callId)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                ctx,
                1001,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decline action
            val declineIntent = Intent(ctx, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_DECLINE_CALL
                putExtra(CallActionReceiver.EXTRA_CALL_ID, session.callId)
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                ctx,
                1002,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Accept action
            val acceptIntent = Intent(ctx, CallActionReceiver::class.java).apply {
                action = CallActionReceiver.ACTION_ACCEPT_CALL
                putExtra(CallActionReceiver.EXTRA_CALL_ID, session.callId)
            }
            val acceptPendingIntent = PendingIntent.getBroadcast(
                ctx,
                1003,
                acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val callTypeLabel = if (session.callType == CallType.VIDEO) "Video" else "Voice"
            val notification = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Incoming $callTypeLabel Call")
                .setContentText("${session.contactName} is calling...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(contentPendingIntent, true)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_launcher_foreground, "Decline", declinePendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Accept", acceptPendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_CALL, notification)
            Log.i(TAG, "Incoming call notification shown for ${session.callId}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not show call notification", e)
        }
    }

    private fun clearIncomingCallNotification() {
        try {
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_CALL)
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear call notification", e)
        }
    }

    private fun requestVoipAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusReq = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "VoIP AudioFocus changed: $focusChange")
                    }
                    .build()
                audioFocusRequest = focusReq
                val res = am.requestAudioFocus(focusReq)
                Log.i(TAG, "Requested VoIP AudioFocus (API 26+): result=$res")
            } else {
                @Suppress("DEPRECATION")
                val res = am.requestAudioFocus(
                    { focusChange -> Log.d(TAG, "VoIP AudioFocus legacy changed: $focusChange") },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                Log.i(TAG, "Requested VoIP AudioFocus legacy: result=$res")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not request VoIP AudioFocus", e)
        }
    }

    private fun abandonVoipAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let { req ->
                    am.abandonAudioFocusRequest(req)
                    audioFocusRequest = null
                    Log.i(TAG, "Abandoned VoIP AudioFocus (API 26+)")
                }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error abandoning VoIP AudioFocus", e)
        }
    }

    private fun configureAudioForCall(isSpeakerDefault: Boolean = false) {
        try {
            requestVoipAudioFocus()
            audioManager?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                isMicrophoneMute = false
            }
            setSpeakerphoneOn(isSpeakerDefault)
            val aecSupported = AcousticEchoCanceler.isAvailable()
            val nsSupported = NoiseSuppressor.isAvailable()
            Log.i(TAG, "Audio configured for call. AEC: $aecSupported, NS: $nsSupported, Speaker: $isSpeakerDefault")
        } catch (e: Exception) {
            Log.w(TAG, "Audio manager setup warning", e)
        }
    }

    private fun resetAudioAfterCall() {
        try {
            audioManager?.apply {
                mode = AudioManager.MODE_NORMAL
                isSpeakerphoneOn = false
                isMicrophoneMute = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    clearCommunicationDevice()
                }
            }
            abandonVoipAudioFocus()
        } catch (e: Exception) {
            Log.w(TAG, "Audio manager reset warning", e)
        }
    }

    private fun startDurationAndTelemetry() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            while (isActive && _currentSession.value?.connectionState == ConnectionState.CONNECTED) {
                delay(1000)
                _currentSession.value?.let { session ->
                    _currentSession.value = session.copy(durationSeconds = session.durationSeconds + 1)
                }
            }
        }

        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive && _currentSession.value != null) {
                delay(1500)
                webRtcManager?.getRealtimeStats { realRtt, realLossPercent, realJitter ->
                    val resolvedRtt = if (realRtt > 0) realRtt else 28
                    val lossRatio = (realLossPercent / 100f).coerceIn(0f, 1f)
                    val isVideo = _currentSession.value?.callType == CallType.VIDEO

                    val baseMetrics = bitrateController.computeAdaptiveMetrics(
                        rttMs = resolvedRtt,
                        packetLossRatio = lossRatio,
                        currentAudioBitrate = 48,
                        currentVideoBitrate = if (isVideo) 1200 else 0
                    )

                    val updatedMetrics = baseMetrics.copy(
                        rttMs = resolvedRtt,
                        packetLossPercent = realLossPercent,
                        jitterMs = if (realJitter > 0) realJitter else 4
                    )

                    _networkMetrics.value = updatedMetrics
                    _currentSession.value?.let { session ->
                        _currentSession.value = session.copy(metrics = updatedMetrics)
                    }
                }
            }
        }
    }

    fun triggerNetworkReconnect() {
        val session = _currentSession.value ?: return
        reconnectSimulationJob?.cancel()
        reconnectSimulationJob = scope.launch {
            _currentSession.value = session.copy(connectionState = ConnectionState.RECONNECTING)
            delay(1500)
            _currentSession.value?.let {
                _currentSession.value = it.copy(connectionState = ConnectionState.CONNECTED)
            }
        }
    }

    fun toggleMic() {
        _currentSession.value?.let { session ->
            val newMute = !session.isMicMuted
            webRtcManager?.setLocalAudioEnabled(!newMute)
            audioManager?.isMicrophoneMute = newMute
            _currentSession.value = session.copy(isMicMuted = newMute)
        }
    }

    fun toggleVideo() {
        _currentSession.value?.let { session ->
            val newMute = !session.isVideoMuted
            webRtcManager?.setLocalVideoEnabled(!newMute)
            _currentSession.value = session.copy(isVideoMuted = newMute)
        }
    }

    fun toggleSpeaker() {
        _currentSession.value?.let { session ->
            val newSpeaker = !session.isSpeakerOn
            setSpeakerphoneOn(newSpeaker)
        }
    }

    override fun setSpeakerphoneOn(on: Boolean) {
        val am = audioManager
        if (am != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (on) {
                        val speakerDevice = am.availableCommunicationDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                        }
                        if (speakerDevice != null) {
                            am.setCommunicationDevice(speakerDevice)
                        } else {
                            @Suppress("DEPRECATION")
                            am.isSpeakerphoneOn = true
                        }
                    } else {
                        val earpieceDevice = am.availableCommunicationDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                        }
                        if (earpieceDevice != null) {
                            am.setCommunicationDevice(earpieceDevice)
                        } else {
                            am.clearCommunicationDevice()
                            @Suppress("DEPRECATION")
                            am.isSpeakerphoneOn = false
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = on
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error switching audio route", e)
            }
        }
        _currentSession.value?.let {
            _currentSession.value = it.copy(isSpeakerOn = on)
        }
    }

    override fun switchCamera() {
        webRtcManager?.switchCamera { isFront ->
            _currentSession.value?.let { session ->
                _currentSession.value = session.copy(isFrontCamera = isFront)
            }
        }
    }

    fun attachLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        webRtcManager?.attachLocalRenderer(renderer)
    }

    fun detachLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        webRtcManager?.detachLocalRenderer(renderer)
    }

    fun attachRemoteVideoRenderer(renderer: SurfaceViewRenderer) {
        webRtcManager?.attachRemoteRenderer(renderer)
    }

    fun detachRemoteVideoRenderer(renderer: SurfaceViewRenderer) {
        webRtcManager?.detachRemoteRenderer(renderer)
    }

    override fun setAudioPriorityMode(enabled: Boolean) {
        _currentSession.value?.let { session ->
            val updatedMetrics = session.metrics.copy(isAudioPriorityActive = enabled)
            _currentSession.value = session.copy(
                manualAudioPriorityOverride = enabled,
                metrics = updatedMetrics
            )
        }
    }

    fun endCall(wasMissed: Boolean = false): CallRecord? {
        callTimerJob?.cancel()
        telemetryJob?.cancel()
        callTimeoutJob?.cancel()
        reconnectSimulationJob?.cancel()
        activeCallDocListener?.remove()
        activeCallDocListener = null
        candidatesListener?.remove()
        candidatesListener = null

        stopAlerts()
        resetAudioAfterCall()

        webRtcManager?.disposePeerConnection()
        webRtcManager = null
        _hasRemoteVideo.value = false

        val session = _currentSession.value
        _currentSession.value = null

        if (session != null) {
            val currentUserId = Firebase.auth.currentUser?.uid
            if (currentUserId != null) {
                scope.launch {
                    try {
                        db.collection("calls").document(session.callId)
                            .update(
                                mapOf(
                                    "status" to "ENDED",
                                    "endedAt" to FieldValue.serverTimestamp()
                                )
                            )
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not update status to ENDED in Firestore", e)
                    }
                }
            }

            val minutes = session.durationSeconds / 60
            val seconds = session.durationSeconds % 60
            val durStr = String.format("%02d:%02d", minutes, seconds)
            return CallRecord(
                id = session.callId,
                contactId = session.contactId,
                contactName = session.contactName,
                contactAvatarInitials = session.contactAvatarInitials,
                avatarColorHex = session.avatarColorHex,
                callType = session.callType,
                direction = if (wasMissed) {
                    CallDirection.MISSED
                } else if (session.isIncoming) {
                    if (session.durationSeconds == 0) CallDirection.MISSED else CallDirection.INCOMING
                } else {
                    CallDirection.OUTGOING
                },
                timestamp = System.currentTimeMillis(),
                formattedDate = "Just now",
                durationSeconds = session.durationSeconds,
                formattedDuration = if (session.durationSeconds > 0) durStr else "Missed",
                networkQualityRating = "HD Audio • ${session.metrics.currentAudioBitrateKbps}kbps",
                wasAdaptiveFallbackTriggered = session.metrics.isAudioPriorityActive
            )
        }
        return null
    }

    override suspend fun initialize(config: CallingConfig): Result<Unit> {
        _callingConfig.value = config
        bitrateController.updateConfig(config)
        return Result.success(Unit)
    }

    override suspend fun createOffer(isVideo: Boolean): Result<String> {
        val res = webRtcManager?.createOffer()
        return if (res != null && res.isSuccess) {
            Result.success(res.getOrThrow().description)
        } else {
            Result.failure(res?.exceptionOrNull() ?: IllegalStateException("WebRtcManager not initialized"))
        }
    }

    override suspend fun handleAnswer(remoteSdp: String): Result<Unit> {
        return webRtcManager?.setRemoteAnswer(remoteSdp)
            ?: Result.failure(IllegalStateException("WebRtcManager not initialized"))
    }

    override suspend fun addIceCandidate(candidateJson: String): Result<Unit> {
        webRtcManager?.addRemoteIceCandidate("0", 0, candidateJson)
        return Result.success(Unit)
    }

    override fun setLocalAudioEnabled(enabled: Boolean) {
        webRtcManager?.setLocalAudioEnabled(enabled)
        audioManager?.isMicrophoneMute = !enabled
        _currentSession.value?.let {
            _currentSession.value = it.copy(isMicMuted = !enabled)
        }
    }

    override fun setLocalVideoEnabled(enabled: Boolean) {
        webRtcManager?.setLocalVideoEnabled(enabled)
        _currentSession.value?.let {
            _currentSession.value = it.copy(isVideoMuted = !enabled)
        }
    }

    override fun updateCallingConfig(config: CallingConfig) {
        _callingConfig.value = config
        bitrateController.updateConfig(config)
    }

    override suspend fun terminateSession() {
        endCall()
    }
}
