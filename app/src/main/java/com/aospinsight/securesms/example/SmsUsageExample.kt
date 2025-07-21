package com.aospinsight.securesms.example

import android.content.Context
import android.util.Log
import com.aospinsight.securesms.broadcastreceiver.ISmsReceivedListener
import com.aospinsight.securesms.sms.SmsManager
import com.aospinsight.securesms.broadcastreceiver.SmsReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Example class demonstrating how to use the SMS functionality
 */
class SmsUsageExample(private val context: Context) : ISmsReceivedListener {
    
    private val smsManager = SmsManager.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val smsReceiver = SmsReceiver()

    init {
        // Set up the SMS receiver listener
        smsReceiver.setSmsReceivedListener(this)
    }
    
    /**
     * Example: Load all SMS conversations
     */
    fun loadAllConversations() {
        coroutineScope.launch {
            try {
                val conversations = smsManager.getSmsConversations()
                Log.d("SmsExample", "Loaded ${conversations.size} conversations")
                
                conversations.forEach { conversation ->
                    Log.d("SmsExample", 
                        "Conversation with ${conversation.displayPhoneNumber}: " +
                        "${conversation.messageCount} messages, " +
                        "${conversation.unreadCount} unread"
                    )
                }
            } catch (e: Exception) {
                Log.e("SmsExample", "Error loading conversations", e)
            }
        }
    }
    
    /**
     * Example: Get messages for a specific phone number
     */
    fun loadMessagesForPhoneNumber(phoneNumber: String) {
        coroutineScope.launch {
            try {
                val messages = smsManager.getSmsForPhoneNumber(phoneNumber)
                Log.d("SmsExample", "Loaded ${messages.size} messages for $phoneNumber")
                
                messages.forEach { message ->
                    Log.d("SmsExample", 
                        "Message: ${message.message} (${message.type}, ${if (message.isRead) "read" else "unread"})"
                    )
                }
            } catch (e: Exception) {
                Log.e("SmsExample", "Error loading messages for $phoneNumber", e)
            }
        }
    }
    
    /**
     * Example: Search for messages containing a keyword
     */
    fun searchMessages(keyword: String) {
        coroutineScope.launch {
            try {
                val messages = smsManager.searchSmsMessages(keyword)
                Log.d("SmsExample", "Found ${messages.size} messages containing '$keyword'")
                
                messages.forEach { message ->
                    Log.d("SmsExample", 
                        "Found in message from ${message.phoneNumber}: ${message.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("SmsExample", "Error searching messages", e)
            }
        }
    }
    
    /**
     * Example: Get unread SMS count
     */
    fun getUnreadCount() {
        coroutineScope.launch {
            try {
                val unreadCount = smsManager.getUnreadSmsCount()
                Log.d("SmsExample", "Unread SMS count: $unreadCount")
            } catch (e: Exception) {
                Log.e("SmsExample", "Error getting unread count", e)
            }
        }
    }
    
    /**
     * Called when a new SMS is received
     */
    override fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        Log.d("SmsExample", "New SMS received from $phoneNumber: $message")
        
        // You can add custom logic here, such as:
        // - Show a notification
        // - Save to database
        // - Process the message content
        // - Apply filters or categorization
        
        // Reload conversations to update the UI
        loadAllConversations()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        smsReceiver.setSmsReceivedListener(null)
    }
}

/**
 * Example usage in an Activity or Fragment:
 * 
 * class ExampleActivity : AppCompatActivity() {
 *     private lateinit var smsExample: SmsUsageExample
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         smsExample = SmsUsageExample(this)
 *         
 *         // Load all conversations
 *         smsExample.loadAllConversations()
 *         
 *         // Search for messages
 *         smsExample.searchMessages("important")
 *         
 *         // Get unread count
 *         smsExample.getUnreadCount()
 *     }
 *     
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         smsExample.cleanup()
 *     }
 * }
 */
