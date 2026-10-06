// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhSwitchRow

/**
 * What this edition can do with a diagnostics report, or null in an edition that cannot.
 *
 * A seam rather than a flavour check inside the sheet: the question "may I send this?" is the
 * same everywhere, and only the answer to "can this app send anything at all?" differs.
 */
interface DiagnosticsOffer {
    /** Whether reports already go out on their own, so the sheet does not offer what is on. */
    val autoUploadEnabled: Boolean

    /** Sends the current log once, now. */
    fun sendNow()

    /** Turns automatic uploads on from here onwards. */
    fun enableAutoUpload()
}

/**
 * Asked the moment a profile the rider picked is proven to work - and nowhere else.
 *
 * The timing is the substance of it. Riders are asked to share diagnostics when something is
 * broken, which is exactly when they are frustrated, in a hurry, and have least reason to trust
 * the app with anything. This asks in the one moment the app has just done something for them,
 * about the one fact the collector cannot obtain any other way: WHICH profile works on WHICH
 * dashboard. Rider 315e0af3 found that answer alone and it stayed on his phone; nobody with the
 * same motorcycle benefited.
 *
 * Keeping the profile and sharing the log are separate answers on purpose. Bundling them would
 * make "no thanks" cost the rider the fix they just found, which is not consent - so the sharing
 * switches start off and "Keep it" alone shares nothing.
 *
 * Not dismissible: the pin is already written, so walking away silently is the one outcome that
 * leaves the rider with a setting they never agreed to. The VM clears [trial] once either answer
 * lands, which is what takes the sheet away.
 */
@Composable
internal fun ProfileTrialConfirmation(
    trial: PendingProfileTrial,
    diagnostics: DiagnosticsOffer?,
    onKeep: (sendNow: Boolean, enableAutoUpload: Boolean) -> Unit,
    onDiscard: () -> Unit
) = key(trial) {
    var sendNow by remember { mutableStateOf(false) }
    var alwaysSend by remember { mutableStateOf(false) }
    MhSheet(
        onDismiss = {},
        title = motoHubText("That worked. Keep it?"),
        body = motoHubText(
            "Your dashboard accepts the %1\$s profile. Keep it for this motorcycle?",
            motoHubText(trial.override.label)
        ),
        primaryLabel = motoHubText("Keep it"),
        onPrimary = { onKeep(sendNow, alwaysSend) },
        secondaryLabel = motoHubText("Undo"),
        onSecondary = onDiscard,
        dismissible = false,
        content = if (diagnostics == null) null else {
            { _ ->
                // Rows straight on the sheet; the footnote pads itself to the title's edge.
                MhFootnote(
                    motoHubText("Sending a report tells riders with the same motorcycle which profile works."),
                    Modifier.padding(horizontal = 12.dp)
                )
                Column {
                    MhSwitchRow(
                        title = motoHubText("Send a report now"),
                        checked = sendNow,
                        onCheckedChange = { sendNow = it }
                    )
                    if (!diagnostics.autoUploadEnabled) {
                        MhSwitchRow(
                            title = motoHubText("Send reports automatically"),
                            checked = alwaysSend,
                            onCheckedChange = { alwaysSend = it }
                        )
                    }
                }
            }
        }
    )
}
