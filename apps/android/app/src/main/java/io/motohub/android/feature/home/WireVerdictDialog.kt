// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhSheet

/**
 * The one question MOTO-HUB cannot answer for itself.
 *
 * Everything else the wire ladder needs, it reads off the protocol: frames accepted, sockets
 * closed, heartbeats returned. But a dashboard can take a perfectly good stream and display none
 * of it - a Zontes 368G swallowed 3900 frames over four minutes with its panel still showing the
 * pairing QR, and from the phone's side that session was indistinguishable from a flawless one.
 *
 * So this is asked once per wire format tried, and only after a session the firmware clearly
 * liked. "No" is what moves the ladder on; without it the search would stop at the first format
 * the dashboard tolerated and never find the one it actually renders.
 */
@Composable
fun WireVerdictDialog(
    motorcycleName: String,
    onAnswer: (projectionSeen: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // Two equal grey answers: this is data, and a lime "Yes" would tilt it. [onDismiss] runs on
    // every way out, answers included, and before the answer; swiping away answers nothing.
    MhSheet(
        onDismiss = onDismiss,
        title = motoHubText("Did it show up on the dashboard?"),
        body = motoHubText(
            "Last time you connected to %1\$s, everything looked fine from the phone. MOTO-HUB " +
                "can't see the dashboard, so it has to ask.",
            motorcycleName
        ),
        primaryLabel = motoHubText("Yes, I saw it"),
        onPrimary = { onAnswer(true) },
        secondaryLabel = motoHubText("No, nothing appeared"),
        onSecondary = { onAnswer(false) },
        primaryStyle = MhActionStyle.NEUTRAL
    ) {
        MhFootnote(
            motoHubText("If it stayed on the pairing screen, MOTO-HUB tries another video format next time."),
            Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Shown to a rider whose wire search is standing still because they only ever mirror.
 *
 * Only Android Auto runs the format the search is testing, so a mirroring session teaches it
 * nothing and the rung never moves. Without this the rider sees a search that simply never
 * progresses and has no way to know why - and the honest fix is one sentence, not a redesign.
 */
@Composable
fun WireNeedsAndroidAutoDialog(onDismiss: () -> Unit) {
    // Any way of closing it counts as read: onDismiss runs on all of them, "Got it" included.
    MhSheet(
        onDismiss = onDismiss,
        title = motoHubText("Try Android Auto once"),
        body = motoHubText(
            "MOTO-HUB can only test a new video format while Android Auto runs; mirroring always " +
                "uses its own. Connect once with Android Auto and the search moves on by itself."
        ),
        primaryLabel = motoHubText("Got it"),
        onPrimary = {}
    )
}
