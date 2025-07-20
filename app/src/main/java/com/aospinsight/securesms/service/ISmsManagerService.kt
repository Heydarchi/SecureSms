package com.aospinsight.securesms.service

import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage

/**
 * Interface for SmsManagerService to provide SMS data access methods
 */
interface ISmsManagerService {
    
    /**
     * Get the latest message from each contact number
     * @return List of conversations with the latest message from each contact
     */
    suspend fun getLatestMessagesFromEachContact(): List<SmsConversation>
    
    /**
     * Get all messages from a specific contact number
     * @param phoneNumber The phone number to get messages from
     * @return List of messages from the specified contact, sorted by timestamp
     */
    suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage>
    
    /**
     * Get a specific conversation by phone number
     * @param phoneNumber The phone number to get the conversation for
     * @return SmsConversation object if found, null otherwise
     */
    suspend fun getConversation(phoneNumber: String): SmsConversation?
    
    /**
     * Refresh and reload all SMS data
     */
    suspend fun refreshSmsData()
    
    /**
     * Register a listener for new SMS messages
     * @param listener The listener to register
     */
    fun registerSmsListener(listener: SmsUpdateListener)
    
    /**
     * Unregister an SMS listener
     * @param listener The listener to unregister
     */
    fun unregisterSmsListener(listener: SmsUpdateListener)
    

}
