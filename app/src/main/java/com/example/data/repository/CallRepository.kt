package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRepository(
    private val context: Context? = null,
    private val authRepository: AuthRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private const val TAG = "CallRepository"
    }

    private val databaseId = try {
        context?.getString(R.string.firestore_database_id)
            ?: "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    } catch (e: Exception) {
        "ai-studio-android-cipherli-72ccaa4b-6463-443c-ac23-a8bf278af715"
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    private var callerListener: ListenerRegistration? = null
    private var receiverListener: ListenerRegistration? = null

    private val callerCalls = mutableMapOf<String, CallRecord>()
    private val receiverCalls = mutableMapOf<String, CallRecord>()
    private val optimisticCalls = mutableMapOf<String, CallRecord>()

    init {
        authRepository?.let { repo ->
            scope.launch {
                repo.currentUser.collect { user ->
                    if (user != null && user.id.isNotBlank()) {
                        attachFirestoreListeners(user.id)
                    } else {
                        detachFirestoreListeners()
                    }
                }
            }
        }
    }

    private fun attachFirestoreListeners(userId: String) {
        detachFirestoreListeners()
        try {
            // Listen for calls initiated by current user
            callerListener = db.collection("calls")
                .whereEqualTo("callerId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Caller call history listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents ?: return@addSnapshotListener
                    synchronized(callerCalls) {
                        callerCalls.clear()
                        for (doc in docs) {
                            val record = mapDocumentToCallRecord(doc.id, doc.data ?: emptyMap(), userId, isCaller = true)
                            if (record != null) {
                                callerCalls[record.id] = record
                            }
                        }
                    }
                    refreshCombinedHistory()
                }

            // Listen for calls received by current user
            receiverListener = db.collection("calls")
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Receiver call history listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents ?: return@addSnapshotListener
                    synchronized(receiverCalls) {
                        receiverCalls.clear()
                        for (doc in docs) {
                            val record = mapDocumentToCallRecord(doc.id, doc.data ?: emptyMap(), userId, isCaller = false)
                            if (record != null) {
                                receiverCalls[record.id] = record
                            }
                        }
                    }
                    refreshCombinedHistory()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach call history listeners", e)
        }
    }

    private fun detachFirestoreListeners() {
        callerListener?.remove()
        callerListener = null
        receiverListener?.remove()
        receiverListener = null
    }

    private fun refreshCombinedHistory() {
        val merged = mutableMapOf<String, CallRecord>()
        synchronized(callerCalls) { merged.putAll(callerCalls) }
        synchronized(receiverCalls) { merged.putAll(receiverCalls) }
        synchronized(optimisticCalls) { merged.putAll(optimisticCalls) }

        // Sort descending by timestamp
        val sortedList = merged.values.sortedByDescending { it.timestamp }
        _callHistory.value = sortedList
    }

    private fun mapDocumentToCallRecord(
        docId: String,
        data: Map<String, Any>,
        currentUserId: String,
        isCaller: Boolean
    ): CallRecord? {
        val status = data["status"] as? String ?: return null
        // Skip calls that are still actively ringing or being initiated unless ended
        val isFinal = status in listOf("ACCEPTED", "DECLINED", "MISSED", "ENDED", "CANCELLED", "EXPIRED", "BUSY")
        if (!isFinal) return null

        val callId = data["callId"] as? String ?: docId
        val callTypeStr = data["callType"] as? String ?: "AUDIO"
        val callType = if (callTypeStr == "VIDEO") CallType.VIDEO else CallType.AUDIO

        val durationSec = (data["durationSeconds"] as? Number)?.toInt() ?: 0
        val createdTimestamp = data["createdAt"] as? Timestamp
        val timeMs = createdTimestamp?.toDate()?.time ?: System.currentTimeMillis()

        val contactId: String
        val contactName: String
        val contactAvatar: String

        val direction: CallDirection
        val formattedDuration: String

        if (isCaller) {
            contactId = data["receiverId"] as? String ?: "unknown"
            contactName = data["receiverName"] as? String ?: "Contact"
            contactAvatar = data["receiverAvatar"] as? String ?: "C"
            direction = CallDirection.OUTGOING
            formattedDuration = when {
                status == "DECLINED" -> "Declined"
                status == "CANCELLED" -> "Cancelled"
                status == "BUSY" -> "Busy"
                durationSec > 0 -> formatDuration(durationSec)
                else -> "Unanswered"
            }
        } else {
            contactId = data["callerId"] as? String ?: "unknown"
            contactName = data["callerName"] as? String ?: "Contact"
            contactAvatar = data["callerAvatar"] as? String ?: "C"
            if (status in listOf("MISSED", "CANCELLED", "EXPIRED", "BUSY") || (durationSec == 0 && status != "ACCEPTED")) {
                direction = CallDirection.MISSED
                formattedDuration = if (status == "DECLINED") "Declined" else "Missed"
            } else if (status == "DECLINED") {
                direction = CallDirection.MISSED
                formattedDuration = "Declined"
            } else {
                direction = CallDirection.INCOMING
                formattedDuration = if (durationSec > 0) formatDuration(durationSec) else "Missed"
            }
        }

        return CallRecord(
            id = callId,
            contactId = contactId,
            contactName = contactName,
            contactAvatarInitials = contactAvatar,
            avatarColorHex = 0xFF00F0FF,
            callType = callType,
            direction = direction,
            timestamp = timeMs,
            formattedDate = formatTimestamp(timeMs),
            durationSeconds = durationSec,
            formattedDuration = formattedDuration,
            networkQualityRating = "HD Audio • 48kbps",
            wasAdaptiveFallbackTriggered = false
        )
    }

    private fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun formatTimestamp(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Today, ${timeFormat.format(Date(timeMs))}"
            }
            diff < 172800_000 -> {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Yesterday, ${timeFormat.format(Date(timeMs))}"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                dateFormat.format(Date(timeMs))
            }
        }
    }

    fun logCall(record: CallRecord) {
        synchronized(optimisticCalls) {
            optimisticCalls[record.id] = record
        }
        refreshCombinedHistory()
    }

    fun clearHistory() {
        synchronized(callerCalls) { callerCalls.clear() }
        synchronized(receiverCalls) { receiverCalls.clear() }
        synchronized(optimisticCalls) { optimisticCalls.clear() }
        _callHistory.value = emptyList()
    }
}
