// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.ui.components.MhEmptyState
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhTabPage
import io.motohub.android.ui.components.ScreenSlideTransition
import io.motohub.android.ui.theme.MotoHubColors

@Composable
fun GarageTabContent(
    profiles: List<MotorcycleProfile>,
    activeProfileId: String?,
    onAddMotorcycle: () -> Unit,
    onImportQrPhoto: () -> Unit,
    onAddMotorcycleManually: () -> Unit,
    onSelectMotorcycle: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onOpenDefaultSettings: () -> Unit = {}
) {
    val active = profiles.firstOrNull { it.id == activeProfileId }

    MhTabPage(title = motoHubText("Garage")) {
        // The rider's first pairing is the one moment this whole tab has a single before/after:
        // the empty state becomes the current-motorcycle card, in a page whose "Garage" title
        // stays put - so this animates only the piece that actually changed.
        ScreenSlideTransition(
            screen = active?.id,
            isBase = { it == null },
            modifier = Modifier.fillMaxWidth()
        ) { activeId ->
            val shownActive = profiles.firstOrNull { it.id == activeId }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (shownActive == null) {
                    MhEmptyState(
                        icon = Icons.Rounded.TwoWheeler,
                        title = motoHubText("No motorcycles yet"),
                        body = motoHubText("Scan the QR code on your dashboard to add one."),
                        actionLabel = motoHubText("Scan QR code"),
                        onAction = onAddMotorcycle,
                        actionIcon = Icons.Rounded.QrCodeScanner
                    )
                    MhListGroup { OtherPairingRows(onImportQrPhoto, onAddMotorcycleManually) }
                } else {
                    val shownOthers = profiles.filterNot { it.id == activeId }
                    if (shownOthers.isEmpty()) {
                        CurrentBikeCard(shownActive) { onOpenDetails(shownActive.id) }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MhSectionHeader(motoHubText("Current motorcycle"))
                            CurrentBikeCard(shownActive) { onOpenDetails(shownActive.id) }
                        }
                        GarageSection(motoHubText("Other motorcycles")) {
                            shownOthers.forEach { profile ->
                                BikeRow(
                                    profile = profile,
                                    onSelect = { onSelectMotorcycle(profile.id) },
                                    onOpenDetails = { onOpenDetails(profile.id) }
                                )
                            }
                        }
                    }
                    GarageSection(motoHubText("Add a motorcycle")) {
                        MhListRow(
                            title = motoHubText("Scan QR code"),
                            icon = Icons.Rounded.QrCodeScanner,
                            onClick = onAddMotorcycle
                        )
                        OtherPairingRows(onImportQrPhoto, onAddMotorcycleManually)
                    }
                    // True for all three paths: each one matches a saved motorcycle by its Wi-Fi name.
                    MhFootnote(motoHubText("Adding a motorcycle you already saved updates it."))
                }
            }
        }

        if (BuildConfig.IS_PRO) {
            GarageSection(motoHubText("Without a motorcycle")) {
                MhListRow(
                    title = motoHubText("Default settings"),
                    subtitle = motoHubText("Used by phone-only display modes without a T-Box"),
                    icon = Icons.Rounded.Tune,
                    onClick = onOpenDefaultSettings
                )
            }
        }
    }
}

/** The two setup rows after "Scan QR code", worded exactly as on Ride so they share one key. */
@Composable
private fun OtherPairingRows(onImportQrPhoto: () -> Unit, onAddMotorcycleManually: () -> Unit) {
    MhListRow(
        title = motoHubText("Import QR code"),
        subtitle = motoHubText("From a photo or screenshot"),
        icon = Icons.Rounded.Image,
        onClick = onImportQrPhoto
    )
    MhListRow(
        title = motoHubText("Enter details manually"),
        subtitle = motoHubText("Wi-Fi name and password"),
        icon = Icons.Rounded.Keyboard,
        onClick = onAddMotorcycleManually
    )
}

/** What a motorcycle is called everywhere in the Garage. */
internal fun MotorcycleProfile.shownName(): String =
    displayName?.takeIf(String::isNotBlank) ?: motoHubText("My motorcycle")

/** A header and its group, 8 dp apart; the page's 16 dp plus the header's own 8 make 24 between sections. */
@Composable
internal fun GarageSection(header: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MhSectionHeader(header)
        MhListGroup(content = content)
    }
}

// Which motorcycle is current is said by its place and its header, not by a colour.
@Composable
private fun CurrentBikeCard(profile: MotorcycleProfile, onOpenDetails: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpenDetails)
    ) {
        MotorcyclePhoto(
            path = profile.photoPath,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(168.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    profile.shownName(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    profile.ssid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MotoHubColors.TextTertiary
            )
        }
    }
}

// Private rather than an MhListRow: the row leads with the motorcycle's own photo, not an icon.
@Composable
private fun BikeRow(profile: MotorcycleProfile, onSelect: () -> Unit, onOpenDetails: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onOpenDetails)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MotorcyclePhoto(path = profile.photoPath, modifier = Modifier.size(48.dp), shape = CircleShape)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.shownName(), style = MaterialTheme.typography.titleMedium)
            Text(
                profile.ssid,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MhSecondaryButton(motoHubText("Use"), onSelect, fillWidth = false)
    }
}
