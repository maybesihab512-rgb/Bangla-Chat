package com.example.data.repository

import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallRepository {
    private val _callHistory = MutableStateFlow<List<CallRecord>>(
        listOf(
            CallRecord(
                id = "call_rec_1",
                contactId = "usr_001",
                contactName = "Elena Rostova",
                contactAvatarInitials = "ER",
                avatarColorHex = 0xFF00F0FF,
                callType = CallType.VIDEO,
                direction = CallDirection.INCOMING,
                timestamp = System.currentTimeMillis() - 7200000,
                formattedDate = "Today, 12:45",
                durationSeconds = 342,
                formattedDuration = "05:42",
                networkQualityRating = "Adaptive 720p • 48kbps Opus",
                wasAdaptiveFallbackTriggered = false
            ),
            CallRecord(
                id = "call_rec_2",
                contactId = "usr_002",
                contactName = "Marcus Vance",
                contactAvatarInitials = "MV",
                avatarColorHex = 0xFF00E699,
                callType = CallType.AUDIO,
                direction = CallDirection.OUTGOING,
                timestamp = System.currentTimeMillis() - 86400000,
                formattedDate = "Yesterday, 16:10",
                durationSeconds = 128,
                formattedDuration = "02:08",
                networkQualityRating = "HD Voice • 32kbps Opus",
                wasAdaptiveFallbackTriggered = false
            ),
            CallRecord(
                id = "call_rec_3",
                contactId = "usr_003",
                contactName = "Kaito Tanaka",
                contactAvatarInitials = "KT",
                avatarColorHex = 0xFFA855F7,
                callType = CallType.AUDIO,
                direction = CallDirection.MISSED,
                timestamp = System.currentTimeMillis() - 86400000 * 2,
                formattedDate = "Sep 29, 21:04",
                durationSeconds = 0,
                formattedDuration = "Missed",
                networkQualityRating = "N/A",
                wasAdaptiveFallbackTriggered = false
            ),
            CallRecord(
                id = "call_rec_4",
                contactId = "usr_004",
                contactName = "Sarah Connor",
                contactAvatarInitials = "SC",
                avatarColorHex = 0xFFFFB020,
                callType = CallType.AUDIO,
                direction = CallDirection.INCOMING,
                timestamp = System.currentTimeMillis() - 86400000 * 3,
                formattedDate = "Sep 28, 14:15",
                durationSeconds = 852,
                formattedDuration = "14:12",
                networkQualityRating = "Data Saver Mode • Audio Priority",
                wasAdaptiveFallbackTriggered = true
            )
        )
    )
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    fun logCall(record: CallRecord) {
        _callHistory.value = listOf(record) + _callHistory.value
    }

    fun clearHistory() {
        _callHistory.value = emptyList()
    }
}
