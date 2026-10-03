package com.example.data.repository

import com.example.model.CallingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SecuritySettings(
    val biometricLockEnabled: Boolean = false,
    val disappearingMessagesDefaultDays: Int = 0, // 0 = off
    val screenSecurityBlockScreenshots: Boolean = true,
    val doubleRatchetKeyRotation: Boolean = true
)

class SettingsRepository {
    private val _callingConfig = MutableStateFlow(CallingConfig())
    val callingConfig: StateFlow<CallingConfig> = _callingConfig.asStateFlow()

    private val _securitySettings = MutableStateFlow(SecuritySettings())
    val securitySettings: StateFlow<SecuritySettings> = _securitySettings.asStateFlow()

    fun updateAdaptiveBitrate(enabled: Boolean) {
        _callingConfig.value = _callingConfig.value.copy(adaptiveBitrateEnabled = enabled)
    }

    fun updateWeakNetworkAudioPriority(enabled: Boolean) {
        _callingConfig.value = _callingConfig.value.copy(weakNetworkAudioPriorityEnabled = enabled)
    }

    fun updateDataSavingMode(enabled: Boolean) {
        _callingConfig.value = _callingConfig.value.copy(dataSavingModeEnabled = enabled)
    }

    fun updateEchoCancellation(enabled: Boolean) {
        _callingConfig.value = _callingConfig.value.copy(echoCancellationEnabled = enabled)
    }

    fun updateNoiseSuppression(enabled: Boolean) {
        _callingConfig.value = _callingConfig.value.copy(noiseSuppressionEnabled = enabled)
    }

    fun updateBiometricLock(enabled: Boolean) {
        _securitySettings.value = _securitySettings.value.copy(biometricLockEnabled = enabled)
    }
}
