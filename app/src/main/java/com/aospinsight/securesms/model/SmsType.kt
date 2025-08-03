package com.aospinsight.securesms.model

enum class SmsType(val value: Int) {
    INBOX(1),      // Received SMS
    SENT(2),       // Sent SMS
    DRAFT(3),      // Draft SMS
    OUTBOX(4),     // Outbox SMS
    FAILED(5),     // Failed SMS
    QUEUED(6),     // Queued SMS
    UNKNOWN(0);   // Unknown type

    companion object {
        fun fromValue(value: Int): SmsType {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}