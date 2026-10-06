// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.safety

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoNotTouch
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.SportsMotorsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.theme.MotoHubColors

/**
 * The briefing before the first ride: a calm full-screen page rather than an alarm. It is still a
 * window of its own above everything, and back and outside taps do nothing, so it blocks exactly as
 * the dialog it replaces did - the only way past it is "I understand".
 *
 * Every clause of the old warning is still here, one row each; only "trip recording" is gone,
 * because this edition records no trips. The page scrolls when a translation runs long, while the
 * switch and the button stay pinned above the navigation bar.
 */
@Composable
fun SafetyDisclaimerDialog(
    doNotShowAgain: Boolean,
    onDoNotShowAgainChanged: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(32.dp))
                    MhIconCircle(
                        Icons.Rounded.SportsMotorsports,
                        size = 56.dp,
                        tint = MotoHubColors.Warning,
                        container = MotoHubColors.WarningContainer
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            motoHubText("Before you ride"),
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            motoHubText("Riding needs your full attention."),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MhListGroup {
                        MhListRow(
                            title = motoHubText("Hands off while moving"),
                            subtitle = motoHubText(
                                "Never use MOTO-HUB, mirroring, Android Auto, navigation or any on-screen " +
                                    "control while the motorcycle is moving."
                            ),
                            icon = Icons.Rounded.DoNotTouch
                        )
                        MhListRow(
                            title = motoHubText("Set up while parked"),
                            subtitle = motoHubText(
                                "Configure and check everything only while parked, and use MOTO-HUB only " +
                                    "where it is completely safe and controlled."
                            ),
                            icon = Icons.Rounded.LocalParking
                        )
                        MhListRow(
                            title = motoHubText("Not a safety device"),
                            subtitle = motoHubText(
                                "MOTO-HUB can't prevent distraction, crashes, injury or damage. You alone " +
                                    "are responsible for riding safely and within the law."
                            ),
                            icon = Icons.Rounded.GppMaybe
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MhListGroup {
                        MhSwitchRow(
                            title = motoHubText("Don't show this again"),
                            checked = doNotShowAgain,
                            onCheckedChange = onDoNotShowAgainChanged
                        )
                    }
                    MhPrimaryButton(motoHubText("I understand"), onContinue)
                }
            }
        }
    }
}
