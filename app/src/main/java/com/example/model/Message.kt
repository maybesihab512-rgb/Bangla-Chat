package com.example.model

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}

enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    SEEN,
    FAILED
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val formattedTime: String,
    val type: MessageType = MessageType.TEXT,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
    val isOutgoing: Boolean = false,
    val replyToMessage: Message? = null,
    val mediaDurationSeconds: Int = 0,
    val mediaFileSize: String = "",
    val mediaFileName: String = "",
    val mediaUrl: String = "",
    val isDeleted: Boolean = false,
    val isEncrypted: Boolean = true,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
)
