// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import androidx.compose.runtime.Composable
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.DashboardDeliveryReport
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.ProfileSuggestions
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhTone
import kotlin.math.roundToInt

/**
 * The screen a rider reaches from "the dashboard is not showing this" - the thing rider 315e0af3
 * needed and found by accident, two days late, in the Garage.
 *
 * It is not the Garage's override list with a different title. That one is a flat nineteen
 * entries in table order, real motorcycles interleaved with one-question experiments, and no way
 * to tell which could possibly apply to the dash in front of you. Here the ordering carries the
 * app's own evidence (see [ProfileSuggestions]) and every row says why it is being offered, so
 * the rider is choosing between explanations rather than guessing from a menu.
 *
 * Plain rows, not choices: this is a list of things to TRY, and a row that looks
 * already-answered invites the rider to close it again. System back is MhScreen's.
 */
@Composable
internal fun ProfileTrialScreen(
    warning: DashboardDeliveryReport,
    suggestions: List<ProfileSuggestions.Suggestion>,
    onTryProfile: (ProfileOverride) -> Unit,
    onBack: () -> Unit
) {
    MhScreen(
        title = motoHubText("Try another profile"),
        subtitle = motoHubText("Pick one to reconnect with it. If the picture appears, you can keep it."),
        onBack = onBack
    ) {
        // The two numbers the verdict was reached on, shown rather than summarised. A rider who
        // has been told "it should work" by an app that was wrong deserves to see what the app is
        // actually looking at - and these numbers are legible without any of the vocabulary
        // underneath them.
        val percent = (warning.rejectedShare * 100).roundToInt()
        MhBanner(
            title = motoHubText("Connected, but not displaying"),
            body = motoHubText(
                "Your dashboard refused %1\$d%% of the picture it was sent (%2\$d frames out of %3\$d).",
                percent,
                warning.rejected,
                warning.rejected + warning.accepted
            ),
            tone = MhTone.WARNING
        )
        MhListGroup {
            suggestions.forEach { suggestion ->
                MhListRow(
                    title = motoHubText(suggestion.override.label),
                    subtitle = describe(suggestion),
                    onClick = { onTryProfile(suggestion.override) }
                )
            }
        }
    }
}

/**
 * The banner that opens [ProfileTrialScreen], on Ride under the motorcycle's name, in whatever
 * state the connection is in.
 *
 * Under the hero rather than above every tab: drawn above the page, it pushed Ride's title down
 * the moment streaming started. It sits where Ride's other problems do, next to the status chip
 * that says "Live" - the claim it contradicts. A caution, not an error: everything the app can
 * check works.
 */
@Composable
internal fun DeliveryWarningBanner(onOpen: () -> Unit) {
    MhBanner(
        title = motoHubText("Your dashboard isn't showing the picture"),
        body = motoHubText("Another profile usually fixes this."),
        tone = MhTone.WARNING,
        actionLabel = motoHubText("Try another profile"),
        onAction = onOpen
    )
}

private fun describe(suggestion: ProfileSuggestions.Suggestion): String? {
    val note = when (suggestion.reason) {
        ProfileSuggestions.Reason.IDENTIFIED -> motoHubText("Your dashboard reports this model.")
        ProfileSuggestions.Reason.SAME_WIRE -> motoHubText("Same protocol your motorcycle already uses.")
        ProfileSuggestions.Reason.NEUTRAL -> motoHubText("Plain settings. A safe fallback.")
        ProfileSuggestions.Reason.EXPERIMENT -> motoHubText("Experimental. May do nothing.")
        ProfileSuggestions.Reason.OTHER -> ""
    }
    return listOf(suggestion.override.riderNote.orEmpty(), note)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }
}
