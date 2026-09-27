package io.github.halilozel1903.shakereport

import android.content.Context
import io.github.halilozel1903.shakereport.core.BugReport
import java.io.File

/**
 * Sends a finished report somewhere other than the share sheet, for example a Slack
 * webhook, a Jira issue or your own backend.
 *
 * ```kotlin
 * class SlackSender(private val webhookUrl: String) : ReportSender {
 *     override val label = "Send to Slack"
 *     override suspend fun send(context: Context, payload: ReportPayload) {
 *         postJson(webhookUrl, """{"text": ${JSONObject.quote(payload.summary)}}""")
 *     }
 * }
 * ```
 */
public interface ReportSender {
    /** Text of the send button on the report screen. */
    public val label: String get() = "Send"

    /**
     * Delivers [payload]. Runs on a background thread (`Dispatchers.IO`), so blocking network
     * calls are fine. Throw to signal a failure: the message is shown to the user and the
     * report screen stays open so they can retry or share instead.
     *
     * @param context The application context.
     */
    public suspend fun send(context: Context, payload: ReportPayload)
}

/**
 * A report ready to be sent.
 *
 * @property report The structured report (description, device info, logs, custom data).
 * @property subject A one-line subject such as `[Bug] Shop 2.3.0 – Total is wrong`.
 * @property summary The report as text without the log lines; good for chat messages.
 * @property text The full report as text, including logs when the user kept them.
 * @property screenshotFile The screenshot with the user's markup as PNG, or `null` when none is attached.
 * @property reportFile [text] written to a `report.txt` file, handy as an attachment.
 */
public class ReportPayload internal constructor(
    public val report: BugReport,
    public val subject: String,
    public val summary: String,
    public val text: String,
    public val screenshotFile: File?,
    public val reportFile: File,
)
