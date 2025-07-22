package com.aospinsight.securesms.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class PermissionUtilsTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk()
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `hasSmsPermissions returns true when all permissions granted`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = PermissionUtils.hasSmsPermissions(mockContext)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasSmsPermissions returns false when READ_SMS permission denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = PermissionUtils.hasSmsPermissions(mockContext)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasSmsPermissions returns false when RECEIVE_SMS permission denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_GRANTED
        
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = PermissionUtils.hasSmsPermissions(mockContext)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasSmsPermissions returns false when all permissions denied`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) 
        } returns PackageManager.PERMISSION_DENIED
        
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECEIVE_SMS) 
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = PermissionUtils.hasSmsPermissions(mockContext)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasSmsPermissions calls ContextCompat for each permission`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, any()) 
        } returns PackageManager.PERMISSION_GRANTED

        // When
        PermissionUtils.hasSmsPermissions(mockContext)

        // Then
        verify { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) }
        verify { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.RECEIVE_SMS) }
    }

    @Test
    fun `hasSmsPermissions handles security exceptions gracefully`() {
        // Given
        every { 
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_SMS) 
        } throws SecurityException("Test exception")

        // When & Then - should not throw, should return false
        val result = PermissionUtils.hasSmsPermissions(mockContext)
        assertThat(result).isFalse()
    }

    @Test
    fun `permission constants have correct values`() {
        // This test ensures we're using the correct Android permission strings
        assertThat(Manifest.permission.READ_SMS).isEqualTo("android.permission.READ_SMS")
        assertThat(Manifest.permission.RECEIVE_SMS).isEqualTo("android.permission.RECEIVE_SMS")
    }
}
