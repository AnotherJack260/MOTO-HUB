// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics.report

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhSheet

/**
 * What a diagnostics report contains, and the two answers to it. Reached from
 * Settings ▸ Diagnostics ▸ "What gets sent" - automatic reports are off until a rider says
 * otherwise, so nothing about this interrupts a first launch.
 *
 * [onDismiss] exists because the dialog used to treat a tap outside as consent
 * (`onDismissRequest = onAccept`). That was defensible while the feature was on by default and
 * this was the notice about it; on an opt-in feature it would turn an idle tap into "yes, send
 * my logs". Swiping the sheet away still changes nothing. "Don't send" is a real answer - it turns
 * automatic reports off - so it is not dressed up as "Not now", and neither answer gets the lime.
 */
@Composable
fun DiagnosticReportNoticeDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    MhSheet(
        onDismiss = onDismiss,
        title = motoHubText("Help improve MOTO-HUB"),
        body = motoHubText(
            "Reports hold your dashboard and phone models, the Android, Android Auto and MOTO-HUB " +
                "versions, and the app log. Never passwords, positions or hardware addresses."
        ),
        primaryLabel = motoHubText("Send reports"),
        onPrimary = onAccept,
        secondaryLabel = motoHubText("Don't send"),
        onSecondary = onDecline,
        primaryStyle = MhActionStyle.NEUTRAL
    ) {
        MhFootnote(motoHubText("At most once a day, after an update or a crash"), Modifier.padding(horizontal = 16.dp))
    }
}
