package com.aospinsight.securesms.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling SMS permissions
 */
class PermissionUtils {
    
    companion object {
        const val SMS_PERMISSION_REQUEST_CODE = 123
        
        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        /**
         * Check if all SMS permissions are granted
         */
        @JvmStatic
        fun hasSmsPermissions(context: Context): Boolean {
            return SMS_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        
        /**
         * Request SMS permissions using new ActivityResultLauncher
         */
        @JvmStatic
        fun requestSmsPermissions(activity: Activity, launcher: ActivityResultLauncher<Array<String>>) {
            launcher.launch(SMS_PERMISSIONS)
        }
        
        /**
         * Request SMS permissions using deprecated method (kept for compatibility)
         * 
         * @deprecated Use requestSmsPermissions(activity: Activity, launcher: ActivityResultLauncher<Array<String>>) instead.
         * 
         * Migration example:
         * ```
         * // Old way:
         * PermissionUtils.requestSmsPermissions(activity)
         * 
         * // New way:
         * val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
         *     // Handle permission results
         * }
         * PermissionUtils.requestSmsPermissions(activity, launcher)
         * ```
         */
        @Deprecated(
            message = "Use requestSmsPermissions with ActivityResultLauncher instead",
            replaceWith = ReplaceWith(
                "requestSmsPermissions(activity, launcher)",
                "androidx.activity.result.contract.ActivityResultContracts"
            ),
            level = DeprecationLevel.WARNING
        )
        @JvmStatic
        fun requestSmsPermissions(activity: Activity) {
            // Check if permissions are already granted to avoid unnecessary requests
            if (hasSmsPermissions(activity)) {
                return
            }
            
            ActivityCompat.requestPermissions(
                activity,
                SMS_PERMISSIONS,
                SMS_PERMISSION_REQUEST_CODE
            )
        }
        
        /**
         * Check if we should show rationale for SMS permissions
         */
        @JvmStatic
        fun shouldShowSmsPermissionRationale(activity: Activity): Boolean {
            return SMS_PERMISSIONS.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
        }
        
        /**
         * Handle permission result
         */
        @JvmStatic
        fun handlePermissionResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
            onPermissionGranted: () -> Unit,
            onPermissionDenied: () -> Unit
        ) {
            if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
                val allGranted = grantResults.isNotEmpty() && grantResults.all { result ->
                    result == PackageManager.PERMISSION_GRANTED
                }
                
                if (allGranted) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }
    }
}
