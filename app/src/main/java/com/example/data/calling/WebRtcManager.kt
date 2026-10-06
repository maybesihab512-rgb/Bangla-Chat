package com.example.data.calling

import android.content.Context
import android.util.Log
import com.example.model.CallingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.Collections
import kotlin.coroutines.resume

class WebRtcManager(
    private val context: Context,
    private val onIceCandidateGenerated: (IceCandidate) -> Unit,
    private val onIceConnectionChangeCallback: (PeerConnection.IceConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "WebRtcManager"
        private var isFactoryInitialized = false

        fun initializeFactoryIfNeeded(context: Context) {
            if (!isFactoryInitialized) {
                val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(initOptions)
                isFactoryInitialized = true
                Log.d(TAG, "PeerConnectionFactory global initialization complete")
            }
        }
    }

    val eglBase: EglBase by lazy { EglBase.create() }

    private var audioDeviceModule: AudioDeviceModule? = null

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        initializeFactoryIfNeeded(context)

        // Setup JavaAudioDeviceModule with hardware AEC and Noise Suppressor for clear two-way audio
        val adm = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
            .setUseHardwareNoiseSuppressor(JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(err: String?) {
                    Log.e(TAG, "AudioRecord init error: $err")
                }
                override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                    err: String?
                ) {
                    Log.e(TAG, "AudioRecord start error: $errorCode - $err")
                }
                override fun onWebRtcAudioRecordError(err: String?) {
                    Log.e(TAG, "AudioRecord runtime error: $err")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(err: String?) {
                    Log.e(TAG, "AudioTrack init error: $err")
                }
                override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                    err: String?
                ) {
                    Log.e(TAG, "AudioTrack start error: $errorCode - $err")
                }
                override fun onWebRtcAudioTrackError(err: String?) {
                    Log.e(TAG, "AudioTrack runtime error: $err")
                }
            })
            .createAudioDeviceModule()
        audioDeviceModule = adm

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        PeerConnectionFactory.builder()
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var remoteAudioTrack: AudioTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private val _hasRemoteVideo = MutableStateFlow(false)
    val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private var isVideoCall: Boolean = false
    var isFrontCamera: Boolean = true
        private set

    // Thread-safe buffer for ICE candidates arriving before setRemoteDescription completes
    private val pendingIceCandidates = Collections.synchronizedList(mutableListOf<IceCandidate>())

    fun initializePeerConnection(config: CallingConfig, isVideo: Boolean): Result<Unit> {
        return try {
            disposePeerConnection()
            isVideoCall = isVideo
            _hasRemoteVideo.value = false

            val iceServers = mutableListOf<PeerConnection.IceServer>()

            // 1. STUN Servers for reflexive candidates
            config.stunServers.forEach { stunUrl ->
                val cleanUrl = stunUrl.trim()
                if (cleanUrl.isNotBlank()) {
                    iceServers.add(PeerConnection.IceServer.builder(cleanUrl).createIceServer())
                }
            }

            // 2. Verified TURN Servers for cross-network relaying through restrictive symmetric NATs
            config.turnServers.forEach { turnConfig ->
                if (turnConfig.isValid()) {
                    val uri = turnConfig.uri.trim()
                    val builder = PeerConnection.IceServer.builder(uri)
                    turnConfig.username?.trim()?.let { if (it.isNotEmpty()) builder.setUsername(it) }
                    turnConfig.password?.trim()?.let { if (it.isNotEmpty()) builder.setPassword(it) }
                    builder.setTlsCertPolicy(turnConfig.tlsCertPolicy)
                    iceServers.add(builder.createIceServer())
                    Log.i(TAG, "Configured authenticated TURN server: $uri")
                } else {
                    Log.d(TAG, "Ignoring invalid/unauthenticated TURN configuration: ${turnConfig.uri}")
                }
            }

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                iceTransportsType = PeerConnection.IceTransportsType.ALL
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                keyType = PeerConnection.KeyType.ECDSA
                iceCandidatePoolSize = 2
            }

            val pcObserver = object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(TAG, "onSignalingChange: $state")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "onIceConnectionChange: $state")
                    onIceConnectionChangeCallback(state)
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "onIceGatheringChange: $state")
                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    Log.d(TAG, "Local IceCandidate generated: mid=${candidate.sdpMid}, sdp=${candidate.sdp}")
                    onIceCandidateGenerated(candidate)
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                    Log.d(TAG, "onIceCandidatesRemoved")
                }

                override fun onAddStream(stream: MediaStream) {
                    Log.d(TAG, "onAddStream with ${stream.audioTracks.size} audio, ${stream.videoTracks.size} video")
                    if (stream.audioTracks.isNotEmpty()) {
                        handleRemoteAudioTrack(stream.audioTracks[0])
                    }
                    if (stream.videoTracks.isNotEmpty() && isVideoCall) {
                        handleRemoteVideoTrack(stream.videoTracks[0])
                    }
                }

                override fun onRemoveStream(stream: MediaStream?) {
                    Log.d(TAG, "onRemoveStream")
                }

                override fun onDataChannel(dc: org.webrtc.DataChannel?) {
                    Log.d(TAG, "onDataChannel")
                }

                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded")
                }

                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                    Log.d(TAG, "onAddTrack: kind=${receiver?.track()?.kind()}")
                    val track = receiver?.track()
                    if (track is AudioTrack) {
                        handleRemoteAudioTrack(track)
                    } else if (track is VideoTrack && isVideoCall) {
                        handleRemoteVideoTrack(track)
                    }
                }

                override fun onTrack(transceiver: RtpTransceiver) {
                    val track = transceiver.receiver.track()
                    Log.d(TAG, "onTrack received: kind=${track?.kind()}")
                    if (track is AudioTrack) {
                        handleRemoteAudioTrack(track)
                    } else if (track is VideoTrack && isVideoCall) {
                        handleRemoteVideoTrack(track)
                    }
                }
            }

            val pc = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver)
                ?: return Result.failure(IllegalStateException("Failed to create PeerConnection"))
            peerConnection = pc

            // 1. Create and attach Local Audio Track
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            }
            val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
            localAudioSource = audioSource
            val audioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource)
            audioTrack.setEnabled(true)
            localAudioTrack = audioTrack
            pc.addTrack(audioTrack, listOf("ARDAMS"))

            // 2. If video call, create and attach Local Video Track
            if (isVideo) {
                setupVideoCapturerAndTrack(pc)
            }

            // Ensure Unified Plan transceivers are set to SEND_RECV direction
            pc.transceivers.forEach { transceiver ->
                if (transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO) {
                    transceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                }
                if (transceiver.mediaType == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO && isVideo) {
                    transceiver.direction = RtpTransceiver.RtpTransceiverDirection.SEND_RECV
                }
            }

            Log.i(TAG, "WebRTC PeerConnection initialized successfully (isVideo=$isVideo)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC", e)
            Result.failure(e)
        }
    }

    private fun handleRemoteAudioTrack(track: AudioTrack) {
        Log.i(TAG, "Binding and enabling remote AudioTrack: id=${track.id()}")
        remoteAudioTrack = track
        try {
            track.setEnabled(true)
            track.setVolume(1.0)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up remote audio track", e)
        }
    }

    private fun handleRemoteVideoTrack(track: VideoTrack) {
        if (!isVideoCall) return
        Log.i(TAG, "Binding and enabling remote VideoTrack: id=${track.id()}")
        remoteVideoTrack = track
        try {
            track.setEnabled(true)
            remoteRenderer?.let {
                track.addSink(it)
            }
            _hasRemoteVideo.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up remote video track", e)
        }
    }

    private fun setupVideoCapturerAndTrack(pc: PeerConnection) {
        try {
            val enumerator = if (Camera2Enumerator.isSupported(context)) {
                Camera2Enumerator(context)
            } else {
                Camera1Enumerator(true)
            }

            val deviceNames = enumerator.deviceNames
            val frontName = deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: deviceNames.firstOrNull()
            if (frontName != null) {
                isFrontCamera = enumerator.isFrontFacing(frontName)
                val capturer = enumerator.createCapturer(frontName, null)
                videoCapturer = capturer

                val sth = SurfaceTextureHelper.create("WebRtcSurfaceTexture", eglBase.eglBaseContext)
                surfaceTextureHelper = sth

                val videoSource = peerConnectionFactory.createVideoSource(false)
                localVideoSource = videoSource

                capturer.initialize(sth, context, videoSource.capturerObserver)
                // 640x480 at 30fps provides optimal balance between clarity and mobile bandwidth efficiency
                capturer.startCapture(640, 480, 30)

                val videoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource)
                videoTrack.setEnabled(true)
                localVideoTrack = videoTrack
                pc.addTrack(videoTrack, listOf("ARDAMS"))

                // Attach to local renderer if already registered
                localRenderer?.let {
                    try { videoTrack.addSink(it) } catch (e: Exception) { Log.w(TAG, "Error adding local sink", e) }
                }

                Log.i(TAG, "Local video track attached from camera: $frontName (isFront=$isFrontCamera)")
            } else {
                Log.w(TAG, "No suitable camera found on device")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start video capturer: ${e.message}")
        }
    }

    fun initSurfaceRenderer(renderer: SurfaceViewRenderer, mirror: Boolean = false) {
        try {
            renderer.init(eglBase.eglBaseContext, null)
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            renderer.setMirror(mirror)
            renderer.setEnableHardwareScaler(true)
        } catch (e: Exception) {
            Log.d(TAG, "SurfaceViewRenderer init status: ${e.message}")
        }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        initSurfaceRenderer(renderer, mirror = isFrontCamera)
        localVideoTrack?.let { track ->
            try { track.addSink(renderer) } catch (e: Exception) { Log.w(TAG, "Error adding local sink", e) }
        }
        Log.i(TAG, "Attached local video renderer")
    }

    fun detachLocalRenderer(renderer: SurfaceViewRenderer) {
        localVideoTrack?.let { track ->
            try { track.removeSink(renderer) } catch (_: Exception) {}
        }
        if (localRenderer == renderer) {
            localRenderer = null
        }
        try { renderer.release() } catch (_: Exception) {}
        Log.i(TAG, "Detached local video renderer")
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        initSurfaceRenderer(renderer, mirror = false)
        remoteVideoTrack?.let { track ->
            try { track.addSink(renderer) } catch (e: Exception) { Log.w(TAG, "Error adding remote sink", e) }
            _hasRemoteVideo.value = true
        }
        Log.i(TAG, "Attached remote video renderer")
    }

    fun detachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteVideoTrack?.let { track ->
            try { track.removeSink(renderer) } catch (_: Exception) {}
        }
        if (remoteRenderer == renderer) {
            remoteRenderer = null
        }
        try { renderer.release() } catch (_: Exception) {}
        Log.i(TAG, "Detached remote video renderer")
    }

    suspend fun createOffer(): Result<SessionDescription> = suspendCancellableCoroutine { cont ->
        val pc = peerConnection ?: run {
            cont.resume(Result.failure(IllegalStateException("PeerConnection not initialized")))
            return@suspendCancellableCoroutine
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.i(TAG, "Local offer set successfully")
                        if (cont.isActive) cont.resume(Result.success(sdp))
                    }
                    override fun onCreateFailure(err: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "SetLocalDescription error: $err")
                        if (cont.isActive) cont.resume(Result.failure(Exception("Set local description failed: $err")))
                    }
                }, sdp)
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "CreateOffer error: $err")
                if (cont.isActive) cont.resume(Result.failure(Exception("Create offer failed: $err")))
            }
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    suspend fun setRemoteOfferAndCreateAnswer(remoteOfferSdp: String): Result<SessionDescription> = suspendCancellableCoroutine { cont ->
        val pc = peerConnection ?: run {
            cont.resume(Result.failure(IllegalStateException("PeerConnection not initialized")))
            return@suspendCancellableCoroutine
        }

        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, remoteOfferSdp)
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.i(TAG, "Remote offer set successfully, draining queued ICE candidates and creating answer...")
                drainPendingIceCandidates()

                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
                }
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answerSdp: SessionDescription) {
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.i(TAG, "Local answer set successfully")
                                if (cont.isActive) cont.resume(Result.success(answerSdp))
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(err: String?) {
                                Log.e(TAG, "SetLocalDescription answer error: $err")
                                if (cont.isActive) cont.resume(Result.failure(Exception("Set local answer failed: $err")))
                            }
                        }, answerSdp)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(err: String?) {
                        Log.e(TAG, "CreateAnswer error: $err")
                        if (cont.isActive) cont.resume(Result.failure(Exception("Create answer failed: $err")))
                    }
                    override fun onSetFailure(p0: String?) {}
                }, constraints)
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "SetRemoteDescription offer error: $err")
                if (cont.isActive) cont.resume(Result.failure(Exception("Set remote offer failed: $err")))
            }
        }, offerDesc)
    }

    suspend fun setRemoteAnswer(remoteAnswerSdp: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        val pc = peerConnection ?: run {
            cont.resume(Result.failure(IllegalStateException("PeerConnection not initialized")))
            return@suspendCancellableCoroutine
        }

        val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, remoteAnswerSdp)
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.i(TAG, "Remote answer set successfully, draining queued ICE candidates...")
                drainPendingIceCandidates()
                if (cont.isActive) cont.resume(Result.success(Unit))
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "SetRemoteDescription answer error: $err")
                if (cont.isActive) cont.resume(Result.failure(Exception("Set remote answer failed: $err")))
            }
        }, answerDesc)
    }

    suspend fun restartIceAndCreateOffer(): Result<SessionDescription> = suspendCancellableCoroutine { cont ->
        val pc = peerConnection ?: run {
            cont.resume(Result.failure(IllegalStateException("PeerConnection not initialized")))
            return@suspendCancellableCoroutine
        }
        pc.restartIce()
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
        }
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.i(TAG, "ICE restart local offer set successfully")
                        if (cont.isActive) cont.resume(Result.success(sdp))
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(err: String?) {
                        Log.e(TAG, "ICE restart setLocalDescription error: $err")
                        if (cont.isActive) cont.resume(Result.failure(Exception("Set local description failed: $err")))
                    }
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "ICE restart createOffer error: $err")
                if (cont.isActive) cont.resume(Result.failure(Exception("ICE restart offer failed: $err")))
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        try {
            val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
            val pc = peerConnection
            if (pc != null && pc.remoteDescription != null) {
                pc.addIceCandidate(candidate)
                Log.d(TAG, "Added remote ICE candidate directly: mid=$sdpMid, mLine=$sdpMLineIndex")
            } else {
                Log.d(TAG, "Queued remote ICE candidate (remoteDescription not set yet): mid=$sdpMid")
                pendingIceCandidates.add(candidate)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error adding remote ICE candidate", e)
        }
    }

    private fun drainPendingIceCandidates() {
        val pc = peerConnection ?: return
        if (pc.remoteDescription == null) return
        synchronized(pendingIceCandidates) {
            val iterator = pendingIceCandidates.iterator()
            var drainedCount = 0
            while (iterator.hasNext()) {
                val cand = iterator.next()
                try {
                    pc.addIceCandidate(cand)
                    drainedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed adding buffered ICE candidate: ${cand.sdpMid}", e)
                }
                iterator.remove()
            }
            if (drainedCount > 0) {
                Log.i(TAG, "Successfully drained $drainedCount buffered ICE candidates")
            }
        }
    }

    fun setAudioPriorityMode(enabled: Boolean) {
        if (!isVideoCall) return
        try {
            // When audio priority is active, suspend local video track transmission to preserve 100% bandwidth for audio
            localVideoTrack?.setEnabled(!enabled)
            Log.i(TAG, "AudioPriorityMode: videoTrack enabled = ${!enabled}")
        } catch (e: Exception) {
            Log.w(TAG, "Error adjusting video track for audio priority", e)
        }
    }

    fun adaptVideoQuality(maxBitrateKbps: Int) {
        if (!isVideoCall) return
        val pc = peerConnection ?: return
        try {
            pc.senders.forEach { sender ->
                if (sender.track() is VideoTrack) {
                    val params = sender.parameters
                    if (params.encodings.isNotEmpty()) {
                        val encoding = params.encodings[0]
                        if (maxBitrateKbps > 0) {
                            encoding.maxBitrateBps = maxBitrateKbps * 1000
                            encoding.minBitrateBps = (maxBitrateKbps * 1000) / 4
                        } else {
                            encoding.maxBitrateBps = null
                        }
                        sender.parameters = params
                        Log.d(TAG, "Adapted video sender maxBitrate to ${maxBitrateKbps}kbps")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error adapting video quality parameters", e)
        }
    }

    fun getRealtimeStats(callback: (rttMs: Int, packetLossPercent: Float, jitterMs: Int) -> Unit) {
        val pc = peerConnection ?: return
        try {
            pc.getStats(RTCStatsCollectorCallback { report ->
                var roundTripTimeMs = -1
                var packetsLost = 0L
                var packetsReceived = 0L
                var jitterMs = 0

                for (stats in report.statsMap.values) {
                    if (stats.type == "candidate-pair") {
                        val currentRtt = (stats.members["currentRoundTripTime"] as? Number)?.toDouble()
                            ?: (stats.members["totalRoundTripTime"] as? Number)?.toDouble()
                        if (currentRtt != null && currentRtt > 0.0) {
                            roundTripTimeMs = (currentRtt * 1000.0).toInt()
                        }
                    } else if (stats.type == "inbound-rtp") {
                        val lost = (stats.members["packetsLost"] as? Number)?.toLong() ?: 0L
                        val rec = (stats.members["packetsReceived"] as? Number)?.toLong() ?: 0L
                        val jit = (stats.members["jitter"] as? Number)?.toDouble() ?: 0.0
                        packetsLost += lost
                        packetsReceived += rec
                        if (jit > 0.0) {
                            jitterMs = (jit * 1000.0).toInt()
                        }
                    }
                }

                val totalPackets = packetsLost + packetsReceived
                val lossPercent = if (totalPackets > 0L) {
                    ((packetsLost.toDouble() / totalPackets.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
                } else {
                    0.0f
                }

                callback(roundTripTimeMs, lossPercent, jitterMs)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Error querying WebRTC Stats API", e)
        }
    }

    fun setLocalAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera(onComplete: ((isFront: Boolean) -> Unit)? = null) {
        val capturer = videoCapturer as? CameraVideoCapturer ?: return
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isFrontCamera = isFront
                localRenderer?.setMirror(isFront)
                onComplete?.invoke(isFront)
                Log.i(TAG, "Switched camera. isFrontCamera=$isFront")
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.e(TAG, "Camera switch error: $errorDescription")
            }
        })
    }

    fun disposePeerConnection() {
        try {
            localRenderer?.let {
                try { localVideoTrack?.removeSink(it) } catch (_: Exception) {}
            }
            remoteRenderer?.let {
                try { remoteVideoTrack?.removeSink(it) } catch (_: Exception) {}
            }
            localRenderer = null
            remoteRenderer = null
            _hasRemoteVideo.value = false

            try {
                videoCapturer?.stopCapture()
                videoCapturer?.dispose()
            } catch (_: Exception) {}
            videoCapturer = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            localVideoTrack?.setEnabled(false)
            localVideoTrack?.dispose()
            localVideoTrack = null

            localVideoSource?.dispose()
            localVideoSource = null

            localAudioTrack?.setEnabled(false)
            localAudioTrack?.dispose()
            localAudioTrack = null

            localAudioSource?.dispose()
            localAudioSource = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
            pendingIceCandidates.clear()

            audioDeviceModule?.release()
            audioDeviceModule = null

            Log.d(TAG, "PeerConnection and media tracks cleanly disposed")
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing PeerConnection", e)
        }
    }

    fun release() {
        disposePeerConnection()
    }
}

