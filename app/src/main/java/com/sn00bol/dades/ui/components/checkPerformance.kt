package com.sn00bol.dades.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Utility to determine if the device should be treated as a low-end device
 * to disable heavy UI effects like Gaussian Blur.
 */
object PerformanceUtils {
    /**
     * Checks if the device is low-end based on available RAM.
     * Threshold set to < 3GB to allow 4GB devices to use blur.
     */
    fun isLowEndDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Threshold: 3GB in bytes
        val totalMemoryGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
        
        // Also check if the device is in low RAM mode
        return totalMemoryGb < 3.0 || activityManager.isLowRamDevice
    }
}

/**
 * Composable helper to remember performance status.
 */
@Composable
fun rememberIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        PerformanceUtils.isLowEndDevice(context)
    }
}
