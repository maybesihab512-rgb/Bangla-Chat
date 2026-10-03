package com.example.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthStatus
import com.example.model.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authStatus: StateFlow<AuthStatus> = authRepository.authStatus
    val currentUser: StateFlow<User?> = authRepository.currentUser

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

    fun completeProfile(name: String, handle: String, status: String) {
        authRepository.completeProfile(name, handle, status)
    }

    fun logOut(activity: Activity? = null) {
        authRepository.logOut(activity)
    }
}
