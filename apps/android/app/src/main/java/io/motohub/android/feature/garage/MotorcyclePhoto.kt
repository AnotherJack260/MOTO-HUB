// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.theme.MotoHubColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MotorcyclePhoto(
    path: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(path) {
        bitmap = path?.let { filePath ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(filePath) }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MotoHubColors.SurfaceHighest),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image == null) {
            // Neutral, and scaled to the frame: the same glyph reads at 48 dp and at 200 dp.
            Icon(
                Icons.Rounded.TwoWheeler,
                contentDescription = null,
                tint = MotoHubColors.TextTertiary,
                modifier = Modifier.fillMaxSize(0.4f)
            )
        } else {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = motoHubText("Motorcycle photo"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
