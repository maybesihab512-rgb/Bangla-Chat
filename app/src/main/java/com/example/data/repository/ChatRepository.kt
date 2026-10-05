package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.Conversation
import com.example.model.DeliveryStatus
import com.example.model.FirestoreConversation
import com.example.model.FirestoreMessage
import com.example.model.Message
import com.example.model.MessageType
import com.example.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PeerPresence(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeenText: String = "Offline",
    val photoUrl: String = ""
)

class ChatRepository(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val databaseId = try {
        context.getString(R.string.firestore_database_id)
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }
    private val db = FirebaseFirestore.getInstance(databaseId)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val currentUserId: String
        get() = authRepository.currentUser.value?.id ?: Firebase.auth.currentUser?.uid ?: ""

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messagesMap = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    val messagesMap: StateFlow<Map<String, List<Message>>> = _messagesMap.asStateFlow()

    private var activeConversationId: String? = null
    private var conversationsListener: ListenerRegistration? = null
    private val activeMessageListeners = mutableMapOf<String, ListenerRegistration>()
    private val activePresenceListeners = mutableMapOf<String, ListenerRegistration>()
    private val _peerPresenceMap = MutableStateFlow<Map<String, PeerPresence>>(emptyMap())
    val peerPresenceMap: StateFlow<Map<String, PeerPresence>> = _peerPresenceMap.asStateFlow()

    init {
        // Observe current user changes and attach real-time conversations listener only when authenticated
        scope.launch {
            authRepository.currentUser.collect { user ->
                val authUser = Firebase.auth.currentUser
                if (user != null && authUser != null && user.id == authUser.uid) {
                    attachConversationsListener(user.id)
                } else {
                    detachAllListeners()
                    _conversations.value = emptyList()
                    _messagesMap.value = emptyMap()
                }
            }
        }
    }

    fun setActiveConversation(convId: String?) {
        activeConversationId = convId
        if (convId != null) {
            markConversationAsRead(convId)
        }
    }

    private fun attachConversationsListener(userId: String) {
        conversationsListener?.remove()
        val authUser = Firebase.auth.currentUser
        if (authUser == null || authUser.uid != userId) {
            return
        }
        val path = "conversations"
        conversationsListener = db.collection(path)
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val fc = doc.toObject(FirestoreConversation::class.java)
                            fc?.let { mapFirestoreConversationToModel(it, userId) }
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error deserializing conversation doc", e)
                            null
                        }
                    }.sortedByDescending { it.lastMessage?.timestamp ?: 0L }

                    _conversations.value = list

                    // Pre-attach real-time message listeners for active conversations
                    list.forEach { conv ->
                        listenToConversationMessages(conv.id)
                    }
                }
            }
    }

    fun listenToConversationMessages(conversationId: String) {
        if (activeMessageListeners.containsKey(conversationId)) return

        val authUser = Firebase.auth.currentUser ?: return
        val currentUserId = authUser.uid
        val path = "conversations/$conversationId/messages"

        val registration = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val fm = doc.toObject(FirestoreMessage::class.java)
                            fm?.let { mapFirestoreMessageToModel(it, currentUserId) }
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error deserializing message doc", e)
                            null
                        }
                    }

                    _messagesMap.value = _messagesMap.value + (conversationId to messages)

                    // Auto-update conversation card preview, position and unread count
                    val currentConvs = _conversations.value
                    if (currentConvs.isNotEmpty()) {
                        val lastMsg = messages.lastOrNull()
                        val unread = if (activeConversationId == conversationId) 0
                        else messages.count { !it.isOutgoing && it.deliveryStatus != DeliveryStatus.SEEN }

                        val updated = currentConvs.map { conv ->
                            if (conv.id == conversationId) {
                                conv.copy(
                                    lastMessage = lastMsg ?: conv.lastMessage,
                                    unreadCount = unread
                                )
                            } else conv
                        }.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
                        _conversations.value = updated
                    }

                    // Auto-update message status for incoming messages:
                    // If conversation is actively opened by receiver -> mark SEEN
                    // Else if status is 'sent' -> mark DELIVERED
                    snapshot.documents.forEach { doc ->
                        val fm = doc.toObject(FirestoreMessage::class.java)
                        if (fm != null && fm.receiverId == currentUserId) {
                            if (activeConversationId == conversationId && fm.status != "seen") {
                                markMessageAsSeen(conversationId, fm.messageId)
                            } else if (fm.status == "sent") {
                                markMessageAsDelivered(conversationId, fm.messageId)
                            }
                        }
                    }
                }
            }

        activeMessageListeners[conversationId] = registration
    }

    private fun markMessageAsDelivered(conversationId: String, messageId: String) {
        if (Firebase.auth.currentUser == null) return
        scope.launch {
            try {
                db.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .update(
                        "status", "delivered",
                        "deliveredAt", FieldValue.serverTimestamp()
                    ).await()
            } catch (e: Exception) {
                // Ignore transient network errors
            }
        }
    }

    private fun markMessageAsSeen(conversationId: String, messageId: String) {
        if (Firebase.auth.currentUser == null) return
        scope.launch {
            try {
                db.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document(messageId)
                    .update(
                        "status", "seen",
                        "seenAt", FieldValue.serverTimestamp()
                    ).await()
            } catch (e: Exception) {
                // Ignore transient network errors
            }
        }
    }

    fun setTyping(conversationId: String, isTyping: Boolean) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val typingRef = db.collection("conversations")
                    .document(conversationId)
                    .collection("typing")
                    .document(currentUserId)

                if (isTyping) {
                    typingRef.set(
                        mapOf(
                            "userId" to currentUserId,
                            "isTyping" to true,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    typingRef.delete().await()
                }
            } catch (e: Exception) {
                // Transient error ignored
            }
        }
    }

    fun observePeerTyping(conversationId: String, peerId: String): Flow<Boolean> = callbackFlow {
        if (peerId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val ref = db.collection("conversations")
            .document(conversationId)
            .collection("typing")
            .document(peerId)

        val registration = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(false)
                return@addSnapshotListener
            }
            val isTyping = snapshot.getBoolean("isTyping") ?: false
            val timestamp = snapshot.getTimestamp("timestamp")
            val isFresh = if (timestamp != null) {
                (System.currentTimeMillis() - timestamp.toDate().time) < 8000
            } else true
            trySend(isTyping && isFresh)
        }

        awaitClose { registration.remove() }
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        replyTo: Message? = null,
        mediaDuration: Int = 0,
        mediaFileName: String = "",
        mediaFileSize: String = "",
        mediaUrl: String = ""
    ) {
        val currentUserId = authRepository.currentUser.value?.id ?: return
        val currentConv = _conversations.value.find { it.id == conversationId }
        val receiverId = currentConv?.participantIds?.firstOrNull { it != currentUserId } ?: ""

        val now = System.currentTimeMillis()
        val messageId = "msg_${now}_${(1000..9999).random()}"
        val formattedTime = timeFormat.format(Date(now))

        // 1. Optimistic local addition so user sees message instantly
        val optimisticMsg = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            senderName = "Me",
            content = content,
            timestamp = now,
            formattedTime = formattedTime,
            type = type,
            deliveryStatus = DeliveryStatus.SENDING,
            isOutgoing = true,
            replyToMessage = replyTo,
            mediaDurationSeconds = mediaDuration,
            mediaFileName = mediaFileName,
            mediaFileSize = mediaFileSize,
            mediaUrl = mediaUrl
        )

        val currentList = _messagesMap.value[conversationId] ?: emptyList()
        _messagesMap.value = _messagesMap.value + (conversationId to (currentList + optimisticMsg))

        // 2. Synchronize to Firestore backend only if authenticated
        val authUser = Firebase.auth.currentUser
        if (authUser != null && authUser.uid == currentUserId) {
            scope.launch {
                val path = "conversations/$conversationId/messages/$messageId"
                try {
                    val messageData = hashMapOf(
                        "messageId" to messageId,
                        "conversationId" to conversationId,
                        "senderId" to currentUserId,
                        "receiverId" to receiverId,
                        "messageText" to content,
                        "messageType" to type.name.lowercase(),
                        "status" to "sent",
                        "replyToMessageId" to (replyTo?.id ?: ""),
                        "replyToSenderName" to (replyTo?.senderName ?: ""),
                        "replyToContent" to (replyTo?.content ?: ""),
                        "mediaFileName" to mediaFileName,
                        "mediaFileSize" to mediaFileSize,
                        "mediaDuration" to mediaDuration,
                        "mediaUrl" to mediaUrl,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    // Write message doc
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(messageId)
                        .set(messageData)
                        .await()

                    // Update parent conversation doc with participantIds and conversationId
                    val participants = if (currentConv != null && currentConv.participantIds.isNotEmpty()) {
                        currentConv.participantIds
                    } else if (receiverId.isNotBlank()) {
                        listOf(currentUserId, receiverId)
                    } else {
                        listOf(currentUserId)
                    }

                    val convUpdate = hashMapOf<String, Any>(
                        "conversationId" to conversationId,
                        "participantIds" to participants,
                        "lastMessage" to content,
                        "lastSenderId" to currentUserId,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("conversations")
                        .document(conversationId)
                        .set(convUpdate, SetOptions.merge())
                        .await()

                    // Mark local message as sent
                    updateLocalMessageStatus(conversationId, messageId, DeliveryStatus.SENT)
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
        } else {
            updateLocalMessageStatus(conversationId, messageId, DeliveryStatus.SENT)
        }
    }

    suspend fun uploadAndSendMedia(
        conversationId: String,
        uri: Uri,
        type: MessageType,
        fileName: String,
        fileSize: String,
        mimeType: String,
        caption: String = "",
        durationSeconds: Int = 0,
        onProgress: (Int) -> Unit = {}
    ): Result<String> {
        val currentUserId = authRepository.currentUser.value?.id ?: Firebase.auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val currentConv = _conversations.value.find { it.id == conversationId }
        val receiverId = currentConv?.participantIds?.firstOrNull { it != currentUserId } ?: ""

        val now = System.currentTimeMillis()
        val messageId = "msg_${now}_${(1000..9999).random()}"
        val formattedTime = timeFormat.format(Date(now))

        val displayContent = caption.ifBlank {
            when (type) {
                MessageType.IMAGE -> "Photo"
                MessageType.VIDEO -> "Video"
                MessageType.FILE -> fileName
                else -> "Media"
            }
        }

        // 1. Immediate optimistic message showing SENDING status and local URI preview
        val optimisticMsg = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            senderName = "Me",
            content = displayContent,
            timestamp = now,
            formattedTime = formattedTime,
            type = type,
            deliveryStatus = DeliveryStatus.SENDING,
            isOutgoing = true,
            mediaFileName = fileName,
            mediaFileSize = fileSize,
            mediaDurationSeconds = durationSeconds,
            mediaUrl = uri.toString()
        )

        val currentList = _messagesMap.value[conversationId] ?: emptyList()
        _messagesMap.value = _messagesMap.value + (conversationId to (currentList + optimisticMsg))

        return try {
            // 2. Upload file to Firebase Storage preserving full quality
            val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val storageRef = FirebaseStorage.getInstance().reference
                .child("chat_media")
                .child(conversationId)
                .child("${messageId}_$safeName")

            val metadata = StorageMetadata.Builder()
                .setContentType(
                    mimeType.ifBlank {
                        when (type) {
                            MessageType.IMAGE -> "image/jpeg"
                            MessageType.VIDEO -> "video/mp4"
                            MessageType.FILE -> "application/octet-stream"
                            else -> "application/octet-stream"
                        }
                    }
                )
                .build()

            val uploadTask = storageRef.putFile(uri, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val total = taskSnapshot.totalByteCount
                if (total > 0) {
                    val progress = ((100.0 * taskSnapshot.bytesTransferred) / total).toInt()
                    onProgress(progress)
                }
            }

            val uploadSnapshot = uploadTask.await()
            val downloadUrl = uploadSnapshot.storage.downloadUrl.await().toString()

            // 3. Write message document to Firestore
            val messageData = hashMapOf(
                "messageId" to messageId,
                "conversationId" to conversationId,
                "senderId" to currentUserId,
                "receiverId" to receiverId,
                "messageText" to displayContent,
                "messageType" to type.name.lowercase(),
                "status" to "sent",
                "replyToMessageId" to "",
                "replyToSenderName" to "",
                "replyToContent" to "",
                "mediaFileName" to fileName,
                "mediaFileSize" to fileSize,
                "mediaDuration" to durationSeconds,
                "mediaUrl" to downloadUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(messageData)
                .await()

            // 4. Update parent conversation metadata
            val summaryText = when (type) {
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VIDEO -> "🎥 Video"
                MessageType.FILE -> "📄 $fileName"
                else -> displayContent
            }
            val convUpdate = hashMapOf(
                "lastMessage" to summaryText,
                "lastSenderId" to currentUserId,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("conversations")
                .document(conversationId)
                .set(convUpdate, SetOptions.merge())
                .await()

            // 5. Update local message to SENT with the real downloadUrl
            val updatedList = (_messagesMap.value[conversationId] ?: emptyList()).map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        deliveryStatus = DeliveryStatus.SENT,
                        mediaUrl = downloadUrl
                    )
                } else msg
            }
            _messagesMap.value = _messagesMap.value + (conversationId to updatedList)

            onProgress(100)
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to upload and send media", e)
            Result.failure(e)
        }
    }

    fun observePeerPresence(peerId: String): StateFlow<PeerPresence> {
        if (peerId.isBlank()) return MutableStateFlow(PeerPresence())
        if (!activePresenceListeners.containsKey(peerId)) {
            val reg = db.collection("users").document(peerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val status = snapshot.getString("onlineStatus") ?: "offline"
                    val lastSeen = snapshot.getTimestamp("lastSeen")
                    val photo = snapshot.getString("profilePhoto") ?: ""
                    val now = System.currentTimeMillis()
                    val lastSeenMs = lastSeen?.toDate()?.time ?: 0L
                    val diffMs = now - lastSeenMs
                    val isOnline = (status == "online") && (lastSeenMs > 0 && diffMs < 75_000L)
                    val lastSeenText = formatLastSeen(lastSeen, isOnline)

                    val presence = PeerPresence(
                        userId = peerId,
                        isOnline = isOnline,
                        lastSeenText = lastSeenText,
                        photoUrl = photo
                    )
                    _peerPresenceMap.value = _peerPresenceMap.value + (peerId to presence)

                    // Also update in-memory conversations list so presence reflects in the list
                    _conversations.value = _conversations.value.map { conv ->
                        if (conv.participantIds.contains(peerId)) {
                            conv.copy(isOnline = isOnline)
                        } else conv
                    }
                }
            activePresenceListeners[peerId] = reg
        }
        val flow = MutableStateFlow(_peerPresenceMap.value[peerId] ?: PeerPresence(userId = peerId))
        scope.launch {
            _peerPresenceMap.collect { map ->
                map[peerId]?.let { flow.value = it }
            }
        }
        return flow.asStateFlow()
    }

    private fun formatLastSeen(timestamp: com.google.firebase.Timestamp?, isOnline: Boolean): String {
        if (isOnline) return "Online"
        if (timestamp == null) return "Offline"
        val diffMs = System.currentTimeMillis() - timestamp.toDate().time
        val minutes = diffMs / (1000 * 60)
        return when {
            minutes < 1 -> "Last seen just now"
            minutes < 60 -> "Last seen ${minutes}m ago"
            minutes < 1440 -> "Last seen ${minutes / 60}h ago"
            else -> "Last seen recently"
        }
    }

    private fun updateLocalMessageStatus(conversationId: String, msgId: String, status: DeliveryStatus) {
        val list = _messagesMap.value[conversationId] ?: return
        val updated = list.map { if (it.id == msgId) it.copy(deliveryStatus = status) else it }
        _messagesMap.value = _messagesMap.value + (conversationId to updated)
    }

    fun getMessagesForConversation(convId: String): List<Message> {
        listenToConversationMessages(convId)
        return _messagesMap.value[convId] ?: emptyList()
    }

    fun getConversationById(convId: String): Conversation? =
        _conversations.value.find { it.id == convId }

    fun markConversationAsRead(conversationId: String) {
        val currentUserId = authRepository.currentUser.value?.id ?: return
        val messages = _messagesMap.value[conversationId] ?: return
        messages.filter { !it.isOutgoing && it.deliveryStatus != DeliveryStatus.SEEN }.forEach { msg ->
            markMessageAsSeen(conversationId, msg.id)
        }
        val currentConvs = _conversations.value
        if (currentConvs.any { it.id == conversationId && it.unreadCount > 0 }) {
            _conversations.value = currentConvs.map {
                if (it.id == conversationId) it.copy(unreadCount = 0) else it
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String, forEveryone: Boolean) {
        scope.launch {
            if (forEveryone && Firebase.auth.currentUser != null) {
                try {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(messageId)
                        .update("messageText", "This message was deleted")
                        .await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
                }
            } else {
                val current = _messagesMap.value[conversationId] ?: return@launch
                _messagesMap.value = _messagesMap.value + (conversationId to current.filterNot { it.id == messageId })
            }
        }
    }

    /**
     * Finds or creates a real 1-to-1 conversation with a target user
     */
    fun createConversationWithUser(targetUser: User): String {
        val currentUser = authRepository.currentUser.value ?: return ""
        val existing = _conversations.value.find {
            !it.isGroup && it.participantIds.contains(targetUser.id) && it.participantIds.contains(currentUser.id)
        }
        if (existing != null) return existing.id

        // Canonical ID: sorted so both peers arrive at the exact same conversation
        val convId = if (currentUser.id < targetUser.id) "conv_${currentUser.id}_${targetUser.id}" else "conv_${targetUser.id}_${currentUser.id}"

        val newConv = Conversation(
            id = convId,
            title = targetUser.name,
            isGroup = false,
            participantIds = listOf(currentUser.id, targetUser.id),
            participantNames = listOf(currentUser.name, targetUser.name),
            lastMessage = null,
            unreadCount = 0,
            avatarInitials = targetUser.avatarInitials,
            avatarColorHex = targetUser.avatarColorHex,
            isOnline = targetUser.isOnline
        )
        _conversations.value = listOf(newConv) + _conversations.value

        val authUser = Firebase.auth.currentUser
        if (authUser != null && authUser.uid == currentUser.id) {
            val convData = hashMapOf(
                "conversationId" to convId,
                "participantIds" to listOf(currentUser.id, targetUser.id),
                "participantNames" to mapOf(
                    currentUser.id to currentUser.name,
                    targetUser.id to targetUser.name
                ),
                "participantPhotos" to mapOf(
                    currentUser.id to currentUser.avatarInitials,
                    targetUser.id to targetUser.avatarInitials
                ),
                "lastMessage" to "Encrypted line opened",
                "lastSenderId" to currentUser.id,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            scope.launch {
                try {
                    db.collection("conversations").document(convId).set(convData, SetOptions.merge()).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, "conversations/$convId")
                }
            }
        }

        listenToConversationMessages(convId)
        return convId
    }

    private fun mapFirestoreConversationToModel(fc: FirestoreConversation, currentUserId: String): Conversation {
        val peerId = fc.participantIds.firstOrNull { it != currentUserId } ?: currentUserId
        val peerName = fc.participantNames[peerId] ?: "Contact"
        val peerPhoto = fc.participantPhotos[peerId] ?: peerName.take(2).uppercase()

        val timeMs = fc.lastMessageTime?.toDate()?.time ?: System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(timeMs))

        val lastMsg = if (fc.lastMessage.isNotBlank()) {
            Message(
                id = "last_${fc.conversationId}",
                conversationId = fc.conversationId,
                senderId = fc.lastSenderId,
                senderName = if (fc.lastSenderId == currentUserId) "Me" else peerName,
                content = fc.lastMessage,
                timestamp = timeMs,
                formattedTime = formattedTime,
                isOutgoing = fc.lastSenderId == currentUserId,
                deliveryStatus = DeliveryStatus.DELIVERED
            )
        } else null

        // Accurate unread count: count incoming unread messages in memory if available, or 1 if lastSenderId != currentUserId
        val convMessages = _messagesMap.value[fc.conversationId]
        val unread = if (convMessages != null) {
            convMessages.count { !it.isOutgoing && it.deliveryStatus != DeliveryStatus.SEEN }
        } else if (fc.lastSenderId.isNotBlank() && fc.lastSenderId != currentUserId) 1 else 0

        return Conversation(
            id = fc.conversationId,
            title = peerName,
            isGroup = fc.participantIds.size > 2,
            participantIds = fc.participantIds,
            participantNames = fc.participantNames.values.toList(),
            lastMessage = lastMsg,
            unreadCount = unread,
            avatarInitials = peerPhoto,
            avatarColorHex = if (peerId.hashCode() % 2 == 0) 0xFF00F0FF else 0xFF00E699,
            isOnline = true
        )
    }

    private fun mapFirestoreMessageToModel(fm: FirestoreMessage, currentUserId: String): Message {
        val timeMs = fm.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(timeMs))
        val isOutgoing = fm.senderId == currentUserId

        val deliveryStatus = when (fm.status.lowercase()) {
            "seen" -> DeliveryStatus.SEEN
            "delivered" -> DeliveryStatus.DELIVERED
            "sending" -> DeliveryStatus.SENDING
            else -> DeliveryStatus.SENT
        }

        val msgType = when (fm.messageType.lowercase()) {
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "file" -> MessageType.FILE
            "voice", "audio" -> MessageType.AUDIO
            else -> MessageType.TEXT
        }

        val replyMsg = if (fm.replyToMessageId.isNotBlank()) {
            Message(
                id = fm.replyToMessageId,
                conversationId = fm.conversationId,
                senderId = "",
                senderName = fm.replyToSenderName.ifBlank { "Peer" },
                content = fm.replyToContent,
                timestamp = 0L,
                formattedTime = "",
                type = MessageType.TEXT,
                deliveryStatus = DeliveryStatus.SEEN
            )
        } else null

        return Message(
            id = fm.messageId,
            conversationId = fm.conversationId,
            senderId = fm.senderId,
            senderName = if (isOutgoing) "Me" else "Peer",
            content = fm.messageText,
            timestamp = timeMs,
            formattedTime = formattedTime,
            type = msgType,
            deliveryStatus = deliveryStatus,
            isOutgoing = isOutgoing,
            replyToMessage = replyMsg,
            mediaFileName = fm.mediaFileName,
            mediaFileSize = fm.mediaFileSize,
            mediaDurationSeconds = fm.mediaDuration,
            mediaUrl = fm.mediaUrl,
            isDeleted = fm.messageText == "This message was deleted"
        )
    }

    private fun detachAllListeners() {
        conversationsListener?.remove()
        conversationsListener = null
        activeMessageListeners.values.forEach { it.remove() }
        activeMessageListeners.clear()
    }
}
