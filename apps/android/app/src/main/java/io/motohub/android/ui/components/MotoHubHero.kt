// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import io.motohub.android.i18n.motoHubText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's icon vocabulary, by name, so every screen asking for "Bike" or "QrScan" gets the same
 * glyph. Material Symbols Rounded - the hand-drawn Canvas set they replace could not be read at a
 * glance next to each other.
 */
fun modeIconVector(mode: String): ImageVector = when (mode) {
    "OSM", "MapLibre" -> Icons.Rounded.Map
    "Mirror" -> Icons.AutoMirrored.Rounded.ScreenShare
    "Dashboard" -> Icons.Rounded.Speed
    "Auto" -> Icons.Rounded.DirectionsCar
    "External" -> Icons.Rounded.Usb
    "Preview" -> Icons.Rounded.Visibility
    "Controls" -> Icons.Rounded.SportsEsports
    "Customize" -> Icons.Rounded.Tune
    "Route" -> Icons.Rounded.Route
    "Gps" -> Icons.Rounded.MyLocation
    "Clear" -> Icons.Rounded.Close
    "QrScan" -> Icons.Rounded.QrCodeScanner
    "Import" -> Icons.Rounded.Image
    "Manual" -> Icons.Rounded.Keyboard
    "Search" -> Icons.Rounded.Search
    "Star" -> Icons.Rounded.Star
    "Clock" -> Icons.Rounded.Schedule
    "Voice" -> Icons.Rounded.Mic
    "Bike" -> Icons.Rounded.TwoWheeler
    else -> Icons.Rounded.Circle
}

@Composable
fun ModeIcon(mode: String, color: Color, iconSize: Dp = 24.dp) {
    Icon(modeIconVector(mode), contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
}

/** A screen's single most important action. Drawn as the primary button; [subtitle] and [color] are
 *  kept for callers written against the old lime card and are no longer shown. */
@Deprecated(
    "Use MhPrimaryButton.",
    ReplaceWith("MhPrimaryButton(motoHubText(title), onClick, icon = modeIconVector(icon))", "io.motohub.android.i18n.motoHubText")
)
@Composable
fun HeroPrimaryAction(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    MhPrimaryButton(motoHubText(title), onClick, icon = modeIconVector(icon))
}

/** A secondary target with a little room to explain itself: neutral icon, title, one line. */
@Deprecated(
    "Use MhListRow inside an MhListGroup.",
    ReplaceWith(
        "MhListRow(title = motoHubText(title), modifier = modifier, subtitle = motoHubText(subtitle), icon = modeIconVector(icon), onClick = onClick)",
        "io.motohub.android.i18n.motoHubText"
    )
)
@Composable
fun HeroTile(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MhIconCircle(modeIconVector(icon))
        Text(motoHubText(title), style = MaterialTheme.typography.titleMedium)
        Text(
            motoHubText(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One option in a grouped list: icon, what it is, what it does, chevron. */
@Deprecated(
    "Use MhListRow inside an MhListGroup.",
    ReplaceWith(
        "MhListRow(title = motoHubText(title), modifier = modifier, subtitle = motoHubText(description), icon = modeIconVector(icon), onClick = onClick)",
        "io.motohub.android.i18n.motoHubText"
    )
)
@Composable
fun HeroOptionRow(
    title: String,
    description: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String = "\u203a"
) {
    MhListRow(
        title = motoHubText(title),
        subtitle = motoHubText(description),
        icon = modeIconVector(icon),
        modifier = modifier,
        onClick = onClick
    )
}
