// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class MotorcycleScopeTest {

    @Test
    fun `reads prefer the scoped key and fall back to the global one`() {
        val stored = setOf("mapping#bikeA")

        // The scoped bike reads its own value.
        assertEquals("mapping#bikeA", MotorcycleScope.readKey("mapping", "bikeA", stored::contains))
        // Another bike with no scoped value falls back to the global key: existing
        // single-bike settings become every bike's default, nothing is lost on upgrade.
        assertEquals("mapping", MotorcycleScope.readKey("mapping", "bikeB", stored::contains))
        // No motorcycle known at all: plain global keys, exactly like before scoping.
        assertEquals("mapping", MotorcycleScope.readKey("mapping", null, stored::contains))
    }

    @Test
    fun `writes always target the scoped key when a motorcycle is known`() {
        assertEquals("mapping#bikeA", MotorcycleScope.writeKeyFor("mapping", "bikeA"))
        assertEquals("mapping", MotorcycleScope.writeKeyFor("mapping", null))
    }

    @Test
    fun `scoped keys cannot collide with plain base keys`() {
        // '#' never appears in gesture/press ids, so a scoped key is never a valid base key.
        assertEquals("volumeUp#id", MotorcycleScope.scopedKey("volumeUp", "id"))
        HandlebarGesture.entries.forEach { gesture -> assert(!gesture.id.contains('#')) }
        PhysicalPress.entries.forEach { press -> assert(!press.id.contains('#')) }
    }

    @Test
    fun `an explicit motorcycle wins over the live session and the garage`() {
        // The Garage editing a motorcycle that is not the live one must write to THAT motorcycle.
        assertEquals("edited", MotorcycleScope.suffixFor("edited", live = { "live" }, active = { "active" }))
        assertEquals("live", MotorcycleScope.suffixFor(null, live = { "live" }, active = { "active" }))
        assertEquals("active", MotorcycleScope.suffixFor(null, live = { null }, active = { "active" }))
        assertEquals(null, MotorcycleScope.suffixFor(null, live = { null }, active = { null }))
    }

    @Test
    fun `forgetting a motorcycle matches only its own scoped keys`() {
        val keys = setOf("enabled", "enabled#abc", "volumeUp#abc", "enabled#xabc", "enabled#abcd", "abc")

        assertEquals(setOf("enabled#abc", "volumeUp#abc"), MotorcycleScope.keysOf(keys, "abc").toSet())
    }

    /**
     * MotorcycleScope reads the garage's active-profile id straight from its preferences
     * (decoding the full profile store per button press would be far too heavy), so these
     * names must stay in lockstep with MotorcycleProfileStore's private constants.
     */
    @Test
    fun `garage preference names match MotorcycleProfileStore`() {
        assertEquals("motorcycle_profiles", MotorcycleScope.GARAGE_PREFERENCES)
        assertEquals("active_id", MotorcycleScope.GARAGE_ACTIVE_ID)
    }
}
