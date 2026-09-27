package io.github.halilozel1903.shakereport

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import io.github.halilozel1903.shakereport.core.BugReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object ReportExporter {
    private const val DIRECTORY = "shakereport"

    /** The FileProvider authority declared in the library manifest. */
    fun authority(context: Context): String = "${context.packageName}.shakereport.fileprovider"

    /** Snapshots the session on the calling (main) thread, then writes the files on IO. */
    suspend fun export(context: Context, session: ReportSession): ReportPayload {
        val strokes = session.strokes.map { stroke -> StrokeSnapshot(stroke.color.toArgb(), stroke.points.toList()) }
        val screenshot = session.screenshot.takeIf { session.includeScreenshot }
        val includeLogs = session.includeLogs
        val report = BugReport(
            description = session.description.trim(),
            deviceInfo = session.deviceInfo,
            logs = if (includeLogs) session.logs.orEmpty() else emptyList(),
            customData = session.customData,
            createdAtMillis = session.createdAtMillis,
            hasScreenshot = screenshot != null,
        )
        val formatter = session.config.formatter

        return withContext(Dispatchers.IO) {
            val root = File(context.cacheDir, DIRECTORY)
            // Keep only the current report on disk.
            root.listFiles()?.forEach { it.deleteRecursively() }
            val directory = File(root, "report-${session.createdAtMillis}").apply { mkdirs() }

            val screenshotFile = screenshot?.let { source ->
                File(directory, "screenshot.png").also { file ->
                    val rendered = renderAnnotations(source, strokes)
                    file.outputStream().use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (rendered !== source) rendered.recycle()
                }
            }
            val text = formatter.format(report, includeLogs = includeLogs)
            val reportFile = File(directory, "report.txt").apply { writeText(text) }

            ReportPayload(
                report = report,
                subject = formatter.subject(report),
                summary = formatter.format(report, includeLogs = false),
                text = text,
                screenshotFile = screenshotFile,
                reportFile = reportFile,
            )
        }
    }

    fun shareIntent(context: Context, payload: ReportPayload, recipients: List<String>, chooserTitle: String): Intent {
        val authority = authority(context)
        val uris = ArrayList<Uri>()
        payload.screenshotFile?.let { uris += FileProvider.getUriForFile(context, authority, it) }
        uris += FileProvider.getUriForFile(context, authority, payload.reportFile)

        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (payload.screenshotFile != null) "*/*" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, payload.subject)
            putExtra(Intent.EXTRA_TEXT, payload.summary)
            if (recipients.isNotEmpty()) putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            // ClipData carries the read grant through the chooser to the target app.
            clipData = ClipData.newRawUri("ShakeReport", uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, chooserTitle)
    }

    /** Draws [strokes] onto a copy of [source]; returns [source] itself when there is nothing to draw. */
    fun renderAnnotations(source: Bitmap, strokes: List<StrokeSnapshot>): Bitmap {
        if (strokes.isEmpty()) return source
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = output.width.toFloat()
        val height = output.height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = width * STROKE_WIDTH_FRACTION
        }
        strokes.forEach { stroke ->
            paint.color = stroke.argb
            val points = stroke.points
            if (points.isEmpty()) return@forEach
            if (points.size == 1) {
                canvas.drawPoint(points[0].x * width, points[0].y * height, paint)
            } else {
                val path = Path().apply {
                    moveTo(points[0].x * width, points[0].y * height)
                    for (i in 1 until points.size) lineTo(points[i].x * width, points[i].y * height)
                }
                canvas.drawPath(path, paint)
            }
        }
        return output
    }
}

internal class StrokeSnapshot(val argb: Int, val points: List<Offset>)
