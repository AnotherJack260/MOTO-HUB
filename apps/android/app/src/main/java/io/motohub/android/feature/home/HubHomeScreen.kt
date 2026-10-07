// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.i18n.motoHubText

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
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
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhPop
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.MhTopBar
import io.motohub.android.ui.components.MotoHubBackground
import io.motohub.android.ui.components.MotoHubNotice
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.NoticeTone
import io.motohub.android.ui.components.ScreenCrossfade
import io.motohub.android.ui.components.SystemKillNotice
import io.motohub.android.ui.components.mhPressable
import io.motohub.android.ui.components.reserveSnackbarClearance
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
    onOpenAdvancedPromo: () -> Unit = {},
    // A tap on the motorcycle's name: MainActivity opens the "Switch motorcycle" sheet.
    onSwitchMotorcycle: () -> Unit = { onTabSelected(HubTab.GARAGE) },
    // The dock shows only on the tab roots; a Settings sub-screen covers it like any pushed screen.
    showDock: Boolean = true
) {
    val session = state.session
    val destination = resolveHubDestination(session, androidAutoActive, externalDisplayActive = externalDisplayActive)
    // Whether what was started is actually on the dashboard yet, by whichever mode is running.
    val ready = when {
        androidAutoActive -> androidAutoStreaming
        externalDisplayActive -> externalDisplayStreaming
        else -> session.phase == SessionPhase.CAPTURING
    }

    // The ride-end receipt. Above the profile-trial return below, so leaving for that screen and
    // back keeps the clock. The mode is captured on the Stop tap: once the session has ended,
    // nothing says any more which one it was. Saveable, not persisted: it is about this ride only.
    val context = LocalContext.current
    var streamingSince by rememberSaveable { mutableStateOf<Long?>(null) }
    var stoppedMode by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(destination, ready) {
        if (destination == HubDestination.ACTIVE_SESSION) {
            if (ready && streamingSince == null) streamingSince = SystemClock.elapsedRealtime()
        } else {
            // Only after the rider's own Stop: a session the phone or the dashboard ended, or a
            // capture prompt the rider cancelled, is not a ride they finished.
            stoppedMode?.let { mode ->
                val streamed = streamingSince?.let { SystemClock.elapsedRealtime() - it } ?: 0L
                MotoHubSnackbar.success(context, rideReceipt(mode, streamed))
            }
            streamingSince = null
            stoppedMode = null
        }
    }

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
            // The dock pads the navigation bar itself. Consumed here, so a page inside a tab
            // (MhScreen ends its scroll clear of that bar) does not leave the same gap again above it.
            // Without the dock the page reaches the bottom edge and pads that bar itself.
            Box(
                Modifier
                    .weight(1f)
                    .then(if (showDock) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
            ) {
                ScreenCrossfade(screen = selectedTab, label = "tab") { tab ->
                    when (tab) {
                        HubTab.RIDE, HubTab.NAV, HubTab.TRIPS -> {
                            // The motorcycle answered: connected after up to 90 seconds of
                            // searching, the picture on the dashboard, or the attempt failed. The
                            // tap itself is acknowledged by the pill's dip, not a buzz.
                            HapticOnChange(destination, HapticFeedbackConstants.CONFIRM) { from, to ->
                                from == HubDestination.CONNECTING &&
                                    (to == HubDestination.MODE_SELECTION || to == HubDestination.ACTIVE_SESSION)
                            }
                            HapticOnChange(ready, HapticFeedbackConstants.CONFIRM) { from, to -> !from && to }
                            // Once per attempt: a new attempt clears the failure.
                            HapticOnChange(failure != null, HapticFeedbackConstants.REJECT) { from, to -> !from && to }

                            // Connect and Cancel now share one spot, so a glove that lands twice
                            // must not cancel the connection it just started: the slot ignores
                            // taps for a moment after every change of state. Not drawn disabled.
                            val armedAt = remember(destination) { SystemClock.uptimeMillis() }
                            val guarded: (() -> Unit) -> () -> Unit = { action ->
                                { if (SystemClock.uptimeMillis() - armedAt >= ACTION_ARM_MILLIS) action() }
                            }
                            val view = LocalView.current
                            val motorcycle = session.motorcycle
                            val shownFailure = rememberLast(failure)

                            RidePage(
                                action = {
                                    // One 56 dp action per state, swapped in place.
                                    ScreenCrossfade(screen = destination, label = "ride-action") { shown ->
                                        when (shown) {
                                            HubDestination.PAIRING -> MhPrimaryButton(
                                                motoHubText("Scan QR code"),
                                                guarded(onScanQr),
                                                icon = Icons.Rounded.QrCodeScanner
                                            )
                                            // Connect is the one thing a rider opens this app to
                                            // do; after a failure the same button is the retry, and
                                            // the banner above carries the fix. A port conflict's
                                            // retry is the companion-app one: it waits for the
                                            // rider to come back from force-stopping it.
                                            HubDestination.CONNECTION -> MhPrimaryButton(
                                                if (failure != null) motoHubText("Try again") else motoHubText("Connect"),
                                                guarded(
                                                    if (failure?.kind == RideFailureKind.PORT_CONFLICT) {
                                                        onCloseCompanionAppAndRetry
                                                    } else {
                                                        onConnectAndDiscover
                                                    }
                                                )
                                            )
                                            HubDestination.CONNECTING ->
                                                MhSecondaryButton(motoHubText("Cancel"), guarded(onCancelConnection))
                                            // The only way back once connect succeeds - without it,
                                            // the rider had no path from here to a different
                                            // motorcycle or a plain Wi-Fi release except
                                            // force-stopping the app.
                                            HubDestination.MODE_SELECTION ->
                                                MhSecondaryButton(motoHubText("Disconnect"), guarded(onDisconnect))
                                            HubDestination.ACTIVE_SESSION -> MhSecondaryButton(
                                                motoHubText("Stop streaming"),
                                                guarded {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                    stoppedMode = modeName(androidAutoActive, externalDisplayActive)
                                                    when {
                                                        androidAutoActive -> onStopAndroidAuto()
                                                        externalDisplayActive -> onStopExternalDisplay()
                                                        else -> onStopProjection()
                                                    }
                                                },
                                                destructive = true
                                            )
                                        }
                                    }
                                }
                            ) {
                                if (motorcycle == null) {
                                    PairingTitle()
                                } else {
                                    RideHero(
                                        motorcycle = motorcycle,
                                        chip = rideChip(destination, ready, riderStep != null),
                                        // At rest only: connecting or riding, the hero is the name
                                        // and the chip, and nothing else moves.
                                        showPhoto = destination == HubDestination.CONNECTION ||
                                            destination == HubDestination.MODE_SELECTION,
                                        // The chevron promises a choice; with one motorcycle there
                                        // is none.
                                        showChevron = state.motorcycles.size >= 2,
                                        onSwitch = onSwitchMotorcycle
                                    )
                                }

                                // The live failure first, and alone: an error screen shows one
                                // problem and one action.
                                Fold(failure != null) {
                                    shownFailure?.let {
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
                                // Only the failing verdict is a banner. The healthy one travels the
                                // same field and is what the confirmation sheet is built on -
                                // showing it here would put "your dashboard is not showing this"
                                // over a dashboard that is showing it.
                                Fold(deliveryWarning != null && !deliveryWarning.healthy) {
                                    DeliveryWarningBanner(onOpen = { showProfileTrial = true })
                                }
                                // Only after the phone has actually stopped a session, and only at
                                // rest: never over a live failure, a connection or a ride.
                                SystemKillNotice(
                                    visible = failure == null && (
                                        destination == HubDestination.PAIRING ||
                                            destination == HubDestination.CONNECTION
                                        ),
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Spacer(Modifier.height(16.dp))

                                // On the two resting states only, and never next to a failure: a
                                // promo never competes with a problem, a connection or a ride.
                                val promo: @Composable () -> Unit = {
                                    if (!BuildConfig.IS_PRO && failure == null) {
                                        MhListGroup { AdvancedPromoRow(onOpenAdvancedPromo) }
                                    }
                                }
                                // Connection states are not navigation, so they fade through rather
                                // than slide, and the height follows on the same clock.
                                ScreenCrossfade(screen = destination, label = "ride", animateHeight = true) { shown ->
                                    when (shown) {
                                        HubDestination.PAIRING -> PairingContent(
                                            promo = promo,
                                            onImportQrPhoto = onImportQrPhoto,
                                            onManualPairing = onManualPairing
                                        )
                                        HubDestination.CONNECTION -> ConnectionContent(
                                            promo = promo,
                                            onScanQr = onScanQr,
                                            onImportQrPhoto = onImportQrPhoto,
                                            onManualPairing = onManualPairing,
                                            onStartPhoneOnlyAndroidAuto = onStartPhoneOnlyAndroidAuto
                                        )
                                        // ?.let, not checkNotNull: a fading-out state is drawn with
                                        // the current session, which may have lost its motorcycle.
                                        HubDestination.CONNECTING -> motorcycle?.let {
                                            ConnectingContent(phase = session.phase, motorcycle = it)
                                        }
                                        HubDestination.MODE_SELECTION -> ModeSelectionContent(
                                            aoaAccessoryConnected = aoaAccessoryConnected,
                                            onStartProjection = onStartProjection,
                                            onStartAndroidAuto = onStartAndroidAuto,
                                            onStartExternalDisplay = onStartExternalDisplay
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
                                            onOpenAndroidAutoHelp = onOpenAndroidAutoSettings
                                        )
                                    }
                                }
                            }
                        }
                        HubTab.GARAGE -> garageContent()
                        HubTab.SETTINGS -> settingsContent()
                    }
                }
            }

            if (showDock) {
                HubBottomNavigation(
                    selected = selectedTab,
                    onSelect = onTabSelected,
                    rideLive = destination == HubDestination.MODE_SELECTION ||
                        destination == HubDestination.ACTIVE_SESSION
                )
            }
        }
    }
}

/** How long the action slot ignores taps after the state under it changed. */
private const val ACTION_ARM_MILLIS = 400L

/**
 * MhTabPage's frame - the same empty bar and gutter, so the title here lines up with Garage's and
 * Settings' - but with the title left to the content: on Ride it is the motorcycle's name, and
 * that name is a button. Below the scroll, the state's one [action], pinned above the dock, so the
 * thing to press is in the same place at rest, after a failure and while connecting.
 */
// ponytail: MhTabPage takes its title as a String; a title slot in the kit would replace this.
// No gradient over the slot as MhScreen draws: the scroll ends above it, so it would fade the
// background into itself.
@Composable
private fun RidePage(action: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        MhTopBar(onBack = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            content()
            Spacer(Modifier.height(16.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                // The dock under it already took the navigation bar, so there is no inset to be
                // inside of: a snackbar lands above this, never on it.
                .reserveSnackbarClearance()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            action()
        }
    }
}

/**
 * Something that folds open under what is above it, 16 dp below it, and folds away again: the gap
 * is inside the fold, so the content below glides instead of snapping by 16 dp at the end.
 */
@Composable
private fun Fold(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
        Column {
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/** The last non-null [value], so something folding away can still be drawn while it goes. */
@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    val last = remember { Last<T>() }
    if (value != null) last.value = value
    return last.value
}

private class Last<T : Any> {
    var value: T? = null
}

/**
 * The [feedback] haptic on the step from [value]'s last state to its new one, when [fires] says
 * so. Remembered from the first composition, so coming back to the tab mid-ride does not buzz.
 */
@Composable
private fun <T> HapticOnChange(value: T, feedback: Int, fires: (from: T, to: T) -> Boolean) {
    val view = LocalView.current
    var last by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (fires(last, value)) view.performHapticFeedback(feedback)
        last = value
    }
}

@Composable
private fun PairingTitle() {
    // The large-title position and metrics, so it sits where every other tab's title does.
    Column(
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
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

/** The streaming mode's name, as Settings and the mode rows say it. */
private fun modeName(androidAutoActive: Boolean, externalDisplayActive: Boolean): String = motoHubText(
    when {
        androidAutoActive -> "Android Auto"
        externalDisplayActive -> "External display"
        else -> "Mirroring"
    }
)

/** "Stopped · Mirroring · 42 min": what a ride the rider stopped streamed, and for how long. */
internal fun rideReceipt(mode: String, streamedMillis: Long): String =
    motoHubText("Stopped · %1\$s · %2\$s", mode, rideDuration(streamedMillis))

/** Under a minute, minutes, or hours and minutes - never seconds: it is a ride, not a stopwatch. */
internal fun rideDuration(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 1 -> motoHubText("<1 min")
        minutes < 60 -> motoHubText("%1\$d min", minutes)
        else -> motoHubText("%1\$d h %2\$d min", minutes / 60, minutes % 60)
    }
}

/**
 * Which motorcycle, and how it is doing: its picture (or the motorcycle glyph), the name in the
 * large-title position, the status chip and the SSID - the line a rider checks against what the
 * dashboard shows. The whole row is the switcher. The big photo is there only at rest, and only if
 * the rider took one.
 */
@Composable
private fun RideHero(
    motorcycle: MotorcycleProfile,
    chip: Pair<String, MhTone>,
    showPhoto: Boolean,
    showChevron: Boolean,
    onSwitch: () -> Unit
) {
    val photo = motorcycle.photoPath?.takeIf(String::isNotBlank)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mhPressable(MaterialTheme.shapes.medium, onClick = onSwitch)
                // LargeTitle's metrics, so the name sits where Garage's and Settings' titles do.
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photo == null) {
                MhIconCircle(Icons.Rounded.TwoWheeler, size = 56.dp)
            } else {
                MotorcyclePhoto(path = photo, modifier = Modifier.size(56.dp), shape = CircleShape)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
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
                    if (showChevron) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = motoHubText("Switch motorcycle"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
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
            }
        }
        // Folds from its top edge on the shared clock, so it moves as one with the state swap below.
        AnimatedVisibility(visible = showPhoto && photo != null, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
            MotorcyclePhoto(
                path = motorcycle.photoPath,
                modifier = Modifier
                    .padding(top = 16.dp)
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
    promo: @Composable () -> Unit,
    onImportQrPhoto: () -> Unit,
    onManualPairing: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    promo: @Composable () -> Unit,
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
        MhListGroup {
            MhListRow(
                title = motoHubText("Connection options"),
                icon = Icons.Rounded.Tune,
                onClick = { showOptions = true }
            )
        }
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
 * it - never "Try again", the pinned button is the retry. The raw message, which logic matches by
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
        // Announced as it arrives: the rider is often looking at the dashboard, not the phone.
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
private fun ConnectingContent(phase: SessionPhase, motorcycle: MotorcycleProfile) {
    val discovering = phase == SessionPhase.DISCOVERING_TBOX
    Column {
        // Two steps: the profile is always loaded by the time this shows, so a third "done" one
        // only padded the list.
        MhListGroup {
            StepRow(motoHubText("Joining %1\$s", motorcycle.ssid), done = discovering, current = !discovering)
            StepRow(motoHubText("Finding the dashboard"), current = discovering)
        }
        // On a hosted network discovery ends in a sweep that can run for a minute, and a rider
        // with no idea of that reads the wait as a hang and closes the app - which is exactly
        // what ends the search.
        Fold(discovering && motorcycle.connectionMode == TBoxConnectionMode.PHONE_HOTSPOT) {
            MhFootnote(motoHubText("This can take up to 90 seconds. Keep MOTO-HUB open."))
        }
    }
}

/**
 * One step of the connection as a timeline: a 4 dp bar, then the label. Done is white with a lime
 * check that pops in, current is lime and pulses, next is Fill. The pulse and the bar's colour are
 * read where they draw, so neither recomposes the row; the current step is a polite live region,
 * so TalkBack says "Finding the dashboard" as it starts.
 */
@Composable
private fun StepRow(text: String, done: Boolean = false, current: Boolean = false) {
    val sweep = tween<Color>(MhMotion.BASE, easing = MhMotion.Standard)
    val bar by animateColorAsState(
        when {
            done -> MaterialTheme.colorScheme.onSurface
            current -> MaterialTheme.colorScheme.primary
            else -> MotoHubColors.Fill
        },
        sweep,
        label = "step-bar"
    )
    val label by animateColorAsState(
        if (done || current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        sweep,
        label = "step-label"
    )
    // 0.35 to 1, not 1 to 0.35: with animations off the transition rests on its target, lit.
    val pulse = if (current) {
        rememberInfiniteTransition(label = "step").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "step-pulse"
        )
    } else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { if (current) liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 36.dp)
                .drawBehind { drawRoundRect(bar, alpha = pulse?.value ?: 1f, cornerRadius = CornerRadius(size.width / 2)) }
        )
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = label)
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            MhPop(done) { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun ModeSelectionContent(
    aoaAccessoryConnected: Boolean,
    onStartProjection: () -> Unit,
    onStartAndroidAuto: () -> Unit,
    onStartExternalDisplay: () -> Unit
) {
    // Equal rows, no lime: the choice is the rider's, and rows survive long translations better
    // than side-by-side tiles.
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
    onOpenAndroidAutoHelp: () -> Unit
) {
    // A report, not a message: the mode and one short line picked from this fixed list, as
    // literals the extractor finds inside the when. Anything that arrives at runtime goes to a
    // banner below, which grows.
    val subtitle = motoHubText(
        when {
            externalDisplayActive && ready -> "Streaming over USB"
            ready -> "On your dashboard"
            riderStep != null -> "Waiting for you"
            else -> "Getting ready"
        }
    )
    val shownStep = rememberLast(riderStep)
    val shownNarration = rememberLast(narration)
    Column {
        MhListGroup {
            // The row's title and icon stay put, so only the line that changed is seen to change.
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = {
                    fadeIn(tween(MhMotion.FAST)) togetherWith fadeOut(tween(MhMotion.FAST)) using
                        SizeTransform(clip = false) { _, _ -> tween(MhMotion.BASE, easing = MhMotion.Standard) }
                },
                label = "session-subtitle"
            ) { line ->
                MhListRow(
                    title = modeName(androidAutoActive, externalDisplayActive),
                    subtitle = line,
                    icon = when {
                        androidAutoActive -> Icons.Rounded.DirectionsCar
                        externalDisplayActive -> Icons.Rounded.Usb
                        else -> Icons.AutoMirrored.Rounded.ScreenShare
                    }
                )
            }
        }
        // An instruction, not a failure: it folds in, and nothing buzzes.
        Fold(riderStep != null) {
            shownStep?.let { RiderStepNotice(it, onOpenAndroidAutoHelp) }
        }
        Fold(narration != null) {
            shownNarration?.let {
                MhBanner(title = motoHubText("Starting Android Auto"), body = it, tone = MhTone.NEUTRAL)
            }
        }
        if (androidAutoActive) {
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(16.dp))
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
