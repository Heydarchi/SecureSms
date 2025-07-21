package com.aospinsight.securesms.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.aospinsight.securesms.broadcastreceiver.ISmsReceivedListener


class SmsReceiver : BroadcastReceiver() {
    
    private val TAG = "SmsReceiver"

    private var smsReceivedListener: ISmsReceivedListener? = null

    fun setSmsReceivedListener(listener: ISmsReceivedListener?) {
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
        
        smsReceivedListener?.onSmsReceived(phoneNumber, message, timestamp)
        
        // - Saving to database
        // - Showing notification
        // - Filtering spam
        // - Encrypting message        
        // - Store the SMS using SmsManager

    }
}
