// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.diagnostics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.ThinkerRideGate
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhTone

/**
 * The Zontes clock-reset experiment bench. One button, one live log: EcBtpClockLab does the
 * Bluetooth work and every line lands both here and in the shared application log.
 */
@Composable
fun ClockLabScreen(
    state: ClockLabUiState,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    // The first field run burned three attempts on the missing "Nearby devices" grant: the lab
    // only logged where to find it and the rider spent minutes in system settings. Ask in place.
    val context = LocalContext.current
    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        ProjectionEventLog.record(
            "CLOCKLAB",
            "Bluetooth permission results: " +
                grants.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" } + "."
        )
        if (grants.values.all { it }) onRun()
    }
    val runWithPermissions: () -> Unit = {
        if (ThinkerRideGate.hasBlePermissions(context)) {
            onRun()
        } else {
            blePermissionLauncher.launch(ThinkerRideGate.blePermissions)
        }
    }

    MhScreen(
        title = motoHubText("Dash clock lab"),
        subtitle = motoHubText(
            "For dashes that reset their clock at every ignition cycle (Zontes and " +
                "Voge above all). With the dash powered on, the lab connects over Bluetooth, " +
                "listens, then pushes the time in five different shapes and records " +
                "every byte. Ride, cycle the ignition, and share the application log: " +
                "it will say which shape the dash accepted."
        ),
        onBack = onBack
    ) {
        if (state.running) {
            MhStatusChip(motoHubText("Running"), MhTone.PROGRESS)
            // Not `loading`: that would disable the one button that ends the run.
            MhSecondaryButton(motoHubText("Stop"), onClick = onStop, destructive = true)
        } else {
            MhPrimaryButton(motoHubText("Run clock experiments"), onClick = runWithPermissions)
        }

        MhSectionHeader(motoHubText("Live log"))
        MhListGroup {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.lines.isEmpty()) {
                    Text(
                        motoHubText("Nothing yet. Power the dash on, then run the experiments."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.lines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
