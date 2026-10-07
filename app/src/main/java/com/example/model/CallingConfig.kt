package com.example.model

import org.webrtc.PeerConnection

data class IceServerConfig(
    val uri: String,
    val username: String? = null,
    val password: String? = null,
    val tlsCertPolicy: PeerConnection.TlsCertPolicy = PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE
) {
    /**
     * Validates whether this TURN/STUN configuration contains authentic
     * server parameters suitable for cross-NAT relaying.
     */
    fun isValid(): Boolean {
        val u = uri.trim()
        val isTurn = u.startsWith("turn:", ignoreCase = true) || u.startsWith("turns:", ignoreCase = true)
        val isStun = u.startsWith("stun:", ignoreCase = true)
        if (!isTurn && !isStun) return false
        if (isTurn) {
            val user = username?.trim().orEmpty()
            val pass = password?.trim().orEmpty()
            if (user.isEmpty() || pass.isEmpty()) return false
        }
        return true
    }
}

data class CallingConfig(
    val adaptiveBitrateEnabled: Boolean = true,
    val weakNetworkAudioPriorityEnabled: Boolean = true,
    val dataSavingModeEnabled: Boolean = false,
    val echoCancellationEnabled: Boolean = true,
    val noiseSuppressionEnabled: Boolean = true,
    val forwardErrorCorrectionEnabled: Boolean = true,
    val wifiMobileSeamlessHandoff: Boolean = true,
    val preferredVideoCodec: String = "VP8 / VP9",
    val preferredAudioCodec: String = "OPUS Fullband (Adaptive)",
    val stunServers: List<String> = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302",
        "stun:stun.cloudflare.com:3478",
        "stun:stun.services.mozilla.com:3478"
    ),
    // Production TURN servers for traversal through restrictive symmetric NATs and mobile CGNAT
    val turnServers: List<IceServerConfig> = listOf(
        IceServerConfig(
            uri = "turn:openrelay.metered.ca:80",
            username = "openrelay",
            password = "openrelay"
        ),
        IceServerConfig(
            uri = "turn:openrelay.metered.ca:443",
            username = "openrelay",
            password = "openrelay"
        ),
        IceServerConfig(
            uri = "turn:openrelay.metered.ca:443?transport=tcp",
            username = "openrelay",
            password = "openrelay"
        ),
        IceServerConfig(
            uri = "turns:openrelay.metered.ca:443?transport=tcp",
            username = "openrelay",
            password = "openrelay",
            tlsCertPolicy = PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK
        )
    )
) {
    fun withTurnServer(uri: String, username: String, password: String): CallingConfig {
        val newTurn = IceServerConfig(uri, username, password)
        return copy(turnServers = turnServers + newTurn)
    }

    val hasValidTurnServers: Boolean
        get() = turnServers.any { it.isValid() }
}

