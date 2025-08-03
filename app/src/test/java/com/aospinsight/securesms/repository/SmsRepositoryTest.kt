package com.aospinsight.securesms.repository

import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import com.aospinsight.securesms.sms.SmsManager
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsRepositoryTest {

    private lateinit var mockSmsManager: SmsManager
    private lateinit var smsRepository: SmsRepository

    @Before
    fun setup() {
        mockSmsManager = mockk<SmsManager>(relaxed = true)
        smsRepository = SmsRepository(mockSmsManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }



    @Test
    fun givenMultipleMessages_whenGetAllConversations_thenReturnConversationsGroupedByPhoneNumber() = runTest {
        // Given
        val testMessages : List<SmsConversation> = listOf(
            SmsConversation("+1234567890", "TestUser-1", listOf(
                SmsMessage(1, "+1234567890", "Hello", 1L, SmsType.INBOX, true),
                SmsMessage(2, "+1234567890", "Hi", 1L, SmsType.SENT, true)
            ), 1000L, 0),
            SmsConversation("+0987654321", "TestUser-2", listOf(
                SmsMessage(3, "+0987654321", "Hey there!", 2L, SmsType.INBOX, false)
            ), 2000L, 1)
        )


        coEvery { mockSmsManager.getSmsConversations() } returns testMessages

        // When
        smsRepository.loadConversations()
        val conversations = smsRepository.conversations.first()

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
    fun givenEmptyCursor_whenGetAllConversations_thenReturnEmptyList() = runTest {
        // Given
        coEvery { mockSmsManager.getSmsConversations() } returns emptyList()

        // When
        smsRepository.loadConversations()
        val conversations = smsRepository.conversations.first()

        // Then
        assertThat(conversations).isEmpty()
    }

    @Test
    fun givenNullCursor_whenGetAllConversations_thenHandleGracefullyAndReturnEmpty() = runTest {
        // Given
        coEvery { mockSmsManager.getSmsConversations() } throws Exception("Test exception")

        // When
        smsRepository.loadConversations()
        val conversations = smsRepository.conversations.first()
        val error = smsRepository.error.first()

        // Then
        assertThat(conversations).isEmpty()
        assertThat(error).isEqualTo("Test exception")
    }

    @Test
    fun givenSpecificPhoneNumber_whenGetMessagesForPhoneNumber_thenReturnFilteredMessages() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val otherPhoneNumber = "+1112223333"
        val allMessages = listOf(
            SmsMessage(1, phoneNumber, "Hello", 1L, SmsType.INBOX, true),
            SmsMessage(2, otherPhoneNumber, "Spam", 2L, SmsType.INBOX, true),
            SmsMessage(3, phoneNumber, "Hi again", 3L, SmsType.SENT, true)
        )
        // Simulate normalization by removing the '+'
        every { mockSmsManager.normalizePhoneNumber(any()) } answers { (firstArg<String>()).replace("+", "") }
        coEvery { mockSmsManager.getAllSmsMessages() } returns allMessages

        // When
        val messages = smsRepository.getMessagesForPhoneNumber(phoneNumber)

        // Then
        assertThat(messages).hasSize(2)
        assertThat(messages.all { it.phoneNumber == phoneNumber }).isTrue()
    }

    @Test
    fun givenSpecificPhoneNumber_whenGetConversation_thenReturnConversationForThatNumber() = runTest {
        // Given a conversation with messages for multiple phone numbers
        val phoneNumber = "+1234567890"
        val testConversations = listOf(
            SmsConversation(phoneNumber, "TestUser-1", listOf(
                SmsMessage(1, phoneNumber, "Hello", 1L, SmsType.INBOX, true),
                SmsMessage(2, phoneNumber, "Hi", 1L, SmsType.SENT, true)
            ), 1000L, 0),
            SmsConversation("+0987654321", "TestUser-2", listOf(
                SmsMessage(3, "+0987654321", "Hey there!", 2L, SmsType.INBOX, false)
            ), 2000L, 1)
        )
        coEvery { mockSmsManager.normalizePhoneNumber(phoneNumber) } returns phoneNumber
        coEvery { mockSmsManager.getSmsConversations() } returns testConversations
        
        // When
        smsRepository.loadConversations()
        val conversation = smsRepository.getConversation(phoneNumber)

        // Then
        assertThat(conversation).isNotNull()
        assertThat(conversation!!.phoneNumber).isEqualTo(phoneNumber)
        assertThat(conversation.messages).hasSize(2)
    }

    @Test
    fun givenPhoneNumberWithNoMessages_whenGetConversation_thenReturnNull() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        coEvery { mockSmsManager.normalizePhoneNumber(phoneNumber) } returns phoneNumber
        coEvery { mockSmsManager.getSmsConversations() } returns emptyList()

        // When
        smsRepository.loadConversations()
        val conversation = smsRepository.getConversation(phoneNumber)

        // Then
        assertThat(conversation).isNull()
    }

    @Test
    fun givenUpdatedData_whenRefresh_thenUpdateConversationsFlow() = runTest {
        // Given
        val testMessages :  List<SmsConversation> = listOf(
            SmsConversation("+1234567890", "TestUser-1", listOf(
                SmsMessage(1,"+1234567890", "Hello", 1L, SmsType.INBOX, true),
                SmsMessage(2, "+1234567890","Hi", 1L, SmsType.SENT, true)
            ), 1000L, 0)
        )
        coEvery { mockSmsManager.getSmsConversations() } returns testMessages

        // When
        smsRepository.refresh()

        // Then
        val conversations = smsRepository.conversations.first()
        assertThat(conversations).hasSize(1)
        assertThat(conversations[0].phoneNumber).isEqualTo("+1234567890")
    }

    @Test
    fun givenInitialEmptyState_whenDataUpdatedAndRefresh_thenConversationsFlowEmitsUpdatedData() = runTest {
        // Given - initial empty state
        coEvery { mockSmsManager.getSmsConversations() } returns emptyList()
        
        val initialConversations = smsRepository.conversations.first()
        assertThat(initialConversations).isEmpty()

        // When - add data and refresh
        val testMessages = listOf(
            SmsConversation("+1234567890", "TestUser-1", listOf(
                SmsMessage(1, "+1234567890", "Hello", 1L, SmsType.INBOX, true)
            ), 1000L, 0)
        )
        coEvery { mockSmsManager.getSmsConversations() } returns testMessages
        smsRepository.refresh()

        // Then
        val updatedConversations = smsRepository.conversations.first()
        assertThat(updatedConversations).hasSize(1)
    }




}
