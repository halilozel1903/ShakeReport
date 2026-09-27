package io.github.halilozel1903.shakereport.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportFormatterTest {
    // 2026-09-27 10:15:30 UTC
    private val createdAt = 1_790_504_130_000L

    private val report = BugReport(
        description = "Total is wrong after applying a coupon\nSteps: add 3 items, apply SAVE10",
        deviceInfo = sampleDeviceInfo(),
        logs = (1..5).map { "line $it" },
        customData = mapOf("user" to "42", "flavor" to "beta"),
        createdAtMillis = createdAt,
        hasScreenshot = true,
    )

    @Test
    fun `subject uses the first line of the description`() {
        assertEquals("[Bug] Shop 2.3.0 – Total is wrong after applying a coupon", ReportFormatter().subject(report))
    }

    @Test
    fun `subject without a description or prefix`() {
        val formatter = ReportFormatter(subjectPrefix = "")
        assertEquals("Shop 2.3.0", formatter.subject(report.copy(description = "  \n ")))
    }

    @Test
    fun `long subjects are shortened`() {
        val formatter = ReportFormatter(maxSubjectLength = 20)
        val subject = formatter.subject(report.copy(description = "A".repeat(50)))
        assertEquals("[Bug] Shop 2.3.0 – " + "A".repeat(19) + "…", subject)
    }

    @Test
    fun `full report contains every section`() {
        val text = ReportFormatter().format(report)
        val expected = """
            [Bug] Shop 2.3.0 – Total is wrong after applying a coupon

            Description
            -----------
            Total is wrong after applying a coupon
            Steps: add 3 items, apply SAVE10

            Device
            ------
            Device:    Google Pixel 8
            Android:   16 (API 36)
            App:       Shop 2.3.0 (230)
            Package:   com.example.shop
            Locale:    en-US
            Screen:    1080 × 2400 px, 420 dpi
            Battery:   87%, charging
            Memory:    3.0 GB free of 8.0 GB
            App heap:  24.0 MB of 256.0 MB
            Reported:  2026-09-27 10:15:30 UTC

            Custom data
            -----------
            user:    42
            flavor:  beta

            Attachments
            -----------
            Screenshot: attached

            Logs (last 5 lines)
            -------------------
            line 1
            line 2
            line 3
            line 4
            line 5
        """.trimIndent() + "\n"
        assertEquals(expected, text)
    }

    @Test
    fun `logs can be left out`() {
        val text = ReportFormatter().format(report, includeLogs = false)
        assertFalse("Logs" in text)
        assertFalse("line 1" in text)
        assertTrue(text.endsWith("Screenshot: attached\n"))
    }

    @Test
    fun `keeps only the most recent log lines`() {
        val text = ReportFormatter(maxLogLines = 2).format(report)
        assertTrue("Logs (last 2 lines)" in text)
        assertFalse("line 3" in text)
        assertTrue(text.endsWith("line 4\nline 5\n"))
    }

    @Test
    fun `empty description and logs have placeholders`() {
        val text = ReportFormatter().format(report.copy(description = "", logs = emptyList(), customData = emptyMap()))
        assertTrue("(no description)" in text)
        assertTrue("(no log lines)" in text)
        assertFalse("Custom data" in text)
    }

    @Test
    fun `takeLastLines streams`() {
        val lines = generateSequence(1) { it + 1 }.take(10_000).map { "l$it" }
        assertEquals(listOf("l9999", "l10000"), ReportFormatter.takeLastLines(lines, 2))
        assertEquals(emptyList(), ReportFormatter.takeLastLines(lines, 0))
        assertEquals(listOf("a"), ReportFormatter.takeLastLines(listOf("a"), 5))
    }

    @Test
    fun `formats timestamps in UTC`() {
        assertEquals("2026-09-27 10:15:30 UTC", ReportFormatter.formatTimestamp(createdAt))
    }
}
