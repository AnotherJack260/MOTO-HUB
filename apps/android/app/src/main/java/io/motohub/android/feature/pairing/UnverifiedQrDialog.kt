// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.pairing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhDialog

/**
 * Asks before saving credentials that decoded cleanly but did not come from a Carbit provisioning
 * address. Two very different codes land here: the QR code of a dashboard whose manufacturer serves
 * it from their own domain, and any unrelated QR that happens to carry a network name. Only the
 * rider can tell those apart, so the SSID is shown and the decision is theirs - with two equal
 * answers, because the app has no business nudging a trust decision either way.
 *
 * [onDismiss] only clears the caller's flag. Cancel, back and a tap outside all mean no, and
 * [onDecline] hears it once the dialog has gone: the dialog reports that it closed before it
 * reports which answer closed it.
 */
@Composable
fun UnverifiedQrDialog(
    payload: TBoxQrPayload,
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmed = remember(payload) { booleanArrayOf(false) }
    DisposableEffect(payload) { onDispose { if (!confirmed[0]) onDecline() } }
    MhDialog(
        onDismiss = onDismiss,
        title = motoHubText("Unfamiliar QR code"),
        body = motoHubText(
            "This code has Wi-Fi details for “%1\$s”, but it comes from an unknown source. " +
                "Rebranded dashboards often do this. Continue only if you scanned your own dashboard.",
            payload.ssid
        ),
        icon = Icons.Rounded.WarningAmber,
        iconTint = io.motohub.android.ui.theme.MotoHubColors.Warning,
        primaryLabel = motoHubText("Use these details"),
        onPrimary = {
            confirmed[0] = true
            onConfirm()
        },
        secondaryLabel = motoHubText("Cancel"),
        primaryStyle = MhActionStyle.NEUTRAL,
        dismissible = true
    )
}
