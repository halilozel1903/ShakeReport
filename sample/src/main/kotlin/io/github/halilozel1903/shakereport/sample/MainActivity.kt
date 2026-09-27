package io.github.halilozel1903.shakereport.sample

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.shakereport.ShakeReport

class MainActivity : ComponentActivity() {
    /** Where the (buggy) total is drawn, used to put demo markup around it. */
    private var totalBounds: Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        logCartActivity()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                CartScreen(
                    onTotalPositioned = { totalBounds = it },
                    onReportClick = { ShakeReport.show(this) },
                )
            }
        }

        // Used by scripts/screenshots.sh: `--es scene report` or `--es scene annotate` opens the
        // report screen directly with a marked-up screenshot of this screen.
        val scene = intent.getStringExtra("scene")
        if (savedInstanceState == null && (scene == "report" || scene == "annotate")) {
            window.decorView.postDelayed({ openDemoReport(openMarkup = scene == "annotate") }, 1_000)
        }
    }

    private fun openDemoReport(openMarkup: Boolean) {
        if (isFinishing) return
        ShakeReport.captureScreenshot(this) { screenshot ->
            ShakeReport.show(
                activity = this,
                screenshot = screenshot?.let(::drawDemoMarkup),
                description = "The total adds the SAVE10 discount instead of subtracting it. " +
                    "Expected $108.90, got $131.10.",
                openMarkup = openMarkup,
            )
        }
    }

    /** Circles the wrong total like a tester would with the pen. */
    private fun drawDemoMarkup(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val bounds = totalBounds
            ?: Rect(bitmap.width * 0.55f, bitmap.height * 0.45f, bitmap.width * 0.95f, bitmap.height * 0.5f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE53935.toInt()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = bitmap.width * 0.012f
        }
        val padX = bitmap.width * 0.05f
        val padY = bitmap.width * 0.035f
        android.graphics.Canvas(bitmap).drawOval(
            RectF(bounds.left - padX, bounds.top - padY, bounds.right + padX, bounds.bottom + padY),
            paint,
        )
        return bitmap
    }

    private fun logCartActivity() {
        Log.d("CartViewModel", "Loaded 3 items from cache")
        Log.i("Coupons", "Applied coupon SAVE10 (-10%)")
        Log.d("PriceCalculator", "subtotal=121.00 discount=12.10")
        Log.w("PriceCalculator", "Discount applied with a positive sign: +12.10")
        Log.d("Checkout", "total=131.10 currency=USD")
    }
}

private data class CartItem(val name: String, val detail: String, val price: String)

private val items = listOf(
    CartItem("Wireless headphones", "Midnight blue", "$79.00"),
    CartItem("USB-C cable", "2 × $9.00", "$18.00"),
    CartItem("Phone case", "Clear", "$24.00"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartScreen(onTotalPositioned: (Rect) -> Unit, onReportClick: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Cart") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Found a bug? Shake your phone.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ShakeReport captures this screen, device info and recent logs, " +
                            "then lets you mark up the screenshot and send it.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onReportClick) { Text("Report a bug") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    item.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(item.price, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    HorizontalDivider()
                    PriceRow("Subtotal", "$121.00")
                    PriceRow("Coupon SAVE10", "−$12.10")
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        // The bug: the discount was added instead of subtracted.
                        Text(
                            "$131.10",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.onGloballyPositioned { onTotalPositioned(it.boundsInWindow()) },
                        )
                    }
                }
            }

            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Checkout") }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
