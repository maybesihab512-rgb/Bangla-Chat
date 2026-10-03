package com.example.data.calling

import com.example.model.CallingConfig
import com.example.model.NetworkQualityMetrics
import com.example.model.NetworkTier

/**
 * Calculates adaptive audio/video bitrates and automatic resolution scaling
 * based on packet loss, RTT, and bandwidth throttling on weak networks.
 */
class AdaptiveBitrateController(
    private var config: CallingConfig = CallingConfig()
) {
    fun updateConfig(newConfig: CallingConfig) {
        this.config = newConfig
    }

    fun computeAdaptiveMetrics(
        rttMs: Int,
        packetLossRatio: Float,
        currentAudioBitrate: Int,
        currentVideoBitrate: Int
    ): NetworkQualityMetrics {
        val tier = when {
            packetLossRatio > 0.15f || rttMs > 400 -> NetworkTier.CRITICAL_WEAK
            packetLossRatio > 0.05f || rttMs > 200 -> NetworkTier.CONGESTED
            packetLossRatio > 0.02f || rttMs > 100 -> NetworkTier.GOOD
            else -> NetworkTier.EXCELLENT
        }

        val bars = when (tier) {
            NetworkTier.EXCELLENT -> 4
            NetworkTier.GOOD -> 3
            NetworkTier.CONGESTED -> 2
            NetworkTier.CRITICAL_WEAK -> 1
        }

        // Automatic audio-priority fallback when network is congested or critically weak
        val shouldPrioritizeAudio = (tier == NetworkTier.CRITICAL_WEAK || tier == NetworkTier.CONGESTED) &&
                config.weakNetworkAudioPriorityEnabled

        val targetAudioKbps = when (tier) {
            NetworkTier.EXCELLENT -> 64
            NetworkTier.GOOD -> 48
            NetworkTier.CONGESTED -> 24
            NetworkTier.CRITICAL_WEAK -> 12 // OPUS narrow-band mode with FEC
        }

        val targetVideoKbps = when {
            shouldPrioritizeAudio -> 0 // Video stream paused to guarantee uninterrupted voice
            tier == NetworkTier.EXCELLENT -> if (config.dataSavingModeEnabled) 800 else 1800
            tier == NetworkTier.GOOD -> if (config.dataSavingModeEnabled) 400 else 900
            tier == NetworkTier.CONGESTED -> 250
            else -> 0
        }

        val targetResolution = when {
            shouldPrioritizeAudio -> "Audio Only (Priority Mode)"
            tier == NetworkTier.EXCELLENT -> if (config.dataSavingModeEnabled) "854x480@30fps" else "1280x720@30fps"
            tier == NetworkTier.GOOD -> "640x360@30fps"
            tier == NetworkTier.CONGESTED -> "426x240@15fps"
            else -> "Suspended"
        }

        return NetworkQualityMetrics(
            bars = bars,
            tier = tier,
            rttMs = rttMs,
            packetLossPercent = packetLossRatio * 100f,
            jitterMs = (rttMs / 12).coerceAtLeast(2),
            currentAudioBitrateKbps = targetAudioKbps,
            currentVideoBitrateKbps = targetVideoKbps,
            currentResolution = targetResolution,
            isAudioPriorityActive = shouldPrioritizeAudio,
            isDataSaverActive = config.dataSavingModeEnabled,
            interfaceType = if (rttMs > 250) "Cellular (Low RSSI)" else "Wi-Fi (5GHz)"
        )
    }
}
