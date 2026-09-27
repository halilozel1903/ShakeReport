package io.github.halilozel1903.shakereport

import io.github.halilozel1903.shakereport.core.ReportFormatter

/**
 * Reads the app's own recent logcat lines. Since Android 4.1 an app can read only its own
 * logs, and `--pid` (Android 7+) keeps the output to this process. Blocking: call it off
 * the main thread.
 */
internal object LogcatCollector {
    fun read(maxLines: Int): List<String> {
        if (maxLines <= 0) return emptyList()
        return try {
            val process = ProcessBuilder(
                "logcat",
                "-d",
                "-v", "threadtime",
                "-t", maxLines.toString(),
                "--pid=${android.os.Process.myPid()}",
            ).redirectErrorStream(true).start()
            val lines = process.inputStream.bufferedReader().useLines { ReportFormatter.takeLastLines(it, maxLines) }
            process.waitFor()
            lines
        } catch (e: Exception) {
            listOf("Could not read logcat: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
