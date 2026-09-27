package io.github.halilozel1903.shakereport.core

internal fun sampleDeviceInfo(): DeviceInfo = DeviceInfo(
    manufacturer = "Google",
    model = "Pixel 8",
    androidVersion = "16",
    sdkInt = 36,
    appName = "Shop",
    packageName = "com.example.shop",
    appVersionName = "2.3.0",
    appVersionCode = 230,
    locale = "en-US",
    screenWidthPx = 1080,
    screenHeightPx = 2400,
    densityDpi = 420,
    batteryPercent = 87,
    isCharging = true,
    availableMemoryBytes = 3L * 1024 * 1024 * 1024,
    totalMemoryBytes = 8L * 1024 * 1024 * 1024,
    appHeapUsedBytes = 24L * 1024 * 1024,
    appHeapMaxBytes = 256L * 1024 * 1024,
)
