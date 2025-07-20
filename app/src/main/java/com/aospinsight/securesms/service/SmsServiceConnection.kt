package com.aospinsight.securesms.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper class to manage binding to SmsManagerService
 */
class SmsServiceConnection(
    private val context: Context
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "SmsServiceConnection"
    }
    
    private var smsService: ISmsManagerService? = null
    private var bound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "Service connected")
            val binder = service as SmsManagerBinder
            smsService = binder
            bound = true
            onServiceConnectedCallback?.invoke(binder)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "Service disconnected")
            smsService = null
            bound = false
            onServiceDisconnectedCallback?.invoke()
        }
    }
    
    private var onServiceConnectedCallback: ((ISmsManagerService) -> Unit)? = null
    private var onServiceDisconnectedCallback: (() -> Unit)? = null
    
    /**
     * Bind to the SMS service
     */
    fun bindService(
        onConnected: ((ISmsManagerService) -> Unit)? = null,
        onDisconnected: (() -> Unit)? = null
    ) {
        onServiceConnectedCallback = onConnected
        onServiceDisconnectedCallback = onDisconnected
        
        Intent(context, SmsManagerService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    /**
     * Unbind from the SMS service
     */
    fun unbindService() {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
            smsService = null
        }
    }
    
    /**
     * Check if service is bound
     */
    fun isBound(): Boolean = bound
    
    /**
     * Get the service instance (null if not bound)
     */
    fun getService(): ISmsManagerService? = smsService
    
    /**
     * Execute an operation with the service, suspending until service is available
     */
    suspend fun <T> withService(operation: suspend (ISmsManagerService) -> T): T? {
        return if (bound && smsService != null) {
            try {
                operation(smsService!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing service operation", e)
                null
            }
        } else {
            Log.w(TAG, "Service not bound, cannot execute operation")
            null
        }
    }
    
    /**
     * Wait for service to be connected
     */
    suspend fun waitForConnection(): ISmsManagerService? = suspendCancellableCoroutine { continuation ->
        if (bound && smsService != null) {
            continuation.resume(smsService)
        } else {
            val originalCallback = onServiceConnectedCallback
            onServiceConnectedCallback = { service ->
                originalCallback?.invoke(service)
                continuation.resume(service)
            }
            
            continuation.invokeOnCancellation {
                onServiceConnectedCallback = originalCallback
            }
        }
    }
    
    // Lifecycle methods
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!bound) {
            bindService()
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        unbindService()
    }
    
    // Convenience methods for common operations
    suspend fun getLatestMessagesFromEachContact(): List<SmsConversation>? {
        return withService { it.getLatestMessagesFromEachContact() }
    }
    
    suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage>? {
        return withService { it.getMessagesFromContact(phoneNumber) }
    }
    
    suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return withService { it.getConversation(phoneNumber) }
    }
    
    suspend fun refreshSmsData() {
        withService { it.refreshSmsData() }
    }
    
    fun registerSmsListener(listener: SmsUpdateListener) {
        smsService?.registerSmsListener(listener)
    }
    
    fun unregisterSmsListener(listener: SmsUpdateListener) {
        smsService?.unregisterSmsListener(listener)
    }
}
