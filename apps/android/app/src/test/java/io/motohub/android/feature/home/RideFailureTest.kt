// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.session.SessionFailureKind
import io.motohub.android.tbox.TBoxConflictDiagnostics
import io.motohub.android.tbox.TBoxVpnDiagnostics
import io.motohub.android.tbox.WifiGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideFailureTest {

    private fun map(
        message: String,
        failureKind: SessionFailureKind? = null,
        hotspot: Boolean = false,
        officialApp: Boolean = false,
        companion: String? = null
    ) = rideFailureOf(message, failureKind, hotspot, officialApp, companion)

    private fun assertMaps(failure: RideFailure, kind: RideFailureKind, fix: RideFix) {
        assertEquals(kind, failure.kind)
        assertEquals(fix, failure.fix)
    }

    @Test
    fun `permission failures come from the kind, whatever language the message is in`() {
        assertMaps(
            map("Consenti dispositivi nelle vicinanze", SessionFailureKind.NEARBY_PERMISSION),
            RideFailureKind.NEARBY_PERMISSION,
            RideFix.APP_SETTINGS
        )
        assertMaps(
            map("Consenti le notifiche", SessionFailureKind.NOTIFICATION_PERMISSION),
            RideFailureKind.NOTIFICATION_PERMISSION,
            RideFix.APP_SETTINGS
        )
        // The text alone no longer identifies them.
        assertMaps(
            map("Allow Nearby devices and Location to detect the T-Box Wi-Fi network."),
            RideFailureKind.GENERIC,
            RideFix.NONE
        )
    }

    @Test
    fun `phone radios off open their settings and hide no details`() {
        val wifi = map(WifiGate.WIFI_OFF_MESSAGE)
        assertMaps(wifi, RideFailureKind.WIFI_OFF, RideFix.WIFI_SETTINGS)
        assertNull(wifi.details)
        assertMaps(map(WifiGate.HOTSPOT_OFF_MESSAGE), RideFailureKind.HOTSPOT_OFF, RideFix.HOTSPOT_SETTINGS)
    }

    @Test
    fun `android auto self mode points at the guide`() {
        val failure = map(AndroidAutoSelfModeHelp.NEVER_CONNECTED_MESSAGE)
        assertMaps(failure, RideFailureKind.ANDROID_AUTO_SETUP, RideFix.ANDROID_AUTO_HELP)
        assertEquals(AndroidAutoSelfModeHelp.NEVER_CONNECTED_MESSAGE, failure.details)
    }

    @Test
    fun `a vpn wins over the hotspot offer`() {
        assertMaps(
            map(TBoxVpnDiagnostics.lockdownMessage(null), hotspot = true),
            RideFailureKind.VPN,
            RideFix.VPN_SETTINGS
        )
    }

    @Test
    fun `port conflict is recognised after showError replaced the socket error`() {
        val named = map(TBoxConflictDiagnostics.portConflictMessage("Zontes Smart"), companion = "Zontes Smart")
        assertMaps(named, RideFailureKind.PORT_CONFLICT, RideFix.COMPANION_APP_SETTINGS)
        assertEquals("Zontes Smart is using the dashboard", named.title)

        assertMaps(map(TBoxConflictDiagnostics.PORT_CONFLICT_MESSAGE), RideFailureKind.PORT_CONFLICT, RideFix.NONE)
        assertMaps(
            map("bind failed: port 10920 address already in use"),
            RideFailureKind.PORT_CONFLICT,
            RideFix.NONE
        )
    }

    @Test
    fun `a failed join offers the phone hotspot only when the vm does`() {
        assertMaps(
            map("$TBOX_NETWORK_FAILURE_PREFIX: timeout"),
            RideFailureKind.NETWORK_NOT_JOINED,
            RideFix.NONE
        )
        assertMaps(map("anything", hotspot = true), RideFailureKind.NETWORK_NOT_JOINED, RideFix.PHONE_HOTSPOT)
    }

    @Test
    fun `discovery failure is dashboard not found`() {
        val failure = map("T-Box non trovato: timeout", officialApp = true, companion = "Zontes Smart")
        assertMaps(failure, RideFailureKind.DASH_NOT_FOUND, RideFix.NONE)
        assertEquals("T-Box non trovato: timeout", failure.details)
    }

    @Test
    fun `anything else is a generic connection failure with the raw text behind details`() {
        val failure = map("Screen capture did not start: x")
        assertMaps(failure, RideFailureKind.GENERIC, RideFix.NONE)
        assertEquals("Screen capture did not start: x", failure.details)
    }
}
