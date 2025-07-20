package com.aospinsight.securesms.service

interface SmsUpdateListener {
    fun onNewSmsReceived(phoneNumber: String, message: String, timestamp: Long)
    fun onSmsDataRefreshed()
}