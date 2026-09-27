package io.github.halilozel1903.shakereport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize

/**
 * The screenshot with the user's pen strokes on top. With [drawingEnabled], dragging a finger
 * adds a stroke in [penColor]. Keeps the image's aspect ratio inside the given constraints.
 */
@Composable
internal fun AnnotatedScreenshot(
    image: ImageBitmap,
    strokes: SnapshotStateList<AnnotationStroke>,
    penColor: Color,
    drawingEnabled: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(modifier.aspectRatio(image.width.toFloat() / image.height.toFloat())) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        val input = if (drawingEnabled) {
            Modifier.pointerInput(penColor) {
                var current: AnnotationStroke? = null
                detectDragGestures(
                    onDragStart = { start: Offset ->
                        val stroke = AnnotationStroke(penColor, start.normalizedIn(size))
                        strokes.add(stroke)
                        current = stroke
                    },
                    onDragEnd = { current = null },
                    onDragCancel = { current = null },
                    onDrag = { change, _ ->
                        change.consume()
                        current?.points?.add(change.position.normalizedIn(size))
                    },
                )
            }
        } else {
            Modifier
        }
        Canvas(Modifier.fillMaxSize().then(input)) {
            strokes.forEach { drawAnnotation(it) }
        }
    }
}

private fun Offset.normalizedIn(size: IntSize): Offset = Offset(
    x = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
    y = (y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f),
)

private fun Offset.scaledTo(size: Size): Offset = Offset(x * size.width, y * size.height)

private fun DrawScope.drawAnnotation(stroke: AnnotationStroke) {
    val points = stroke.points
    if (points.isEmpty()) return
    val width = size.width * STROKE_WIDTH_FRACTION
    if (points.size == 1) {
        drawCircle(color = stroke.color, radius = width / 2, center = points[0].scaledTo(size))
        return
    }
    val path = Path()
    val first = points[0].scaledTo(size)
    path.moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val point = points[i].scaledTo(size)
        path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
