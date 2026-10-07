// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.compose.animation.AnimatedVisibility
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
 *
 * [visible] is the caller's say on where: never over a live failure, a connection or a ride. It
 * folds in and out on the shared clock, and so does the close.
 */
@Composable
fun SystemKillNotice(visible: Boolean = true, modifier: Modifier = Modifier) {
    // Remembered rather than read on every pass: acknowledging clears the report, and the banner
    // still has to be drawn while it folds away. The report is decided before the first frame.
    val kill = remember { ProcessExitReport.unacknowledgedSystemKill } ?: return
    val context = LocalContext.current
    var dismissed by remember(kill.at) { mutableStateOf(false) }

    val appName = remember {
        runCatching {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }.getOrDefault("RideLink")
    }
    // Read once per occurrence, not per recomposition: the rider may change the setting and come
    // back, and re-reading on every frame would make the text flicker between the two pieces of
    // advice while the settings screen animates away. The label is picked from the same answer,
    // so the button always opens the screen the advice talks about.
    val exempt = remember(kill.at) { BatteryOptimisationGate.isExempt(context) }
    val advice = remember(kill.at) { BatteryOptimisationGate.advice(context, appName) }
    val time = remember(kill.at) { TIME_FORMAT.format(Date(kill.at)) }

    AnimatedVisibility(visible = visible && !dismissed, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
        MhBanner(
            // What happened, not whose fault it was: a rider reads a defence as an excuse.
            title = motoHubText("Your phone closed MOTO-HUB"),
            modifier = modifier,
            // Two whole sentences rather than one joined onto the other: not every language
            // separates sentences with a space.
            body = if (exempt) {
                motoHubText("At %1\$s, while it was running.", time)
            } else {
                motoHubText("At %1\$s, while it was running. Battery optimisation usually does this.", time)
            },
            tone = MhTone.NEUTRAL,
            actionLabel = if (exempt) motoHubText("Open app settings") else motoHubText("Open battery settings"),
            onAction = { BatteryOptimisationGate.openSettings(context) },
            onDismiss = {
                ProcessExitReport.acknowledgeSystemKill(context)
                dismissed = true
            },
            details = {
                Text(
                    advice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
