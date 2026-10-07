// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.motohub.android.session.LogLevel
import io.motohub.android.session.ProjectionEvent
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTopBarAction
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.theme.MotoHubColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ApplicationLogScreen(
    events: List<ProjectionEvent>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // Asked first: clearing also truncates the log file, so there is nothing to undo it with.
    var confirmingClear by remember { mutableStateOf(false) }
    // Copy answers on its own button: the glyph turns into a lime check for a moment. Android 13
    // and later confirm a copy themselves, so this is the app's one acknowledgement there.
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MILLIS)
            copied = false
        }
    }

    MhScreen(
        title = motoHubText("Application logs"),
        subtitle = motoHubText("What MOTO-HUB recorded on this phone"),
        onBack = onBack,
        scrollable = false,
        // Copy and Share act on the whole log, so they sit with the title, not above the entries.
        actions = {
            MhTopBarAction(
                icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                contentDescription = if (copied) motoHubText("Log copied") else motoHubText("Copy log"),
                active = copied,
                onClick = {
                    onCopy()
                    copied = true
                }
            )
            // No spacer: the 48 dp targets already leave 8 dp between the 40 dp circles.
            MhTopBarAction(Icons.Rounded.Share, motoHubText("Share"), onShare)
        }
    ) {
        Text(
            motoHubText("%1\$d entries · newest first", events.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (events.isEmpty()) {
                item {
                    Text(
                        motoHubText("No diagnostic events have been recorded."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(events.asReversed(), key = { it.sequence }) {
                LogEntryCard(it)
            }
            // At the end, the way Reset actions ends Button mapping: red, and a sheet asks first.
            if (events.isNotEmpty()) {
                item {
                    MhListGroup(Modifier.padding(top = 16.dp)) {
                        MhListRow(
                            title = motoHubText("Clear log"),
                            icon = Icons.Rounded.DeleteSweep,
                            iconTint = MotoHubColors.Error,
                            titleColor = MaterialTheme.colorScheme.error,
                            showChevron = false,
                            onClick = { confirmingClear = true }
                        )
                    }
                }
            }
        }
    }

    if (confirmingClear) {
        val count = events.size
        MhSheet(
            onDismiss = { confirmingClear = false },
            title = motoHubText("Clear the log?"),
            body = motoHubText("This deletes all %1\$d entries from this phone. It can't be undone.", count),
            primaryLabel = motoHubText("Clear"),
            onPrimary = {
                onClear()
                MotoHubSnackbar.success(context, motoHubText("Log cleared"))
            },
            secondaryLabel = motoHubText("Cancel"),
            primaryStyle = MhActionStyle.DESTRUCTIVE
        )
    }
}

@Composable
private fun LogEntryCard(event: ProjectionEvent) {
    // Only problems get a colour; an ordinary line stays grey instead of lime.
    val levelColor = when (event.level) {
        LogLevel.DEBUG -> MotoHubColors.TextTertiary
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.WARNING -> MotoHubColors.Warning
        LogLevel.ERROR -> MotoHubColors.Error
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(levelColor, CircleShape))
                Text(
                    event.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                LOG_TIME_FORMAT.format(Date(event.timestampMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            event.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            // A handful of very long entries (raw CLIENT_INFO JSON, hex dumps under
            // verbose T-Box logging) laying out in full every time they scroll into view
            // was heavy enough to hang the screen - this list is for scanning, not
            // reading a full JSON blob; Copy/Share still export the untruncated text.
            maxLines = 20,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val LOG_TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private const val COPIED_MILLIS = 1_500L
