package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DraftManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cipherlink_drafts_v1", Context.MODE_PRIVATE)

    private val _draftsFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val draftsFlow: StateFlow<Map<String, String>> = _draftsFlow.asStateFlow()

    init {
        loadAllDrafts()
    }

    private fun draftKey(userId: String, conversationId: String): String =
        "draft_${userId}_${conversationId}"

    fun saveDraft(userId: String, conversationId: String, draftText: String) {
        if (userId.isBlank() || conversationId.isBlank()) return
        val key = draftKey(userId, conversationId)
        val clean = draftText.trim()
        if (clean.isEmpty()) {
            clearDraft(userId, conversationId)
        } else {
            prefs.edit().putString(key, draftText).apply()
            val current = _draftsFlow.value.toMutableMap()
            current[conversationId] = draftText
            _draftsFlow.value = current
        }
    }

    fun getDraft(userId: String, conversationId: String): String {
        if (userId.isBlank() || conversationId.isBlank()) return ""
        val key = draftKey(userId, conversationId)
        return prefs.getString(key, "") ?: ""
    }

    fun clearDraft(userId: String, conversationId: String) {
        if (userId.isBlank() || conversationId.isBlank()) return
        val key = draftKey(userId, conversationId)
        prefs.edit().remove(key).apply()
        val current = _draftsFlow.value.toMutableMap()
        current.remove(conversationId)
        _draftsFlow.value = current
    }

    private fun loadAllDrafts() {
        val map = mutableMapOf<String, String>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("draft_") && value is String && value.isNotBlank()) {
                val parts = key.split("_")
                if (parts.size >= 3) {
                    val convId = key.substringAfter("${parts[1]}_")
                    map[convId] = value
                }
            }
        }
        _draftsFlow.value = map
    }
}
