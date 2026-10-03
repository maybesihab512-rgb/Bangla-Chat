package com.example.data.calling

import com.example.model.CallingConfig
import com.example.model.NetworkQualityMetrics
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architectural abstraction for future WebRTC / VoIP real-time media engine.
 * Designed for adaptive bitrates, forward error correction (FEC), DTLS-SRTP encryption,
 * and seamless network handoffs.
 */
interface WebRtcEngineInterface {
    val networkMetrics: StateFlow<NetworkQualityMetrics>
    
    suspend fun initialize(config: CallingConfig): Result<Unit>
    suspend fun createOffer(isVideo: Boolean): Result<String>
    suspend fun handleAnswer(remoteSdp: String): Result<Unit>
    suspend fun addIceCandidate(candidateJson: String): Result<Unit>
    
    fun setLocalAudioEnabled(enabled: Boolean)
    fun setLocalVideoEnabled(enabled: Boolean)
    fun switchCamera()
    fun setSpeakerphoneOn(on: Boolean)
    
    fun updateCallingConfig(config: CallingConfig)
    fun setAudioPriorityMode(enabled: Boolean)
    
    suspend fun terminateSession()
}
