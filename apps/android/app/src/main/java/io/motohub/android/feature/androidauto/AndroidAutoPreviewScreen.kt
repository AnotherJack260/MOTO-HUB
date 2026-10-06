// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.androidauto

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.motohub.android.androidauto.AndroidAutoRuntime
import io.motohub.android.androidauto.AndroidAutoRuntimeState
import io.motohub.android.androidauto.AndroidAutoPreviewView
import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MOTION_MILLIS
import io.motohub.android.ui.components.MhIconButton
import io.motohub.android.ui.components.MhNavIcon
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhTone
import io.motohub.android.ui.components.MhTopBar
import io.motohub.android.ui.components.MhTopBarAction

@Composable
fun AndroidAutoPreviewScreen(onBack: () -> Unit, startFullscreen: Boolean = false) {
    val view = LocalView.current
    val runtimeState by AndroidAutoRuntime.state.collectAsStateWithLifecycle()
    var fullscreen by rememberSaveable(startFullscreen) { mutableStateOf(startFullscreen) }
    val window = (view.context as? ComponentActivity)?.window
    val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }

    DisposableEffect(view, insetsController) {
        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(fullscreen, insetsController) {
        if (fullscreen) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }
    BackHandler(enabled = !fullscreen, onBack = onBack)

    val streaming = runtimeState is AndroidAutoRuntimeState.Streaming
    val sessionActive = runtimeState is AndroidAutoRuntimeState.Preparing ||
        runtimeState is AndroidAutoRuntimeState.ReceiverReady || streaming
    val startupDetail by AndroidAutoRuntime.startupDetail.collectAsStateWithLifecycle()
    // The startup detail is usually narration, but two of its values are an instruction the
    // rider has to carry out. Those two are stored in English - the flat line is IPC payload
    // matched by identity - so they are recognised here and drawn from the catalogue instead of
    // being passed through raw, which left them English on a phone set to any other language.
    val riderStep = AndroidAutoSelfModeHelp.riderStepOf(startupDetail)
    val riderStepLine = riderStep?.let { "${motoHubText(it.action)} · ${motoHubText(it.where)}" }
    // motoHubText on the runtime branches too: the stop reason and the failure message reach
    // this screen as plain strings, so the catalogue is the only place they can be translated,
    // and one with no entry falls back to itself.
    val status = when (val state = runtimeState) {
        AndroidAutoRuntimeState.Idle ->
            motoHubText("Android Auto isn't running. Start it from the Ride tab.")
        AndroidAutoRuntimeState.Preparing -> motoHubText("Preparing Android Auto…")
        // Not "connected": at this point MOTO-HUB is only listening, and is still asking Google
        // Android Auto to project here — which can take several seconds and several attempts.
        AndroidAutoRuntimeState.ReceiverReady ->
            riderStepLine ?: startupDetail?.let(::motoHubText)
                ?: motoHubText("Waiting for Android Auto to start projecting…")
        AndroidAutoRuntimeState.Streaming -> motoHubText("Live preview · touch enabled")
        is AndroidAutoRuntimeState.Stopped -> motoHubText(state.reason)
        is AndroidAutoRuntimeState.Failed -> motoHubText(state.message)
    }

    val preview: @Composable () -> Unit = {
        AndroidView(
            factory = ::AndroidAutoPreviewView,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }

    // preview() binds one SurfaceView to a shared runtime registration, not one per view. A
    // slide transition would keep both the windowed and fullscreen layouts composed together
    // for the length of the animation, and two of these would each try to bind their own
    // Surface to that single registration - see the PRO edition of this screen for the same
    // reasoning. So the view is mounted exactly once here; only the top bar slides.
    val headerHeightPx = remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val topInset by animateDpAsState(
        targetValue = if (fullscreen) 0.dp else with(density) { headerHeightPx.floatValue.toDp() },
        animationSpec = tween(MOTION_MILLIS, easing = FastOutSlowInEasing),
        label = "aa-preview-top-inset"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset)
        ) {
            preview()
            if (!sessionActive) {
                PreviewStatusCard(status, Modifier.align(Alignment.Center))
            }
            if (fullscreen) {
                // The one control over the stream: a way back out, on a dark disc so it reads on
                // whatever Android Auto is drawing underneath.
                MhIconButton(
                    Icons.Rounded.FullscreenExit,
                    contentDescription = motoHubText("Exit fullscreen"),
                    onClick = { fullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    tint = Color.White
                )
            }
        }
        AnimatedVisibility(
            visible = !fullscreen,
            enter = fadeIn(tween(MOTION_MILLIS)) + slideInVertically(tween(MOTION_MILLIS)) { -it },
            exit = fadeOut(tween(MOTION_MILLIS)) + slideOutVertically(tween(MOTION_MILLIS)) { -it }
        ) {
            // Measured whole, status bar included, so the preview starts right under it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { headerHeightPx.floatValue = it.size.height.toFloat() }
                    .padding(bottom = 12.dp)
            ) {
                MhTopBar(
                    onBack = onBack,
                    navIcon = MhNavIcon.CLOSE,
                    title = motoHubText("Android Auto"),
                    actions = {
                        PreviewStatusChip(runtimeState)
                        Spacer(Modifier.width(8.dp))
                        MhTopBarAction(Icons.Rounded.Fullscreen, motoHubText("Fullscreen"), onClick = { fullscreen = true })
                    }
                )
                // Its own full-width line under the bar rather than squeezed beside the chip: an
                // Android Auto failure is seven lines of instructions. Nothing caps it and
                // nothing sizes it: the bar grows and the preview below is inset by however tall
                // it ends up.
                Text(
                    text = if (streaming) motoHubText("Touch the preview to control Android Auto") else status,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PreviewStatusChip(state: AndroidAutoRuntimeState) {
    val (label, tone) = when (state) {
        AndroidAutoRuntimeState.Streaming -> motoHubText("Live") to MhTone.LIVE
        AndroidAutoRuntimeState.Preparing,
        AndroidAutoRuntimeState.ReceiverReady -> motoHubText("Starting") to MhTone.PROGRESS
        AndroidAutoRuntimeState.Idle,
        is AndroidAutoRuntimeState.Stopped -> motoHubText("Stopped") to MhTone.NEUTRAL
        is AndroidAutoRuntimeState.Failed -> motoHubText("Failed") to MhTone.ERROR
    }
    MhStatusChip(label, tone)
}

/**
 * The whole message, over the dead preview.
 *
 * Scrollable rather than capped: an Android Auto failure is a paragraph of instructions, and it
 * is longer than the space between the header and the bottom of a phone in landscape. Truncating
 * it would hide the one step that fixes the ride, so it scrolls instead.
 */
@Composable
private fun PreviewStatusCard(status: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = status,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}
