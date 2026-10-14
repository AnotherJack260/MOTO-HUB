// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.encoding

import android.os.PowerManager.THERMAL_STATUS_LIGHT
import android.os.PowerManager.THERMAL_STATUS_MODERATE
import android.os.PowerManager.THERMAL_STATUS_NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class AutoImagePolicyTest {
    /** The owner's dash on Automatic: Smoother 1.75, Balanced 2.5, Sharper 4.0 Mbps. */
    private val owner = AutoImageRange(floor = 1_750_000, start = 2_500_000, ceiling = 4_000_000)
    private val lossy = AdaptiveVideoPolicy.DROP_THRESHOLD

    private fun automatic(range: AutoImageRange = owner) = AdaptiveVideoLoop().apply { reset(range) }

    private fun AdaptiveVideoLoop.run(
        lost: Int = 0,
        thermal: Int = THERMAL_STATUS_NONE,
        fixedBitrate: Int = 4_000_000
    ): AdaptiveVideoDecision = tick(
        fixedBitrate = fixedBitrate,
        baseFrameRate = 30,
        thermalStatus = thermal,
        frameRateThermalStatus = thermal,
        lostFrames = lost
    )

    private fun AdaptiveVideoLoop.clean(ticks: Int) = repeat(ticks) { run() }

    private val AdaptiveVideoLoop.target get() = auto!!.target

    @Test
    fun `Automatic starts at Balanced with the cap at Sharper`() {
        val loop = automatic()
        assertEquals(AutoImageState(target = 2_500_000, cap = 4_000_000), loop.auto)
        assertEquals(2_500_000, loop.run().bitrate)
    }

    @Test
    fun `the start is clamped into the range and the dash's ceiling wins`() {
        assertEquals(4_000_000, automatic(AutoImageRange(1_750_000, 5_000_000, 4_000_000)).target)
        assertEquals(1_750_000, automatic(AutoImageRange(1_750_000, 1_000_000, 4_000_000)).target)
        // A dash cap under Smoother's bitrate: everything sits at the cap.
        val capped = automatic(AutoImageRange(1_750_000, 2_500_000, 1_500_000))
        assertEquals(AutoImageRange(1_500_000, 1_500_000, 1_500_000), capped.range)
        assertEquals(AutoImageState(target = 1_500_000, cap = 1_500_000), capped.auto)
    }

    @Test
    fun `six clean ticks give one step up`() {
        val loop = automatic()
        loop.clean(5)
        assertEquals(2_500_000, loop.target)
        loop.clean(1)
        assertEquals(2_750_000, loop.target)
        assertEquals(0, loop.auto!!.cleanTicks)
    }

    @Test
    fun `a step up never passes the cap`() {
        val next = AutoImagePolicy.next(
            owner,
            AutoImageState(target = 3_900_000, cap = 4_000_000, cleanTicks = 5),
            stall = false, lostFrames = 0, thermalStatus = THERMAL_STATUS_NONE, linkFactor = 1f
        )
        assertEquals(4_000_000, next.target)
    }

    @Test
    fun `the owner's range reaches Sharper in 36 clean ticks and stays there`() {
        val loop = automatic()
        loop.clean(35)
        assertEquals(3_750_000, loop.target)
        loop.clean(1)
        assertEquals(4_000_000, loop.target)
        loop.clean(100)
        assertEquals(4_000_000, loop.target)
        assertEquals(4_000_000, loop.run().bitrate)
    }

    @Test
    fun `a stall steps down by a fifth, caps one step under the failed level and holds 60 s`() {
        val loop = automatic()
        loop.clean(24)
        assertEquals(3_500_000, loop.target)

        loop.noteStall()
        assertEquals(2_800_000, loop.run().bitrate)
        assertEquals(AutoImageState(target = 2_800_000, cap = 3_250_000, holdTicks = 12), loop.auto)

        // Twelve clean ticks of hold, with nothing counted toward a step up.
        loop.clean(12)
        assertEquals(AutoImageState(target = 2_800_000, cap = 3_250_000), loop.auto)
        loop.clean(6)
        assertEquals(3_050_000, loop.target)
        loop.clean(6)
        assertEquals(3_250_000, loop.target)
        // The level that stalled is never tried again on this encoder.
        loop.clean(60)
        assertEquals(3_250_000, loop.target)
    }

    @Test
    fun `a stall during the hold still steps down`() {
        val loop = automatic()
        loop.noteStall()
        loop.run()
        loop.clean(3)
        loop.noteStall()
        loop.run()
        assertEquals(1_750_000, loop.target) // 2.0 * 0.8 = 1.6, held up by the floor
        assertEquals(12, loop.auto!!.holdTicks)
    }

    @Test
    fun `one lossy tick leaves the target to the link factor`() {
        val loop = automatic()
        assertEquals(2_000_000, loop.run(lost = lossy).bitrate)
        assertEquals(2_500_000, loop.target)
        assertEquals(1, loop.auto!!.lossTicks)
        loop.run()
        assertEquals(0, loop.auto!!.lossTicks)
    }

    @Test
    fun `two lossy ticks step down without cutting twice`() {
        val loop = automatic()
        val today = AdaptiveVideoLoop().apply { reset(null) }
        loop.run(lost = lossy)
        today.run(lost = lossy, fixedBitrate = 2_500_000)

        val stepped = loop.run(lost = lossy)
        val fixed = today.run(lost = lossy, fixedBitrate = 2_500_000)
        assertEquals(2_000_000, loop.target)
        assertEquals(1_600_000, fixed.bitrate) // today's 0.64 x target
        assertEquals(fixed.bitrate, stepped.bitrate)
        assertEquals(0.8f, loop.linkFactor, 0.0001f)
        assertEquals(0, loop.auto!!.lossTicks)
    }

    @Test
    fun `a stall and a lossy tick together cut a fifth, not a third`() {
        val loop = automatic()
        loop.noteStall()
        val decision = loop.run(lost = lossy)
        assertEquals(2_000_000, decision.bitrate)
        assertEquals(1f, loop.linkFactor, 0.0001f)
    }

    @Test
    fun `the loss count restarts after a step down`() {
        val loop = automatic()
        loop.run(lost = lossy)
        loop.run(lost = lossy)
        assertEquals(2_000_000, loop.target)
        loop.run(lost = lossy)
        assertEquals(2_000_000, loop.target)
        assertEquals(1, loop.auto!!.lossTicks)
        loop.run(lost = lossy)
        assertEquals(1_750_000, loop.target)
    }

    @Test
    fun `a tick is clean only once this tick's link update brings the link back to full`() {
        val loop = automatic()
        loop.run(lost = lossy) // link 0.8
        loop.clean(3) // 0.85, 0.9, 0.95: none of them clean
        assertEquals(0, loop.auto!!.cleanTicks)
        loop.clean(1) // the update brings it to 1.0, so this one counts
        assertEquals(1, loop.auto!!.cleanTicks)
    }

    @Test
    fun `the floor holds`() {
        val loop = automatic()
        repeat(10) {
            loop.noteStall()
            loop.run()
        }
        assertEquals(1_750_000, loop.target)
        assertEquals(1_750_000, loop.auto!!.cap)
        assertEquals(1_750_000, loop.run().bitrate)
    }

    @Test
    fun `moderate heat blocks the step up, light heat does not`() {
        val hot = automatic()
        repeat(12) { hot.run(thermal = THERMAL_STATUS_MODERATE) }
        assertEquals(2_500_000, hot.target)

        val warm = automatic()
        repeat(6) { warm.run(thermal = THERMAL_STATUS_LIGHT) }
        assertEquals(2_750_000, warm.target)
    }

    @Test
    fun `an outage changes nothing, a stall raised in it included`() {
        val loop = automatic()
        loop.clean(3)
        val before = loop.auto
        val link = loop.linkFactor
        loop.noteStall()
        loop.absorbOutage()
        assertEquals(before, loop.auto)
        assertEquals(link, loop.linkFactor)
        loop.run()
        assertEquals(2_500_000, loop.target)
        assertEquals(4, loop.auto!!.cleanTicks)
    }

    @Test
    fun `a refused bitrate change freezes Automatic and is not recorded as applied`() {
        val loop = automatic()
        val first = loop.run()
        assertTrue(loop.bitrateApplied(first.bitrate, accepted = false))
        assertTrue(loop.frozen)
        assertEquals(-1, loop.appliedBitrate)
        // Frozen: a stall and a long clean spell move nothing.
        loop.noteStall()
        loop.run()
        loop.clean(40)
        assertEquals(2_500_000, loop.target)
        // It says so once.
        assertFalse(loop.bitrateApplied(first.bitrate, accepted = false))
    }

    @Test
    fun `an accepted change is recorded, and a fixed image records whatever the codec said`() {
        val loop = automatic()
        assertFalse(loop.bitrateApplied(2_500_000, accepted = true))
        assertEquals(2_500_000, loop.appliedBitrate)

        val fixed = AdaptiveVideoLoop().apply { reset(null) }
        assertFalse(fixed.bitrateApplied(4_000_000, accepted = false))
        assertEquals(4_000_000, fixed.appliedBitrate)
        assertFalse(fixed.frozen)
    }

    @Test
    fun `a lower Carbit rate lowers the ceiling, the cap and the target, and never raises them`() {
        val loop = automatic()
        loop.clean(36)
        assertEquals(4_000_000, loop.target)

        loop.updateCeiling(3_000_000)
        assertEquals(3_000_000, loop.range!!.ceiling)
        assertEquals(AutoImageState(target = 3_000_000, cap = 3_000_000), loop.auto)
        assertEquals(3_000_000, loop.run().bitrate)

        loop.updateCeiling(5_000_000)
        loop.updateCeiling(0)
        assertEquals(3_000_000, loop.range!!.ceiling)
        assertEquals(3_000_000, loop.auto!!.cap)
        loop.clean(60)
        assertEquals(3_000_000, loop.target)

        // Under the floor: the dash's limit wins over Smoother's bitrate.
        loop.updateCeiling(1_000_000)
        assertEquals(1_000_000, loop.target)
        assertEquals(1_000_000, loop.auto!!.cap)
        assertEquals(1_000_000, loop.range!!.floor)
    }

    @Test
    fun `Automatic never exceeds the dash's cap on any trace`() {
        val loop = automatic()
        val lostTrace = listOf(0, 0, 9, 9, 0, 3, 0, 0, 12, 0) + List(200) { 0 }
        lostTrace.forEachIndexed { i, lost ->
            if (i == 40) loop.noteStall()
            val decision = loop.run(lost = lost)
            assertTrue(decision.bitrate <= owner.ceiling)
            assertTrue(loop.target <= loop.auto!!.cap)
            assertTrue(loop.auto!!.cap <= owner.ceiling)
            assertTrue(loop.target >= owner.floor)
        }
    }

    @Test
    fun `a fixed image keeps today's decisions exactly and ignores stalls`() {
        listOf(4_000_000, 1_750_000).forEach { configured -> // Sharper, Smoother
            val loop = AdaptiveVideoLoop().apply { reset(null) }
            var previousLink = AdaptiveVideoPolicy.LINK_MAX
            listOf(0, lossy, lossy, 0, 0, lossy, 0, 0, 0, 0, 0, 0, 0, 0).forEachIndexed { i, lost ->
                if (i % 3 == 0) loop.noteStall()
                val today = AdaptiveVideoPolicy.decide(
                    baseBitrate = configured,
                    baseFrameRate = 30,
                    thermalStatus = THERMAL_STATUS_NONE,
                    previousLinkFactor = previousLink,
                    lostFrames = lost
                )
                previousLink = today.linkFactor
                assertEquals(today, loop.run(lost = lost, fixedBitrate = configured))
                assertNull(loop.auto)
            }
        }
    }

    @Test
    fun `heat and link combine as the minimum on Automatic, tick by tick`() {
        val loop = automatic()
        // 30 s clean but warm enough to block the climb, then loss on a moderate phone.
        val trace = List(6) { 0 } + listOf(lossy, 0, lossy, lossy, 0, 0, 0, 0, 0, 0)
        trace.forEach { lost ->
            val decision = loop.run(lost = lost, thermal = THERMAL_STATUS_MODERATE)
            val target = loop.target
            val expected = (target * min(0.8f, loop.linkFactor)).toInt()
                .coerceIn(AdaptiveVideoPolicy.MIN_BITRATE, target)
            assertEquals(expected, decision.bitrate)
            assertEquals(min(20, AdaptiveVideoPolicy.linkFrameRateCap(30, loop.linkFactor)), decision.frameRate)
        }
        assertEquals(2_000_000, loop.target) // one step down, from the two lossy ticks in a row
    }
}
