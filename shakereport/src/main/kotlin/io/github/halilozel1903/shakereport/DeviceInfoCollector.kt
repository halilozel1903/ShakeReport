package io.github.halilozel1903.shakereport

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import io.github.halilozel1903.shakereport.core.DeviceInfo

internal object DeviceInfoCollector {
    fun collect(activity: Activity): DeviceInfo {
        val context = activity.applicationContext
        val packageInfo = runCatching { context.packageInfo() }.getOrNull()
        val (width, height) = screenSize(activity)

        val battery = context.getSystemService(BatteryManager::class.java)
        val batteryLevel = battery
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

        val memory = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory)
        val runtime = Runtime.getRuntime()

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            appName = context.applicationInfo.loadLabel(context.packageManager).toString(),
            packageName = context.packageName,
            appVersionName = packageInfo?.versionName ?: "unknown",
            appVersionCode = packageInfo?.let(::versionCodeOf) ?: 0L,
            locale = activity.resources.configuration.locales[0].toLanguageTag(),
            screenWidthPx = width,
            screenHeightPx = height,
            densityDpi = activity.resources.displayMetrics.densityDpi,
            batteryPercent = batteryLevel,
            isCharging = battery?.isCharging,
            availableMemoryBytes = memory.availMem,
            totalMemoryBytes = memory.totalMem,
            isLowMemory = memory.lowMemory,
            appHeapUsedBytes = runtime.totalMemory() - runtime.freeMemory(),
            appHeapMaxBytes = runtime.maxMemory(),
        )
    }

    private fun screenSize(activity: Activity): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            legacyScreenSize(activity)
        }

    @Suppress("DEPRECATION")
    private fun legacyScreenSize(activity: Activity): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun Context.packageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            legacyPackageInfo()
        }

    @Suppress("DEPRECATION")
    private fun Context.legacyPackageInfo(): PackageInfo = packageManager.getPackageInfo(packageName, 0)

    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else legacyVersionCode(info)

    @Suppress("DEPRECATION")
    private fun legacyVersionCode(info: PackageInfo): Long = info.versionCode.toLong()
}
