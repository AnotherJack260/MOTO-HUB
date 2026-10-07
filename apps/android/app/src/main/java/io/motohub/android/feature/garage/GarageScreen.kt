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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.ui.components.MhButtonSize
import io.motohub.android.ui.components.MhEmptyState
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTabPage
import io.motohub.android.ui.components.MhTopBarAction
import io.motohub.android.ui.components.ScreenCrossfade
import io.motohub.android.ui.components.mhPressable
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
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    MhTabPage(
        title = motoHubText("Garage"),
        // Once a motorcycle exists the three ways to add one move behind "+"; the empty state
        // still shows them, so it needs no "+".
        actions = {
            if (active != null) {
                MhTopBarAction(Icons.Rounded.Add, motoHubText("Add a motorcycle"), onClick = { showAddSheet = true })
            }
        }
    ) {
        // The first pairing, or "Use" on another motorcycle, swaps this section in place: the
        // "Garage" title stays put, the section fades through, and the height follows (P11: state
        // swaps fade, only navigation slides).
        ScreenCrossfade(
            screen = active?.id,
            modifier = Modifier.fillMaxWidth(),
            label = "garage-current",
            animateHeight = true
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

    if (showAddSheet) {
        // P7's three rows; each runs once the sheet has gone (P10).
        MhSheet(
            onDismiss = { showAddSheet = false },
            title = motoHubText("Add a motorcycle"),
            // True for all three paths: each one matches a saved motorcycle by its Wi-Fi name.
            body = motoHubText("Adding a motorcycle you already saved updates it.")
        ) { close ->
            MhListRow(
                title = motoHubText("Scan QR code"),
                icon = Icons.Rounded.QrCodeScanner,
                onClick = { close(onAddMotorcycle) }
            )
            OtherPairingRows({ close(onImportQrPhoto) }, { close(onAddMotorcycleManually) })
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

// Which motorcycle is current is said by its place and its header, not by a colour. With a photo
// the picture runs edge to edge across the top, rounded only by the card's own clip; without one
// the card is a single 72 dp row led by the motorcycle-glyph circle.
@Composable
private fun CurrentBikeCard(profile: MotorcycleProfile, onOpenDetails: () -> Unit) {
    val hasPhoto = profile.photoPath != null
    Column(
        Modifier
            .fillMaxWidth()
            .mhPressable(MaterialTheme.shapes.large, onClick = onOpenDetails)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (hasPhoto) {
            MotorcyclePhoto(
                path = profile.photoPath,
                modifier = Modifier.fillMaxWidth().height(168.dp),
                shape = RectangleShape
            )
        }
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasPhoto) MotorcyclePhoto(path = null, modifier = Modifier.size(56.dp), shape = CircleShape)
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
        // 40 dp like every row's icon circle, so the text column lines up with MhListRow's.
        MotorcyclePhoto(path = profile.photoPath, modifier = Modifier.size(40.dp), shape = CircleShape)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.shownName(), style = MaterialTheme.typography.titleMedium)
            Text(
                profile.ssid,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MhSecondaryButton(motoHubText("Use"), onSelect, size = MhButtonSize.COMPACT)
    }
}
