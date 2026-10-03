package com.example.model

import com.google.firebase.Timestamp

data class FirestoreMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val messageType: String = "text",
    val status: String = "sent",
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToContent: String = "",
    val mediaFileName: String = "",
    val mediaFileSize: String = "",
    val mediaDuration: Int = 0,
    val createdAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val seenAt: Timestamp? = null
)
