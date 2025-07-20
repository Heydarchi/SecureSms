# SecureSms - SMS Management Android Application

This Android application provides comprehensive SMS management functionality including receiving, fetching, and categorizing SMS messages by phone number.

## Features

- **BroadcastReceiver**: Automatically receives incoming SMS messages
- **SMS Manager**: Fetches all SMS messages from the device
- **Categorization**: Groups SMS messages by phone number
- **Contact Integration**: Shows contact names when available
- **Permission Handling**: Proper runtime permission management
- **Modern UI**: Built with Jetpack Compose

## Architecture

### Core Components

1. **SmsReceiver** - BroadcastReceiver for incoming SMS
2. **SmsManager** - Main class for SMS operations
3. **SmsRepository** - Data layer for managing SMS data
4. **Data Models** - SmsMessage, SmsConversation, SmsType

### Key Classes

#### `SmsReceiver`
- Receives incoming SMS messages via BroadcastReceiver
- Processes and saves received messages
- Provides callback interface for real-time SMS notifications

#### `SmsManager`
- Singleton class for all SMS operations
- Fetches all SMS messages from device
- Categorizes messages by phone number
- Provides search functionality
- Handles contact name resolution

#### `SmsMessage`
Data class representing an SMS message:
```kotlin
data class SmsMessage(
    val id: Long,
    val phoneNumber: String,
    val message: String,
    val timestamp: Long,
    val type: SmsType,
    val isRead: Boolean
)
```

#### `SmsConversation`
Data class representing SMS conversations grouped by phone number:
```kotlin
data class SmsConversation(
    val phoneNumber: String,
    val displayName: String?,
    val messages: List<SmsMessage>,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)
```

## Permissions Required

The application requires the following permissions:

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

## Usage Examples

### Basic Usage

```kotlin
// Get SMS Manager instance
val smsManager = SmsManager.getInstance(context)

// Check permissions
if (smsManager.hasPermissions()) {
    // Fetch all conversations
    val conversations = smsManager.getSmsConversations()
    
    // Get messages for specific phone number
    val messages = smsManager.getSmsForPhoneNumber("+1234567890")
    
    // Search messages
    val searchResults = smsManager.searchSmsMessages("keyword")
}
```

### Setting up SMS Receiver

```kotlin
class MainActivity : AppCompatActivity(), SmsReceiver.Companion.OnSmsReceivedListener {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set SMS received listener
        SmsReceiver.setSmsReceivedListener(this)
    }
    
    override fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        // Handle new SMS
        Log.d("SMS", "New SMS from $phoneNumber: $message")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        SmsReceiver.setSmsReceivedListener(null)
    }
}
```

## Dependencies

The project uses the following key dependencies:

```kotlin
// Coroutines for async operations
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// ViewModel and LiveData
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Jetpack Compose
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.ui)
implementation(libs.androidx.material3)
```

## AndroidManifest Configuration

The BroadcastReceiver is registered in AndroidManifest.xml:

```xml
<receiver
    android:name=".sms.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="1000">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

## Key Features

### 1. Real-time SMS Reception
- Automatically receives incoming SMS via BroadcastReceiver
- High priority intent filter for immediate processing
- Callback interface for UI updates

### 2. SMS Categorization
- Groups messages by phone number
- Sorts conversations by latest message timestamp
- Tracks unread message count per conversation
- Contact name resolution when available

### 3. Permission Management
- Runtime permission requests
- Permission status checking
- User-friendly permission explanations

### 4. Search and Filtering
- Search messages by content or phone number
- Filter by message type (inbox, sent, etc.)
- Case-insensitive search

### 5. Contact Integration
- Resolves contact names for phone numbers
- Falls back to phone number if no contact found
- Normalizes phone numbers for consistent grouping

## Security Considerations

- All SMS operations require proper permissions
- Permissions are checked before any SMS access
- Sensitive SMS data should be handled securely
- Consider encryption for stored SMS data

## Testing

To test the SMS functionality:

1. Grant SMS permissions to the app
2. Send SMS to the device
3. Check if SmsReceiver captures the message
4. Verify that messages are properly categorized
5. Test search functionality

## Future Enhancements

- SMS encryption/decryption
- Backup and restore functionality
- Advanced filtering options
- Conversation threading
- Message scheduling
- Spam detection

## Notes

- Minimum SDK: 29 (Android 10)
- Target SDK: 36
- Built with Kotlin and Jetpack Compose
- Uses modern Android architecture patterns
