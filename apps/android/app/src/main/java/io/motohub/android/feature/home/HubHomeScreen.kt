// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.i18n.motoHubText

import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.motohub.android.BuildConfig
import io.motohub.android.androidauto.AndroidAutoRuntime
import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.feature.garage.MotorcyclePhoto
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.session.SessionPhase
import io.motohub.android.session.TBoxConnectionMode
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.WifiDirectGate
import io.motohub.android.tbox.WifiGate
import io.motohub.android.ui.components.HubBottomNavigation
import io.motohub.android.ui.components.HubTab
import io.motohub.android.ui.components.MOTION_MILLIS
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTextButton
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.MhTopBar
import io.motohub.android.ui.components.MotoHubBackground
import io.motohub.android.ui.components.MotoHubNotice
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.NoticeTone
import io.motohub.android.ui.components.ScreenCrossfade
import io.motohub.android.ui.components.SystemKillNotice
import io.motohub.android.ui.theme.MotoHubColors

@Composable
fun HubHomeScreen(
    state: HubUiState,
    selectedTab: HubTab,
    onTabSelected: (HubTab) -> Unit,
    onScanQr: () -> Unit,
    onImportQrPhoto: () -> Unit,
    onManualPairing: () -> Unit,
    onTryPhoneHotspot: () -> Unit,
    onConnectAndDiscover: () -> Unit,
    companionAppName: String?,
    onCloseCompanionAppAndRetry: () -> Unit,
    onOpenCompanionAppSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenAndroidAutoSettings: () -> Unit,
    onCancelConnection: () -> Unit,
    onDisconnect: () -> Unit,
    onStartProjection: () -> Unit,
    androidAutoActive: Boolean,
    androidAutoStreaming: Boolean,
    onStartAndroidAuto: () -> Unit,
    onStopAndroidAuto: () -> Unit,
    onOpenAndroidAutoPreview: () -> Unit,
    onStartPhoneOnlyAndroidAuto: () -> Unit,
    dimDisplayEnabled: Boolean,
    onDimDisplayChanged: (Boolean) -> Unit,
    onStopProjection: () -> Unit,
    garageContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    // ── External display (USB AOA) ──
    aoaAccessoryConnected: Boolean = false,
    externalDisplayActive: Boolean = false,
    externalDisplayStreaming: Boolean = false,
    onStartExternalDisplay: () -> Unit = {},
    onStopExternalDisplay: () -> Unit = {},
    onTryProfile: (ProfileOverride) -> Unit = {},
    onKeepTrialledProfile: (sendNow: Boolean, enableAutoUpload: Boolean) -> Unit = { _, _ -> },
    onDiscardTrialledProfile: () -> Unit = {},
    diagnosticsOffer: DiagnosticsOffer? = null,
    onOpenAdvancedPromo: () -> Unit = {}
) {
    val session = state.session
    val destination = resolveHubDestination(session, androidAutoActive, externalDisplayActive = externalDisplayActive)

    // A drill-down rather than an expanding card: this is a decision with its own evidence and
    // its own list, and the rider taking it has already been told the wrong thing once.
    var showProfileTrial by rememberSaveable { mutableStateOf(false) }
    val deliveryWarning = session.deliveryWarning
    if (showProfileTrial && deliveryWarning != null) {
        MotoHubBackground(Modifier.fillMaxSize()) {
            ProfileTrialScreen(
                warning = deliveryWarning,
                suggestions = state.profileSuggestions,
                onTryProfile = {
                    showProfileTrial = false
                    onTryProfile(it)
                },
                onBack = { showProfileTrial = false }
            )
        }
        return
    }

    state.trialToConfirm?.let { trial ->
        ProfileTrialConfirmation(
            trial = trial,
            diagnostics = diagnosticsOffer,
            onKeep = onKeepTrialledProfile,
            onDiscard = onDiscardTrialledProfile
        )
    }

    // Whether what was started is actually on the dashboard yet, by whichever mode is running.
    val ready = when {
        androidAutoActive -> androidAutoStreaming
        externalDisplayActive -> externalDisplayStreaming
        else -> session.phase == SessionPhase.CAPTURING
    }
    // Starting Google Android Auto can take several seconds and several attempts; the narration
    // says which one is running so the screen is not a motionless "getting ready".
    val androidAutoStartupDetail by AndroidAutoRuntime.startupDetail.collectAsStateWithLifecycle()
    // A step the rider has to carry out by hand is not a status line: it gets a notice of its own.
    val riderStep = androidAutoStartupDetail
        ?.takeIf { androidAutoActive && !ready }
        ?.let(AndroidAutoSelfModeHelp::riderStepOf)
    // Everything Android Auto says about its own startup while it is still working on it: a
    // runtime string, sometimes a paragraph. Never a row subtitle.
    val narration = androidAutoStartupDetail
        ?.takeIf { androidAutoActive && !ready && riderStep == null }
        ?.let(::motoHubText)
    val failure = if (session.phase != SessionPhase.ERROR) null else rideFailureOf(
        session.message,
        session.failureKind,
        session.offerPhoneHotspotRetry,
        session.offerOfficialAppHelp,
        companionAppName
    )

    MotoHubBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            // Only the failing verdict is a banner. The healthy one travels the same field and
            // is what the confirmation above is built on - showing it here would put "your
            // dashboard is not showing this" over a dashboard that is showing it.
            deliveryWarning?.takeIf { !it.healthy }?.let {
                DeliveryWarningBanner(onOpen = { showProfileTrial = true })
            }

            // The dock pads the navigation bar itself. Consumed here, so a page inside a tab
            // (MhScreen ends its scroll clear of that bar) does not leave the same gap again above it.
            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) {
                ScreenCrossfade(screen = selectedTab, label = "tab") { tab ->
                    when (tab) {
                        HubTab.RIDE, HubTab.NAV, HubTab.TRIPS -> RidePage {
                            ConfirmOnChange(destination) { from, to ->
                                from == HubDestination.CONNECTION && to == HubDestination.CONNECTING
                            }
                            ConfirmOnChange(ready) { from, to -> !from && to }

                            val motorcycle = session.motorcycle
                            if (motorcycle == null) {
                                PairingTitle()
                            } else {
                                RideHero(
                                    motorcycle = motorcycle,
                                    chip = rideChip(destination, ready, riderStep != null),
                                    // At rest only: while connecting or riding, Cancel and Stop
                                    // stay above the fold. The name and chip never move.
                                    showPhoto = destination == HubDestination.CONNECTION ||
                                        destination == HubDestination.MODE_SELECTION,
                                    onSwitch = { onTabSelected(HubTab.GARAGE) }
                                )
                            }
                            // Only after the phone has actually stopped a session: the one thing
                            // on screen that explains something the rider has already lived through.
                            SystemKillNotice()

                            val errorBanner: @Composable () -> Unit = {
                                failure?.let {
                                    RideErrorBanner(
                                        failure = it,
                                        companionAppName = companionAppName,
                                        onOpenWifiSettings = onOpenWifiSettings,
                                        onTryPhoneHotspot = onTryPhoneHotspot,
                                        onOpenCompanionAppSettings = onOpenCompanionAppSettings,
                                        onOpenAndroidAutoHelp = onOpenAndroidAutoSettings
                                    )
                                }
                            }
                            // On the two resting states only: a promo never competes with a
                            // connection in progress or a ride.
                            val promo: @Composable () -> Unit = {
                                if (!BuildConfig.IS_PRO) MhListGroup { AdvancedPromoRow(onOpenAdvancedPromo) }
                            }
                            // Connection states are not navigation, so they fade rather than slide.
                            ScreenCrossfade(screen = destination, label = "ride") { shown ->
                                when (shown) {
                                    HubDestination.PAIRING -> PairingContent(
                                        errorBanner = errorBanner,
                                        promo = promo,
                                        onScanQr = onScanQr,
                                        onImportQrPhoto = onImportQrPhoto,
                                        onManualPairing = onManualPairing
                                    )
                                    HubDestination.CONNECTION -> ConnectionContent(
                                        failure = failure,
                                        errorBanner = errorBanner,
                                        promo = promo,
                                        // A port conflict's retry is the companion-app one: it
                                        // waits for the rider to come back from force-stopping it.
                                        onRetry = if (failure?.kind == RideFailureKind.PORT_CONFLICT) {
                                            onCloseCompanionAppAndRetry
                                        } else {
                                            onConnectAndDiscover
                                        },
                                        onScanQr = onScanQr,
                                        onImportQrPhoto = onImportQrPhoto,
                                        onManualPairing = onManualPairing,
                                        onStartPhoneOnlyAndroidAuto = onStartPhoneOnlyAndroidAuto
                                    )
                                    // ?.let, not checkNotNull: a fading-out state is drawn with
                                    // the current session, which may have lost its motorcycle.
                                    HubDestination.CONNECTING -> motorcycle?.let {
                                        ConnectingContent(phase = session.phase, motorcycle = it, onCancel = onCancelConnection)
                                    }
                                    HubDestination.MODE_SELECTION -> ModeSelectionContent(
                                        aoaAccessoryConnected = aoaAccessoryConnected,
                                        onStartProjection = onStartProjection,
                                        onStartAndroidAuto = onStartAndroidAuto,
                                        onStartExternalDisplay = onStartExternalDisplay,
                                        onDisconnect = onDisconnect
                                    )
                                    HubDestination.ACTIVE_SESSION -> ActiveSessionContent(
                                        androidAutoActive = androidAutoActive,
                                        externalDisplayActive = externalDisplayActive,
                                        ready = ready,
                                        riderStep = riderStep,
                                        narration = narration,
                                        dimDisplayEnabled = dimDisplayEnabled,
                                        onDimDisplayChanged = onDimDisplayChanged,
                                        onOpenAndroidAutoPreview = onOpenAndroidAutoPreview,
                                        onOpenAndroidAutoHelp = onOpenAndroidAutoSettings,
                                        onStop = when {
                                            androidAutoActive -> onStopAndroidAuto
                                            externalDisplayActive -> onStopExternalDisplay
                                            else -> onStopProjection
                                        }
                                    )
                                }
                            }
                        }
                        HubTab.GARAGE -> garageContent()
                        HubTab.SETTINGS -> settingsContent()
                    }
                }
            }

            HubBottomNavigation(
                selected = selectedTab,
                onSelect = onTabSelected,
                rideLive = destination == HubDestination.MODE_SELECTION ||
                    destination == HubDestination.ACTIVE_SESSION
            )
        }
    }
}

/**
 * MhTabPage's frame - the same empty bar and gutter, so the title here lines up with Garage's and
 * Settings' - but with the title left to the content: on Ride it is the motorcycle's name, and
 * that name is a button.
 */
// ponytail: MhTabPage takes its title as a String; a title slot in the kit would replace this.
@Composable
private fun RidePage(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MhTopBar(onBack = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The confirm haptic on the step from [value]'s last state to its new one, when [fires] says so.
 * Remembered from the first composition, so coming back to the tab mid-ride does not buzz.
 */
@Composable
private fun <T> ConfirmOnChange(value: T, fires: (from: T, to: T) -> Boolean) {
    val view = LocalView.current
    var last by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (fires(last, value)) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        last = value
    }
}

@Composable
private fun PairingTitle() {
    // The large-title position and metrics, so it sits where every other tab's title does.
    Column(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            motoHubText("Connect your motorcycle"),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            motoHubText("Scan the QR code on your dashboard to save its Wi-Fi details."),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun rideChip(destination: HubDestination, ready: Boolean, actionNeeded: Boolean): Pair<String, MhTone> =
    when (destination) {
        HubDestination.CONNECTING -> motoHubText("Connecting") to MhTone.PROGRESS
        HubDestination.MODE_SELECTION -> motoHubText("Connected") to MhTone.LIVE
        // "Starting" over a session that is in fact waiting on the rider reads as "sit still, it
        // is working on it" - the exact wrong instruction.
        HubDestination.ACTIVE_SESSION -> when {
            actionNeeded -> motoHubText("Action needed") to MhTone.WARNING
            ready -> motoHubText("Live") to MhTone.LIVE
            else -> motoHubText("Starting") to MhTone.PROGRESS
        }
        else -> motoHubText("Not connected") to MhTone.NEUTRAL
    }

/**
 * Which motorcycle, and how it is doing: the name in the large-title position (a tap switches
 * motorcycles in the Garage), the status chip and the SSID - the line a rider checks against what
 * the dashboard shows. The photo is there only at rest, and only if the rider took one.
 */
@Composable
private fun RideHero(
    motorcycle: MotorcycleProfile,
    chip: Pair<String, MhTone>,
    showPhoto: Boolean,
    onSwitch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onSwitch)
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // No maxLines: a long name wraps rather than losing its end.
            Text(
                motorcycle.displayName?.takeIf(String::isNotBlank) ?: motoHubText("My motorcycle"),
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = motoHubText("Switch motorcycle"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MhStatusChip(chip.first, chip.second)
            Text(
                motorcycle.ssid,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(
            visible = showPhoto && !motorcycle.photoPath.isNullOrBlank(),
            enter = fadeIn(tween(MOTION_MILLIS)) + expandVertically(tween(MOTION_MILLIS)),
            exit = fadeOut(tween(MOTION_MILLIS)) + shrinkVertically(tween(MOTION_MILLIS))
        ) {
            MotorcyclePhoto(
                path = motorcycle.photoPath,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .aspectRatio(16f / 9f),
                // MotorcyclePhoto types this as RoundedCornerShape, so it cannot take the theme's
                // Shapes directly; kept in step with shapes.large by hand.
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun PairingContent(
    errorBanner: @Composable () -> Unit,
    promo: @Composable () -> Unit,
    onScanQr: () -> Unit,
    onImportQrPhoto: () -> Unit,
    onManualPairing: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        errorBanner()
        MhPrimaryButton(motoHubText("Scan QR code"), onScanQr, icon = Icons.Rounded.QrCodeScanner)
        MhListGroup {
            MhListRow(
                title = motoHubText("Import QR code"),
                subtitle = motoHubText("From a photo or screenshot"),
                icon = Icons.Rounded.Image,
                onClick = onImportQrPhoto
            )
            MhListRow(
                title = motoHubText("Enter details manually"),
                subtitle = motoHubText("Wi-Fi name and password"),
                icon = Icons.Rounded.Keyboard,
                onClick = onManualPairing
            )
        }
        promo()
    }
}

@Composable
private fun ConnectionContent(
    failure: RideFailure?,
    errorBanner: @Composable () -> Unit,
    promo: @Composable () -> Unit,
    onRetry: () -> Unit,
    onScanQr: () -> Unit,
    onImportQrPhoto: () -> Unit,
    onManualPairing: () -> Unit,
    onStartPhoneOnlyAndroidAuto: () -> Unit
) {
    // Here rather than one level up, and not saveable: leaving this state (auto-connect starting,
    // say) takes the sheet with it, and ScreenCrossfade's state holder would otherwise reopen it
    // the next time the rider lands back here.
    var showOptions by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        errorBanner()
        // Connect is the one thing a rider opens this app to do; after a failure the same button
        // is the retry, and the banner above carries the fix.
        MhPrimaryButton(
            if (failure != null) motoHubText("Try again") else motoHubText("Connect"),
            onRetry,
            icon = Icons.Rounded.TwoWheeler
        )
        MhTextButton(
            motoHubText("Connection options"),
            onClick = { showOptions = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        promo()
    }
    if (showOptions) {
        MhSheet(onDismiss = { showOptions = false }, title = motoHubText("Connection options")) { close ->
            Column {
                MhListRow(
                    title = motoHubText("Scan QR code"),
                    icon = Icons.Rounded.QrCodeScanner,
                    onClick = { close(onScanQr) }
                )
                MhListRow(
                    title = motoHubText("Import QR code"),
                    subtitle = motoHubText("From a photo or screenshot"),
                    icon = Icons.Rounded.Image,
                    onClick = { close(onImportQrPhoto) }
                )
                MhListRow(
                    title = motoHubText("Enter details manually"),
                    subtitle = motoHubText("Wi-Fi name and password"),
                    icon = Icons.Rounded.Keyboard,
                    onClick = { close(onManualPairing) }
                )
                // Runs Android Auto entirely on the phone's own screen via
                // PhoneOnlyAndroidAutoBridge - no T-Box, no Bluetooth, no motorcycle required.
                // Exists for testing the Android Auto path itself (identity, handlebar mapping,
                // ...) without a bike to hand.
                MhListRow(
                    title = motoHubText("Android Auto on this phone"),
                    subtitle = motoHubText("No motorcycle needed"),
                    icon = Icons.Rounded.DirectionsCar,
                    onClick = { close(onStartPhoneOnlyAndroidAuto) }
                )
            }
        }
    }
}

/**
 * The connection failure: what went wrong in a few words, one line, and the one action that fixes
 * it - never "Try again", the hero button is the retry. The raw message, which logic matches by
 * identity and which is already in the rider's language where it can be, sits behind "Details".
 */
@Composable
private fun RideErrorBanner(
    failure: RideFailure,
    companionAppName: String?,
    onOpenWifiSettings: () -> Unit,
    onTryPhoneHotspot: () -> Unit,
    onOpenCompanionAppSettings: () -> Unit,
    onOpenAndroidAutoHelp: () -> Unit
) {
    // Not threaded up as callbacks like the others: plain navigation intents with no session
    // state behind them, and the screen each opens is fixed by the failure that produced it.
    val context = LocalContext.current
    val fix: Pair<String, () -> Unit>? = when (failure.fix) {
        RideFix.NONE -> null
        RideFix.WIFI_SETTINGS -> motoHubText("Open Wi-Fi settings") to onOpenWifiSettings
        RideFix.HOTSPOT_SETTINGS -> motoHubText("Open hotspot settings") to {
            if (!WifiGate.openHotspotSettings(context)) {
                MotoHubSnackbar.error(context, motoHubText("Couldn't open hotspot settings"))
            }
        }
        RideFix.PHONE_HOTSPOT -> motoHubText("Use phone hotspot") to onTryPhoneHotspot
        RideFix.COMPANION_APP_SETTINGS ->
            motoHubText("Open %1\$s app settings", companionAppName.orEmpty()) to onOpenCompanionAppSettings
        RideFix.ANDROID_AUTO_HELP -> motoHubText("Show me how") to onOpenAndroidAutoHelp
        RideFix.VPN_SETTINGS -> motoHubText("Open VPN settings") to {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { MotoHubSnackbar.error(context, motoHubText("Couldn't open VPN settings")) }
        }
        RideFix.APP_SETTINGS -> motoHubText("Open app settings") to {
            if (!WifiDirectGate.openAppInfo(context, context.packageName)) {
                MotoHubSnackbar.error(context, motoHubText("Couldn't open app settings"))
            }
        }
    }
    // The companion app is one possible cause among several when the dashboard does not answer,
    // so its hint waits behind the fold. For a port conflict it is the evidence and the banner's
    // own fix instead.
    val companion = companionAppName?.takeIf { failure.kind == RideFailureKind.DASH_NOT_FOUND && it.isNotBlank() }
    val raw = failure.details
    MhBanner(
        title = failure.title,
        body = failure.body,
        tone = failure.tone,
        actionLabel = fix?.first,
        onAction = fix?.second,
        details = if (raw == null) null else {
            {
                Text(raw, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (companion != null) {
                    Text(
                        motoHubText(
                            "%1\$s can keep the dashboard busy in the background. If this keeps happening, force-stop it.",
                            companion
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MhSecondaryButton(
                        motoHubText("Open %1\$s app settings", companion),
                        onOpenCompanionAppSettings,
                        fillWidth = false
                    )
                }
            }
        }
    )
}

@Composable
private fun ConnectingContent(
    phase: SessionPhase,
    motorcycle: MotorcycleProfile,
    onCancel: () -> Unit
) {
    val discovering = phase == SessionPhase.DISCOVERING_TBOX
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MhListGroup {
            StepRow(motoHubText("Profile loaded"), done = true)
            StepRow(motoHubText("Joining %1\$s", motorcycle.ssid), done = discovering, current = !discovering)
            StepRow(motoHubText("Finding the dashboard"), current = discovering)
        }
        // On a hosted network discovery ends in a sweep that can run for a minute, and a rider
        // with no idea of that reads the spinner as a hang and closes the app - which is exactly
        // what ends the search.
        if (discovering && motorcycle.connectionMode == TBoxConnectionMode.PHONE_HOTSPOT) {
            MhFootnote(motoHubText("This can take up to 90 seconds. Keep MOTO-HUB open."))
        }
        MhSecondaryButton(motoHubText("Cancel"), onCancel)
    }
}

/** One step of the connection, with MhListRow's metrics: a check, a spinner, or an empty circle. */
@Composable
private fun StepRow(text: String, done: Boolean = false, current: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (done) {
            MhIconCircle(Icons.Rounded.Check, tint = MaterialTheme.colorScheme.primary)
        } else {
            Box(Modifier.size(40.dp).background(MotoHubColors.Fill, CircleShape), contentAlignment = Alignment.Center) {
                if (current) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                }
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (done || current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModeSelectionContent(
    aoaAccessoryConnected: Boolean,
    onStartProjection: () -> Unit,
    onStartAndroidAuto: () -> Unit,
    onStartExternalDisplay: () -> Unit,
    onDisconnect: () -> Unit
) {
    // Equal rows, no lime: the choice is the rider's, and rows survive long translations better
    // than side-by-side tiles.
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MhSectionHeader(motoHubText("Show on the dashboard"))
            MhListGroup {
                MhListRow(
                    title = motoHubText("Mirroring"),
                    subtitle = motoHubText("Your phone's screen on the dashboard"),
                    icon = Icons.AutoMirrored.Rounded.ScreenShare,
                    onClick = onStartProjection
                )
                MhListRow(
                    title = motoHubText("Android Auto"),
                    subtitle = motoHubText("Maps, music and calls"),
                    icon = Icons.Rounded.DirectionsCar,
                    onClick = onStartAndroidAuto
                )
                if (aoaAccessoryConnected) {
                    MhListRow(
                        title = motoHubText("External display"),
                        subtitle = motoHubText("Stream over USB"),
                        icon = Icons.Rounded.Usb,
                        onClick = onStartExternalDisplay
                    )
                }
            }
        }
        // The only way back once connect succeeds - without it, the rider had no path from here
        // to a different motorcycle or a plain Wi-Fi release except force-stopping the app.
        MhSecondaryButton(motoHubText("Disconnect"), onDisconnect)
    }
}

@Composable
private fun ActiveSessionContent(
    androidAutoActive: Boolean,
    externalDisplayActive: Boolean,
    ready: Boolean,
    riderStep: AndroidAutoSelfModeHelp.RiderStep?,
    narration: String?,
    dimDisplayEnabled: Boolean,
    onDimDisplayChanged: (Boolean) -> Unit,
    onOpenAndroidAutoPreview: () -> Unit,
    onOpenAndroidAutoHelp: () -> Unit,
    onStop: () -> Unit
) {
    val view = LocalView.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // A report, not a message: the mode and one short line picked from this fixed list, as
        // literals the extractor finds inside the when. Anything that arrives at runtime goes to
        // a MotoHubNotice below, which grows.
        MhListGroup {
            MhListRow(
                title = motoHubText(
                    when {
                        androidAutoActive -> "Android Auto"
                        externalDisplayActive -> "External display"
                        else -> "Mirroring"
                    }
                ),
                subtitle = motoHubText(
                    when {
                        externalDisplayActive && ready -> "Streaming over USB"
                        ready -> "On your dashboard"
                        riderStep != null -> "Waiting for you"
                        else -> "Getting ready"
                    }
                ),
                icon = when {
                    androidAutoActive -> Icons.Rounded.DirectionsCar
                    externalDisplayActive -> Icons.Rounded.Usb
                    else -> Icons.AutoMirrored.Rounded.ScreenShare
                }
            )
        }
        if (riderStep != null) {
            RiderStepNotice(riderStep, onOpenAndroidAutoHelp)
        } else if (narration != null) {
            MotoHubNotice(label = motoHubText("Starting Android Auto"), tone = NoticeTone.INFO, body = narration)
        }
        if (androidAutoActive) {
            MhListGroup {
                MhListRow(
                    title = motoHubText("Preview and touch"),
                    subtitle = motoHubText("See and control Android Auto here"),
                    icon = Icons.Rounded.Visibility,
                    onClick = onOpenAndroidAutoPreview
                )
            }
        } else if (!externalDisplayActive) {
            // The dimmer works on the mirroring capture only; external display has nothing to dim.
            MhListGroup {
                MhSwitchRow(
                    title = motoHubText("Dim phone screen"),
                    subtitle = motoHubText("The dashboard stays on"),
                    icon = Icons.Rounded.Brightness4,
                    checked = dimDisplayEnabled,
                    onCheckedChange = onDimDisplayChanged
                )
            }
        }
        MhSecondaryButton(
            motoHubText("Stop streaming"),
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onStop()
            },
            destructive = true
        )
    }
}

/**
 * The one thing Android Auto needs from the rider, set to be read at arm's length on a bike.
 *
 * Deliberately not a row subtitle: this is the only text on the screen that is an instruction
 * rather than a report, so it gets the accent panel, full onSurface contrast, the thing to tap on
 * its own line above the menu path it is buried in, and the way into the full guide for the rider
 * who has never opened Android Auto's developer settings. It is a [MotoHubNotice], so however long
 * the instruction turns out to be it is drawn in full.
 *
 * Every line goes through the catalogue. The step itself stays English wherever it is stored:
 * [AndroidAutoSelfModeHelp.RiderStep.flat] is IPC payload matched by identity, so it cannot be
 * translated at the source - it is translated here, at the one point where it is read rather
 * than compared. tools/i18n/extract.py collects the literals from the RiderStep declarations.
 */
@Composable
private fun RiderStepNotice(step: AndroidAutoSelfModeHelp.RiderStep, onOpenHelp: () -> Unit) {
    MotoHubNotice(
        label = motoHubText("Do this in Android Auto"),
        tone = NoticeTone.ACTION,
        headline = motoHubText(step.action),
        body = motoHubText(step.where),
        // The menu above only exists once Android Auto's developer options are unlocked, so the
        // rider who has not done that opens it and finds nothing. Muted: it is the one line here
        // that is background rather than the thing to do.
        footnote = step.prerequisite?.let { motoHubText(it) },
        actions = { MhSecondaryButton(motoHubText("Show me how"), onOpenHelp) }
    )
}
