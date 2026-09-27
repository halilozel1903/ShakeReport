package io.github.halilozel1903.shakereport.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeDetectorTest {
    private val g = ShakeDetector.STANDARD_GRAVITY

    /** Feeds a sample of [force] g along the x axis. */
    private fun ShakeDetector.jolt(force: Float, at: Long): Boolean = onAcceleration(force * g, 0f, 0f, at)

    @Test
    fun `gravity alone is one g`() {
        assertEquals(1f, ShakeDetector.gForce(0f, 0f, g), 0.0001f)
        assertEquals(1f, ShakeDetector.gForce(0f, g, 0f), 0.0001f)
    }

    @Test
    fun `phone at rest never shakes`() {
        val detector = ShakeDetector()
        repeat(500) { i -> assertFalse(detector.onAcceleration(0.3f, 0.2f, g, i * 20L)) }
        assertEquals(0, detector.pendingPeaks)
    }

    @Test
    fun `two strong peaks inside the window trigger`() {
        val detector = ShakeDetector(ShakeConfig(thresholdG = 2.5f, shakeCount = 2, windowMillis = 1_000))
        assertFalse(detector.jolt(3f, at = 0))
        assertEquals(1, detector.pendingPeaks)
        assertTrue(detector.jolt(3f, at = 400))
        assertEquals(0, detector.pendingPeaks)
    }

    @Test
    fun `peaks below the threshold are ignored`() {
        val detector = ShakeDetector(ShakeConfig(thresholdG = 2.5f))
        assertFalse(detector.jolt(2.4f, at = 0))
        assertFalse(detector.jolt(2.4f, at = 300))
        assertEquals(0, detector.pendingPeaks)
    }

    @Test
    fun `peaks too far apart do not add up`() {
        val detector = ShakeDetector(ShakeConfig(shakeCount = 2, windowMillis = 500))
        assertFalse(detector.jolt(3f, at = 0))
        assertFalse(detector.jolt(3f, at = 800))
        assertEquals(1, detector.pendingPeaks)
        assertTrue(detector.jolt(3f, at = 1_200))
    }

    @Test
    fun `samples of one jolt count once`() {
        val detector = ShakeDetector(ShakeConfig(shakeCount = 2, minPeakGapMillis = 100))
        assertFalse(detector.jolt(3f, at = 0))
        assertFalse(detector.jolt(3.2f, at = 20))
        assertFalse(detector.jolt(3.1f, at = 60))
        assertEquals(1, detector.pendingPeaks)
        assertTrue(detector.jolt(3f, at = 150))
    }

    @Test
    fun `cooldown suppresses repeated shakes`() {
        val detector = ShakeDetector(ShakeConfig(shakeCount = 1, minPeakGapMillis = 0, cooldownMillis = 2_000))
        assertTrue(detector.jolt(3f, at = 1_000))
        assertFalse(detector.jolt(3f, at = 1_500))
        assertFalse(detector.jolt(3f, at = 2_999))
        assertTrue(detector.jolt(3f, at = 3_000))
    }

    @Test
    fun `reset forgets peaks and cooldown`() {
        val detector = ShakeDetector(ShakeConfig(shakeCount = 3))
        detector.jolt(3f, at = 0)
        detector.jolt(3f, at = 200)
        assertEquals(2, detector.pendingPeaks)
        detector.reset()
        assertEquals(0, detector.pendingPeaks)
        assertFalse(detector.jolt(3f, at = 400))
    }

    @Test
    fun `firm preset needs three shakes`() {
        val detector = ShakeDetector(ShakeConfig.Firm)
        assertFalse(detector.jolt(3.5f, at = 0))
        assertFalse(detector.jolt(3.5f, at = 300))
        assertTrue(detector.jolt(3.5f, at = 600))
    }

    @Test
    fun `rejects invalid configuration`() {
        assertFailsWith<IllegalArgumentException> { ShakeConfig(thresholdG = 1f) }
        assertFailsWith<IllegalArgumentException> { ShakeConfig(shakeCount = 0) }
        assertFailsWith<IllegalArgumentException> { ShakeConfig(windowMillis = 0) }
        assertFailsWith<IllegalArgumentException> { ShakeConfig(cooldownMillis = -1) }
    }
}
