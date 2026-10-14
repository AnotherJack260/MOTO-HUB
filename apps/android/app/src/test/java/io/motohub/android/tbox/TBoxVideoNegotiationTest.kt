// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.tbox

import io.motohub.android.encoding.AutoImageRange
import io.motohub.android.encoding.EncoderProfile
import io.motohub.android.feature.settings.ImageQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TBoxVideoNegotiationTest {
    @Test
    fun `captures an area emitted synchronously while transport starts`() = runBlocking {
        val transport = FakeTransport(TBoxEvent.VideoArea(720, 712))

        val result = transport.negotiateVideoConfiguration(HOST, null, 100)

        assertEquals(
            TBoxVideoConfiguration(
                rawArea = TBoxEvent.VideoArea(720, 712),
                encoderProfile = EncoderProfile(720, 704),
                source = TBoxVideoAreaSource.LIVE
            ),
            result.getOrThrow()
        )
    }

    @Test
    fun `uses saved geometry only when the live area times out`() = runBlocking {
        val transport = FakeTransport(null)

        val result = transport.negotiateVideoConfiguration(
            HOST,
            savedArea = TBoxEvent.VideoArea(1024, 601),
            videoAreaTimeoutMillis = 10
        )

        assertEquals(TBoxVideoAreaSource.SAVED, result.getOrThrow().source)
        assertEquals(EncoderProfile(1024, 592), result.getOrThrow().encoderProfile)
    }

    @Test
    fun `uses the validated fallback when live and saved geometry are unavailable`() = runBlocking {
        val transport = FakeTransport(null)

        val result = transport.negotiateVideoConfiguration(
            HOST,
            savedArea = null,
            videoAreaTimeoutMillis = 10,
            fallbackArea = TBoxEvent.VideoArea(800, 480)
        )

        assertEquals(TBoxVideoAreaSource.FALLBACK, result.getOrThrow().source)
        assertEquals(TBoxEvent.VideoArea(800, 480, isFallback = true), result.getOrThrow().rawArea)
        assertEquals(EncoderProfile(800, 480), result.getOrThrow().encoderProfile)
    }

    @Test
    fun `fails instead of inventing dimensions when no geometry exists`() = runBlocking {
        val transport = FakeTransport(null)

        val result = transport.negotiateVideoConfiguration(HOST, null, 10)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("no saved or fallback geometry"))
    }

    @Test
    fun `the encoder follows Carbit's plan without outrunning Android Auto's frame rate`() {
        // The Zontes 350D polls (wantFps 0) and names no bitrate: Carbit plans 36 fps at 3 Mbps.
        val plan = CarbitVideoPlan(1024, 464, 3_000_000, 36, 3, cbr = false, landscape = true, codec = 2)
        val base = EncoderProfile(width = 1024, height = 464, frameRate = 30, bitRate = 2_500_000)

        // The bitrate is left to imageBitrates, which takes the plan's rate as its base and cap.
        assertEquals(
            EncoderProfile(
                width = 1024,
                height = 464,
                frameRate = 30,
                bitRate = 2_500_000,
                keyframeIntervalSeconds = 3,
                plainGopWithoutIntraRefresh = true,
                variableBitrate = true
            ),
            base.followingCarbitPlan(plan)
        )
        // A slower dash keeps its own rate, and CBR only when it asked.
        val slow = base.followingCarbitPlan(plan.copy(fps = 20, cbr = true))
        assertEquals(20, slow.frameRate)
        assertEquals(false, slow.variableBitrate)
    }

    @Test
    fun `image bitrates scale the base and never pass the dash's cap`() {
        // Under a Carbit plan the plan's rate is base and cap: Balanced is the plan, Sharper
        // cannot raise it, Smoother still lowers it (the three numbers the plan test used to pin).
        assertEquals(ImageBitrates(2_100_000, 3_000_000, 3_000_000), imageBitrates(3_000_000, 3_000_000))
        // The owner's Zontes: 2.5 Mbps negotiated, the dash asks 8,388,608, so Sharper stays 4 Mbps.
        assertEquals(ImageBitrates(1_750_000, 2_500_000, 4_000_000), imageBitrates(2_500_000, 8_388_608))
        assertEquals(ImageBitrates(1_750_000, 2_500_000, 4_000_000), imageBitrates(2_500_000, null))
        // A dash asking for less than the floor gets exactly what it asked, for all three.
        assertEquals(ImageBitrates(1_000_000, 1_000_000, 1_000_000), imageBitrates(2_500_000, 1_000_000))
    }

    @Test
    fun `a fixed image configures its end of the range and Automatic starts at Balanced`() {
        val base = EncoderProfile(1024, 464, keyframeIntervalSeconds = 3, plainGopWithoutIntraRefresh = true, variableBitrate = true)
        val rates = imageBitrates(2_500_000, 8_388_608)

        // The owner's Sharper session is today's encoder exactly: 4 Mbps, no ceiling, no range.
        val (sharper, fixed) = base.forImage(ImageQuality.SHARPER, rates, plan = null, keyframeOverride = null)
        assertEquals(base.copy(bitRate = 4_000_000), sharper)
        assertEquals(null, fixed)
        assertEquals(1_750_000, base.forImage(ImageQuality.SMOOTHER, rates, null, null).first.bitRate)

        val (automatic, range) = base.forImage(ImageQuality.AUTOMATIC, rates, null, null)
        assertEquals(base.copy(bitRate = 2_500_000, maxBitRate = 4_000_000), automatic)
        assertEquals(AutoImageRange(floor = 1_750_000, start = 2_500_000, ceiling = 4_000_000), range)
    }

    @Test
    fun `the rider's I-frame interval wins over the Carbit plan's GOP`() {
        val plan = CarbitVideoPlan(1024, 464, 3_000_000, 36, 3, cbr = false, landscape = true, codec = 2)
        val base = EncoderProfile(width = 1024, height = 464, keyframeIntervalSeconds = 2)
        val rates = imageBitrates(plan.bitRate, plan.bitRate)

        assertEquals(3, base.forImage(ImageQuality.SHARPER, rates, plan, keyframeOverride = null).first.keyframeIntervalSeconds)
        assertEquals(1, base.forImage(ImageQuality.SHARPER, rates, plan, keyframeOverride = 1).first.keyframeIntervalSeconds)
        // 0 is "Every frame": all-intra.
        assertEquals(0, base.forImage(ImageQuality.SHARPER, rates, null, keyframeOverride = 0).first.keyframeIntervalSeconds)
        assertEquals(3_000_000, base.forImage(ImageQuality.SHARPER, rates, plan, null).first.bitRate)
    }

    @Test
    fun `only a new picture shape restarts the encoder`() {
        val plan = CarbitVideoPlan(1024, 464, 3_000_000, 36, 3, cbr = false, landscape = true, codec = 2)

        assertEquals(false, plan.copy(bitRate = 6_000_000, fps = 20).needsNewEncoder(plan))
        assertEquals(true, plan.copy(width = 800).needsNewEncoder(plan))
        assertEquals(true, plan.copy(landscape = false).needsNewEncoder(plan))
        assertEquals(true, plan.needsNewEncoder(null))
    }

    private class FakeTransport(
        private val areaOnStart: TBoxEvent.VideoArea?
    ) : TBoxTransport {
        private val mutableEvents = MutableSharedFlow<TBoxEvent>()
        override val events: Flow<TBoxEvent> = mutableEvents.asSharedFlow()

        override suspend fun discover(link: TBoxLink, expectedModelId: String?): Result<TBoxHost> =
            Result.success(HOST)

        override suspend fun start(host: TBoxHost): Result<Unit> {
            areaOnStart?.let { mutableEvents.emit(it) }
            return Result.success(Unit)
        }

        override fun offerAccessUnit(avcc: ByteArray): Boolean = true

        override suspend fun stop() = Unit
    }

    private companion object {
        val HOST = TBoxHost("192.168.43.1", 10930, "ECARX")
    }
}
