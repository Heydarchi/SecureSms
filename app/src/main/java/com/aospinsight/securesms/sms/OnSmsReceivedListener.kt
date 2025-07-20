package com.aospinsight.securesms.sms

interface OnSmsReceivedListener {
    fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long)
}