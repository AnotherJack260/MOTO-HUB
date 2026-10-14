// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.settings

import io.motohub.android.androidauto.AndroidAutoVideoPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidAutoSizeTest {
    private val auto = AndroidAutoResolutionMode.AUTO
    private val autoDpi = AndroidAutoDensityMode.AUTO

    @Test
    fun `each size resolves for both orientations`() {
        val landscape = false
        val portrait = true
        assertEquals(AndroidAutoVideoPreset.LANDSCAPE_1920X1080 to 240, AndroidAutoSize.COMPACT.resolve(landscape, auto, autoDpi))
        assertEquals(AndroidAutoVideoPreset.PORTRAIT_1080X1920 to 240, AndroidAutoSize.COMPACT.resolve(portrait, auto, autoDpi))
        // Standard is exactly the old AUTO resolution at AUTO density.
        assertEquals(null to null, AndroidAutoSize.STANDARD.resolve(landscape, auto, autoDpi))
        assertEquals(null to null, AndroidAutoSize.STANDARD.resolve(portrait, auto, autoDpi))
        assertEquals(AndroidAutoVideoPreset.LANDSCAPE_1280X720 to 320, AndroidAutoSize.LARGE.resolve(landscape, auto, autoDpi))
        assertEquals(AndroidAutoVideoPreset.PORTRAIT_720X1280 to 320, AndroidAutoSize.LARGE.resolve(portrait, auto, autoDpi))
        // Custom replays the stored pair whatever the dashboard's shape, as before.
        val fhd = AndroidAutoResolutionMode.LANDSCAPE_FHD
        val dpi240 = AndroidAutoDensityMode.DPI_240
        assertEquals(AndroidAutoVideoPreset.LANDSCAPE_1920X1080 to 240, AndroidAutoSize.CUSTOM.resolve(landscape, fhd, dpi240))
        assertEquals(AndroidAutoVideoPreset.LANDSCAPE_1920X1080 to 240, AndroidAutoSize.CUSTOM.resolve(portrait, fhd, dpi240))
        assertEquals(null to null, AndroidAutoSize.CUSTOM.resolve(landscape, auto, autoDpi))
    }

    @Test
    fun `a motorcycle with no size reads it from its stored resolution and density`() {
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromStoredPair(null, null))
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromStoredPair("AUTO", "AUTO"))
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromStoredPair("AUTO", null))
        // A retired QHD rider gets today's AUTO, not 800x480 at 320 dpi.
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromStoredPair("LANDSCAPE_QHD", "DPI_320"))
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromStoredPair("PORTRAIT_QHD", null))
        // The owner's pair without a migration: Custom, which replays FHD at 240.
        assertEquals(AndroidAutoSize.CUSTOM, AndroidAutoSize.fromStoredPair("LANDSCAPE_FHD", "DPI_240"))
        assertEquals(AndroidAutoSize.CUSTOM, AndroidAutoSize.fromStoredPair("AUTO", "DPI_320"))
    }

    @Test
    fun `every named size round-trips through its stored pair on a proven dashboard`() {
        listOf(AndroidAutoSize.COMPACT, AndroidAutoSize.LARGE).forEach { size ->
            listOf(false, true).forEach { portrait ->
                val (preset, dpi) = size.resolve(portrait, auto, autoDpi)
                val resolution = AndroidAutoResolutionMode.entries.single { it.preset == preset }
                val density = AndroidAutoDensityMode.entries.single { it.dpi == dpi }
                assertEquals(size, AndroidAutoSize.fromLegacy(resolution.name, density.name, portrait))
                // The same pair on a dashboard of the other shape is not that size: Custom keeps it.
                assertEquals(AndroidAutoSize.CUSTOM, AndroidAutoSize.fromLegacy(resolution.name, density.name, !portrait))
                // And with the orientation unproven, neither.
                assertEquals(AndroidAutoSize.CUSTOM, AndroidAutoSize.fromLegacy(resolution.name, density.name, null))
            }
        }
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromLegacy("AUTO", "AUTO", false))
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromLegacy(null, null, null))
        assertEquals(AndroidAutoSize.STANDARD, AndroidAutoSize.fromLegacy("LANDSCAPE_QHD", "DPI_320", false))
    }

    @Test
    fun `the owner's legacy values become Compact and resolve to the same call`() {
        val legacy = AndroidAutoSize.CUSTOM.resolve(
            false,
            AndroidAutoResolutionMode.LANDSCAPE_FHD,
            AndroidAutoDensityMode.DPI_240
        )
        val migrated = AndroidAutoSize.fromLegacy("LANDSCAPE_FHD", "DPI_240", portrait = false)

        assertEquals(AndroidAutoSize.COMPACT, migrated)
        assertEquals(legacy, migrated.resolve(false, auto, autoDpi))
    }

    @Test
    fun `the old video quality maps onto image`() {
        assertEquals(ImageQuality.SHARPER, ImageQuality.fromLegacy("SHARPER"))
        assertEquals(ImageQuality.SMOOTHER, ImageQuality.fromLegacy("SMOOTHER"))
        assertEquals(ImageQuality.AUTOMATIC, ImageQuality.fromLegacy("BALANCED"))
        assertEquals(ImageQuality.AUTOMATIC, ImageQuality.fromLegacy(null))
        assertEquals(ImageQuality.AUTOMATIC, ImageQuality.fromLegacy("NONSENSE"))
    }
}
