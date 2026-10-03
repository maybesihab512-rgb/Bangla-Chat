package com.example.data

import android.content.Context
import com.example.data.calling.CallingService
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserRepository

object CipherAppContainer {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getContext(): Context {
        return appContext ?: error("CipherAppContainer must be initialized with Context before accessing repositories.")
    }

    val authRepository: AuthRepository by lazy { AuthRepository(getContext()) }
    val chatRepository: ChatRepository by lazy { ChatRepository(getContext(), authRepository) }
    val callRepository: CallRepository by lazy { CallRepository() }
    val userRepository: UserRepository by lazy { UserRepository(getContext(), authRepository) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository() }
    val callingService: CallingService by lazy { CallingService(getContext(), authRepository) }
}
