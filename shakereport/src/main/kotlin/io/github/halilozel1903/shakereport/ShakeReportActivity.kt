package io.github.halilozel1903.shakereport

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect

/** Hosts the report screen. Declared in the library manifest; started by [ShakeReport]. */
internal class ShakeReportActivity : ComponentActivity() {
    private var session: ReportSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val session = ReportSessionHolder.current
        if (session == null) {
            // Process was recreated: the screenshot is gone, so there is nothing to show.
            finish()
            return
        }
        this.session = session

        setContent {
            ShakeReportTheme {
                LaunchedEffect(session.shareIntent) {
                    val intent = session.shareIntent ?: return@LaunchedEffect
                    session.shareIntent = null
                    try {
                        startActivity(intent)
                        finish()
                    } catch (e: ActivityNotFoundException) {
                        session.error = getString(R.string.shakereport_share_failed)
                    }
                }
                LaunchedEffect(session.finished) {
                    if (session.finished) finish()
                }
                ReportScreen(
                    session = session,
                    onClose = { finish() },
                    onShare = { session.share(applicationContext, getString(R.string.shakereport_share_title)) },
                    onSend = { sender -> session.send(applicationContext, sender) },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && ReportSessionHolder.current === session) {
            ReportSessionHolder.current = null
        }
    }
}
