// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.garage

import io.motohub.android.i18n.motoHubText

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotTouch
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.motohub.android.BuildConfig
import io.motohub.android.androidauto.AndroidAutoDisplayMode
import io.motohub.android.androidauto.TBoxScreenMargins
import io.motohub.android.feature.controls.HandlebarControlStore
import io.motohub.android.feature.controls.HandlebarDetail
import io.motohub.android.feature.controls.HandlebarMappingScreen
import io.motohub.android.feature.controls.MotorcycleScope
import io.motohub.android.feature.controls.handlebarProblem
import io.motohub.android.feature.controls.rememberOnResume
import io.motohub.android.feature.settings.AndroidAutoAspectMatchingMode
import io.motohub.android.feature.settings.AndroidAutoSize
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.units.UnitFormat
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.TBoxCapabilityStore
import io.motohub.android.tbox.TBoxModelProfile
import io.motohub.android.ui.components.MhActionStyle
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhIconCircle
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhSwitchRow
import io.motohub.android.ui.components.MhTextField
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.ScreenSlideTransition
import io.motohub.android.ui.theme.MotoHubColors

private enum class MotorcycleDetail {
    ANDROID_AUTO_DISPLAY, TFT_MARGINS, PROFILE_OVERRIDE, ADVANCED, AA_RESOLUTION, AA_DENSITY, HANDLEBAR, HANDLEBAR_MAPPING
}

/** Where back goes: the screen that opened this one, or the main list. */
private val MotorcycleDetail?.parent: MotorcycleDetail?
    get() = when (this) {
        MotorcycleDetail.ANDROID_AUTO_DISPLAY, MotorcycleDetail.TFT_MARGINS, MotorcycleDetail.PROFILE_OVERRIDE ->
            MotorcycleDetail.ADVANCED
        MotorcycleDetail.HANDLEBAR_MAPPING -> MotorcycleDetail.HANDLEBAR
        else -> null
    }

private val MotorcycleDetail?.depth: Int get() = if (this == null) 0 else 1 + parent.depth

/** The pickers the main list opens (P1). */
private enum class DetailsSheet { SIZE, FIT, IMAGE, ON_ARRIVAL }

/** Where a motorcycle photo comes from when the rider adds or changes it. */
enum class MotorcyclePhotoSource { CAMERA, GALLERY, FILES }

/**
 * Motorcycle profile screen. A compact hub (photo, name, connection info) with the
 * less-frequently-touched settings behind drill-down screens, mirroring the same
 * hub/detail pattern [io.motohub.android.feature.settings.SettingsScreen] already uses -
 * replacing what used to be nine stacked cards in a single long scroll with mixed
 * (some instant, some button-gated) save behavior. The photo is the page header and the name is
 * a row that opens its own sheet (Save or Cancel); every other field saves immediately.
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
    // Every setting on these screens is THIS motorcycle's, whichever one is live or active.
    val context = LocalContext.current
    val bike = remember(profile.id) { MotorcycleScope.of(context, profile.id) }
    // Details go three deep (Advanced > Display fit), so the slide is told directly whether a
    // move went up a level, the way Settings does it.
    var goingUp by remember { mutableStateOf(false) }
    val go: (MotorcycleDetail?) -> Unit = { to ->
        goingUp = to.depth < detail.depth
        detail = to
    }

    // Each screen's MhScreen answers system back like its arrow: a sub-detail returns to its
    // parent, this list leaves through onBack.
    ScreenSlideTransition(screen = detail, isBase = { goingUp }, label = "motorcycle-details") { current ->
        val back = { go(current.parent) }
        when (current) {
            null -> MotorcycleDetailsMainList(
                profile = profile,
                bike = bike,
                onBack = onBack,
                onSave = onSave,
                onOpenDetail = go,
                onCustomizeDashboard = onCustomizeDashboard,
                onDisplayModeChanged = onDisplayModeChanged,
                onChoosePhoto = onChoosePhoto,
                onRemovePhoto = onRemovePhoto,
                onRequestDelete = { showDeleteConfirmation = true }
            )
            MotorcycleDetail.ADVANCED -> MotorcycleAdvancedScreen(
                profile = profile,
                bike = bike,
                displayMode = displayMode,
                onBack = back,
                onOpenProfileOverride = { go(MotorcycleDetail.PROFILE_OVERRIDE) },
                onOpenDisplayFit = { go(MotorcycleDetail.ANDROID_AUTO_DISPLAY) },
                onOpenScreenMargins = { go(MotorcycleDetail.TFT_MARGINS) },
                onOpenCapabilities = onOpenCapabilities
            )
            MotorcycleDetail.AA_RESOLUTION -> AndroidAutoResolutionDetail(bike, back)
            MotorcycleDetail.AA_DENSITY -> AndroidAutoDensityDetail(bike, back)
            // The handlebar screens read their stores through LocalContext, so this motorcycle's
            // scoped context is provided to them whole; nothing under it casts it to an Activity.
            MotorcycleDetail.HANDLEBAR -> CompositionLocalProvider(LocalContext provides bike) {
                HandlebarDetail(onBack = back, onOpenMapping = { go(MotorcycleDetail.HANDLEBAR_MAPPING) })
            }
            MotorcycleDetail.HANDLEBAR_MAPPING -> CompositionLocalProvider(LocalContext provides bike) {
                HandlebarMappingScreen(onBack = back)
            }
            MotorcycleDetail.ANDROID_AUTO_DISPLAY -> AndroidAutoDisplayDetail(
                displayMode = displayMode,
                onDisplayModeChanged = onDisplayModeChanged,
                onBack = back
            )
            MotorcycleDetail.TFT_MARGINS -> TftMarginsDetail(
                profile = profile,
                screenMargins = screenMargins,
                onScreenMarginsChanged = onScreenMarginsChanged,
                onBack = back
            )
            MotorcycleDetail.PROFILE_OVERRIDE -> ProfileOverrideDetail(
                profile = profile,
                onSave = onSave,
                onBack = back
            )
        }
    }

    if (showDeleteConfirmation) {
        // The glossary's curly quotes go round a name the rider typed; an unnamed one has none.
        val typedName = profile.displayName?.takeIf(String::isNotBlank)
        MhSheet(
            onDismiss = { showDeleteConfirmation = false },
            title = typedName?.let { motoHubText("Remove “%1\$s”?", it) } ?: motoHubText("Remove this motorcycle?"),
            body = if (profile.photoPath != null) {
                motoHubText("Its connection details and photo will be deleted from this phone.")
            } else {
                motoHubText("Its connection details will be deleted from this phone.")
            },
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
    bike: Context,
    onBack: () -> Unit,
    onSave: (MotorcycleProfile) -> Boolean,
    onOpenDetail: (MotorcycleDetail) -> Unit,
    onCustomizeDashboard: () -> Unit,
    onDisplayModeChanged: (AndroidAutoDisplayMode) -> Unit,
    onChoosePhoto: (MotorcyclePhotoSource) -> Unit,
    onRemovePhoto: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val context = LocalContext.current
    val units = MotoHubSettings.distanceUnits(context)
    // This motorcycle's values, re-read whenever the list comes back from a sub-screen.
    var size by remember(profile.id) { mutableStateOf(MotoHubSettings.androidAutoSize(bike)) }
    var fit by remember(profile.id) { mutableStateOf(MotoHubSettings.androidAutoAspectMatching(bike)) }
    var ignoreTouch by remember(profile.id) { mutableStateOf(MotoHubSettings.disableTouchscreen(bike)) }
    var image by remember(profile.id) { mutableStateOf(MotoHubSettings.imageQuality(bike)) }
    var onArrival by remember(profile.id) {
        mutableStateOf(MotoHubSettings.autostartService(bike).takeIf { MotoHubSettings.autostartEnabled(bike) })
    }
    // "On" while presses can't arrive is the one answer that sends a rider hunting, so the broken
    // prerequisite is said here, a screen up from where it is fixed.
    val handlebarProblem = rememberOnResume { handlebarProblem(bike) }
    // The touch row only where there is a touchscreen to ignore, by the profile a session resolves.
    val touchDashboard = remember(profile) {
        TBoxModelProfile.resolve(
            profile.modelId,
            TBoxCapabilityStore(context).load(profile)?.capabilities,
            ProfileOverride.byKey(profile.profileOverrideKey)
        ).supportsScreenTouch
    }
    var sheet by rememberSaveable(profile.id) { mutableStateOf<DetailsSheet?>(null) }
    fun log(setting: String, value: String) =
        ProjectionEventLog.record("GARAGE", "$setting for ${profile.ssid}: $value.")
    // The field edits the profile's km-native tank range in the rider's display unit.
    var fuelTankRangeText by rememberSaveable(profile.id) {
        mutableStateOf(
            profile.fuelTankRangeKm?.let { UnitFormat.wholeDistanceFromKm(it, units) }?.toString().orEmpty()
        )
    }
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }
    // What the Name sheet's field holds while it is open; null when it is closed.
    var nameDraft by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }

    // Success stays silent: the row, or the field itself, already shows the new value.
    fun persist(updated: MotorcycleProfile) {
        if (!onSave(updated)) MotoHubSnackbar.error(context, motoHubText("Couldn't save changes"))
    }

    MhScreen(
        title = profile.shownName(),
        onBack = onBack,
        // Without a photo: the standard large title and collapse; "Add photo" is the first row.
        header = if (profile.photoPath == null) null else {
            { DetailsHeader(profile, onOpenPhotoSheet = { showPhotoSheet = true }) }
        }
    ) {
        MhListGroup {
            if (profile.photoPath == null) {
                MhListRow(
                    title = motoHubText("Add photo"),
                    icon = Icons.Rounded.AddAPhoto,
                    showChevron = false,
                    onClick = { showPhotoSheet = true }
                )
            }
            MhListRow(
                title = motoHubText("Name"),
                icon = Icons.Rounded.Edit,
                value = profile.shownName(),
                onClick = { nameDraft = profile.displayName.orEmpty() }
            )
        }
        // CORE only stores and exports it - nothing reads it there - so it is not asked for.
        if (BuildConfig.IS_PRO) {
            MhTextField(
                value = fuelTankRangeText,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    fuelTankRangeText = digitsOnly
                    persist(
                        profile.copy(
                            fuelTankRangeKm = digitsOnly.toDoubleOrNull()?.takeIf { it > 0 }
                                ?.let { UnitFormat.kmFromDistance(it, units) }
                        )
                    )
                },
                label = motoHubText("Tank range (%1\$s)", UnitFormat.wholeDistanceLabel(units)),
                helper = motoHubText("Distance on a full tank."),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        GarageSection(motoHubText("Android Auto")) {
            MhListRow(
                title = motoHubText("Size"),
                icon = Icons.Rounded.ZoomOutMap,
                value = size.title(),
                onClick = { sheet = DetailsSheet.SIZE }
            )
            // Custom's two answers, folded in under it. The stored pair is kept while another
            // size is chosen, so coming back to Custom shows it again.
            AnimatedVisibility(size == AndroidAutoSize.CUSTOM, enter = MhMotion.foldIn, exit = MhMotion.foldOut) {
                Column {
                    MhListRow(
                        title = motoHubText("Resolution"),
                        icon = Icons.Rounded.Monitor,
                        value = MotoHubSettings.androidAutoResolution(bike).title(bike),
                        onClick = { onOpenDetail(MotorcycleDetail.AA_RESOLUTION) }
                    )
                    MhListRow(
                        title = motoHubText("Interface size"),
                        icon = Icons.Rounded.FormatSize,
                        value = bike.getString(MotoHubSettings.androidAutoDensity(bike).labelRes),
                        onClick = { onOpenDetail(MotorcycleDetail.AA_DENSITY) }
                    )
                }
            }
            MhListRow(
                title = motoHubText("Fit"),
                icon = Icons.Rounded.FitScreen,
                value = fit.fitTitle(),
                onClick = { sheet = DetailsSheet.FIT }
            )
            if (BuildConfig.IS_PRO) {
                MhListRow(
                    title = motoHubText("Customize dashboard"),
                    subtitle = motoHubText("Choose widgets for each side panel"),
                    icon = Icons.Rounded.Tune,
                    onClick = onCustomizeDashboard
                )
            }
            if (touchDashboard) {
                MhSwitchRow(
                    title = motoHubText("Ignore the touchscreen"),
                    subtitle = motoHubText("Use handlebar buttons even on a touch dashboard"),
                    icon = Icons.Rounded.DoNotTouch,
                    checked = ignoreTouch,
                    onCheckedChange = {
                        ignoreTouch = it
                        MotoHubSettings.setDisableTouchscreen(bike, it)
                        log("Ignore the touchscreen", "$it")
                    }
                )
            }
        }

        GarageSection(motoHubText("Video")) {
            MhListRow(
                title = motoHubText("Image"),
                icon = Icons.Rounded.HighQuality,
                value = image.title(),
                onClick = { sheet = DetailsSheet.IMAGE }
            )
        }

        GarageSection(motoHubText("On the motorcycle")) {
            MhListRow(
                title = motoHubText("On arrival"),
                icon = Icons.Rounded.PlayCircle,
                value = onArrival?.title() ?: motoHubText("Off"),
                onClick = { sheet = DetailsSheet.ON_ARRIVAL }
            )
            MhListRow(
                title = motoHubText("Handlebar buttons"),
                icon = Icons.Rounded.SportsEsports,
                subtitle = handlebarProblem,
                subtitleColor = MotoHubColors.Warning,
                value = when {
                    handlebarProblem != null -> null
                    HandlebarControlStore.isEnabled(bike) -> motoHubText("On")
                    else -> motoHubText("Off")
                },
                onClick = { onOpenDetail(MotorcycleDetail.HANDLEBAR) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Info rows: nothing to tap, so no chevron.
            GarageSection(motoHubText("Connection")) {
                MhListRow(title = motoHubText("Wi-Fi name"), icon = Icons.Rounded.Wifi, trailing = { MonoValue(profile.ssid) })
                profile.modelId?.let { modelId ->
                    MhListRow(title = motoHubText("Dashboard ID"), icon = Icons.Rounded.Memory, trailing = { MonoValue(modelId) })
                }
            }
            MhFootnote(motoHubText("The Wi-Fi password is stored securely on this phone."))
        }

        // Advanced, collapsed to one row: one level deeper, the way Settings nests.
        MhListGroup {
            MhListRow(
                title = motoHubText("Advanced"),
                icon = Icons.Rounded.Tune,
                onClick = { onOpenDetail(MotorcycleDetail.ADVANCED) }
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

    // Each pick writes through `bike`, captured here: a sheet's own LocalContext is not the motorcycle's.
    val dismiss = { sheet = null }
    when (sheet) {
        DetailsSheet.SIZE -> SizeSheet(
            current = size,
            onPick = {
                size = it
                MotoHubSettings.setAndroidAutoSize(bike, it)
                log("Android Auto size", it.name)
            },
            onDismiss = dismiss
        )
        DetailsSheet.FIT -> FitSheet(
            current = fit,
            onPick = {
                fit = it
                MotoHubSettings.setAndroidAutoAspectMatching(bike, it)
                // "Show everything" has to be true on a profile whose own Display fit crops
                // (CFMOTO MTX800 fills): the whole frame, letterboxed. Display fit stays editable.
                if (it == AndroidAutoAspectMatchingMode.MANUAL) onDisplayModeChanged(AndroidAutoDisplayMode.LETTERBOX)
                log("Android Auto fit", it.name)
            },
            onDismiss = dismiss
        )
        DetailsSheet.IMAGE -> ImageSheet(
            current = image,
            onPick = {
                image = it
                MotoHubSettings.setImageQuality(bike, it)
                log("Image", it.name)
            },
            onDismiss = dismiss
        )
        DetailsSheet.ON_ARRIVAL -> OnArrivalSheet(
            current = onArrival,
            onPick = { service ->
                onArrival = service
                // "Do nothing" keeps the service, so turning it back on starts the same thing.
                service?.let { MotoHubSettings.setAutostartService(bike, it) }
                MotoHubSettings.setAutostartEnabled(bike, service != null)
                log("On arrival", service?.name ?: "NOTHING")
            },
            onDismiss = dismiss
        )
        null -> Unit
    }

    if (showPhotoSheet) {
        MotorcyclePhotoSheet(
            hasPhoto = profile.photoPath != null,
            onDismiss = { showPhotoSheet = false },
            onChoosePhoto = onChoosePhoto,
            onRemovePhoto = onRemovePhoto
        )
    }

    nameDraft?.let { draft ->
        // Saved once, on Save or the keyboard's Done; Cancel, back or a swipe keep the old name.
        val save = { persist(profile.copy(displayName = draft.trim().takeIf { it.isNotEmpty() })) }
        MhSheet(
            onDismiss = { nameDraft = null },
            title = motoHubText("Name"),
            primaryLabel = motoHubText("Save"),
            onPrimary = save,
            secondaryLabel = motoHubText("Cancel")
        ) { close ->
            MhTextField(
                value = draft,
                onValueChange = { nameDraft = it },
                label = motoHubText("Name"),
                modifier = Modifier.padding(horizontal = 16.dp),
                placeholder = motoHubText("My motorcycle"),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { close(save) })
            )
        }
    }
}

/**
 * Where a motorcycle's photo comes from: camera, gallery, files, and removal when there is one.
 * The same two picker rows as the QR import sheet. Motorcycle details and the Ride hero's options
 * both open it.
 */
@Composable
internal fun MotorcyclePhotoSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onChoosePhoto: (MotorcyclePhotoSource) -> Unit,
    onRemovePhoto: () -> Unit
) {
    MhSheet(onDismiss = onDismiss, title = motoHubText("Motorcycle photo")) { close ->
        MhListRow(
            title = motoHubText("Take a photo"),
            icon = Icons.Rounded.PhotoCamera,
            onClick = { close { onChoosePhoto(MotorcyclePhotoSource.CAMERA) } }
        )
        MhListRow(
            title = motoHubText("Choose from gallery"),
            icon = Icons.Rounded.PhotoLibrary,
            onClick = { close { onChoosePhoto(MotorcyclePhotoSource.GALLERY) } }
        )
        MhListRow(
            title = motoHubText("Browse files"),
            subtitle = motoHubText("Downloads, cloud drives and other folders"),
            icon = Icons.Rounded.FolderOpen,
            onClick = { close { onChoosePhoto(MotorcyclePhotoSource.FILES) } }
        )
        if (hasPhoto) {
            MhListRow(
                title = motoHubText("Remove photo"),
                icon = Icons.Rounded.DeleteOutline,
                iconTint = MotoHubColors.Error,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { close(onRemovePhoto) }
            )
        }
    }
}

/**
 * The page header of a motorcycle with a photo, in place of a large title: the picture full-bleed
 * under the floating bar, fading into the page, with the name on its lower edge. The picture opens
 * the photo sheet, and the name is the screen's heading. Without a photo the screen keeps the
 * standard large title instead.
 */
@Composable
private fun DetailsHeader(profile: MotorcycleProfile, onOpenPhotoSheet: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val changePhoto = motoHubText("Change photo")
    Box(Modifier.fillMaxWidth()) {
        MotorcyclePhoto(
            path = profile.photoPath,
            modifier = Modifier
                .matchParentSize()
                .semantics { contentDescription = changePhoto }
                .clickable(role = Role.Button, onClick = onOpenPhotoSheet),
            shape = RectangleShape
        )
        // Into the page's own colour, so the name reads on any picture. Draw-only: taps fall
        // through to the photo.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to background))
        )
        Row(
            // ponytail: a fixed 168 dp of picture above the name; size it by the photo's
            // aspect if riders' photos crop badly.
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 168.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                profile.shownName(),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            // A camera, not a pencil: next to the name, a pencil would read as "rename".
            MhIconCircle(Icons.Rounded.PhotoCamera, container = MotoHubColors.SurfaceHigh)
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
                    title = mode.title,
                    subtitle = mode.description,
                    selected = mode == displayMode,
                    onClick = { onDisplayModeChanged(mode) }
                )
            }
        }
        // What separates it from Fit, which decides the shape Android Auto draws in.
        MhFootnote(motoHubText("Used when Android Auto doesn't fill the screen."))
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

// The two neutral choices are rider words and get translated; every other entry is a model name,
// shown as written.
internal fun ProfileOverride.title(): String = when (this) {
    ProfileOverride.AUTO -> motoHubText("Auto")
    ProfileOverride.GENERIC -> motoHubText("Generic dashboard")
    else -> label
}

// A rider line, or nothing. Experiments have none: the footnote under their group speaks for them.
// The developer group keeps its developer note.
private fun ProfileOverride.subtitle(): String? = when {
    this == ProfileOverride.AUTO -> motoHubText("Detect from the motorcycle (recommended)")
    this == ProfileOverride.GENERIC -> motoHubText("Neutral defaults for a dashboard that is not recognised")
    !riderSelectable -> description
    experimental -> null
    else -> riderNote
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
