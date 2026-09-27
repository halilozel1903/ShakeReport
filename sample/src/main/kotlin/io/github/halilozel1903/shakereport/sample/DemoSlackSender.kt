package io.github.halilozel1903.shakereport.sample

import android.content.Context
import android.util.Log
import io.github.halilozel1903.shakereport.ReportPayload
import io.github.halilozel1903.shakereport.ReportSender
import kotlinx.coroutines.delay

/**
 * Shows the [ReportSender] API without a real backend. A real sender would post
 * [ReportPayload.summary] to a Slack webhook and upload [ReportPayload.screenshotFile]
 * (see the README for a complete example).
 */
class DemoSlackSender : ReportSender {
    override val label: String = "Send to Slack"

    override suspend fun send(context: Context, payload: ReportPayload) {
        delay(1_200) // pretend to talk to the network
        Log.i("DemoSlackSender", "Sent \"${payload.subject}\" with ${payload.report.logs.size} log lines")
    }
}
