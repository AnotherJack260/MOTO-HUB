// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.pairing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectThrottleTest {
    @Test
    fun sameCodeHeldInViewBuzzesOnce() {
        val throttle = RejectThrottle()
        assertTrue(throttle.shouldBuzz("WIFI:S:vehicle-info", 1_000))
        assertFalse(throttle.shouldBuzz("WIFI:S:vehicle-info", 1_100))
        // Still in view frame after frame: each sighting keeps it quiet, however long it lasts.
        assertFalse(throttle.shouldBuzz("WIFI:S:vehicle-info", 2_900))
        assertFalse(throttle.shouldBuzz("WIFI:S:vehicle-info", 4_500))
    }

    @Test
    fun differentCodeBuzzesAtOnce() {
        val throttle = RejectThrottle()
        assertTrue(throttle.shouldBuzz("https://example.com", 1_000))
        assertTrue(throttle.shouldBuzz("WIFI:S:vehicle-info", 1_050))
    }

    @Test
    fun sameCodeBuzzesAgainAfterTwoSecondsOutOfSight() {
        val throttle = RejectThrottle()
        assertTrue(throttle.shouldBuzz("https://example.com", 1_000))
        assertFalse(throttle.shouldBuzz("https://example.com", 2_999))
        assertTrue(throttle.shouldBuzz("https://example.com", 4_999))
    }
}
