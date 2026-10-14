// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.tbox

import io.motohub.android.encoding.AutoImageRange
import io.motohub.android.encoding.EncoderProfile
import io.motohub.android.feature.settings.ImageQuality
import io.motohub.android.feature.settings.VideoQuality
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

enum class TBoxVideoAreaSource {
    LIVE,
    SAVED,
    FALLBACK
}

data class TBoxVideoConfiguration(
    val rawArea: TBoxEvent.VideoArea,
    val encoderProfile: EncoderProfile,
    val source: TBoxVideoAreaSource
)

/**
 * Starts EasyConn while already listening for the runtime TFT capture dimensions.
 *
 * [videoAreaTimeoutMillis] bounds ONLY the wait for the live [TBoxEvent.VideoArea]; it does not
 * and cannot bound [start], which blocks in the native session until Go's own
 * `StartupTimeoutSec` gives up. The name it used to carry - `timeoutMillis` - read like the
 * handshake budget and was quoted as one: rider 94b0a3da (2026-08-30) waited 25.037 s on a dash
 * that never answered, in a call site that believed it had capped him at 10 s. The real budget is
 * `RIDE_DAEMON_STARTUP_TIMEOUT_SEC`, and the "Starting EasyConn handshake" line names it.
 */
suspend fun TBoxTransport.negotiateVideoConfiguration(
    host: TBoxHost,
    savedArea: TBoxEvent.VideoArea?,
    videoAreaTimeoutMillis: Long,
    fallbackArea: TBoxEvent.VideoArea? = null
): Result<TBoxVideoConfiguration> = coroutineScope {
    val liveArea = async(start = CoroutineStart.UNDISPATCHED) {
        withTimeoutOrNull(videoAreaTimeoutMillis) {
            events.filterIsInstance<TBoxEvent.VideoArea>().first()
        }
    }
    val startResult = start(host)
    startResult.exceptionOrNull()?.let { failure ->
        liveArea.cancel()
        return@coroutineScope Result.failure(failure)
    }

    selectVideoConfiguration(liveArea.await(), savedArea, fallbackArea)
}

internal fun selectVideoConfiguration(
    liveArea: TBoxEvent.VideoArea?,
    savedArea: TBoxEvent.VideoArea?,
    fallbackArea: TBoxEvent.VideoArea? = null
): Result<TBoxVideoConfiguration> {
    val selected = when {
        liveArea != null -> liveArea to if (liveArea.isFallback) {
            TBoxVideoAreaSource.FALLBACK
        } else {
            TBoxVideoAreaSource.LIVE
        }
        savedArea != null -> savedArea to TBoxVideoAreaSource.SAVED
        fallbackArea != null -> fallbackArea.copy(isFallback = true) to TBoxVideoAreaSource.FALLBACK
        else -> null
    } ?: return Result.failure(
        IllegalStateException(
            "The T-Box did not provide a valid video area and no saved or fallback geometry is available."
        )
    )
    val area = selected.first
    return runCatching {
        TBoxVideoConfiguration(
            rawArea = area,
            encoderProfile = EncoderProfile.forTBoxArea(area.width, area.height),
            source = selected.second
        )
    }
}

/**
 * What Carbit Ride 2.4 would configure for the dash's CAPTURE_CONFIG (Api.carbitEncoderPlan), for
 * a profile with [TBoxModelProfile.carbitExactVideo]. [width] x [height] is the size the dash was
 * answered and will decode (CaptureWidth x CaptureHeight); [codec] 2 is H.264.
 */
data class CarbitVideoPlan(
    val width: Int,
    val height: Int,
    val bitRate: Int,
    val fps: Int,
    val iFrameSeconds: Int,
    val cbr: Boolean,
    val landscape: Boolean,
    val codec: Int
) {
    /** A new picture shape needs a new encoder; a running one keeps its rates. */
    fun needsNewEncoder(running: CarbitVideoPlan?): Boolean =
        running == null || width != running.width || height != running.height ||
            landscape != running.landscape
}

/**
 * This profile encoding to Carbit's [plan]: its size, its GOP on plain IDRs (Carbit uses no intra
 * refresh) and CBR only when the dash asked for it. The bitrate is not set here: the plan's rate
 * is the base and the cap of [imageBitrates], so Sharper can never raise it (it turned the Zontes
 * 350D's 8.4 Mbps plan into 13.4 Mbps once, and bigger frames are what a polling dash falls behind
 * on, rider log 2026-10-08). The frame rate never rises above this profile's, which is Android
 * Auto's source rate; the adaptive controller's thermal and link caps then work under both.
 */
internal fun EncoderProfile.followingCarbitPlan(plan: CarbitVideoPlan): EncoderProfile =
    copy(
        width = plan.width,
        height = plan.height,
        frameRate = plan.fps.coerceAtMost(frameRate),
        keyframeIntervalSeconds = plan.iFrameSeconds,
        plainGopWithoutIntraRefresh = true,
        variableBitrate = !plan.cbr
    )

/** The three image bitrates for one encoder: Smoother, Automatic's start, Sharper. */
data class ImageBitrates(val smoother: Int, val balanced: Int, val sharper: Int)

/**
 * [base]: the rider's Advanced override, else the Carbit plan's bitrate when a plan is in use,
 * else the profile's encoderBitRate, else the negotiated 2.5 Mbps.
 * [dashCap]: the Carbit plan's bitrate when a plan is in use, else the dash's own CAPTURE_CONFIG
 * BitRate when it named one, else none. Every value is today's [VideoQuality] multiplier, capped
 * by [dashCap].
 */
internal fun imageBitrates(base: Int, dashCap: Int?): ImageBitrates {
    fun capped(quality: VideoQuality) =
        quality.bitrateFor(base).let { if (dashCap != null) it.coerceAtMost(dashCap) else it }
    return ImageBitrates(
        smoother = capped(VideoQuality.SMOOTHER),
        balanced = capped(VideoQuality.BALANCED),
        sharper = capped(VideoQuality.SHARPER)
    )
}

/**
 * The encoder one motorcycle's session is built with, and Automatic's range (null for a fixed
 * image). Sharper and Smoother are configured at their end of [rates]; Automatic is configured at
 * Balanced, may be raised to Sharper, and starts its controller on that range. Then Carbit's
 * [plan], then the rider's I-frame override, which has to come after the plan because the plan
 * sets its own GOP.
 */
internal fun EncoderProfile.forImage(
    image: ImageQuality,
    rates: ImageBitrates,
    plan: CarbitVideoPlan?,
    keyframeOverride: Int?
): Pair<EncoderProfile, AutoImageRange?> {
    val bitRate = when (image) {
        ImageQuality.SHARPER -> rates.sharper
        ImageQuality.SMOOTHER -> rates.smoother
        ImageQuality.AUTOMATIC -> rates.balanced.coerceIn(rates.smoother, rates.sharper)
    }
    val auto = if (image == ImageQuality.AUTOMATIC) AutoImageRange(rates.smoother, bitRate, rates.sharper) else null
    val configured = copy(bitRate = bitRate, maxBitRate = auto?.ceiling)
        .let { profile -> plan?.let { profile.followingCarbitPlan(it) } ?: profile }
    return (keyframeOverride?.let { configured.copy(keyframeIntervalSeconds = it) } ?: configured) to auto
}
