package io.github.halilozel1903.shakereport.core

import java.util.Locale

/**
 * A snapshot of the device and app at the moment a report was started.
 *
 * Collected on Android by the `shakereport` module; plain data here so it can be
 * formatted and tested anywhere.
 */
public data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val appName: String,
    val packageName: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val locale: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val densityDpi: Int,
    /** Battery level from 0 to 100, or `null` when unknown. */
    val batteryPercent: Int? = null,
    /** Whether the device is charging, or `null` when unknown. */
    val isCharging: Boolean? = null,
    val availableMemoryBytes: Long = 0,
    val totalMemoryBytes: Long = 0,
    val isLowMemory: Boolean = false,
    /** Java heap used by the app. */
    val appHeapUsedBytes: Long = 0,
    /** Java heap the app may grow to. */
    val appHeapMaxBytes: Long = 0,
) {
    /** "Google Pixel 8" instead of "Google Google Pixel 8" or "google Pixel 8". */
    public val deviceName: String
        get() {
            val maker = manufacturer.trim().replaceFirstChar { it.titlecase(Locale.US) }
            val cleanModel = model.trim()
            return when {
                maker.isEmpty() -> cleanModel
                cleanModel.startsWith(maker, ignoreCase = true) ->
                    cleanModel.replaceFirstChar { it.titlecase(Locale.US) }
                else -> "$maker $cleanModel"
            }
        }

    /** Human readable label and value pairs, in display order. */
    public fun entries(): List<Pair<String, String>> = buildList {
        add("Device" to deviceName)
        add("Android" to "$androidVersion (API $sdkInt)")
        add("App" to "$appName $appVersionName ($appVersionCode)")
        add("Package" to packageName)
        add("Locale" to locale)
        add("Screen" to "$screenWidthPx × $screenHeightPx px, $densityDpi dpi")
        batteryPercent?.let { level ->
            val charging = when (isCharging) {
                true -> ", charging"
                false -> ", not charging"
                null -> ""
            }
            add("Battery" to "$level%$charging")
        }
        if (totalMemoryBytes > 0) {
            val low = if (isLowMemory) " (low memory)" else ""
            add("Memory" to "${formatBytes(availableMemoryBytes)} free of ${formatBytes(totalMemoryBytes)}$low")
        }
        if (appHeapMaxBytes > 0) {
            add("App heap" to "${formatBytes(appHeapUsedBytes)} of ${formatBytes(appHeapMaxBytes)}")
        }
    }

    public companion object {
        /** Formats a byte count with binary units: `512 B`, `1.5 KB`, `3.2 GB`. */
        public fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val units = listOf("KB", "MB", "GB", "TB")
            var value = bytes / 1024.0
            var unit = 0
            while (value >= 1024 && unit < units.lastIndex) {
                value /= 1024
                unit++
            }
            return String.format(Locale.US, "%.1f %s", value, units[unit])
        }
    }
}
