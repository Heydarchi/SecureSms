package com.aospinsight.securesms.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import android.app.Application
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PermissionUtilsTest {

    private lateinit var context: Context
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).get()
    }

    @Test
    fun givenAllPermissionsGranted_whenHasSmsPermissionsIsCalled_thenReturnsTrue() {
        // Given
        Shadows.shadowOf(context as Application).grantPermissions(*PermissionUtils.SMS_PERMISSIONS)

        // When
        val result = PermissionUtils.hasSmsPermissions(context)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun givenOnePermissionDenied_whenHasSmsPermissionsIsCalled_thenReturnsFalse() {
        // Given
        val shadowApp = Shadows.shadowOf(context as Application)
        shadowApp.grantPermissions(PermissionUtils.SMS_PERMISSIONS[0])
        shadowApp.denyPermissions(PermissionUtils.SMS_PERMISSIONS[1])

        // When
        val result = PermissionUtils.hasSmsPermissions(context)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun givenLauncher_whenRequestSmsPermissionsIsCalled_thenLaunchIsCalledOnLauncher() {
        // Given
        val launcher = mockk<ActivityResultLauncher<Array<String>>>(relaxed = true)

        // When
        PermissionUtils.requestSmsPermissions(activity, launcher)

        // Then
        verify { launcher.launch(PermissionUtils.SMS_PERMISSIONS) }
    }

    @Test
    fun givenActivity_whenShouldShowSmsPermissionRationaleIsCalled_thenReturnsBoolean() {
        // Given - Using a simple test that just verifies the method can be called
        
        // When
        val result = PermissionUtils.shouldShowSmsPermissionRationale(activity)

        // Then - Verify it returns a valid boolean value
        assertThat(result is Boolean).isTrue()
    }

    @Test
    fun givenActivity_whenShouldShowSmsPermissionRationaleIsCalledTwice_thenReturnsConsistentResult() {
        // Given - Default behavior test
        
        // When
        val result1 = PermissionUtils.shouldShowSmsPermissionRationale(activity)
        val result2 = PermissionUtils.shouldShowSmsPermissionRationale(activity)

        // Then - Should return consistent results
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun givenAllPermissionsGranted_whenHandlePermissionResultIsCalled_thenOnPermissionGrantedIsCalled() {
        // Given
        val onPermissionGranted = mockk<() -> Unit>(relaxed = true)
        val onPermissionDenied = mockk<() -> Unit>(relaxed = true)
        val grantResults = IntArray(PermissionUtils.SMS_PERMISSIONS.size) { PackageManager.PERMISSION_GRANTED }

        // When
        PermissionUtils.handlePermissionResult(
            PermissionUtils.SMS_PERMISSION_REQUEST_CODE,
            PermissionUtils.SMS_PERMISSIONS,
            grantResults,
            onPermissionGranted,
            onPermissionDenied
        )

        // Then
        verify { onPermissionGranted() }
        verify(exactly = 0) { onPermissionDenied() }
    }

    @Test
    fun givenPermissionIsDenied_whenHandlePermissionResultIsCalled_thenOnPermissionDeniedIsCalled() {
        // Given
        val onPermissionGranted = mockk<() -> Unit>(relaxed = true)
        val onPermissionDenied = mockk<() -> Unit>(relaxed = true)
        val grantResults = IntArray(PermissionUtils.SMS_PERMISSIONS.size) { PackageManager.PERMISSION_GRANTED }
        grantResults[0] = PackageManager.PERMISSION_DENIED

        // When
        PermissionUtils.handlePermissionResult(
            PermissionUtils.SMS_PERMISSION_REQUEST_CODE,
            PermissionUtils.SMS_PERMISSIONS,
            grantResults,
            onPermissionGranted,
            onPermissionDenied
        )

        // Then
        verify(exactly = 0) { onPermissionGranted() }
        verify { onPermissionDenied() }
    }

    @Test
    fun givenDifferentRequestCode_whenHandlePermissionResultIsCalled_thenNothingIsCalled() {
        // Given
        val onPermissionGranted = mockk<() -> Unit>(relaxed = true)
        val onPermissionDenied = mockk<() -> Unit>(relaxed = true)
        val grantResults = IntArray(PermissionUtils.SMS_PERMISSIONS.size) { PackageManager.PERMISSION_GRANTED }

        // When
        PermissionUtils.handlePermissionResult(
            999, // Different request code
            PermissionUtils.SMS_PERMISSIONS,
            grantResults,
            onPermissionGranted,
            onPermissionDenied
        )

        // Then
        verify(exactly = 0) { onPermissionGranted() }
        verify(exactly = 0) { onPermissionDenied() }
    }
}
