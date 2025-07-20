package com.aospinsight.securesms.model

import java.util.Date


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

