// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.BatteryOptimisationGate
import io.motohub.android.session.ProcessExitReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tells a rider whose previous session was ended by the phone that it was the phone, and what to
 * change so it is less likely to happen again.
 *
 * Shown only after it has actually happened, and only once per occurrence. A warning shown to
 * everyone before every long session is one riders learn to dismiss without reading, and most of
 * them are never killed; this one appears to the person who just watched their TFT go dark, in
 * the minute they have a reason to care.
 *
 * Deliberately not styled as an error. Nothing is broken and there is nothing to retry - the last
 * session ended, this explains why, and the rider decides what to do about it. The close icon is
 * the acknowledgement, so the rider can put it away without acting on it.
 */
@Composable
fun SystemKillNotice(modifier: Modifier = Modifier) {
    val kill = ProcessExitReport.unacknowledgedSystemKill ?: return
    val context = LocalContext.current
    var dismissed by remember(kill.at) { mutableStateOf(false) }
    if (dismissed) return

    val appName = remember {
        runCatching {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }.getOrDefault("MOTO-HUB")
    }
    // Read once per occurrence, not per recomposition: the rider may change the setting and come
    // back, and re-reading on every frame would make the text flicker between the two pieces of
    // advice while the settings screen animates away. The label is picked from the same answer,
    // so the button always opens the screen the advice talks about.
    val exempt = remember(kill.at) { BatteryOptimisationGate.isExempt(context) }
    val advice = remember(kill.at) { BatteryOptimisationGate.advice(context, appName) }
    val time = remember(kill.at) { TIME_FORMAT.format(Date(kill.at)) }

    MhBanner(
        title = motoHubText("Your phone stopped the last session"),
        modifier = modifier,
        body = motoHubText("It closed %2\$s at %1\$s. This wasn't an app fault.", time, appName),
        tone = MhTone.NEUTRAL,
        actionLabel = if (exempt) motoHubText("Open app settings") else motoHubText("Open battery settings"),
        onAction = { BatteryOptimisationGate.openSettings(context) },
        onDismiss = {
            ProcessExitReport.acknowledgeSystemKill(context)
            dismissed = true
        },
        details = {
            Text(
                motoHubText(advice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
