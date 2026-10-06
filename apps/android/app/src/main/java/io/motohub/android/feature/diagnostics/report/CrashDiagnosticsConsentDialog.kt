// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics.report

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhDialog
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTextButton

/**
 * Asked once, on the launch after MOTO-HUB crashed, of a rider who never turned automatic
 * diagnostics on.
 *
 * The switch under Settings ▸ Diagnostics is off until someone opts in, which is the right
 * default and also means the one report worth having - the crash that just happened - is the one
 * that never leaves. This is the missing half of that: the question at the moment it costs
 * nothing to answer, with the same sentence about what would be sent that the settings notice
 * carries, so the answer is informed wherever it is given.
 *
 * [onSend] carries [alwaysSend] rather than a second dialog: a rider who is happy to send this
 * one is usually happy to send the next, and being asked every time is its own annoyance.
 * "Don't send" sends nothing and changes no setting - see
 * [DiagnosticReportScheduler.onCrashReportDeclined]. The two answers are equal grey pills: consent
 * that the app nudges with its accent colour is not freely given.
 *
 * Unlike the safety disclaimer this dialog is dismissible. Refusing to take an answer would make
 * it a toll gate on a rider who opened the app to get somewhere, and an unanswered question
 * returns on the next launch anyway. [onDismiss] runs on every way out, answers included, so it
 * must only put the question away for this launch.
 */
@Composable
fun CrashDiagnosticsConsentDialog(
    alwaysSend: Boolean,
    onAlwaysSendChanged: (Boolean) -> Unit,
    onSend: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyNotice: () -> Unit,
    onDismiss: () -> Unit
) {
    MhDialog(
        onDismiss = onDismiss,
        title = motoHubText("MOTO-HUB closed unexpectedly"),
        body = motoHubText("Sending the report lets the developer find out why."),
        primaryLabel = motoHubText("Send report"),
        onPrimary = onSend,
        secondaryLabel = motoHubText("Don't send"),
        onSecondary = onDecline,
        primaryStyle = MhActionStyle.NEUTRAL,
        dismissible = true
    ) {
        Text(
            motoHubText(
                "Reports hold your dashboard and phone models, the Android, Android Auto and MOTO-HUB " +
                    "versions, and the app log. Never passwords, positions or hardware addresses."
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        MhSwitchRow(
            title = motoHubText("Send reports automatically"),
            checked = alwaysSend,
            onCheckedChange = onAlwaysSendChanged,
            modifier = Modifier.bleed(16.dp)
        )
        // Consent is the legal ground this report travels on, so the full account of what
        // happens to it has to be reachable from the question itself, not from a settings page
        // the rider would have to go looking for afterwards.
        MhTextButton(
            motoHubText("How your data is handled"),
            onClick = onOpenPrivacyNotice,
            modifier = Modifier.offset(x = (-12).dp)
        )
    }
}

// ponytail: MhDialog's body has no row inset like MhSheet's (K2), so a row's own 16 dp padding
// would push it off the text column. Widen it by that much on both sides instead; drop this when
// the kit insets the dialog body the way it does the sheet's.
private fun Modifier.bleed(by: Dp) = layout { measurable, constraints ->
    val extra = (by * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-by.roundToPx(), 0) }
}
