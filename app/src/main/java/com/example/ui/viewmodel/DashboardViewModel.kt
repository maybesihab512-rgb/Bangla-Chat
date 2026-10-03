package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calling.CallingService
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.model.CallRecord
import com.example.model.Conversation
import com.example.model.NetworkQualityMetrics
import com.example.model.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val user: User? = null,
    val recentConversations: List<Conversation> = emptyList(),
    val recentCalls: List<CallRecord> = emptyList(),
    val totalUnreadMessages: Int = 0,
    val missedCallsCount: Int = 0,
    val onlineContactsCount: Int = 0,
    val networkMetrics: NetworkQualityMetrics = NetworkQualityMetrics(),
    val isE2eeActive: Boolean = true
)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    private val callingService: CallingService
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.currentUser,
        chatRepository.conversations,
        callRepository.callHistory,
        userRepository.contacts,
        callingService.networkMetrics
    ) { user, convs, calls, contacts, metrics ->
        val totalUnread = convs.sumOf { it.unreadCount }
        val missed = calls.count { it.direction == com.example.model.CallDirection.MISSED }
        val online = contacts.count { it.isOnline }

        DashboardUiState(
            user = user,
            recentConversations = convs.take(5),
            recentCalls = calls.take(4),
            totalUnreadMessages = totalUnread,
            missedCallsCount = missed,
            onlineContactsCount = online,
            networkMetrics = metrics,
            isE2eeActive = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
