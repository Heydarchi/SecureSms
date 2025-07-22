package com.aospinsight.securesms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.broadcastreceiver.ISmsReceivedListener
import com.aospinsight.securesms.sms.SmsManager
import com.aospinsight.securesms.broadcastreceiver.SmsReceiver
import com.aospinsight.securesms.repository.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class SmsManagerService : Service(), ISmsReceivedListener {
    
    private companion object {
        const val TAG = "SmsManagerService"
    }

    private lateinit var smsRepository: SmsRepository
    private lateinit var smsManager: SmsManager
    private lateinit var binder: SmsManagerBinder
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val smsReceiver = SmsReceiver()

    // List to hold SMS update listeners (keeping for backward compatibility with service-level listeners)
    private val smsListeners = CopyOnWriteArrayList<SmsUpdateListener>()
    

    override fun onCreate() {
        super.onCreate()
        smsManager = SmsManager(this)
        smsRepository = SmsRepository(smsManager)
        binder = SmsManagerBinder(smsRepository)
        
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
        
        // Notify binder listeners about new SMS
        if (::binder.isInitialized) {
            binder.notifyNewSmsReceived(phoneNumber, message, timestamp)
        }
        
        // Refresh data when new SMS is received
        serviceScope.launch {
            refreshSmsData()
            
            // Notify service-level listeners (for backward compatibility)
            smsListeners.forEach { listener ->
                try {
                    listener.onNewSmsReceived(phoneNumber, message, timestamp)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying SMS listener", e)
                }
            }
            
            // Notify binder listeners about data refresh
            if (::binder.isInitialized) {
                binder.notifyDataRefreshed()
            }
        }
    }
    
    // Implementation of ISmsManagerService methods - delegate to binder (which uses SmsManager)
    
    suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return if (::binder.isInitialized) {
            binder.getLatestMessagesFromEachContact()
        } else {
            Log.w(TAG, "Binder not initialized")
            emptyList()
        }
    }
    
    suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return if (::binder.isInitialized) {
            binder.getMessagesFromContact(phoneNumber)
        } else {
            Log.w(TAG, "Binder not initialized")
            emptyList()
        }
    }
    
    suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return if (::binder.isInitialized) {
            binder.getConversation(phoneNumber)
        } else {
            Log.w(TAG, "Binder not initialized")
            null
        }
    }
    
    suspend fun refreshSmsData() {
        if (::binder.isInitialized) {
            binder.refreshSmsData()
        } else {
            Log.w(TAG, "Binder not initialized")
        }
    }
    
    fun registerSmsListener(listener: SmsUpdateListener) {
        // Register with both service-level listeners (for backward compatibility) and binder
        if (!smsListeners.contains(listener)) {
            smsListeners.add(listener)
            Log.d(TAG, "Registered SMS listener at service level, total: ${smsListeners.size}")
        }
        
        if (::binder.isInitialized) {
            binder.registerSmsListener(listener)
        }
    }
    
    fun unregisterSmsListener(listener: SmsUpdateListener) {
        // Unregister from both service-level listeners and binder
        if (smsListeners.remove(listener)) {
            Log.d(TAG, "Unregistered SMS listener from service level, total: ${smsListeners.size}")
        }
        
        if (::binder.isInitialized) {
            binder.unregisterSmsListener(listener)
        }
    }
}