package com.aospinsight.securesms.service

import android.os.Binder
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.sms.SmsManager
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Binder class that provides access to SMS functionality through SmsManager
 */
class SmsManagerBinder(
    private val smsManager: SmsManager) : Binder(), ISmsManagerService {
    
    // List to hold SMS update listeners
    private val smsListeners = CopyOnWriteArrayList<SmsUpdateListener>()
    
    override suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return smsManager.getLatestMessagesFromEachContact()
    }
    
    override suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return smsManager.getMessagesFromContact(phoneNumber)
    }
    
    override suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return smsManager.getConversation(phoneNumber)
    }
    
    override suspend fun refreshSmsData() {
        smsManager.refreshSmsData()
        // Notify listeners about data refresh
        notifyDataRefreshed()
    }
    
    override fun registerSmsListener(listener: SmsUpdateListener) {
        if (!smsListeners.contains(listener)) {
            smsListeners.add(listener)
        }
    }
    
    override fun unregisterSmsListener(listener: SmsUpdateListener) {
        smsListeners.remove(listener)
    }
    
    /**
     * Notify listeners about new SMS received
     */
    fun notifyNewSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        smsListeners.forEach { listener ->
            try {
                listener.onNewSmsReceived(phoneNumber, message, timestamp)
            } catch (e: Exception) {
                // Log error but continue with other listeners
            }
        }
    }
    
    /**
     * Notify listeners about data refresh
     */
    fun notifyDataRefreshed() {
        smsListeners.forEach { listener ->
            try {
                listener.onSmsDataRefreshed()
            } catch (e: Exception) {
                // Log error but continue with other listeners
            }
        }
    }
}
