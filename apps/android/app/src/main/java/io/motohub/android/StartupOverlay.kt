// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android

/**
 * The app-wide interruptions MainActivity can raise, most important first. Composed on their own
 * they used to stack: after a crash, with a wire question pending and the safety page not yet
 * silenced, three windows ended up on top of each other with the least important one - the update,
 * a second later - above the rest.
 *
 * [waitsForModals] marks the ones nobody asked for just now. They never land on top of a sheet or
 * dialog the rider has open; the companion conflict follows a start the rider (or autostart) just
 * asked for, so it does not wait.
 */
internal enum class StartupOverlay(val waitsForModals: Boolean) {
    SAFETY(false),
    COMPANION(false),
    CRASH_CONSENT(false),
    UPDATE(true),
    WIRE_VERDICT(true),
    WIRE_NUDGE(true)
}

/**
 * One interruption at a time. The one on screen stays until it closes, even when something more
 * important turns up meanwhile - a sheet must never vanish from under the rider's thumb - and the
 * rest just stay pending until it is their turn: nothing is lost, only queued.
 *
 * @param shown what is on screen now, or null.
 * @param pending everything that wants the screen.
 * @param modalsOpen `MhModals.open`: kit sheets and dialogs on screen, ours included.
 */
internal fun nextOverlay(shown: StartupOverlay?, pending: Set<StartupOverlay>, modalsOpen: Int): StartupOverlay? {
    if (shown != null && shown in pending) return shown
    return StartupOverlay.entries.firstOrNull { it in pending && (!it.waitsForModals || modalsOpen == 0) }
}
