// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.settings

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.motohub.android.BuildConfig
import io.motohub.android.R
import io.motohub.android.feature.diagnostics.report.SupportIdSection
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTabPage
import io.motohub.android.ui.components.ScreenSlideTransition

/**
 * App-wide settings only. Everything that belongs to a motorcycle (Android Auto, image, frame
 * rate, autostart, handlebar, clocks) lives in Garage › Motorcycle details, per motorcycle.
 */
private enum class SettingsDetail { LANGUAGE, AUTOMATION, DIAGNOSTICS, DEVELOPER }

@Composable
fun SettingsTabContent(
    onOpenNetworkDiagnostics: () -> Unit,
    onOpenClockLab: () -> Unit,
    onOpenBleExplorer: () -> Unit,
    onOpenApplicationLogs: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAndroidAutoHelp: () -> Unit,
    seamlessResumeEnabled: Boolean,
    onSeamlessResumeChanged: (Boolean) -> Unit,
    // True on the Settings root, false on any sub-screen: the dock shows only on tab roots.
    onAtRootChanged: (Boolean) -> Unit = {}
) {
    var detail by rememberSaveable { mutableStateOf<SettingsDetail?>(null) }
    LaunchedEffect(detail) { onAtRootChanged(detail == null) }

    // System back needs nothing here: every sub-screen is an MhScreen, whose BackHandler does what
    // its arrow does - back to the root, since nothing here nests any deeper.
    ScreenSlideTransition(detail, isBase = { it == null }, label = "settings") { shown ->
        val back = { detail = null }
        when (shown) {
            null -> SettingsRoot(
                open = { detail = it },
                onOpenAbout = onOpenAbout,
                onOpenAndroidAutoHelp = onOpenAndroidAutoHelp
            )
            SettingsDetail.LANGUAGE -> LanguageDetail(back)
            SettingsDetail.AUTOMATION -> AutomationDetail(back, seamlessResumeEnabled, onSeamlessResumeChanged)
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
    onOpenAndroidAutoHelp: () -> Unit
) {
    val context = LocalContext.current
    MhTabPage(context.getString(R.string.settings_title)) {
        MhSectionHeader(motoHubText("Connection"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Auto-connect and recovery"),
                icon = Icons.Rounded.Wifi,
                onClick = { open(SettingsDetail.AUTOMATION) }
            )
        }
        MhSectionHeader(motoHubText("Help"))
        MhListGroup {
            MhListRow(
                title = motoHubText("Android Auto won't start"),
                icon = Icons.AutoMirrored.Rounded.Help,
                onClick = onOpenAndroidAutoHelp
            )
            MhListRow(
                title = motoHubText("Diagnostics"),
                icon = Icons.Rounded.MonitorHeart,
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
            MhListRow(
                title = motoHubText("About MOTO-HUB"),
                icon = Icons.Rounded.Info,
                value = BuildConfig.VERSION_NAME,
                onClick = onOpenAbout
            )
        }
        MhListGroup {
            MhListRow(
                title = motoHubText("Developer tools"),
                icon = Icons.Rounded.Terminal,
                onClick = { open(SettingsDetail.DEVELOPER) }
            )
        }
    }
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
