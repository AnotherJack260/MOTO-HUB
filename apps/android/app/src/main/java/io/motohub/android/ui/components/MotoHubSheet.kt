// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.motohub.android.ui.theme.MotoHubColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * A decision that does not need the whole screen: grabber, title, a line or two, then the actions
 * stacked as pills (primary on top). [content] goes between the body and the actions for the
 * sheets that carry a list or a field.
 *
 * Actions run after the sheet has slid away, so the screen underneath never changes while the
 * sheet is still covering it. Set [dismissible] to false only for something the rider must answer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MhSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    body: String? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    destructivePrimary: Boolean = false,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.(close: (after: () -> Unit) -> Unit) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { dismissible || it != SheetValue.Hidden }
    )
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after -> scope.hideThen(sheetState, after) }
    ModalBottomSheet(
        onDismissRequest = { if (dismissible) onDismiss() },
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
                    .background(MotoHubColors.SurfaceHighest, CircleShape)
            )
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = dismissible)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
            }
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content?.invoke(this, close)
            if (primaryLabel != null || secondaryLabel != null) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (primaryLabel != null && onPrimary != null) {
                        if (destructivePrimary) {
                            MhSecondaryButton(primaryLabel, { close(onPrimary) }, destructive = true)
                        } else {
                            MhPrimaryButton(primaryLabel, { close(onPrimary) })
                        }
                    }
                    if (secondaryLabel != null) {
                        MhSecondaryButton(secondaryLabel, { close(onSecondary ?: onDismiss) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun CoroutineScope.hideThen(state: SheetState, after: () -> Unit) {
    launch { state.hide() }.invokeOnCompletion { after() }
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
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT).show()
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
                messages.collect { message ->
                    // Newest wins: a second message replaces the first instead of queueing
                    // behind it, so nothing on screen is ever older than the last tap.
                    hostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = hostState.showSnackbar(
                            SnackbarVisuals(message),
                        )
                        if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
                    }
                }
            }
        }
        SnackbarHost(hostState, modifier) { data -> Snack(data) }
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
                .semantics { liveRegion = LiveRegionMode.Polite }
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
                MhTextButton(label, onClick = { data.performAction() }, color = MotoHubColors.Lime)
            }
        }
    }
}
