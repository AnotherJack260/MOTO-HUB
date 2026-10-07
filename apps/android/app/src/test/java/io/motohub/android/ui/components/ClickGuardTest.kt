// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ClickGuardTest {
    @Test
    fun aGloveThatLandsTwiceClicksOnce() {
        val guard = ClickGuard(windowMs = 500)
        // The first tap passes even at time 0; the window counts from the last tap let through.
        val passed = listOf(0L, 120L, 499L, 500L, 700L, 999L, 1000L).filter(guard::pass)
        assertEquals(listOf(0L, 500L, 1000L), passed)
    }
}
