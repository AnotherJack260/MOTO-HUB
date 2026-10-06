// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.i18n.motoHubText
import io.motohub.android.session.SessionFailureKind
import io.motohub.android.tbox.TBoxConflictDiagnostics
import io.motohub.android.tbox.TBoxVpnDiagnostics
import io.motohub.android.tbox.WifiGate
import io.motohub.android.ui.components.MhTone

/**
 * How HubViewModel opens the message for a failed join of the dashboard's Wi-Fi. A constant so
 * the mapper below can recognise it; the text is the one the VM always wrote, and it stays
 * untranslated because the exception's own message follows it.
 */
internal const val TBOX_NETWORK_FAILURE_PREFIX = "Unable to connect to the T-Box network"

internal enum class RideFailureKind {
    WIFI_OFF, HOTSPOT_OFF, ANDROID_AUTO_SETUP, VPN, PORT_CONFLICT, NETWORK_NOT_JOINED,
    DASH_NOT_FOUND, NEARBY_PERMISSION, NOTIFICATION_PERMISSION, GENERIC
}

/** The one thing the banner offers to fix it. Never "Try again": the hero button is the retry. */
internal enum class RideFix {
    NONE, WIFI_SETTINGS, HOTSPOT_SETTINGS, PHONE_HOTSPOT, COMPANION_APP_SETTINGS,
    ANDROID_AUTO_HELP, VPN_SETTINGS, APP_SETTINGS
}

/**
 * A connection failure as the Ride tab presents it: a short title, one line, one fix, and the raw
 * message - which logic and the sister app match by identity, so it never changes - folded away
 * behind "Details" ([details], shown as is; null where the title already says everything).
 */
internal data class RideFailure(
    val kind: RideFailureKind,
    val tone: MhTone,
    val title: String,
    val body: String,
    val fix: RideFix = RideFix.NONE,
    val details: String? = null
)

/**
 * Maps a session failure to its presentation. Pure: no Context, so it runs on the JVM, where
 * [motoHubText] falls back to the English literal. The first match wins.
 *
 * Every predicate reads something that does not change with the language: a raw constant, a flag
 * the VM set where it knew the cause, or [failureKind]. Never a translated sentence - the VM
 * outlives an in-app language change, so its message can be in the old language.
 */
internal fun rideFailureOf(
    message: String,
    failureKind: SessionFailureKind?,
    offerPhoneHotspotRetry: Boolean,
    offerOfficialAppHelp: Boolean,
    companionAppName: String?
): RideFailure = when {
    failureKind == SessionFailureKind.NEARBY_PERMISSION -> RideFailure(
        RideFailureKind.NEARBY_PERMISSION,
        MhTone.WARNING,
        motoHubText("Allow nearby devices"),
        motoHubText("MOTO-HUB needs it to find the dashboard's Wi-Fi."),
        RideFix.APP_SETTINGS
    )
    failureKind == SessionFailureKind.NOTIFICATION_PERMISSION -> RideFailure(
        RideFailureKind.NOTIFICATION_PERMISSION,
        MhTone.WARNING,
        motoHubText("Allow notifications"),
        motoHubText("They keep streaming visible and let you stop it."),
        RideFix.APP_SETTINGS
    )
    message == WifiGate.WIFI_OFF_MESSAGE -> RideFailure(
        RideFailureKind.WIFI_OFF,
        MhTone.WARNING,
        motoHubText("Wi-Fi is off"),
        motoHubText("Turn it on, then try again."),
        RideFix.WIFI_SETTINGS
    )
    message == WifiGate.HOTSPOT_OFF_MESSAGE -> RideFailure(
        RideFailureKind.HOTSPOT_OFF,
        MhTone.WARNING,
        motoHubText("Hotspot is off"),
        motoHubText("This dashboard joins your phone's hotspot. Turn it on, then try again."),
        RideFix.HOTSPOT_SETTINGS
    )
    AndroidAutoSelfModeHelp.isMessageAboutSelfMode(message) -> RideFailure(
        RideFailureKind.ANDROID_AUTO_SETUP,
        MhTone.WARNING,
        motoHubText("Android Auto needs a setting"),
        motoHubText("Turn on one option in Android Auto, then start it again."),
        RideFix.ANDROID_AUTO_HELP,
        details = message
    )
    // Before the network family: a VPN failure can arrive with the hotspot offer set, and hosting
    // a hotspot cannot fix a VPN.
    TBoxVpnDiagnostics.isVpnRoutingMessage(message) -> RideFailure(
        RideFailureKind.VPN,
        MhTone.ERROR,
        motoHubText("A VPN is blocking the dashboard"),
        motoHubText("Turn the VPN off while you ride, then try again."),
        RideFix.VPN_SETTINGS,
        details = message
    )
    // Both replacements as well as the raw detector: showError has already swapped the socket
    // error for portConflictMessage(), which isPortConflict() does not recognise.
    TBoxConflictDiagnostics.isPortConflict(message) ||
        message == TBoxConflictDiagnostics.PORT_CONFLICT_MESSAGE ||
        message == TBoxConflictDiagnostics.portConflictMessage(companionAppName) ->
        if (companionAppName.isNullOrBlank()) {
            RideFailure(
                RideFailureKind.PORT_CONFLICT,
                MhTone.ERROR,
                motoHubText("Another app is using the dashboard"),
                motoHubText("Force-stop your motorcycle's companion app, then try again."),
                details = message
            )
        } else {
            RideFailure(
                RideFailureKind.PORT_CONFLICT,
                MhTone.ERROR,
                motoHubText("%1\$s is using the dashboard", companionAppName),
                motoHubText("Force-stop it in its app settings, then try again."),
                RideFix.COMPANION_APP_SETTINGS,
                details = message
            )
        }
    offerPhoneHotspotRetry || message.startsWith(TBOX_NETWORK_FAILURE_PREFIX) -> RideFailure(
        RideFailureKind.NETWORK_NOT_JOINED,
        MhTone.ERROR,
        motoHubText("Couldn't join the dashboard's Wi-Fi"),
        if (offerPhoneHotspotRetry) {
            motoHubText("Check the dashboard shows its pairing screen. Some dashboards join your phone's hotspot instead.")
        } else {
            motoHubText("Check the dashboard shows its pairing screen, then try again.")
        },
        if (offerPhoneHotspotRetry) RideFix.PHONE_HOTSPOT else RideFix.NONE,
        details = message
    )
    // Set only on the discovery-failure path, so this is locale-safe where "T-Box not found: %1$s"
    // (translated at source) would not be.
    offerOfficialAppHelp -> RideFailure(
        RideFailureKind.DASH_NOT_FOUND,
        MhTone.ERROR,
        motoHubText("Dashboard not found"),
        motoHubText("The phone joined the dashboard's Wi-Fi but got no answer."),
        details = message
    )
    else -> RideFailure(
        RideFailureKind.GENERIC,
        MhTone.ERROR,
        motoHubText("Connection failed"),
        motoHubText("Check the dashboard is on, then try again."),
        details = message
    )
}
