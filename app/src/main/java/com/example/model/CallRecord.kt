package com.example.model

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallRecord(
    val id: String,
    val contactId: String,
    val contactName: String,
    val contactAvatarInitials: String,
    val avatarColorHex: Long = 0xFF00F0FF,
    val callType: CallType = CallType.AUDIO,
    val direction: CallDirection = CallDirection.OUTGOING,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = "Today, 14:22",
    val durationSeconds: Int = 0,
    val formattedDuration: String = "00:00",
    val networkQualityRating: String = "HD Audio (OPUS 32kbps)",
    val wasAdaptiveFallbackTriggered: Boolean = false
)
