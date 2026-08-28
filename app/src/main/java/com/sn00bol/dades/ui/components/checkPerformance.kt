package com.sn00bol.dades.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
object PerformanceUtils {
    fun isLowEndDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMemoryGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
        return totalMemoryGb < 3.0 || activityManager.isLowRamDevice
    }
}
@Composable
fun rememberIsLowEndDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        PerformanceUtils.isLowEndDevice(context)
    }
}
