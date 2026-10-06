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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTextButton
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.theme.MotoHubColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    MhScreen(
        title = motoHubText("Application logs"),
        subtitle = motoHubText("Persistent events from Wi-Fi, T-Box, mirroring, encoder, and Android Auto."),
        onBack = onBack,
        scrollable = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MhSecondaryButton(motoHubText("Copy log"), onCopy, Modifier.weight(1f))
            MhSecondaryButton(motoHubText("Share"), onShare, Modifier.weight(1f))
            MhTextButton(motoHubText("Clear"), onClick = { confirmingClear = true }, color = MaterialTheme.colorScheme.error)
        }
        Text(
            motoHubText("%1\$d entries · newest first", events.size),
            modifier = Modifier.padding(horizontal = 4.dp),
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
