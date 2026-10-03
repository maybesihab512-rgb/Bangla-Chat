package com.example.model

data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val avatarInitials: String = "",
    val avatarColorHex: Long = 0xFF00F0FF,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isOnline: Boolean = false,
    val typingUser: String? = null,
    val e2eeProtocol: String = "End-to-End Encrypted"
)
