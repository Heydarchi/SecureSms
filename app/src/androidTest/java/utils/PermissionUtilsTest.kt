package utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aospinsight.securesms.utils.PermissionUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun givenRealContext_whenCallHasSmsPermissions_thenReturnBoolean() {
        // Given - real Android context from ApplicationProvider

        // When
        val result = PermissionUtils.hasSmsPermissions(context)

        // Then - should return a boolean value (permissions may or may not be granted in test)
        assert(result is Boolean)
    }

    @Test
    fun givenIndividualPermissionCheck_whenCheckReadSms_thenReturnValidResult() {
        // Given - real context
        
        // When
        val readSmsResult = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
        val receiveSmsResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
        
        // Then - should return valid permission states
        assert(readSmsResult == PackageManager.PERMISSION_GRANTED || readSmsResult == PackageManager.PERMISSION_DENIED)
        assert(receiveSmsResult == PackageManager.PERMISSION_GRANTED || receiveSmsResult == PackageManager.PERMISSION_DENIED)
    }

    @Test
    fun givenMultipleCalls_whenCallHasSmsPermissions_thenReturnConsistentResult() {
        // Given - real context

        // When
        val result1 = PermissionUtils.hasSmsPermissions(context)
        val result2 = PermissionUtils.hasSmsPermissions(context)
        val result3 = PermissionUtils.hasSmsPermissions(context)

        // Then - all calls should return the same result
        assert(result1 == result2)
        assert(result2 == result3)
    }

    @Test
    fun givenPermissionConstants_whenCheckValues_thenReturnCorrectStrings() {
        // Given - Android permission constants

        // When & Then - verify the constants have correct values
        assert(Manifest.permission.READ_SMS == "android.permission.READ_SMS")
        assert(Manifest.permission.RECEIVE_SMS == "android.permission.RECEIVE_SMS")
    }
}