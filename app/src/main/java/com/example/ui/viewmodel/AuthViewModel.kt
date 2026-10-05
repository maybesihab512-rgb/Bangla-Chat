package com.example.ui.viewmodel

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthStatus
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = authRepository.authStatus
    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    private val _profileUpdateError = MutableStateFlow<String?>(null)
    val profileUpdateError: StateFlow<String?> = _profileUpdateError.asStateFlow()

    fun attemptAutoSignIn(activity: Activity, onComplete: () -> Unit = {}) {
        authRepository.attemptAutoSignIn(activity, onComplete)
    }

    fun continueWithGoogle(activity: Activity, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            authRepository.continueWithGoogle(activity, onComplete, onError)
        }
    }

    fun requestPhoneOtp(
        activity: Activity? = null,
        phoneNumber: String,
        onCodeSent: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            authRepository.requestPhoneOtp(activity, phoneNumber, onCodeSent, onError)
        }
    }

    fun verifyOtp(
        code: String,
        phoneNumber: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {
        return authRepository.verifyOtp(code, phoneNumber, onSuccess, onError)
    }

    fun completeProfile(name: String, handle: String, status: String, phone: String = "", photoUrl: String = "") {
        authRepository.completeProfile(name, handle, status, phone, photoUrl)
    }

    fun updateProfile(
        displayName: String,
        phoneNumber: String,
        photoUri: Uri?,
        statusMessage: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            _profileUpdateError.value = null
            val result = authRepository.updateProfile(displayName, phoneNumber, photoUri, statusMessage)
            _isUpdatingProfile.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to update profile"
                _profileUpdateError.value = msg
                onError(msg)
            }
        }
    }

    fun hasValidSession(): Boolean {
        return authRepository.hasValidSession()
    }

    fun updatePresence(isOnline: Boolean) {
        authRepository.updatePresence(isOnline)
    }

    fun logOut(activity: Activity? = null) {
        authRepository.logOut(activity)
    }
}
