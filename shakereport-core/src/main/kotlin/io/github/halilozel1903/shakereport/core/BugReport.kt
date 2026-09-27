package io.github.halilozel1903.shakereport.core

/**
 * Everything a bug report contains, except the screenshot image itself.
 *
 * @property description What the user wrote.
 * @property deviceInfo Device and app details.
 * @property logs Recent log lines of the app, oldest first.
 * @property customData Extra key/value pairs supplied by the app (user id, build flavor, feature flags…).
 * @property createdAtMillis Wall clock time the report was started, in epoch milliseconds.
 * @property hasScreenshot Whether a screenshot is attached.
 */
public data class BugReport(
    val description: String,
    val deviceInfo: DeviceInfo,
    val logs: List<String> = emptyList(),
    val customData: Map<String, String> = emptyMap(),
    val createdAtMillis: Long,
    val hasScreenshot: Boolean = false,
)
