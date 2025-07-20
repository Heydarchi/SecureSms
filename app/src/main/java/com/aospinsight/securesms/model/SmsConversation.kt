package com.aospinsight.securesms.model


data class SmsConversation(
    val phoneNumber: String,
    val displayName: String? = null,
    val messages: List<SmsMessage>,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
) {
    val displayPhoneNumber: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: phoneNumber.takeIf { it.isNotBlank() } ?: "Unknown"
    
    val lastMessage: SmsMessage?
        get() = messages.maxByOrNull { it.timestamp }
    
    val messageCount: Int
        get() = messages.size
}
