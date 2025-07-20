package com.aospinsight.securesms.repository

import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.sms.SmsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository class for managing SMS data
 */
class SmsRepository private constructor(private val smsManager: SmsManager) {
    
    companion object {
        @Volatile
        private var INSTANCE: SmsRepository? = null
        
        fun getInstance(smsManager: SmsManager): SmsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmsRepository(smsManager).also { INSTANCE = it }
            }
        }
    }
    
    private val _conversations = MutableStateFlow<List<SmsConversation>>(emptyList())
    val conversations: Flow<List<SmsConversation>> = _conversations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: Flow<String?> = _error.asStateFlow()
    
    /**
     * Load all SMS conversations
     */
    suspend fun loadConversations() {
        _isLoading.value = true
        _error.value = null
        
        try {
            val conversations = smsManager.getSmsConversations()
            _conversations.value = conversations
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Get messages for a specific phone number
     */
    suspend fun getMessagesForPhoneNumber(phoneNumber: String): List<SmsMessage> {
        return try {
            smsManager.getSmsForPhoneNumber(phoneNumber)
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }
    
    /**
     * Search messages
     */
    suspend fun searchMessages(query: String): List<SmsMessage> {
        return try {
            smsManager.searchSmsMessages(query)
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }
    
    /**
     * Get unread count
     */
    suspend fun getUnreadCount(): Int {
        return try {
            smsManager.getUnreadSmsCount()
        } catch (e: Exception) {
            _error.value = e.message
            0
        }
    }
    
    /**
     * Refresh conversations
     */
    suspend fun refresh() {
        loadConversations()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
