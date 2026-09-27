package io.github.halilozel1903.shakereport.core

import kotlin.math.sqrt

/**
 * Turns raw accelerometer samples into shake events.
 *
 * Pure logic with no Android dependency: feed it the values of an accelerometer
 * `SensorEvent` (m/s², gravity included) and a monotonic timestamp in milliseconds.
 * Not thread safe; call it from a single thread (the sensor thread or the main thread).
 *
 * ```kotlin
 * val detector = ShakeDetector(ShakeConfig.Default)
 * if (detector.onAcceleration(x, y, z, timestampMillis)) openReport()
 * ```
 */
public class ShakeDetector(public val config: ShakeConfig = ShakeConfig.Default) {
    private val peaks = ArrayDeque<Long>()
    private var lastPeakAtMillis: Long? = null
    private var lastShakeAtMillis: Long? = null

    /** Peaks counted in the current window. Useful for debugging and tests. */
    public val pendingPeaks: Int get() = peaks.size

    /**
     * Processes one sample and returns `true` when it completes a shake.
     *
     * @param x Acceleration on the x axis in m/s².
     * @param y Acceleration on the y axis in m/s².
     * @param z Acceleration on the z axis in m/s².
     * @param timestampMillis A monotonic clock, for example `SensorEvent.timestamp / 1_000_000`.
     */
    public fun onAcceleration(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        val lastShake = lastShakeAtMillis
        if (lastShake != null && timestampMillis - lastShake < config.cooldownMillis) return false
        if (gForce(x, y, z) < config.thresholdG) return false

        val lastPeak = lastPeakAtMillis
        if (lastPeak != null && timestampMillis - lastPeak < config.minPeakGapMillis) return false
        lastPeakAtMillis = timestampMillis

        peaks.addLast(timestampMillis)
        while (peaks.isNotEmpty() && timestampMillis - peaks.first() > config.windowMillis) {
            peaks.removeFirst()
        }

        if (peaks.size >= config.shakeCount) {
            peaks.clear()
            lastPeakAtMillis = null
            lastShakeAtMillis = timestampMillis
            return true
        }
        return false
    }

    /** Forgets all peaks and the cooldown, for example when listening restarts. */
    public fun reset() {
        peaks.clear()
        lastPeakAtMillis = null
        lastShakeAtMillis = null
    }

    public companion object {
        /** Standard gravity in m/s², the same value as `SensorManager.GRAVITY_EARTH`. */
        public const val STANDARD_GRAVITY: Float = 9.80665f

        /** Total acceleration of a sample, in multiples of Earth's gravity. */
        public fun gForce(x: Float, y: Float, z: Float): Float =
            sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY
    }
}
