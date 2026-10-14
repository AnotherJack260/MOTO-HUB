// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.data

import io.motohub.android.data.PerBikeSettingsMigration.Bike
import io.motohub.android.data.PerBikeSettingsMigration.Garage
import io.motohub.android.feature.controls.MotorcycleScope
import io.motohub.android.feature.controls.PhysicalPress
import io.motohub.android.feature.settings.AndroidAutoDensityMode
import io.motohub.android.feature.settings.AndroidAutoResolutionMode
import io.motohub.android.feature.settings.AndroidAutoSize
import io.motohub.android.feature.settings.AutostartService
import io.motohub.android.feature.settings.ImageQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerBikeSettingsMigrationTest {

    private val zontes = Bike("zontes", "ZONTES-350D", null, "zontes_350d_vbr_test")
    private val second = Bike("cfmoto", "CFMOTO-800NK", "37426", null)
    private val landscapeProven: (Bike) -> Boolean? = { false }

    /** The owner's phone before the upgrade, plus a second motorcycle's calibration of its own. */
    private val ownerState: Map<String, Map<String, Any?>> = mapOf(
        SETTINGS to mapOf(
            "android_auto_resolution" to "LANDSCAPE_FHD",
            "android_auto_density" to "DPI_240",
            "video_quality" to "SHARPER",
            "video_power_mode" to "SMOOTH",
            "autostart_enabled" to true,
            "autostart_service" to "ANDROID_AUTO",
            "dash_clock_sync" to true,
            "auto_connect" to true,
            "logging_enabled" to true
        ),
        HANDLEBAR to mapOf(
            "enabled" to true,
            "input_mode" to "avrcp",
            "dashboard_reports_holds" to true,
            "volumeUp" to "scrollBack",
            "trackForward" to "dpadRight",
            "managed_by_companion" to false,
            "defaults_ver" to 3
        ),
        CALIBRATION to mapOf(
            "up_press" to "volumeUp",
            "down_press" to "volumeDown",
            "select_press" to "enter",
            "left_hold" to "",
            "right_hold" to "__missing__",
            "observed_volume_rocker_silent" to "true",
            // The second motorcycle's own teaching shadows the global binding.
            "up_press#cfmoto" to "trackForward",
            "left_hold#cfmoto" to "__missing__"
        )
    )

    @Test
    fun `the garage is read by the profile decoder's own rules`() {
        assertEquals(Garage.Corrupt, PerBikeSettingsMigration.garageOf("{not json", null, false))
        assertEquals(Garage.Corrupt, PerBikeSettingsMigration.garageOf("{}", null, false))
        assertEquals(Garage.LegacyPending, PerBikeSettingsMigration.garageOf(null, "ZONTES", legacyPassword = true))
        assertEquals(Garage.Empty, PerBikeSettingsMigration.garageOf(null, null, false))
        assertEquals(Garage.Empty, PerBikeSettingsMigration.garageOf(null, "  ", legacyPassword = true))
        assertEquals(Garage.Empty, PerBikeSettingsMigration.garageOf("", null, false))
        assertEquals(Garage.Empty, PerBikeSettingsMigration.garageOf("[]", "ZONTES", legacyPassword = true))
        // A non-object element and a blank ssid are not motorcycles.
        assertEquals(Garage.Empty, PerBikeSettingsMigration.garageOf("""[1, {"id":"a","ssid":"  "}]""", null, false))

        val bikes = PerBikeSettingsMigration.garageOf(
            """[7, {"id":"a","ssid":" ZONTES ","model_id":null,"profile_override_key":"zontes_350d_vbr_test"},
               {"id":"b","ssid":"CF","model_id":"37426"}]""",
            legacySsid = null,
            legacyPassword = false
        )
        assertEquals(
            Garage.Bikes(listOf(Bike("a", "ZONTES", null, "zontes_350d_vbr_test"), Bike("b", "CF", "37426", null))),
            bikes
        )
        assertEquals(false, (bikes as Garage.Bikes).unstable)
        val blankId = PerBikeSettingsMigration.garageOf("""[{"ssid":"ZONTES"}]""", null, false)
        assertEquals(true, (blankId as Garage.Bikes).unstable)
    }

    @Test
    fun `a corrupt or not yet converted garage moves nothing and is retried`() {
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(ownerState, Garage.Corrupt))
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(ownerState, Garage.LegacyPending))
    }

    @Test
    fun `an old profile with no saved password keeps the globals for the motorcycle paired next`() {
        // An ssid alone is still a motorcycle: never Empty, which would delete its settings.
        val garage = PerBikeSettingsMigration.garageOf(null, "ZONTES-350D", legacyPassword = false)
        assertEquals(Garage.LegacyStranded, garage)

        // loadAll() can never convert it, so this is not deferred: DONE, and nothing deleted.
        val done = runAll(ownerState, garage)
        assertEquals(ownerState.getValue(SETTINGS) + (PerBikeSettingsMigration.DONE to true), done[SETTINGS])
        assertEquals(ownerState[HANDLEBAR], done[HANDLEBAR])
        assertEquals(ownerState[CALIBRATION], done[CALIBRATION])
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(done, garage))
        // Re-paired, the Zontes reads FHD/240, Sharper, Smooth, autostart and its handlebar again.
        assertEquals(reads(ownerState, zontes.id), reads(done, zontes.id))
        assertEquals(
            AndroidAutoSize.CUSTOM.resolve(false, AndroidAutoResolutionMode.LANDSCAPE_FHD, AndroidAutoDensityMode.DPI_240),
            reads(done, zontes.id)["size"]
        )
        assertEquals(ImageQuality.SHARPER, reads(done, zontes.id)["image"])
    }

    @Test
    fun `an empty garage deletes the old globals so no future motorcycle inherits them`() {
        val done = runAll(ownerState, Garage.Empty)

        assertEquals(
            mapOf("auto_connect" to true, "logging_enabled" to true, PerBikeSettingsMigration.DONE to true),
            done[SETTINGS]
        )
        assertEquals(mapOf("managed_by_companion" to false, "defaults_ver" to 3), done[HANDLEBAR])
        assertEquals(mapOf("up_press#cfmoto" to "trackForward", "left_hold#cfmoto" to "__missing__"), done[CALIBRATION])
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(done, Garage.Empty))
    }

    @Test
    fun `an unstable id gets copies for the others but no delete and no DONE`() {
        val garage = Garage.Bikes(listOf(zontes, Bike("", "NEW-DASH", null, null)))
        val copied = runAll(ownerState, garage)

        assertEquals("LANDSCAPE_FHD", copied.getValue(SETTINGS)["android_auto_resolution#zontes"])
        assertEquals("LANDSCAPE_FHD", copied.getValue(SETTINGS)["android_auto_resolution"])
        assertEquals("SHARPER", copied.getValue(SETTINGS)["video_quality"])
        assertTrue(copied.getValue(SETTINGS).keys.none { it.endsWith("#") })
        assertEquals(null, copied.getValue(SETTINGS)[PerBikeSettingsMigration.DONE])
        // Nothing left to do until a garage save gives the new motorcycle its id.
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(copied, garage))
    }

    @Test
    fun `copies fill only absent keys, keep their types, and a second plan is empty`() {
        val state = ownerState + (SETTINGS to ownerState.getValue(SETTINGS) + ("video_power_mode#zontes" to "SAVER"))
        val garage = Garage.Bikes(listOf(zontes, second))
        val done = runAll(state, garage)
        val settings = done.getValue(SETTINGS)

        assertEquals("SAVER", settings["video_power_mode#zontes"])
        assertEquals("SMOOTH", settings["video_power_mode#cfmoto"])
        assertEquals(true, settings["autostart_enabled#cfmoto"])
        assertEquals(true, done.getValue(HANDLEBAR)["dashboard_reports_holds#zontes"])
        assertEquals("trackForward", done.getValue(CALIBRATION)["up_press#cfmoto"])
        assertEquals(emptyList<PerBikeSettingsMigration.Step>(), plan(done, garage))
        // Even with DONE removed, re-planning the migrated state finds nothing to copy.
        val rerun = done + (SETTINGS to settings - PerBikeSettingsMigration.DONE)
        assertEquals(
            listOf(PerBikeSettingsMigration.Step(SETTINGS, mapOf(PerBikeSettingsMigration.DONE to true), emptySet())),
            plan(rerun, garage)
        )
    }

    @Test
    fun `an interrupted migration resumes to the same end and never changes a read on the way`() {
        val garage = Garage.Bikes(listOf(zontes, second))
        val steps = plan(ownerState, garage)
        val uninterrupted = runAll(ownerState, garage)
        val before = listOf(zontes.id, second.id).associateWith { reads(ownerState, it) }

        for (k in 0..steps.size) {
            val crashed = steps.take(k).fold(ownerState) { state, step -> PerBikeSettingsMigration.apply(state, step) }
            listOf(zontes.id, second.id).forEach { id ->
                assertEquals("reads of $id after $k steps", before.getValue(id), reads(crashed, id))
            }
            assertEquals("resumed after $k steps", uninterrupted, runAll(crashed, garage))
        }
        assertEquals(before.getValue(second.id), reads(uninterrupted, second.id))
        // The second motorcycle keeps its own calibration; the global one fills the rest.
        assertEquals("trackForward", reads(uninterrupted, second.id)["up_press"])
        assertEquals("enter", reads(uninterrupted, second.id)["select_press"])
        assertEquals("", reads(uninterrupted, zontes.id)["left_hold"])
        assertEquals("__missing__", reads(uninterrupted, zontes.id)["right_hold"])
    }

    @Test
    fun `image and size are derived from each motorcycle's old values`() {
        fun derived(settings: Map<String, Any?>, orientation: Boolean?) =
            PerBikeSettingsMigration.derived(settings, listOf(zontes)) { orientation }

        assertEquals("SHARPER", derived(mapOf("video_quality" to "SHARPER"), null)["image_quality#zontes"])
        assertEquals("AUTOMATIC", derived(mapOf("video_quality" to "BALANCED"), null)["image_quality#zontes"])
        assertEquals("SMOOTHER", derived(mapOf("video_quality" to "SMOOTHER"), null)["image_quality#zontes"])
        assertEquals("AUTOMATIC", derived(mapOf("video_quality" to "LOUD"), null)["image_quality#zontes"])
        assertEquals("AUTOMATIC", derived(emptyMap(), null)["image_quality#zontes"])

        val fhd = mapOf("android_auto_resolution" to "LANDSCAPE_FHD", "android_auto_density" to "DPI_240")
        assertEquals("COMPACT", derived(fhd, false)["android_auto_size#zontes"])
        assertEquals("CUSTOM", derived(fhd, null)["android_auto_size#zontes"])
        assertEquals("CUSTOM", derived(fhd, true)["android_auto_size#zontes"])
        assertEquals(
            "STANDARD",
            derived(mapOf("android_auto_resolution" to "AUTO", "android_auto_density" to "AUTO"), false)["android_auto_size#zontes"]
        )
        assertEquals(
            "STANDARD",
            derived(mapOf("android_auto_resolution" to "LANDSCAPE_QHD", "android_auto_density" to "DPI_320"), false)["android_auto_size#zontes"]
        )
        // A motorcycle's own scoped value wins over the global it would otherwise inherit.
        assertEquals(
            "SMOOTHER",
            derived(mapOf("video_quality" to "SHARPER", "video_quality#zontes" to "SMOOTHER"), null)["image_quality#zontes"]
        )
        // Never over a value the motorcycle already has.
        assertEquals(null, derived(mapOf("image_quality#zontes" to "SHARPER"), null)["image_quality#zontes"])
    }

    @Test
    fun `autostart with no stored service keeps starting mirroring`() {
        val state = mapOf(SETTINGS to mapOf<String, Any?>("autostart_enabled" to true))
        val done = runAll(state, Garage.Bikes(listOf(zontes)))

        assertEquals(true, reads(done, zontes.id)["autostart_enabled"])
        assertEquals(AutostartService.MIRRORING, autostartService(reads(done, zontes.id)["autostart_service"]))
        // An invalid name is copied raw and keeps decoding to the same default.
        val invalid = runAll(mapOf(SETTINGS to mapOf<String, Any?>("autostart_service" to "TELEPORT")), Garage.Bikes(listOf(zontes)))
        assertEquals("TELEPORT", invalid.getValue(SETTINGS)["autostart_service#zontes"])
        assertEquals(AutostartService.MIRRORING, autostartService(reads(invalid, zontes.id)["autostart_service"]))
    }

    @Test
    fun `the owner's Zontes reads exactly what it read before, and a new motorcycle the defaults`() {
        val done = runAll(ownerState, Garage.Bikes(listOf(zontes)))
        val settings = done.getValue(SETTINGS)

        assertEquals(
            mapOf(
                "auto_connect" to true,
                "logging_enabled" to true,
                "android_auto_resolution#zontes" to "LANDSCAPE_FHD",
                "android_auto_density#zontes" to "DPI_240",
                "android_auto_size#zontes" to "COMPACT",
                "image_quality#zontes" to "SHARPER",
                "video_power_mode#zontes" to "SMOOTH",
                "autostart_enabled#zontes" to true,
                "autostart_service#zontes" to "ANDROID_AUTO",
                "dash_clock_sync#zontes" to true,
                PerBikeSettingsMigration.DONE to true
            ),
            settings
        )
        assertEquals(reads(ownerState, zontes.id), reads(done, zontes.id))
        assertEquals(
            mapOf(
                "managed_by_companion" to false,
                "defaults_ver" to 3,
                "enabled#zontes" to true,
                "input_mode#zontes" to "avrcp",
                "dashboard_reports_holds#zontes" to true,
                "volumeUp#zontes" to "scrollBack",
                "trackForward#zontes" to "dpadRight"
            ),
            done[HANDLEBAR]
        )

        val fresh = reads(done, "new-motorcycle")
        assertTrue(fresh.filterKeys { it !in DERIVED_READS }.values.all { it == null || it == false })
        assertEquals(AndroidAutoSize.STANDARD.resolve(false, AndroidAutoResolutionMode.AUTO, AndroidAutoDensityMode.AUTO), fresh["size"])
        assertEquals(ImageQuality.AUTOMATIC, fresh["image"])
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────

    private fun plan(state: Map<String, Map<String, Any?>>, garage: Garage) =
        PerBikeSettingsMigration.plan(state, garage, landscapeProven)

    private fun runAll(state: Map<String, Map<String, Any?>>, garage: Garage): Map<String, Map<String, Any?>> =
        plan(state, garage).fold(state) { acc, step -> PerBikeSettingsMigration.apply(acc, step) }

    /**
     * What motorcycle [id] reads through MotorcycleScope's real key resolution: every raw value
     * the migration moves, the resolved Android Auto call on its landscape dashboard, the image,
     * and whether its handlebar counts as taught (the input to gestureFor's assumptions).
     */
    private fun reads(state: Map<String, Map<String, Any?>>, id: String): Map<String, Any?> {
        fun read(file: String, base: String): Any? = state[file].orEmpty().let { all ->
            all[MotorcycleScope.readKey(base, id, all::containsKey)]
        }
        val calibration = state[CALIBRATION].orEmpty()
        val resolution = read(SETTINGS, "android_auto_resolution") as String?
        val density = read(SETTINGS, "android_auto_density") as String?
        val size = (read(SETTINGS, "android_auto_size") as String?)?.let(AndroidAutoSize::valueOf)
            ?: AndroidAutoSize.fromStoredPair(resolution, density)
        val image = (read(SETTINGS, "image_quality") as String?)?.let(ImageQuality::valueOf)
            ?: ImageQuality.fromLegacy(read(SETTINGS, "video_quality") as String?)
        return PerBikeSettingsMigration.SETTINGS_BASES.associateWith { read(SETTINGS, it) } +
            PerBikeSettingsMigration.HANDLEBAR_BASES.associateWith { read(HANDLEBAR, it) } +
            PerBikeSettingsMigration.CALIBRATION_BASES.associateWith { read(CALIBRATION, it) } +
            mapOf(
                "size" to size.resolve(
                    false,
                    AndroidAutoResolutionMode.entries.firstOrNull { it.name == resolution } ?: AndroidAutoResolutionMode.AUTO,
                    AndroidAutoDensityMode.entries.firstOrNull { it.name == density } ?: AndroidAutoDensityMode.AUTO
                ),
                "image" to image,
                "calibrated" to PhysicalPress.entries.any {
                    MotorcycleScope.scopedKey(it.id, id) in calibration || it.id in calibration
                }
            )
    }

    private fun autostartService(raw: Any?): AutostartService =
        AutostartService.entries.firstOrNull { it.name == raw } ?: AutostartService.MIRRORING

    private companion object {
        const val SETTINGS = "moto_hub_settings"
        const val HANDLEBAR = "handlebar_controls"
        const val CALIBRATION = "handlebar_calibration"
        val DERIVED_READS = setOf("size", "image")
    }
}
