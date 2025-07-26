package com.aospinsight.securesms.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.*

class SmsMessageTest {

    @Test
    fun givenValidData_whenCreateSmsMessage_thenReturnSmsMessageWithCorrectProperties() {
        // Given
        val id = 1L
        val phoneNumber = "+1234567890"
        val message = "Test message"
        val timestamp = System.currentTimeMillis()
        val type = SmsType.INBOX
        val isRead = false

        // When
        val smsMessage = SmsMessage(
            id = id,
            phoneNumber = phoneNumber,
            message = message,
            timestamp = timestamp,
            type = type,
            isRead = isRead
        )

        // Then
        assertThat(smsMessage.id).isEqualTo(id)
        assertThat(smsMessage.phoneNumber).isEqualTo(phoneNumber)
        assertThat(smsMessage.message).isEqualTo(message)
        assertThat(smsMessage.timestamp).isEqualTo(timestamp)
        assertThat(smsMessage.type).isEqualTo(type)
        assertThat(smsMessage.isRead).isEqualTo(isRead)
    }

    @Test
    fun givenTimestamp_whenAccessingDateProperty_thenReturnCorrectDateObject() {
        // Given
        val timestamp = 1640995200000L // Jan 1, 2022 00:00:00 UTC
        val smsMessage = SmsMessage(
            id = 1L,
            phoneNumber = "+1234567890",
            message = "Test",
            timestamp = timestamp,
            type = SmsType.INBOX
        )

        // When
        val date = smsMessage.date

        // Then
        assertThat(date).isEqualTo(Date(timestamp))
    }

    @Test
    fun givenNonBlankPhoneNumber_whenAccessingDisplayPhoneNumber_thenReturnPhoneNumber() {
        // Given
        val phoneNumber = "+1234567890"
        val smsMessage = SmsMessage(
            id = 1L,
            phoneNumber = phoneNumber,
            message = "Test",
            timestamp = System.currentTimeMillis(),
            type = SmsType.INBOX
        )

        // When
        val displayPhoneNumber = smsMessage.displayPhoneNumber

        // Then
        assertThat(displayPhoneNumber).isEqualTo(phoneNumber)
    }

    @Test
    fun givenBlankPhoneNumber_whenAccessingDisplayPhoneNumber_thenReturnUnknown() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            phoneNumber = "",
            message = "Test",
            timestamp = System.currentTimeMillis(),
            type = SmsType.INBOX
        )

        // When
        val displayPhoneNumber = smsMessage.displayPhoneNumber

        // Then
        assertThat(displayPhoneNumber).isEqualTo("Unknown")
    }

    @Test
    fun givenWhitespacePhoneNumber_whenAccessingDisplayPhoneNumber_thenReturnUnknown() {
        // Given
        val smsMessage = SmsMessage(
            id = 1L,
            phoneNumber = "   ",
            message = "Test",
            timestamp = System.currentTimeMillis(),
            type = SmsType.INBOX
        )

        // When
        val displayPhoneNumber = smsMessage.displayPhoneNumber

        // Then
        assertThat(displayPhoneNumber).isEqualTo("Unknown")
    }

    @Test
    fun givenTwoSmsMessagesWithSameData_whenComparing_thenReturnEqual() {
        // Given
        val id = 1L
        val phoneNumber = "+1234567890"
        val message = "Test message"
        val timestamp = System.currentTimeMillis()
        val type = SmsType.INBOX
        val isRead = false

        val smsMessage1 = SmsMessage(id, phoneNumber, message, timestamp, type, isRead)
        val smsMessage2 = SmsMessage(id, phoneNumber, message, timestamp, type, isRead)

        // Then
        assertThat(smsMessage1).isEqualTo(smsMessage2)
        assertThat(smsMessage1.hashCode()).isEqualTo(smsMessage2.hashCode())
    }
}
