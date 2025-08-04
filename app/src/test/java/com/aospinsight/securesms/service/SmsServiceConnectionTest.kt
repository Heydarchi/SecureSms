package com.aospinsight.securesms.service

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.lifecycle.LifecycleOwner
import com.aospinsight.securesms.repository.SmsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SmsServiceConnectionTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockContext: Context

    @RelaxedMockK
    private lateinit var mockLifecycleOwner: LifecycleOwner

    private lateinit var smsServiceConnection: SmsServiceConnection

    @Before
    fun setUp() {
        every { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) } returns true
        smsServiceConnection = SmsServiceConnection(mockContext)
    }

    @Test
    fun `givenOnConnectedAndOnDisconnected_whenBindService_thenBindsToService`() {
        // Given
        val onConnected: (ISmsManagerService) -> Unit = {}
        val onDisconnected: () -> Unit = {}

        // When
        smsServiceConnection.bindService(onConnected, onDisconnected)

        // Then
        verify { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) }
    }

    @Test
    fun `givenServiceIsBound_whenUnbindService_thenUnbindsFromService`() {
        // Given
        smsServiceConnection.bindService()
        // Simulate service connected
        val serviceConnection = slot<ServiceConnection>()
        verify { mockContext.bindService(any<Intent>(), capture(serviceConnection), any<Int>()) }

        val mockSmsRepository = mockk<SmsRepository>(relaxed = true)
        val mockBinder = spyk(SmsManagerBinder(mockSmsRepository))
        serviceConnection.captured.onServiceConnected(mockk(), mockBinder)


        // When
        smsServiceConnection.unbindService()

        // Then
        verify { mockContext.unbindService(any()) }
        assertThat(smsServiceConnection.isBound()).isFalse()
    }

    @Test
    fun `givenServiceNotBound_whenOnStart_thenBindsToService`() {
        // When
        smsServiceConnection.onStart(mockLifecycleOwner)

        // Then
        verify { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) }
    }

    @Test
    fun `givenServiceIsBound_whenOnStop_thenUnbindsFromService`() {
        // Given
        smsServiceConnection.bindService()
        // Simulate service connected
        val serviceConnection = slot<ServiceConnection>()
        verify { mockContext.bindService(any<Intent>(), capture(serviceConnection), any<Int>()) }

        val mockSmsRepository = mockk<SmsRepository>(relaxed = true)
        val mockBinder = spyk(SmsManagerBinder(mockSmsRepository))
        serviceConnection.captured.onServiceConnected(mockk(), mockBinder)

        // When
        smsServiceConnection.onStop(mockLifecycleOwner)

        // Then
        verify { mockContext.unbindService(any()) }
    }

    @Test
    fun `givenServiceIsBound_whenWithService_thenExecutesOperation`() = runTest {
        // Given
        val mockSmsRepository = mockk<SmsRepository>(relaxed = true)
        val operation: suspend (ISmsManagerService) -> String = { "result" }

        smsServiceConnection.bindService()
        val serviceConnection = slot<ServiceConnection>()
        verify { mockContext.bindService(any<Intent>(), capture(serviceConnection), any<Int>()) }

        val mockBinder = spyk(SmsManagerBinder(mockSmsRepository))
        serviceConnection.captured.onServiceConnected(mockk(), mockBinder)

        // When
        val result = smsServiceConnection.withService(operation)

        // Then
        assertThat(result).isEqualTo("result")
    }

    @Test
    fun `givenServiceNotBound_whenWithService_thenDoesNotExecuteOperation`() = runTest {
        // Given
        val operation: suspend (ISmsManagerService) -> String = { "result" }

        // When
        val result = smsServiceConnection.withService(operation)

        // Then
        assertThat(result).isNull()
    }
}
