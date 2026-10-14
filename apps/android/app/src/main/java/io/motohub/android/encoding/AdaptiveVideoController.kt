// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.encoding

import android.content.Context
import android.os.PowerManager
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.feature.settings.VideoPowerMode
import kotlin.math.min

/**
 * Adjusts a live encoder to what the phone and the bike link can actually carry.
 *
 * Thermal pressure protects the phone from sustained encoder throttling, and stays a Power mode
 * AUTO courtesy: a rider who picked a fixed mode asked for that frame rate and gets it.
 *
 * Lost frames are the downstream signal, and they are not a preference - a link that is discarding
 * frames wastes the bitrate spent on them and, on a GOP stream, smears the TFT until the next
 * keyframe. So the link backoff applies in every mode, multiplicative down and slow back up, with
 * the rider's mode as the ceiling it recovers to. Loss counts both access units the transport
 * refused and frames it accepted and then dropped: the CORE video pipe does the latter, and
 * counting only the former left the controller blind exactly when it was needed. A rider's CFMOTO
 * MTX800 on 2026-08-17 lost ~280 dashboard frames in that pipe against a single rejection, at a
 * fixed Smooth 30fps that nothing was allowed to lower.
 *
 * Automatic image adds a slow, learned bitrate target under those fast factors ([AutoImagePolicy]),
 * between Smoother's and Sharper's bitrates; Sharper and Smoother are that range's ends, held fixed.
 */
data class AdaptiveVideoDecision(
    val bitrate: Int,
    val frameRate: Int,
    val linkFactor: Float
)

object AdaptiveVideoPolicy {
    const val MIN_BITRATE = 600_000
    const val DROP_THRESHOLD = 8
    const val LINK_MIN = 0.4f
    const val LINK_MAX = 1.0f
    const val LINK_BACKOFF = 0.8f
    const val LINK_RECOVERY_STEP = 0.05f

    /** The slowest the link backoff may pace a stream. Below this the TFT reads as a slideshow,
     *  and a link that cannot carry 12fps has a problem no encoder setting is going to solve. */
    const val MIN_FRAME_RATE = 12

    fun thermalBitrateFactor(status: Int): Float = when {
        status <= PowerManager.THERMAL_STATUS_LIGHT -> 1.0f
        status == PowerManager.THERMAL_STATUS_MODERATE -> 0.8f
        status == PowerManager.THERMAL_STATUS_SEVERE -> 0.6f
        else -> 0.5f
    }

    fun thermalFrameRateCap(status: Int, baseFrameRate: Int): Int = when {
        status <= PowerManager.THERMAL_STATUS_LIGHT -> baseFrameRate
        status == PowerManager.THERMAL_STATUS_MODERATE -> min(baseFrameRate, 20)
        status == PowerManager.THERMAL_STATUS_SEVERE -> min(baseFrameRate, 15)
        else -> min(baseFrameRate, 12)
    }

    fun nextLinkFactor(previous: Float, lostFrames: Int): Float = if (
        lostFrames >= DROP_THRESHOLD
    ) {
        (previous * LINK_BACKOFF).coerceAtLeast(LINK_MIN)
    } else {
        (previous + LINK_RECOVERY_STEP).coerceAtMost(LINK_MAX)
    }

    /** Fewer, larger-budget frames on a link that is losing them. Cutting only the bitrate leaves
     *  the same frame count queued on a transport whose queue is what overflowed. */
    fun linkFrameRateCap(baseFrameRate: Int, linkFactor: Float): Int =
        (baseFrameRate * linkFactor).toInt()
            .coerceAtLeast(MIN_FRAME_RATE.coerceAtMost(baseFrameRate))
            .coerceAtMost(baseFrameRate)

    /**
     * @param baseBitrate the most this tick may send: the encoder's configured rate for a fixed
     * image, Automatic image's learned target otherwise.
     * @param baseFrameRate the ceiling this session may run at: the encoder's own rate under Power
     * mode AUTO, the mode's rate otherwise.
     * @param thermalStatus the heat the bitrate answers to; see [thermalStatuses].
     * @param frameRateThermalStatus the heat the frame rate answers to; see [thermalStatuses].
     */
    fun decide(
        baseBitrate: Int,
        baseFrameRate: Int,
        thermalStatus: Int,
        previousLinkFactor: Float,
        lostFrames: Int,
        frameRateThermalStatus: Int = thermalStatus
    ): AdaptiveVideoDecision = decideAt(
        baseBitrate = baseBitrate,
        baseFrameRate = baseFrameRate,
        thermalStatus = thermalStatus,
        frameRateThermalStatus = frameRateThermalStatus,
        linkFactor = nextLinkFactor(previousLinkFactor, lostFrames)
    )

    /** [decide] with this tick's link factor already known: Automatic image moves it in between
     *  (see [AdaptiveVideoLoop.tick]). Heat and link combine as the minimum, not the product. */
    fun decideAt(
        baseBitrate: Int,
        baseFrameRate: Int,
        thermalStatus: Int,
        frameRateThermalStatus: Int,
        linkFactor: Float
    ): AdaptiveVideoDecision {
        val factor = min(thermalBitrateFactor(thermalStatus), linkFactor)
        return AdaptiveVideoDecision(
            bitrate = (baseBitrate * factor).toInt().coerceIn(
                MIN_BITRATE.coerceAtMost(baseBitrate),
                baseBitrate
            ),
            frameRate = min(
                thermalFrameRateCap(frameRateThermalStatus, baseFrameRate),
                linkFrameRateCap(baseFrameRate, linkFactor)
            ),
            linkFactor = linkFactor
        )
    }

    /**
     * The heat that (bitrate, frame rate) answer to. A rider who named a frame rate gets it, so heat
     * reaches the frame rate only under Power mode AUTO. The bitrate is Automatic image's to manage
     * too, so heat reaches it under either. With a fixed image and a fixed frame rate nothing is
     * throttled for heat behind the rider's back.
     */
    fun thermalStatuses(status: Int, autoFrameRate: Boolean, automaticImage: Boolean): Pair<Int, Int> =
        Pair(
            if (autoFrameRate || automaticImage) status else PowerManager.THERMAL_STATUS_NONE,
            if (autoFrameRate) status else PowerManager.THERMAL_STATUS_NONE
        )
}

/**
 * Automatic image's bitrates for one encoder: [floor] is Smoother's, [start] Balanced's and
 * [ceiling] Sharper's, each one already capped by the dash's request or the Carbit plan.
 */
data class AutoImageRange(val floor: Int, val start: Int, val ceiling: Int) {
    /** floor <= start <= ceiling, the ceiling winning: it is the dash's own limit. */
    fun normalized(): AutoImageRange {
        val top = ceiling.coerceAtLeast(1)
        val bottom = floor.coerceIn(1, top)
        return AutoImageRange(bottom, start.coerceIn(bottom, top), top)
    }
}

/** [cap] is the highest target this encoder may still try: a level that stalled is not tried again. */
data class AutoImageState(
    val target: Int,
    val cap: Int,
    val cleanTicks: Int = 0,
    val holdTicks: Int = 0,
    val lossTicks: Int = 0
)

/**
 * The slow, learned half of Automatic image. The link and thermal factors react within a tick and
 * forget; this target steps down at once on a stall or on sustained loss, and climbs back only after
 * a quiet spell, never to a level that already failed on this encoder.
 */
object AutoImagePolicy {
    /** 60 s at the controller's 5 s tick with no step up after a step down. */
    const val HOLD_TICKS = 12

    /** 30 s clean per step up. */
    const val CLEAN_TICKS = 6

    /** Consecutive lossy ticks before the target steps down; a single one is the link factor's job. */
    const val LOSS_TICKS = 2

    fun start(range: AutoImageRange): AutoImageState =
        AutoImageState(target = range.start, cap = range.ceiling)

    /** A tenth of the start: one step up, and how far under a failed level the cap goes. */
    fun step(range: AutoImageRange): Int = range.start / 10

    /** @param linkFactor this tick's, after [AdaptiveVideoPolicy.nextLinkFactor]. */
    fun next(
        range: AutoImageRange,
        state: AutoImageState,
        stall: Boolean,
        lostFrames: Int,
        thermalStatus: Int,
        linkFactor: Float
    ): AutoImageState {
        val lossTicks = if (lostFrames >= AdaptiveVideoPolicy.DROP_THRESHOLD) state.lossTicks + 1 else 0
        if (stall || lossTicks >= LOSS_TICKS) {
            val target = (state.target.toLong() * 4 / 5).toInt().coerceAtLeast(range.floor)
            return AutoImageState(
                target = target,
                // ponytail: maxOf(target, …) only matters on a range whose floor is under half its
                // start, which imageBitrates never builds; it keeps target <= cap regardless.
                cap = maxOf(target, minOf(state.cap, state.target - step(range))),
                holdTicks = HOLD_TICKS
            )
        }
        if (state.holdTicks > 0) return state.copy(holdTicks = state.holdTicks - 1, lossTicks = lossTicks)
        val clean = lostFrames == 0 &&
            thermalStatus <= PowerManager.THERMAL_STATUS_LIGHT &&
            // 0.05 float steps can land a hair under 1.0 on the way back up.
            linkFactor >= AdaptiveVideoPolicy.LINK_MAX - 0.001f
        val cleanTicks = if (clean) state.cleanTicks + 1 else 0
        if (cleanTicks < CLEAN_TICKS) return state.copy(cleanTicks = cleanTicks, lossTicks = lossTicks)
        return state.copy(
            target = (state.target + step(range)).coerceAtMost(state.cap),
            cleanTicks = 0,
            lossTicks = lossTicks
        )
    }

    /** A rate-only Carbit plan update lowers the ceiling, the cap and, where they sit above it, the
     *  target and the floor. It never raises anything, and a non-positive rate is no request. */
    fun lowerCeiling(
        range: AutoImageRange,
        state: AutoImageState,
        ceiling: Int
    ): Pair<AutoImageRange, AutoImageState> {
        if (ceiling <= 0 || ceiling >= range.ceiling) return range to state
        val cap = min(state.cap, ceiling)
        return range.copy(floor = min(range.floor, ceiling), ceiling = ceiling) to
            state.copy(target = min(state.target, cap), cap = cap)
    }
}

/**
 * One encoder's adaptive state with nothing Android in it, so whole rides replay in a unit test.
 * [AdaptiveVideoController] reads the phone and the encoder, and applies what this decides.
 */
internal class AdaptiveVideoLoop {
    var linkFactor = AdaptiveVideoPolicy.LINK_MAX
        private set
    var appliedBitrate = -1
        private set
    var range: AutoImageRange? = null
        private set

    /** Null for a fixed image: Sharper, Smoother, or no range given. */
    var auto: AutoImageState? = null
        private set

    /** The codec refused a bitrate change, so Automatic stops moving for this encoder. */
    var frozen = false
        private set
    private var stallPending = false

    val automatic: Boolean get() = auto != null

    fun reset(range: AutoImageRange?) {
        linkFactor = AdaptiveVideoPolicy.LINK_MAX
        appliedBitrate = -1
        frozen = false
        stallPending = false
        this.range = range?.normalized()
        auto = this.range?.let(AutoImagePolicy::start)
    }

    /** A fixed image ignores stalls, as it always has. */
    fun noteStall() {
        if (auto != null) stallPending = true
    }

    fun updateCeiling(ceiling: Int) {
        val r = range ?: return
        val s = auto ?: return
        val (lowered, state) = AutoImagePolicy.lowerCeiling(r, s, ceiling)
        range = lowered
        auto = state
    }

    /** A tick inside an outage judges nothing, a stall raised during it included. */
    fun absorbOutage() {
        stallPending = false
    }

    /** @param fixedBitrate the encoder's configured rate, the base for a fixed image. */
    fun tick(
        fixedBitrate: Int,
        baseFrameRate: Int,
        thermalStatus: Int,
        frameRateThermalStatus: Int,
        lostFrames: Int
    ): AdaptiveVideoDecision {
        var link = AdaptiveVideoPolicy.nextLinkFactor(linkFactor, lostFrames)
        val stall = stallPending
        stallPending = false
        val before = auto
        val r = range
        if (before != null && r != null && !frozen) {
            val after = AutoImagePolicy.next(r, before, stall, lostFrames, thermalStatus, link)
            // Transfer, not compound: the learned target takes over the cut the link factor already
            // made, so the bitrate applied on this tick is never cut by both. Written as the ratio
            // rather than "/ 0.8" so that a target held up by the floor keeps the link's cut.
            if (after.target < before.target) {
                link = (link * (before.target.toFloat() / after.target))
                    .coerceAtMost(AdaptiveVideoPolicy.LINK_MAX)
            }
            auto = after
        }
        linkFactor = link
        return AdaptiveVideoPolicy.decideAt(
            baseBitrate = auto?.target ?: fixedBitrate,
            baseFrameRate = baseFrameRate,
            thermalStatus = thermalStatus,
            frameRateThermalStatus = frameRateThermalStatus,
            linkFactor = link
        )
    }

    /**
     * A fixed image records [bitrate] whatever the codec answered, as it always has. Automatic
     * believes only a success, and the first refusal freezes it; returns true on that refusal.
     */
    fun bitrateApplied(bitrate: Int, accepted: Boolean): Boolean {
        if (accepted || auto == null) {
            appliedBitrate = bitrate
            return false
        }
        val first = !frozen
        frozen = true
        return first
    }
}

/** Owns the live policy state for one projection encoder. */
class AdaptiveVideoController(
    context: Context,
    private val log: (String) -> Unit
) {
    // Services can construct their fields before attachBaseContext(). Resolve the application
    // context only when the first stream tick runs, after Android has attached the component.
    private val componentContext = context
    private val appContext by lazy { componentContext.applicationContext ?: componentContext }
    private val powerManager by lazy { appContext.getSystemService(PowerManager::class.java) }
    private val loop = AdaptiveVideoLoop()
    private var lastLostFrames = 0L
    private var appliedFrameRate = -1
    private var lastOutputBytes = 0L
    private var lastTickNanos = System.nanoTime()

    /**
     * Called right after encoder.start(). [auto] turns Automatic image on for this encoder; null
     * keeps the bitrate it was configured with as the ceiling, as before.
     */
    @Synchronized
    fun reset(auto: AutoImageRange? = null) {
        loop.reset(auto)
        lastLostFrames = 0L
        appliedFrameRate = -1
        lastOutputBytes = 0L
        lastTickNanos = System.nanoTime()
    }

    /** RideDaemon's event 5 opened a stall: the dash froze waiting for a keyframe. Automatic image
     *  steps down on the next tick; a fixed image ignores it. */
    @Synchronized
    fun noteStall() = loop.noteStall()

    /** A Carbit rate-only plan update: lowers Automatic's ceiling and cap, never raises them. A
     *  fixed image keeps its configured bitrate, as before. */
    @Synchronized
    fun updateCeiling(ceiling: Int) = loop.updateCeiling(ceiling)

    /**
     * @param linkDown true while the session has no working transport at all - a broken video
     * pipe, a recovery in flight. Every access unit is rejected then, which is not the "weak bike
     * link" this policy exists to soften: reading it as one collapsed the bitrate to the floor
     * within twenty seconds and handed the recovered session a picture degraded for no reason.
     * The outage's rejections are absorbed, not judged.
     */
    @Synchronized
    fun onTick(encoder: AvcEncoder?, linkDown: Boolean = false) {
        val activeEncoder = encoder ?: return
        val baseBitrate = activeEncoder.targetBitrate()
        if (baseBitrate <= 0) return
        // What the codec actually produced since the last tick: KEY_BIT_RATE is a target, and a
        // codec may ignore a dynamic change without saying so.
        val now = System.nanoTime()
        val outputBytes = activeEncoder.outputBytesTotal()
        val measuredKbps = if (now > lastTickNanos) {
            ((outputBytes - lastOutputBytes).coerceAtLeast(0L) * 8_000_000L / (now - lastTickNanos)).toInt()
        } else {
            0
        }
        lastOutputBytes = outputBytes
        lastTickNanos = now
        if (linkDown) {
            lastLostFrames = lostFramesTotal(activeEncoder)
            loop.absorbOutage()
            return
        }

        val mode = MotoHubSettings.videoPowerMode(appContext)
        val autoMode = mode == VideoPowerMode.AUTO
        // Outside AUTO the rider named a frame rate, so it becomes the ceiling the link backoff
        // works under, and heat is left out of it entirely.
        val ceilingFrameRate = if (autoMode) {
            activeEncoder.baseFrameRate
        } else {
            mode.frameRate.coerceAtMost(activeEncoder.baseFrameRate)
        }
        val (thermalStatus, frameRateThermalStatus) = AdaptiveVideoPolicy.thermalStatuses(
            status = if (autoMode || loop.automatic) {
                runCatching { powerManager?.currentThermalStatus }
                    .getOrNull()
                    ?: PowerManager.THERMAL_STATUS_NONE
            } else {
                PowerManager.THERMAL_STATUS_NONE
            },
            autoFrameRate = autoMode,
            automaticImage = loop.automatic
        )
        val totalLost = lostFramesTotal(activeEncoder)
        val lostThisTick = (totalLost - lastLostFrames)
            .coerceAtLeast(0L)
            .toInt()
        lastLostFrames = totalLost
        val decision = loop.tick(
            fixedBitrate = baseBitrate,
            baseFrameRate = ceilingFrameRate,
            thermalStatus = thermalStatus,
            frameRateThermalStatus = frameRateThermalStatus,
            lostFrames = lostThisTick
        )
        val linkFactor = decision.linkFactor
        if (!loop.frozen && decision.bitrate != loop.appliedBitrate) {
            val accepted = activeEncoder.setEncoderBitrate(decision.bitrate)
            if (loop.bitrateApplied(decision.bitrate, accepted)) {
                log(
                    "[adaptive] The codec refused a bitrate change, so Automatic image stops " +
                        "moving for this encoder."
                )
            }
            log(
                "[adaptive] ${mode.label} thermal=${thermalLabel(thermalStatus)} " +
                    "lost/tick=$lostThisTick link=${(linkFactor * 100).toInt()}% " +
                    "bitrate=${decision.bitrate / 1000}kbps measured=${measuredKbps}kbps" +
                    (loop.auto?.let { " auto=${it.target / 1000}kbps cap=${it.cap / 1000}kbps" } ?: "")
            )
        }
        if (decision.frameRate != appliedFrameRate) {
            activeEncoder.setFrameCap(decision.frameRate)
            appliedFrameRate = decision.frameRate
            log(
                "[adaptive] ${mode.label} thermal=${thermalLabel(frameRateThermalStatus)} " +
                    "link=${(linkFactor * 100).toInt()}% fps=${decision.frameRate}"
            )
        }
    }

    /** Access units the transport refused, plus frames it accepted and then dropped. Both are one
     *  picture that never reached the TFT. */
    private fun lostFramesTotal(encoder: AvcEncoder): Long =
        encoder.rejectedAccessUnitsTotal() + encoder.transportDroppedFramesTotal()

    private fun thermalLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "none"
        PowerManager.THERMAL_STATUS_LIGHT -> "light"
        PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
        else -> status.toString()
    }
}
