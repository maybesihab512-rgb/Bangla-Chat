package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.model.Conversation
import com.example.model.Message
import com.example.model.MessageType
import com.example.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatFilter {
    ALL,
    DIRECT,
    GROUPS,
    UNREAD
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    val filteredConversations: StateFlow<List<Conversation>> = combine(
        chatRepository.conversations,
        _searchQuery,
        _selectedFilter
    ) { convs, query, filter ->
        convs.filter { conv ->
            val matchesFilter = when (filter) {
                ChatFilter.ALL -> true
                ChatFilter.DIRECT -> !conv.isGroup
                ChatFilter.GROUPS -> conv.isGroup
                ChatFilter.UNREAD -> conv.unreadCount > 0
            }
            val matchesQuery = query.isBlank() ||
                    conv.title.contains(query, ignoreCase = true) ||
                    conv.lastMessage?.content?.contains(query, ignoreCase = true) == true
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User Directory for New Chat
    val availablePeers: StateFlow<List<User>> = userRepository.contacts

    // Targeted Real-time User Search state
    private val _peerSearchResults = MutableStateFlow<List<User>>(emptyList())
    val peerSearchResults: StateFlow<List<User>> = _peerSearchResults.asStateFlow()

    private val _isSearchingPeers = MutableStateFlow(false)
    val isSearchingPeers: StateFlow<Boolean> = _isSearchingPeers.asStateFlow()

    private var searchJob: Job? = null

    // Active conversation state
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _audioRecordingSeconds = MutableStateFlow(0)
    val audioRecordingSeconds: StateFlow<Int> = _audioRecordingSeconds.asStateFlow()

    // Realtime message state cache per conversation
    private val cachedMessageFlows = mutableMapOf<String, StateFlow<List<Message>>>()
    private val cachedTypingFlows = mutableMapOf<String, StateFlow<Boolean>>()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
    }

    fun searchPeers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _peerSearchResults.value = availablePeers.value
            _isSearchingPeers.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearchingPeers.value = true
            delay(250) // Debounce typing
            val results = userRepository.searchUsers(query)
            _peerSearchResults.value = results
            _isSearchingPeers.value = false
        }
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
        chatRepository.setActiveConversation(id)
        chatRepository.listenToConversationMessages(id)
    }

    fun clearActiveConversation() {
        _activeConversationId.value = null
        chatRepository.setActiveConversation(null)
    }

    fun getConversation(id: String): Conversation? = chatRepository.getConversationById(id)

    fun getMessages(conversationId: String): StateFlow<List<Message>> {
        return cachedMessageFlows.getOrPut(conversationId) {
            val flow = MutableStateFlow<List<Message>>(chatRepository.getMessagesForConversation(conversationId))
            viewModelScope.launch {
                chatRepository.messagesMap.collect { map ->
                    flow.value = map[conversationId] ?: emptyList()
                }
            }
            flow
        }
    }

    val currentUserId: String
        get() = chatRepository.currentUserId

    fun observeTypingForConversation(conversationId: String): StateFlow<Boolean> {
        val conv = getConversation(conversationId)
        val peerId = conv?.participantIds?.firstOrNull { it != currentUserId }
            ?: if (conversationId.startsWith("conv_")) {
                conversationId.removePrefix("conv_").split("_").firstOrNull { it != currentUserId } ?: ""
            } else ""
        return observePeerTyping(conversationId, peerId)
    }

    fun observePeerTyping(conversationId: String, peerId: String): StateFlow<Boolean> {
        val key = "${conversationId}_$peerId"
        return cachedTypingFlows.getOrPut(key) {
            chatRepository.observePeerTyping(conversationId, peerId).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
        }
    }

    fun setTyping(conversationId: String, isTyping: Boolean) {
        chatRepository.setTyping(conversationId, isTyping)
    }

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaFileName: String = "",
        mediaFileSize: String = "",
        mediaDuration: Int = 0
    ) {
        if (content.isBlank() && type == MessageType.TEXT) return

        chatRepository.sendMessage(
            conversationId = conversationId,
            content = content,
            type = type,
            replyTo = _replyingTo.value,
            mediaFileName = mediaFileName,
            mediaFileSize = mediaFileSize,
            mediaDuration = mediaDuration
        )
        _replyingTo.value = null
        chatRepository.setTyping(conversationId, false)
    }

    fun startVoiceRecording() {
        _isRecordingAudio.value = true
        _audioRecordingSeconds.value = 0
        viewModelScope.launch {
            while (_isRecordingAudio.value) {
                delay(1000)
                if (_isRecordingAudio.value) {
                    _audioRecordingSeconds.value += 1
                }
            }
        }
    }

    fun stopVoiceRecordingAndSend(conversationId: String) {
        val duration = _audioRecordingSeconds.value
        _isRecordingAudio.value = false
        if (duration > 0) {
            sendMessage(
                conversationId = conversationId,
                content = "Encrypted voice memo ($duration sec)",
                type = MessageType.AUDIO,
                mediaDuration = duration,
                mediaFileSize = "${duration * 12} KB (Opus adaptive)"
            )
        }
        _audioRecordingSeconds.value = 0
    }

    fun cancelVoiceRecording() {
        _isRecordingAudio.value = false
        _audioRecordingSeconds.value = 0
    }

    fun deleteMessage(conversationId: String, messageId: String, forEveryone: Boolean) {
        chatRepository.deleteMessage(conversationId, messageId, forEveryone)
    }

    fun startChatWithContact(user: User): String {
        return chatRepository.createConversationWithUser(user)
    }
}
