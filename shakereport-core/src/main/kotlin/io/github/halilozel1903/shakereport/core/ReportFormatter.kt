package io.github.halilozel1903.shakereport.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Turns a [BugReport] into plain text for e-mails, chat messages and issue trackers.
 *
 * @property subjectPrefix Put in front of every subject, for example `[Bug]` or `[Beta]`.
 * @property maxLogLines Only the most recent lines are kept.
 * @property maxSubjectLength The description part of the subject is cut to this length.
 */
public class ReportFormatter(
    public val subjectPrefix: String = "[Bug]",
    public val maxLogLines: Int = 300,
    public val maxSubjectLength: Int = 60,
) {
    init {
        require(maxLogLines >= 0) { "maxLogLines must be >= 0" }
        require(maxSubjectLength >= 10) { "maxSubjectLength must be >= 10" }
    }

    /** A one-line subject: prefix, app, version and the start of the description. */
    public fun subject(report: BugReport): String {
        val info = report.deviceInfo
        val firstLine = report.description.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        val summary = firstLine?.let { " – " + ellipsize(it, maxSubjectLength) } ?: ""
        val prefix = if (subjectPrefix.isBlank()) "" else "${subjectPrefix.trim()} "
        return "$prefix${info.appName} ${info.appVersionName}$summary"
    }

    /**
     * The full report as text.
     *
     * @param includeLogs Append the log section. Leave it out for short chat messages
     *   and attach the full report as a file instead.
     */
    public fun format(report: BugReport, includeLogs: Boolean = true): String = buildString {
        appendLine(subject(report))
        appendLine()

        section("Description")
        appendLine(report.description.trim().ifEmpty { "(no description)" })
        appendLine()

        section("Device")
        val entries = report.deviceInfo.entries() + ("Reported" to formatTimestamp(report.createdAtMillis))
        appendAligned(entries)

        if (report.customData.isNotEmpty()) {
            appendLine()
            section("Custom data")
            appendAligned(report.customData.entries.map { it.key to it.value })
        }

        appendLine()
        section("Attachments")
        appendLine(if (report.hasScreenshot) "Screenshot: attached" else "Screenshot: none")

        if (includeLogs) {
            val lines = takeLastLines(report.logs, maxLogLines)
            appendLine()
            section("Logs (last ${lines.size} lines)")
            if (lines.isEmpty()) appendLine("(no log lines)") else lines.forEach { appendLine(it) }
        }
    }.trimEnd() + "\n"

    private fun StringBuilder.section(title: String) {
        appendLine(title)
        appendLine("-".repeat(title.length))
    }

    private fun StringBuilder.appendAligned(entries: List<Pair<String, String>>) {
        val width = entries.maxOfOrNull { it.first.length } ?: 0
        entries.forEach { (label, value) -> appendLine("${(label + ":").padEnd(width + 3)}$value") }
    }

    public companion object {
        /** Keeps the last [max] entries of [lines]; streams, so huge inputs are fine. */
        public fun takeLastLines(lines: Sequence<String>, max: Int): List<String> {
            require(max >= 0) { "max must be >= 0" }
            if (max == 0) return emptyList()
            val buffer = ArrayDeque<String>(minOf(max, 1024))
            for (line in lines) {
                if (buffer.size == max) buffer.removeFirst()
                buffer.addLast(line)
            }
            return buffer.toList()
        }

        /** Keeps the last [max] entries of [lines]. */
        public fun takeLastLines(lines: List<String>, max: Int): List<String> =
            takeLastLines(lines.asSequence(), max)

        /** `2026-09-27 14:03:09 UTC`. Uses no `java.time`, so it works on every Android version. */
        public fun formatTimestamp(epochMillis: Long): String {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.format(Date(epochMillis))
        }

        internal fun ellipsize(text: String, max: Int): String =
            if (text.length <= max) text else text.take(max - 1).trimEnd() + "…"
    }
}
