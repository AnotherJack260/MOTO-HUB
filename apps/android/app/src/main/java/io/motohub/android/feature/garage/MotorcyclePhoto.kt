// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.theme.MotoHubColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A motorcycle's photo, or - when it has none - a `Fill` frame with an "add a photo" glyph, plus
 * the words "Add photo" once the frame is 120 dp tall or more. A frame whose photo is still
 * decoding stays plain, so it never flashes "Add photo" on its way to the picture.
 */
@Composable
fun MotorcyclePhoto(
    path: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var decoded by remember(path) { mutableStateOf(path == null) }

    LaunchedEffect(path) {
        bitmap = path?.let { filePath ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(filePath) }
        }
        decoded = true
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(MotoHubColors.Fill),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = motoHubText("Motorcycle photo"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (decoded) {
            val labelled = maxHeight >= 120.dp
            // An icon circle's proportion in a row avatar; a fixed glyph above the label.
            val glyph = if (labelled) 32.dp else min(maxWidth, maxHeight) * 0.5f
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Rounded.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(glyph)
                )
                if (labelled) {
                    Text(
                        motoHubText("Add photo"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
