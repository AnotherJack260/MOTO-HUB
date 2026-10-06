// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import io.motohub.android.i18n.motoHubText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.androidauto.AndroidAutoDisplayMode
import io.motohub.android.androidauto.TBoxScreenMargins
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.units.UnitFormat
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.TBoxModelProfile
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhTextField
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.ScreenSlideTransition
import io.motohub.android.ui.theme.MotoHubColors

private enum class MotorcycleDetail { ANDROID_AUTO_DISPLAY, TFT_MARGINS, PROFILE_OVERRIDE }

/** Where a motorcycle photo comes from when the rider adds or changes it. */
enum class MotorcyclePhotoSource { CAMERA, GALLERY, FILES }

/**
 * Motorcycle profile screen. A compact hub (photo, name, connection info) with the
 * less-frequently-touched settings behind drill-down screens, mirroring the same
 * hub/detail pattern [io.motohub.android.feature.settings.SettingsScreen] already uses -
 * replacing what used to be nine stacked cards in a single long scroll with mixed
 * (some instant, some button-gated) save behavior. Every field here now saves immediately,
 * with no separate "Save profile" step.
 */
@Composable
fun MotorcycleDetailsScreen(
    profile: MotorcycleProfile,
    displayMode: AndroidAutoDisplayMode,
    screenMargins: TBoxScreenMargins,
    onBack: () -> Unit,
    onSave: (MotorcycleProfile) -> Boolean,
    onOpenCapabilities: () -> Unit,
    onCustomizeDashboard: () -> Unit,
    onDisplayModeChanged: (AndroidAutoDisplayMode) -> Unit,
    onScreenMarginsChanged: (TBoxScreenMargins) -> Unit,
    onChoosePhoto: (MotorcyclePhotoSource) -> Unit,
    onRemovePhoto: () -> Unit,
    onDelete: () -> Unit
) {
    var detail by rememberSaveable(profile.id) { mutableStateOf<MotorcycleDetail?>(null) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    // Each screen's MhScreen answers system back like its arrow: a sub-detail returns here, this
    // list leaves through onBack.
    ScreenSlideTransition(screen = detail, isBase = { it == null }, label = "motorcycle-details") { current ->
        when (current) {
            null -> MotorcycleDetailsMainList(
                profile = profile,
                displayMode = displayMode,
                onBack = onBack,
                onSave = onSave,
                onOpenDetail = { detail = it },
                onOpenCapabilities = onOpenCapabilities,
                onCustomizeDashboard = onCustomizeDashboard,
                onChoosePhoto = onChoosePhoto,
                onRemovePhoto = onRemovePhoto,
                onRequestDelete = { showDeleteConfirmation = true }
            )
            MotorcycleDetail.ANDROID_AUTO_DISPLAY -> AndroidAutoDisplayDetail(
                displayMode = displayMode,
                onDisplayModeChanged = onDisplayModeChanged,
                onBack = { detail = null }
            )
            MotorcycleDetail.TFT_MARGINS -> TftMarginsDetail(
                profile = profile,
                screenMargins = screenMargins,
                onScreenMarginsChanged = onScreenMarginsChanged,
                onBack = { detail = null }
            )
            MotorcycleDetail.PROFILE_OVERRIDE -> ProfileOverrideDetail(
                profile = profile,
                onSave = onSave,
                onBack = { detail = null }
            )
        }
    }

    if (showDeleteConfirmation) {
        MhSheet(
            onDismiss = { showDeleteConfirmation = false },
            title = motoHubText("Remove %1\$s?", profile.shownName()),
            body = motoHubText("Its connection details and photo will be deleted from this phone."),
            primaryLabel = motoHubText("Remove"),
            onPrimary = onDelete,
            secondaryLabel = motoHubText("Cancel"),
            primaryStyle = MhActionStyle.DESTRUCTIVE
        )
    }
}

@Composable
private fun MotorcycleDetailsMainList(
    profile: MotorcycleProfile,
    displayMode: AndroidAutoDisplayMode,
    onBack: () -> Unit,
    onSave: (MotorcycleProfile) -> Boolean,
    onOpenDetail: (MotorcycleDetail) -> Unit,
    onOpenCapabilities: () -> Unit,
    onCustomizeDashboard: () -> Unit,
    onChoosePhoto: (MotorcyclePhotoSource) -> Unit,
    onRemovePhoto: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    val units = MotoHubSettings.distanceUnits(context)
    var name by rememberSaveable(profile.id) { mutableStateOf(profile.displayName.orEmpty()) }
    // The field edits the profile's km-native tank range in the rider's display unit.
    var fuelTankRangeText by rememberSaveable(profile.id) {
        mutableStateOf(
            profile.fuelTankRangeKm?.let { UnitFormat.wholeDistanceFromKm(it, units) }?.toString().orEmpty()
        )
    }
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }

    // Success stays silent: a snackbar per keystroke would be noise.
    fun persist(newName: String = name, newFuelText: String = fuelTankRangeText) {
        val saved = onSave(
            profile.copy(
                displayName = newName.trim().takeIf { it.isNotEmpty() },
                fuelTankRangeKm = newFuelText.toDoubleOrNull()?.takeIf { it > 0 }
                    ?.let { UnitFormat.kmFromDistance(it, units) }
            )
        )
        if (!saved) MotoHubSnackbar.error(context, motoHubText("Couldn't save changes"))
    }

    MhScreen(
        title = name.trim().ifEmpty { motoHubText("My motorcycle") },
        onBack = onBack
    ) {
        val hasPhoto = profile.photoPath != null
        val photoAction = if (hasPhoto) motoHubText("Change photo") else motoHubText("Add photo")
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(MaterialTheme.shapes.large)
                .semantics { contentDescription = photoAction }
                .clickable(role = Role.Button) { showPhotoSheet = true }
        ) {
            MotorcyclePhoto(path = profile.photoPath, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp))
            MhIconCircle(
                if (hasPhoto) Icons.Rounded.Edit else Icons.Rounded.PhotoCamera,
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                container = MotoHubColors.SurfaceHigh
            )
        }
        MhTextField(
            value = name,
            onValueChange = { input ->
                name = input
                persist(newName = input)
            },
            label = motoHubText("Name")
        )
        // CORE only stores and exports it - nothing reads it there - so it is not asked for.
        if (BuildConfig.IS_PRO) {
            MhTextField(
                value = fuelTankRangeText,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    fuelTankRangeText = digitsOnly
                    persist(newFuelText = digitsOnly)
                },
                label = motoHubText("Tank range (%1\$s)", UnitFormat.wholeDistanceLabel(units)),
                helper = motoHubText("Distance on a full tank."),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        GarageSection(motoHubText("Android Auto")) {
            MhListRow(
                title = motoHubText("Display fit"),
                icon = Icons.Rounded.AspectRatio,
                value = displayMode.shortLabel,
                onClick = { onOpenDetail(MotorcycleDetail.ANDROID_AUTO_DISPLAY) }
            )
            MhListRow(
                title = motoHubText("Screen margins"),
                subtitle = motoHubText("Leave room for the dashboard's own display"),
                icon = Icons.Rounded.CropFree,
                onClick = { onOpenDetail(MotorcycleDetail.TFT_MARGINS) }
            )
            if (BuildConfig.IS_PRO) {
                MhListRow(
                    title = motoHubText("Customize dashboard"),
                    subtitle = motoHubText("Choose widgets for each side panel"),
                    icon = Icons.Rounded.Tune,
                    onClick = onCustomizeDashboard
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Info rows: nothing to tap, so no chevron.
            GarageSection(motoHubText("Connection")) {
                MhListRow(title = motoHubText("Wi-Fi name"), trailing = { MonoValue(profile.ssid) })
                profile.modelId?.let { modelId ->
                    MhListRow(title = motoHubText("Dashboard ID"), trailing = { MonoValue(modelId) })
                }
            }
            MhFootnote(motoHubText("The Wi-Fi password is stored securely on this phone."))
        }

        GarageSection(motoHubText("Advanced")) {
            MhListRow(
                title = motoHubText("Dashboard profile"),
                icon = Icons.Rounded.Memory,
                value = ProfileOverride.byKey(profile.profileOverrideKey).title(),
                onClick = { onOpenDetail(MotorcycleDetail.PROFILE_OVERRIDE) }
            )
            MhListRow(
                title = motoHubText("Dashboard capabilities"),
                subtitle = motoHubText("What the dashboard reports about itself"),
                icon = Icons.Rounded.Info,
                onClick = onOpenCapabilities
            )
        }

        MhListGroup {
            MhListRow(
                title = motoHubText("Remove motorcycle"),
                icon = Icons.Rounded.DeleteOutline,
                iconTint = MotoHubColors.Error,
                titleColor = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = onRequestDelete
            )
        }
    }

    if (showPhotoSheet) {
        // The same two picker rows as the QR import sheet, plus the camera and removal.
        MhSheet(onDismiss = { showPhotoSheet = false }, title = motoHubText("Motorcycle photo")) { close ->
            MhListRow(
                title = motoHubText("Take a photo"),
                icon = Icons.Rounded.PhotoCamera,
                showChevron = false,
                onClick = { close { onChoosePhoto(MotorcyclePhotoSource.CAMERA) } }
            )
            MhListRow(
                title = motoHubText("Choose from gallery"),
                icon = Icons.Rounded.PhotoLibrary,
                showChevron = false,
                onClick = { close { onChoosePhoto(MotorcyclePhotoSource.GALLERY) } }
            )
            MhListRow(
                title = motoHubText("Browse files"),
                subtitle = motoHubText("Downloads, cloud drives and other folders"),
                icon = Icons.Rounded.FolderOpen,
                showChevron = false,
                onClick = { close { onChoosePhoto(MotorcyclePhotoSource.FILES) } }
            )
            if (profile.photoPath != null) {
                MhListRow(
                    title = motoHubText("Remove photo"),
                    icon = Icons.Rounded.DeleteOutline,
                    iconTint = MotoHubColors.Error,
                    titleColor = MaterialTheme.colorScheme.error,
                    showChevron = false,
                    onClick = { close(onRemovePhoto) }
                )
            }
        }
    }
}

// Capped so a long Wi-Fi name wraps instead of squeezing the row title; never cut.
@Composable
private fun MonoValue(text: String) {
    Text(
        text,
        modifier = Modifier.widthIn(max = 180.dp),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Also reused by [io.motohub.android.feature.garage.DefaultDashboardSettingsScreen]. */
@Composable
fun AndroidAutoDisplayDetail(
    displayMode: AndroidAutoDisplayMode,
    onDisplayModeChanged: (AndroidAutoDisplayMode) -> Unit,
    onBack: () -> Unit
) {
    MhScreen(title = motoHubText("Display fit"), onBack = onBack) {
        MhListGroup {
            AndroidAutoDisplayMode.entries.forEach { mode ->
                MhChoiceRow(
                    title = mode.shortLabel,
                    subtitle = mode.description,
                    selected = mode == displayMode,
                    onClick = { onDisplayModeChanged(mode) }
                )
            }
        }
    }
}

/** Also reused by [io.motohub.android.feature.garage.DefaultDashboardSettingsScreen]. */
@Composable
fun TftMarginsDetail(
    profile: MotorcycleProfile,
    screenMargins: TBoxScreenMargins,
    onScreenMarginsChanged: (TBoxScreenMargins) -> Unit,
    onBack: () -> Unit
) {
    MhScreen(title = motoHubText("Screen margins"), onBack = onBack) {
        MhFootnote(motoHubText("Leave room for the dashboard's own display. Applies to Android Auto video and touch."))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarginField(motoHubText("Top"), screenMargins.top, { value ->
                onScreenMarginsChanged(screenMargins.copy(top = value))
            }, Modifier.weight(1f))
            MarginField(motoHubText("Bottom"), screenMargins.bottom, { value ->
                onScreenMarginsChanged(screenMargins.copy(bottom = value))
            }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MarginField(motoHubText("Left"), screenMargins.left, { value ->
                onScreenMarginsChanged(screenMargins.copy(left = value))
            }, Modifier.weight(1f))
            MarginField(motoHubText("Right"), screenMargins.right, { value ->
                onScreenMarginsChanged(screenMargins.copy(right = value))
            }, Modifier.weight(1f))
        }
        MhFootnote(motoHubText("In pixels, 0 to %1\$d.", TBoxScreenMargins.MAX))
        MhSecondaryButton(
            motoHubText("Reset to default"),
            onClick = {
                // Restore this motorcycle model's own default margins, not zero -
                // some models (e.g. the 800NK family) ship with a non-zero default
                // because their native UI occupies part of the TFT out of the box.
                onScreenMarginsChanged(TBoxModelProfile.fromModelId(profile.modelId).defaultScreenMargins)
            }
        )
    }
}

@Composable
private fun ProfileOverrideDetail(
    profile: MotorcycleProfile,
    onSave: (MotorcycleProfile) -> Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var profileOverrideKey by rememberSaveable(profile.id) {
        mutableStateOf(profile.profileOverrideKey)
    }

    @Composable
    fun OverrideRow(override: ProfileOverride) {
        val isActive = ProfileOverride.byKey(profileOverrideKey) == override
        MhChoiceRow(
            title = override.title(),
            subtitle = override.subtitle(),
            selected = isActive,
            onClick = {
                // Tapping the chosen one again goes back to Auto.
                val newKey = if (isActive) null else override.key
                if (onSave(profile.copy(profileOverrideKey = newKey))) {
                    profileOverrideKey = newKey
                } else {
                    MotoHubSnackbar.error(context, motoHubText("Couldn't save changes"))
                }
                val label = if (isActive) motoHubText("Auto") else override.title()
                ProjectionEventLog.record(
                    "GARAGE",
                    "Profile override for ${profile.ssid}: $label."
                )
            }
        )
    }

    // A partition of the enum by its own flags, so the next experiment files itself.
    val neutral = listOf(ProfileOverride.AUTO, ProfileOverride.GENERIC)
    val developer = ProfileOverride.entries.filter { !it.riderSelectable }
    val experimental = ProfileOverride.entries.filter { it.experimental && it.riderSelectable }
    val motorcycles = ProfileOverride.entries - neutral - developer - experimental

    MhScreen(title = motoHubText("Dashboard profile"), onBack = onBack) {
        MhFootnote(motoHubText("Auto suits most motorcycles. Choose one only if the dashboard misbehaves."))
        MhListGroup { neutral.forEach { OverrideRow(it) } }
        GarageSection(motoHubText("Motorcycles")) { motorcycles.forEach { OverrideRow(it) } }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GarageSection(motoHubText("Experimental")) { experimental.forEach { OverrideRow(it) } }
            MhFootnote(motoHubText("Trials for specific dashboard problems. Switch back to Auto if nothing improves."))
        }
        GarageSection(motoHubText("Developer")) { developer.forEach { OverrideRow(it) } }
    }
}

// The two neutral choices are rider words and get translated; every other entry is a model name
// or a technical note, shown as written.
private fun ProfileOverride.title(): String = when (this) {
    ProfileOverride.AUTO -> motoHubText("Auto")
    ProfileOverride.GENERIC -> motoHubText("Generic dashboard")
    else -> label
}

private fun ProfileOverride.subtitle(): String = when (this) {
    ProfileOverride.AUTO -> motoHubText("Detect from the motorcycle (recommended)")
    ProfileOverride.GENERIC -> motoHubText("Neutral defaults for a dashboard that is not recognised")
    else -> description
}

val AndroidAutoDisplayMode.shortLabel: String
    get() = when (this) {
        AndroidAutoDisplayMode.LETTERBOX -> motoHubText("Fit")
        AndroidAutoDisplayMode.STRETCH -> motoHubText("Stretch")
        AndroidAutoDisplayMode.FILL -> motoHubText("Crop")
    }

@Composable
private fun MarginField(
    label: String,
    value: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier
) {
    MhTextField(
        value = value.toString(),
        onValueChange = { input ->
            input.toIntOrNull()?.let { onValueChanged(it.coerceIn(0, TBoxScreenMargins.MAX)) }
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
