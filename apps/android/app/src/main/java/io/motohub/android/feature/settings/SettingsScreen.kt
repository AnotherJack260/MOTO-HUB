// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.motohub.android.BuildConfig
import io.motohub.android.R
import io.motohub.android.data.MotorcycleProfileStore
import io.motohub.android.feature.controls.HandlebarControlStore
import io.motohub.android.feature.controls.HandlebarHidCaptureService
import io.motohub.android.feature.controls.HandlebarInputMode
import io.motohub.android.feature.controls.HandlebarMappingScreen
import io.motohub.android.feature.controls.HandlebarPressHud
import io.motohub.android.feature.controls.MediaButtonBridge
import io.motohub.android.feature.diagnostics.report.SupportIdSection
import io.motohub.android.feature.home.AdvancedPromoRow
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.TBoxCapabilityStore
import io.motohub.android.tbox.TBoxClockAskRegistry
import io.motohub.android.tbox.TBoxWireLadder
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTabPage
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.ScreenSlideTransition
import io.motohub.android.ui.theme.MotoHubColors

private enum class SettingsDetail {
    LANGUAGE, AUTOSTART, VIDEO, ANDROID_AUTO, ANDROID_AUTO_RESOLUTION, ANDROID_AUTO_DENSITY, HANDLEBAR,
    HANDLEBAR_MAPPING, AUTOMATION, CLOCK, DIAGNOSTICS, DEVELOPER
}

/** Where back goes: the screen that opened this one, or the root. */
private val SettingsDetail?.parent: SettingsDetail?
    get() = when (this) {
        SettingsDetail.ANDROID_AUTO_RESOLUTION, SettingsDetail.ANDROID_AUTO_DENSITY -> SettingsDetail.ANDROID_AUTO
        SettingsDetail.HANDLEBAR_MAPPING -> SettingsDetail.HANDLEBAR
        else -> null
    }

private val SettingsDetail?.depth: Int get() = if (this == null) 0 else 1 + parent.depth

@Composable
fun SettingsTabContent(
    onOpenNetworkDiagnostics: () -> Unit,
    onOpenClockLab: () -> Unit,
    onOpenBleExplorer: () -> Unit,
    onOpenApplicationLogs: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenAndroidAutoHelp: () -> Unit,
    seamlessResumeEnabled: Boolean,
    onSeamlessResumeChanged: (Boolean) -> Unit
) {
    var detail by rememberSaveable { mutableStateOf<SettingsDetail?>(null) }
    // ScreenSlideTransition takes its direction from isBase(target) alone, which fits one card over
    // a floor. Settings goes three deep (Android Auto > Resolution), so it is told directly whether
    // this move went up a level - otherwise back from Resolution would slide in like a new screen.
    var goingUp by remember { mutableStateOf(false) }
    val go: (SettingsDetail?) -> Unit = { to ->
        goingUp = to.depth < detail.depth
        detail = to
    }

    // System back needs nothing here: every sub-screen is an MhScreen, whose BackHandler does what
    // its arrow does - the parent, so back from Button mapping lands on Handlebar buttons.
    ScreenSlideTransition(detail, isBase = { goingUp }, label = "settings") { shown ->
        val back = { go(shown.parent) }
        when (shown) {
            null -> SettingsRoot(
                open = go,
                onOpenAbout = onOpenAbout,
                onOpenAdvanced = onOpenAdvanced,
                onOpenAndroidAutoHelp = onOpenAndroidAutoHelp
            )
            SettingsDetail.LANGUAGE -> LanguageDetail(back)
            SettingsDetail.AUTOSTART -> AutostartDetail(back)
            SettingsDetail.VIDEO -> VideoQualityDetail(back)
            SettingsDetail.ANDROID_AUTO -> AndroidAutoDetail(
                onBack = back,
                onOpenResolution = { go(SettingsDetail.ANDROID_AUTO_RESOLUTION) },
                onOpenDensity = { go(SettingsDetail.ANDROID_AUTO_DENSITY) }
            )
            SettingsDetail.ANDROID_AUTO_RESOLUTION -> AndroidAutoResolutionDetail(back)
            SettingsDetail.ANDROID_AUTO_DENSITY -> AndroidAutoDensityDetail(back)
            SettingsDetail.HANDLEBAR -> HandlebarDetail(
                onBack = back,
                onOpenMapping = { go(SettingsDetail.HANDLEBAR_MAPPING) }
            )
            // The calibration-first mapping screen (shared with the companion app): one card
            // per PHYSICAL button, taught by pressing, instead of raw Bluetooth gesture names
            // that lie on half the dashes (a 700MT's held rocker arrives as "next track").
            SettingsDetail.HANDLEBAR_MAPPING -> HandlebarMappingScreen(onBack = back)
            SettingsDetail.AUTOMATION -> AutomationDetail(back, seamlessResumeEnabled, onSeamlessResumeChanged)
            SettingsDetail.CLOCK -> ClockDetail(back)
            SettingsDetail.DIAGNOSTICS -> MhScreen(title = motoHubText("Diagnostics"), onBack = back) {
                SupportIdSection(onOpenApplicationLogs = onOpenApplicationLogs)
            }
            SettingsDetail.DEVELOPER -> DeveloperDetail(
                onBack = back,
                onOpenNetworkDiagnostics = onOpenNetworkDiagnostics,
                onOpenClockLab = onOpenClockLab,
                onOpenBleExplorer = onOpenBleExplorer
            )
        }
    }
}

// Values are read from the stores in composition rather than remembered: the root is composed
// afresh each time a sub-screen slides away, so it always shows what was just chosen.
@Composable
private fun SettingsRoot(
    open: (SettingsDetail) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenAndroidAutoHelp: () -> Unit
) {
    val context = LocalContext.current
    var autoUpdateChecks by remember { mutableStateOf(MotoHubSettings.autoUpdateChecks(context)) }
    MhTabPage(context.getString(R.string.settings_title)) {
        MhSectionHeader(motoHubText("On the motorcycle"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Video quality"),
                icon = Icons.Rounded.HighQuality,
                value = "${context.getString(MotoHubSettings.videoQuality(context).labelRes)} · " +
                    context.getString(MotoHubSettings.videoPowerMode(context).labelRes),
                onClick = { open(SettingsDetail.VIDEO) }
            )
            MhListRow(
                title = motoHubText("Android Auto"),
                icon = Icons.Rounded.DirectionsCar,
                value = androidAutoValue(context),
                onClick = { open(SettingsDetail.ANDROID_AUTO) }
            )
            MhListRow(
                title = motoHubText("Handlebar buttons"),
                icon = Icons.Rounded.SportsEsports,
                value = if (HandlebarControlStore.isEnabled(context)) motoHubText("On") else motoHubText("Off"),
                onClick = { open(SettingsDetail.HANDLEBAR) }
            )
        }
        MhSectionHeader(motoHubText("Connection"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Start automatically"),
                icon = Icons.Rounded.PlayCircle,
                value = if (MotoHubSettings.autostartEnabled(context)) {
                    MotoHubSettings.autostartService(context).title()
                } else {
                    motoHubText("Off")
                },
                onClick = { open(SettingsDetail.AUTOSTART) }
            )
            MhListRow(
                title = motoHubText("Auto-connect and recovery"),
                icon = Icons.Rounded.Wifi,
                onClick = { open(SettingsDetail.AUTOMATION) }
            )
            // No value: three independent switches don't reduce to one word.
            MhListRow(
                title = motoHubText("Dashboard clock"),
                icon = Icons.Rounded.Schedule,
                onClick = { open(SettingsDetail.CLOCK) }
            )
        }
        MhSectionHeader(motoHubText("Help"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Android Auto won't start"),
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                onClick = onOpenAndroidAutoHelp
            )
            MhListRow(
                title = motoHubText("Diagnostics"),
                icon = Icons.Rounded.PrivacyTip,
                onClick = { open(SettingsDetail.DIAGNOSTICS) }
            )
        }
        MhSectionHeader(motoHubText("App"))
        MhListGroup {
            if (AppLanguageManager.isSupported) {
                MhListRow(
                    title = context.getString(R.string.language_title),
                    icon = Icons.Rounded.Translate,
                    value = context.getString(AppLanguageManager.current(context).labelRes),
                    onClick = { open(SettingsDetail.LANGUAGE) }
                )
            }
            MhSwitchRow(
                title = context.getString(R.string.settings_check_updates_on_launch),
                subtitle = motoHubText("At most once a day"),
                icon = Icons.Rounded.SystemUpdate,
                checked = autoUpdateChecks,
                onCheckedChange = {
                    autoUpdateChecks = it
                    MotoHubSettings.setAutoUpdateChecks(context, it)
                    ProjectionEventLog.record("SETTINGS", "Automatic update checks changed to enabled=$it.")
                }
            )
            MhListRow(
                title = motoHubText("About MOTO-HUB"),
                icon = Icons.Rounded.Info,
                value = BuildConfig.VERSION_NAME,
                onClick = onOpenAbout
            )
            // The always-reachable way to the ADV-SOLO page: Ride shows the same row only while
            // nothing is connecting or streaming. Draws nothing in ADVANCED.
            AdvancedPromoRow(onOpenDetails = onOpenAdvanced)
        }
        MhListGroup {
            MhListRow(
                title = motoHubText("Developer tools"),
                icon = Icons.Rounded.Code,
                onClick = { open(SettingsDetail.DEVELOPER) }
            )
        }
    }
}

/** "Auto", or the source in pixels, plus the interface size when it is not the source's own. */
private fun androidAutoValue(context: Context): String {
    val source = MotoHubSettings.androidAutoResolution(context).title(context)
    val density = MotoHubSettings.androidAutoDensity(context)
    return if (density == AndroidAutoDensityMode.AUTO) source else "$source · ${context.getString(density.labelRes)}"
}

/** Pixels are machine values, so they are not translated; AUTO keeps its catalogue label. */
private fun AndroidAutoResolutionMode.title(context: Context): String =
    preset?.source?.let { "${it.width} × ${it.height}" } ?: context.getString(labelRes)

private fun AutostartService.title(): String = when (this) {
    AutostartService.MIRRORING -> motoHubText("Mirroring")
    AutostartService.ANDROID_AUTO -> motoHubText("Android Auto")
    AutostartService.RIDE_DASHBOARD -> motoHubText("Ride Dashboard")
}

@Composable
private fun LanguageDetail(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val current = AppLanguageManager.current(context)
    MhScreen(title = context.getString(R.string.language_title), onBack = onBack) {
        MhListGroup {
            AppLanguage.entries.forEach { language ->
                MhChoiceRow(
                    // Native names, so a rider stuck in a language they can't read still finds theirs.
                    title = context.getString(language.labelRes),
                    subtitle = if (language == AppLanguage.SYSTEM) motoHubText("Follows the phone") else null,
                    selected = current == language,
                    onClick = {
                        if (current != language) {
                            AppLanguageManager.set(context, language)
                            ProjectionEventLog.record(
                                "SETTINGS",
                                "Application language changed to ${language.tag ?: "system default"}."
                            )
                            // LocaleManager updates the application configuration; recreating
                            // the activity makes every Compose screen pick up the new resources.
                            activity?.recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AutostartDetail(onBack: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(MotoHubSettings.autostartEnabled(context)) }
    var service by remember { mutableStateOf(MotoHubSettings.autostartService(context)) }
    MhScreen(title = motoHubText("Start automatically"), onBack = onBack) {
        MhListGroup {
            MhSwitchRow(
                title = motoHubText("Start on connect"),
                subtitle = motoHubText("Skips the mode picker when the motorcycle connects"),
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    MotoHubSettings.setAutostartEnabled(context, it)
                    ProjectionEventLog.record("SETTINGS", "Autostart on connect changed to enabled=$it.")
                }
            )
        }
        MhSectionHeader(motoHubText("What to start"))
        MhListGroup {
            AutostartService.entries
                .filter { BuildConfig.IS_PRO || !it.advancedOnly }
                .forEach { candidate ->
                    MhChoiceRow(
                        title = candidate.title(),
                        subtitle = if (candidate == AutostartService.MIRRORING) {
                            motoHubText("Android asks to allow screen capture each time")
                        } else {
                            null
                        },
                        selected = service == candidate,
                        onClick = {
                            service = candidate
                            MotoHubSettings.setAutostartService(context, candidate)
                            ProjectionEventLog.record("SETTINGS", "Autostart service changed to ${candidate.name}.")
                        }
                    )
                }
        }
        MhFootnote(motoHubText("Runs once per app launch. Stop streaming and you're back in control."))
    }
}

@Composable
private fun VideoQualityDetail(onBack: () -> Unit) {
    val context = LocalContext.current
    var quality by remember { mutableStateOf(MotoHubSettings.videoQuality(context)) }
    var powerMode by remember { mutableStateOf(MotoHubSettings.videoPowerMode(context)) }
    MhScreen(title = motoHubText("Video quality"), onBack = onBack) {
        MhSectionHeader(motoHubText("Picture"))
        MhListGroup {
            VideoQuality.entries.forEach { candidate ->
                MhChoiceRow(
                    title = context.getString(candidate.labelRes),
                    subtitle = when (candidate) {
                        VideoQuality.SMOOTHER -> motoHubText("Lower bitrate, less heat and network load")
                        VideoQuality.BALANCED -> motoHubText("Recommended")
                        VideoQuality.SHARPER -> motoHubText("Higher bitrate for crisper maps and text")
                    },
                    selected = quality == candidate,
                    onClick = {
                        quality = candidate
                        MotoHubSettings.setVideoQuality(context, candidate)
                        ProjectionEventLog.record("SETTINGS", "Video quality changed to ${candidate.name}.")
                    }
                )
            }
        }
        MhSectionHeader(motoHubText("Power mode"))
        MhListGroup {
            VideoPowerMode.entries.forEach { candidate ->
                MhChoiceRow(
                    title = context.getString(candidate.labelRes),
                    subtitle = when (candidate) {
                        VideoPowerMode.AUTO -> motoHubText("Adapts to phone heat and Wi-Fi")
                        VideoPowerMode.SMOOTH -> motoHubText("30 fps")
                        VideoPowerMode.BALANCED -> motoHubText("24 fps")
                        VideoPowerMode.SAVER -> motoHubText("20 fps, less heat and battery")
                    },
                    selected = powerMode == candidate,
                    onClick = {
                        powerMode = candidate
                        MotoHubSettings.setVideoPowerMode(context, candidate)
                        ProjectionEventLog.record("SETTINGS", "Video power mode changed to ${candidate.name}.")
                    }
                )
            }
        }
    }
}

@Composable
private fun AndroidAutoDetail(onBack: () -> Unit, onOpenResolution: () -> Unit, onOpenDensity: () -> Unit) {
    val context = LocalContext.current
    var aspectMatching by remember { mutableStateOf(MotoHubSettings.androidAutoAspectMatching(context)) }
    var disableTouchscreen by remember { mutableStateOf(MotoHubSettings.disableTouchscreen(context)) }
    MhScreen(title = motoHubText("Android Auto"), onBack = onBack) {
        // Nine coded sources and seven densities do not belong on one scrolling page next to the
        // margins picker: each is its own question, so each gets its own screen and this one shows
        // the answers. Read straight from the store rather than remembered - returning from a
        // child recomposes this screen, and a remembered copy would show the old choice.
        MhListGroup {
            MhListRow(
                title = motoHubText("Resolution"),
                value = MotoHubSettings.androidAutoResolution(context).title(context),
                onClick = onOpenResolution
            )
            MhListRow(
                title = motoHubText("Interface size"),
                value = context.getString(MotoHubSettings.androidAutoDensity(context).labelRes),
                onClick = onOpenDensity
            )
        }
        MhSectionHeader(motoHubText("Screen margins"))
        MhListGroup {
            AndroidAutoAspectMatchingMode.entries.forEach { candidate ->
                MhChoiceRow(
                    title = context.getString(candidate.labelRes),
                    subtitle = when (candidate) {
                        AndroidAutoAspectMatchingMode.AUTO -> motoHubText("Use the whole screen")
                        AndroidAutoAspectMatchingMode.MANUAL -> motoHubText("Use the screen margins set in Garage")
                    },
                    selected = aspectMatching == candidate,
                    onClick = {
                        aspectMatching = candidate
                        MotoHubSettings.setAndroidAutoAspectMatching(context, candidate)
                        ProjectionEventLog.record("SETTINGS", "Android Auto aspect matching changed to ${candidate.name}.")
                    }
                )
            }
        }
        MhSectionHeader(motoHubText("Controls"))
        MhListGroup {
            MhSwitchRow(
                title = motoHubText("Ignore the touchscreen"),
                subtitle = motoHubText("Use handlebar buttons even on a touch dashboard"),
                checked = disableTouchscreen,
                onCheckedChange = {
                    disableTouchscreen = it
                    MotoHubSettings.setDisableTouchscreen(context, it)
                    ProjectionEventLog.record("SETTINGS", "Disable touchscreen changed to enabled=$it.")
                }
            )
        }
    }
}

@Composable
private fun AndroidAutoResolutionDetail(onBack: () -> Unit) {
    val context = LocalContext.current
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

// The warning is part of the sentence rather than a badge: these sources are not worse, they are
// unproven, and a rider choosing one should read why before they ride on it.
private fun AndroidAutoResolutionMode.subtitle(): String = when (this) {
    AndroidAutoResolutionMode.AUTO -> motoHubText("Matches your dashboard")
    AndroidAutoResolutionMode.LANDSCAPE_SD, AndroidAutoResolutionMode.PORTRAIT_SD -> motoHubText("Standard")
    AndroidAutoResolutionMode.LANDSCAPE_HD, AndroidAutoResolutionMode.PORTRAIT_HD ->
        motoHubText("Sharper, more phone load")
    AndroidAutoResolutionMode.LANDSCAPE_FHD -> motoHubText("Experimental · untested on any dashboard")
    AndroidAutoResolutionMode.LANDSCAPE_QHD, AndroidAutoResolutionMode.PORTRAIT_QHD ->
        motoHubText("Experimental · heavy on the phone")
    AndroidAutoResolutionMode.LANDSCAPE_UHD, AndroidAutoResolutionMode.PORTRAIT_UHD ->
        motoHubText("Experimental · 4K, expect heat and drops")
}

@Composable
private fun AndroidAutoDensityDetail(onBack: () -> Unit) {
    val context = LocalContext.current
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

@Composable
private fun HandlebarDetail(onBack: () -> Unit, onOpenMapping: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(HandlebarControlStore.isEnabled(context)) }
    var pressBanner by remember { mutableStateOf(HandlebarPressHud.isEnabled(context)) }
    var inputMode by remember { mutableStateOf(HandlebarControlStore.inputMode(context)) }
    // Granted outside this app, in system settings, so the only moment it can have changed is a
    // return to this screen - hence the resume watch rather than a plain read in composition.
    var hidServiceEnabled by remember { mutableStateOf(HandlebarHidCaptureService.isEnabled(context)) }
    var openedAccessibilitySettings by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hidServiceEnabled = HandlebarHidCaptureService.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                    val applied = MediaButtonBridge.setTargetCaptureActive(MediaButtonBridge.TARGET_ANDROID_AUTO, value)
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
            MhSwitchRow(
                title = motoHubText("Show button presses on the dashboard"),
                subtitle = motoHubText("A one-second banner names each press"),
                checked = pressBanner,
                onCheckedChange = {
                    pressBanner = it
                    HandlebarPressHud.setEnabled(context, it)
                    ProjectionEventLog.record("SETTINGS", "Press banner changed to enabled=$it.")
                }
            )
        }
        MhSectionHeader(motoHubText("Music volume"))
        MhListGroup {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = MaterialTheme.colorScheme.onSurfaceVariant
                Icon(Icons.AutoMirrored.Rounded.VolumeDown, contentDescription = null, tint = tint)
                Slider(
                    value = listeningVolume,
                    onValueChange = { listeningVolume = it },
                    onValueChangeFinished = { MediaButtonBridge.setVolume(context, listeningVolume.toInt()) },
                    modifier = Modifier.weight(1f).semantics { contentDescription = motoHubText("Music volume") },
                    valueRange = 0f..volumeLevels.second.toFloat(),
                    steps = (volumeLevels.second - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.onSurface,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MotoHubColors.Fill,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = tint)
            }
        }
        MhFootnote(motoHubText("While buttons control Android Auto, set your listening level here."))
        MhSectionHeader(motoHubText("Input protocol"))
        MhListGroup {
            HandlebarInputMode.entries.forEach { candidate ->
                MhChoiceRow(
                    title = when (candidate) {
                        HandlebarInputMode.AVRCP -> motoHubText("AVRCP (media keys)")
                        HandlebarInputMode.HID -> motoHubText("HID (D-pad)")
                    },
                    subtitle = when (candidate) {
                        HandlebarInputMode.AVRCP -> motoHubText("Most dashboards")
                        HandlebarInputMode.HID -> motoHubText("Remotes that pair as a Bluetooth keyboard")
                    },
                    selected = inputMode == candidate,
                    onClick = {
                        inputMode = candidate
                        HandlebarControlStore.setInputMode(context, candidate)
                        // A session may well be running while the rider is in here - that is when
                        // they discover the protocol is wrong. Without this the switch takes effect
                        // only at the next session start.
                        MediaButtonBridge.inputModeChanged()
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
                    body = motoHubText("HID presses aren't seen until you turn it on."),
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
    }
}

@Composable
private fun AutomationDetail(
    onBack: () -> Unit,
    seamlessResumeEnabled: Boolean,
    onSeamlessResumeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var autoConnect by remember { mutableStateOf(MotoHubSettings.autoConnect(context)) }
    var autoRecovery by remember { mutableStateOf(MotoHubSettings.autoRecovery(context)) }
    MhScreen(title = motoHubText("Auto-connect and recovery"), onBack = onBack) {
        MhListGroup {
            MhSwitchRow(
                title = motoHubText("Auto-connect on launch"),
                subtitle = motoHubText("Look for the motorcycle when the app opens"),
                checked = autoConnect,
                onCheckedChange = {
                    autoConnect = it
                    MotoHubSettings.setAutoConnect(context, it)
                    ProjectionEventLog.record("SETTINGS", "Auto-connect changed to enabled=$it.")
                }
            )
            MhSwitchRow(
                title = motoHubText("Recover stalled streams"),
                subtitle = motoHubText("Restarts Android Auto if it freezes"),
                checked = autoRecovery,
                onCheckedChange = {
                    autoRecovery = it
                    MotoHubSettings.setAutoRecovery(context, it)
                    ProjectionEventLog.record("SETTINGS", "Auto-recovery changed to enabled=$it.")
                }
            )
            // MainActivity owns this one: turning it on may first need "Display over other apps",
            // and says so if the rider comes back without it.
            MhSwitchRow(
                title = motoHubText("Seamless resume"),
                subtitle = motoHubText("Android Auto resumes after a long drop, screen off too"),
                checked = seamlessResumeEnabled,
                onCheckedChange = onSeamlessResumeChanged
            )
        }
    }
}

@Composable
private fun ClockDetail(onBack: () -> Unit) {
    val context = LocalContext.current
    var dashClock by remember { mutableStateOf(MotoHubSettings.dashClockSync(context)) }
    var bluetoothClock by remember { mutableStateOf(MotoHubSettings.bluetoothClockSync(context)) }
    var keepWifiDirect by remember { mutableStateOf(MotoHubSettings.keepWifiDirectAfterDisconnect(context)) }
    // Some firmware asks MOTO-HUB for the time, is answered, and goes on counting from its own
    // power-on anyway. On those the switch below cannot do anything in either position, and a
    // rider with no way of knowing that spends the evening toggling it - so it is said here,
    // from what that dashboard has actually been seen doing on this phone.
    val dashDiscardsClock = remember {
        runCatching {
            val motorcycle = MotorcycleProfileStore(context).load()
            val capabilities = motorcycle?.let { TBoxCapabilityStore(context).load(it)?.capabilities }
            TBoxClockAskRegistry.discardsTime(context, TBoxWireLadder.fingerprintOf(capabilities))
        }.getOrDefault(false)
    }
    MhScreen(title = motoHubText("Dashboard clock"), onBack = onBack) {
        MhListGroup {
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
                    MotoHubSettings.setDashClockSync(context, it)
                    ProjectionEventLog.record("SETTINGS", "Dash clock sync changed to enabled=$it.")
                }
            )
            MhSwitchRow(
                title = motoHubText("Set the clock over Bluetooth"),
                subtitle = motoHubText("Experimental. For dashboards stuck at 00:00."),
                checked = bluetoothClock,
                onCheckedChange = {
                    bluetoothClock = it
                    MotoHubSettings.setBluetoothClockSync(context, it)
                    ProjectionEventLog.record("SETTINGS", "Bluetooth clock sync changed to enabled=$it.")
                }
            )
            MhSwitchRow(
                title = motoHubText("Stay on the dashboard's Wi-Fi"),
                subtitle = motoHubText("For dashboards that lose the time after disconnecting"),
                checked = keepWifiDirect,
                onCheckedChange = {
                    keepWifiDirect = it
                    MotoHubSettings.setKeepWifiDirectAfterDisconnect(context, it)
                    ProjectionEventLog.record("SETTINGS", "Keep Wi-Fi Direct after disconnect changed to enabled=$it.")
                }
            )
        }
        MhFootnote(
            motoHubText(
                "Turn the Wi-Fi clock off only if your dashboard still shows 01.01.1970; there it can " +
                    "overwrite a time set by hand. The Bluetooth clock needs the motorcycle paired with " +
                    "this phone first."
            )
        )
    }
}

@Composable
private fun DeveloperDetail(
    onBack: () -> Unit,
    onOpenNetworkDiagnostics: () -> Unit,
    onOpenClockLab: () -> Unit,
    onOpenBleExplorer: () -> Unit
) {
    val context = LocalContext.current
    // The master switch lives in Diagnostics, a different screen, so reading it on the way in is
    // enough: it cannot change while this one is open.
    val loggingEnabled = remember { MotoHubSettings.loggingEnabled(context) }
    var verboseLogging by remember { mutableStateOf(MotoHubSettings.verboseTBoxLogging(context)) }
    MhScreen(title = motoHubText("Developer tools"), onBack = onBack) {
        MhListGroup {
            MhListRow(
                title = motoHubText("Network diagnostics"),
                subtitle = motoHubText("T-Box discovery, Wi-Fi binding, cellular routes"),
                icon = Icons.Rounded.NetworkCheck,
                onClick = onOpenNetworkDiagnostics
            )
            MhListRow(
                title = motoHubText("Dash clock lab"),
                subtitle = motoHubText("Experiments for dashes that reset the clock (Zontes, Voge)"),
                icon = Icons.Rounded.Schedule,
                onClick = onOpenClockLab
            )
            MhListRow(
                title = motoHubText("Bluetooth LE explorer"),
                subtitle = motoHubText("Scan, connect and read any BLE device byte by byte"),
                icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                onClick = onOpenBleExplorer
            )
        }
        MhSectionHeader(motoHubText("Logging"))
        MhListGroup {
            MhSwitchRow(
                title = motoHubText("Verbose T-Box logging"),
                subtitle = if (loggingEnabled) {
                    motoHubText("Protocol detail and hex dumps")
                } else {
                    motoHubText("Needs the diagnostic log on")
                },
                checked = verboseLogging,
                enabled = loggingEnabled,
                onCheckedChange = {
                    verboseLogging = it
                    MotoHubSettings.setVerboseTBoxLogging(context, it)
                    ProjectionEventLog.record("SETTINGS", "Verbose T-Box logging changed to enabled=$it.")
                }
            )
        }
    }
}
