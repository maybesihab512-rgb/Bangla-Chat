package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.repository.SecuritySettings
import com.example.data.repository.SettingsRepository
import com.example.model.CallingConfig
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val callingConfig: StateFlow<CallingConfig> = settingsRepository.callingConfig
    val securitySettings: StateFlow<SecuritySettings> = settingsRepository.securitySettings

    fun toggleAdaptiveBitrate(enabled: Boolean) {
        settingsRepository.updateAdaptiveBitrate(enabled)
    }

    fun toggleWeakNetworkAudioPriority(enabled: Boolean) {
        settingsRepository.updateWeakNetworkAudioPriority(enabled)
    }

    fun toggleDataSavingMode(enabled: Boolean) {
        settingsRepository.updateDataSavingMode(enabled)
    }

    fun toggleEchoCancellation(enabled: Boolean) {
        settingsRepository.updateEchoCancellation(enabled)
    }

    fun toggleNoiseSuppression(enabled: Boolean) {
        settingsRepository.updateNoiseSuppression(enabled)
    }

    fun toggleBiometricLock(enabled: Boolean) {
        settingsRepository.updateBiometricLock(enabled)
    }
}
