package com.aospinsight.securesms.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockCursor: Cursor
    private lateinit var smsRepository: SmsRepository
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        mockContext = mockk()
        mockContentResolver = mockk()
        mockCursor = mockk()
        
        every { mockContext.contentResolver } returns mockContentResolver
        
        smsRepository = SmsRepository(mockContext, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun setupCursorWithData(messages: List<TestSmsData>) {
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

        messages.forEachIndexed { index, message ->
            every { mockCursor.getLong(0) } returnsMany messages.map { it.id }
            every { mockCursor.getString(1) } returnsMany messages.map { it.phoneNumber }
            every { mockCursor.getString(2) } returnsMany messages.map { it.body }
            every { mockCursor.getLong(3) } returnsMany messages.map { it.timestamp }
            every { mockCursor.getInt(4) } returnsMany messages.map { it.type }
            every { mockCursor.getInt(5) } returnsMany messages.map { if (it.isRead) 1 else 0 }
        }
    }

    private fun setupEmptyCursor() {
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.close() } just Runs
    }

    data class TestSmsData(
        val id: Long,
        val phoneNumber: String,
        val body: String,
        val timestamp: Long,
        val type: Int,
        val isRead: Boolean
    )

    @Test
    fun `getAllConversations returns conversations grouped by phone number`() = runTest {
        // Given
        val testMessages = listOf(
            TestSmsData(1L, "+1234567890", "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestSmsData(2L, "+1234567890", "Message 2", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true),
            TestSmsData(3L, "+0987654321", "Message 3", 3000L, Telephony.Sms.MESSAGE_TYPE_INBOX, false)
        )
        
        setupCursorWithData(testMessages)
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val conversations = smsRepository.getAllConversations()

        // Then
        assertThat(conversations).hasSize(2)
        
        val conv1 = conversations.find { it.phoneNumber == "+1234567890" }
        assertThat(conv1).isNotNull()
        assertThat(conv1!!.messages).hasSize(2)
        assertThat(conv1.unreadCount).isEqualTo(0)
        
        val conv2 = conversations.find { it.phoneNumber == "+0987654321" }
        assertThat(conv2).isNotNull()
        assertThat(conv2!!.messages).hasSize(1)
        assertThat(conv2.unreadCount).isEqualTo(1)
    }

    @Test
    fun `getAllConversations returns empty list when no messages`() = runTest {
        // Given
        setupEmptyCursor()
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val conversations = smsRepository.getAllConversations()

        // Then
        assertThat(conversations).isEmpty()
    }

    @Test
    fun `getAllConversations handles null cursor gracefully`() = runTest {
        // Given
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns null

        // When
        val conversations = smsRepository.getAllConversations()

        // Then
        assertThat(conversations).isEmpty()
    }

    @Test
    fun `getMessagesForPhoneNumber returns filtered messages`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val testMessages = listOf(
            TestSmsData(1L, phoneNumber, "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestSmsData(2L, phoneNumber, "Message 2", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true),
            TestSmsData(3L, "+0987654321", "Message 3", 3000L, Telephony.Sms.MESSAGE_TYPE_INBOX, false)
        )
        
        setupCursorWithData(testMessages)
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsRepository.getMessagesForPhoneNumber(phoneNumber)

        // Then
        assertThat(messages).hasSize(2)
        assertThat(messages.all { it.phoneNumber == phoneNumber }).isTrue()
    }

    @Test
    fun `getConversation returns conversation for specific phone number`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val testMessages = listOf(
            TestSmsData(1L, phoneNumber, "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestSmsData(2L, phoneNumber, "Message 2", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true)
        )
        
        setupCursorWithData(testMessages)
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val conversation = smsRepository.getConversation(phoneNumber)

        // Then
        assertThat(conversation).isNotNull()
        assertThat(conversation!!.phoneNumber).isEqualTo(phoneNumber)
        assertThat(conversation.messages).hasSize(2)
    }

    @Test
    fun `getConversation returns null when phone number has no messages`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        setupEmptyCursor()
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val conversation = smsRepository.getConversation(phoneNumber)

        // Then
        assertThat(conversation).isNull()
    }

    @Test
    fun `refresh updates conversations flow`() = runTest {
        // Given
        val testMessages = listOf(
            TestSmsData(1L, "+1234567890", "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true)
        )
        
        setupCursorWithData(testMessages)
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        smsRepository.refresh()

        // Then
        val conversations = smsRepository.conversationsFlow.first()
        assertThat(conversations).hasSize(1)
        assertThat(conversations[0].phoneNumber).isEqualTo("+1234567890")
    }

    @Test
    fun `conversationsFlow emits updated data after refresh`() = runTest {
        // Given - initial empty state
        setupEmptyCursor()
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor
        
        val initialConversations = smsRepository.conversationsFlow.first()
        assertThat(initialConversations).isEmpty()

        // When - add data and refresh
        val testMessages = listOf(
            TestSmsData(1L, "+1234567890", "Message 1", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true)
        )
        setupCursorWithData(testMessages)
        smsRepository.refresh()

        // Then
        val updatedConversations = smsRepository.conversationsFlow.first()
        assertThat(updatedConversations).hasSize(1)
    }

    @Test
    fun `sms type conversion works correctly`() = runTest {
        // Given
        val testMessages = listOf(
            TestSmsData(1L, "+1234567890", "Inbox", 1000L, Telephony.Sms.MESSAGE_TYPE_INBOX, true),
            TestSmsData(2L, "+1234567890", "Sent", 2000L, Telephony.Sms.MESSAGE_TYPE_SENT, true),
            TestSmsData(3L, "+1234567890", "Draft", 3000L, Telephony.Sms.MESSAGE_TYPE_DRAFT, true),
            TestSmsData(4L, "+1234567890", "Outbox", 4000L, Telephony.Sms.MESSAGE_TYPE_OUTBOX, true),
            TestSmsData(5L, "+1234567890", "Failed", 5000L, Telephony.Sms.MESSAGE_TYPE_FAILED, true),
            TestSmsData(6L, "+1234567890", "Queued", 6000L, Telephony.Sms.MESSAGE_TYPE_QUEUED, true),
            TestSmsData(7L, "+1234567890", "Unknown", 7000L, 999, true) // Unknown type
        )
        
        setupCursorWithData(testMessages)
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        // When
        val messages = smsRepository.getMessagesForPhoneNumber("+1234567890")

        // Then
        assertThat(messages).hasSize(7)
        assertThat(messages[0].type).isEqualTo(SmsType.INBOX)
        assertThat(messages[1].type).isEqualTo(SmsType.SENT)
        assertThat(messages[2].type).isEqualTo(SmsType.DRAFT)
        assertThat(messages[3].type).isEqualTo(SmsType.OUTBOX)
        assertThat(messages[4].type).isEqualTo(SmsType.FAILED)
        assertThat(messages[5].type).isEqualTo(SmsType.QUEUED)
        assertThat(messages[6].type).isEqualTo(SmsType.UNKNOWN) // Default for unknown type
    }

    @Test
    fun `repository handles cursor exceptions gracefully`() = runTest {
        // Given
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } throws SecurityException("No permission")

        // When
        val conversations = smsRepository.getAllConversations()

        // Then - should return empty list instead of throwing
        assertThat(conversations).isEmpty()
    }

    @Test
    fun `repository handles cursor reading exceptions gracefully`() = runTest {
        // Given
        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor
        every { mockCursor.moveToNext() } throws IllegalStateException("Cursor error")
        every { mockCursor.close() } just Runs

        // When
        val conversations = smsRepository.getAllConversations()

        // Then - should return empty list and close cursor
        assertThat(conversations).isEmpty()
        verify { mockCursor.close() }
    }
}
