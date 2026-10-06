// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.pairing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.theme.MotoHubColors

/** Where the rider keeps the picture of the QR code. */
enum class QrImageSource { GALLERY, FILES }

/**
 * The photo picker alone reaches only what the gallery indexes. A QR code arrives just as
 * often as a screenshot in Downloads, a PNG saved out of a chat, or a file on a cloud drive -
 * none of which the gallery lists, so a rider holding one had no way in at all and no hint that
 * the picker was the reason. These are the same two doors the motorcycle photo already offers,
 * worded the same way; the camera is missing on purpose, because live scanning is its own action
 * next to this one.
 *
 * One sheet from the pick to the answer. Choosing a source does not close it - the system picker
 * opens on top, and backing out of the picker leaves the sheet as it was. Reading and a failed
 * read replace its content in place, so the failure shows where the rider is looking, with the
 * same two doors right under it for the next try.
 *
 * [failure] is the decoder's verdict, empty when it had nothing useful to say; [decoded] is a
 * code that was read, handed to [onDecoded] once the sheet has slid away.
 */
@Composable
fun QrImportSheet(
    reading: Boolean,
    progress: Float,
    failure: String?,
    decoded: TBoxQrPayload?,
    onSelect: (QrImageSource) -> Unit,
    onDecoded: (TBoxQrPayload) -> Unit,
    onDismiss: () -> Unit
) {
    val busy = reading || decoded != null
    MhSheet(
        onDismiss = onDismiss,
        title = when {
            busy -> motoHubText("Reading the code…")
            failure != null -> motoHubText("Couldn't read the QR code")
            else -> motoHubText("Import QR code")
        },
        body = failure?.takeIf { !busy }?.ifEmpty { motoHubText("Try a sharper photo with the whole code in frame.") },
        // The decoder finishes whatever the rider does, so a read cannot be walked away from halfway.
        dismissible = !busy
    ) { close ->
        if (busy) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MotoHubColors.Fill,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        } else {
            MhListRow(
                title = motoHubText("Choose from gallery"),
                icon = Icons.Rounded.PhotoLibrary,
                showChevron = false,
                onClick = { onSelect(QrImageSource.GALLERY) }
            )
            MhListRow(
                title = motoHubText("Browse files"),
                subtitle = motoHubText("Downloads, cloud drives and other folders"),
                icon = Icons.Rounded.FolderOpen,
                showChevron = false,
                onClick = { onSelect(QrImageSource.FILES) }
            )
        }
        // Away first, then the answer: the snackbar, the unverified-code dialog or the manual
        // form must not open under a sheet that is still on screen.
        if (decoded != null) LaunchedEffect(decoded) { close { onDecoded(decoded) } }
    }
}
