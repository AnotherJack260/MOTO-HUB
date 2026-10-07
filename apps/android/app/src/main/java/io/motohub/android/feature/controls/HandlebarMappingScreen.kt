// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.controls

import android.Manifest
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.androidauto.AndroidAutoRuntime
import io.motohub.android.androidauto.AndroidAutoRuntimeState
import io.motohub.android.data.MotorcycleProfileStore
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.ui.components.MOTION_MILLIS
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhBanner
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhNavIcon
import io.motohub.android.ui.components.MhPop
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTextButton
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.ScreenSlideTransition
import io.motohub.android.ui.theme.MotoHubColors
import kotlinx.coroutines.delay

/**
 * Handlebar mapping, organised the way a rider thinks about it: one group per BUTTON, with a
 * row for press, double press and hold.
 *
 * It used to list Bluetooth gesture names instead - and those do not line up with the buttons.
 * A held up rocker on a CFMOTO 700MT arrives as the "next track" command, which on a handlebar
 * with a real left button is that button instead. Guessing produced labels that were wrong on
 * one bike or the other, so nothing is guessed here: the volume steps and play/pause are the
 * only bindings assumed, everything else is learned from the rider.
 */
@Composable
fun HandlebarMappingScreen(
    onBack: () -> Unit,
    // ponytail: MhScreen draws a back arrow, never a label; kept so the callers still compile.
    @Suppress("UNUSED_PARAMETER") backLabel: String = "",
    // The handlebar's own on/off belongs on this screen rather than on the page that opens
    // it. Applying it to a running session is not something this screen can do on its own,
    // though - which
    // projection is live, and which side of the app owns its bridge, is known only to the
    // caller. Null means the caller has no session to apply it to, and no switch is shown.
    captureControl: ((Boolean) -> Boolean)? = null,
    captureDetail: (@Composable (Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var revision by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<PhysicalPress?>(null) }
    var calibrating by remember { mutableStateOf(false) }
    var showTeachPrerequisite by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    // Set when the prerequisite sheet asked MainActivity to start a session, so the rider
    // doesn't have to notice it came up and tap "Teach my handlebar" a second time - the whole
    // point of asking was to get straight into teaching once there is something to capture.
    var awaitingSessionForTeach by remember { mutableStateOf(false) }
    val androidAutoState by AndroidAutoRuntime.state.collectAsState()
    LaunchedEffect(androidAutoState, awaitingSessionForTeach) {
        if (!awaitingSessionForTeach) return@LaunchedEffect
        // Streaming specifically, not just Preparing/ReceiverReady: MediaButtonBridge only
        // flips captureActive once video is actually flowing (see onVideoReady), so opening the
        // wizard any earlier would just look broken - presses would register nothing yet.
        if (androidAutoState is AndroidAutoRuntimeState.Streaming) {
            awaitingSessionForTeach = false
            calibrating = true
        }
    }
    val lastGesture by HandlebarGestureFeed.lastGesture.collectAsState()
    var litGesture by remember { mutableStateOf<HandlebarGesture?>(null) }
    LaunchedEffect(lastGesture?.atElapsedRealtimeMillis) {
        // The feed keeps its last event forever, so without the age check re-opening this
        // screen lights a button for a press the rider made minutes ago.
        //
        // Every path assigns the light, none of them returns leaving it as it was: the previous
        // run is cancelled the moment a newer event arrives, so a bare return would strand
        // whatever that run had switched on with nothing left to switch it off.
        val event = lastGesture
        val age = event?.let { SystemClock.elapsedRealtime() - it.atElapsedRealtimeMillis }
        if (event == null || age == null || age >= HIGHLIGHT_MILLIS) {
            litGesture = null
            return@LaunchedEffect
        }
        litGesture = event.gesture
        delay(HIGHLIGHT_MILLIS - age)
        litGesture = null
    }

    // The one way out of the wizard: Done, the close icon and system back (MhScreen sends back
    // to its onBack) all land here. Back used to only drop the flag, skipping the refresh below
    // and leaving a stale volume pin behind.
    val finishTeaching = {
        revision++
        calibrating = false
        // The taught set may have just changed whether volume presses exist on
        // this handlebar — a live capture must re-decide the volume pin NOW, not
        // at the next session (stale pin = phone volume keys acting as handlebar
        // presses).
        MediaButtonBridge.refreshVolumeGestureUse()
    }

    ScreenSlideTransition(
        screen = calibrating,
        isBase = { !it },
        modifier = Modifier.fillMaxSize()
    ) { teaching ->
        if (teaching) {
            // The pin has to be held for the whole wizard, not only while a step is on
            // screen: an AVRCP volume rocker is invisible without it, so the volume steps
            // looked dead and got skipped. Tied to the screen rather than to onDone so that
            // leaving with Back releases it too.
            DisposableEffect(Unit) {
                MediaButtonBridge.setCalibrating(true)
                onDispose { MediaButtonBridge.setCalibrating(false) }
            }
            HandlebarCalibrationScreen(onDone = finishTeaching)
        } else {
            HandlebarListContent(
                captureControl = captureControl,
                captureDetail = captureDetail,
                onBack = onBack,
                revision = revision,
                litGesture = litGesture,
                teachLoading = awaitingSessionForTeach,
                onTeach = {
                    // The wizard only ever sees a press through a live capture (MediaButtonBridge
                    // captureActive), so with no Android Auto session at all there is nothing to
                    // teach from and it would just sit there looking broken. Asked before it
                    // opens rather than inside it, so the rider is never left guessing why
                    // nothing on the motorcycle does anything once it has.
                    val sessionRunning = androidAutoState is AndroidAutoRuntimeState.Streaming ||
                        androidAutoState is AndroidAutoRuntimeState.ReceiverReady ||
                        androidAutoState is AndroidAutoRuntimeState.Preparing
                    if (sessionRunning) calibrating = true else showTeachPrerequisite = true
                },
                onEdit = { editing = it },
                onReset = { confirmReset = true }
            )
        }
    }

    // Locals, not `editing` itself: the sheet's action runs after its onDismiss has cleared it.
    val pressBeingEdited = editing
    val editingGesture = pressBeingEdited?.let { HandlebarCalibration.gestureFor(context, it) }
    // A press with nothing taught for it yet cannot be edited - drop back to the list rather
    // than open a picker for a gesture that does not exist.
    if (pressBeingEdited != null && editingGesture == null) editing = null
    if (pressBeingEdited != null && editingGesture != null) {
        HandlebarActionSheet(
            press = pressBeingEdited,
            current = HandlebarControlStore.action(context, editingGesture),
            onDismiss = { editing = null },
            onPicked = { action ->
                HandlebarControlStore.setAction(context, editingGesture, action)
                ProjectionEventLog.record(
                    "CONTROLS",
                    "Handlebar ${pressBeingEdited.id} (${editingGesture.id}) -> ${action.id}"
                )
                revision++
            }
        )
    }
    if (showTeachPrerequisite) {
        HandlebarTeachPrerequisiteSheet(
            motorcycles = remember { MotorcycleProfileStore(context).loadAll() },
            onConnect = { profile ->
                ProjectionEventLog.record(
                    "CONTROLS",
                    "Handlebar teach requested a connect to ${profile.ssid} to get a live session."
                )
                HandlebarTeachPrerequisiteRequest.publish(
                    HandlebarTeachPrerequisiteRequest.Choice.Connect(profile.id)
                )
                awaitingSessionForTeach = true
            },
            onPhoneOnly = {
                ProjectionEventLog.record(
                    "CONTROLS",
                    "Handlebar teach requested a phone-only Android Auto session."
                )
                HandlebarTeachPrerequisiteRequest.publish(HandlebarTeachPrerequisiteRequest.Choice.PhoneOnly)
                awaitingSessionForTeach = true
            },
            onDismiss = { showTeachPrerequisite = false }
        )
    }
    if (confirmReset) {
        MhSheet(
            onDismiss = { confirmReset = false },
            title = motoHubText("Reset all actions?"),
            body = motoHubText("Every press goes back to its default. Taught buttons stay."),
            primaryLabel = motoHubText("Reset"),
            onPrimary = {
                HandlebarControlStore.reset(context)
                revision++
                ProjectionEventLog.record("CONTROLS", "Handlebar mapping reset to defaults.")
                MotoHubSnackbar.success(context, motoHubText("Actions reset"))
            },
            secondaryLabel = motoHubText("Cancel"),
            primaryStyle = MhActionStyle.DESTRUCTIVE
        )
    }
}

@Composable
private fun HandlebarListContent(
    captureControl: ((Boolean) -> Boolean)?,
    captureDetail: (@Composable (Boolean) -> Unit)?,
    onBack: () -> Unit,
    revision: Int,
    litGesture: HandlebarGesture?,
    teachLoading: Boolean,
    onTeach: () -> Unit,
    onEdit: (PhysicalPress) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var captureEnabled by remember { mutableStateOf(HandlebarControlStore.isEnabled(context)) }
    val handlebarTaught = HandlebarCalibration.isCalibrated(context)

    MhScreen(
        title = motoHubText("Button mapping"),
        subtitle = motoHubText("Teach it once, then pick what each press does."),
        onBack = onBack
    ) {
        AnimatedVisibility(visible = !captureEnabled, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
            CaptureOffBanner(
                managedByCompanion = HandlebarControlStore.isManagedByCompanion(context),
                onEnable = captureControl?.let { control ->
                    {
                        captureEnabled = true
                        HandlebarControlStore.setEnabled(context, true)
                        control(true)
                    }
                }
            )
        }

        MhListGroup {
            if (captureControl != null) {
                MhSwitchRow(
                    title = motoHubText("Handlebar controls"),
                    subtitle = motoHubText("Use the motorcycle's buttons without looking down."),
                    checked = captureEnabled,
                    onCheckedChange = { value ->
                        captureEnabled = value
                        HandlebarControlStore.setEnabled(context, value)
                        captureControl(value)
                    }
                )
            }
            BluetoothStatusRows()
            LiveCaptureRow(litGesture)
        }
        if (captureControl != null) captureDetail?.invoke(captureEnabled)

        // The one lime control, and only until the handlebar is taught: after that the mappings
        // are what the rider came for.
        if (handlebarTaught) {
            MhSecondaryButton(motoHubText("Teach again"), onTeach, loading = teachLoading)
        } else {
            MhPrimaryButton(motoHubText("Teach my handlebar"), onTeach, loading = teachLoading)
        }

        // Before anything is taught there is nothing of this handlebar's to map: a wall of
        // "Not taught yet" rows only buried the one button that fixes it.
        if (handlebarTaught) {
            HandlebarCalibration.presentButtons(context).forEach { button ->
                ButtonSection(button = button, revision = revision, litGesture = litGesture, onEdit = onEdit)
            }
            HandlebarTimingSection()
            MhListGroup {
                MhListRow(
                    title = motoHubText("Reset actions"),
                    icon = Icons.Rounded.RestartAlt,
                    iconTint = MotoHubColors.Error,
                    titleColor = MaterialTheme.colorScheme.error,
                    showChevron = false,
                    onClick = onReset
                )
            }
        }
    }
}

/** A section header with its group, 8 dp apart as the design system spaces them. */
@Composable
private fun Section(header: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MhSectionHeader(header)
        MhListGroup(content = content)
    }
}

/**
 * The master switch, said where it is felt.
 *
 * A rider could teach every button, watch this page light up as presses arrived, and never
 * see the one switch deciding that none of them would be performed.
 */
@Composable
private fun CaptureOffBanner(managedByCompanion: Boolean, onEnable: (() -> Unit)?) {
    MhBanner(
        title = motoHubText("Button presses are off"),
        tone = MhTone.WARNING,
        body = when {
            managedByCompanion -> motoHubText("MOTO-HUB ADV-SOLO manages this. Turn them on there.")
            onEnable != null -> motoHubText("They still show up here, but do nothing.")
            // CORE: the switch lives on the page that opened this one.
            else -> motoHubText(
                "They show up here, but do nothing until “Buttons control Android Auto” is on in Handlebar buttons."
            )
        },
        actionLabel = if (onEnable != null && !managedByCompanion) motoHubText("Turn on") else null,
        onAction = onEnable
    )
}

/**
 * Asked before opening the teach wizard when no Android Auto session is running - see the
 * "Teach my handlebar" button above. Offers exactly the two ways this app can get a live
 * session: connect to a saved motorcycle's T-Box (with a choice of which, if more than one is
 * saved), or start Android Auto entirely on the phone with no T-Box at all. Neither is done
 * here directly - this screen has no session-state or permission-launcher access of its own,
 * so the choice is published for MainActivity to act on (see [HandlebarTeachPrerequisiteRequest]).
 */
@Composable
private fun HandlebarTeachPrerequisiteSheet(
    motorcycles: List<MotorcycleProfile>,
    onConnect: (MotorcycleProfile) -> Unit,
    onPhoneOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    // Source choices, like the Ride tab's connection options: no lime, no Cancel pill - swipe or
    // back cancels.
    MhSheet(
        onDismiss = onDismiss,
        title = motoHubText("Android Auto isn't running"),
        body = motoHubText("Teaching needs Android Auto running to hear the buttons.")
    ) { close ->
        Column {
            motorcycles.forEach { profile ->
                MhListRow(
                    title = motoHubText(
                        "Connect to %1\$s",
                        profile.displayName?.takeIf(String::isNotBlank) ?: profile.ssid
                    ),
                    icon = Icons.Rounded.TwoWheeler,
                    onClick = { close { onConnect(profile) } }
                )
            }
            MhListRow(
                title = motoHubText("Android Auto on this phone"),
                subtitle = motoHubText("No motorcycle needed"),
                icon = Icons.Rounded.DirectionsCar,
                onClick = { close(onPhoneOnly) }
            )
        }
    }
}

/**
 * The buttons ride Bluetooth AVRCP, not the T-Box link: a phone that never paired to the
 * motorcycle receives nothing, and no amount of mapping can fix that. This row shows what is
 * connected RIGHT NOW (queried from the live audio profiles, never the bond list) so the rider
 * knows whether to blame the pairing before blaming the mapping. It re-queries on every resume,
 * so coming back from the system Bluetooth settings shows the fresh state.
 */
@Composable
private fun BluetoothStatusRows() {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<BluetoothStatus.Status?>(null) }
    LaunchedEffect(refresh) {
        BluetoothStatus.query(context) { fresh -> status = fresh }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }
    val current = status
    // A2DP/HEADSET "connected device" only means something for an AVRCP dash - a HID remote
    // pairs as a keyboard and never shows up in that list, so current.describe() would call an
    // actually-working HID remote "nothing connected" (field report 2026-08-13). HID
    // mode has no per-device connection signal to show at all here; "ready" is the same
    // Bluetooth-on-and-permitted check that actually gates capture (see
    // BluetoothStatus.canReceiveHandlebarKeys).
    val hidMode = HandlebarControlStore.inputMode(context) == HandlebarInputMode.HID
    val connected = current?.connected == true
    val ready = if (hidMode) current?.enabled == true && current.permitted else connected
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        current?.permitted == false

    MhListRow(
        title = motoHubText("Motorcycle Bluetooth"),
        subtitle = when {
            current == null -> motoHubText("Checking Bluetooth…")
            !hidMode -> current.describe()
            !current.supported -> motoHubText("This phone has no Bluetooth.")
            !current.enabled -> motoHubText("Bluetooth is off. Turn it on, then pair the remote.")
            !current.permitted -> motoHubText("Allow Bluetooth access to read the remote.")
            // A HID remote pairs as a keyboard and never shows as connected, so on is all there is to say.
            else -> motoHubText("Bluetooth is on")
        },
        icon = Icons.Rounded.Bluetooth,
        iconTint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        iconContainer = if (ready) MaterialTheme.colorScheme.primaryContainer else MotoHubColors.Fill,
        // The row is the way to the system Bluetooth settings; nothing to open on a phone without.
        onClick = if (current?.supported == false) null else {
            { BluetoothStatus.openBluetoothSettings(context) }
        }
    )
    if (current != null && current.supported && needsPermission) {
        MhListRow(
            title = motoHubText("Allow Bluetooth"),
            icon = Icons.Rounded.Lock,
            onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) }
        )
    }
}

/** Shows the most recent press the motorcycle sent, named as the rider taught it. */
@Composable
private fun LiveCaptureRow(litGesture: HandlebarGesture?) {
    val context = LocalContext.current
    val active = litGesture != null
    val tint by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        tween(MOTION_MILLIS),
        label = "captureTint"
    )
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer else MotoHubColors.Fill,
        tween(MOTION_MILLIS),
        label = "captureContainer"
    )
    MhListRow(
        title = litGesture?.let { gesture ->
            HandlebarCalibration.pressFor(context, gesture)?.title()
                ?: motoHubText("A button not taught yet")
        } ?: motoHubText("Press a handlebar button"),
        subtitle = if (active) motoHubText("Received") else motoHubText("Listening"),
        icon = Icons.Rounded.TouchApp,
        iconTint = tint,
        iconContainer = container
    )
}

/**
 * One physical button, with the ways of pressing it that were taught. A press the motorcycle
 * never sent (skipped, or not on this handlebar) has nothing to map, so it has no row; a button
 * with none has no section.
 */
@Composable
private fun ButtonSection(
    button: HandlebarButton,
    revision: Int,
    litGesture: HandlebarGesture?,
    onEdit: (PhysicalPress) -> Unit
) {
    val context = LocalContext.current
    val taught = remember(button, revision) {
        PhysicalPress.entries
            .filter { it.button == button }
            .mapNotNull { press -> HandlebarCalibration.gestureFor(context, press)?.let { press to it } }
    }
    if (taught.isEmpty()) return
    Section(button.title()) {
        taught.forEach { (press, gesture) ->
            // A lit row fades its background in; no border - the design system has no glow.
            val highlight by animateColorAsState(
                if (gesture == litGesture) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                tween(MOTION_MILLIS),
                label = "pressHighlight"
            )
            MhListRow(
                title = press.kind.title(),
                modifier = Modifier.background(highlight),
                value = HandlebarControlStore.action(context, gesture).displayLabel(),
                onClick = { onEdit(press) }
            )
        }
    }
}

/**
 * Asks for each press in turn and records which command the motorcycle answered with.
 * "Not on my motorcycle" hides that row for good, so the screen ends up describing this
 * handlebar and no other.
 */
@Composable
private fun HandlebarCalibrationScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var learned by remember { mutableStateOf(0) }
    val lastGesture by HandlebarGestureFeed.lastGesture.collectAsState()
    val press = PhysicalPress.entries.getOrNull(step)
    // The instant this step began. A gesture older than it belongs to a previous step - or to
    // before the screen even opened, which is how a stale feed value once got recorded as
    // "up_press = trackBack" with the rider's hand nowhere near the handlebar.
    var stepStartedAt by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    // Feedback for a gesture that arrived but didn't match what this step needs - shown instead
    // of silently accepting it (see the LaunchedEffect below) or silently ignoring it.
    var mismatchHint by remember { mutableStateOf<String?>(null) }
    // Feedback for a step that is skipped automatically because it can never be satisfied (a
    // volume-mapped button has no hold gesture at all) - shown instead of silently advancing
    // past it with nothing on screen to explain why (field report 2026-08-13).
    var skipNotice by remember { mutableStateOf<String?>(null) }
    // The step whose press was recorded: its chip says so during the pause before the next one,
    // and keeps saying so while it fades out. A step number rather than a flag, so the outgoing
    // block isn't flipped back to "Listening" by the reset for the next step.
    var capturedStep by remember { mutableIntStateOf(-1) }
    val view = LocalView.current

    /** The gesture recorded for this button's PRESS step, taught earlier in the fixed
     *  press/double/hold order - tells us which family (and so which double/hold siblings, if
     *  any) this physical button belongs to. */
    fun baseGestureFor(current: PhysicalPress): HandlebarGesture? = PhysicalPress.entries
        .firstOrNull { it.button == current.button && it.kind == PressKind.PRESS }
        ?.let { HandlebarCalibration.gestureFor(context, it) }

    LaunchedEffect(step) {
        stepStartedAt = SystemClock.elapsedRealtime()
        mismatchHint = null
        skipNotice = null
        val current = press ?: return@LaunchedEffect
        if (current.kind == PressKind.PRESS) return@LaunchedEffect
        val baseGesture = baseGestureFor(current)
        val expected = if (current.kind == PressKind.DOUBLE) {
            baseGesture?.doubleSibling()
        } else {
            baseGesture?.longSibling()
        }
        if (baseGesture != null && expected == null) {
            // Known upfront, before any press arrives: this button's family has no hold gesture
            // at all (volume never does - see HandlebarGesture.longSibling). Waiting for one
            // would hang forever, so this is announced and skipped immediately instead of
            // silently advancing past it on the next unrelated press.
            skipNotice = motoHubText("This button has no hold. Skipping it.")
            ProjectionEventLog.record(
                "CONTROLS",
                "Skipped ${current.id}: ${baseGesture.id} has no hold sibling."
            )
            learned++
            delay(SKIP_NOTICE_MILLIS)
            step++
        }
    }

    // Gestures are observed, not obeyed, for as long as this screen is up.
    DisposableEffect(Unit) {
        HandlebarGestureFeed.setCaptureOnly(true)
        onDispose { HandlebarGestureFeed.setCaptureOnly(false) }
    }

    LaunchedEffect(lastGesture?.atElapsedRealtimeMillis, step) {
        val event = lastGesture ?: return@LaunchedEffect
        val current = press ?: return@LaunchedEffect
        if (event.atElapsedRealtimeMillis <= stepStartedAt) return@LaunchedEffect
        // The no-hold-exists case is announced and skipped by the step-change effect above,
        // before any press arrives - this only validates the shape of an actual incoming gesture.
        if (skipNotice != null) return@LaunchedEffect

        // The "press" step for this same physical button was always taught first (PhysicalPress
        // orders press/double/hold together per button) - its recorded gesture is this button's
        // family, and tells us which gesture id actually PROVES a double or a hold happened,
        // rather than accepting whatever fires next (field report 2026-08-13: a single tap
        // during the double/hold step silently overwrote both with the plain press).
        val baseGesture = baseGestureFor(current)
        val expected = when (current.kind) {
            PressKind.PRESS -> null
            PressKind.DOUBLE -> baseGesture?.doubleSibling()
            PressKind.HOLD -> baseGesture?.longSibling()
        }

        if (expected != null && event.gesture != expected) {
            mismatchHint = when (current.kind) {
                PressKind.DOUBLE -> motoHubText("That was one press. Press twice, quickly.")
                PressKind.HOLD -> motoHubText("That was a tap. Hold it a moment longer.")
                PressKind.PRESS -> null
            }
            // Once per gesture event (the feed delivers them already classified), so a repeated
            // wrong press buzzes again even though the hint reads the same. The rider's eyes are
            // on the handlebar.
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            return@LaunchedEffect
        }

        mismatchHint = null
        HandlebarCalibration.record(context, current, event.gesture)
        capturedStep = step
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        ProjectionEventLog.record("CONTROLS", "Calibrated ${current.id} = ${event.gesture.id}")
        learned++
        delay(CALIBRATION_CONFIRM_MILLIS)
        step++
    }

    MhScreen(
        title = null,
        onBack = onDone,
        navIcon = MhNavIcon.CLOSE,
        bottomBar = {
            if (press == null) {
                MhPrimaryButton(motoHubText("Done"), onDone)
            } else {
                // No lime while listening: the action is on the handlebar, not on the phone.
                MhSecondaryButton(
                    motoHubText("Not on my motorcycle"),
                    onClick = {
                        HandlebarCalibration.recordMissing(context, press)
                        ProjectionEventLog.record(
                            "CONTROLS",
                            "Marked ${press.id} as absent from this handlebar."
                        )
                        step++
                    }
                )
                MhTextButton(
                    motoHubText("Skip"),
                    onClick = {
                        // Logged for the same reason the automatic skip above is: a wizard run to the
                        // end teaches nothing if every step was skipped, and a support case cannot
                        // tell that from a wizard whose presses never arrived. Support 0df154af read
                        // as the second and may well have been the first.
                        ProjectionEventLog.record("CONTROLS", "Skipped ${press.id}; rider tapped Skip.")
                        step++
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) {
        val total = PhysicalPress.entries.size
        // The header and the footnote belong to the steps: they fold away as the last page slides
        // in. Coerced, because while folding they still draw once the step count has run out.
        AnimatedVisibility(visible = press != null, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
            val shown = step.coerceAtMost(total - 1)
            val progress by animateFloatAsState((shown + 1f) / total, tween(MOTION_MILLIS), label = "teachProgress")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    motoHubText("Step %1\$d of %2\$d", shown + 1, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MotoHubColors.SurfaceHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
        // The next step arrives from a little to the right while the last one fades: "moved on",
        // without the whole page sliding like a new screen.
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(MhMotion.BASE, easing = MhMotion.Enter)) { it / 5 } +
                    fadeIn(tween(MhMotion.BASE, easing = MhMotion.Enter))) togetherWith
                    fadeOut(tween(MhMotion.FAST, easing = MhMotion.Exit))
            },
            label = "teach-step"
        ) { shownStep ->
            val shownPress = PhysicalPress.entries.getOrNull(shownStep)
            if (shownPress == null) {
                TeachFinished(learned)
            } else {
                TeachStep(
                    press = shownPress,
                    captured = capturedStep == shownStep,
                    // The outgoing block lets its notes go: they were about that step.
                    mismatchHint = mismatchHint.takeIf { shownStep == step },
                    skipNotice = skipNotice.takeIf { shownStep == step }
                )
            }
        }
        AnimatedVisibility(visible = press != null, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
            // A CFMOTO CFDL16 keeps the SHORT rocker press to itself and sends the phone nothing.
            MhFootnote(
                motoHubText(
                    "Nothing happens? Some dashboards keep a button to themselves. Tap “Not on my motorcycle”."
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * One step of the wizard: which button, how to press it, and whether it has been heard. A press
 * that lands sweeps the circle to lime as the chip says "Got it".
 */
@Composable
private fun TeachStep(press: PhysicalPress, captured: Boolean, mismatchHint: String?, skipNotice: String?) {
    val sweep = tween<Color>(MhMotion.FAST, easing = MhMotion.Standard)
    val circle by animateColorAsState(if (captured) MotoHubColors.LimeContainer else MotoHubColors.Fill, sweep, label = "teach-circle")
    val glyph by animateColorAsState(
        if (captured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        sweep,
        label = "teach-glyph"
    )
    // Kept for the fade out: the hint clears the moment the right press lands.
    val lastHint = remember { mutableStateOf(mismatchHint) }
    if (mismatchHint != null) lastHint.value = mismatchHint
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MhIconCircle(press.button.icon(), size = 72.dp, tint = glyph, container = circle)
        Text(
            press.button.title(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            when (press.kind) {
                PressKind.PRESS -> motoHubText("Press once")
                PressKind.DOUBLE -> motoHubText("Press twice, quickly")
                PressKind.HOLD -> motoHubText("Press and hold")
            },
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        // One chip that changes, not two: the kit sweeps its colour and crossfades the word.
        MhStatusChip(
            if (captured) motoHubText("Got it") else motoHubText("Listening"),
            if (captured) MhTone.LIVE else MhTone.PROGRESS
        )
        // A correction, not an error, and announced: the rider's eyes are on the handlebar.
        AnimatedVisibility(
            visible = mismatchHint != null,
            enter = fadeIn(tween(MhMotion.FAST)),
            exit = fadeOut(tween(MhMotion.FAST))
        ) {
            Text(
                lastHint.value.orEmpty(),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = MotoHubColors.Warning,
                textAlign = TextAlign.Center
            )
        }
        skipNotice?.let { notice ->
            Text(
                notice,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The wizard's last page, including the run that taught nothing (every step skipped). */
@Composable
private fun TeachFinished(learned: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (learned > 0) {
            // The check pops in as the page arrives: the one overshoot the wizard has.
            var arrived by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { arrived = true }
            Box(Modifier.size(72.dp)) {
                MhPop(arrived) {
                    MhIconCircle(
                        Icons.Rounded.Check,
                        size = 72.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        container = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            Text(motoHubText("Handlebar taught"), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(
                motoHubText("%1\$d presses learned. Missing ones are hidden.", learned),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            MhIconCircle(Icons.Rounded.Info, size = 72.dp)
            Text(motoHubText("No presses learned"), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(
                motoHubText("Nothing reached the phone. Check the Bluetooth pairing and try again."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Timing, where the gestures it governs live - not on a separate screen from them. */
@Composable
private fun HandlebarTimingSection() {
    val context = LocalContext.current
    var doubleTap by remember { mutableStateOf(HandlebarTimingPrefs.doubleTap(context)) }
    var hold by remember { mutableStateOf(HandlebarTimingPrefs.selectHold(context)) }
    var eager by remember { mutableStateOf(HandlebarTimingPrefs.eagerSingles(context)) }
    var holdsOn by remember { mutableStateOf(HandlebarTimingPrefs.holdsEnabled(context)) }
    // Which value sheet is open: DOUBLE for the double press window, HOLD for the hold time.
    var sheet by remember { mutableStateOf<PressKind?>(null) }

    Section(motoHubText("Timing")) {
        MhSwitchRow(
            title = motoHubText("Snappy singles"),
            subtitle = motoHubText("A press acts at once. A double runs the single first."),
            checked = eager,
            onCheckedChange = { value ->
                eager = value
                HandlebarTimingPrefs.setEagerSingles(context, value)
                ProjectionEventLog.record("CONTROLS", "Handlebar snappy singles set to $value.")
            }
        )
        MhSwitchRow(
            title = motoHubText("Hold gestures"),
            subtitle = motoHubText("Turn off if every click needs a long press."),
            checked = holdsOn,
            onCheckedChange = { value ->
                holdsOn = value
                HandlebarTimingPrefs.setHoldsEnabled(context, value)
                ProjectionEventLog.record("CONTROLS", "Handlebar hold gestures set to $value.")
            }
        )
        MhListRow(
            title = motoHubText("Double press window"),
            value = motoHubText("%1\$d ms", doubleTap.millis),
            onClick = { sheet = PressKind.DOUBLE }
        )
        MhListRow(
            title = motoHubText("Hold time"),
            value = motoHubText("%1\$d ms", hold.millis),
            onClick = { sheet = PressKind.HOLD }
        )
    }

    // DoubleTapDelay.label / SelectHoldDelay.label stay in the enums untouched; the UI words
    // its own subtitles here.
    when (sheet) {
        PressKind.DOUBLE -> MhSheet(
            onDismiss = { sheet = null },
            title = motoHubText("Double press window"),
            body = motoHubText("How long to wait for a second press.")
        ) { close ->
            Column {
                DoubleTapDelay.entries.forEach { option ->
                    MhChoiceRow(
                        title = motoHubText("%1\$d ms", option.millis),
                        subtitle = when (option) {
                            DoubleTapDelay.FAST -> motoHubText("Snappier singles")
                            DoubleTapDelay.NORMAL -> motoHubText("Balanced")
                            DoubleTapDelay.SLOW -> motoHubText("Forgiving doubles")
                        },
                        selected = option == doubleTap,
                        onClick = {
                            close {
                                doubleTap = option
                                HandlebarTimingPrefs.setDoubleTap(context, option)
                            }
                        }
                    )
                }
            }
        }
        PressKind.HOLD -> MhSheet(
            onDismiss = { sheet = null },
            title = motoHubText("Hold time"),
            body = motoHubText("How long a press must last to count as a hold.")
        ) { close ->
            Column {
                SelectHoldDelay.entries.forEach { option ->
                    MhChoiceRow(
                        title = motoHubText("%1\$d ms", option.millis),
                        subtitle = when (option) {
                            SelectHoldDelay.SHORT -> motoHubText("Quicker hold")
                            SelectHoldDelay.NORMAL -> motoHubText("Balanced")
                            SelectHoldDelay.LONG -> motoHubText("Deliberate hold")
                        },
                        selected = option == hold,
                        onClick = {
                            close {
                                hold = option
                                HandlebarTimingPrefs.setSelectHold(context, option)
                            }
                        }
                    )
                }
            }
        }
        else -> Unit
    }
}

/** Every action, grouped, on one sheet: a tap picks it and closes the sheet. */
@Composable
private fun HandlebarActionSheet(
    press: PhysicalPress,
    current: HandlebarAction,
    onPicked: (HandlebarAction) -> Unit,
    onDismiss: () -> Unit
) {
    MhSheet(onDismiss = onDismiss, title = press.title()) { close ->
        ActionFamily.entries.forEach { family ->
            // CORE never registers the dashboard sink (see HandlebarActionRunner), so these are
            // dropped on every press there. The group still shows when it holds the current
            // action, so a press mapped to one can be moved off it.
            if (family == ActionFamily.DASHBOARD && !BuildConfig.IS_PRO && current.family() != family) {
                return@forEach
            }
            // Rows sit straight on the sheet; the inner padding lines headers up with the title.
            Column {
                family.title()?.let { MhSectionHeader(it, Modifier.padding(start = 12.dp)) }
                HandlebarAction.entries.filter { it.family() == family }.forEach { candidate ->
                    MhChoiceRow(
                        title = candidate.displayLabel(),
                        selected = candidate == current,
                        onClick = { close { onPicked(candidate) } }
                    )
                }
                if (family == ActionFamily.DASHBOARD) {
                    // Chosen from here it looks like any other action, and the rider finds out it
                    // is not one only by reading a log line. Field case, 2026-08-29: every front
                    // button of an Xbox pad mapped to these, in an Android Auto session, every
                    // press resolved and then dropped.
                    MhFootnote(
                        motoHubText("Only works while the Ride Dashboard is running."),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

// The enums' own labels stay raw English: logs, the HUD on the dashboard and the companion app
// read them. What the phone shows is worded here, one literal per value, so it can be translated.

private fun HandlebarButton.title(): String = when (this) {
    HandlebarButton.UP -> motoHubText("Up")
    HandlebarButton.DOWN -> motoHubText("Down")
    HandlebarButton.LEFT -> motoHubText("Left")
    HandlebarButton.RIGHT -> motoHubText("Right")
    HandlebarButton.SELECT -> motoHubText("Select / OK")
}

private fun PressKind.title(): String = when (this) {
    PressKind.PRESS -> motoHubText("Press")
    PressKind.DOUBLE -> motoHubText("Double press")
    PressKind.HOLD -> motoHubText("Hold")
}

/** "Up · Double press": both halves translated, never glued from fragments like [PhysicalPress.label]. */
private fun PhysicalPress.title(): String = motoHubText("%1\$s · %2\$s", button.title(), kind.title())

// The physical button, so not the auto-mirrored arrows: left stays left in a right-to-left locale.
@Suppress("DEPRECATION")
private fun HandlebarButton.icon(): ImageVector = when (this) {
    HandlebarButton.UP -> Icons.Rounded.KeyboardArrowUp
    HandlebarButton.DOWN -> Icons.Rounded.KeyboardArrowDown
    HandlebarButton.LEFT -> Icons.Rounded.KeyboardArrowLeft
    HandlebarButton.RIGHT -> Icons.Rounded.KeyboardArrowRight
    HandlebarButton.SELECT -> Icons.Rounded.RadioButtonChecked
}

private fun HandlebarAction.displayLabel(): String = when (this) {
    HandlebarAction.NONE -> motoHubText("Do nothing")
    HandlebarAction.SCROLL_FORWARD -> motoHubText("Scroll forward")
    HandlebarAction.SCROLL_BACK -> motoHubText("Scroll back")
    HandlebarAction.DPAD_UP -> motoHubText("Cursor up")
    HandlebarAction.DPAD_DOWN -> motoHubText("Cursor down")
    HandlebarAction.DPAD_LEFT -> motoHubText("Cursor left")
    HandlebarAction.DPAD_RIGHT -> motoHubText("Cursor right")
    HandlebarAction.SELECT -> motoHubText("Select / OK")
    HandlebarAction.BACK -> motoHubText("Back")
    HandlebarAction.HOME -> motoHubText("Home")
    HandlebarAction.ASSISTANT -> motoHubText("Voice assistant")
    HandlebarAction.NAV_1 -> motoHubText("Saved place %1\$d", 1)
    HandlebarAction.NAV_2 -> motoHubText("Saved place %1\$d", 2)
    HandlebarAction.NAV_3 -> motoHubText("Saved place %1\$d", 3)
    HandlebarAction.DASH_NEXT_PANEL -> motoHubText("Next panel")
    HandlebarAction.DASH_FULLSCREEN_MAP -> motoHubText("Full-screen map")
    HandlebarAction.DASH_MAP_ZOOM -> motoHubText("Center map on me")
    HandlebarAction.DASH_WIDGET_LEFT -> motoHubText("Change left widget")
    HandlebarAction.DASH_WIDGET_RIGHT -> motoHubText("Change right widget")
    HandlebarAction.MEDIA_PLAY_PAUSE -> motoHubText("Play / pause")
    HandlebarAction.MEDIA_NEXT -> motoHubText("Next track")
    HandlebarAction.MEDIA_PREVIOUS -> motoHubText("Previous track")
    HandlebarAction.MEDIA_VOLUME_UP -> motoHubText("Volume up")
    HandlebarAction.MEDIA_VOLUME_DOWN -> motoHubText("Volume down")
}

/** Action families, in the order the picker lists them. */
private enum class ActionFamily { CURSOR, SYSTEM, MUSIC, NAVIGATION, DASHBOARD, OFF }

/** Null for "Do nothing", which needs no header. */
private fun ActionFamily.title(): String? = when (this) {
    ActionFamily.CURSOR -> motoHubText("Move the cursor")
    ActionFamily.SYSTEM -> motoHubText("Android Auto")
    ActionFamily.MUSIC -> motoHubText("Music")
    ActionFamily.NAVIGATION -> motoHubText("Navigate to")
    ActionFamily.DASHBOARD -> motoHubText("Ride Dashboard")
    ActionFamily.OFF -> null
}

private fun HandlebarAction.family(): ActionFamily = when (this) {
    HandlebarAction.NONE -> ActionFamily.OFF
    HandlebarAction.SCROLL_FORWARD, HandlebarAction.SCROLL_BACK,
    HandlebarAction.DPAD_UP, HandlebarAction.DPAD_DOWN,
    HandlebarAction.DPAD_LEFT, HandlebarAction.DPAD_RIGHT,
    HandlebarAction.SELECT -> ActionFamily.CURSOR
    HandlebarAction.BACK, HandlebarAction.HOME, HandlebarAction.ASSISTANT -> ActionFamily.SYSTEM
    HandlebarAction.NAV_1, HandlebarAction.NAV_2, HandlebarAction.NAV_3 -> ActionFamily.NAVIGATION
    HandlebarAction.DASH_NEXT_PANEL, HandlebarAction.DASH_FULLSCREEN_MAP,
    HandlebarAction.DASH_MAP_ZOOM,
    HandlebarAction.DASH_WIDGET_LEFT, HandlebarAction.DASH_WIDGET_RIGHT -> ActionFamily.DASHBOARD
    HandlebarAction.MEDIA_PLAY_PAUSE, HandlebarAction.MEDIA_NEXT, HandlebarAction.MEDIA_PREVIOUS,
    HandlebarAction.MEDIA_VOLUME_UP, HandlebarAction.MEDIA_VOLUME_DOWN -> ActionFamily.MUSIC
}

private const val HIGHLIGHT_MILLIS = 1_400L
private const val CALIBRATION_CONFIRM_MILLIS = 700L
/** Longer than CALIBRATION_CONFIRM_MILLIS - this is an unrequested notice about a step the
 *  rider never got a chance to act on, not a confirmation of something they just did. */
private const val SKIP_NOTICE_MILLIS = 1800L
