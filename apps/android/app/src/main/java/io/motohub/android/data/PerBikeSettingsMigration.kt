// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.data

import android.content.Context
import android.content.SharedPreferences
import io.motohub.android.androidauto.AndroidAutoCapabilityProfiles
import io.motohub.android.androidauto.TBoxDisplayGeometryStore
import io.motohub.android.feature.controls.HandlebarCalibration
import io.motohub.android.feature.controls.HandlebarControlStore
import io.motohub.android.feature.controls.HandlebarGesture
import io.motohub.android.feature.controls.MotorcycleScope
import io.motohub.android.feature.controls.PhysicalPress
import io.motohub.android.feature.settings.AndroidAutoSize
import io.motohub.android.feature.settings.ImageQuality
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.tbox.ProfileOverride
import io.motohub.android.tbox.TBoxCapabilityStore
import io.motohub.android.tbox.TBoxModelProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Moves the pre-garage settings onto every saved motorcycle, once.
 *
 * Each global value is copied to `"<key>#<id>"` for every motorcycle that has no value of its own,
 * Size and Image are derived from the old resolution/density and video_quality, and then the
 * globals are deleted, so a motorcycle added later starts from the defaults instead of inheriting
 * them. The planner is pure and every step is one commit that only fills absent keys or removes
 * keys, with DONE last: a crash anywhere leaves a state the next launch re-plans to the same end.
 * Until the deletes have run every read still falls back to the globals (MotorcycleScope.readKey),
 * so a half-finished or deferred migration never changes what a session does.
 *
 * It reads the garage's raw JSON: no decryption, and it never writes `motorcycle_profiles`.
 */
internal object PerBikeSettingsMigration {
    const val DONE = "per_bike_settings_v1"

    /** Set by the launch that first found only the pre-garage profile: see [Garage.LegacyPending]. */
    const val LEGACY_WAITED = "per_bike_settings_legacy_waited"

    private const val SETTINGS = MotorcycleScope.SETTINGS_FILE
    private const val HANDLEBAR = MotorcycleScope.HANDLEBAR_FILE
    private const val CALIBRATION = MotorcycleScope.CALIBRATION_FILE

    /** One motorcycle as the raw garage stores it. A blank [id] is not keyable yet. */
    data class Bike(val id: String, val ssid: String, val modelId: String?, val profileOverrideKey: String?)

    sealed interface Garage {
        /** `profiles` is set but is not a JSON array: nothing moves, tried again next launch. */
        data object Corrupt : Garage

        /**
         * Only the pre-garage profile exists, its ssid alone: one launch waits for loadAll() to
         * convert it, and the next migrates. Still unconverted by then (no saved password, or one
         * that no longer decrypts), it never will be: DONE, with nothing deleted, so the globals
         * stay the fallback of the motorcycle the rider pairs next.
         */
        data object LegacyPending : Garage

        /** No motorcycle: the globals are deleted so they never become a future motorcycle's fallback. */
        data object Empty : Garage

        data class Bikes(val bikes: List<Bike>) : Garage {
            /** An id is still blank: copy for the others, but delete nothing until a save writes it. */
            val unstable: Boolean get() = bikes.any { it.id.isBlank() }
        }
    }

    /** One file's commit: typed puts (Boolean and String exactly as read) and removals. */
    data class Step(val file: String, val puts: Map<String, Any>, val removes: Set<String>)

    /** Per-motorcycle in `handlebar_controls`; not `managed_by_companion` or `defaults_ver`. */
    val HANDLEBAR_BASES: Set<String> = HandlebarGesture.entries.map { it.id }.toSet() + setOf(
        HandlebarControlStore.ENABLED,
        HandlebarControlStore.INPUT_MODE,
        HandlebarControlStore.DASHBOARD_REPORTS_HOLDS
    )

    val CALIBRATION_BASES: Set<String> =
        PhysicalPress.entries.map { it.id }.toSet() + HandlebarCalibration.KEY_VOLUME_ROCKER_SILENT

    val SETTINGS_BASES: Set<String> = setOf(
        MotoHubSettings.KEY_ANDROID_AUTO_RESOLUTION,
        MotoHubSettings.KEY_ANDROID_AUTO_DENSITY,
        MotoHubSettings.KEY_ANDROID_AUTO_ASPECT_MATCHING,
        MotoHubSettings.KEY_DISABLE_TOUCHSCREEN,
        MotoHubSettings.KEY_VIDEO_POWER_MODE,
        MotoHubSettings.KEY_AUTOSTART_ENABLED,
        MotoHubSettings.KEY_AUTOSTART_SERVICE,
        MotoHubSettings.KEY_DASH_CLOCK_SYNC,
        MotoHubSettings.KEY_BLUETOOTH_CLOCK_SYNC,
        MotoHubSettings.KEY_KEEP_WIFI_DIRECT_ON_DISCONNECT
    )

    fun run(context: Context) {
        val app = context.applicationContext
        if (prefs(app, SETTINGS).getBoolean(DONE, false)) return
        // V2 of the handlebar defaults only ever reached the global keys: it has to see them
        // before they are copied.
        HandlebarControlStore.ensureDefaultsMigrated(app)
        val garage = garageOf(app)
        val state = MotorcycleScope.SCOPED_FILES.associateWith { prefs(app, it).all }
        val steps = plan(state, garage) { provenOrientation(app, it) }
        steps.forEach { step ->
            val editor = prefs(app, step.file).edit()
            step.removes.forEach { editor.remove(it) }
            step.puts.forEach { (key, value) -> editor.putTyped(key, value) }
            check(editor.commit()) { "Android did not save ${step.file}." }
        }
        when {
            garage == Garage.Corrupt -> ProjectionEventLog.warning(
                "SETTINGS",
                "Per-motorcycle settings not moved yet: the garage is unreadable. Retrying next launch."
            )
            garage == Garage.LegacyPending && steps.none { DONE in it.puts } -> ProjectionEventLog.record(
                "SETTINGS",
                "Per-motorcycle settings wait for the old motorcycle profile to be converted."
            )
            garage == Garage.LegacyPending -> ProjectionEventLog.warning(
                "SETTINGS",
                "The old motorcycle profile was never converted; its settings stay as the next " +
                    "motorcycle's starting point."
            )
            garage is Garage.Bikes && garage.unstable -> ProjectionEventLog.record(
                "SETTINGS",
                "Per-motorcycle settings copied; the old ones stay until every motorcycle has an id."
            )
            else -> ProjectionEventLog.record(
                "SETTINGS",
                "Per-motorcycle settings moved to ${(garage as? Garage.Bikes)?.bikes?.size ?: 0} motorcycle(s)."
            )
        }
    }

    /**
     * The commits still needed to reach the migrated state from [state] (file name to that file's
     * `all` map), in order; empty once DONE. [orientation] answers true for a dashboard proven
     * portrait, false for proven landscape, null when unproven.
     */
    internal fun plan(
        state: Map<String, Map<String, Any?>>,
        garage: Garage,
        orientation: (Bike) -> Boolean?
    ): List<Step> {
        val settings = state[SETTINGS].orEmpty()
        if (settings[DONE] == true) return emptyList()
        val bikes = when (garage) {
            Garage.Corrupt -> return emptyList()
            Garage.LegacyPending -> return listOf(
                if (settings[LEGACY_WAITED] == true) {
                    Step(SETTINGS, mapOf(DONE to true), setOf(LEGACY_WAITED))
                } else {
                    Step(SETTINGS, mapOf(LEGACY_WAITED to true), emptySet())
                }
            )
            Garage.Empty -> emptyList()
            is Garage.Bikes -> garage.bikes.filter { it.id.isNotBlank() }
        }
        val handlebar = state[HANDLEBAR].orEmpty()
        val calibration = state[CALIBRATION].orEmpty()
        val ids = bikes.map { it.id }
        val copySteps = listOf(
            Step(HANDLEBAR, copies(handlebar, HANDLEBAR_BASES, ids), emptySet()),
            Step(CALIBRATION, copies(calibration, CALIBRATION_BASES, ids), emptySet()),
            Step(SETTINGS, copies(settings, SETTINGS_BASES, ids) + derived(settings, bikes, orientation), emptySet())
        )
        val deleteSteps = if (garage is Garage.Bikes && garage.unstable) emptyList() else listOf(
            Step(HANDLEBAR, emptyMap(), HANDLEBAR_BASES intersect handlebar.keys),
            Step(CALIBRATION, emptyMap(), CALIBRATION_BASES intersect calibration.keys),
            Step(SETTINGS, emptyMap(), (SETTINGS_BASES + MotoHubSettings.KEY_VIDEO_QUALITY) intersect settings.keys),
            Step(SETTINGS, mapOf(DONE to true), setOf(LEGACY_WAITED) intersect settings.keys)
        )
        return (copySteps + deleteSteps).filter { it.puts.isNotEmpty() || it.removes.isNotEmpty() }
    }

    /** [step] applied to [state], the way the executor commits it. Tests only. */
    internal fun apply(state: Map<String, Map<String, Any?>>, step: Step): Map<String, Map<String, Any?>> =
        state + (step.file to (state[step.file].orEmpty() - step.removes + step.puts))

    /**
     * Every global of [bases] copied to each id that has no value of its own, raw and with its
     * type: an invalid enum name copied stays invalid and keeps decoding to the default it does
     * today, and calibration's UNBOUND/MISSING markers survive.
     */
    internal fun copies(all: Map<String, Any?>, bases: Set<String>, ids: List<String>): Map<String, Any> =
        buildMap {
            bases.forEach { base ->
                val value = all[base] ?: return@forEach
                ids.forEach { id ->
                    val key = MotorcycleScope.scopedKey(base, id)
                    if (key !in all) put(key, value)
                }
            }
        }

    /** Size and Image for each motorcycle that has neither yet, from its effective old values. */
    internal fun derived(all: Map<String, Any?>, bikes: List<Bike>, orientation: (Bike) -> Boolean?): Map<String, Any> =
        buildMap {
            bikes.forEach { bike ->
                fun effective(base: String) = (all[MotorcycleScope.scopedKey(base, bike.id)] ?: all[base]) as? String
                val image = MotorcycleScope.scopedKey(MotoHubSettings.KEY_IMAGE_QUALITY, bike.id)
                if (image !in all) {
                    put(image, ImageQuality.fromLegacy(effective(MotoHubSettings.KEY_VIDEO_QUALITY)).name)
                }
                val size = MotorcycleScope.scopedKey(MotoHubSettings.KEY_ANDROID_AUTO_SIZE, bike.id)
                if (size !in all) {
                    put(
                        size,
                        AndroidAutoSize.fromLegacy(
                            effective(MotoHubSettings.KEY_ANDROID_AUTO_RESOLUTION),
                            effective(MotoHubSettings.KEY_ANDROID_AUTO_DENSITY),
                            orientation(bike)
                        ).name
                    )
                }
            }
        }

    /**
     * The garage from its raw preferences, by the profile decoder's own rule: an entry is a
     * motorcycle when it is a JSON object with a non-blank ssid. With no garage yet, the pre-garage
     * [legacySsid] alone means these settings have a motorcycle, whatever its password.
     */
    internal fun garageOf(profilesJson: String?, legacySsid: String?): Garage {
        if (profilesJson.isNullOrBlank()) {
            return if (legacySsid.isNullOrBlank()) Garage.Empty else Garage.LegacyPending
        }
        val array = runCatching { JSONArray(profilesJson) }.getOrNull() ?: return Garage.Corrupt
        val bikes = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val ssid = item.optString(MotorcycleProfileStore.KEY_SSID).trim()
            if (ssid.isEmpty()) return@mapNotNull null
            Bike(
                id = item.optString(MotorcycleProfileStore.KEY_PROFILE_ID),
                ssid = ssid,
                modelId = item.nullableString(MotorcycleProfileStore.KEY_MODEL_ID),
                profileOverrideKey = item.nullableString(MotorcycleProfileStore.KEY_PROFILE_OVERRIDE_KEY)
            )
        }
        return if (bikes.isEmpty()) Garage.Empty else Garage.Bikes(bikes)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun garageOf(context: Context): Garage =
        garageOf(
            profilesJson = prefs(context, MotorcycleProfileStore.PREFERENCES_NAME)
                .getString(MotorcycleProfileStore.KEY_PROFILES, null),
            legacySsid = prefs(context, MotorcycleProfileStore.LEGACY_PREFERENCES_NAME)
                .getString(MotorcycleProfileStore.LEGACY_KEY_SSID, null)
        )

    /**
     * Portrait (true) or landscape (false) only when AndroidAutoSessionService.prepareReceiver
     * decides that orientation whatever happens to the saved geometry: the geometry is usable
     * there AND agrees with the fallback preset, so using it or vetoing it lands the same way.
     */
    private fun provenOrientation(context: Context, bike: Bike): Boolean? {
        val modelId = bike.modelId ?: TBoxModelProfile.modelIdForSsid(bike.ssid)
        val stub = MotorcycleProfile(bike.ssid, "", bike.id, modelId, profileOverrideKey = bike.profileOverrideKey)
        val capabilities = TBoxCapabilityStore(context).load(stub)?.capabilities
        val override = ProfileOverride.byKey(bike.profileOverrideKey)
        val fallback = TBoxModelProfile.defaultAndroidAutoPreset(modelId, capabilities, override)
        val usable = AndroidAutoCapabilityProfiles.usableSavedGeometryForAuto(
            TBoxDisplayGeometryStore(context).load(bike.ssid),
            fallback,
            TBoxModelProfile.hasValidatedAndroidAutoPreset(modelId, capabilities, override)
        ) ?: return null
        val portrait = usable.height > usable.width
        return portrait.takeIf { it == fallback.isPortrait }
    }

    private fun SharedPreferences.Editor.putTyped(key: String, value: Any) {
        when (value) {
            is Boolean -> putBoolean(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            else -> error("Unexpected ${value::class.java.simpleName} under $key")
        }
    }

    private fun prefs(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
}
