package com.example.model

enum class ConnectionState {
    IDLE,
    CONNECTING,
    SECURE_HANDSHAKE,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED
}

enum class NetworkTier {
    EXCELLENT, // Full 1080p60 / 64kbps Opus
    GOOD,      // 720p30 / 48kbps Opus
    CONGESTED, // 360p15 / 24kbps Opus + FEC
    CRITICAL_WEAK // Audio-only priority mode: 12kbps Opus
}

data class NetworkQualityMetrics(
    val bars: Int = 4, // 1 to 4
    val tier: NetworkTier = NetworkTier.EXCELLENT,
    val rttMs: Int = 28,
    val packetLossPercent: Float = 0.2f,
    val jitterMs: Int = 4,
    val currentAudioBitrateKbps: Int = 48,
    val currentVideoBitrateKbps: Int = 1200,
    val currentResolution: String = "1280x720@30fps",
    val isAudioPriorityActive: Boolean = false,
    val isDataSaverActive: Boolean = false,
    val interfaceType: String = "Wi-Fi (5GHz)"
)
