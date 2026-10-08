// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.encoding

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacingTest {
    @Test
    fun `a jittery source just under the cap keeps every frame`() {
        // Android Auto on the Zontes: ~29.5 fps out of the decoder under a 30 cap. The strict
        // interval test this replaced passed about two frames in three (rider log, 2026-10-08).
        assertTrue(passRate(sourceFps = 29.5, frameCap = 30, jitterMs = 4.0) >= 0.99)
    }

    @Test
    fun `a source twice the cap is halved`() {
        assertEquals(0.5, passRate(sourceFps = 60.0, frameCap = 30, jitterMs = 1.0), 0.02)
    }

    @Test
    fun `the jitter tolerance never lets a faster source past the cap`() {
        // Balanced's 24 fps over a 30 fps source. Measuring the tolerance from the last frame
        // instead of from its slot passed ~88% of these: 26 fps under a 24 cap.
        assertTrue(passRate(sourceFps = 30.0, frameCap = 24, jitterMs = 2.0) <= 24.0 / 30.0)
    }

    private fun passRate(sourceFps: Double, frameCap: Int, jitterMs: Double): Double {
        val random = Random(7)
        var deadline = 0L
        var passed = 0
        repeat(FRAMES) { k ->
            val now = 1_000_000_000L +
                (k * 1e9 / sourceFps + random.nextDouble(-jitterMs, jitterMs) * 1e6).toLong()
            if (frameDue(now, deadline, frameCap)) {
                passed++
                deadline = nextFrameDeadline(now, deadline, frameCap)
            }
        }
        return passed.toDouble() / FRAMES
    }

    private companion object {
        const val FRAMES = 3_000
    }
}
