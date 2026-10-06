// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics

import io.motohub.android.i18n.motoHubText

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.motohub.android.session.ProjectionEvent
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhTone

@Composable
fun NetworkDiagnosticsScreen(
    state: NetworkDiagnosticsUiState,
    projectionEvents: List<ProjectionEvent>,
    onRunTests: () -> Unit,
    onBack: () -> Unit
) {
    var showEvents by rememberSaveable { mutableStateOf(false) }
    var showNetworks by rememberSaveable { mutableStateOf(false) }

    MhScreen(
        title = motoHubText("Network diagnostics"),
        subtitle = motoHubText("Check T-Box and cellular routes without starting a VPN or changing the default network."),
        onBack = onBack
    ) {
        MhPrimaryButton(
            if (state.running) motoHubText("Running tests…") else motoHubText("Run network tests"),
            onClick = onRunTests,
            loading = state.running
        )

        MhSectionHeader(motoHubText("Result"))
        MhListGroup {
            Text(
                state.conclusion,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        MhListGroup {
            state.checks.forEach { check ->
                MhListRow(title = check.title, subtitle = check.detail, trailing = { CheckStatusChip(check.status) })
            }
        }

        DetailSection(
            title = motoHubText("Session events"),
            detail = if (projectionEvents.isEmpty()) {
                motoHubText("No events recorded")
            } else {
                motoHubText("%1\$d events", projectionEvents.size)
            },
            expanded = showEvents,
            onToggle = { showEvents = !showEvents }
        ) {
            if (projectionEvents.isEmpty()) {
                DetailLine(motoHubText("Start a projection to populate the log."))
            } else {
                projectionEvents.asReversed().forEach { event ->
                    val time = DateFormat.format("HH:mm:ss", event.timestampMillis)
                    DetailLine(motoHubText("%1\$s  %2\$s: %3\$s", time, event.source, event.message), mono = true)
                }
            }
        }

        DetailSection(
            title = motoHubText("Detected networks"),
            detail = if (state.networkSnapshot.isEmpty()) {
                motoHubText("Run the test first")
            } else {
                motoHubText("%1\$d routes", state.networkSnapshot.size)
            },
            expanded = showNetworks,
            onToggle = { showNetworks = !showNetworks }
        ) {
            if (state.networkSnapshot.isEmpty()) {
                DetailLine(motoHubText("No network snapshot available."))
            } else {
                state.networkSnapshot.forEach { line -> DetailLine(line, mono = true) }
            }
        }
    }
}

@Composable
private fun CheckStatusChip(status: NetworkDiagnosticStatus) {
    val (label, tone) = when (status) {
        NetworkDiagnosticStatus.PASSED -> motoHubText("Passed") to MhTone.LIVE
        NetworkDiagnosticStatus.FAILED -> motoHubText("Failed") to MhTone.ERROR
        NetworkDiagnosticStatus.RUNNING -> motoHubText("Running") to MhTone.PROGRESS
        NetworkDiagnosticStatus.SKIPPED -> motoHubText("Skipped") to MhTone.NEUTRAL
        NetworkDiagnosticStatus.NOT_RUN -> motoHubText("Not run") to MhTone.NEUTRAL
    }
    MhStatusChip(label, tone)
}

/** A row that folds its lines open in a group of their own underneath. */
@Composable
private fun DetailSection(
    title: String,
    detail: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    MhListGroup {
        MhListRow(
            title = title,
            subtitle = detail,
            trailing = {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = onToggle
        )
    }
    if (expanded) {
        MhListGroup {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DetailLine(text: String, mono: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (mono) FontFamily.Monospace else null,
        color = if (mono) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
