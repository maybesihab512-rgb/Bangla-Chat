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

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        initializeFactoryIfNeeded(context)
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        PeerConnectionFactory.builder()
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
            config.stunServers.forEach { stunUrl ->
                iceServers.add(PeerConnection.IceServer.builder(stunUrl).createIceServer())
            }
            config.turnServers.forEach { turnConfig ->
                val builder = PeerConnection.IceServer.builder(turnConfig.uri)
                if (!turnConfig.username.isNullOrBlank()) {
                    builder.setUsername(turnConfig.username)
                }
                if (!turnConfig.password.isNullOrBlank()) {
                    builder.setPassword(turnConfig.password)
                }
                iceServers.add(builder.createIceServer())
            }

            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                iceTransportsType = PeerConnection.IceTransportsType.ALL
                tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
                bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
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
                        remoteAudioTrack = stream.audioTracks[0]
                        remoteAudioTrack?.setEnabled(true)
                        remoteAudioTrack?.setVolume(1.0)
                    }
                    if (stream.videoTracks.isNotEmpty() && isVideoCall) {
                        val videoTrack = stream.videoTracks[0]
                        remoteVideoTrack = videoTrack
                        videoTrack.setEnabled(true)
                        remoteRenderer?.let {
                            try { videoTrack.addSink(it) } catch (e: Exception) { Log.w(TAG, "Error adding sink", e) }
                        }
                        _hasRemoteVideo.value = true
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
                    Log.d(TAG, "onAddTrack")
                }

                override fun onTrack(transceiver: RtpTransceiver) {
                    val track = transceiver.receiver.track()
                    Log.d(TAG, "onTrack received: kind=${track?.kind()}")
                    if (track is AudioTrack) {
                        remoteAudioTrack = track
                        track.setEnabled(true)
                        track.setVolume(1.0)
                    } else if (track is VideoTrack && isVideoCall) {
                        remoteVideoTrack = track
                        track.setEnabled(true)
                        remoteRenderer?.let {
                            try { track.addSink(it) } catch (e: Exception) { Log.w(TAG, "Error adding sink", e) }
                        }
                        _hasRemoteVideo.value = true
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

            Log.i(TAG, "WebRTC PeerConnection initialized successfully (isVideo=$isVideo)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC", e)
            Result.failure(e)
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

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        try {
            val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
            val pc = peerConnection
            if (pc != null && pc.remoteDescription != null) {
                pc.addIceCandidate(candidate)
                Log.d(TAG, "Added remote ICE candidate directly: $sdpMid")
            } else {
                Log.d(TAG, "Queued remote ICE candidate (remoteDescription not set yet): $sdpMid")
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
            while (iterator.hasNext()) {
                val cand = iterator.next()
                try {
                    pc.addIceCandidate(cand)
                    Log.d(TAG, "Drained pending ICE candidate: ${cand.sdpMid}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed adding buffered ICE candidate", e)
                }
                iterator.remove()
            }
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
            Log.d(TAG, "PeerConnection and media tracks cleanly disposed")
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing PeerConnection", e)
        }
    }

    fun release() {
        disposePeerConnection()
    }
}
