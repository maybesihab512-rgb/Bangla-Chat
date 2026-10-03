package com.example.model

data class User(
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    val phone: String = "",
    val email: String = "",
    val avatarInitials: String = "",
    val avatarColorHex: Long = 0xFF00F0FF,
    val statusMessage: String = "Available",
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val e2eeFingerprint: String = "9F82:B3A1:440E:71D9:2C58:EF03",
    val isVerifiedAccount: Boolean = true
)
