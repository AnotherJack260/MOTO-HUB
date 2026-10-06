// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.pairing

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import io.motohub.android.session.TBoxConnectionMode
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhNavIcon
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTextField
import io.motohub.android.ui.components.MhTone

/**
 * Fallback pairing path for motorcycles that don't show an EasyConn QR code
 * (reported on some US-market bikes whose only official companion is
 * CFMOTO RideSync). The rider must already know the T-Box's Wi-Fi network
 * name and password from another source (the bike itself, its manual, or a
 * dealer) - this screen does not discover or guess credentials, it only
 * removes the QR scan as a hard requirement to enter them.
 */
@Composable
fun ManualPairingScreen(
    ssid: String,
    password: String,
    connectionMode: TBoxConnectionMode,
    formError: String?,
    /**
     * A name the phone has evidence for, which [ssid] differs from only by spacing or
     * punctuation. Null unless there is one - see [manualSsidVerdict].
     */
    ssidSuggestion: String?,
    onAcceptSsidSuggestion: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConnectionModeChanged: (TBoxConnectionMode) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    var showModes by rememberSaveable { mutableStateOf(false) }

    MhScreen(
        title = motoHubText("Enter details manually"),
        onBack = onClose,
        navIcon = MhNavIcon.CLOSE,
        subtitle = motoHubText("Use the Wi-Fi name and password shown on your dashboard or in its manual."),
        // Disabled while blank, so the ViewModel's empty-name error cannot be reached from here;
        // its guard stays for every other caller.
        bottomBar = { MhPrimaryButton(motoHubText("Save"), onSave, enabled = ssid.isNotBlank()) }
    ) {
        // Monospace also switches off autocorrect and capitalisation: a keyboard that "fixes"
        // CFMOTO6627 into "CFMOTO 6627" creates a motorcycle that can never be joined.
        MhTextField(
            value = ssid,
            onValueChange = onSsidChanged,
            label = motoHubText("Wi-Fi name (SSID)"),
            monospace = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        // Deliberately not an error colour, and deliberately not blocking. The rider may well
        // be right; this only makes the other reading visible before a second motorcycle is
        // created that can never be joined.
        ssidSuggestion?.let { suggestion ->
            MhBanner(
                title = motoHubText("Did you mean “%1\$s”?", suggestion),
                body = motoHubText("This phone knows that name. To keep yours, tap Save again."),
                tone = MhTone.NEUTRAL,
                actionLabel = motoHubText("Use this name"),
                onAction = onAcceptSsidSuggestion
            )
        }
        MhTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = motoHubText("Password"),
            isPassword = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        // A subtitle rather than a trailing value: the value slot cuts long text, and a translated
        // mode name must never be cut.
        MhListGroup {
            MhListRow(
                title = motoHubText("Connection type"),
                subtitle = connectionMode.label(),
                onClick = { showModes = true }
            )
        }
        // In practice a persistence failure: the raw reason stays one tap away for support.
        formError?.let { reason ->
            MhBanner(
                title = motoHubText("Couldn't save the motorcycle"),
                details = {
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    if (showModes) {
        MhSheet(
            onDismiss = { showModes = false },
            title = motoHubText("Connection type"),
            body = motoHubText("Keep Auto unless your dashboard asks for something else.")
        ) { close ->
            TBoxConnectionMode.entries.forEach { mode ->
                MhChoiceRow(
                    title = mode.label(),
                    subtitle = mode.hint(),
                    selected = mode == connectionMode,
                    onClick = { close { onConnectionModeChanged(mode) } }
                )
            }
        }
    }
}

private fun TBoxConnectionMode.label(): String = when (this) {
    TBoxConnectionMode.AUTO -> motoHubText("Auto")
    TBoxConnectionMode.ACCESS_POINT -> motoHubText("Access point")
    TBoxConnectionMode.WIFI_DIRECT -> motoHubText("Wi-Fi Direct (P2P)")
    // Named from the rider's point of view: what they have to do, not what the dash is. They pick
    // this after their dash asks them to open a hotspot, so "phone hotspot" is the phrase they
    // just read on the screen.
    TBoxConnectionMode.PHONE_HOTSPOT -> motoHubText("My phone hosts the hotspot")
    // For a dash that shows no credentials anywhere and no network in any scan: the app hosts a
    // hotspot itself and hands it over on Bluetooth. Named for what the rider observes - their
    // dash asked them to open an app and did nothing else.
    TBoxConnectionMode.BLE_PROVISIONED -> motoHubText("Set up over Bluetooth")
    // Normally set by scanning the ThinkerRide QR; offered here for rebadged units whose code
    // points at an OEM host. "KOVE" is the brand a rider would look for, ThinkerRide the tech.
    TBoxConnectionMode.THINKERRIDE -> motoHubText("KOVE / ThinkerRide (Bluetooth)")
}

private fun TBoxConnectionMode.hint(): String = when (this) {
    TBoxConnectionMode.AUTO -> motoHubText("Works for most dashboards")
    TBoxConnectionMode.ACCESS_POINT -> motoHubText("Your phone joins the dashboard's Wi-Fi")
    TBoxConnectionMode.WIFI_DIRECT -> motoHubText("For dashboards that connect over Wi-Fi Direct")
    TBoxConnectionMode.PHONE_HOTSPOT -> motoHubText("Your dashboard asks you to turn on your phone's hotspot")
    // Says nothing about the name field, which this mode still requires.
    TBoxConnectionMode.BLE_PROVISIONED -> motoHubText("For dashboards set up from their own app over Bluetooth")
    TBoxConnectionMode.THINKERRIDE -> motoHubText("KOVE and other ThinkerRide dashboards")
}
