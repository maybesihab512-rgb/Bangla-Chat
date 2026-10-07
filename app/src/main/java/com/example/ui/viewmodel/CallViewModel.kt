package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calling.ActiveCallSession
import com.example.data.calling.CallingService
import com.example.data.repository.CallRepository
import com.example.data.repository.UserRepository
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.NetworkQualityMetrics
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CallFilter {
    ALL,
    MISSED
}

class CallViewModel(
    private val callRepository: CallRepository,
    private val callingService: CallingService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    val selectedFilter: StateFlow<CallFilter> = _selectedFilter.asStateFlow()

    val filteredCalls: StateFlow<List<CallRecord>> = combine(
        callRepository.callHistory,
        _selectedFilter
    ) { calls, filter ->
        when (filter) {
            CallFilter.ALL -> calls
            CallFilter.MISSED -> calls.filter { it.direction == CallDirection.MISSED }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeSession: StateFlow<ActiveCallSession?> = callingService.currentSession
    val networkMetrics: StateFlow<NetworkQualityMetrics> = callingService.networkMetrics
    val hasRemoteVideo: StateFlow<Boolean> = callingService.hasRemoteVideo

    fun setFilter(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun startCall(contact: User, type: CallType) {
        callingService.startCall(
            contactId = contact.id,
            contactName = contact.name,
            contactAvatarInitials = contact.avatarInitials,
            avatarColorHex = contact.avatarColorHex,
            type = type
        )
    }

    fun startCallWithDetails(
        contactId: String,
        name: String,
        avatarInitials: String,
        avatarColorHex: Long = 0xFF00F0FF,
        isVideo: Boolean
    ) {
        callingService.startCall(
            contactId = contactId,
            contactName = name,
            contactAvatarInitials = avatarInitials,
            avatarColorHex = avatarColorHex,
            type = if (isVideo) CallType.VIDEO else CallType.AUDIO
        )
    }

    fun startCallById(contactId: String, isVideo: Boolean) {
        val contact = userRepository.getContactById(contactId) ?: User(
            id = contactId,
            name = "Contact",
            handle = "user_$contactId",
            avatarInitials = "C"
        )
        startCall(contact, if (isVideo) CallType.VIDEO else CallType.AUDIO)
    }

    fun attachLocalVideoRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        callingService.attachLocalVideoRenderer(renderer)
    }

    fun detachLocalVideoRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        callingService.detachLocalVideoRenderer(renderer)
    }

    fun attachRemoteVideoRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        callingService.attachRemoteVideoRenderer(renderer)
    }

    fun detachRemoteVideoRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        callingService.detachRemoteVideoRenderer(renderer)
    }

    fun answerCall() {
        callingService.answerIncomingCall()
    }

    fun declineCall() {
        val record = callingService.declineIncomingCall()
        if (record != null) {
            callRepository.logCall(record)
        }
    }

    fun toggleMic() {
        callingService.toggleMic()
    }

    fun toggleVideo() {
        callingService.toggleVideo()
    }

    fun toggleSpeaker() {
        callingService.toggleSpeaker()
    }

    fun switchCamera() {
        callingService.switchCamera()
    }

    fun toggleAudioPriorityMode(enabled: Boolean) {
        callingService.setAudioPriorityMode(enabled)
    }

    fun endCall() {
        val record = callingService.endCall()
        if (record != null) {
            callRepository.logCall(record)
        }
    }
}
