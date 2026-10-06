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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.motohub.android.ui.theme.MotoHubColors

/**
 * One of the few things allowed to stop the rider - crash-data consent, the unverified QR code.
 * Everything else is a sheet or a snackbar. The API mirrors [MhSheet], and so does the contract:
 * [onDismiss] runs once, whichever way the dialog went, and only clears the caller's flag; the
 * actions run after it, so they capture what they need in a local `val` first.
 *
 * The actions are the sheet's stacked full-width pills, not Material's 40 dp text buttons, so
 * they are glove-sized. Back and tapping outside do nothing unless [dismissible]: these are
 * questions with an answer, and a stray tap must not count as one. For consent and trust answers
 * use `primaryStyle = NEUTRAL` - two equal pills, no lime nudge.
 */
@Composable
fun MhDialog(
    onDismiss: () -> Unit,
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryStyle: MhActionStyle = MhActionStyle.LIME,
    dismissible: Boolean = false,
    content: (@Composable ColumnScope.(close: (after: () -> Unit) -> Unit) -> Unit)? = null
) {
    CountAsModal()
    // A dialog has no slide to wait for, but a double tap still lands twice before the caller's
    // flag change has removed it.
    val closing = remember { mutableStateOf(false) }
    val close: (() -> Unit) -> Unit = { after ->
        if (!closing.value) {
            closing.value = true
            onDismiss()
            after()
        }
    }
    AlertDialog(
        onDismissRequest = { if (dismissible) close {} },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MhActionButton(primaryLabel, primaryStyle) { close(onPrimary) }
                if (secondaryLabel != null) {
                    MhSecondaryButton(secondaryLabel, { close(onSecondary ?: {}) })
                }
            }
        },
        modifier = modifier,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title) },
        text = if (body == null && content == null) null else {
            {
                MotoHubDialogBody {
                    if (body != null) Text(body, style = MaterialTheme.typography.bodyLarge)
                    content?.invoke(this, close)
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MotoHubColors.SurfaceHigh,
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
