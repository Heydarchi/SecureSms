package com.aospinsight.securesms.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.aospinsight.securesms.model.SmsConversation
import com.aospinsight.securesms.model.SmsMessage
import com.aospinsight.securesms.model.SmsType
import com.aospinsight.securesms.repository.SmsRepository
import com.aospinsight.securesms.service.ISmsManagerService
import com.aospinsight.securesms.service.SmsManagerBinder
import com.aospinsight.securesms.service.SmsManagerService
import com.aospinsight.securesms.service.SmsUpdateListener
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SmsServiceIntegrationTest {

    private lateinit var mockContext: Context
    private lateinit var mockRepository: SmsRepository
    private lateinit var smsManagerService: SmsManagerService
    private lateinit var serviceConnection: TestServiceConnection

    @Before
    fun setup() {
        mockContext = mockk()
        mockRepository = mockk()
        
        smsManagerService = SmsManagerService()
        
        // Mock permission checks for the service context
        every { mockContext.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
        
        // Manually initialize the binder field using reflection to avoid complex service lifecycle
        val binderField = SmsManagerService::class.java.getDeclaredField("binder")
        binderField.isAccessible = true
        binderField.set(smsManagerService, SmsManagerBinder(mockRepository))
        
        // Also inject mock repository using reflection
        val repositoryField = SmsManagerService::class.java.getDeclaredField("smsRepository")
        repositoryField.isAccessible = true
        repositoryField.set(smsManagerService, mockRepository)
        
        serviceConnection = TestServiceConnection()
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

    class TestServiceConnection : ServiceConnection {
        var service: ISmsManagerService? = null
        var isConnected = false

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder as ISmsManagerService
            isConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isConnected = false
        }
    }

    @Test
    fun givenMockRepositoryWithConversations_whenCompleteServiceBindingAndDataRetrieval_thenFlowWorksCorrectly() = runTest {
        // Given
        val expectedConversations = listOf(
            createTestConversation("+1234567890", "John Doe"),
            createTestConversation("+0987654321", "Jane Smith")
        )
        coEvery { mockRepository.getAllConversations() } returns expectedConversations

        // When - Bind to service
        val intent = Intent(mockContext, SmsManagerService::class.java)
        val binder = smsManagerService.onBind(intent)
        serviceConnection.onServiceConnected(ComponentName("test", "test"), binder)

        // Then - Service should be connected
        assertThat(serviceConnection.isConnected).isTrue()
        assertThat(serviceConnection.service).isNotNull()

        // When - Retrieve conversations through service
        val conversations = serviceConnection.service!!.getLatestMessagesFromEachContact()

        // Then - Data should be retrieved correctly
        assertThat(conversations).isEqualTo(expectedConversations)
        coVerify { mockRepository.getAllConversations() }
    }

    @Test
    fun `service listener registration and notification flow works`() = runTest {
        // Given
        val mockListener = mockk<SmsUpdateListener>(relaxed = true)
        coEvery { mockRepository.refresh() } just Runs

        // When - Bind to service and register listener
        val binder = smsManagerService.onBind(Intent()) as SmsManagerBinder
        binder.registerSmsListener(mockListener)

        // When - Trigger refresh
        binder.refreshSmsData()

        // Then - Listener should be notified
        verify { mockListener.onSmsDataRefreshed() }
        coVerify { mockRepository.refresh() }
    }

    @Test
    fun `complete message retrieval for specific contact works`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val expectedMessages = listOf(
            createTestSmsMessage(1L, phoneNumber, "Message 1", 1000L),
            createTestSmsMessage(2L, phoneNumber, "Message 2", 2000L)
        )
        coEvery { mockRepository.getMessagesForPhoneNumber(phoneNumber) } returns expectedMessages

        // When - Bind to service and get messages
        val binder = smsManagerService.onBind(Intent()) as ISmsManagerService
        val messages = binder.getMessagesFromContact(phoneNumber)

        // Then
        assertThat(messages).isEqualTo(expectedMessages)
        coVerify { mockRepository.getMessagesForPhoneNumber(phoneNumber) }
    }

    @Test
    fun `conversation retrieval flow works end-to-end`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val expectedConversation = createTestConversation(phoneNumber, "John Doe", 3)
        coEvery { mockRepository.getConversation(phoneNumber) } returns expectedConversation

        // When - Complete flow from service binding to conversation retrieval
        val binder = smsManagerService.onBind(Intent()) as ISmsManagerService
        val conversation = binder.getConversation(phoneNumber)

        // Then
        assertThat(conversation).isEqualTo(expectedConversation)
        assertThat(conversation?.phoneNumber).isEqualTo(phoneNumber)
        assertThat(conversation?.displayName).isEqualTo("John Doe")
        assertThat(conversation?.messages?.size).isEqualTo(3)
        coVerify { mockRepository.getConversation(phoneNumber) }
    }

    @Test
    fun `error handling works throughout the complete flow`() = runTest {
        // Given - Repository throws exception
        coEvery { mockRepository.getAllConversations() } throws RuntimeException("Database error")

        // When - Try to get conversations through service
        val binder = smsManagerService.onBind(Intent()) as ISmsManagerService
        val conversations = binder.getLatestMessagesFromEachContact()

        // Then - Should handle error gracefully
        assertThat(conversations).isEmpty()
        coVerify { mockRepository.getAllConversations() }
    }

    @Test
    fun `multiple service bindings return same binder instance`() {
        // When
        val binder1 = smsManagerService.onBind(Intent())
        val binder2 = smsManagerService.onBind(Intent())

        // Then
        assertThat(binder1).isSameInstanceAs(binder2)
        assertThat(binder1).isInstanceOf(SmsManagerBinder::class.java)
        assertThat(binder2).isInstanceOf(SmsManagerBinder::class.java)
    }

    @Test
    fun `service lifecycle with multiple listeners works correctly`() = runTest {
        // Given
        val listener1 = mockk<SmsUpdateListener>(relaxed = true)
        val listener2 = mockk<SmsUpdateListener>(relaxed = true)
        val binder = smsManagerService.onBind(Intent()) as SmsManagerBinder

        // When - Register multiple listeners
        binder.registerSmsListener(listener1)
        binder.registerSmsListener(listener2)

        // When - Trigger notifications
        binder.notifyNewSmsReceived("+1234567890", "Test message", System.currentTimeMillis())
        binder.notifyDataRefreshed()

        // Then - All listeners should be notified
        verify { listener1.onNewSmsReceived(any(), any(), any()) }
        verify { listener2.onNewSmsReceived(any(), any(), any()) }
        verify { listener1.onSmsDataRefreshed() }
        verify { listener2.onSmsDataRefreshed() }

        // When - Unregister one listener
        binder.unregisterSmsListener(listener1)
        binder.notifyDataRefreshed()

        // Then - Only remaining listener should be notified
        verify(exactly = 1) { listener1.onSmsDataRefreshed() } // Only once from before
        verify(exactly = 2) { listener2.onSmsDataRefreshed() } // Twice total
    }

    @Test
    fun `service handles null and empty data gracefully throughout flow`() = runTest {
        // Given - Repository returns null/empty data
        coEvery { mockRepository.getAllConversations() } returns emptyList()
        coEvery { mockRepository.getConversation(any()) } returns null
        coEvery { mockRepository.getMessagesForPhoneNumber(any()) } returns emptyList()

        // When - Use service methods
        val binder = smsManagerService.onBind(Intent()) as ISmsManagerService
        val conversations = binder.getLatestMessagesFromEachContact()
        val conversation = binder.getConversation("+1234567890")
        val messages = binder.getMessagesFromContact("+1234567890")

        // Then - Should handle gracefully
        assertThat(conversations).isEmpty()
        assertThat(conversation).isNull()
        assertThat(messages).isEmpty()
    }
}
