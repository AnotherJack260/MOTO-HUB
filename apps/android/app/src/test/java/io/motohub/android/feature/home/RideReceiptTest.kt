// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class RideReceiptTest {

    private val minute = 60_000L

    @Test
    fun `under a minute, minutes, then hours and minutes`() {
        assertEquals("<1 min", rideDuration(0))
        assertEquals("<1 min", rideDuration(minute - 1))
        assertEquals("1 min", rideDuration(minute))
        assertEquals("59 min", rideDuration(60 * minute - 1))
        assertEquals("1 h 0 min", rideDuration(60 * minute))
        assertEquals("2 h 5 min", rideDuration(125 * minute + 30_000))
    }

    @Test
    fun `the receipt names the mode and the time`() {
        assertEquals("Stopped · Mirroring · 42 min", rideReceipt("Mirroring", 42 * minute))
    }
}
