package com.aospinsight.securesms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.sms.OnSmsReceivedListener
import com.aospinsight.securesms.sms.SmsManager
import com.aospinsight.securesms.sms.SmsReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class SmsManagerService : Service(), OnSmsReceivedListener {
    
    private companion object {
        const val TAG = "SmsManagerService"
    }
    
    private lateinit var smsManager: SmsManager
    private val binder = SmsManagerBinder(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val smsReceiver = SmsReceiver()

    // List to hold SMS update listeners
    private val smsListeners = CopyOnWriteArrayList<SmsUpdateListener>()
    
    // Cache for conversations
    private var cachedConversations: List<SmsConversation> = emptyList()

    override fun onCreate() {
        super.onCreate()
        smsManager = SmsManager.getInstance(this)
        
        // Set SMS received listener
        smsReceiver.setSmsReceivedListener(this)
        
        // Initial load of SMS data
        serviceScope.launch {
            refreshSmsData()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Service unbound")
        return super.onUnbind(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        smsReceiver.setSmsReceivedListener(null)
        smsListeners.clear()
    }

    override fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        Log.d(TAG, "New SMS received from $phoneNumber")
        
        // Refresh data when new SMS is received
        serviceScope.launch {
            refreshSmsData()
            
            // Notify all listeners
            smsListeners.forEach { listener ->
                try {
                    listener.onNewSmsReceived(phoneNumber, message, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying SMS listener", e)
                }
            }
        }
    }
    
    // Implementation of ISmsManagerService methods - delegate to SmsManager
    
    suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return try {
            // Always get fresh data from SmsManager
            val conversations = smsManager.getLatestMessagesFromEachContact()
            cachedConversations = conversations
            conversations
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest messages from each contact", e)
            emptyList()
        }
    }
    
    suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return try {
            smsManager.getMessagesFromContact(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages from contact $phoneNumber", e)
            emptyList()
        }
    }
    
    suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return try {
            smsManager.getConversation(phoneNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting conversation for $phoneNumber", e)
            null
        }
    }
    
    suspend fun refreshSmsData() {
        try {
            Log.d(TAG, "Refreshing SMS data")
            cachedConversations = smsManager.refreshSmsData()
            
            // Notify listeners about data refresh
            smsListeners.forEach { listener ->
                try {
                    listener.onSmsDataRefreshed()
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying data refresh to listener", e)
                }
            }
            
            Log.d(TAG, "SMS data refreshed, found ${cachedConversations.size} conversations")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing SMS data", e)
        }
    }
    
    fun registerSmsListener(listener: SmsUpdateListener) {
        if (!smsListeners.contains(listener)) {
            smsListeners.add(listener)
            Log.d(TAG, "Registered SMS listener, total: ${smsListeners.size}")
        }
    }
    
    fun unregisterSmsListener(listener: SmsUpdateListener) {
        if (smsListeners.remove(listener)) {
            Log.d(TAG, "Unregistered SMS listener, total: ${smsListeners.size}")
        }
    }
}