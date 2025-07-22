package com.aospinsight.securesms.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import com.aospinsight.securesms.sms.SmsManager
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmsManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockCursor: Cursor
    private lateinit var smsManager: SmsManager

    @Before
    fun setup() {
        mockContext = mockk()
        mockCursor = mockk()
        smsManager = SmsManager(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun setupCursorWithMessages(messages: List<TestMessage>) {
        every { mockCursor.moveToNext() } returnsMany (messages.map { true } + false)
        every { mockCursor.close() } just Runs
        
        val columnIndices = mapOf(
            Telephony.Sms._ID to 0,
            Telephony.Sms.ADDRESS to 1,
            Telephony.Sms.BODY to 2,
            Telephony.Sms.DATE to 3,
            Telephony.Sms.TYPE to 4,
            Telephony.Sms.READ to 5
        )
        
        columnIndices.forEach { (column, index) ->
            every { mockCursor.getColumnIndexOrThrow(column) } returns index
        }

        every { mockCursor.getLong(0) } returnsMany messages.map { it.id }
        every { mockCursor.getString(1) } returnsMany messages.map { it.address }
        every { mockCursor.getString(2) } returnsMany messages.map { it.body }
        every { mockCursor.getLong(3) } returnsMany messages.map { it.date }
        every { mockCursor.getInt(4) } returnsMany messages.map { it.type }
        every { mockCursor.getInt(5) } returnsMany messages.map { if (it.read) 1 else 0 }
    }

    private fun setupEmptyCursor() {
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.close() } just Runs
    }

    data class TestMessage(
        val id: Long,
        val address: String,
        val body: String,
        val date: Long,
        val type: Int,
        val read: Boolean
    )

    @Test
    suspend fun `getAllMessages returns all SMS messages`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1234567890", "Hello", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+0987654321", "Hi", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true)
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).hasSize(2)
        assertThat(messages[0].phoneNumber).isEqualTo("+1234567890")
        assertThat(messages[0].message).isEqualTo("Hello")
        assertThat(messages[0].type).isEqualTo(SmsType.INBOX)
        assertThat(messages[1].phoneNumber).isEqualTo("+0987654321")
        assertThat(messages[1].message).isEqualTo("Hi")
        assertThat(messages[1].type).isEqualTo(SmsType.SENT)
    }

    @Test
    suspend fun `getAllMessages returns empty list when no messages`() {
        // Given
        setupEmptyCursor()
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `getAllMessages handles null cursor gracefully`() {
        // Given
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns null

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `getMessagesForContact returns filtered messages`() {
        // Given
        val targetPhoneNumber = "+1234567890"
        val testMessages = listOf(
            TestMessage(1L, targetPhoneNumber, "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+0987654321", "Message 2", 2000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(3L, targetPhoneNumber, "Message 3", 3000L, Telephony.Sms.MESSAGE_TYPE_SENT, true)
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getMessagesFromContact(targetPhoneNumber)

        // Then
        assertThat(messages).hasSize(2)
        assertThat(messages.all { it.phoneNumber == targetPhoneNumber }).isTrue()
        assertThat(messages[0].message).isEqualTo("Message 1")
        assertThat(messages[1].message).isEqualTo("Message 3")
    }

    @Test
    suspend fun `getMessagesForContact returns empty list when no matches`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1111111111", "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true)
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getMessagesFromContact("+9999999999")

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `getLatestMessageFromEachContact returns one message per contact`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1234567890", "Older", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+1234567890", "Newer", 2000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(3L, "+0987654321", "Latest", 3000L, Telephony.Sms.MESSAGE_TYPE_SENT, true)
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getLatestMessagesFromEachContact()

        // Then
        assertThat(messages).hasSize(2)
        
        val contact1Message = messages.find { it.phoneNumber == "+1234567890" }
        assertThat(contact1Message?.messages?.last()).isEqualTo("Newer") // Latest message
        
        val contact2Message = messages.find { it.phoneNumber == "+0987654321" }
        assertThat(contact2Message?.messages?.first()).isEqualTo("Latest")
    }

    @Test
    suspend fun `getLatestMessageFromEachContact returns empty list when no messages`() {
        // Given
        setupEmptyCursor()
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getLatestMessagesFromEachContact()

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `SMS type conversion handles all known types`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1234567890", "Inbox", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+1234567890", "Sent", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true),
            TestMessage(3L, "+1234567890", "Draft", 3000L, Telephony.Sms.MESSAGE_TYPE_DRAFT, true),
            TestMessage(4L, "+1234567890", "Outbox", 4000L, Telephony.Sms.MESSAGE_TYPE_OUTBOX, true),
            TestMessage(5L, "+1234567890", "Failed", 5000L, Telephony.Sms.MESSAGE_TYPE_FAILED, true),
            TestMessage(6L, "+1234567890", "Queued", 6000L, Telephony.Sms.MESSAGE_TYPE_QUEUED, true),
            TestMessage(7L, "+1234567890", "Unknown", 7000L, 999, true) // Unknown type
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).hasSize(7)
        assertThat(messages[0].type).isEqualTo(SmsType.INBOX)
        assertThat(messages[1].type).isEqualTo(SmsType.SENT)
        assertThat(messages[2].type).isEqualTo(SmsType.DRAFT)
        assertThat(messages[3].type).isEqualTo(SmsType.OUTBOX)
        assertThat(messages[4].type).isEqualTo(SmsType.FAILED)
        assertThat(messages[5].type).isEqualTo(SmsType.QUEUED)
        assertThat(messages[6].type).isEqualTo(SmsType.UNKNOWN)
    }

    @Test
    suspend fun `SMS read status conversion works correctly`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1234567890", "Read", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+1234567890", "Unread", 2000L, Telephony.Sms.MESSAGE_TYPE_INBOX, false)
        )
        
        setupCursorWithMessages(testMessages)
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).hasSize(2)
        assertThat(messages[0].isRead).isTrue()
        assertThat(messages[1].isRead).isFalse()
    }

    @Test
    suspend fun `SmsManager handles cursor reading exceptions gracefully`() {
        // Given
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor
        every { mockCursor.moveToNext() } throws IllegalStateException("Cursor error")
        every { mockCursor.close() } just Runs

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).isEmpty()
        verify { mockCursor.close() }
    }

    @Test
    suspend fun `SmsManager handles security exceptions gracefully`() {
        // Given
        every { mockContext.contentResolver.query(any(), any(), any(), any(), any()) } throws SecurityException("No permission")

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `getMessagesForContact handles empty phone number`() {
        // When
        val messages = smsManager.getMessagesFromContact("")

        // Then
        assertThat(messages).isEmpty()
    }

    @Test
    suspend fun `getAllMessages orders messages by date descending`() {
        // Given
        val testMessages = listOf(
            TestMessage(1L, "+1234567890", "First", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(2L, "+1234567890", "Third", 3000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestMessage(3L, "+1234567890", "Second", 2000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true)
        )
        
        setupCursorWithMessages(testMessages)
        // Mock the query to use proper ordering
        every { 
            mockContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                any(),
                any(),
                any(),
                "${Telephony.Sms.DATE} DESC"
            ) 
        } returns mockCursor

        // When
        val messages = smsManager.getAllSmsMessages()

        // Then
        verify { 
            mockContext.contentResolver.query(
                any(),
                any(),
                any(),
                any(),
                "${Telephony.Sms.DATE} DESC"
            )
        }
    }
}
