package com.aospinsight.securesms.service

import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import com.aospinsight.securesms.repository.SmsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmsManagerBinderTest {

    private lateinit var mockRepository: SmsRepository
    private lateinit var smsManagerBinder: SmsManagerBinder
    private lateinit var mockListener: SmsUpdateListener

    @Before
    fun setup() {
        mockRepository = mockk()
        smsManagerBinder = SmsManagerBinder(mockRepository)
        mockListener = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createTestSmsMessage(
        id: Long = 1L,
        phoneNumber: String = "+1234567890",
        message: String = "Test message",
        timestamp: Long = System.currentTimeMillis(),
        type: SmsType = SmsType.INBOX,
        isRead: Boolean = true
    ) = SmsMessage(id, phoneNumber, message, timestamp, type, isRead)

    private fun createTestConversation(
        phoneNumber: String = "+1234567890",
        displayName: String? = "John Doe",
        messageCount: Int = 1
    ) = SmsConversation(
        phoneNumber = phoneNumber,
        displayName = displayName,
        messages = (1..messageCount).map { createTestSmsMessage(it.toLong()) },
        lastMessageTimestamp = System.currentTimeMillis(),
        unreadCount = 0
    )

    @Test
    fun givenMockRepository_whenGetLatestMessagesFromEachContact_thenReturnConversationsFromRepository() = runTest {
        // Given
        val expectedConversations = listOf(
            createTestConversation("+1234567890", "John Doe"),
            createTestConversation("+0987654321", "Jane Smith")
        )
        coEvery { mockRepository.getAllConversations() } returns expectedConversations

        // When
        val result = smsManagerBinder.getLatestMessagesFromEachContact()

        // Then
        assertThat(result).isEqualTo(expectedConversations)
        coVerify { mockRepository.getAllConversations() }
    }

    @Test
    fun givenRepositoryThrowsException_whenGetLatestMessagesFromEachContact_thenReturnEmptyList() = runTest {
        // Given
        coEvery { mockRepository.getAllConversations() } throws RuntimeException("Database error")

        // When
        val result = smsManagerBinder.getLatestMessagesFromEachContact()

        // Then
        assertThat(result).isEmpty()
        coVerify { mockRepository.getAllConversations() }
    }

    @Test
    fun givenPhoneNumber_whenGetMessagesFromContact_thenReturnMessagesFromRepository() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val expectedMessages = listOf(
            createTestSmsMessage(1L, phoneNumber, "Message 1"),
            createTestSmsMessage(2L, phoneNumber, "Message 2")
        )
        coEvery { mockRepository.getMessagesForPhoneNumber(phoneNumber) } returns expectedMessages

        // When
        val result = smsManagerBinder.getMessagesFromContact(phoneNumber)

        // Then
        assertThat(result).isEqualTo(expectedMessages)
        coVerify { mockRepository.getMessagesForPhoneNumber(phoneNumber) }
    }

    @Test
    fun givenRepositoryThrowsException_whenGetMessagesFromContact_thenReturnEmptyList() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        coEvery { mockRepository.getMessagesForPhoneNumber(phoneNumber) } throws RuntimeException("Error")

        // When
        val result = smsManagerBinder.getMessagesFromContact(phoneNumber)

        // Then
        assertThat(result).isEmpty()
        coVerify { mockRepository.getMessagesForPhoneNumber(phoneNumber) }
    }

    @Test
    fun givenPhoneNumber_whenGetConversation_thenReturnConversationFromRepository() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val expectedConversation = createTestConversation(phoneNumber)
        coEvery { mockRepository.getConversation(phoneNumber) } returns expectedConversation

        // When
        val result = smsManagerBinder.getConversation(phoneNumber)

        // Then
        assertThat(result).isEqualTo(expectedConversation)
        coVerify { mockRepository.getConversation(phoneNumber) }
    }

    @Test
    fun givenRepositoryThrowsException_whenGetConversation_thenReturnNull() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        coEvery { mockRepository.getConversation(phoneNumber) } throws RuntimeException("Error")

        // When
        val result = smsManagerBinder.getConversation(phoneNumber)

        // Then
        assertThat(result).isNull()
        coVerify { mockRepository.getConversation(phoneNumber) }
    }

    @Test
    fun givenRegisteredListener_whenRefreshSmsData_thenCallRepositoryRefreshAndNotifyListeners() = runTest {
        // Given
        coEvery { mockRepository.refresh() } just Runs
        smsManagerBinder.registerSmsListener(mockListener)

        // When
        smsManagerBinder.refreshSmsData()

        // Then
        coVerify { mockRepository.refresh() }
        verify { mockListener.onSmsDataRefreshed() }
    }

    @Test
    fun givenRepositoryException_whenRefreshSmsData_thenHandleExceptionGracefully() = runTest {
        // Given
        coEvery { mockRepository.refresh() } throws RuntimeException("Refresh error")

        // When & Then (should not throw)
        smsManagerBinder.refreshSmsData()

        // Verify repository was called
        coVerify { mockRepository.refresh() }
    }

    @Test
    fun givenNewListener_whenRegisterSmsListener_thenAddListenerWhenNotAlreadyPresent() {
        // When
        smsManagerBinder.registerSmsListener(mockListener)

        // Then - verify listener is registered by triggering notification
        smsManagerBinder.notifyDataRefreshed()
        verify { mockListener.onSmsDataRefreshed() }
    }

    @Test
    fun givenDuplicateListener_whenRegisterSmsListener_thenDoNotAddDuplicateListener() {
        // Given
        smsManagerBinder.registerSmsListener(mockListener)

        // When - register same listener again
        smsManagerBinder.registerSmsListener(mockListener)

        // Then - listener should only be called once
        smsManagerBinder.notifyDataRefreshed()
        verify(exactly = 1) { mockListener.onSmsDataRefreshed() }
    }

    @Test
    fun givenRegisteredListener_whenUnregisterSmsListener_thenRemoveListener() {
        // Given
        smsManagerBinder.registerSmsListener(mockListener)

        // When
        smsManagerBinder.unregisterSmsListener(mockListener)

        // Then - listener should not be called
        smsManagerBinder.notifyDataRefreshed()
        verify(exactly = 0) { mockListener.onSmsDataRefreshed() }
    }

    @Test
    fun givenNonExistentListener_whenUnregisterSmsListener_thenHandleGracefully() {
        // When & Then (should not throw)
        smsManagerBinder.unregisterSmsListener(mockListener)
    }

    @Test
    fun givenRegisteredListeners_whenNotifyNewSmsReceived_thenCallAllRegisteredListeners() {
        // Given
        val mockListener2 = mockk<SmsUpdateListener>(relaxed = true)
        smsManagerBinder.registerSmsListener(mockListener)
        smsManagerBinder.registerSmsListener(mockListener2)

        val phoneNumber = "+1234567890"
        val message = "Test message"
        val timestamp = System.currentTimeMillis()

        // When
        smsManagerBinder.notifyNewSmsReceived(phoneNumber, message, timestamp)

        // Then
        verify { mockListener.onNewSmsReceived(phoneNumber, message, timestamp) }
        verify { mockListener2.onNewSmsReceived(phoneNumber, message, timestamp) }
    }

    @Test
    fun givenListenerException_whenNotifyNewSmsReceived_thenHandleExceptionGracefully() {
        // Given
        val mockListener2 = mockk<SmsUpdateListener>(relaxed = true)
        every { mockListener.onNewSmsReceived(any(), any(), any()) } throws RuntimeException("Listener error")
        
        smsManagerBinder.registerSmsListener(mockListener)
        smsManagerBinder.registerSmsListener(mockListener2)

        // When & Then (should not throw and should call other listeners)
        smsManagerBinder.notifyNewSmsReceived("+1234567890", "Test", System.currentTimeMillis())

        // Verify both listeners were called
        verify { mockListener.onNewSmsReceived(any(), any(), any()) }
        verify { mockListener2.onNewSmsReceived(any(), any(), any()) }
    }

    @Test
    fun givenRegisteredListeners_whenNotifyDataRefreshed_thenCallAllRegisteredListeners() {
        // Given
        val mockListener2 = mockk<SmsUpdateListener>(relaxed = true)
        smsManagerBinder.registerSmsListener(mockListener)
        smsManagerBinder.registerSmsListener(mockListener2)

        // When
        smsManagerBinder.notifyDataRefreshed()

        // Then
        verify { mockListener.onSmsDataRefreshed() }
        verify { mockListener2.onSmsDataRefreshed() }
    }

    @Test
    fun givenListenerException_whenNotifyDataRefreshed_thenHandleExceptionGracefully() {
        // Given
        val mockListener2 = mockk<SmsUpdateListener>(relaxed = true)
        every { mockListener.onSmsDataRefreshed() } throws RuntimeException("Listener error")
        
        smsManagerBinder.registerSmsListener(mockListener)
        smsManagerBinder.registerSmsListener(mockListener2)

        // When & Then (should not throw and should call other listeners)
        smsManagerBinder.notifyDataRefreshed()

        // Verify both listeners were called
        verify { mockListener.onSmsDataRefreshed() }
        verify { mockListener2.onSmsDataRefreshed() }
    }
}
