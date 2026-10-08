// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.home

import io.motohub.android.session.HubSessionState
import io.motohub.android.session.SessionPhase

internal enum class HubDestination {
    PAIRING,
    CONNECTING,
    CONNECTION,
    MODE_SELECTION,
    ACTIVE_SESSION
}

internal fun resolveHubDestination(
    session: HubSessionState,
    androidAutoActive: Boolean,
    featureSessionActive: Boolean = false,
    externalDisplayActive: Boolean = false
): HubDestination = when {
    // Android Auto on this phone runs with no motorcycle saved, and its Stop is on ACTIVE_SESSION.
    session.motorcycle == null && (androidAutoActive || externalDisplayActive) -> HubDestination.ACTIVE_SESSION
    session.motorcycle == null -> HubDestination.PAIRING
    session.phase == SessionPhase.CONNECTING_NETWORK ||
        session.phase == SessionPhase.DISCOVERING_TBOX -> HubDestination.CONNECTING
    androidAutoActive || featureSessionActive || externalDisplayActive ||
        session.phase == SessionPhase.REQUESTING_PROJECTION ||
        session.phase == SessionPhase.CAPTURING -> HubDestination.ACTIVE_SESSION
    session.phase == SessionPhase.READY -> HubDestination.MODE_SELECTION
    else -> HubDestination.CONNECTION
}
