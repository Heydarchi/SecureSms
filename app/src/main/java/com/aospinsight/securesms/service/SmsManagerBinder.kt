package com.aospinsight.securesms.service

import android.os.Binder
import android.util.Log
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.repository.SmsRepository
import java.util.concurrent.CopyOnWriteArrayList


class SmsManagerBinder(
    private val smsRepository: SmsRepository) : Binder(), ISmsManagerService {
    
    companion object {
        private const val TAG = "SmsManagerBinder"
    }
    
    // List to hold SMS update listeners
    private val smsListeners = CopyOnWriteArrayList<SmsUpdateListener>()
    
    override suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return try {
            smsRepository.getAllConversations()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest messages from each contact", e)
            emptyList()
        }
    }
    
    override suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return try {
            smsRepository.getMessagesForPhoneNumber(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages from contact $phoneNumber", e)
            emptyList()
        }
    }
    
    override suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return try {
            smsRepository.getConversation(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversation for $phoneNumber", e)
            null
        }
    }
    
    override suspend fun refreshSmsData() {
        try {
            smsRepository.refresh()
            // Notify listeners about data refresh
            notifyDataRefreshed()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing SMS data", e)
        }
    }
    
    override fun registerSmsListener(listener: SmsUpdateListener) {
        if (!smsListeners.contains(listener)) {
            smsListeners.add(listener)
            Log.d(TAG, "Registered SMS listener, total: ${smsListeners.size}")
        } else {
            Log.d(TAG, "Listener already registered")
        }
    }
    
    override fun unregisterSmsListener(listener: SmsUpdateListener) {
        if (smsListeners.remove(listener)) {
            Log.d(TAG, "Unregistered SMS listener, total: ${smsListeners.size}")
        } else {
            Log.d(TAG, "Listener was not registered")
        }
    }
    
    /**
     * Notify listeners about new SMS received
     */
    fun notifyNewSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        Log.d(TAG, "Notifying ${smsListeners.size} listeners about new SMS from $phoneNumber")
        smsListeners.forEach { listener ->
            try {
                listener.onNewSmsReceived(phoneNumber, message, timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying listener about new SMS", e)
            }
        }
    }
    
    /**
     * Notify listeners about data refresh
     */
    fun notifyDataRefreshed() {
        Log.d(TAG, "Notifying ${smsListeners.size} listeners about data refresh")
        smsListeners.forEach { listener ->
            try {
                listener.onSmsDataRefreshed()
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying listener about data refresh", e)
            }
        }
    }
}
