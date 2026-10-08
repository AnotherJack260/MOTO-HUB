// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.tbox

import io.motohub.android.aa.AaNavigationGuidance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class TBoxHost(
    val ipAddress: String,
    val port: Int,
    val packageName: String
)

sealed interface TBoxEvent {
    data class Capabilities(val value: TBoxCapabilities) : TBoxEvent
    data class VideoArea(
        val width: Int,
        val height: Int,
        /** True when the area came from a compatibility fallback rather than the T-Box. */
        val isFallback: Boolean = false
    ) : TBoxEvent
    data class Touch(val action: Int, val pointerId: Int, val x: Int, val y: Int) : TBoxEvent
    data object VideoStreamStart : TBoxEvent

    /**
     * The transport dropped queued video up to the next IDR, so the dash's picture is frozen until
     * the encoder makes one: request a sync frame now. Repeated about once a second while it is
     * still owed. [opensStall] is true only for the first ask of a run, the one worth a log line.
     */
    data class KeyframeNeeded(val opensStall: Boolean) : TBoxEvent
    data class Warning(val message: String) : TBoxEvent
    data class FatalError(val message: String) : TBoxEvent
    data object Stopped : TBoxEvent

    /** The dash asked the phone to disconnect (EasyConn 0x106F0): end the mode as a rider Stop does. */
    data object DashDisconnect : TBoxEvent

    /** A Carbit-exact session's dash sent (or updated) its CAPTURE_CONFIG; see [CarbitVideoPlan]. */
    data class CarbitPlan(val plan: CarbitVideoPlan) : TBoxEvent
}

/**
 * Which wire protocol a [TBoxModelProfile] speaks, and therefore which [TBoxTransport]
 * implementation a session must be routed through (see SelectingTBoxTransport).
 */
enum class TBoxTransportFamily {
    /** Carbit/EasyConn dashes: the phone is the TCP client of the dash's services. */
    EASYCONN,

    /** ThinkerRide dashes (KOVE family): BLE handshake, then the dash connects to the phone. */
    THINKERRIDE,

    /** Yunmo SoftAP dashes (Moto Morini X-Cape 1200): one TCP socket to 192.168.4.1:8200. */
    YUNMO
}

sealed interface TBoxTransportStatus {
    data object Unavailable : TBoxTransportStatus
    data object Ready : TBoxTransportStatus
    data class Failure(val reason: String) : TBoxTransportStatus
}

interface TBoxTransport {
    /** Selects the profile whose wire-level capabilities will be advertised for the next session. */
    /**
     * [motorcycle] is optional and only [TBoxWireLadder] uses it: its memory of which wire format
     * a dashboard accepted is per motorcycle. A caller with none gets the profile's own settings.
     */
    fun configureProtocolProfile(
        profile: TBoxModelProfile,
        motorcycle: io.motohub.android.session.MotorcycleProfile? = null
    ) = Unit

    /**
     * The profile this transport is actually running, when that is not simply the one the caller
     * configured. Discovery can change it: a dash that answers Yunmo after EasyConn found nothing
     * is routed to a different family *and* a different profile than the saved motorcycle resolves
     * to, and the session's encoder settings have to follow the same switch. Null means "nothing
     * to correct, use what you resolved".
     */
    val activeProtocolProfile: TBoxModelProfile? get() = null
    suspend fun discover(link: TBoxLink, expectedModelId: String? = null): Result<TBoxHost>
    suspend fun start(host: TBoxHost): Result<Unit>
    fun offerAccessUnit(avcc: ByteArray): Boolean

    /**
     * Offers one JPEG still, for the dash families whose OEM app never streams H.264.
     *
     * Separate from [offerAccessUnit] because a still carries its own [frameId]: these dashes
     * acknowledge by id, so an id invented downstream would throw away the link's only liveness
     * signal. Returns false by default - a transport without a still path must refuse rather
     * than absorb, or a profile that asked for stills gets silently served something else.
     */
    fun offerStillFrame(jpeg: ByteArray, frameId: Int): Boolean = false
    suspend fun stop()

    /**
     * [stop] for an orderly end the rider or the app chose while the link is still up: a
     * transport that can hand the dash back its own UI first (EasyConn's ReleaseDash) does so.
     * Never for a link that is already lost: that is [stop], or the stop waits out the dash's
     * answers on a dead socket first.
     */
    suspend fun release() = stop()

    /**
     * Android Auto's turn-by-turn for the dash's own arrows, where the wire has them (EasyConn).
     * Blocks until the dash answers: call it from one worker, newest state only.
     */
    fun showNavigation(guidance: AaNavigationGuidance.Snapshot) = Unit

    /** Carbit's encoder plan for this session's CAPTURE_CONFIG, for a Carbit-exact profile only. */
    fun carbitVideoPlan(): CarbitVideoPlan? = null
    val events: Flow<TBoxEvent>
}

/** Keeps UI and session code honest until the GPL transport AAR is packaged. */
class UnavailableTBoxTransport : TBoxTransport {
    override suspend fun discover(link: TBoxLink, expectedModelId: String?): Result<TBoxHost> = Result.failure(
        IllegalStateException("hudlib.aar is not integrated")
    )

    override suspend fun start(host: TBoxHost): Result<Unit> = Result.failure(
        IllegalStateException("hudlib.aar is not integrated")
    )

    override fun offerAccessUnit(avcc: ByteArray): Boolean = false

    override suspend fun stop() = Unit

    override val events: Flow<TBoxEvent> = emptyFlow()
}
