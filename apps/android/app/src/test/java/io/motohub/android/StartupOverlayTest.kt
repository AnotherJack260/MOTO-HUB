// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android

import io.motohub.android.StartupOverlay.COMPANION
import io.motohub.android.StartupOverlay.CRASH_CONSENT
import io.motohub.android.StartupOverlay.SAFETY
import io.motohub.android.StartupOverlay.UPDATE
import io.motohub.android.StartupOverlay.WIRE_NUDGE
import io.motohub.android.StartupOverlay.WIRE_VERDICT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupOverlayTest {
    @Test
    fun mostImportantGoesFirst() {
        val all = StartupOverlay.entries.toMutableSet()
        // Each one waits for every one above it.
        listOf(SAFETY, COMPANION, CRASH_CONSENT, UPDATE, WIRE_VERDICT, WIRE_NUDGE).forEach { expected ->
            assertEquals(expected, nextOverlay(shown = null, pending = all, modalsOpen = 0))
            all.remove(expected)
        }
        assertNull(nextOverlay(shown = null, pending = all, modalsOpen = 0))
    }

    @Test
    fun theOneOnScreenStaysUntilItCloses() {
        // The crash dialog that turned up while the update sheet was up waits its turn.
        assertEquals(UPDATE, nextOverlay(shown = UPDATE, pending = setOf(CRASH_CONSENT, UPDATE), modalsOpen = 1))
        assertEquals(CRASH_CONSENT, nextOverlay(shown = UPDATE, pending = setOf(CRASH_CONSENT), modalsOpen = 1))
    }

    @Test
    fun startupPromptsWaitForTheRidersOwnSheet() {
        assertNull(nextOverlay(shown = null, pending = setOf(UPDATE, WIRE_VERDICT, WIRE_NUDGE), modalsOpen = 1))
        // A start the rider just asked for does not wait.
        assertEquals(COMPANION, nextOverlay(shown = null, pending = setOf(COMPANION, UPDATE), modalsOpen = 1))
    }
}
