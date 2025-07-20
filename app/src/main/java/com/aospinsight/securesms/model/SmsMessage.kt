package com.aospinsight.securesms.model

import java.util.Date

/**
 * Data class representing an SMS message
 */
data class SmsMessage(
    val id: Long,
    val phoneNumber: String,
    val message: String,
    val timestamp: Long,
    val type: SmsType,
    val isRead: Boolean = false
) {
    val date: Date
        get() = Date(timestamp)
    
    val displayPhoneNumber: String
        get() = phoneNumber.takeIf { it.isNotBlank() } ?: "Unknown"
}

/**
 * Enum representing SMS message types
 */
enum class SmsType(val value: Int) {
    INBOX(1),      // Received SMS
    SENT(2),       // Sent SMS
    DRAFT(3),      // Draft SMS
    OUTBOX(4),     // Outbox SMS
    FAILED(5),     // Failed SMS
    QUEUED(6);     // Queued SMS
    
    companion object {
        fun fromValue(value: Int): SmsType {
            return values().find { it.value == value } ?: INBOX
        }
    }
}
