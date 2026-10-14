// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.androidauto.AndroidAutoDisplayMode
import io.motohub.android.feature.controls.BluetoothStatus
import io.motohub.android.feature.controls.rememberOnResume
import io.motohub.android.feature.settings.AndroidAutoAspectMatchingMode
import io.motohub.android.feature.settings.AndroidAutoDensityMode
import io.motohub.android.feature.settings.AndroidAutoResolutionMode
import io.motohub.android.feature.settings.AndroidAutoSize
import io.motohub.android.feature.settings.AutostartService
import io.motohub.android.feature.settings.ImageQuality
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.feature.settings.VideoPowerMode
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.TBoxCapabilityStore
import io.motohub.android.tbox.TBoxClockAskRegistry
import io.motohub.android.tbox.TBoxWifiDirectConnector
import io.motohub.android.tbox.TBoxWireLadder
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhSwitchRow

// One motorcycle's settings, beyond the main Motorcycle details list: its pickers (P1 sheets), the
// Advanced screen, and the Size › Custom screens. Every read and write goes through the edited
// motorcycle's scoped context, never LocalContext: sheet content is its own dialog composition,
// whose LocalContext is not the motorcycle's (see MotoHubSheet).

/** One row of a [ChoiceSheet]. */
internal class SheetChoice<out T>(val value: T, val title: String, val subtitle: String? = null)

/** A P1 picker: rows straight on the sheet, a tap picks and closes, no pills. */
@Composable
internal fun <T> ChoiceSheet(
    title: String,
    body: String? = null,
    choices: List<SheetChoice<T>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit
) {
    MhSheet(onDismiss = onDismiss, title = title, body = body) { close ->
        Column {
            choices.forEach { choice ->
                MhChoiceRow(
                    title = choice.title,
                    subtitle = choice.subtitle,
                    selected = choice.value == selected,
                    onClick = { close { onPick(choice.value) } }
                )
            }
        }
    }
}

// ── values, one word per row ─────────────────────────────────────────────────────────────────

internal fun AndroidAutoSize.title(): String = when (this) {
    AndroidAutoSize.COMPACT -> motoHubText("Compact")
    AndroidAutoSize.STANDARD -> motoHubText("Standard")
    AndroidAutoSize.LARGE -> motoHubText("Large")
    AndroidAutoSize.CUSTOM -> motoHubText("Custom")
}

/** Fit: AUTO advertises the panel's own shape, MANUAL only the screen margins. */
internal fun AndroidAutoAspectMatchingMode.fitTitle(): String = when (this) {
    AndroidAutoAspectMatchingMode.AUTO -> motoHubText("Fill the screen")
    AndroidAutoAspectMatchingMode.MANUAL -> motoHubText("Show everything")
}

internal fun ImageQuality.title(): String = when (this) {
    ImageQuality.AUTOMATIC -> motoHubText("Automatic")
    ImageQuality.SHARPER -> motoHubText("Sharper")
    ImageQuality.SMOOTHER -> motoHubText("Smoother")
}

internal fun AutostartService.title(): String = when (this) {
    AutostartService.MIRRORING -> motoHubText("Mirroring")
    AutostartService.ANDROID_AUTO -> motoHubText("Android Auto")
    AutostartService.RIDE_DASHBOARD -> motoHubText("Ride Dashboard")
}

/**
 * Named by frame rate: the catalogue's "Balanced" also named a picture choice, and "Smooth" sat
 * next to the picture's "Smoother". Only the words changed; the stored enum and its logs did not.
 */
internal fun VideoPowerMode.title(): String = when (this) {
    VideoPowerMode.AUTO -> motoHubText("Auto")
    VideoPowerMode.SMOOTH -> motoHubText("30 fps")
    VideoPowerMode.BALANCED -> motoHubText("24 fps")
    VideoPowerMode.SAVER -> motoHubText("20 fps")
}

private fun baseBitrateTitle(bitsPerSecond: Int?): String =
    bitsPerSecond?.let { motoHubText("%1\$d Mbps", it / 1_000_000) } ?: motoHubText("Auto")

private fun keyframeTitle(seconds: Int?): String = when (seconds) {
    null -> motoHubText("Auto")
    0 -> motoHubText("Every frame")
    else -> motoHubText("%1\$d s", seconds)
}

private fun carbitExactTitle(exact: Boolean?): String = when (exact) {
    null -> motoHubText("Auto")
    true -> motoHubText("On")
    false -> motoHubText("Off")
}

// ── main-list sheets ─────────────────────────────────────────────────────────────────────────

@Composable
internal fun SizeSheet(current: AndroidAutoSize, onPick: (AndroidAutoSize) -> Unit, onDismiss: () -> Unit) {
    ChoiceSheet(
        title = motoHubText("Size"),
        body = motoHubText("How big Android Auto draws on the dashboard."),
        choices = listOf(
            SheetChoice(AndroidAutoSize.COMPACT, AndroidAutoSize.COMPACT.title(), motoHubText("Smaller text, more map")),
            SheetChoice(AndroidAutoSize.STANDARD, AndroidAutoSize.STANDARD.title(), motoHubText("Matches your dashboard")),
            SheetChoice(AndroidAutoSize.LARGE, AndroidAutoSize.LARGE.title(), motoHubText("Bigger buttons, less map")),
            SheetChoice(AndroidAutoSize.CUSTOM, AndroidAutoSize.CUSTOM.title(), motoHubText("Pick the resolution and interface size"))
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss
    )
}

@Composable
internal fun FitSheet(
    current: AndroidAutoAspectMatchingMode,
    onPick: (AndroidAutoAspectMatchingMode) -> Unit,
    onDismiss: () -> Unit
) {
    ChoiceSheet(
        title = motoHubText("Fit"),
        choices = listOf(
            SheetChoice(AndroidAutoAspectMatchingMode.AUTO, AndroidAutoAspectMatchingMode.AUTO.fitTitle(), motoHubText("Recommended")),
            SheetChoice(
                AndroidAutoAspectMatchingMode.MANUAL,
                AndroidAutoAspectMatchingMode.MANUAL.fitTitle(),
                motoHubText("The whole picture, with black bars")
            )
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss
    )
}

@Composable
internal fun ImageSheet(current: ImageQuality, onPick: (ImageQuality) -> Unit, onDismiss: () -> Unit) {
    ChoiceSheet(
        title = motoHubText("Image"),
        choices = listOf(
            SheetChoice(
                ImageQuality.AUTOMATIC,
                ImageQuality.AUTOMATIC.title(),
                motoHubText("Adapts to the dashboard, the phone's heat and the Wi-Fi")
            ),
            SheetChoice(ImageQuality.SHARPER, ImageQuality.SHARPER.title(), motoHubText("Higher bitrate for crisper maps and text")),
            SheetChoice(ImageQuality.SMOOTHER, ImageQuality.SMOOTHER.title(), motoHubText("Lower bitrate, less heat and network load"))
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss
    )
}

/** [current] and [onPick] carry the service to start, or null for "Do nothing". */
@Composable
internal fun OnArrivalSheet(current: AutostartService?, onPick: (AutostartService?) -> Unit, onDismiss: () -> Unit) {
    ChoiceSheet(
        title = motoHubText("On arrival"),
        body = motoHubText("Runs once per app launch. Stop streaming and you're back in control."),
        choices = listOfNotNull(
            SheetChoice(AutostartService.ANDROID_AUTO, motoHubText("Start Android Auto")),
            SheetChoice(
                AutostartService.MIRRORING,
                motoHubText("Start mirroring"),
                motoHubText("Android asks to allow screen capture each time")
            ),
            // ADVANCED only, as before: CORE's autostart refuses it.
            if (BuildConfig.IS_PRO) SheetChoice(AutostartService.RIDE_DASHBOARD, AutostartService.RIDE_DASHBOARD.title()) else null,
            SheetChoice<AutostartService?>(null, motoHubText("Do nothing"))
        ),
        selected = current,
        onPick = onPick,
        onDismiss = onDismiss
    )
}

// ── Advanced ─────────────────────────────────────────────────────────────────────────────────

private enum class AdvancedSheet { FRAME_RATE, BASE_BITRATE, KEYFRAME, CARBIT_EXACT }

/** The Base bitrate choices, in Mbps. */
private val BASE_BITRATES_MBPS = listOf(2, 3, 4, 6, 8)

/** The I-frame interval choices, in seconds; 0 ("Every frame") is offered on its own. */
private val KEYFRAME_SECONDS = listOf(1, 2, 3)

/**
 * Motorcycle details › Advanced (P8): the dashboard profile and the video overrides, the
 * compositor's fit and margins, the dashboard clock, the Wi-Fi Direct teardown and the
 * capabilities. All of [profile]'s, through [bike].
 */
@Composable
internal fun MotorcycleAdvancedScreen(
    profile: MotorcycleProfile,
    bike: Context,
    displayMode: AndroidAutoDisplayMode,
    onBack: () -> Unit,
    onOpenProfileOverride: () -> Unit,
    onOpenDisplayFit: () -> Unit,
    onOpenScreenMargins: () -> Unit,
    onOpenCapabilities: () -> Unit
) {
    var sheet by rememberSaveable(profile.id) { mutableStateOf<AdvancedSheet?>(null) }
    var frameRate by remember(profile.id) { mutableStateOf(MotoHubSettings.videoPowerMode(bike)) }
    var baseBitrate by remember(profile.id) { mutableStateOf(MotoHubSettings.baseBitrateOverride(bike)) }
    var keyframe by remember(profile.id) { mutableStateOf(MotoHubSettings.keyframeIntervalOverride(bike)) }
    var carbitExact by remember(profile.id) { mutableStateOf(MotoHubSettings.carbitExactOverride(bike)) }
    var dashClock by remember(profile.id) { mutableStateOf(MotoHubSettings.dashClockSync(bike)) }
    var bluetoothClock by remember(profile.id) { mutableStateOf(MotoHubSettings.bluetoothClockSync(bike)) }
    var keepWifiDirect by remember(profile.id) { mutableStateOf(MotoHubSettings.keepWifiDirectAfterDisconnect(bike)) }
    // Some firmware asks MOTO-HUB for the time, is answered, and goes on counting from its own
    // power-on anyway. On those the switch below cannot do anything in either position, and a
    // rider with no way of knowing that spends the evening toggling it - so it is said here,
    // from what THIS motorcycle's dashboard has actually been seen doing on this phone.
    val dashDiscardsClock = remember(profile.id) {
        runCatching {
            val capabilities = TBoxCapabilityStore(bike).load(profile)?.capabilities
            TBoxClockAskRegistry.discardsTime(bike, TBoxWireLadder.fingerprintOf(capabilities))
        }.getOrDefault(false)
    }
    // Paired in system settings, so read again when the rider comes back from there.
    val nothingPaired = rememberOnResume { nothingPaired(bike) }
    fun log(setting: String, value: String) =
        ProjectionEventLog.record("GARAGE", "$setting for ${profile.ssid}: $value.")

    MhScreen(title = motoHubText("Advanced"), onBack = onBack) {
        GarageSection(motoHubText("Video")) {
            MhListRow(
                title = motoHubText("Dashboard profile"),
                icon = Icons.Rounded.Memory,
                value = ProfileOverride.byKey(profile.profileOverrideKey).title(),
                onClick = onOpenProfileOverride
            )
            MhListRow(
                title = motoHubText("Frame rate"),
                icon = Icons.Rounded.Speed,
                value = frameRate.title(),
                onClick = { sheet = AdvancedSheet.FRAME_RATE }
            )
            MhListRow(
                title = motoHubText("Base bitrate"),
                icon = Icons.Rounded.DataUsage,
                value = baseBitrateTitle(baseBitrate),
                onClick = { sheet = AdvancedSheet.BASE_BITRATE }
            )
            MhListRow(
                title = motoHubText("I-frame interval"),
                icon = Icons.Rounded.Timer,
                value = keyframeTitle(keyframe),
                onClick = { sheet = AdvancedSheet.KEYFRAME }
            )
            MhListRow(
                title = motoHubText("Carbit-exact media"),
                icon = Icons.Rounded.Sync,
                value = carbitExactTitle(carbitExact),
                onClick = { sheet = AdvancedSheet.CARBIT_EXACT }
            )
        }
        GarageSection(motoHubText("Android Auto")) {
            MhListRow(
                title = motoHubText("Display fit"),
                icon = Icons.Rounded.AspectRatio,
                value = displayMode.title,
                onClick = onOpenDisplayFit
            )
            MhListRow(
                title = motoHubText("Screen margins"),
                subtitle = motoHubText("Leave room for the dashboard's own display"),
                icon = Icons.Rounded.CropFree,
                onClick = onOpenScreenMargins
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GarageSection(motoHubText("Dashboard clock")) {
                MhSwitchRow(
                    title = motoHubText("Set the clock over Wi-Fi"),
                    subtitle = if (dashDiscardsClock) {
                        motoHubText("Your dashboard ignores it and keeps its own clock")
                    } else {
                        motoHubText("Sets the time on most dashboards")
                    },
                    checked = dashClock,
                    onCheckedChange = {
                        dashClock = it
                        MotoHubSettings.setDashClockSync(bike, it)
                        log("Dash clock sync", "$it")
                    }
                )
                MhSwitchRow(
                    title = motoHubText("Set the clock over Bluetooth"),
                    // The pairing caveat only where it applies, instead of in every rider's footnote.
                    subtitle = if (nothingPaired) {
                        motoHubText("Needs the motorcycle paired over Bluetooth")
                    } else {
                        motoHubText("For dashboards stuck at 00:00 (experimental)")
                    },
                    checked = bluetoothClock,
                    onCheckedChange = {
                        bluetoothClock = it
                        MotoHubSettings.setBluetoothClockSync(bike, it)
                        log("Bluetooth clock sync", "$it")
                    }
                )
            }
            MhFootnote(motoHubText("Turn off the Wi-Fi clock only if the dashboard still shows the wrong date after connecting."))
        }
        // Only a Wi-Fi Direct dashboard has a group to keep.
        if (TBoxWifiDirectConnector.isWifiDirectSsid(profile.ssid)) {
            GarageSection(motoHubText("Connection")) {
                MhSwitchRow(
                    title = motoHubText("Stay on the dashboard's Wi-Fi"),
                    subtitle = motoHubText("For dashboards that lose the time after disconnecting"),
                    checked = keepWifiDirect,
                    onCheckedChange = {
                        keepWifiDirect = it
                        MotoHubSettings.setKeepWifiDirectAfterDisconnect(bike, it)
                        log("Keep Wi-Fi Direct after disconnect", "$it")
                    }
                )
            }
        }
        MhListGroup {
            MhListRow(
                title = motoHubText("Dashboard capabilities"),
                subtitle = motoHubText("What the dashboard reports about itself"),
                icon = Icons.Rounded.Info,
                onClick = onOpenCapabilities
            )
        }
    }

    val dismiss = { sheet = null }
    when (sheet) {
        AdvancedSheet.FRAME_RATE -> ChoiceSheet(
            title = motoHubText("Frame rate"),
            choices = VideoPowerMode.entries.map { mode ->
                SheetChoice(
                    mode,
                    mode.title(),
                    when (mode) {
                        VideoPowerMode.AUTO -> motoHubText("Adapts to phone heat and Wi-Fi")
                        VideoPowerMode.SMOOTH -> motoHubText("Smoothest")
                        VideoPowerMode.BALANCED -> null
                        VideoPowerMode.SAVER -> motoHubText("Less heat and battery")
                    }
                )
            },
            selected = frameRate,
            onPick = {
                frameRate = it
                MotoHubSettings.setVideoPowerMode(bike, it)
                log("Frame rate", it.name)
            },
            onDismiss = dismiss
        )
        AdvancedSheet.BASE_BITRATE -> ChoiceSheet(
            title = motoHubText("Base bitrate"),
            body = motoHubText("Sharper and Smoother scale this value."),
            choices = listOf(SheetChoice<Int?>(null, motoHubText("Auto"), motoHubText("Set by the dashboard profile"))) +
                BASE_BITRATES_MBPS.map { mbps -> SheetChoice<Int?>(mbps * 1_000_000, baseBitrateTitle(mbps * 1_000_000)) },
            selected = baseBitrate,
            onPick = {
                baseBitrate = it
                MotoHubSettings.setBaseBitrateOverride(bike, it)
                log("Base bitrate", it?.toString() ?: "AUTO")
            },
            onDismiss = dismiss
        )
        AdvancedSheet.KEYFRAME -> ChoiceSheet(
            title = motoHubText("I-frame interval"),
            choices = listOf(
                SheetChoice<Int?>(null, motoHubText("Auto"), motoHubText("Set by the dashboard profile")),
                SheetChoice<Int?>(0, keyframeTitle(0), motoHubText("Survives lost frames, softer picture"))
            ) + KEYFRAME_SECONDS.map { seconds -> SheetChoice<Int?>(seconds, keyframeTitle(seconds)) },
            selected = keyframe,
            onPick = {
                keyframe = it
                MotoHubSettings.setKeyframeIntervalOverride(bike, it)
                log("I-frame interval", it?.toString() ?: "AUTO")
            },
            onDismiss = dismiss
        )
        AdvancedSheet.CARBIT_EXACT -> ChoiceSheet(
            title = motoHubText("Carbit-exact media"),
            body = motoHubText("Answers the dashboard the way Carbit Ride does. Auto follows the dashboard profile."),
            choices = listOf(null, true, false).map { SheetChoice(it, carbitExactTitle(it)) },
            selected = carbitExact,
            onPick = {
                carbitExact = it
                MotoHubSettings.setCarbitExactOverride(bike, it)
                log("Carbit-exact media", it?.toString()?.uppercase() ?: "AUTO")
            },
            onDismiss = dismiss
        )
        null -> Unit
    }
}

// ── Size › Custom ────────────────────────────────────────────────────────────────────────────

@Composable
internal fun AndroidAutoResolutionDetail(context: Context, onBack: () -> Unit) {
    var resolution by remember { mutableStateOf(MotoHubSettings.androidAutoResolution(context)) }
    val row: @Composable (AndroidAutoResolutionMode) -> Unit = { candidate ->
        MhChoiceRow(
            title = candidate.title(context),
            subtitle = candidate.subtitle(),
            selected = resolution == candidate,
            onClick = {
                resolution = candidate
                MotoHubSettings.setAndroidAutoResolution(context, candidate)
                ProjectionEventLog.record("SETTINGS", "Android Auto resolution changed to ${candidate.name}.")
            }
        )
    }
    MhScreen(title = motoHubText("Resolution"), onBack = onBack) {
        // Every source the Android Auto protocol defines, split the way a rider thinks about
        // them - the shape of their dashboard first, the number of pixels second.
        MhListGroup { AndroidAutoResolutionMode.entries.filter { it.preset == null }.forEach { row(it) } }
        MhSectionHeader(motoHubText("Landscape"))
        MhListGroup { AndroidAutoResolutionMode.entries.filter { it.landscape }.forEach { row(it) } }
        MhSectionHeader(motoHubText("Portrait"))
        MhListGroup {
            AndroidAutoResolutionMode.entries.filter { it.preset != null && !it.landscape }.forEach { row(it) }
        }
    }
}

/** Pixels are machine values, so they are not translated; AUTO keeps its catalogue label. */
internal fun AndroidAutoResolutionMode.title(context: Context): String =
    preset?.source?.let { "${it.width} × ${it.height}" } ?: context.getString(labelRes)

// The warning is part of the sentence rather than a badge: these sources are not worse, they are
// unproven, and a rider choosing one should read why before they ride on it.
private fun AndroidAutoResolutionMode.subtitle(): String = when (this) {
    AndroidAutoResolutionMode.AUTO -> motoHubText("Matches your dashboard")
    AndroidAutoResolutionMode.LANDSCAPE_SD, AndroidAutoResolutionMode.PORTRAIT_SD -> motoHubText("Standard")
    AndroidAutoResolutionMode.LANDSCAPE_HD, AndroidAutoResolutionMode.PORTRAIT_HD ->
        motoHubText("Sharper, more phone load")
    AndroidAutoResolutionMode.LANDSCAPE_FHD -> motoHubText("Experimental · untested on any dashboard")
    AndroidAutoResolutionMode.LANDSCAPE_UHD, AndroidAutoResolutionMode.PORTRAIT_UHD ->
        motoHubText("Experimental · 4K, expect heat and drops")
}

@Composable
internal fun AndroidAutoDensityDetail(context: Context, onBack: () -> Unit) {
    var density by remember { mutableStateOf(MotoHubSettings.androidAutoDensity(context)) }
    MhScreen(title = motoHubText("Interface size"), onBack = onBack) {
        MhListGroup {
            AndroidAutoDensityMode.entries.forEach { candidate ->
                MhChoiceRow(
                    title = context.getString(candidate.labelRes),
                    subtitle = when (candidate) {
                        AndroidAutoDensityMode.AUTO -> motoHubText("Matches the resolution")
                        AndroidAutoDensityMode.DPI_120 -> motoHubText("Smallest, most map")
                        AndroidAutoDensityMode.DPI_160 -> motoHubText("Small · landscape default")
                        AndroidAutoDensityMode.DPI_213 -> motoHubText("Between small and standard")
                        AndroidAutoDensityMode.DPI_240 -> motoHubText("Standard · portrait default")
                        AndroidAutoDensityMode.DPI_320 -> motoHubText("Large, easier with gloves")
                        AndroidAutoDensityMode.DPI_480 -> motoHubText("Largest, high resolutions only")
                    },
                    selected = density == candidate,
                    onClick = {
                        density = candidate
                        MotoHubSettings.setAndroidAutoDensity(context, candidate)
                        ProjectionEventLog.record("SETTINGS", "Android Auto density changed to ${candidate.name}.")
                    }
                )
            }
        }
        MhFootnote(motoHubText("Higher dpi means bigger buttons and less map."))
    }
}

/**
 * Nothing paired, as far as this phone can tell: no bonded device at all, or no grant to look.
 * ponytail: the bond list can't say which device is the motorcycle, so a phone with only
 * headphones paired counts as paired; telling them apart needs the dashboard's address.
 */
@SuppressLint("MissingPermission")
private fun nothingPaired(context: Context): Boolean {
    if (!BluetoothStatus.hasConnectPermission(context)) return true
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return true
    return runCatching { adapter.bondedDevices.isEmpty() }.getOrDefault(true)
}
