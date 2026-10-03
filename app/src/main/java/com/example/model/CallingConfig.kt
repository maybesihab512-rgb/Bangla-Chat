package com.example.model

data class IceServerConfig(
    val uri: String,
    val username: String? = null,
    val password: String? = null
)

data class CallingConfig(
    val adaptiveBitrateEnabled: Boolean = true,
    val weakNetworkAudioPriorityEnabled: Boolean = true,
    val dataSavingModeEnabled: Boolean = false,
    val echoCancellationEnabled: Boolean = true,
    val noiseSuppressionEnabled: Boolean = true,
    val forwardErrorCorrectionEnabled: Boolean = true,
    val wifiMobileSeamlessHandoff: Boolean = true,
    val preferredVideoCodec: String = "VP9 / AV1",
    val preferredAudioCodec: String = "OPUS Fullband (Adaptive)",
    val stunServers: List<String> = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302"
    ),
    val turnServers: List<IceServerConfig> = listOf(
        IceServerConfig(
            uri = "turn:turn.openrelay.metered.ca:80",
            username = "openrelay",
            password = "openrelaypassword"
        )
    )
)
