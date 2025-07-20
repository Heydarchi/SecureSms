package com.aospinsight.securesms

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.service.SmsServiceConnection
import com.aospinsight.securesms.service.SmsUpdateListener
import com.aospinsight.securesms.ui.theme.SecureSmsTheme
import com.aospinsight.securesms.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), SmsUpdateListener {
    
    private lateinit var smsServiceConnection: SmsServiceConnection
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
            // Refresh data when permissions are granted
            refreshSmsData()
        } else {
            Toast.makeText(this, "SMS permissions denied", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize service connection
        smsServiceConnection = SmsServiceConnection(this)
        lifecycle.addObserver(smsServiceConnection)
        
        // Bind to service
        smsServiceConnection.bindService(
            onConnected = { service ->
                Log.d("MainActivity", "SMS service connected")
                service.registerSmsListener(this@MainActivity)
                refreshSmsData()
            },
            onDisconnected = {
                Log.d("MainActivity", "SMS service disconnected")
            }
        )
        
        setContent {
            SecureSmsTheme {
                SmsApp()
            }
        }
        
        // Check permissions on startup
        if (!PermissionUtils.hasSmsPermissions(this)) {
            PermissionUtils.requestSmsPermissions(this, requestPermissionLauncher)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        smsServiceConnection.unregisterSmsListener(this)
        lifecycle.removeObserver(smsServiceConnection)
    }
    
    // Implementation of SmsUpdateListener
    override fun onNewSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        runOnUiThread {
            Toast.makeText(this, "New SMS from $phoneNumber", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "New SMS received from $phoneNumber: $message")
        }
    }
    
    override fun onSmsDataRefreshed() {
        Log.d("MainActivity", "SMS data refreshed")
        // The UI will automatically refresh due to the LaunchedEffect in SmsApp
    }
    
    @Composable
    fun SmsApp() {
        var conversations by remember { mutableStateOf<List<SmsConversation>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            loadConversations { convs, error ->
                conversations = convs
                errorMessage = error
                isLoading = false
            }
        }
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Secure SMS") },
                    actions = {
                        TextButton(
                            onClick = {
                                isLoading = true
                                loadConversations { convs, error ->
                                    conversations = convs
                                    errorMessage = error
                                    isLoading = false
                                }
                            }
                        ) {
                            Text("Refresh")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (!PermissionUtils.hasSmsPermissions(context)) {
                    PermissionCard()
                } else {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        errorMessage != null -> {
                            ErrorCard(errorMessage!!)
                        }
                        conversations.isEmpty() -> {
                            EmptyStateCard()
                        }
                        else -> {
                            ConversationsList(conversations)
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun PermissionCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "SMS Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This app needs SMS permissions to read and receive messages.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { PermissionUtils.requestSmsPermissions(this@MainActivity, requestPermissionLauncher) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
    
    @Composable
    fun ErrorCard(message: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    
    @Composable
    fun EmptyStateCard() {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No SMS Messages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No SMS conversations found on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    
    @Composable
    fun ConversationsList(conversations: List<SmsConversation>) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations) { conversation ->
                ConversationCard(conversation)
            }
        }
    }
    
    @Composable
    fun ConversationCard(conversation: SmsConversation) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.displayPhoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        conversation.lastMessage?.let { lastMsg ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastMsg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatTimestamp(conversation.lastMessageTimestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (conversation.unreadCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            @OptIn(ExperimentalMaterial3Api::class)
                            Badge {
                                Text(conversation.unreadCount.toString())
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Messages: ${conversation.messageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = conversation.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    private fun loadConversations(callback: (List<SmsConversation>, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val conversations = smsServiceConnection.getLatestMessagesFromEachContact()
                if (conversations != null) {
                    callback(conversations, null)
                } else {
                    callback(emptyList(), "SMS service not available")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading conversations", e)
                callback(emptyList(), "Error loading SMS: ${e.message}")
            }
        }
    }
    
    private fun refreshSmsData() {
        lifecycleScope.launch {
            try {
                smsServiceConnection.refreshSmsData()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error refreshing SMS data", e)
            }
        }
    }
    
    /**
     * Example method showing how to get messages from a specific contact
     */
    private fun getMessagesFromSpecificContact(phoneNumber: String) {
        lifecycleScope.launch {
            try {
                val messages = smsServiceConnection.getMessagesFromContact(phoneNumber)
                if (messages != null) {
                    Log.d("MainActivity", "Found ${messages.size} messages from $phoneNumber")
                    messages.forEach { message ->
                        Log.d("MainActivity", "Message: ${message.message} at ${message.timestamp}")
                    }
                } else {
                    Log.w("MainActivity", "Could not get messages from $phoneNumber - service not available")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting messages from $phoneNumber", e)
            }
        }
    }
    
    /**
     * Example method showing how to get a specific conversation
     */
    private fun getSpecificConversation(phoneNumber: String) {
        lifecycleScope.launch {
            try {
                val conversation = smsServiceConnection.getConversation(phoneNumber)
                if (conversation != null) {
                    Log.d("MainActivity", "Conversation with $phoneNumber has ${conversation.messageCount} messages")
                    Log.d("MainActivity", "Last message: ${conversation.lastMessage?.message}")
                    Log.d("MainActivity", "Unread count: ${conversation.unreadCount}")
                } else {
                    Log.w("MainActivity", "No conversation found with $phoneNumber")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting conversation with $phoneNumber", e)
            }
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}