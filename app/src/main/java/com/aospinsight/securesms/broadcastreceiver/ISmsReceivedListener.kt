package com.aospinsight.securesms.broadcastreceiver

interface ISmsReceivedListener {
    fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long)
}