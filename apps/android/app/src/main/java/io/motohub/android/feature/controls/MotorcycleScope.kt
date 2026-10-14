// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.controls

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import io.motohub.android.tbox.TBoxSessionRegistry

/**
 * A Context that pins [MotorcycleScope] to one motorcycle: the Garage editing a motorcycle that is
 * not the live one, or a caller that already holds the profile (the transport, the Wi-Fi Direct
 * connector, the permission check).
 *
 * The Garage hands it to whole screens as their `LocalContext`, so nothing under it may cast its
 * context to an Activity: it is a wrapper, never the Activity itself.
 */
class MotorcycleContext(base: Context, val motorcycleId: String) : ContextWrapper(base)

/**
 * Namespaces settings to one motorcycle, so every bike in the garage keeps its own calibration,
 * button mapping and Android Auto setup — a 700MT's held-rocker bindings must not leak onto a CFDL16.
 *
 * A scoped value is stored under `"<base>#<motorcycleId>"`. **Reads fall back to the un-scoped
 * (global) key** when the motorcycle has no value yet: before PerBikeSettingsMigration copies the
 * globals onto every saved motorcycle, nothing a session reads changes. Writes always go to the
 * scoped key (or the global key when no motorcycle is known).
 *
 * The motorcycle is, in this order: an explicit [MotorcycleContext]'s; else the live session's
 * ([TBoxSessionRegistry.current]) when one is running; otherwise the garage's active profile —
 * read as the raw `active_id` preference rather than through
 * [io.motohub.android.data.MotorcycleProfileStore.load], which decodes and decrypts the whole
 * profile list and is far too heavy for something consulted on every button press.
 */
object MotorcycleScope {

    /** [context] pinned to [motorcycleId]: every read and write through it means that motorcycle. */
    fun of(context: Context, motorcycleId: String): Context = MotorcycleContext(context, motorcycleId)

    /** Stable id of the motorcycle settings should scope to, or null for global keys. */
    fun suffix(context: Context): String? = suffixFor(
        explicit = (context as? MotorcycleContext)?.motorcycleId,
        live = { TBoxSessionRegistry.current()?.motorcycle?.id },
        active = { garageActiveId(context) }
    )

    /** The garage's active motorcycle, raw, whatever is streaming. */
    fun garageActiveId(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(GARAGE_PREFERENCES, Context.MODE_PRIVATE)
            .getString(GARAGE_ACTIVE_ID, null)

    /** The resolution order, pure: an explicit wrapper, then the live session, then the garage. */
    internal fun suffixFor(explicit: String?, live: () -> String?, active: () -> String?): String? =
        explicit ?: live() ?: active()

    fun getString(p: SharedPreferences, context: Context, base: String, default: String?): String? =
        p.getString(readKey(base, suffix(context), p::contains), default)

    fun putString(p: SharedPreferences, context: Context, base: String, value: String) {
        p.edit().putString(writeKey(context, base), value).apply()
    }

    fun getBoolean(p: SharedPreferences, context: Context, base: String, default: Boolean): Boolean =
        p.getBoolean(readKey(base, suffix(context), p::contains), default)

    fun putBoolean(p: SharedPreferences, context: Context, base: String, value: Boolean) {
        p.edit().putBoolean(writeKey(context, base), value).apply()
    }

    /**
     * Drops the current motorcycle's scoped value only (the global key when no motorcycle is
     * known), reverting the setting to its fallback/default. Never touches the global key while
     * a motorcycle is scoped: calibration re-teaching releases a binding from *this* bike's
     * conflicting press — clearing the global would delete another bike's fallback with it.
     */
    fun remove(p: SharedPreferences, context: Context, base: String) {
        p.edit().remove(writeKey(context, base)).apply()
    }

    /** True when the current motorcycle has any value stored under [base] — scoped or global. */
    fun contains(p: SharedPreferences, context: Context, base: String): Boolean {
        suffix(context)?.let { if (p.contains(scopedKey(base, it))) return true }
        return p.contains(base)
    }

    /** Deletes every value stored for [motorcycleId], in each file that scopes per motorcycle. */
    fun forget(context: Context, motorcycleId: String) {
        SCOPED_FILES.forEach { file ->
            val p = context.applicationContext.getSharedPreferences(file, Context.MODE_PRIVATE)
            val editor = p.edit()
            keysOf(p.all.keys, motorcycleId).forEach { editor.remove(it) }
            editor.apply()
        }
    }

    internal fun keysOf(keys: Set<String>, motorcycleId: String): List<String> =
        keys.filter { it.endsWith("#$motorcycleId") }

    private fun writeKey(context: Context, base: String): String =
        writeKeyFor(base, suffix(context))

    /** Pure key resolution for a READ: the scoped key when one exists, else the global base. */
    internal fun readKey(base: String, suffix: String?, hasKey: (String) -> Boolean): String {
        if (suffix != null) {
            val scoped = scopedKey(base, suffix)
            if (hasKey(scoped)) return scoped
        }
        return base
    }

    /** Pure key resolution for a WRITE: always scoped when a motorcycle is known. */
    internal fun writeKeyFor(base: String, suffix: String?): String =
        suffix?.let { scopedKey(base, it) } ?: base

    internal fun scopedKey(base: String, motorcycleId: String): String = "$base#$motorcycleId"

    // MotorcycleProfileStore's own constants are private; these mirror them and are asserted
    // by unit test so a rename there cannot silently break the scope.
    internal const val GARAGE_PREFERENCES = "motorcycle_profiles"
    internal const val GARAGE_ACTIVE_ID = "active_id"

    /** Every preferences file that holds `"<base>#<motorcycleId>"` keys. */
    internal const val SETTINGS_FILE = "moto_hub_settings"
    internal const val HANDLEBAR_FILE = "handlebar_controls"
    internal const val CALIBRATION_FILE = "handlebar_calibration"
    internal val SCOPED_FILES = listOf(SETTINGS_FILE, HANDLEBAR_FILE, CALIBRATION_FILE)
}
