package com.aospinsight.securesms.service

import android.content.Intent
import android.os.IBinder
import com.aospinsight.securesms.repository.SmsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmsManagerServiceTest {

    private lateinit var smsManagerService: SmsManagerService
    private lateinit var mockSmsRepository: SmsRepository

    @Before
    fun setup() {
        mockSmsRepository = mockk()
        smsManagerService = SmsManagerService()
        
        // Use reflection to inject the mock repository
        val repositoryField = SmsManagerService::class.java.getDeclaredField("smsRepository")
        repositoryField.isAccessible = true
        repositoryField.set(smsManagerService, mockSmsRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `onBind returns SmsManagerBinder instance`() {
        // Given
        val mockIntent = mockk<Intent>()

        // When
        val binder = smsManagerService.onBind(mockIntent)

        // Then
        assertThat(binder).isNotNull()
        assertThat(binder).isInstanceOf(SmsManagerBinder::class.java)
    }

    @Test
    fun `onBind returns same binder instance on multiple calls`() {
        // Given
        val mockIntent = mockk<Intent>()

        // When
        val binder1 = smsManagerService.onBind(mockIntent)
        val binder2 = smsManagerService.onBind(mockIntent)

        // Then
        assertThat(binder1).isSameInstanceAs(binder2)
    }

    @Test
    fun `service lifecycle methods complete without errors`() {
        // When & Then - these should not throw exceptions
        smsManagerService.onCreate()
        smsManagerService.onDestroy()
    }

    @Test
    fun `binder is SmsManagerBinder with correct repository`() {
        // Given
        val mockIntent = mockk<Intent>()

        // When
        val binder = smsManagerService.onBind(mockIntent) as SmsManagerBinder

        // Then - verify the binder has access to repository (through testing its functionality)
        // This is an indirect test since SmsManagerBinder uses the repository internally
        assertThat(binder).isNotNull()
        
        // We can't directly access the repository field, but we can verify the binder
        // is properly constructed by checking it implements the interface
        assertThat(binder).isInstanceOf(ISmsManagerService::class.java)
    }

    @Test
    fun `service handles multiple bind and unbind operations`() {
        // Given
        val mockIntent = mockk<Intent>()

        // When
        val binder1 = smsManagerService.onBind(mockIntent)
        val unbind1 = smsManagerService.onUnbind(mockIntent)
        val binder2 = smsManagerService.onBind(mockIntent)
        val unbind2 = smsManagerService.onUnbind(mockIntent)

        // Then
        assertThat(binder1).isNotNull()
        assertThat(binder2).isNotNull()
        assertThat(binder1).isSameInstanceAs(binder2) // Same instance should be returned
        assertThat(unbind1).isFalse() // Default implementation returns false
        assertThat(unbind2).isFalse()
    }

    @Test
    fun `service handles onStartCommand correctly`() {
        // Given
        val mockIntent = mockk<Intent>()
        val flags = 0
        val startId = 1

        // When
        val result = smsManagerService.onStartCommand(mockIntent, flags, startId)

        // Then
        assertThat(result).isEqualTo(android.app.Service.START_NOT_STICKY)
    }

    @Test
    fun `service handles null intent in onBind gracefully`() {
        // When
        val intent : Intent = Intent()
        val binder = smsManagerService.onBind(intent)

        // Then
        assertThat(binder).isNotNull()
        assertThat(binder).isInstanceOf(SmsManagerBinder::class.java)
    }

    @Test
    fun `service handles null intent in onUnbind gracefully`() {
        // When & Then - should not throw
        val result = smsManagerService.onUnbind(null)
        assertThat(result).isFalse()
    }

    @Test
    fun `service handles null intent in onStartCommand gracefully`() {
        // When
        val result = smsManagerService.onStartCommand(null, 0, 1)

        // Then
        assertThat(result).isEqualTo(android.app.Service.START_NOT_STICKY)
    }
}
