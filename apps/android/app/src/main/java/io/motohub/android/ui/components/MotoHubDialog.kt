// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * One of the three things allowed to stop the rider: the safety acknowledgement, crash-data
 * consent, the unverified-QR warning. Everything else is a sheet or a snackbar.
 *
 * Back and tapping outside do nothing unless [dismissible]: these are questions with an answer,
 * and a stray tap must not count as one. The buttons are the kit's, not Material's 40 dp text
 * buttons, so they are glove-sized; when they do not fit side by side the confirm goes on top.
 */
@Composable
fun MhDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    dismissible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        confirmButton = { MhPrimaryButton(confirmLabel, onConfirm) },
        dismissButton = dismissLabel?.let { label ->
            { MhTextButton(label, onClick = { onDismiss?.invoke() }, modifier = Modifier.fillMaxWidth()) }
        },
        title = { Text(title) },
        text = { MotoHubDialogBody(content = content) },
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible)
    )
}

/**
 * Body slot for every MOTO-HUB [androidx.compose.material3.AlertDialog].
 *
 * Material 3 gives the text slot whatever height is left over after the title and the buttons, but
 * it never scrolls it: the overflow is simply clipped and unreachable. A narrow phone wraps the
 * same paragraphs onto more lines, and a large system font size makes each line taller, so a
 * dialog that fits on one device silently loses its last sentences on another. Riders were left
 * unable to read a safety warning to the end.
 *
 * Wrapping the body here keeps it scrollable, and fades the bottom edge while there is more to
 * read so the rider can see that scrolling is possible.
 */
@Composable
fun MotoHubDialogBody(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    val fadeColor = AlertDialogDefaults.containerColor
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
        if (scroll.canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FADE_HEIGHT)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, fadeColor)))
            )
        }
    }
}

private val FADE_HEIGHT = 28.dp
