// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.controls

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.theme.MotoHubColors

/**
 * Whether [context]'s motorcycle is the one live handlebar capture belongs to: the live T-Box
 * session's motorcycle, else the garage's active one. The Garage edits any motorcycle's stored
 * values, but presses are always interpreted with this one's handlebar, so only it may touch the
 * live capture or teach. Always true for a context that names no motorcycle.
 */
internal fun ownsLiveCapture(context: Context): Boolean =
    (context as? MotorcycleContext)?.motorcycleId
        ?.let { it == MotorcycleScope.suffix(context.applicationContext) } ?: true

/**
 * One motorcycle's handlebar buttons. Shown by the Garage under `LocalContext provides` that
 * motorcycle's [MotorcycleContext], so every store call here is scoped to it. The press banner and
 * the music volume are the rider's, not the motorcycle's, and the footnote says so.
 */
@Composable
internal fun HandlebarDetail(onBack: () -> Unit, onOpenMapping: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(HandlebarControlStore.isEnabled(context)) }
    var pressBanner by remember { mutableStateOf(HandlebarPressHud.isEnabled(context)) }
    var inputMode by remember { mutableStateOf(HandlebarControlStore.inputMode(context)) }
    // Granted outside this app, in system settings, so the only moment it can have changed is a
    // return to this screen - hence the resume watch rather than a plain read in composition.
    val hidServiceEnabled = rememberOnResume { HandlebarHidCaptureService.isEnabled(context) }
    var openedAccessibilitySettings by rememberSaveable { mutableStateOf(false) }
    val volumeLevels = remember { MediaButtonBridge.volumeLevels(context) }
    var listeningVolume by remember { mutableStateOf(volumeLevels.first.toFloat()) }
    val openAccessibilitySettings = {
        openedAccessibilitySettings = true
        HandlebarHidCaptureService.openAccessibilitySettings(context)
    }
    MhScreen(title = motoHubText("Handlebar buttons"), onBack = onBack) {
        if (HandlebarControlStore.isManagedByCompanion(context)) {
            // The companion re-pushes ITS handlebar configuration at every session start,
            // silently overwriting anything set here — without this note the Core switch
            // looks broken ("I enabled it and it turned itself off").
            MhBanner(
                title = motoHubText("Managed by the companion app"),
                body = motoHubText("It re-applies its own button setup each time streaming starts. Change it there."),
                tone = MhTone.WARNING
            )
        }
        MhListGroup {
            MhSwitchRow(
                title = motoHubText("Buttons control Android Auto"),
                subtitle = motoHubText("Needs the motorcycle paired over Bluetooth"),
                checked = enabled,
                onCheckedChange = { value ->
                    enabled = value
                    HandlebarControlStore.setEnabled(context, value)
                    // Another motorcycle's capture is not this one's to switch: the stored value
                    // applies when this motorcycle next streams.
                    val applied = ownsLiveCapture(context) &&
                        MediaButtonBridge.setTargetCaptureActive(MediaButtonBridge.TARGET_ANDROID_AUTO, value)
                    ProjectionEventLog.record(
                        "SETTINGS",
                        "Handlebar capture changed to enabled=$value; liveSession=$applied."
                    )
                }
            )
            MhListRow(
                title = motoHubText("Button mapping"),
                subtitle = motoHubText("What each press, double press and hold does"),
                onClick = onOpenMapping
            )
            // Here rather than in Diagnostics: this is where a rider checks whether presses arrive.
            // Greyed while the buttons are off, like the volume below: there are no presses to name.
            MhSwitchRow(
                title = motoHubText("Show button presses on the dashboard"),
                subtitle = motoHubText("A one-second banner names each press"),
                checked = pressBanner,
                enabled = enabled,
                onCheckedChange = {
                    pressBanner = it
                    HandlebarPressHud.setEnabled(context, it)
                    ProjectionEventLog.record("SETTINGS", "Press banner changed to enabled=$it.")
                }
            )
        }
        MhSectionHeader(motoHubText("Music volume"))
        MhListGroup {
            // Disabled the way a kit row is: the whole row at 45%, the slider's own colours kept.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(if (enabled) 1f else 0.45f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = MaterialTheme.colorScheme.onSurfaceVariant
                val thumb = MaterialTheme.colorScheme.onSurface
                val active = MaterialTheme.colorScheme.primary
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = tint)
                Slider(
                    value = listeningVolume,
                    onValueChange = { listeningVolume = it },
                    onValueChangeFinished = { MediaButtonBridge.setVolume(context, listeningVolume.toInt()) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = motoHubText("Music volume") },
                    enabled = enabled,
                    valueRange = 0f..volumeLevels.second.toFloat(),
                    steps = (volumeLevels.second - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = thumb,
                        activeTrackColor = active,
                        inactiveTrackColor = MotoHubColors.Fill,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        disabledThumbColor = thumb,
                        disabledActiveTrackColor = active,
                        disabledInactiveTrackColor = MotoHubColors.Fill,
                        disabledActiveTickColor = Color.Transparent,
                        disabledInactiveTickColor = Color.Transparent
                    )
                )
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = tint)
            }
        }
        MhFootnote(motoHubText("The music level while the buttons control Android Auto."))
        MhSectionHeader(motoHubText("Button type"))
        MhListGroup {
            HandlebarInputMode.entries.forEach { candidate ->
                MhChoiceRow(
                    title = when (candidate) {
                        HandlebarInputMode.AVRCP -> motoHubText("Media controls")
                        HandlebarInputMode.HID -> motoHubText("Bluetooth keyboard")
                    },
                    subtitle = when (candidate) {
                        HandlebarInputMode.AVRCP -> motoHubText("Most dashboards")
                        HandlebarInputMode.HID -> motoHubText("For remotes paired as a Bluetooth keyboard")
                    },
                    selected = inputMode == candidate,
                    onClick = {
                        inputMode = candidate
                        HandlebarControlStore.setInputMode(context, candidate)
                        // A session may well be running while the rider is in here - that is when
                        // they discover the protocol is wrong. Without this the switch takes effect
                        // only at the next session start. Only the capture owner's, though.
                        if (ownsLiveCapture(context)) MediaButtonBridge.inputModeChanged()
                        ProjectionEventLog.record("SETTINGS", "Handlebar input mode changed to ${candidate.name}.")
                    }
                )
            }
            if (inputMode == HandlebarInputMode.HID && (hidServiceEnabled || openedAccessibilitySettings)) {
                MhListRow(
                    title = motoHubText("Accessibility service"),
                    value = if (hidServiceEnabled) motoHubText("On") else motoHubText("Off"),
                    onClick = openAccessibilitySettings
                )
            }
        }
        if (inputMode == HandlebarInputMode.HID && !hidServiceEnabled) {
            if (!openedAccessibilitySettings) {
                MhBanner(
                    title = motoHubText("Accessibility service is off"),
                    body = motoHubText("Bluetooth keyboard presses aren't seen until you turn it on."),
                    tone = MhTone.WARNING,
                    actionLabel = motoHubText("Open accessibility settings"),
                    onAction = openAccessibilitySettings
                )
            } else {
                // Second half of the grant, and only shown once the rider has come back from the
                // first half without the service on - which is exactly what the Android 13+
                // restricted-settings gate looks like from here. Never shown pre-emptively: on a
                // phone where the toggle worked normally this step would be noise.
                // See HandlebarHidCaptureService.openAppInfo.
                MhBanner(
                    title = motoHubText("Switch greyed out?"),
                    body = motoHubText(
                        "Android blocks it for apps from outside a store. In app settings, open the " +
                            "three-dot menu and allow restricted settings."
                    ),
                    tone = MhTone.WARNING,
                    actionLabel = motoHubText("Open app settings"),
                    onAction = { HandlebarHidCaptureService.openAppInfo(context) }
                )
            }
        }
        MhFootnote(motoHubText("Timing, the press banner and music volume apply to every motorcycle."))
    }
}

/**
 * What stops the handlebar buttons working right now, for the parent row; null when nothing does
 * or they are off. HID is exempt from the Bluetooth grant: its presses arrive through the
 * accessibility service (see MediaButtonBridge), so only that service is checked for it.
 */
internal fun handlebarProblem(context: Context): String? = when {
    !HandlebarControlStore.isEnabled(context) -> null
    HandlebarControlStore.inputMode(context) == HandlebarInputMode.HID ->
        if (HandlebarHidCaptureService.isEnabled(context)) null else motoHubText("Accessibility service is off")
    !BluetoothStatus.hasConnectPermission(context) -> motoHubText("Bluetooth access is off")
    else -> null
}

/**
 * A value set outside the app (a permission, an accessibility service, a pairing), read again
 * each time the app comes back to the front: the rider changes it in system settings and returns.
 */
@Composable
internal fun <T> rememberOnResume(read: () -> T): T {
    var value by remember { mutableStateOf(read()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { value = read() }
    return value
}
