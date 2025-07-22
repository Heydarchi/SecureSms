package com.aospinsight.securesms.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager class for handling SMS operations including fetching and categorizing messages
 */
class SmsManager internal constructor(private val context: Context) {

    private val TAG = "SmsManager"
        

    /**
     * Check if SMS permissions are granted
     */
    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Fetch all SMS messages from the device
     */
    suspend fun getAllSmsMessages(): List<SmsMessage> = withContext(Dispatchers.IO) {
        if (!hasPermissions()) {
            Log.w(TAG, "SMS permissions not granted")
            return@withContext emptyList()
        }
        
        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        
        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
                
                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val address = it.getString(addressIndex) ?: ""
                    val body = it.getString(bodyIndex) ?: ""
                    val date = it.getLong(dateIndex)
                    val type = SmsType.fromValue(it.getInt(typeIndex))
                    val isRead = it.getInt(readIndex) == 1
                    
                    messages.add(
                        SmsMessage(
                            id = id,
                            phoneNumber = address,
                            message = body,
                            timestamp = date,
                            type = type,
                            isRead = isRead
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SMS messages: ${e.message}")
        }
        
        return@withContext messages
    }
    
    /**
     * Categorize SMS messages by phone number
     */
    suspend fun getSmsConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        val allMessages = getAllSmsMessages()
        return@withContext categorizeSmsMessages(allMessages)
    }
    
    /**
     * Categorize SMS messages by phone number
     */
    private suspend fun categorizeSmsMessages(messages: List<SmsMessage>): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversationMap = mutableMapOf<String, MutableList<SmsMessage>>()
        
        // Group messages by phone number
        messages.forEach { message ->
            val phoneNumber = normalizePhoneNumber(message.phoneNumber)
            conversationMap.getOrPut(phoneNumber) { mutableListOf() }.add(message)
        }
        
        // Convert to SmsConversation objects
        val conversations = conversationMap.map { (phoneNumber, messageList) ->
            val sortedMessages = messageList.sortedByDescending { it.timestamp }
            val unreadCount = messageList.count { !it.isRead && it.type == SmsType.INBOX }
            val lastMessageTimestamp = sortedMessages.firstOrNull()?.timestamp ?: 0L
            val displayName = getContactName(phoneNumber)
            
            SmsConversation(
                phoneNumber = phoneNumber,
                displayName = displayName,
                messages = sortedMessages,
                lastMessageTimestamp = lastMessageTimestamp,
                unreadCount = unreadCount
            )
        }
        
        // Sort conversations by last message timestamp
        return@withContext conversations.sortedByDescending { it.lastMessageTimestamp }
    }
    
    /**
     * Get SMS messages for a specific phone number
     */
    suspend fun getSmsForPhoneNumber(phoneNumber: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        val allMessages = getAllSmsMessages()
        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        
        return@withContext allMessages.filter { 
            normalizePhoneNumber(it.phoneNumber) == normalizedPhoneNumber 
        }.sortedByDescending { it.timestamp }
    }
    
    /**
     * Save a received SMS (called from SmsReceiver)
     */
    fun saveReceivedSms(phoneNumber: String, message: String, timestamp: Long) {
        Log.d(TAG, "Saving received SMS from $phoneNumber")
        // Here you can implement additional saving logic if needed
        // For example, saving to a local database, encrypting, etc.
    }
    
    /**
     * Get contact name for a phone number
     */
    private fun getContactName(phoneNumber: String): String? {
        if (!hasPermissions()) return null
        
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting contact name: ${e.message}")
        }
        
        return null
    }
    
    /**
     * Normalize phone number for consistent comparison
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^\\d+]"), "")
            .let { cleaned ->
                when {
                    cleaned.startsWith("+") -> cleaned
                    cleaned.length == 10 -> "+1$cleaned"  // Assume US number
                    cleaned.length == 11 && cleaned.startsWith("1") -> "+$cleaned"
                    else -> cleaned
                }
            }
    }
    
    /**
     * Search SMS messages by keyword
     */
    suspend fun searchSmsMessages(query: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        val allMessages = getAllSmsMessages()
        return@withContext allMessages.filter { 
            it.message.contains(query, ignoreCase = true) || 
            it.phoneNumber.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * Get unread SMS count
     */
    suspend fun getUnreadSmsCount(): Int = withContext(Dispatchers.IO) {
        val allMessages = getAllSmsMessages()
        return@withContext allMessages.count { !it.isRead && it.type == SmsType.INBOX }
    }
    
    /**
     * Get the latest messages from each contact (same as getSmsConversations but with explicit naming)
     */
    suspend fun getLatestMessagesFromEachContact(): List<SmsConversation> {
        return getSmsConversations()
    }
    
    /**
     * Get all messages from a specific contact number
     */
    suspend fun getMessagesFromContact(phoneNumber: String): List<SmsMessage> {
        return getSmsForPhoneNumber(phoneNumber)
    }
    

    
    /**
     * Refresh SMS data cache (for future implementation if caching is needed)
     */
    suspend fun refreshSmsData(): List<SmsConversation> {
        Log.d(TAG, "Refreshing SMS data")
        return getSmsConversations()
    }
}
