package com.aospinsight.securesms.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmsConversationTest {

    private fun createTestSmsMessage(
        id: Long = 1L,
        phoneNumber: String = "+1234567890",
        message: String = "Test message",
        timestamp: Long = System.currentTimeMillis(),
        type: SmsType = SmsType.INBOX,
        isRead: Boolean = true
    ) = SmsMessage(id, phoneNumber, message, timestamp, type, isRead)

    @Test
    fun givenValidData_whenCreateSmsConversation_thenReturnConversationWithCorrectProperties() {
        // Given
        val phoneNumber = "+1234567890"
        val displayName = "John Doe"
        val messages = listOf(
            createTestSmsMessage(1L, phoneNumber, "Message 1", 1000L),
            createTestSmsMessage(2L, phoneNumber, "Message 2", 2000L)
        )
        val lastMessageTimestamp = 2000L
        val unreadCount = 1

        // When
        val conversation = SmsConversation(
            phoneNumber = phoneNumber,
            displayName = displayName,
            messages = messages,
            lastMessageTimestamp = lastMessageTimestamp,
            unreadCount = unreadCount
        )

        // Then
        assertThat(conversation.phoneNumber).isEqualTo(phoneNumber)
        assertThat(conversation.displayName).isEqualTo(displayName)
        assertThat(conversation.messages).isEqualTo(messages)
        assertThat(conversation.lastMessageTimestamp).isEqualTo(lastMessageTimestamp)
        assertThat(conversation.unreadCount).isEqualTo(unreadCount)
    }

    @Test
    fun givenNonBlankDisplayName_whenAccessingDisplayPhoneNumber_thenReturnDisplayName() {
        // Given
        val phoneNumber = "+1234567890"
        val displayName = "John Doe"
        val conversation = SmsConversation(
            phoneNumber = phoneNumber,
            displayName = displayName,
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val result = conversation.displayPhoneNumber

        // Then
        assertThat(result).isEqualTo(displayName)
    }

    @Test
    fun givenNullDisplayName_whenAccessingDisplayPhoneNumber_thenReturnPhoneNumber() {
        // Given
        val phoneNumber = "+1234567890"
        val conversation = SmsConversation(
            phoneNumber = phoneNumber,
            displayName = null,
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val result = conversation.displayPhoneNumber

        // Then
        assertThat(result).isEqualTo(phoneNumber)
    }

    @Test
    fun givenBlankDisplayName_whenAccessingDisplayPhoneNumber_thenReturnPhoneNumber() {
        // Given
        val phoneNumber = "+1234567890"
        val conversation = SmsConversation(
            phoneNumber = phoneNumber,
            displayName = "   ",
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val result = conversation.displayPhoneNumber

        // Then
        assertThat(result).isEqualTo(phoneNumber)
    }

    @Test
    fun givenBlankPhoneNumberAndNullDisplayName_whenAccessingDisplayPhoneNumber_thenReturnUnknown() {
        // Given
        val conversation = SmsConversation(
            phoneNumber = "",
            displayName = null,
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val result = conversation.displayPhoneNumber

        // Then
        assertThat(result).isEqualTo("Unknown")
    }

    @Test
    fun givenMultipleMessages_whenAccessingLastMessage_thenReturnMessageWithLatestTimestamp() {
        // Given
        val messages = listOf(
            createTestSmsMessage(1L, timestamp = 1000L, message = "First"),
            createTestSmsMessage(2L, timestamp = 3000L, message = "Latest"),
            createTestSmsMessage(3L, timestamp = 2000L, message = "Middle")
        )
        val conversation = SmsConversation(
            phoneNumber = "+1234567890",
            displayName = null,
            messages = messages,
            lastMessageTimestamp = 3000L,
            unreadCount = 0
        )

        // When
        val lastMessage = conversation.lastMessage

        // Then
        assertThat(lastMessage).isNotNull()
        assertThat(lastMessage?.message).isEqualTo("Latest")
        assertThat(lastMessage?.timestamp).isEqualTo(3000L)
    }

    @Test
    fun givenEmptyMessagesList_whenAccessingLastMessage_thenReturnNull() {
        // Given
        val conversation = SmsConversation(
            phoneNumber = "+1234567890",
            displayName = null,
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val lastMessage = conversation.lastMessage

        // Then
        assertThat(lastMessage).isNull()
    }

    @Test
    fun givenMultipleMessages_whenAccessingMessageCount_thenReturnCorrectCount() {
        // Given
        val messages = listOf(
            createTestSmsMessage(1L),
            createTestSmsMessage(2L),
            createTestSmsMessage(3L)
        )
        val conversation = SmsConversation(
            phoneNumber = "+1234567890",
            displayName = null,
            messages = messages,
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val count = conversation.messageCount

        // Then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun givenEmptyMessagesList_whenAccessingMessageCount_thenReturnZero() {
        // Given
        val conversation = SmsConversation(
            phoneNumber = "+1234567890",
            displayName = null,
            messages = emptyList(),
            lastMessageTimestamp = 0L,
            unreadCount = 0
        )

        // When
        val count = conversation.messageCount

        // Then
        assertThat(count).isEqualTo(0)
    }
}
