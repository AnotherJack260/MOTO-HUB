// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.tbox

import io.motohub.android.encoding.EncoderProfile
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
 * refresh), CBR only when the dash asked for it, and its bitrate as the base the rider's [quality]
 * scales. The frame rate never rises above this profile's, which is Android Auto's source rate;
 * the adaptive controller's thermal and link caps then work under both, as for any profile.
 */
internal fun EncoderProfile.followingCarbitPlan(plan: CarbitVideoPlan, quality: VideoQuality): EncoderProfile =
    copy(
        width = plan.width,
        height = plan.height,
        frameRate = plan.fps.coerceAtMost(frameRate),
        bitRate = quality.bitrateFor(plan.bitRate),
        keyframeIntervalSeconds = plan.iFrameSeconds,
        plainGopWithoutIntraRefresh = true,
        variableBitrate = !plan.cbr
    )
