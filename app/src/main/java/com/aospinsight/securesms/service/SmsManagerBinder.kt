package com.aospinsight.securesms.service

import android.os.Binder
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage

/**
 * Binder class that provides access to SmsManagerService
 */
class SmsManagerBinder(private val service: SmsManagerService) : Binder(), ISmsManagerService {
    
    override suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return service.getLatestMessagesFromEachContact()
    }
    
    override suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return service.getMessagesFromContact(phoneNumber)
    }
    
    override suspend fun getConversation(phoneNumber: String): SmsConversation? {
        return service.getConversation(phoneNumber)
    }
    
    override suspend fun refreshSmsData() {
        service.refreshSmsData()
    }
    
    override fun registerSmsListener(listener: SmsUpdateListener) {
        service.registerSmsListener(listener)
    }
    
    override fun unregisterSmsListener(listener: SmsUpdateListener) {
        service.unregisterSmsListener(listener)
    }
    
    /**
     * Get the service instance
     */
    fun getService(): SmsManagerService {
        return service
    }
}
