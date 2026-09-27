package io.github.halilozel1903.shakereport.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DeviceInfoTest {
    @Test
    fun `device name avoids a duplicated manufacturer`() {
        val info = sampleDeviceInfo()
        assertEquals("Google Pixel 8", info.deviceName)
        assertEquals("Samsung SM-S921B", info.copy(manufacturer = "samsung", model = "SM-S921B").deviceName)
        assertEquals("Google Pixel 8", info.copy(model = "google Pixel 8").deviceName)
        assertEquals("Pixel 8", info.copy(manufacturer = "").deviceName)
    }

    @Test
    fun `entries are human readable`() {
        val entries = sampleDeviceInfo().entries().toMap()
        assertEquals("16 (API 36)", entries["Android"])
        assertEquals("Shop 2.3.0 (230)", entries["App"])
        assertEquals("1080 × 2400 px, 420 dpi", entries["Screen"])
        assertEquals("87%, charging", entries["Battery"])
        assertEquals("3.0 GB free of 8.0 GB", entries["Memory"])
        assertEquals("24.0 MB of 256.0 MB", entries["App heap"])
    }

    @Test
    fun `unknown values are left out`() {
        val entries = sampleDeviceInfo()
            .copy(batteryPercent = null, totalMemoryBytes = 0, appHeapMaxBytes = 0)
            .entries()
            .toMap()
        assertFalse("Battery" in entries)
        assertFalse("Memory" in entries)
        assertFalse("App heap" in entries)
    }

    @Test
    fun `low memory is flagged`() {
        val entries = sampleDeviceInfo().copy(isLowMemory = true).entries().toMap()
        assertEquals("3.0 GB free of 8.0 GB (low memory)", entries["Memory"])
    }

    @Test
    fun `formats bytes`() {
        assertEquals("512 B", DeviceInfo.formatBytes(512))
        assertEquals("1.5 KB", DeviceInfo.formatBytes(1536))
        assertEquals("1.0 MB", DeviceInfo.formatBytes(1024 * 1024))
        assertEquals("2.0 TB", DeviceInfo.formatBytes(2L * 1024 * 1024 * 1024 * 1024))
    }
}
