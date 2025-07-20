package com.aospinsight.securesms

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.aospinsight.securesms.sms.SmsManager
import com.aospinsight.securesms.sms.SmsReceiver
import com.aospinsight.securesms.ui.theme.SecureSmsTheme
import com.aospinsight.securesms.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), SmsReceiver.Companion.OnSmsReceivedListener {
    
    private lateinit var smsManager: SmsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        smsManager = SmsManager.getInstance(this)
        
        // Set SMS received listener
        SmsReceiver.setSmsReceivedListener(this)
        
        setContent {
            SecureSmsTheme {
                SmsApp()
            }
        }
        
        // Check permissions on startup
        if (!PermissionUtils.hasSmsPermissions(this)) {
            PermissionUtils.requestSmsPermissions(this)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SmsReceiver.setSmsReceivedListener(null)
    }
    
    override fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        runOnUiThread {
            Toast.makeText(this, "New SMS from $phoneNumber", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "New SMS received from $phoneNumber: $message")
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionUtils.handlePermissionResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onPermissionGranted = {
                Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
            },
            onPermissionDenied = {
                Toast.makeText(this, "SMS permissions denied", Toast.LENGTH_LONG).show()
            }
        )
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
                    onClick = { PermissionUtils.requestSmsPermissions(this@MainActivity) },
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
                val conversations = smsManager.getSmsConversations()
                callback(conversations, null)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading conversations", e)
                callback(emptyList(), "Error loading SMS: ${e.message}")
            }
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}