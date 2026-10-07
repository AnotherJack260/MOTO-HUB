// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.androidauto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.theme.MotoHubColors

/**
 * How to get Android Auto to project when it will not start on its own.
 *
 * Android Auto 17.4 removed the entry points an app could use to ask for projection, so on those
 * releases the rider has to start it from Android Auto's own developer menu. That is a sequence
 * of taps in another app, buried behind a hidden menu — exactly the kind of thing that belongs in
 * front of the rider rather than in a support thread.
 *
 * The head unit server leads, and "Add new cars to Android Auto" follows it. That order is the
 * other way round from how this page first read, and field case FF3D-A418 is why: a rider on
 * 17.4.663054 turned the switch on, took it for the whole fix because it was named first, and
 * spent an hour retrying a path his release had already closed.
 *
 * The two live in different places, which this page used to get wrong: tapping "Version" ten
 * times unlocks Android Auto's developer options, and "Add new cars" is then a switch inside
 * the Developer settings list - but "Start head unit server" is not in that list at all. It is
 * in the three-dot menu at the top right of Android Auto's ordinary settings screen. Step 2
 * says so in as many words, because the rider who scrolls Developer settings looking for it
 * finds nothing and concludes the whole page is wrong. Rider copy names the head unit server only
 * by quoting that menu label; the step itself is "Allow MOTO-HUB to connect".
 *
 * Shown as a full-screen overlay straight from MainActivity, not inside the hub. MhScreen brings
 * the background and the back handling, so the swipe-back gesture closes the page instead of
 * minimising the app. The one button that matters is pinned to the bottom, not buried mid-page.
 */
@Composable
fun AndroidAutoHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    MhScreen(
        title = motoHubText("Android Auto won't start"),
        subtitle = motoHubText("Start it from Android Auto's settings"),
        onBack = onBack,
        bottomBar = {
            MhPrimaryButton(
                motoHubText("Open Android Auto settings"),
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = {
                    if (!AndroidAutoSelfModeHelp.openAndroidAutoSettings(context)) {
                        MotoHubSnackbar.error(context, motoHubText("Couldn't open Android Auto"))
                    }
                }
            )
        }
    ) {
        MhListGroup {
            StepRow(
                1,
                motoHubText("Unlock developer options"),
                motoHubText("In Android Auto settings, scroll down and tap “Version” 10 times.")
            )
            StepRow(
                2,
                motoHubText("Allow MOTO-HUB to connect"),
                motoHubText("On the same screen, open ⋮ and tap “Start head unit server”.")
            )
            StepRow(
                3,
                motoHubText("Leave it running"),
                motoHubText("A notification confirms it. It stays on until you stop it or restart the phone.")
            )
            StepRow(
                4,
                motoHubText("Start Android Auto in MOTO-HUB"),
                motoHubText("It connects by itself within a few seconds.")
            )
        }
        MhSectionHeader(motoHubText("For older Android Auto versions"))
        MhListGroup {
            // Not step 5: it isn't part of the sequence above, so it gets a glyph, not a number.
            StepRow(
                icon = Icons.Rounded.ToggleOn,
                title = motoHubText("Turn on “Add new cars to Android Auto”"),
                body = motoHubText("In Developer settings. Older versions call it “Unknown sources”.")
            )
        }
        MhFootnote(motoHubText("Recent Android Auto versions require this manual step."))
    }
}

/**
 * A numbered step laid out like an MhListRow: the number in a neutral circle where a row's icon
 * goes, never lime - nothing here is tappable. An [icon] takes the number's place, same circle,
 * for a tip outside the sequence. ponytail: MhListRow takes only an icon, and the kit has no
 * numeral glyphs.
 */
@Composable
private fun StepRow(number: Int = 0, title: String, body: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            MhIconCircle(icon, size = 32.dp)
        } else {
            Box(
                Modifier.size(32.dp).background(MotoHubColors.Fill, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
