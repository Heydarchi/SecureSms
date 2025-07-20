package com.aospinsight.securesms.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

/**
 * BroadcastReceiver to handle incoming SMS messages
 */
class SmsReceiver : BroadcastReceiver() {
    
    private val TAG = "SmsReceiver"

    private var smsReceivedListener: OnSmsReceivedListener? = null

    fun setSmsReceivedListener(listener: OnSmsReceivedListener?) {
        smsReceivedListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            if (smsMessages != null && smsMessages.isNotEmpty()) {
                for (smsMessage in smsMessages) {
                    processSmsMessage(context, smsMessage)
                }
            }
        }
    }
    
    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        val phoneNumber = smsMessage.originatingAddress ?: "Unknown"
        val message = smsMessage.messageBody ?: ""
        val timestamp = smsMessage.timestampMillis
        
        Log.d(TAG, "SMS from: $phoneNumber, Message: $message")
        
        // Notify listener about the new SMS
        smsReceivedListener?.onSmsReceived(phoneNumber, message, timestamp)
        
        // You can add additional processing here such as:
        // - Saving to database
        // - Showing notification
        // - Filtering spam
        // - Encrypting message
        
        // Store the SMS using SmsManager
        try {
            val smsManager = SmsManager.getInstance(context)
            smsManager.saveReceivedSms(phoneNumber, message, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving SMS: ${e.message}")
        }
    }
}
