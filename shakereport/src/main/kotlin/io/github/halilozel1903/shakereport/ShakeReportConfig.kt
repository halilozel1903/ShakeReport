package io.github.halilozel1903.shakereport

import io.github.halilozel1903.shakereport.core.ReportFormatter
import io.github.halilozel1903.shakereport.core.ShakeConfig

/**
 * Settings for [ShakeReport.install].
 *
 * @property shake How hard and how often to shake. See [ShakeConfig.Sensitive], [ShakeConfig.Default]
 *   and [ShakeConfig.Firm].
 * @property sender Optional custom destination (Slack, Jira, your backend…). The report screen then
 *   shows a button for it next to the share sheet. `null` shows the share sheet only.
 * @property emailRecipients Pre-filled "To" addresses when the report is shared to an e-mail app.
 * @property subjectPrefix Put in front of every subject, for example `[Beta]`.
 * @property includeLogs Collect the app's recent logcat lines. The user can still turn them off per report.
 * @property maxLogLines How many recent log lines to collect.
 * @property customData Extra key/value pairs added to every report (user id, flavor, feature flags…).
 *   Called on the main thread when a report starts.
 */
public class ShakeReportConfig(
    public val shake: ShakeConfig = ShakeConfig.Default,
    public val sender: ReportSender? = null,
    public val emailRecipients: List<String> = emptyList(),
    public val subjectPrefix: String = "[Bug]",
    public val includeLogs: Boolean = true,
    public val maxLogLines: Int = 300,
    public val customData: () -> Map<String, String> = { emptyMap() },
) {
    init {
        require(maxLogLines in 0..10_000) { "maxLogLines must be between 0 and 10000" }
    }

    internal val formatter: ReportFormatter
        get() = ReportFormatter(subjectPrefix = subjectPrefix, maxLogLines = maxLogLines)
}
