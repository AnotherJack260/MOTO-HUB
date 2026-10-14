// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.settings

import android.content.Context
import io.motohub.android.R
import io.motohub.android.androidauto.AndroidAutoVideoPreset
import io.motohub.android.feature.controls.MotorcycleScope
import kotlin.math.roundToInt

enum class VideoQuality(
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int,
    private val bitrateMultiplier: Float
) {
    SMOOTHER("Smoother", "Lower bitrate, less heat and network load.", R.string.video_quality_smoother, R.string.video_quality_smoother_description, 0.7f),
    BALANCED("Balanced", "Current MOTO-HUB quality and recommended default.", R.string.video_quality_balanced, R.string.video_quality_balanced_description, 1.0f),
    SHARPER("Sharper", "Higher bitrate for crisper maps and text.", R.string.video_quality_sharper, R.string.video_quality_sharper_description, 1.6f);

    fun bitrateFor(baseBitrate: Int): Int = (baseBitrate * bitrateMultiplier).roundToInt()
}

enum class VideoPowerMode(
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val frameRate: Int
) {
    AUTO("Auto", "Adapt bitrate and frame rate to phone temperature and Wi-Fi quality.", R.string.video_power_auto, R.string.video_power_auto_description, 30),
    SMOOTH("Smooth", "30 FPS with the selected video quality.", R.string.video_power_smooth, R.string.video_power_smooth_description, 30),
    BALANCED("Balanced", "Stable 24 FPS with the selected video quality.", R.string.video_power_balanced, R.string.video_power_balanced_description, 24),
    SAVER("Saver", "20 FPS for reduced heat and battery use.", R.string.video_power_saver, R.string.video_power_saver_description, 20)
}

/**
 * The coded sources a rider can pick under Size › Custom, plus AUTO.
 *
 * Almost every source the Android Auto protocol defines: MOTO-HUB is the head unit, so whatever
 * the protocol can ask the phone for is a resolution some dashboard out there could want, and a
 * rider who knows their panel is a better judge of it than a table of the dashes we have seen.
 * What the app does NOT do is choose the unvalidated ones by itself - see
 * [AndroidAutoVideoPreset.autoSelectable]. The two QHD sources were retired (owner decision,
 * 2026-10-14); a stored QHD name now reads as Size Standard ([AndroidAutoSize.fromStoredPair]).
 */
enum class AndroidAutoResolutionMode(
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val preset: AndroidAutoVideoPreset?
) {
    AUTO(
        "Auto",
        "Keep MOTO-HUB automatic orientation based on the learned T-Box geometry.",
        R.string.android_auto_resolution_auto,
        R.string.android_auto_resolution_auto_description,
        null
    ),
    LANDSCAPE_SD(
        "Landscape 800 x 480",
        "Standard landscape Android Auto source.",
        R.string.android_auto_resolution_landscape_sd,
        R.string.android_auto_resolution_landscape_sd_description,
        AndroidAutoVideoPreset.LANDSCAPE_800X480
    ),
    LANDSCAPE_HD(
        "Landscape 1280 x 720",
        "Sharper HD landscape source with higher phone load.",
        R.string.android_auto_resolution_landscape_hd,
        R.string.android_auto_resolution_landscape_hd_description,
        AndroidAutoVideoPreset.LANDSCAPE_1280X720
    ),
    LANDSCAPE_FHD(
        "Landscape 1920 x 1080",
        "Full HD landscape source. Not validated on any known dashboard.",
        R.string.android_auto_resolution_landscape_fhd,
        R.string.android_auto_resolution_landscape_fhd_description,
        AndroidAutoVideoPreset.LANDSCAPE_1920X1080
    ),
    LANDSCAPE_UHD(
        "Landscape 3840 x 2160",
        "4K landscape source. The phone encodes and decodes 4K at once; expect heat and drops.",
        R.string.android_auto_resolution_landscape_uhd,
        R.string.android_auto_resolution_landscape_uhd_description,
        AndroidAutoVideoPreset.LANDSCAPE_3840X2160
    ),
    PORTRAIT_SD(
        "Portrait 720 x 1280",
        "Standard portrait Android Auto source.",
        R.string.android_auto_resolution_portrait_sd,
        R.string.android_auto_resolution_portrait_sd_description,
        AndroidAutoVideoPreset.PORTRAIT_720X1280
    ),
    PORTRAIT_HD(
        "Portrait 1080 x 1920",
        "Sharper HD portrait source with higher phone load.",
        R.string.android_auto_resolution_portrait_hd,
        R.string.android_auto_resolution_portrait_hd_description,
        AndroidAutoVideoPreset.PORTRAIT_1080X1920
    ),
    PORTRAIT_UHD(
        "Portrait 2160 x 3840",
        "4K portrait source. The phone encodes and decodes 4K at once; expect heat and drops.",
        R.string.android_auto_resolution_portrait_uhd,
        R.string.android_auto_resolution_portrait_uhd_description,
        AndroidAutoVideoPreset.PORTRAIT_2160X3840
    );

    /** Whether this is a source no dashboard has been seen to accept. */
    val experimental: Boolean get() = preset?.autoSelectable == false

    /** True for the landscape sources; false for AUTO and for the portrait ones. */
    val landscape: Boolean
        get() = preset != null && preset.source.width >= preset.source.height
}

/**
 * How big Android Auto draws itself on the dashboard.
 *
 * Resolution is pixels; density is size. Android Auto lays its UI out in dp, and
 * dp = px * 160 / dpi - so raising the dpi makes every button, label and map icon bigger and
 * fits less on screen, and lowering it does the opposite. The same 800x480 panel wants a
 * different answer on a 5" TFT than on a 10" one, and until now every rider got whichever single
 * value the resolution preset happened to carry.
 *
 * [AUTO] keeps exactly that value, so the default behaviour is unchanged.
 */
enum class AndroidAutoDensityMode(
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int,
    /** The dpi to advertise, or null to keep the selected resolution's own density. */
    val dpi: Int?
) {
    AUTO(
        "Auto",
        "Use the density that comes with the selected resolution.",
        R.string.android_auto_density_auto,
        R.string.android_auto_density_auto_description,
        null
    ),
    DPI_120(
        "120 dpi",
        "Smallest interface, most map on screen. Hard to read on a small TFT.",
        R.string.android_auto_density_120,
        R.string.android_auto_density_120_description,
        120
    ),
    DPI_160(
        "160 dpi",
        "Small interface. The density the landscape sources use by default.",
        R.string.android_auto_density_160,
        R.string.android_auto_density_160_description,
        160
    ),
    DPI_213(
        "213 dpi",
        "Between small and standard. Useful when 160 is too tight and 240 too coarse.",
        R.string.android_auto_density_213,
        R.string.android_auto_density_213_description,
        213
    ),
    DPI_240(
        "240 dpi",
        "Standard interface. The density the portrait sources use by default.",
        R.string.android_auto_density_240,
        R.string.android_auto_density_240_description,
        240
    ),
    DPI_320(
        "320 dpi",
        "Large interface, easier to read and to hit with gloves. Less map on screen.",
        R.string.android_auto_density_320,
        R.string.android_auto_density_320_description,
        320
    ),
    DPI_480(
        "480 dpi",
        "Largest interface. Only sensible on the high-resolution sources.",
        R.string.android_auto_density_480,
        R.string.android_auto_density_480_description,
        480
    )
}

/**
 * Whether the motorcycle's explicit TFT safe margins are also advertised to Android Auto.
 * The negotiated T-Box VideoArea is already the projection canvas and never creates AA margins.
 */
enum class AndroidAutoAspectMatchingMode(
    val label: String,
    val description: String,
    val labelRes: Int,
    val descriptionRes: Int
) {
    AUTO(
        "Auto",
        "Match Android Auto's layout to the dashboard's real shape, so the map fills the panel " +
            "instead of sitting between black bars.",
        R.string.android_auto_insets_auto,
        R.string.android_auto_insets_auto_description
    ),
    MANUAL(
        "Manual",
        "Advertise the per-motorcycle TFT Safe Margins to Android Auto.",
        R.string.android_auto_insets_manual,
        R.string.android_auto_insets_manual_description
    )
}

/**
 * How big Android Auto draws on one motorcycle's dashboard. The dashboard decides the orientation;
 * the rider only picks the size. [STANDARD] is exactly the old AUTO resolution at AUTO density,
 * and [CUSTOM] replays the stored [AndroidAutoResolutionMode] and [AndroidAutoDensityMode] as
 * they always were. Read when Android Auto starts, so a change applies on the next start.
 */
enum class AndroidAutoSize {
    COMPACT, STANDARD, LARGE, CUSTOM;

    /** The override preset and dpi handed to AndroidAutoCapabilityProfiles.select; (null, null) is AUTO. */
    fun resolve(
        portrait: Boolean,
        custom: AndroidAutoResolutionMode,
        customDpi: AndroidAutoDensityMode
    ): Pair<AndroidAutoVideoPreset?, Int?> = when (this) {
        // 1920x1080 at 240 dpi: the layout of 1280x720 at 160, drawn sharper. The owner's setup.
        COMPACT -> (if (portrait) AndroidAutoVideoPreset.PORTRAIT_1080X1920 else AndroidAutoVideoPreset.LANDSCAPE_1920X1080) to 240
        STANDARD -> null to null
        // ponytail: unvalidated on a bike (a 640x290 dp layout on a 1024x464 dash); becomes
        // 1280x720 at 240 if Android Auto crops or refuses it.
        LARGE -> (if (portrait) AndroidAutoVideoPreset.PORTRAIT_720X1280 else AndroidAutoVideoPreset.LANDSCAPE_1280X720) to 320
        CUSTOM -> custom.preset to customDpi.dpi
    }

    companion object {
        /** Retired from the resolution list. A rider on one gets today's AUTO, not SD at its dpi. */
        private val RETIRED_RESOLUTIONS = setOf("LANDSCAPE_QHD", "PORTRAIT_QHD")

        /**
         * The Size a motorcycle with no stored Size reads, from its raw resolution and density
         * names: AUTO/AUTO (or nothing) and a retired QHD name are Standard, anything else is
         * Custom, which replays the stored pair exactly. So the session never changes, migrated
         * or not.
         */
        fun fromStoredPair(rawRes: String?, rawDpi: String?): AndroidAutoSize = when {
            rawRes in RETIRED_RESOLUTIONS -> STANDARD
            (rawRes == null || rawRes == "AUTO") && (rawDpi == null || rawDpi == "AUTO") -> STANDARD
            else -> CUSTOM
        }

        /**
         * Migration only: a named size when the stored pair is exactly that size's pair for a
         * dashboard whose orientation is proven ([portrait] non-null), else [fromStoredPair].
         */
        fun fromLegacy(rawRes: String?, rawDpi: String?, portrait: Boolean?): AndroidAutoSize =
            portrait?.let { proven ->
                listOf(COMPACT, LARGE).firstOrNull { it.storedPair(proven) == (rawRes to rawDpi) }
            } ?: fromStoredPair(rawRes, rawDpi)

        /** The resolution and density names whose Custom pair resolves the same as [this]. */
        private fun AndroidAutoSize.storedPair(portrait: Boolean): Pair<String?, String?> {
            val (preset, dpi) = resolve(portrait, AndroidAutoResolutionMode.AUTO, AndroidAutoDensityMode.AUTO)
            return AndroidAutoResolutionMode.entries.firstOrNull { it.preset == preset }?.name to
                AndroidAutoDensityMode.entries.firstOrNull { it.dpi == dpi }?.name
        }
    }
}

/**
 * The picture one motorcycle streams. Sharper and Smoother are the top and the bottom of
 * Automatic's bitrate range, held fixed ([VideoQuality] keeps the multipliers).
 */
enum class ImageQuality {
    AUTOMATIC, SHARPER, SMOOTHER;

    companion object {
        /** The old video_quality: Sharper and Smoother keep their meaning, anything else is Automatic. */
        fun fromLegacy(raw: String?): ImageQuality = when (raw) {
            VideoQuality.SHARPER.name -> SHARPER
            VideoQuality.SMOOTHER.name -> SMOOTHER
            else -> AUTOMATIC
        }
    }
}

/**
 * The screen MOTO-HUB puts on the TFT by itself once the motorcycle link comes up, when
 * autostart is enabled.
 *
 * [RIDE_DASHBOARD] exists only in Advanced - Core ships Mirror and Android Auto - so Core's
 * settings never offers it and Core's autostart refuses it if the stored value somehow names it.
 */
enum class AutostartService(
    val label: String,
    val description: String,
    val advancedOnly: Boolean = false
) {
    MIRRORING(
        "Mirroring",
        "Share the phone screen. Android still asks for screen-capture permission each time.",
    ),
    ANDROID_AUTO(
        "Android Auto",
        "Launch Android Auto on the dashboard."
    ),
    RIDE_DASHBOARD(
        "Ride Dashboard",
        "Render the native dashboard, in the mode selected below.",
        advancedOnly = true
    )
}

enum class DistanceUnits(val label: String, val description: String, val labelRes: Int, val descriptionRes: Int) {
    KILOMETERS("Kilometers", "Distances and speeds in km and km/h.", R.string.distance_units_kilometers, R.string.distance_units_kilometers_description),
    MILES("Miles", "Distances and speeds in mi and mph.", R.string.distance_units_miles, R.string.distance_units_miles_description)
}

enum class RoutePreference(val label: String, val description: String, val labelRes: Int, val descriptionRes: Int) {
    FASTEST("Fastest", "Quickest route, motorways allowed.", R.string.route_preference_fastest, R.string.route_preference_fastest_description),
    SCENIC("Scenic", "Bias toward back roads and away from motorways.", R.string.route_preference_scenic, R.string.route_preference_scenic_description)
}

/**
 * App settings, plus one motorcycle's settings.
 *
 * The per-motorcycle ones (Android Auto size and fit, touch, image, frame rate, the Advanced
 * overrides, autostart, the clocks, Wi-Fi Direct) are stored as `"<key>#<motorcycleId>"` and read
 * through [MotorcycleScope]: an explicit [io.motohub.android.feature.controls.MotorcycleContext]
 * first, else the live session's motorcycle, else the garage's active one. Pass the caller's
 * context through as it is - never `applicationContext`, which would drop that wrapper.
 */
object MotoHubSettings {
    // Internal: PerBikeSettingsMigration moves the per-motorcycle ones and needs their names.
    internal const val PREFERENCES = "moto_hub_settings"
    /** Legacy, read only to derive [KEY_IMAGE_QUALITY] until the migration deletes it. */
    internal const val KEY_VIDEO_QUALITY = "video_quality"
    internal const val KEY_IMAGE_QUALITY = "image_quality"
    internal const val KEY_VIDEO_POWER_MODE = "video_power_mode"
    internal const val KEY_DISABLE_TOUCHSCREEN = "disable_touchscreen"
    private const val KEY_SEAMLESS_RESUME = "seamless_resume"
    internal const val KEY_ANDROID_AUTO_SIZE = "android_auto_size"
    internal const val KEY_ANDROID_AUTO_RESOLUTION = "android_auto_resolution"
    internal const val KEY_ANDROID_AUTO_DENSITY = "android_auto_density"
    internal const val KEY_ANDROID_AUTO_ASPECT_MATCHING = "android_auto_aspect_matching"
    private const val KEY_AUTO_CONNECT = "auto_connect"
    internal const val KEY_AUTOSTART_ENABLED = "autostart_enabled"
    internal const val KEY_AUTOSTART_SERVICE = "autostart_service"
    private const val KEY_AUTO_RECOVERY = "auto_recovery"
    internal const val KEY_KEEP_WIFI_DIRECT_ON_DISCONNECT = "keep_wifi_direct_on_disconnect"
    internal const val KEY_BLUETOOTH_CLOCK_SYNC = "bluetooth_clock_sync"
    internal const val KEY_DASH_CLOCK_SYNC = "dash_clock_sync"
    private const val KEY_BASE_BITRATE = "base_bitrate_bps"
    private const val KEY_KEYFRAME_INTERVAL = "keyframe_interval_s"
    private const val KEY_CARBIT_EXACT = "carbit_exact"
    private const val KEY_AUTO_RECORD_TRIPS = "auto_record_trips"
    private const val KEY_SHOW_RECORDED_TRACK = "show_recorded_track_on_dashboard"
    private const val KEY_DISTANCE_UNITS = "distance_units"
    private const val KEY_ROUTE_PREFERENCE = "route_preference"
    private const val KEY_NAV_VOICE_ENABLED = "nav_voice_enabled"
    private const val KEY_USE_DEMO_ROUTING_SERVER = "use_demo_routing_server"
    private const val KEY_SAFETY_DISCLAIMER_ACKNOWLEDGED = "safety_disclaimer_acknowledged"
    private const val KEY_PERMISSION_PRIMER_SEEN = "permission_primer_seen"
    private const val KEY_VERBOSE_TBOX_LOGGING = "verbose_tbox_logging"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"

    fun distanceUnits(context: Context): DistanceUnits = enumPreference(
        context = context,
        key = KEY_DISTANCE_UNITS,
        default = DistanceUnits.KILOMETERS
    )

    fun setDistanceUnits(context: Context, units: DistanceUnits) {
        preferences(context).edit().putString(KEY_DISTANCE_UNITS, units.name).apply()
    }

    fun routePreference(context: Context): RoutePreference = enumPreference(
        context = context,
        key = KEY_ROUTE_PREFERENCE,
        default = RoutePreference.FASTEST
    )

    fun setRoutePreference(context: Context, preference: RoutePreference) {
        preferences(context).edit().putString(KEY_ROUTE_PREFERENCE, preference.name).apply()
    }

    fun navVoiceEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_NAV_VOICE_ENABLED, true)

    fun setNavVoiceEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_NAV_VOICE_ENABLED, enabled).apply()
    }

    /**
     * Route with FOSSGIS's free public Valhalla demo server instead of
     * requiring a personal Stadia Maps key. Only takes effect while no key is
     * configured - see the advanced routing implementation.
     */
    fun useDemoRoutingServer(context: Context): Boolean =
        preferences(context).getBoolean(KEY_USE_DEMO_ROUTING_SERVER, false)

    fun setUseDemoRoutingServer(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_USE_DEMO_ROUTING_SERVER, enabled).apply()
    }

    /** Per motorcycle. Absent, it is derived from the motorcycle's old video_quality. */
    fun imageQuality(context: Context): ImageQuality =
        scopedEnumOrNull<ImageQuality>(context, KEY_IMAGE_QUALITY)
            ?: ImageQuality.fromLegacy(scopedString(context, KEY_VIDEO_QUALITY))

    fun setImageQuality(context: Context, quality: ImageQuality) {
        putScoped(context, KEY_IMAGE_QUALITY, quality.name)
    }

    /** Per motorcycle: the frame-rate cap (Advanced › Frame rate). */
    fun videoPowerMode(context: Context): VideoPowerMode =
        scopedEnum(context, KEY_VIDEO_POWER_MODE, VideoPowerMode.AUTO)

    fun setVideoPowerMode(context: Context, mode: VideoPowerMode) {
        putScoped(context, KEY_VIDEO_POWER_MODE, mode.name)
    }

    /**
     * Per motorcycle, Advanced › Base bitrate in bps: the value Sharper and Smoother scale; null
     * is Auto (the Carbit plan, the dashboard profile, or the negotiated rate).
     */
    fun baseBitrateOverride(context: Context): Int? = scopedInt(context, KEY_BASE_BITRATE)

    fun setBaseBitrateOverride(context: Context, bitsPerSecond: Int?) {
        putScopedOrRemove(context, KEY_BASE_BITRATE, bitsPerSecond?.toString())
    }

    /** Per motorcycle, Advanced › I-frame interval in seconds, 0 for every frame; null is Auto. */
    fun keyframeIntervalOverride(context: Context): Int? = scopedInt(context, KEY_KEYFRAME_INTERVAL)

    fun setKeyframeIntervalOverride(context: Context, seconds: Int?) {
        putScopedOrRemove(context, KEY_KEYFRAME_INTERVAL, seconds?.toString())
    }

    /**
     * Per motorcycle, Advanced › Carbit-exact media; null is Auto (the dashboard profile's own
     * carbitExactVideo). It moves the Carbit encoder plan with it: a dash answered Carbit's way is
     * sent Carbit's picture size.
     */
    fun carbitExactOverride(context: Context): Boolean? = when (scopedString(context, KEY_CARBIT_EXACT)) {
        CARBIT_ON -> true
        CARBIT_OFF -> false
        else -> null
    }

    fun setCarbitExactOverride(context: Context, exact: Boolean?) {
        putScopedOrRemove(context, KEY_CARBIT_EXACT, exact?.let { if (it) CARBIT_ON else CARBIT_OFF })
    }

    private const val CARBIT_ON = "on"
    private const val CARBIT_OFF = "off"

    /**
     * Per motorcycle: force Android Auto to use focus/handlebar controls instead of advertising
     * touch input.
     */
    fun disableTouchscreen(context: Context): Boolean =
        scopedBoolean(context, KEY_DISABLE_TOUCHSCREEN, false)

    fun setDisableTouchscreen(context: Context, enabled: Boolean) {
        putScoped(context, KEY_DISABLE_TOUCHSCREEN, enabled)
    }

    fun seamlessResume(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SEAMLESS_RESUME, false)

    fun setSeamlessResume(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_SEAMLESS_RESUME, enabled).apply()
    }

    /**
     * Per motorcycle. Absent, it is derived from the motorcycle's raw resolution and density
     * (which still fall back to the pre-garage globals until the migration removes them), so the
     * session is the same whether or not the migration has run.
     */
    fun androidAutoSize(context: Context): AndroidAutoSize =
        scopedEnumOrNull<AndroidAutoSize>(context, KEY_ANDROID_AUTO_SIZE)
            ?: AndroidAutoSize.fromStoredPair(
                scopedString(context, KEY_ANDROID_AUTO_RESOLUTION),
                scopedString(context, KEY_ANDROID_AUTO_DENSITY)
            )

    fun setAndroidAutoSize(context: Context, size: AndroidAutoSize) {
        putScoped(context, KEY_ANDROID_AUTO_SIZE, size.name)
    }

    /** Per motorcycle, used only when [androidAutoSize] is Custom. */
    fun androidAutoResolution(context: Context): AndroidAutoResolutionMode =
        scopedEnum(context, KEY_ANDROID_AUTO_RESOLUTION, AndroidAutoResolutionMode.AUTO)

    fun setAndroidAutoResolution(context: Context, mode: AndroidAutoResolutionMode) {
        putScoped(context, KEY_ANDROID_AUTO_RESOLUTION, mode.name)
    }

    /** Per motorcycle, used only when [androidAutoSize] is Custom. */
    fun androidAutoDensity(context: Context): AndroidAutoDensityMode =
        scopedEnum(context, KEY_ANDROID_AUTO_DENSITY, AndroidAutoDensityMode.AUTO)

    fun setAndroidAutoDensity(context: Context, mode: AndroidAutoDensityMode) {
        putScoped(context, KEY_ANDROID_AUTO_DENSITY, mode.name)
    }

    /** Per motorcycle: Fit. AUTO is "Fill the screen", MANUAL is "Show everything". */
    fun androidAutoAspectMatching(context: Context): AndroidAutoAspectMatchingMode =
        scopedEnum(context, KEY_ANDROID_AUTO_ASPECT_MATCHING, AndroidAutoAspectMatchingMode.AUTO)

    fun setAndroidAutoAspectMatching(context: Context, mode: AndroidAutoAspectMatchingMode) {
        putScoped(context, KEY_ANDROID_AUTO_ASPECT_MATCHING, mode.name)
    }

    fun autoConnect(context: Context): Boolean = preferences(context).getBoolean(KEY_AUTO_CONNECT, false)

    fun setAutoConnect(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }

    /**
     * Put the configured service on the TFT as soon as the motorcycle link is up, instead of
     * leaving the rider on the mode picker. Off by default: which screen a rider wants is a
     * personal choice, and starting one uninvited would take over their dashboard.
     *
     * Per motorcycle ("On arrival"), with [autostartService].
     */
    fun autostartEnabled(context: Context): Boolean =
        scopedBoolean(context, KEY_AUTOSTART_ENABLED, false)

    fun setAutostartEnabled(context: Context, enabled: Boolean) {
        putScoped(context, KEY_AUTOSTART_ENABLED, enabled)
    }

    /** MIRRORING by default, as it always was: a motorcycle with autostart on and no stored service
     *  must keep starting Mirroring. */
    fun autostartService(context: Context): AutostartService =
        scopedEnum(context, KEY_AUTOSTART_SERVICE, AutostartService.MIRRORING)

    fun setAutostartService(context: Context, service: AutostartService) {
        putScoped(context, KEY_AUTOSTART_SERVICE, service.name)
    }

    /** Recovery is enabled by default so a transient T-Box transport failure does not end a ride. */
    fun autoRecovery(context: Context): Boolean = preferences(context).getBoolean(KEY_AUTO_RECOVERY, true)

    fun setAutoRecovery(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_AUTO_RECOVERY, enabled).apply()
    }

    /**
     * Skip releasing the Wi-Fi Direct group when disconnecting from a bike that uses it, instead
     * of the default eager teardown. Off by default: staying associated to the dash's network has
     * a real cost (no other network usable over that link until the group is actually torn down,
     * i.e. Wi-Fi off or the app fully exits), so only a rider whose dash needs this should pay it.
     * See [io.motohub.android.tbox.TBoxLink.WifiDirect]. Per motorcycle.
     */
    fun keepWifiDirectAfterDisconnect(context: Context): Boolean =
        scopedBoolean(context, KEY_KEEP_WIFI_DIRECT_ON_DISCONNECT, false)

    fun setKeepWifiDirectAfterDisconnect(context: Context, enabled: Boolean) {
        putScoped(context, KEY_KEEP_WIFI_DIRECT_ON_DISCONNECT, enabled)
    }

    /**
     * Answer the dashboard's clock questions over Bluetooth as well as over Wi-Fi.
     *
     * Off by default and deliberately opt-in: the protocol runs on generic serial-over-BLE service
     * UUIDs that intercoms, OBD dongles and tyre sensors also use, so this is not something to
     * switch on for riders who are not chasing a wrong dash clock. Even when enabled,
     * [io.motohub.android.tbox.EcBtpTimeLink] never transmits to a device until that device has
     * sent a valid frame of the protocol. Per motorcycle.
     */
    fun bluetoothClockSync(context: Context): Boolean =
        scopedBoolean(context, KEY_BLUETOOTH_CLOCK_SYNC, false)

    fun setBluetoothClockSync(context: Context, enabled: Boolean) {
        putScoped(context, KEY_BLUETOOTH_CLOCK_SYNC, enabled)
    }

    /**
     * Tell the dashboard the wall-clock time over Wi-Fi when it asks, and when it does not ask.
     *
     * On by default, because that answer is what sets the clock on every dash that reads it, and
     * an unanswered clock question is what left Morini X-Cape and Voge clusters at 01.01.1970 in
     * the first place.
     *
     * It exists as a switch because one Voge panel class does the opposite. Two SSDQ01-0120 units
     * are indistinguishable by firmware string - same flavor 51, same channel 37501, same
     * version_name - and behave in opposite ways: one accepts the reply and keeps a real date,
     * the other asks, is answered, discards it, and shows 01.01.1970 anyway. Writing a clock into
     * the second kind does not fix it and can overwrite a time its rider set by hand on the
     * dashboard itself, which then goes back to epoch on every connect.
     *
     * Turning this off makes the daemon answer the clock question with an empty body and never
     * push the time unasked ([io.motohub.android.tbox.RideDaemonTransport] maps it to
     * `MobileConfig.SkipDashClockSync`). The handshake still completes; the dash is simply not
     * told a time. Only turn it off for a dash that asks for the time and ignores it - a dash that
     * never asks at all is the case the daemon now handles on its own. Per motorcycle.
     */
    fun dashClockSync(context: Context): Boolean =
        scopedBoolean(context, KEY_DASH_CLOCK_SYNC, true)

    fun setDashClockSync(context: Context, enabled: Boolean) {
        putScoped(context, KEY_DASH_CLOCK_SYNC, enabled)
    }

    /**
     * Extra T-Box protocol diagnostics: full CLIENT_INFO JSON, every candidate
     * profile's CLIENT_INFO score (not just the winner), hex dumps of
     * unrecognized PXC/media-control commands, and Wi-Fi link quality at
     * connect time. Off by default - this is meaningfully more verbose and
     * more expensive to format than the app's normal event log, and only
     * useful when a rider is actively helping diagnose a connection problem.
     */
    // On by default (2026-07-30): a connection problem's first occurrence is the one that
    // matters, and with verbose off it produced a log with the diagnosis missing - riders had
    // to reproduce the failure after finding this switch. The stable identifiers that once
    // justified defaulting off (HUID/uuid in the raw CLIENT_INFO) are redacted since the same
    // change. A rider who turns it off stays off - this is a default, not an override.
    fun verboseTBoxLogging(context: Context): Boolean =
        cachedBoolean(context, KEY_VERBOSE_TBOX_LOGGING, true)

    fun setVerboseTBoxLogging(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_VERBOSE_TBOX_LOGGING, enabled).apply()
    }

    /**
     * Master switch for [io.motohub.android.session.ProjectionEventLog]: when false, nothing
     * is recorded to Logcat, memory, or the diagnostic log file at all - not just the verbose
     * extras. On by default, since the base (non-verbose) log is what most troubleshooting in
     * this app has relied on and is not the heavy part; this exists for riders who want to
     * turn logging off entirely rather than just dial back its verbosity.
     */
    fun loggingEnabled(context: Context): Boolean =
        cachedBoolean(context, KEY_LOGGING_ENABLED, true)

    fun setLoggingEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
    }

    fun autoRecordTrips(context: Context): Boolean =
        preferences(context).getBoolean(KEY_AUTO_RECORD_TRIPS, true)

    fun setAutoRecordTrips(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_AUTO_RECORD_TRIPS, enabled).apply()
    }

    fun showRecordedTrackOnDashboard(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SHOW_RECORDED_TRACK, true)

    fun setShowRecordedTrackOnDashboard(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_SHOW_RECORDED_TRACK, enabled).apply()
    }

    /** True after the rider chose not to see the startup safety warning again. */
    fun safetyDisclaimerAcknowledged(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SAFETY_DISCLAIMER_ACKNOWLEDGED, false)

    fun setSafetyDisclaimerAcknowledged(context: Context, acknowledged: Boolean) {
        preferences(context).edit().putBoolean(KEY_SAFETY_DISCLAIMER_ACKNOWLEDGED, acknowledged).apply()
    }

    /** True once the rider has tapped Continue on the sheet that explains the first stream's prompts. */
    fun permissionPrimerSeen(context: Context): Boolean =
        preferences(context).getBoolean(KEY_PERMISSION_PRIMER_SEEN, false)

    fun setPermissionPrimerSeen(context: Context, seen: Boolean) {
        preferences(context).edit().putBoolean(KEY_PERMISSION_PRIMER_SEEN, seen).apply()
    }


    private inline fun <reified T : Enum<T>> enumPreference(
        context: Context,
        key: String,
        default: T
    ): T {
        val stored = preferences(context).getString(key, default.name)
        return enumValues<T>().firstOrNull { it.name == stored } ?: default
    }

    // ── per-motorcycle values ────────────────────────────────────────────────────────────────
    //
    // Scoped through MotorcycleScope with the CALLER's context, so an explicit MotorcycleContext
    // reaches it. An unknown stored name falls back to that one key's default, nothing else.

    private fun scopedString(context: Context, key: String): String? =
        MotorcycleScope.getString(preferences(context), context, key, null)

    private inline fun <reified T : Enum<T>> scopedEnumOrNull(context: Context, key: String): T? =
        scopedString(context, key)?.let { s -> enumValues<T>().firstOrNull { it.name == s } }

    private inline fun <reified T : Enum<T>> scopedEnum(context: Context, key: String, default: T): T =
        scopedEnumOrNull<T>(context, key) ?: default

    private fun scopedBoolean(context: Context, key: String, default: Boolean): Boolean =
        MotorcycleScope.getBoolean(preferences(context), context, key, default)

    private fun scopedInt(context: Context, key: String): Int? = scopedString(context, key)?.toIntOrNull()

    private fun putScoped(context: Context, key: String, value: String) {
        MotorcycleScope.putString(preferences(context), context, key, value)
    }

    private fun putScoped(context: Context, key: String, value: Boolean) {
        MotorcycleScope.putBoolean(preferences(context), context, key, value)
    }

    /** Null drops this motorcycle's value, which reads as Auto. */
    private fun putScopedOrRemove(context: Context, key: String, value: String?) {
        if (value == null) {
            MotorcycleScope.remove(preferences(context), context, key)
        } else {
            putScoped(context, key, value)
        }
    }

    private fun preferences(context: Context) = context.applicationContext.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    // ── cached flags ─────────────────────────────────────────────────────────────────────────
    //
    // SharedPreferences serves reads from memory, but every call still takes its internal lock
    // and hashes the key - and these two flags are read on the hottest paths in the app: the
    // logging master switch on EVERY ProjectionEventLog.record(), and the verbose flag on every
    // T-Box protocol event, touch moves included. The values change only when the rider flips a
    // switch, so they are cached and invalidated by the preference change itself. Anything
    // read once per session does NOT belong here - the plain accessors stay the default.

    private val flagCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * Held in a field on purpose: SharedPreferences keeps only a WEAK reference to its listeners,
     * so a listener that is not referenced anywhere is collected and the cache silently goes
     * stale - a flag the rider turned off would keep reading as on until the process restarts.
     */
    private val cacheInvalidation =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null) flagCache.clear() else flagCache.remove(key)
        }

    @Volatile
    private var cacheInvalidationRegistered = false

    private fun cachedBoolean(context: Context, key: String, default: Boolean): Boolean {
        flagCache[key]?.let { return it }
        val preferences = preferences(context)
        if (!cacheInvalidationRegistered) {
            synchronized(flagCache) {
                if (!cacheInvalidationRegistered) {
                    preferences.registerOnSharedPreferenceChangeListener(cacheInvalidation)
                    cacheInvalidationRegistered = true
                }
            }
        }
        val value = preferences.getBoolean(key, default)
        flagCache[key] = value
        return value
    }
}
