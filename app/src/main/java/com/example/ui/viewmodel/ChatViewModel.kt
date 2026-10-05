package com.example.ui.viewmodel

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CipherAppContainer
import com.example.data.audio.AudioPlaybackManager
import com.example.data.audio.AudioRecordManager
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.model.Conversation
import com.example.model.Message
import com.example.model.MessageType
import com.example.model.User
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

enum class ChatFilter {
    ALL,
    DIRECT,
    GROUPS,
    UNREAD
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    val audioRecordManager: AudioRecordManager = CipherAppContainer.audioRecordManager,
    val audioPlaybackManager: AudioPlaybackManager = CipherAppContainer.audioPlaybackManager
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

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _isUploadingMedia = MutableStateFlow(false)
    val isUploadingMedia: StateFlow<Boolean> = _isUploadingMedia.asStateFlow()

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
        mediaDuration: Int = 0,
        mediaUrl: String = ""
    ) {
        if (content.isBlank() && type == MessageType.TEXT) return

        chatRepository.sendMessage(
            conversationId = conversationId,
            content = content,
            type = type,
            replyTo = _replyingTo.value,
            mediaFileName = mediaFileName,
            mediaFileSize = mediaFileSize,
            mediaDuration = mediaDuration,
            mediaUrl = mediaUrl
        )
        _replyingTo.value = null
        chatRepository.setTyping(conversationId, false)
    }

    fun uploadMedia(
        conversationId: String,
        uri: Uri,
        type: MessageType,
        fileName: String,
        fileSize: String,
        mimeType: String,
        caption: String = "",
        durationSeconds: Int = 0,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isUploadingMedia.value = true
            _uploadProgress.value = 0
            val result = chatRepository.uploadAndSendMedia(
                conversationId = conversationId,
                uri = uri,
                type = type,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                caption = caption,
                durationSeconds = durationSeconds,
                onProgress = { pct ->
                    _uploadProgress.value = pct
                }
            )
            _isUploadingMedia.value = false
            _uploadProgress.value = null
            result.onSuccess { url ->
                onSuccess(url)
            }.onFailure { err ->
                val errorMsg = err.localizedMessage ?: "Upload failed"
                onError(errorMsg)
            }
        }
    }

    fun startVoiceRecording(): Boolean {
        val result = audioRecordManager.startRecording()
        if (result.isSuccess) {
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
            return true
        } else {
            Log.e("ChatViewModel", "Could not start audio recording: ${result.exceptionOrNull()?.message}")
            return false
        }
    }

    fun stopVoiceRecordingAndSend(conversationId: String) {
        val timerDuration = _audioRecordingSeconds.value
        _isRecordingAudio.value = false
        _audioRecordingSeconds.value = 0

        val stopResult = audioRecordManager.stopRecording()
        if (stopResult.isSuccess) {
            val (file, recordedSec) = stopResult.getOrThrow()
            val duration = if (recordedSec > 0) recordedSec else timerDuration.coerceAtLeast(1)

            viewModelScope.launch {
                var finalMediaUrl = ""
                try {
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference.child("voice_messages/$conversationId/${file.name}")
                    storageRef.putFile(Uri.fromFile(file)).await()
                    finalMediaUrl = storageRef.downloadUrl.await().toString()
                    Log.i("ChatViewModel", "Uploaded voice recording to Firebase Storage: $finalMediaUrl")
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Firebase Storage upload error, falling back to data URL", e)
                    try {
                        withContext(Dispatchers.IO) {
                            val bytes = file.readBytes()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            finalMediaUrl = "data:audio/mp4;base64,$base64"
                        }
                    } catch (ex: Exception) {
                        Log.e("ChatViewModel", "Error reading audio bytes", ex)
                        finalMediaUrl = file.absolutePath
                    }
                }

                val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                sendMessage(
                    conversationId = conversationId,
                    content = "Voice memo (${duration}s)",
                    type = MessageType.AUDIO,
                    mediaFileName = file.name,
                    mediaFileSize = "$sizeKb KB (AAC)",
                    mediaDuration = duration,
                    mediaUrl = finalMediaUrl
                )
            }
        } else {
            Log.w("ChatViewModel", "Audio recording failed or file was empty: ${stopResult.exceptionOrNull()?.message}")
        }
    }

    fun cancelVoiceRecording() {
        _isRecordingAudio.value = false
        _audioRecordingSeconds.value = 0
        audioRecordManager.cancelRecording()
    }

    fun playAudio(messageId: String, mediaUrl: String) {
        audioPlaybackManager.playOrToggle(messageId, mediaUrl)
    }

    fun stopAudio() {
        audioPlaybackManager.stop()
    }

    fun deleteMessage(conversationId: String, messageId: String, forEveryone: Boolean) {
        chatRepository.deleteMessage(conversationId, messageId, forEveryone)
    }

    fun startChatWithContact(user: User): String {
        return chatRepository.createConversationWithUser(user)
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.release()
        audioPlaybackManager.release()
    }
}
