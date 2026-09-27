package io.github.halilozel1903.shakereport.core

/**
 * How hard and how often the device has to be shaken before a report opens.
 *
 * A "peak" is an accelerometer sample whose total force exceeds [thresholdG]. A shake is
 * detected when [shakeCount] peaks happen within [windowMillis]. Samples closer together
 * than [minPeakGapMillis] belong to the same jolt and count once.
 *
 * @property thresholdG Force in multiples of Earth's gravity. A phone lying still reads 1g.
 * @property shakeCount Peaks needed inside the window.
 * @property windowMillis Time window the peaks have to fall into.
 * @property minPeakGapMillis Minimum time between two counted peaks.
 * @property cooldownMillis Quiet period after a detected shake, so one shake opens one report.
 */
public data class ShakeConfig(
    val thresholdG: Float = 2.7f,
    val shakeCount: Int = 2,
    val windowMillis: Long = 1_000,
    val minPeakGapMillis: Long = 100,
    val cooldownMillis: Long = 2_000,
) {
    init {
        require(thresholdG > 1f) { "thresholdG must be > 1 (gravity alone is 1g)" }
        require(shakeCount >= 1) { "shakeCount must be >= 1" }
        require(windowMillis > 0) { "windowMillis must be > 0" }
        require(minPeakGapMillis >= 0) { "minPeakGapMillis must be >= 0" }
        require(cooldownMillis >= 0) { "cooldownMillis must be >= 0" }
    }

    public companion object {
        /** Reacts to a light shake. Handy on emulators and tablets. */
        public val Sensitive: ShakeConfig = ShakeConfig(thresholdG = 2.0f, shakeCount = 2)

        /** A deliberate shake, rarely triggered by walking or putting the phone down. */
        public val Default: ShakeConfig = ShakeConfig()

        /** Needs three strong shakes. For apps that are used while moving. */
        public val Firm: ShakeConfig = ShakeConfig(thresholdG = 3.3f, shakeCount = 3, windowMillis = 1_500)
    }
}
