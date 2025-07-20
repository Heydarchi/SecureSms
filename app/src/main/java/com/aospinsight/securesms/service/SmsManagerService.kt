package com.aospinsight.securesms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.aospinsight.securesms.sms.OnSmsReceivedListener
import com.aospinsight.securesms.sms.SmsManager
import com.aospinsight.securesms.sms.SmsReceiver

class SmsManagerService : Service(), OnSmsReceivedListener {
    private lateinit var smsManager: SmsManager
    private val smsReceiver: SmsReceiver = SmsReceiver()

    init {
        smsReceiver.setSmsReceivedListener(this)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onSmsReceived(phoneNumber: String, message: String, timestamp: Long) {
        TODO("Not yet implemented")
    }
}