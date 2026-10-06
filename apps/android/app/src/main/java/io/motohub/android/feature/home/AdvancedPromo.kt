// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.ui.components.MotoHubSnackbar
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.motohub.android.BuildConfig
import io.motohub.android.i18n.motoHubText
import io.motohub.android.ui.components.MhFootnote
import io.motohub.android.ui.components.MhListGroup
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhPrimaryButton
import io.motohub.android.ui.components.MhScreen
import io.motohub.android.ui.components.MhSectionHeader
import io.motohub.android.ui.components.MhStatusChip
import io.motohub.android.ui.components.MhTone

// ADV-SOLO, not ADVANCED: ADVANCED is deprecated, and ADV-SOLO replaces both it and this app.
private const val ADVANCED_PACKAGE_NAME = "io.motohub.android.solo"
private const val ADVANCED_RELEASES_URL =
    "https://github.com/vincenzobpt/MOTO-HUB-ADV-SOLO-releases/releases/latest"

/**
 * Whether MOTO-HUB ADV-SOLO is on the phone.
 *
 * Requires the `<package>` entry for [ADVANCED_PACKAGE_NAME] in the manifest's `<queries>`:
 * without it Android hides the package from `getPackageInfo` and every rider - including the
 * ones who already installed ADV-SOLO - would keep being offered the download.
 */
private fun isAdvancedInstalled(context: Context): Boolean =
    runCatching { context.packageManager.getPackageInfo(ADVANCED_PACKAGE_NAME, 0) }.isSuccess

/**
 * Whether ADV-SOLO is installed, rechecked on every resume so it flips by itself when the rider
 * comes back from installing it.
 */
@Composable
private fun rememberAdvancedInstalled(): MutableState<Boolean> {
    val context = LocalContext.current
    val installed = remember { mutableStateOf(isAdvancedInstalled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) installed.value = isAdvancedInstalled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return installed
}

/**
 * Opens ADV-SOLO. Between the resume recheck and this tap it could have been uninstalled, so a
 * missing launcher intent returns false and the caller falls back to the pitch rather than doing
 * nothing at all.
 */
private fun launchAdvanced(context: Context): Boolean {
    val launch = context.packageManager.getLaunchIntentForPackage(ADVANCED_PACKAGE_NAME) ?: return false
    runCatching { context.startActivity(launch) }.onFailure {
        MotoHubSnackbar.error(context, motoHubText("Couldn't open ADV-SOLO"))
    }
    return true
}

/**
 * How Core tells a rider ADV-SOLO exists: the app that replaces it. A bare row - the caller puts it
 * in its own group - on Ride (pairing and connection only, never while connecting or riding) and
 * in Settings, with the same words in both.
 *
 * It pointed at ADVANCED until ADVANCED was deprecated (1.1.119). The names below still say
 * "advanced" because the screen they belong to is the same one; what it offers is ADV-SOLO.
 *
 * One row, two jobs, decided by whether ADV-SOLO is installed: a rider who doesn't have it gets
 * the pitch (the page, then the release page), and a rider who does gets a shortcut that just
 * opens it - being sold an app you already run is the fastest way to make this row feel like an
 * advert to scroll past.
 */
@Composable
fun AdvancedPromoRow(onOpenDetails: () -> Unit) {
    if (BuildConfig.IS_PRO) return
    val context = LocalContext.current
    var installed by rememberAdvancedInstalled()
    MhListRow(
        title = motoHubText("MOTO-HUB ADV-SOLO"),
        subtitle = if (installed) {
            motoHubText("Installed on this phone")
        } else {
            motoHubText("Free app with navigation, trips and a dashboard")
        },
        icon = Icons.Rounded.TwoWheeler,
        onClick = {
            if (!installed || !launchAdvanced(context)) {
                installed = false
                onOpenDetails()
            }
        }
    )
}

/**
 * The pitch, as a screen rather than a dialog.
 *
 * It used to be an [androidx.compose.material3.AlertDialog]: six bullets in a scrolling modal,
 * one link, and no room to say how the two apps relate. A rider deciding whether to install a
 * second app is doing exactly the kind of reading a modal is worst at, so this is a drill-down
 * like every other explanation in the app - reached from the promo row on Ride and from Settings.
 * Calm, not salesy: no edition colour, and Discord and GitHub live in About.
 *
 * Nothing here is capped or fixed-height: every paragraph and every feature description wraps for
 * as long as it needs, in whichever of the nine languages the phone is set to.
 */
@Composable
fun AdvancedPromoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Same resume recheck as the row: a rider who leaves for the release page and comes back
    // installed should find this screen already turned into "open it", not still selling.
    var installed by rememberAdvancedInstalled()

    MhScreen(
        title = motoHubText("MOTO-HUB ADV-SOLO"),
        subtitle = motoHubText("A free app from the same developer that replaces this one."),
        onBack = onBack,
        bottomBar = {
            if (installed) {
                MhPrimaryButton(
                    motoHubText("Open ADV-SOLO"),
                    onClick = { if (!launchAdvanced(context)) installed = false }
                )
            } else {
                MhPrimaryButton(
                    motoHubText("Download ADV-SOLO"),
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ADVANCED_RELEASES_URL)))
                        }.onFailure {
                            MotoHubSnackbar.error(context, motoHubText("Couldn't open the browser"))
                        }
                    }
                )
            }
        }
    ) {
        if (installed) MhStatusChip(motoHubText("Installed"), MhTone.NEUTRAL)
        MhSectionHeader(motoHubText("What it adds"))
        // Not tappable - there is nowhere to go.
        MhListGroup {
            MhListRow(
                title = motoHubText("Ride Dashboard"),
                subtitle = motoHubText("GPS speed, live map and trip stats drawn on the motorcycle's screen."),
                icon = Icons.Rounded.Speed
            )
            MhListRow(
                title = motoHubText("Navigation"),
                subtitle = motoHubText("Search, motorcycle routing, route preview and turn guidance."),
                icon = Icons.Rounded.Route
            )
            MhListRow(
                title = motoHubText("On the route"),
                subtitle = motoHubText("Weather ahead, fuel prices and speed cameras along the way."),
                icon = Icons.Rounded.MyLocation
            )
            MhListRow(
                title = motoHubText("Trips"),
                subtitle = motoHubText("Recording, replay, analysis and GPX export of everything you ride."),
                icon = Icons.Rounded.Schedule
            )
            MhListRow(
                title = motoHubText("Discovery and coaching"),
                subtitle = motoHubText("AI place search while you ride, and a Riding Coach afterwards."),
                icon = Icons.Rounded.Search
            )
            MhListRow(
                title = motoHubText("Group intercom"),
                subtitle = motoHubText("Rider-to-rider voice over the internet, no extra headset box."),
                icon = Icons.Rounded.Mic
            )
        }
        MhSectionHeader(motoHubText("Before you switch"))
        MhFootnote(motoHubText("ADV-SOLO connects to the motorcycle on its own and doesn't need this app."))
        MhFootnote(
            motoHubText(
                "Motorcycles paired here don't move across: pair them again in ADV-SOLO. It can remove " +
                    "MOTO-HUB for you afterwards."
            )
        )
        MhFootnote(motoHubText("No subscription, no account and no extra hardware."))
    }
}
