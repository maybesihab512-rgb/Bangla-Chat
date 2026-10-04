package com.example.data.repository

import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CallRepository {
    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    fun logCall(record: CallRecord) {
        _callHistory.value = listOf(record) + _callHistory.value
    }

    fun clearHistory() {
        _callHistory.value = emptyList()
    }
}
