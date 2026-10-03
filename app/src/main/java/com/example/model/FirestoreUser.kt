package com.example.model

import com.google.firebase.Timestamp

data class FirestoreUser(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val profilePhoto: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val onlineStatus: String = "online",
    val lastSeen: Timestamp? = null,
    val createdAt: Timestamp? = null
)
