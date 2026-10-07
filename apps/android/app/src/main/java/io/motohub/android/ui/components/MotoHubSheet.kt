// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.motohub.android.ui.theme.MotoHubColors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * How many kit sheets and dialogs are on screen right now. Startup prompts wait for zero, so they
 * never land on top of a sheet the rider has just opened. Only the kit writes it.
 */
object MhModals {
    var open by mutableIntStateOf(0)
        internal set
}

/** Counts the calling sheet or dialog in [MhModals] for as long as it is composed. */
@Composable
internal fun CountAsModal() {
    DisposableEffect(Unit) {
        MhModals.open++
        onDispose { MhModals.open-- }
    }
}

/**
 * True inside an [MhSheet]'s content. A row on a sheet acts - picks, imports, removes - and never
 * navigates, so [MhListRow] leaves out its chevron there unless a caller asks for it.
 */
val LocalMhInSheet = staticCompositionLocalOf { false }

/**
 * A decision that does not need the whole screen: grabber, title, a line or two, then the actions
 * stacked as pills (primary on top). [content] goes between the body and the actions for the
 * sheets that carry a list or a field. It is inset 4 dp, so an MhListRow or MhChoiceRow (16 dp of
 * its own) lines up with the 20 dp title; anything else in the slot pads itself 16 dp. Rows in it
 * have no chevron by default ([LocalMhInSheet]).
 *
 * [onDismiss] means "the sheet is gone", whichever way it went: scrim, back, swipe, or any button.
 * It runs exactly once and only clears the caller's `showX` flag - a flag that is never cleared
 * leaves an invisible window that swallows every touch. Cancel's own logic goes in [onSecondary].
 *
 * Actions run after the sheet has slid away and after [onDismiss], so the screen underneath never
 * changes while the sheet still covers it. That order is why an action must capture what it needs
 * in a local `val` first and never read state its [onDismiss] clears:
 * ```
 * val bike = editing ?: return
 * MhSheet(onDismiss = { editing = null }, onPrimary = { remove(bike) }, ...)
 * ```
 * Actions run once: a double tap with gloves would otherwise start two slides and run them twice.
 * Set [dismissible] to false only for something the rider must answer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MhSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    body: String? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryStyle: MhActionStyle = MhActionStyle.LIME,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.(close: (after: () -> Unit) -> Unit) -> Unit)? = null
) {
    CountAsModal()
    // Declared before the sheet state, whose veto reads it: once a button has started closing a
    // non-dismissible sheet, Hidden has to be allowed or the sheet slides off screen while still
    // counting itself as open. Both are read through State so the veto lambda never changes -
    // the sheet state is keyed on it, and a new lambda would rebuild the sheet mid-flight.
    val closing = remember { mutableStateOf(false) }
    val canDismiss by rememberUpdatedState(dismissible)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { canDismiss || closing.value || it != SheetValue.Hidden }
    )
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after ->
        if (!closing.value) {
            closing.value = true
            // One coroutine, in order: if the sheet is torn down mid-slide, nothing after runs.
            scope.launch {
                sheetState.hide()
                onDismiss()
                after()
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MotoHubColors.SurfaceHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
        ),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MotoHubColors.Fill, CircleShape)
            )
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val inset = Modifier.padding(horizontal = 16.dp)
            if (title != null) {
                Text(title, modifier = inset.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
            }
            if (body != null) {
                Text(
                    body,
                    modifier = inset,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (content != null) {
                CompositionLocalProvider(LocalMhInSheet provides true) { content(this, close) }
            }
            if (primaryLabel != null || secondaryLabel != null) {
                Column(inset.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (primaryLabel != null && onPrimary != null) {
                        MhActionButton(primaryLabel, primaryStyle) { close(onPrimary) }
                    }
                    if (secondaryLabel != null) {
                        // Not `?: onDismiss`: close() already calls it.
                        MhSecondaryButton(secondaryLabel, { close(onSecondary ?: {}) })
                    }
                }
            }
        }
    }
}

enum class MhSnackTone { SUCCESS, INFO, ERROR }

/**
 * The app's one way to say "done" or "that didn't work" without stopping the rider: a short line
 * that floats above the tab bar and goes away on its own. Callable from anywhere, including
 * callbacks with no composition in reach. While no host is on screen - the app is in the
 * background - it falls back to a system toast so the message is not lost.
 */
object MotoHubSnackbar {
    data class Message(
        val text: String,
        val tone: MhSnackTone,
        val actionLabel: String?,
        val onAction: (() -> Unit)?
    )

    private val messages = MutableSharedFlow<Message>(extraBufferCapacity = 8)

    fun show(
        context: Context,
        text: String,
        tone: MhSnackTone = MhSnackTone.INFO,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        if (messages.subscriptionCount.value > 0) {
            messages.tryEmit(Message(text, tone, actionLabel, onAction))
        } else {
            // show() is called from callbacks on any thread; a Toast made off the main looper
            // crashes or silently never appears.
            val app = context.applicationContext
            Handler(Looper.getMainLooper()).post { Toast.makeText(app, text, Toast.LENGTH_SHORT).show() }
        }
    }

    fun error(context: Context, text: String) = show(context, text, MhSnackTone.ERROR)
    fun success(context: Context, text: String) = show(context, text, MhSnackTone.SUCCESS)

    @Composable
    fun Host(modifier: Modifier = Modifier) {
        val hostState = remember { SnackbarHostState() }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Newest wins: a new message cancels the one being shown, which takes it off
                // screen, instead of queueing behind it on the host's mutex - so nothing on screen
                // is ever older than the last tap, however fast they come.
                messages.collectLatest { message ->
                    val result = hostState.showSnackbar(SnackbarVisuals(message))
                    if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
                }
            }
        }
        SnackbarHost(hostState, modifier.imePadding()) { data -> Snack(data) }
    }

    private class SnackbarVisuals(val payload: Message) : androidx.compose.material3.SnackbarVisuals {
        override val message: String get() = payload.text
        override val actionLabel: String? get() = payload.actionLabel
        override val withDismissAction: Boolean get() = false
        override val duration: SnackbarDuration
            get() = if (payload.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
    }

    @Composable
    private fun Snack(data: SnackbarData) {
        val tone = (data.visuals as? SnackbarVisuals)?.payload?.tone ?: MhSnackTone.INFO
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MotoHubColors.SurfaceHighest)
                // A tap on the message dismisses it rather than reaching the button underneath.
                // No liveRegion here: SnackbarHost already sets one, and two make TalkBack say it twice.
                .clickable(onClick = data::dismiss)
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (tone) {
                    MhSnackTone.SUCCESS -> Icons.Rounded.CheckCircle
                    MhSnackTone.ERROR -> Icons.Rounded.ErrorOutline
                    MhSnackTone.INFO -> Icons.Rounded.Info
                },
                contentDescription = null,
                tint = when (tone) {
                    MhSnackTone.SUCCESS -> MotoHubColors.Lime
                    MhSnackTone.ERROR -> MotoHubColors.Error
                    MhSnackTone.INFO -> MotoHubColors.TextSecondary
                },
                modifier = Modifier.size(22.dp)
            )
            Text(
                data.visuals.message,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            data.visuals.actionLabel?.let { label ->
                // onSurface, not lime: the snackbar floats over a screen that has its own lime action.
                MhTextButton(label, onClick = { data.performAction() })
            }
        }
    }
}
