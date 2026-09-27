package io.github.halilozel1903.shakereport

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ReportScreen(
    session: ReportSession,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSend: (ReportSender) -> Unit,
) {
    val image = session.image
    if (session.markupOpen && image != null) {
        BackHandler { session.markupOpen = false }
        MarkupScreen(session, image)
    } else {
        ReportForm(session, onClose, onShare, onSend)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportForm(
    session: ReportSession,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onSend: (ReportSender) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(session.error) {
        val error = session.error ?: return@LaunchedEffect
        session.error = null
        snackbar.showSnackbar(error)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shakereport_title)) },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text(stringResource(R.string.shakereport_cancel)) }
                },
            )
        },
        bottomBar = { SendBar(session, onShare, onSend) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenshotCard(session)
            OutlinedTextField(
                value = session.description,
                onValueChange = { session.description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.shakereport_description_label)) },
                placeholder = { Text(stringResource(R.string.shakereport_description_placeholder)) },
                minLines = 4,
            )
            if (session.config.includeLogs) LogsCard(session)
            DeviceInfoCard(session)
        }
    }
}

@Composable
private fun ScreenshotCard(session: ReportSession) {
    val image = session.image
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (image != null) {
                val shape = RoundedCornerShape(12.dp)
                AnnotatedScreenshot(
                    image = image,
                    strokes = session.strokes,
                    penColor = session.penColor,
                    drawingEnabled = false,
                    contentDescription = stringResource(R.string.shakereport_screenshot),
                    modifier = Modifier
                        .width(112.dp)
                        .clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        .clickable { session.markupOpen = true },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.shakereport_screenshot), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(
                        if (image != null) R.string.shakereport_screenshot_hint else R.string.shakereport_no_screenshot,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (image != null) {
                    FilledTonalButton(onClick = { session.markupOpen = true }) {
                        Text(stringResource(R.string.shakereport_mark_up))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = session.includeScreenshot,
                            onCheckedChange = { session.includeScreenshot = it },
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.shakereport_attach), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsCard(session: ReportSession) {
    val logs = session.logs
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.shakereport_logs), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (logs == null) {
                        stringResource(R.string.shakereport_logs_loading)
                    } else {
                        stringResource(R.string.shakereport_logs_count, logs.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = session.includeLogs, onCheckedChange = { session.includeLogs = it })
        }
    }
}

@Composable
private fun DeviceInfoCard(session: ReportSession) {
    val entries = remember(session) { session.deviceInfo.entries() + session.customData.toList() }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.shakereport_device_info),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            entries.forEach { (label, value) ->
                Row {
                    Text(
                        text = label,
                        modifier = Modifier.width(96.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SendBar(session: ReportSession, onShare: () -> Unit, onSend: (ReportSender) -> Unit) {
    val sender = session.config.sender
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sender != null) {
                OutlinedButton(onClick = onShare, enabled = !session.busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shakereport_share))
                }
                Button(onClick = { onSend(sender) }, enabled = !session.busy, modifier = Modifier.weight(1f)) {
                    if (session.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.shakereport_sending))
                    } else {
                        Text(sender.label, maxLines = 1)
                    }
                }
            } else {
                Button(onClick = onShare, enabled = !session.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.shakereport_share_report))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkupScreen(session: ReportSession, image: ImageBitmap) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shakereport_markup_title)) },
                actions = {
                    TextButton(
                        onClick = { session.strokes.removeLastOrNull() },
                        enabled = session.strokes.isNotEmpty(),
                    ) { Text(stringResource(R.string.shakereport_undo)) }
                    TextButton(
                        onClick = { session.strokes.clear() },
                        enabled = session.strokes.isNotEmpty(),
                    ) { Text(stringResource(R.string.shakereport_clear)) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PenColors.forEachIndexed { index, color ->
                        ColorSwatch(
                            color = color,
                            selected = color == session.penColor,
                            description = stringResource(R.string.shakereport_pen_color, index + 1),
                            onClick = { session.penColor = color },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { session.markupOpen = false }) {
                        Text(stringResource(R.string.shakereport_done))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.shakereport_markup_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnnotatedScreenshot(
                    image = image,
                    strokes = session.strokes,
                    penColor = session.penColor,
                    drawingEnabled = true,
                    contentDescription = stringResource(R.string.shakereport_screenshot),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .semantics { contentDescription = description }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
