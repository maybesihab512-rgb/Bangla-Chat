package com.example.data.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.CipherAppContainer

class CallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ACCEPT_CALL = "com.example.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.example.ACTION_DECLINE_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
        private const val TAG = "CallActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val callId = intent.getStringExtra(EXTRA_CALL_ID)
        Log.i(TAG, "Call action received: $action for callId=$callId")

        when (action) {
            ACTION_DECLINE_CALL -> {
                try {
                    CipherAppContainer.callingService.declineIncomingCall()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decline call from notification", e)
                }
            }
            ACTION_ACCEPT_CALL -> {
                try {
                    CipherAppContainer.callingService.answerIncomingCall()
                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("CALL_ACTION", "ACCEPT_CALL")
                        putExtra("CALL_ID", callId)
                    }
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to accept call from notification", e)
                }
            }
        }
    }
}
