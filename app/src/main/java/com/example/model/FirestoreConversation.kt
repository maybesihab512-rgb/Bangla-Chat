package com.example.model

import com.google.firebase.Timestamp

data class FirestoreConversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastMessageTime: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
