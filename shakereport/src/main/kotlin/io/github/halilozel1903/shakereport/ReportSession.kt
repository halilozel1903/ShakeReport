package io.github.halilozel1903.shakereport

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.halilozel1903.shakereport.core.DeviceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The state of one report while the report screen is open. It lives outside the activity
 * so drawings, text and running sends survive configuration changes.
 */
internal class ReportSession(
    val screenshot: Bitmap?,
    val deviceInfo: DeviceInfo,
    val customData: Map<String, String>,
    val config: ShakeReportConfig,
    val createdAtMillis: Long,
    initialDescription: String,
    openMarkup: Boolean,
) {
    val image: ImageBitmap? = screenshot?.asImageBitmap()
    val strokes = mutableStateListOf<AnnotationStroke>()

    var description by mutableStateOf(initialDescription)
    var includeScreenshot by mutableStateOf(screenshot != null)
    var includeLogs by mutableStateOf(config.includeLogs)

    /** `null` while logcat is still being read. */
    var logs by mutableStateOf<List<String>?>(if (config.includeLogs) null else emptyList())
    var markupOpen by mutableStateOf(openMarkup && screenshot != null)
    var penColor by mutableStateOf(PenColors.first())

    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var shareIntent by mutableStateOf<Intent?>(null)
    var finished by mutableStateOf(false)

    /** Writes the files and prepares the share sheet; the activity starts it. Main thread. */
    fun share(context: Context, chooserTitle: String) = runAction(context) { payload ->
        shareIntent = ReportExporter.shareIntent(context, payload, config.emailRecipients, chooserTitle)
    }

    /** Writes the files and hands them to [sender]. Main thread. */
    fun send(context: Context, sender: ReportSender) = runAction(context) { payload ->
        withContext(Dispatchers.IO) { sender.send(context, payload) }
        Toast.makeText(context, R.string.shakereport_sent, Toast.LENGTH_SHORT).show()
        finished = true
    }

    private fun runAction(context: Context, block: suspend (ReportPayload) -> Unit) {
        if (busy) return
        busy = true
        ShakeReport.scope.launch {
            try {
                block(ReportExporter.export(context, this@ReportSession))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = context.getString(R.string.shakereport_send_failed, e.message ?: e.javaClass.simpleName)
            } finally {
                busy = false
            }
        }
    }
}

/** One pen stroke; points are normalized to the image (0..1 on both axes). */
internal class AnnotationStroke(val color: Color, start: Offset) {
    val points = mutableStateListOf(start)
}

internal val PenColors: List<Color> = listOf(
    Color(0xFFE53935),
    Color(0xFFFFC107),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
)

/** Stroke width as a fraction of the image width, the same on screen and in the exported PNG. */
internal const val STROKE_WIDTH_FRACTION: Float = 0.012f

/** At most one report is open at a time. Main thread only. */
internal object ReportSessionHolder {
    var current: ReportSession? = null
}
