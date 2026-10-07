// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android

import io.motohub.android.i18n.motoHubText

import android.app.Activity
import android.app.ActivityManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.Intent
import android.hardware.usb.UsbManager
import android.net.Uri
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import io.motohub.android.ui.components.MotoHubSnackbar
import io.motohub.android.ui.components.MhChoiceRow
import io.motohub.android.ui.components.MhListRow
import io.motohub.android.ui.components.MhModals
import io.motohub.android.ui.components.MhSheet
import io.motohub.android.ui.components.MhSnackTone
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import io.motohub.android.aa.AaSelfMode
import io.motohub.android.androidauto.PhoneOnlyAndroidAutoLaunchRequest
import io.motohub.android.androidauto.AndroidAutoRuntime
import io.motohub.android.androidauto.AndroidAutoSelfModeHelp
import io.motohub.android.androidauto.AndroidAutoRuntimeState
import io.motohub.android.androidauto.AndroidAutoSessionService
import io.motohub.android.androidauto.AndroidAutoDisplayMode
import io.motohub.android.androidauto.AndroidAutoDisplayModeStore
import io.motohub.android.feature.controls.BluetoothStatus
import io.motohub.android.feature.controls.MediaButtonBridge
import io.motohub.android.ipc.IpcBridgeContract
import io.motohub.android.androidauto.TBoxDisplayGeometryStore
import io.motohub.android.androidauto.TBoxScreenMargins
import io.motohub.android.androidauto.TBoxScreenMarginsStore
import io.motohub.android.data.MotorcyclePhotoStore
import io.motohub.android.data.MotorcycleProfileStore
import io.motohub.android.session.AutoConnectDecision
import io.motohub.android.session.autoConnectDecision
import io.motohub.android.session.dashReachable
import io.motohub.android.session.MotorcycleProfile
import io.motohub.android.tbox.ThinkerRideGate
import io.motohub.android.feature.about.AboutScreen
import io.motohub.android.feature.garage.GarageTabContent
import io.motohub.android.feature.garage.MotorcycleDetailsScreen
import io.motohub.android.feature.garage.MotorcyclePhoto
import io.motohub.android.feature.garage.MotorcyclePhotoSource
import io.motohub.android.feature.garage.TBoxCapabilityScreen
import io.motohub.android.feature.garage.shownName
import io.motohub.android.feature.home.HubHomeScreen
import io.motohub.android.feature.home.HubViewModel
import io.motohub.android.feature.home.WireNeedsAndroidAutoDialog
import io.motohub.android.feature.home.WireVerdictDialog
import io.motohub.android.feature.androidauto.AndroidAutoHelpScreen
import io.motohub.android.feature.androidauto.AndroidAutoPreviewScreen
import io.motohub.android.feature.androidauto.CompanionConflictGateDialog
import io.motohub.android.feature.androidauto.rememberCompanionConflictGate
import io.motohub.android.feature.controls.HandlebarTeachPrerequisiteRequest
import io.motohub.android.feature.diagnostics.BleExplorerScreen
import io.motohub.android.feature.diagnostics.ClockLabScreen
import io.motohub.android.feature.diagnostics.ClockLabViewModel
import io.motohub.android.feature.diagnostics.NetworkDiagnosticsScreen
import io.motohub.android.feature.diagnostics.NetworkDiagnosticsViewModel
import io.motohub.android.feature.diagnostics.ApplicationLogScreen
import io.motohub.android.feature.diagnostics.DiagnosticLogShare
import io.motohub.android.feature.pairing.ManualPairingScreen
import io.motohub.android.feature.pairing.TBoxQrOrigin
import io.motohub.android.feature.pairing.TBoxQrPayload
import io.motohub.android.feature.pairing.TBoxQrPhotoDecoder
import io.motohub.android.feature.pairing.QrImageSource
import io.motohub.android.feature.pairing.QrImportSheet
import io.motohub.android.feature.pairing.TBoxQrScannerScreen
import io.motohub.android.tbox.WifiDirectGate
import io.motohub.android.feature.pairing.UnverifiedQrDialog
import io.motohub.android.feature.safety.SafetyDisclaimerDialog
import io.motohub.android.feature.settings.AutostartService
import io.motohub.android.feature.settings.MotoHubSettings
import io.motohub.android.feature.settings.SettingsTabContent
import io.motohub.android.session.ProjectionSessionService
import io.motohub.android.feature.diagnostics.report.CrashDiagnosticsConsentDialog
import io.motohub.android.feature.diagnostics.report.DiagnosticReportScheduler
import io.motohub.android.feature.diagnostics.report.PrivacyNoticeDialog
import io.motohub.android.session.CrashRecovery
import io.motohub.android.session.ProjectionEventLog
import io.motohub.android.session.ProjectionRuntime
import io.motohub.android.session.PhoneDisplayDimmer
import io.motohub.android.session.PhoneDisplayDimPreferences
import io.motohub.android.session.SessionPhase
import io.motohub.android.externaldisplay.AoaAccessoryRuntime
import io.motohub.android.externaldisplay.AoaExternalRuntime
import io.motohub.android.externaldisplay.AoaExternalRuntimeState
import io.motohub.android.externaldisplay.AoaExternalService
import io.motohub.android.tbox.TBoxCapabilityStore
import io.motohub.android.tbox.TBoxModelProfile
import io.motohub.android.tbox.TBoxPortScanResult
import io.motohub.android.tbox.TBoxPortScanner
import io.motohub.android.tbox.TBoxScanPermissions
import io.motohub.android.tbox.CompanionAppRegistry
import io.motohub.android.tbox.WifiGate
import io.motohub.android.ui.components.HubScreenKey
import io.motohub.android.ui.components.HubScreenTransition
import io.motohub.android.ui.components.HubTab
import io.motohub.android.ui.theme.MotoHubTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

private val TBoxScreenMarginsSaver = listSaver<TBoxScreenMargins, Int>(
    save = { margins -> listOf(margins.top, margins.bottom, margins.left, margins.right) },
    restore = { values ->
        TBoxScreenMargins(
            top = values[0],
            bottom = values[1],
            left = values[2],
            right = values[3]
        )
    }
)

/** What the permission primer explains, and the start it continues. */
private class PermissionPrimer(val notifications: Boolean, val microphone: Boolean, val start: () -> Unit)

private fun applyPhoneOnlyAndroidAutoDisplayMode(context: Context, displayMode: String?) {
    val mode = displayMode?.let { runCatching { AndroidAutoDisplayMode.valueOf(it) }.getOrNull() } ?: return
    AndroidAutoDisplayModeStore(context).save(
        MotorcycleProfile(
            ssid = "phone-only-android-auto",
            password = "",
            id = "phone-only-android-auto"
        ),
        mode
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: HubViewModel by viewModels()
    private val diagnosticsViewModel: NetworkDiagnosticsViewModel by viewModels()
    private val clockLabViewModel: ClockLabViewModel by viewModels()
   private val androidAutoLaunchPending = AtomicBoolean(false)
    private val androidAutoPhoneOnlyBridge by lazy {
        io.motohub.android.androidauto.createAndroidAutoPhoneOnlyBridge(applicationContext)
    }

    /**
     * Starts a connection only once Android is willing to accept the Wi-Fi request behind it.
     *
     * `WifiNetworkFactory` drops a `WifiNetworkSpecifier` request coming from a process that is
     * neither a foreground app nor a foreground service, and answers `onUnavailable` a few tens
     * of milliseconds later. The join never happens, and the verdict reads exactly like a dash
     * that stopped broadcasting - riders were re-scanning QR codes over a race they could not
     * see. The race is real: the runtime permission sheet leaves this activity PAUSED for a few
     * frames after its result callback runs, so a first pairing (the one case that always asks
     * for permissions) connected from a background process every single time.
     *
     * The gate is the importance the platform itself reads, not the lifecycle state alone -
     * during mirroring the projection foreground service legitimately carries the request with
     * this activity in the background, and that path must keep working untouched.
     */
    private fun connectWhenAndroidAccepts(reason: String) {
        if (isForegroundEnoughForWifiRequest()) {
            viewModel.connectAndDiscover()
            return
        }
        ProjectionEventLog.record(
            "CONNECTION",
            "Connect ($reason) deferred: MOTO-HUB is not in the foreground yet " +
                "(importance=${processImportance()}); Android would refuse the Wi-Fi request."
        )
        lifecycleScope.launch {
            withResumed {}
            // Process importance trails the resume callback by a frame or two on some builds.
            var waited = 0L
            while (!isForegroundEnoughForWifiRequest() && waited < FOREGROUND_SETTLE_TIMEOUT_MS) {
                delay(FOREGROUND_SETTLE_POLL_MS)
                waited += FOREGROUND_SETTLE_POLL_MS
            }
            ProjectionEventLog.record(
                "CONNECTION",
                "Running the deferred connect ($reason) ${waited}ms after the resume; " +
                    "importance=${processImportance()}."
            )
            viewModel.connectAndDiscover()
        }
    }

    private fun processImportance(): Int {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance
    }

    /** Mirrors AOSP's `WifiNetworkFactory.isRequestFromForegroundAppOrService`. */
    private fun isForegroundEnoughForWifiRequest(): Boolean =
        processImportance() <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE

    private val profileTrialDiagnostics by lazy {
        io.motohub.android.feature.diagnostics.report.ProfileTrialDiagnosticsOffer.createOrNull(applicationContext)
    }

    /**
     * Registered as a field, before STARTED, because that is what the Activity Result API
     * requires - and because this request can arrive on a cold launch whose only purpose it is.
     *
     * Always finishes. An activity the companion app opened to ask one question has nothing to
     * show once it is answered, and a rider who tapped a button over there should be looking at
     * that button again - including when the answer is no, which is also what a rider sees once
     * Android has stopped showing the dialog after two refusals. The card they came from is
     * still up, with the way to system settings on it.
     */
    private val handlebarBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        ProjectionEventLog.record(
            "PERMISSION",
            "Handlebar Bluetooth permission result: granted=$granted."
        )
        // A session is usually already running when this is answered - the rider left it to come
        // here. Nothing is broadcast when a permission is granted, so the bridge that skipped
        // capture for want of it has to be told.
        if (granted) MediaButtonBridge.bluetoothPermissionGranted()
        finish()
    }

    /**
     * The same hand-off as [handlebarBluetoothLauncher], for the grants that decide whether this
     * app can see the Wi-Fi air - see [TBoxScanPermissions] for what is blind without them and
     * what that has cost.
     *
     * A field, and always finishing, for the reasons given on that launcher. The result is logged
     * per permission rather than as one boolean because a partial grant is a real state: a rider
     * can allow "Nearby devices" and refuse location on the same sheet, and the next report has
     * to be able to say which.
     */
    private val tboxScanPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        ProjectionEventLog.record(
            "PERMISSION",
            "Wi-Fi permission results (asked on the companion app's behalf): " +
                grants.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" } + "."
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProjectionEventLog.record("UI", "Main activity created.")
        // Nothing is sent without consent: this only decides whether there is a reason to ask.
        DiagnosticReportScheduler.onAppStarted(this, CrashRecovery.previousCrashRecovered)
        // Always light icons: the app is dark whatever the phone's theme is, and the platform
        // default would pick dark icons on a light-mode phone - invisible on this background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        refreshAoaAccessoryConnected(intent)
        handleAndroidAutoPreviewLaunchIntent(intent)
        handleHandlebarBluetoothRequestIntent(intent)
        handleTBoxScanPermissionRequestIntent(intent)

        setContent {
            MotoHubTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val diagnosticsState by diagnosticsViewModel.uiState.collectAsStateWithLifecycle()
                val clockLabState by clockLabViewModel.uiState.collectAsStateWithLifecycle()
                val projectionEvents by ProjectionEventLog.events.collectAsStateWithLifecycle()
                val androidAutoState by AndroidAutoRuntime.state.collectAsStateWithLifecycle()
                val androidAutoActive = androidAutoState is AndroidAutoRuntimeState.Preparing ||
                    androidAutoState is AndroidAutoRuntimeState.ReceiverReady ||
                    androidAutoState is AndroidAutoRuntimeState.Streaming
                val androidAutoStreaming = androidAutoState is AndroidAutoRuntimeState.Streaming
                val aoaExternalState by AoaExternalRuntime.state.collectAsStateWithLifecycle()
                val externalDisplayActive = aoaExternalState is AoaExternalRuntimeState.Starting ||
                    aoaExternalState is AoaExternalRuntimeState.Streaming
                val externalDisplayStreaming = aoaExternalState is AoaExternalRuntimeState.Streaming
                var selectedTab by rememberSaveable { mutableStateOf(HubTab.RIDE) }
                // The dock shows only on the tab roots: Settings reports whether it is on its own.
                var settingsAtRoot by rememberSaveable { mutableStateOf(true) }
                var showMotorcycleSwitcher by rememberSaveable { mutableStateOf(false) }
                var showQrScanner by rememberSaveable { mutableStateOf(false) }
                var showManualPairing by rememberSaveable { mutableStateOf(false) }
                var showNetworkDiagnostics by rememberSaveable { mutableStateOf(false) }
                var showClockLab by rememberSaveable { mutableStateOf(false) }
                var showBleExplorer by rememberSaveable { mutableStateOf(false) }
                var showApplicationLogs by rememberSaveable { mutableStateOf(false) }
                var showAbout by rememberSaveable { mutableStateOf(false) }
                var showAndroidAutoHelp by rememberSaveable { mutableStateOf(false) }
                val launchedPhoneOnlyAa =
                    intent?.getBooleanExtra(IpcBridgeContract.EXTRA_START_PHONE_ONLY_ANDROID_AUTO, false) == true
                val launchedPhoneOnlyAaDisplayMode =
                    intent?.getStringExtra(IpcBridgeContract.EXTRA_ANDROID_AUTO_DISPLAY_MODE)
                var showAndroidAutoPreview by rememberSaveable { mutableStateOf(launchedPhoneOnlyAa) }
                var androidAutoPreviewIsPhoneOnly by rememberSaveable { mutableStateOf(launchedPhoneOnlyAa) }
                var androidAutoPhoneOnlyLaunchedFromPro by rememberSaveable { mutableStateOf(launchedPhoneOnlyAa) }
                var showQrImageSource by remember { mutableStateOf(false) }
                var qrPhotoProcessing by remember { mutableStateOf(false) }
                var qrPhotoProgress by remember { mutableStateOf(0 to 0) }
                // The import sheet's other two states: a failed read (empty = no useful verdict)
                // and a code that was read, held until the sheet has slid away.
                var qrImportFailure by remember { mutableStateOf<String?>(null) }
                var qrImportDecoded by remember { mutableStateOf<TBoxQrPayload?>(null) }
                // The sheet stays up under the picker, so a double tap would open a second one.
                var qrPickerOpen by remember { mutableStateOf(false) }
                var pendingUnverifiedQr by remember { mutableStateOf<TBoxQrPayload?>(null) }
                var lastAutoConnectAttemptAt by remember { mutableStateOf(0L) }
                var autoConnectAttempts by remember { mutableStateOf(0) }
                var editorProfileId by rememberSaveable { mutableStateOf<String?>(null) }
                var capabilityProfileId by rememberSaveable { mutableStateOf<String?>(null) }
                var photoTargetProfileId by rememberSaveable { mutableStateOf<String?>(null) }
                var returnToGarageAfterPairing by rememberSaveable { mutableStateOf(false) }
                val context = LocalContext.current

                // A refused permission names the fix: "Allow the camera to …" [Settings].
                fun allowInAppSettings(text: String) {
                    MotoHubSnackbar.show(
                        context,
                        text,
                        MhSnackTone.ERROR,
                        actionLabel = motoHubText("Settings"),
                        onAction = {
                            if (!WifiDirectGate.openAppInfo(context, context.packageName)) {
                                MotoHubSnackbar.error(context, motoHubText("Couldn't open app settings"))
                            }
                        }
                    )
                }

                // Every QR path ends like the manual form: a snackbar saying whether the motorcycle
                // was saved. Nothing else on screen tells the rider.
                fun saveQrPairing(payload: TBoxQrPayload) {
                    if (viewModel.applyQrPairing(payload)) {
                        MotoHubSnackbar.success(context, motoHubText("Motorcycle saved"))
                    } else {
                        MotoHubSnackbar.error(context, motoHubText("Couldn't save the motorcycle"))
                    }
                }

                // A code that corroborates itself is saved straight away; anything else decoded
                // cleanly but from a source we cannot vouch for waits for the rider to confirm.
                fun acceptQrPayload(payload: TBoxQrPayload) {
                    // A dash that wants the phone to host names itself but no network, so there is
                    // nothing to save and the rider goes straight to the form to type what the dash
                    // is showing them. The mode is already selected for them.
                    if (viewModel.needsPhoneHotspotCredentials(payload)) {
                        viewModel.prepareQrPhoneHotspotSetup(payload)
                        showManualPairing = true
                        return
                    }
                    if (payload.origin == TBoxQrOrigin.RECOGNISED) {
                        saveQrPairing(payload)
                    } else {
                        ProjectionEventLog.record(
                            "PAIRING",
                            "QR decoded from an unrecognised provisioning source; " +
                                "asking the rider before saving ssid=${payload.ssid}."
                        )
                        pendingUnverifiedQr = payload
                    }
                }
                LaunchedEffect(Unit) {
                    // Cold start: the launch Intent is available before composition, so start
                    // directly here; the SharedFlow event below is only for warm starts.
                    if (launchedPhoneOnlyAa) {
                        applyPhoneOnlyAndroidAutoDisplayMode(context, launchedPhoneOnlyAaDisplayMode)
                        androidAutoPhoneOnlyBridge.start(onFailure = { message ->
                            MotoHubSnackbar.error(context, message)
                        })
                    }
                    PhoneOnlyAndroidAutoLaunchRequest.requests.collect { displayMode ->
                        applyPhoneOnlyAndroidAutoDisplayMode(context, displayMode)
                        androidAutoPhoneOnlyBridge.start(onFailure = { message ->
                            MotoHubSnackbar.error(context, message)
                        })
                        androidAutoPreviewIsPhoneOnly = true
                        androidAutoPhoneOnlyLaunchedFromPro = true
                        showAndroidAutoPreview = true
                    }
                }
                var showSafetyDisclaimer by rememberSaveable {
                    mutableStateOf(!MotoHubSettings.safetyDisclaimerAcknowledged(context))
                }
                var seamlessResumeEnabled by remember {
                    mutableStateOf(
                        MotoHubSettings.seamlessResume(context) && Settings.canDrawOverlays(context)
                    )
                }
                var seamlessResumePermissionPending by remember { mutableStateOf(false) }
                val displayModeStore = remember(context) { AndroidAutoDisplayModeStore(context) }
               val displayGeometryStore = remember(context) { TBoxDisplayGeometryStore(context) }
                val screenMarginsStore = remember(context) { TBoxScreenMarginsStore(context) }
                val capabilityStore = remember(context) { TBoxCapabilityStore(context) }
                var portScanInProgress by remember { mutableStateOf(false) }
                var portScanResult by remember { mutableStateOf<TBoxPortScanResult?>(null) }
                val portScanScope = rememberCoroutineScope()
                fun scanTBoxPorts(profile: MotorcycleProfile) {
                    if (portScanInProgress) return
                    portScanInProgress = true
                    portScanResult = null
                    portScanScope.launch {
                        portScanResult = TBoxPortScanner.scan(context, profile).getOrNull()
                        portScanInProgress = false
                    }
                }
                val motorcyclePhotoStore = remember(context) { MotorcyclePhotoStore(context) }
                val motorcycleId = state.session.motorcycle?.id
               var motorcycleDetailsDisplayMode by rememberSaveable {
                   mutableStateOf(AndroidAutoDisplayMode.LETTERBOX)
               }
                var motorcycleDetailsScreenMargins by rememberSaveable(
                    stateSaver = TBoxScreenMarginsSaver
                ) {
                    mutableStateOf(TBoxScreenMargins.NONE)
                }
                var dimDisplayEnabled by rememberSaveable {
                    mutableStateOf(PhoneDisplayDimPreferences.isEnabled(context))
                }
                val projectionManager = context.getSystemService(
                    MediaProjectionManager::class.java
                )
                // Shared tail of every photo source (gallery, document picker, camera).
                val storeMotorcyclePhoto: (Uri?) -> Unit = { uri ->
                    val profileId = photoTargetProfileId
                    photoTargetProfileId = null
                    val profile = state.motorcycles.firstOrNull { it.id == profileId }
                    if (uri != null && profile != null) {
                        motorcyclePhotoStore.copyFromUri(profile.id, uri)
                            .onSuccess { photoPath ->
                                if (viewModel.updateMotorcycle(profile.copy(photoPath = photoPath))) {
                                    motorcyclePhotoStore.delete(profile.photoPath)
                                } else {
                                    motorcyclePhotoStore.delete(photoPath)
                                }
                                ProjectionEventLog.record("GARAGE", "Photo updated for motorcycle ${profile.ssid}.")
                            }
                            .onFailure {
                                ProjectionEventLog.error("GARAGE", "Unable to store the selected motorcycle photo.", it)
                                MotoHubSnackbar.error(context, motoHubText("Couldn't save the photo"))
                            }
                    }
                }
                val motorcyclePhotoLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri -> storeMotorcyclePhoto(uri) }
                // The document picker reaches Downloads, SD cards and cloud providers the media picker hides.
                val motorcyclePhotoFileLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> storeMotorcyclePhoto(uri) }
                var motorcycleCameraCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
                val motorcyclePhotoCameraLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture()
                ) { captured ->
                    val captureUri = motorcycleCameraCaptureUri?.let(Uri::parse)
                    motorcycleCameraCaptureUri = null
                    storeMotorcyclePhoto(captureUri.takeIf { captured })
                    motorcyclePhotoStore.discardCameraCapture(captureUri)
                }
                val launchMotorcycleCamera = {
                    val captureUri = motorcyclePhotoStore.createCameraCaptureUri()
                    motorcycleCameraCaptureUri = captureUri.toString()
                    motorcyclePhotoCameraLauncher.launch(captureUri)
                }
                // CAMERA is declared in the manifest, so even the system camera intent needs the grant.
                val motorcyclePhotoCameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    ProjectionEventLog.record("PERMISSION", "Camera permission for the garage photo: granted=$granted.")
                    if (granted) {
                        launchMotorcycleCamera()
                    } else {
                        photoTargetProfileId = null
                        allowInAppSettings(motoHubText("Allow the camera to take a photo"))
                    }
                }
                // Motorcycle details and the Ride hero's options: one photo flow behind both.
                fun chooseMotorcyclePhoto(profile: MotorcycleProfile, source: MotorcyclePhotoSource) {
                    photoTargetProfileId = profile.id
                    ProjectionEventLog.record("GARAGE", "Photo source chosen for ${profile.ssid}: $source.")
                    when (source) {
                        MotorcyclePhotoSource.GALLERY -> motorcyclePhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        MotorcyclePhotoSource.FILES -> motorcyclePhotoFileLauncher.launch(arrayOf("image/*"))
                        MotorcyclePhotoSource.CAMERA ->
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                launchMotorcycleCamera()
                            } else {
                                motorcyclePhotoCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                    }
                }
                fun removeMotorcyclePhoto(profile: MotorcycleProfile) {
                    val oldPath = profile.photoPath
                    if (viewModel.updateMotorcycle(profile.copy(photoPath = null))) {
                        motorcyclePhotoStore.delete(oldPath)
                        ProjectionEventLog.record("GARAGE", "Photo removed for motorcycle ${profile.ssid}.")
                        MotoHubSnackbar.success(context, motoHubText("Photo removed"))
                    }
                }
                // The consent result can arrive before the activity is resumed, and some ROMs
                // refuse the foreground start in that window (WB-27). The consent is then held and
                // tried once more on the next resume; a second refusal ends it as a cancellation.
                var pendingProjectionConsent by remember { mutableStateOf<Pair<Int, Intent>?>(null) }
                val startMirroringSession: (Int, Intent) -> Boolean = { resultCode, data ->
                    if (ProjectionSessionService.start(context, resultCode, data)) {
                        viewModel.onProjectionRequested()
                        true
                    } else {
                        false
                    }
                }
                val projectionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    ProjectionEventLog.record(
                        "PERMISSION",
                        "Screen capture consent returned resultCode=${result.resultCode}, hasData=${result.data != null}."
                    )
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        if (!startMirroringSession(result.resultCode, result.data!!)) {
                            ProjectionEventLog.warning(
                                "MIRROR",
                                "Mirroring start refused before the app was in front; retrying on resume."
                            )
                            pendingProjectionConsent = result.resultCode to result.data!!
                        }
                    } else {
                        viewModel.onProjectionCancelled()
                    }
                }
                val currentStartMirroringSession by rememberUpdatedState(startMirroringSession)
                val projectionRetryLifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(projectionRetryLifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            pendingProjectionConsent?.let { (resultCode, data) ->
                                pendingProjectionConsent = null
                                if (!currentStartMirroringSession(resultCode, data)) viewModel.onProjectionCancelled()
                            }
                        }
                    }
                    projectionRetryLifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { projectionRetryLifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val externalDisplayProjectionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    ProjectionEventLog.record(
                        "PERMISSION",
                        "External display screen capture consent returned resultCode=${result.resultCode}, hasData=${result.data != null}."
                    )
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        AoaExternalService.start(context, result.resultCode, result.data!!)
                    }
                }
                val aoaAccessoryConnected by AoaAccessoryRuntime.connected.collectAsStateWithLifecycle()
                DisposableEffect(Unit) {
                    // ACTION_USB_ACCESSORY_ATTACHED is only ever resolved to an activity
                    // launch/onNewIntent by the system, never sent as a broadcast - handled in
                    // onCreate/onNewIntent below. DETACHED, unlike ATTACHED, is a real broadcast.
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(receiverContext: Context, intent: Intent) {
                            AoaAccessoryRuntime.publish(AoaExternalService.isAccessoryConnected(context))
                        }
                    }
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose { context.unregisterReceiver(receiver) }
                }
                var projectionPermissionPending by rememberSaveable { mutableStateOf(false) }
                var androidAutoPermissionPending by rememberSaveable { mutableStateOf(false) }
                // Asked, not assumed: the gate probes the three EasyConn reverse ports at the
                // moment a projection is about to start, so what the rider is shown is the state
                // of their phone right now rather than the fact that a companion app exists.
                val companionConflictGate = rememberCompanionConflictGate()
                var externalDisplayPermissionPending by rememberSaveable { mutableStateOf(false) }
                // Mirrors androidAutoPermissionPending for the phone-only path (see
                // startPhoneOnlyBridge below) - a real T-Box session and a phone-only one both
                // post a media notification for the same MediaButtonBridge/handlebar reason, so
                // both need POST_NOTIFICATIONS before starting, not just the T-Box one.
                var phoneOnlyAndroidAutoPermissionPending by rememberSaveable { mutableStateOf(false) }
                // Set right before the permission chain starts (see continueAndroidAutoPhoneOnlyStart)
                // and read once the bridge actually starts, on either side of a permission
                // request round-trip. The handlebar teach dialog wants the session running
                // silently in the background - captureActive is all it needs - and opening the
                // full-screen preview there just hands the rider a "Close" button they can hit
                // by accident and kill the very session they asked for.
                var phoneOnlyAndroidAutoShowPreview by rememberSaveable { mutableStateOf(true) }
                var microphonePermissionAction by rememberSaveable { mutableStateOf<String?>(null) }
                // Starts the phone-only bridge itself - permission checks (notification, mic)
                // happen in continueAndroidAutoPhoneOnlyStart below, exactly like
                // continueAndroidAutoStart does for the real T-Box path. Skipping them here was
                // the field bug reported 2026-08-13: without RECORD_AUDIO, invoking Android
                // Auto's Assistant left it waiting on a microphone stream that never arrived,
                // and every handlebar press after that point silently did nothing.
                val startPhoneOnlyBridge: () -> Unit = {
                    ProjectionEventLog.record(
                        "ANDROID_AUTO",
                        "User started phone-only Android Auto (no T-Box) for testing."
                    )
                    // Same bridge Advanced's own "Android Auto - straight to your phone" card
                    // starts over IPC (see PhoneOnlyAndroidAutoLaunchRequest) - here it's an
                    // in-process button tap instead of a launch Intent. Deliberately NOT
                    // androidAutoPhoneOnlyLaunchedFromPro: that flag finish()es this Activity
                    // when the preview closes, which is right for a launch FROM Advanced but
                    // would exit Core's own UI for a tap made inside it.
                    androidAutoPhoneOnlyBridge.start(onFailure = { message ->
                        MotoHubSnackbar.error(context, message)
                    })
                    androidAutoPreviewIsPhoneOnly = true
                    if (phoneOnlyAndroidAutoShowPreview) {
                        showAndroidAutoPreview = true
                    }
                }
                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val action = microphonePermissionAction
                    microphonePermissionAction = null
                    ProjectionEventLog.record("PERMISSION", "Microphone permission result: granted=$granted.")
                    if (granted) {
                        when (action) {
                            "full" -> startAndroidAuto()
                            "phone_only" -> startPhoneOnlyBridge()
                        }
                    } else {
                        // After two refusals Android stops asking, and every later tap was silent.
                        ProjectionEventLog.warning("PERMISSION", "Microphone permission denied; Android Auto not started.")
                        allowInAppSettings(motoHubText("Allow the microphone to start Android Auto"))
                    }
                }
                val requestMicAndStart: (String) -> Unit = { action ->
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        startAndroidAuto()
                    } else {
                        microphonePermissionAction = action
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
                val requestMicAndStartPhoneOnly: () -> Unit = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        startPhoneOnlyBridge()
                    } else {
                        microphonePermissionAction = "phone_only"
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
               val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    ProjectionEventLog.record("PERMISSION", "Notification permission result: granted=$granted.")
                    if (projectionPermissionPending) {
                        projectionPermissionPending = false
                        if (granted) {
                            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        } else {
                            viewModel.onNotificationPermissionDenied()
                        }
                    } else if (androidAutoPermissionPending) {
                        androidAutoPermissionPending = false
                       if (granted) {
                            requestMicAndStart("full")
                       } else {
                            viewModel.onNotificationPermissionDenied()
                        }
                    } else if (externalDisplayPermissionPending) {
                        externalDisplayPermissionPending = false
                        if (granted) {
                            externalDisplayProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        } else {
                            viewModel.onNotificationPermissionDenied()
                        }
                    } else if (phoneOnlyAndroidAutoPermissionPending) {
                        phoneOnlyAndroidAutoPermissionPending = false
                        if (granted) {
                            requestMicAndStartPhoneOnly()
                        } else {
                            viewModel.onNotificationPermissionDenied()
                        }
                    }
                }
                // The first stream fires the system's permission prompts back to back; say why first.
                // Once Continue has been tapped it never shows again: a later refusal lands on the
                // microphone snackbar or the "Allow notifications" banner instead.
                var permissionPrimer by remember { mutableStateOf<PermissionPrimer?>(null) }
                fun primeThen(androidAuto: Boolean, start: () -> Unit) {
                    val notifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    val microphone = androidAuto &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                        PackageManager.PERMISSION_GRANTED
                    if ((notifications || microphone) && !MotoHubSettings.permissionPrimerSeen(context)) {
                        permissionPrimer = PermissionPrimer(notifications, microphone, start)
                    } else {
                        start()
                    }
                }
                val continueAndroidAutoStart: () -> Unit = {
                    primeThen(androidAuto = true) {
                        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        if (notificationGranted) {
                            requestMicAndStart("full")
                        } else {
                            androidAutoPermissionPending = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                // Same permission sequence as continueAndroidAutoStart (notification, then mic)
                // for the phone-only path - see startPhoneOnlyBridge for why skipping this was a bug.
                val continueAndroidAutoPhoneOnlyStart: (Boolean) -> Unit = { showPreview ->
                    primeThen(androidAuto = true) {
                        phoneOnlyAndroidAutoShowPreview = showPreview
                        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        if (notificationGranted) {
                            requestMicAndStartPhoneOnly()
                        } else {
                            phoneOnlyAndroidAutoPermissionPending = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                // Hoisted out of the home screen's callbacks so autostart can run the exact same
                // sequence a tap does - permission checks included - instead of a second, subtly
                // different copy of it.
                val startMirroring: () -> Unit = {
                    // Mirroring needs the same reverse ports Android Auto does, and used to walk
                    // into the conflict with nothing said at all - only the AA path warned.
                    companionConflictGate.gate("Mirroring") {
                        primeThen(androidAuto = false) {
                            val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            if (notificationGranted) {
                                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                            } else {
                                projectionPermissionPending = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }
                val startAndroidAutoWithWarning: () -> Unit = {
                    companionConflictGate.gate("Android Auto") { continueAndroidAutoStart() }
                }
                val wifiPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    ProjectionEventLog.record(
                        "PERMISSION",
                        "Wi-Fi permission results: ${grants.entries.joinToString { "${it.key.substringAfterLast('.')}=${it.value}" }}."
                    )
                    if (grants.values.all { it }) {
                        connectWhenAndroidAccepts("after the Wi-Fi permission grant")
                    } else {
                        viewModel.onNearbyWifiPermissionDenied()
                    }
                }
                // Same permission-check-then-connect sequence HubHomeScreen's own Connect
                // button uses (see onConnectAndDiscover below) - pulled out so the handlebar
                // teach prerequisite dialog (see HandlebarTeachPrerequisiteRequest) can reuse it
                // after selecting a different motorcycle, instead of duplicating the check.
                val connectToActiveMotorcycle: () -> Unit = {
                    val permissions =
                        tboxConnectPermissions(context, viewModel.uiState.value.session.motorcycle)
                    if (permissions.all { permission ->
                            ContextCompat.checkSelfPermission(context, permission) ==
                                PackageManager.PERMISSION_GRANTED
                        }
                    ) {
                        connectWhenAndroidAccepts("Connect button")
                    } else {
                        wifiPermissionLauncher.launch(permissions)
                    }
                }
                LaunchedEffect(Unit) {
                    HandlebarTeachPrerequisiteRequest.requests.collect { choice ->
                        when (choice) {
                            HandlebarTeachPrerequisiteRequest.Choice.PhoneOnly ->
                                // Silent: calibration only needs captureActive, and a fresh
                                // rider seeing an unexpected full-screen preview open here
                                // could tap its "Close" button without realizing that stops
                                // the very session they just asked for.
                                continueAndroidAutoPhoneOnlyStart(false)
                            is HandlebarTeachPrerequisiteRequest.Choice.Connect -> {
                                viewModel.selectMotorcycle(choice.motorcycleId)
                                connectToActiveMotorcycle()
                            }
                        }
                    }
                }
                suspend fun attemptAutoConnect() {
                    if (!MotoHubSettings.autoConnect(context)) {
                        ProjectionEventLog.debug("AUTO_CONNECT", "Auto-connect on launch is disabled.")
                        return
                    }
                    if (ProjectionRuntime.riderStopped) {
                        ProjectionEventLog.debug("AUTO_CONNECT", "Auto-connect skipped: the rider stopped manually.")
                        return
                    }
                    // Read from the ViewModel, not from the composition's `state`: below STARTED
                    // collectAsStateWithLifecycle stops collecting, so `state` freezes at whatever
                    // was true when the rider left the screen - and this runs on every resume,
                    // where a frozen phase would have it attempt against a link that came up
                    // while the app was away.
                    val session = viewModel.uiState.value.session
                    val profile = session.motorcycle
                    val phase = session.phase
                    if (profile == null ||
                        (phase != SessionPhase.NETWORK_SETUP_REQUIRED && phase != SessionPhase.ERROR)
                    ) {
                        ProjectionEventLog.debug(
                            "AUTO_CONNECT",
                            "Auto-connect skipped; profilePresent=${profile != null}, phase=$phase."
                        )
                        return
                    }
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastAutoConnectAttemptAt < AUTO_CONNECT_RETRY_COOLDOWN_MS) return
                    // Deliberately before the timestamp is stamped: a skip must not push the
                    // cooldown out, so the resume that finally finds the dash on the air is not
                    // made to wait for a decision that cost nothing.
                    // Both rungs read ONCE and shared with the cancel evidence: asking twice
                    // would spend a second scan read and could answer differently between the
                    // decision and the observation that retires the evidence.
                    val dashBroadcasting = viewModel.isDashBroadcasting()
                    val associatedToDash = viewModel.isAssociatedToDash()
                    val decision = autoConnectDecision(
                        riderCancelled = viewModel.riderCancelledConnect,
                        previousAttempts = autoConnectAttempts,
                        dashBroadcasting = dashBroadcasting,
                        associatedToDash = associatedToDash,
                        dashReachableWhenCancelled = viewModel.dashReachableWhenCancelled(
                            dashReachable(dashBroadcasting, associatedToDash)
                        )
                    )
                    if (decision is AutoConnectDecision.Skip) {
                        ProjectionEventLog.debug("AUTO_CONNECT", "Auto-connect skipped; ${decision.reason}")
                        return
                    }
                    lastAutoConnectAttemptAt = now
                    autoConnectAttempts++
                    delay(AUTO_CONNECT_START_DELAY_MS)
                    ProjectionEventLog.record(
                        "AUTO_CONNECT",
                        "Launching automatic connection to saved motorcycle ${profile.ssid}."
                    )
                    val permissions = tboxConnectPermissions(context, profile)
                    if (permissions.all { permission ->
                            ContextCompat.checkSelfPermission(context, permission) ==
                                PackageManager.PERMISSION_GRANTED
                        }
                    ) {
                        connectWhenAndroidAccepts("auto-connect")
                    } else {
                        wifiPermissionLauncher.launch(permissions)
                    }
                }
                // Retries on every resume (not just app launch): the motorcycle's Wi-Fi AP
                // may not be in range yet the first time MOTO-HUB opens - e.g. the app is
                // opened before the bike is powered on. attemptAutoConnect()'s own phase
                // check already skips this once a connection is established or in progress.
                val autoConnectScope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            autoConnectScope.launch { attemptAutoConnect() }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                // A resume is not the only moment the bike appears. Riders power the dash up
                // AFTER opening MOTO-HUB - rider 36a3fd37, 2026-09-01, said it plainly: "I
                // thought, OK, I'll open MotoPlay on the motorcycle, and when it connects to the
                // motorcycle's Wi-Fi the transmission will happen. But it didn't." Nothing was
                // watching: his app sat on NETWORK_SETUP_REQUIRED for ten minutes while the dash
                // broadcast, and joined in 5110ms the instant he brought it back to the front.
                //
                // So: keep asking while the rider is looking at MOTO-HUB. attemptAutoConnect() is
                // the same function the resume path calls and carries every brake - the phase
                // check stops it re-entering an attempt already running, the cooldown stops
                // bursts, and the rider's cancel still outranks it.
                //
                // RESUMED, and nothing wider, is the gate. The switch says "Auto-connect on
                // launch" and that is exactly what it means: the phone in a pocket is not this
                // feature's problem. A link that was already up and drops is the recovery
                // watchdog's, which is a different switch on the same screen. The alternative -
                // a foreground service holding the process at foreground-service importance so
                // the request is accepted with the screen off - bought that pocket case at the
                // price of a Wi-Fi specifier request (a 30s scan) every 15s for as long as it
                // ran, and of a notification for something the rider never asked to watch.
                //
                // The interval widens as the wait goes on: the gap this covers is about two
                // minutes (unlock, start the bike, wait for the dash to boot), so the first
                // minutes stay as quick as they were, and a phone left on a desk with MOTO-HUB
                // open settles at two minutes instead of scanning all afternoon. Every return to
                // the screen restarts the block, so the rider who comes back gets 15s again.
                LaunchedEffect(lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        var waits = 0
                        while (true) {
                            delay(
                                minOf(
                                    AUTO_CONNECT_WATCH_INTERVAL_MS shl waits.coerceAtMost(3),
                                    AUTO_CONNECT_WATCH_MAX_INTERVAL_MS
                                )
                            )
                            waits++
                            attemptAutoConnect()
                        }
                    }
                }
                // ── Autostart on connect ────────────────────────────────────────────────────
                //
                // Fires at most once per app launch, the first time a T-Box link comes up (phase
                // READY - the "what should I show?" screen). One-shot on purpose: re-arming would
                // restart the very screen the rider just stopped as soon as they connect again,
                // leaving no way back to the picker.
                var autostartArmed by rememberSaveable { mutableStateOf(true) }
                // Collected from the ViewModel rather than keyed on the composition's snapshot,
                // for the reason attemptAutoConnect reads it there too: an attempt in flight when
                // the rider leaves the screen lands minutes later, and a stopped activity neither
                // recomposes nor re-keys this effect. Keyed on the phase alone it would have
                // connected and then sat there, started nothing, and waited to be looked at.
                LaunchedEffect(Unit) {
                    viewModel.uiState
                        .map { it.session.phase }
                        .distinctUntilChanged()
                        .collect { phase ->
                            if (phase != SessionPhase.READY) return@collect
                            if (!autostartArmed) return@collect
                            if (!MotoHubSettings.autostartEnabled(context)) return@collect
                            val service = MotoHubSettings.autostartService(context)
                            autostartArmed = false
                            if (service.advancedOnly) {
                                ProjectionEventLog.warning(
                                    "AUTOSTART",
                                    "${service.label} is configured but this edition cannot run it; nothing started."
                                )
                                return@collect
                            }
                            ProjectionEventLog.record(
                                "AUTOSTART",
                                "T-Box link is up; starting ${service.label} automatically."
                            )
                            // Let the mode screen settle before a system consent dialog lands on
                            // top of it.
                            delay(AUTOSTART_ON_CONNECT_DELAY_MS)
                            // Disconnect or Cancel can land inside that delay.
                            if (ProjectionRuntime.riderStopped) {
                                ProjectionEventLog.debug("AUTOSTART", "Autostart skipped: the rider stopped manually.")
                                return@collect
                            }
                            when (service) {
                                AutostartService.MIRRORING -> startMirroring()
                                AutostartService.ANDROID_AUTO -> startAndroidAutoWithWarning()
                                AutostartService.RIDE_DASHBOARD -> Unit
                            }
                        }
                }
                // A call drops a phone-only session: the dialer covers the app and Android Auto
                // closes its socket (open-headunit #83). Hanging up brings the rider back here, so
                // the resume is when it starts again - once per drop, through the same permission
                // checks a tap goes through, and with the preview left as the rider had it.
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME &&
                            androidAutoPhoneOnlyBridge.takeInterruptedSession()
                        ) {
                            ProjectionEventLog.record(
                                "ANDROID_AUTO",
                                "Restarting Android Auto on this phone after it was interrupted (e.g. a call)."
                            )
                            continueAndroidAutoPhoneOnlyStart(showAndroidAutoPreview)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val overlayPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    val granted = PhoneDisplayDimmer.canDim(context)
                    ProjectionEventLog.record("PERMISSION", "Display overlay permission result: granted=$granted.")
                    PhoneDisplayDimPreferences.setEnabled(context, granted)
                    dimDisplayEnabled = granted
                    if (granted && state.session.phase == SessionPhase.CAPTURING) {
                        ProjectionSessionService.dimDisplay(context)
                    }
                }
                val seamlessResumePermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    val granted = Settings.canDrawOverlays(context)
                    if (granted && seamlessResumePermissionPending) {
                        MotoHubSettings.setSeamlessResume(context, true)
                        seamlessResumeEnabled = true
                    } else if (!granted) {
                        MotoHubSettings.setSeamlessResume(context, false)
                        seamlessResumeEnabled = false
                        // Otherwise the switch the rider just turned on silently flips back.
                        MotoHubSnackbar.error(context, motoHubText("Seamless resume needs “Display over other apps”"))
                    }
                    seamlessResumePermissionPending = false
                    ProjectionEventLog.record(
                        "PERMISSION",
                        "Seamless resume overlay permission result: granted=$granted, " +
                            "enabled=$seamlessResumeEnabled."
                    )
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    ProjectionEventLog.record("PERMISSION", "Camera permission result: granted=$granted.")
                    if (granted) {
                        showQrScanner = true
                    } else {
                        if (returnToGarageAfterPairing) {
                            returnToGarageAfterPairing = false
                            selectedTab = HubTab.GARAGE
                        }
                        // A snackbar, not the Ride banner: it floats over whichever tab the
                        // rider asked from, and a denied camera is not a connection error.
                        ProjectionEventLog.warning("PERMISSION", "Camera permission denied.")
                        allowInAppSettings(motoHubText("Allow the camera to scan QR codes"))
                    }
                }
                // One decoder behind two doors. The photo picker indexes the gallery and nothing
                // else, so a pairing code saved to Downloads or pulled out of a chat was
                // unreachable; OpenDocument reaches those, and the rider picks which on the way in.
                val decodeQrImage: (Uri?) -> Unit = decode@{ uri ->
                    qrPickerOpen = false
                    if (uri == null) {
                        ProjectionEventLog.debug("PAIRING", "QR photo picker closed without a selection.")
                        return@decode
                    }
                    ProjectionEventLog.record("PAIRING", "QR photo selected; starting ML Kit decoding.")

                    qrPhotoProcessing = true
                    qrPhotoProgress = 0 to 0
                    qrImportFailure = null
                    TBoxQrPhotoDecoder.scan(
                        context = context,
                        uri = uri,
                        onProgress = { attempt, total -> qrPhotoProgress = attempt to total }
                    ) { result ->
                        qrPhotoProcessing = false
                        result
                            // The import sheet hands it to acceptQrPayload once it has slid away.
                            .onSuccess { payload -> qrImportDecoded = payload }
                            .onFailure { failure ->
                                ProjectionEventLog.debug(
                                    "PAIRING",
                                    "QR photo decoding failed after preprocessing attempts: ${failure.message}"
                                )
                                // TBoxQrPhotoDecoder carries the parser's own verdict out as the
                                // last failure, and that verdict is the useful half: it names the
                                // code that was actually read and what to scan instead. Replacing
                                // it with one fixed sentence told a rider who had photographed the
                                // vehicle-information code that no QR was found at all.
                                //
                                // Only our two rider-facing throwables are surfaced - the parser's
                                // check() and the decoder's own "no readable QR" - so an ML Kit or
                                // file-read failure still gets the generic wording instead of a
                                // stack-trace message.
                                val explained = failure
                                    .takeIf { it is IllegalStateException || it is IllegalArgumentException }
                                    ?.message
                                    ?.takeIf(String::isNotBlank)
                                ProjectionEventLog.warning(
                                    "PAIRING",
                                    explained
                                        ?: "No QR code with motorcycle Wi-Fi details could be read from the photo."
                                )
                                // Shown in the import sheet, where the rider is looking - not as
                                // a Ride error: a photo that would not read is not a failed
                                // connection. Empty lets the sheet give its own advice.
                                qrImportFailure = explained.orEmpty()
                            }
                    }
                }
                val qrPhotoLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri -> decodeQrImage(uri) }
                // Reaches Downloads, SD cards and cloud providers the media picker hides - the
                // same second door the motorcycle photo has had since it was asked for there.
                val qrPhotoFileLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> decodeQrImage(uri) }
                // Garage's "Use" and Ride's "Switch motorcycle" sheet: one path, one set of answers.
                fun selectMotorcycle(profileId: String) {
                    when (viewModel.selectMotorcycle(profileId)) {
                        HubViewModel.GarageResult.DONE -> selectedTab = HubTab.RIDE
                        HubViewModel.GarageResult.STREAMING ->
                            MotoHubSnackbar.error(context, motoHubText("Stop streaming first"))
                        HubViewModel.GarageResult.FAILED ->
                            MotoHubSnackbar.error(context, motoHubText("Couldn't switch motorcycles"))
                    }
                }
                // Garage's card and row, and the Ride hero's options.
                fun openMotorcycleDetails(profileId: String) {
                    val profile = state.motorcycles.firstOrNull { it.id == profileId } ?: return
                    motorcycleDetailsDisplayMode = displayModeStore.load(profile)
                    motorcycleDetailsScreenMargins = screenMarginsStore.load(
                        profile,
                        TBoxModelProfile.fromModelId(profile.modelId).defaultScreenMargins
                    )
                    editorProfileId = profileId
                }
                // "Import QR code" from any tab: Ride and Garage both open the same sheet.
                val importQrPhoto: () -> Unit = {
                    ProjectionEventLog.record("UI", "User requested QR decoding from a photo.")
                    showQrImageSource = true
                }

                // Which full-screen destination is on top, derived from the same state the old
                // if/else chain read. The chain replaced the whole tree in a single frame; the
                // key gives HubScreenTransition an identity to slide between instead.
                val hubScreen = when {
                    showApplicationLogs -> HubScreenKey.APPLICATION_LOGS
                    showAndroidAutoHelp -> HubScreenKey.ANDROID_AUTO_HELP
                    showAbout -> HubScreenKey.ABOUT
                    showAndroidAutoPreview -> HubScreenKey.ANDROID_AUTO_PREVIEW
                    capabilityProfileId != null -> HubScreenKey.CAPABILITIES
                    editorProfileId != null -> HubScreenKey.MOTORCYCLE_DETAILS
                    showNetworkDiagnostics -> HubScreenKey.NETWORK_DIAGNOSTICS
                    showClockLab -> HubScreenKey.CLOCK_LAB
                    showBleExplorer -> HubScreenKey.BLE_EXPLORER
                    showQrScanner -> HubScreenKey.QR_SCANNER
                    showManualPairing -> HubScreenKey.MANUAL_PAIRING
                    else -> HubScreenKey.HOME
                }
                // The dock shows only on the tab roots (Settings says when it is on its own).
                val showDock = selectedTab != HubTab.SETTINGS || settingsAtRoot
                // The last profile each profile-keyed screen actually showed. During the slide
                // out its id has already been nulled, and without this the exiting screen would
                // recompose against a missing profile and vanish mid-animation.
                var lastCapabilityProfile by remember { mutableStateOf<MotorcycleProfile?>(null) }
                var lastEditorProfile by remember { mutableStateOf<MotorcycleProfile?>(null) }
                HubScreenTransition(hubScreen) { screen ->
                    when (screen) {
                        HubScreenKey.APPLICATION_LOGS ->
                    ApplicationLogScreen(
                        events = projectionEvents,
                        onCopy = {
                            val text = ProjectionEventLog.exportText()
                            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                                ClipData.newPlainText(motoHubText("MOTO-HUB diagnostics"), text)
                            )
                            ProjectionEventLog.record("LOG", "Diagnostic log copied to the clipboard.")
                            // Android 13+ confirms a copy itself; a second confirmation would repeat it.
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                MotoHubSnackbar.success(context, motoHubText("Log copied"))
                            }
                        },
                        onShare = {
                            val text = ProjectionEventLog.exportText()
                            val shareIntent = runCatching {
                                DiagnosticLogShare.createShareIntent(context, text)
                            }.onFailure { failure ->
                                ProjectionEventLog.error("LOG", "Diagnostic log file share failed.", failure)
                            MotoHubSnackbar.error(context, motoHubText("Couldn't create the log file"))
                            }.getOrNull()
                            if (shareIntent != null) {
                                ProjectionEventLog.record("LOG", "Diagnostic log file share sheet opened.")
                                context.startActivity(Intent.createChooser(shareIntent, motoHubText("Share MOTO-HUB log")))
                            }
                        },
                        onClear = ProjectionEventLog::clear,
                        onBack = {
                            ProjectionEventLog.record("UI", "Application log screen closed.")
                            showApplicationLogs = false
                        }
                    )
                        HubScreenKey.ANDROID_AUTO_HELP ->
                    AndroidAutoHelpScreen(
                        onBack = {
                            ProjectionEventLog.record("UI", "Android Auto help screen closed.")
                            showAndroidAutoHelp = false
                        }
                    )
                        HubScreenKey.ABOUT ->
                    AboutScreen(
                        onOpenGithub = { url ->
                            ProjectionEventLog.record("UI", "GitHub repository link opened: $url")
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }.onFailure {
                                ProjectionEventLog.error("UI", "Unable to open the GitHub repository.", it)
                                MotoHubSnackbar.error(context, motoHubText("Couldn't open GitHub"))
                            }
                        },
                        onBack = {
                            ProjectionEventLog.record("UI", "About screen closed.")
                            showAbout = false
                        }
                    )
                        HubScreenKey.ANDROID_AUTO_PREVIEW ->
                    AndroidAutoPreviewScreen(
                        onBack = {
                            ProjectionEventLog.record("UI", "Android Auto phone preview closed.")
                            if (androidAutoPhoneOnlyLaunchedFromPro) {
                                // Advanced launched Core just for this preview - closing it IS
                                // closing the whole point of this Activity instance.
                                androidAutoPhoneOnlyBridge.stop()
                                showAndroidAutoPreview = false
                                androidAutoPreviewIsPhoneOnly = false
                                androidAutoPhoneOnlyLaunchedFromPro = false
                                finish()
                            } else {
                                // Otherwise this is closing the PREVIEW, not the session: a
                                // phone-only test session (and its handlebar capture) keeps
                                // running in the background exactly like a real T-Box session
                                // does when the rider switches tabs - HubDestination.ACTIVE_SESSION
                                // picks it up from the same AndroidAutoRuntime state either way,
                                // with its own "reopen preview" and explicit Stop actions.
                                // androidAutoPreviewIsPhoneOnly deliberately stays true so
                                // onStopAndroidAuto knows which session that Stop belongs to.
                                showAndroidAutoPreview = false
                            }
                        }
                    )
                        HubScreenKey.CAPABILITIES -> {
                    // The live lookup falls back to the last profile shown so the screen can
                    // still draw itself while it slides out after its id has been cleared.
                    val liveCapabilityProfile =
                        state.motorcycles.firstOrNull { it.id == capabilityProfileId }
                    if (liveCapabilityProfile != null) lastCapabilityProfile = liveCapabilityProfile
                    val profile = liveCapabilityProfile ?: lastCapabilityProfile
                    if (profile == null) {
                        capabilityProfileId = null
                        selectedTab = HubTab.GARAGE
                    } else {
                        TBoxCapabilityScreen(
                            profile = profile,
                            snapshot = capabilityStore.load(profile),
                            geometry = displayGeometryStore.load(profile.ssid),
                            portScanInProgress = portScanInProgress,
                            portScanResult = portScanResult,
                            onScanPorts = { scanTBoxPorts(profile) },
                            onBack = {
                                capabilityProfileId = null
                                editorProfileId = profile.id
                                portScanResult = null
                            }
                        )
                    }
                        }
                        HubScreenKey.MOTORCYCLE_DETAILS -> {
                    val liveEditorProfile = state.motorcycles.firstOrNull { it.id == editorProfileId }
                    if (liveEditorProfile != null) lastEditorProfile = liveEditorProfile
                    val profile = liveEditorProfile ?: lastEditorProfile
                    if (profile == null) {
                        editorProfileId = null
                        selectedTab = HubTab.GARAGE
                    } else {
                        MotorcycleDetailsScreen(
                           profile = profile,
                           displayMode = motorcycleDetailsDisplayMode,
                            screenMargins = motorcycleDetailsScreenMargins,
                            // Back to the tab it was opened from: Garage, or the Ride hero's options.
                            onBack = { editorProfileId = null },
                            onSave = { updatedProfile -> viewModel.updateMotorcycle(updatedProfile) },
                            onOpenCapabilities = {
                                capabilityProfileId = profile.id
                                editorProfileId = null
                                ProjectionEventLog.record(
                                    "UI",
                                    "T-Box capability inspector opened for ${profile.ssid}."
                                )
                            },
                            onCustomizeDashboard = {},
                            onDisplayModeChanged = { mode ->
                                displayModeStore.save(profile, mode)
                                motorcycleDetailsDisplayMode = mode
                               ProjectionEventLog.record(
                                   "ANDROID_AUTO",
                                   "TFT display mode changed for ${profile.ssid}: $mode."
                               )
                           },
                            onScreenMarginsChanged = { margins ->
                                motorcycleDetailsScreenMargins = margins
                                screenMarginsStore.save(profile, margins)
                                ProjectionEventLog.record("ANDROID_AUTO", "TFT screen margins changed for ${profile.ssid}: $margins.")
                            },
                            onChoosePhoto = { source -> chooseMotorcyclePhoto(profile, source) },
                            onRemovePhoto = { removeMotorcyclePhoto(profile) },
                            onDelete = {
                                when (viewModel.deleteMotorcycle(profile.id)) {
                                    HubViewModel.GarageResult.DONE -> {
                                        // Only once the profile is gone: a refused delete keeps
                                        // the motorcycle, and it must keep its photo with it.
                                        motorcyclePhotoStore.delete(profile.photoPath)
                                        editorProfileId = null
                                        selectedTab = HubTab.GARAGE
                                        MotoHubSnackbar.success(context, motoHubText("Motorcycle removed"))
                                    }
                                    HubViewModel.GarageResult.STREAMING ->
                                        MotoHubSnackbar.error(context, motoHubText("Stop streaming first"))
                                    HubViewModel.GarageResult.FAILED ->
                                        MotoHubSnackbar.error(context, motoHubText("Couldn't remove the motorcycle"))
                                }
                            }
                        )
                    }
                        }
                        HubScreenKey.NETWORK_DIAGNOSTICS ->
                    NetworkDiagnosticsScreen(
                        state = diagnosticsState,
                        projectionEvents = projectionEvents,
                        onRunTests = diagnosticsViewModel::runTests,
                        onBack = {
                            ProjectionEventLog.record("UI", "Network diagnostics screen closed.")
                            showNetworkDiagnostics = false
                        }
                    )
                        HubScreenKey.BLE_EXPLORER ->
                    BleExplorerScreen(
                        onBack = {
                            ProjectionEventLog.record("UI", "Bluetooth LE explorer closed.")
                            showBleExplorer = false
                        }
                    )
                        HubScreenKey.CLOCK_LAB ->
                    ClockLabScreen(
                        state = clockLabState,
                        onRun = clockLabViewModel::run,
                        onStop = clockLabViewModel::stop,
                        onBack = {
                            ProjectionEventLog.record("UI", "Dash clock lab screen closed.")
                            showClockLab = false
                        }
                    )
                        HubScreenKey.QR_SCANNER ->
                    TBoxQrScannerScreen(
                        onPayload = { payload ->
                            acceptQrPayload(payload)
                            ProjectionEventLog.record("UI", "QR scanner closed after a valid code.")
                            showQrScanner = false
                            if (returnToGarageAfterPairing) {
                                returnToGarageAfterPairing = false
                                selectedTab = HubTab.GARAGE
                            }
                        },
                        onManualPairing = {
                            ProjectionEventLog.record(
                                "UI",
                                "QR scanner handed over to manual pairing."
                            )
                            showQrScanner = false
                            viewModel.resetManualPairingForm()
                            showManualPairing = true
                        },
                        onClose = {
                            ProjectionEventLog.record("UI", "QR scanner cancelled by the user.")
                            showQrScanner = false
                            if (returnToGarageAfterPairing) {
                                returnToGarageAfterPairing = false
                                selectedTab = HubTab.GARAGE
                            }
                        }
                    )
                        HubScreenKey.MANUAL_PAIRING ->
                    ManualPairingScreen(
                        ssid = state.ssid,
                        password = state.password,
                        connectionMode = state.connectionMode,
                        formError = state.formError,
                        ssidSuggestion = state.ssidSuggestion,
                        onAcceptSsidSuggestion = viewModel::acceptSsidSuggestion,
                        onSsidChanged = viewModel::onSsidChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onConnectionModeChanged = viewModel::onConnectionModeChanged,
                        onSave = {
                            if (viewModel.saveMotorcycle()) {
                                ProjectionEventLog.record("UI", "Manual pairing screen closed after a saved profile.")
                                MotoHubSnackbar.success(context, motoHubText("Motorcycle saved"))
                                showManualPairing = false
                                if (returnToGarageAfterPairing) {
                                    returnToGarageAfterPairing = false
                                    selectedTab = HubTab.GARAGE
                                }
                            }
                        },
                        onClose = {
                            ProjectionEventLog.record("UI", "Manual pairing screen cancelled by the user.")
                            showManualPairing = false
                            if (returnToGarageAfterPairing) {
                                returnToGarageAfterPairing = false
                                selectedTab = HubTab.GARAGE
                            }
                        }
                    )
                        else ->
                    HubHomeScreen(
                        state = state,
                        selectedTab = selectedTab,
                        // A tab is only ever tapped from its root (the dock is gone everywhere
                        // else), so tapping the selected one is already "back to its root".
                        onTabSelected = { selectedTab = it },
                        showDock = showDock,
                        onSwitchMotorcycle = {
                            ProjectionEventLog.record("UI", "Switch motorcycle sheet opened.")
                            showMotorcycleSwitcher = true
                        },
                        onOpenMotorcycleDetails = { state.session.motorcycle?.id?.let(::openMotorcycleDetails) },
                        // The saved profile, not the session's copy: the photo is written back to it.
                        onChooseMotorcyclePhoto = { source ->
                            state.motorcycles.firstOrNull { it.id == state.session.motorcycle?.id }
                                ?.let { chooseMotorcyclePhoto(it, source) }
                        },
                        onRemoveMotorcyclePhoto = {
                            state.motorcycles.firstOrNull { it.id == state.session.motorcycle?.id }
                                ?.let(::removeMotorcyclePhoto)
                        },
                        onScanQr = {
                            ProjectionEventLog.record("UI", "User requested live QR scanning.")
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                showQrScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onImportQrPhoto = importQrPhoto,
                        onManualPairing = {
                            ProjectionEventLog.record("UI", "User requested manual (no-QR) pairing.")
                            viewModel.resetManualPairingForm()
                            showManualPairing = true
                        },
                        onTryPhoneHotspot = {
                            // Same screen as manual pairing, but pre-filled instead of reset: the
                            // rider is changing one setting on a motorcycle they already entered,
                            // not adding a new one.
                            viewModel.preparePhoneHotspotRetry()
                            showManualPairing = true
                        },
                        onConnectAndDiscover = connectToActiveMotorcycle,
                        companionAppName = CompanionAppRegistry.installedName(context),
                        onCloseCompanionAppAndRetry = {
                            // Android 14+ cannot close another app's process; this action is a
                            // plain retry for after the user has force-stopped the companion app.
                            ProjectionEventLog.record(
                                "CONNECTION",
                                "Retry requested from the companion-app conflict help."
                            )
                            lifecycleScope.launch {
                                delay(OFFICIAL_APP_CLOSE_RETRY_DELAY_MS)
                                // The rider was sent to another app's settings to force-stop it,
                                // so this retry often lands with MOTO-HUB still in the background.
                                connectWhenAndroidAccepts("companion-app conflict retry")
                            }
                        },
                        onOpenCompanionAppSettings = {
                            val companion = CompanionAppRegistry.installed(context)
                            if (companion == null ||
                                !CompanionAppRegistry.openAppSettings(context, companion)
                            ) {
                                MotoHubSnackbar.error(context, motoHubText("Couldn't open app settings"))
                            }
                        },
                        onOpenAndroidAutoSettings = {
                            // The guide, not Android Auto itself: dropping the rider into another
                            // app without telling them which hidden menu to open is what made the
                            // first tester hunt for a setting that is not in the settings list.
                            ProjectionEventLog.record("UI", "Android Auto help screen opened.")
                            showAndroidAutoHelp = true
                        },
                        onOpenWifiSettings = {
                            if (!WifiGate.openWifiSettings(context)) {
                                MotoHubSnackbar.error(context, motoHubText("Couldn't open Wi-Fi settings"))
                            }
                        },
                        onCancelConnection = viewModel::cancelConnection,
                        onDisconnect = viewModel::disconnect,
                        onTryProfile = viewModel::tryProfile,
                        onKeepTrialledProfile = { sendNow, alwaysSend ->
                            // The switch first: a rider who ticked both expects the report that
                            // goes out now to be the first of the automatic ones, not a one-off
                            // followed by silence.
                            if (alwaysSend) profileTrialDiagnostics?.enableAutoUpload()
                            if (sendNow) profileTrialDiagnostics?.sendNow()
                            viewModel.keepTrialledProfile()
                        },
                        onDiscardTrialledProfile = viewModel::discardTrialledProfile,
                        diagnosticsOffer = profileTrialDiagnostics,
                        onStartProjection = {
                            ProjectionEventLog.record("MIRROR", "User selected mirroring mode.")
                            startMirroring()
                        },
                        androidAutoActive = androidAutoActive,
                        androidAutoStreaming = androidAutoStreaming,
                        onStartAndroidAuto = startAndroidAutoWithWarning,
                        onStopAndroidAuto = {
                            ProjectionEventLog.record("ANDROID_AUTO", "User requested Android Auto stop.")
                            if (androidAutoPreviewIsPhoneOnly) {
                                androidAutoPhoneOnlyBridge.stop()
                                androidAutoPreviewIsPhoneOnly = false
                            } else {
                                // A manual stop wins: nothing reconnects or restarts by itself
                                // until the rider taps Connect (or the app starts cold).
                                ProjectionRuntime.riderStopped = true
                                AndroidAutoSessionService.stop(
                                    context,
                                    "Android Auto stopped by the user."
                                )
                            }
                        },
                        onOpenAndroidAutoPreview = {
                            ProjectionEventLog.record("UI", "Android Auto phone preview opened.")
                            showAndroidAutoPreview = true
                        },
                        onStartPhoneOnlyAndroidAuto = { continueAndroidAutoPhoneOnlyStart(true) },
                        dimDisplayEnabled = dimDisplayEnabled,
                        onDimDisplayChanged = { enabled ->
                            ProjectionEventLog.record("DISPLAY", "User changed display dimmer preference to enabled=$enabled.")
                            if (!enabled) {
                                PhoneDisplayDimPreferences.setEnabled(context, false)
                                dimDisplayEnabled = false
                                if (state.session.phase == SessionPhase.CAPTURING) {
                                    ProjectionSessionService.restoreDisplay(context)
                                }
                            } else if (PhoneDisplayDimmer.canDim(context)) {
                                PhoneDisplayDimPreferences.setEnabled(context, true)
                                dimDisplayEnabled = true
                                if (state.session.phase == SessionPhase.CAPTURING) {
                                    ProjectionSessionService.dimDisplay(context)
                                }
                            } else {
                                overlayPermissionLauncher.launch(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        },
                        onStopProjection = {
                            ProjectionEventLog.record("MIRROR", "User requested mirroring stop.")
                            // Latches the rider's stop in the service, which the notification shares.
                            ProjectionSessionService.stop(context)
                        },
                        // ── External display (USB AOA) ──
                        aoaAccessoryConnected = aoaAccessoryConnected,
                        externalDisplayActive = externalDisplayActive,
                        externalDisplayStreaming = externalDisplayStreaming,
                        onStartExternalDisplay = {
                            ProjectionEventLog.record("EXTERNAL", "User selected external display mode.")
                            primeThen(androidAuto = false) {
                                val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (notificationGranted) {
                                    externalDisplayProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                                } else {
                                    externalDisplayPermissionPending = true
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        onStopExternalDisplay = {
                            ProjectionEventLog.record("EXTERNAL", "User requested external display stop.")
                            AoaExternalService.stop(context)
                        },
                        garageContent = {
                            GarageTabContent(
                                profiles = state.motorcycles,
                                activeProfileId = state.session.motorcycle?.id,
                                onAddMotorcycle = {
                                    returnToGarageAfterPairing = true
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        showQrScanner = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                // No returnToGarageAfterPairing: the import sheet opens over
                                // this tab, which never changes, and a set flag would leak into
                                // a later scan started from Ride if the rider backs out here.
                                onImportQrPhoto = importQrPhoto,
                                onAddMotorcycleManually = {
                                    ProjectionEventLog.record("UI", "User requested manual (no-QR) pairing from the Garage.")
                                    returnToGarageAfterPairing = true
                                    viewModel.resetManualPairingForm()
                                    showManualPairing = true
                                },
                                onSelectMotorcycle = ::selectMotorcycle,
                                onOpenDetails = ::openMotorcycleDetails
                            )
                        },
                        settingsContent = {
                            SettingsTabContent(
                                onAtRootChanged = { settingsAtRoot = it },
                                onOpenNetworkDiagnostics = {
                                    ProjectionEventLog.record("UI", "Network diagnostics screen opened.")
                                    showNetworkDiagnostics = true
                                },
                                onOpenBleExplorer = {
                                    ProjectionEventLog.record("UI", "Bluetooth LE explorer opened.")
                                    showBleExplorer = true
                                },
                                onOpenClockLab = {
                                    ProjectionEventLog.record("UI", "Dash clock lab screen opened.")
                                    showClockLab = true
                                },
                                onOpenApplicationLogs = {
                                    ProjectionEventLog.record("UI", "Application log screen opened.")
                                    showApplicationLogs = true
                                },
                                onOpenAndroidAutoHelp = {
                                    ProjectionEventLog.record("UI", "Android Auto help screen opened.")
                                    showAndroidAutoHelp = true
                                },
                                onOpenAbout = {
                                    ProjectionEventLog.record("UI", "About screen opened.")
                                    showAbout = true
                                },
                                seamlessResumeEnabled = seamlessResumeEnabled,
                                onSeamlessResumeChanged = { enabled ->
                                    if (!enabled) {
                                        MotoHubSettings.setSeamlessResume(context, false)
                                        seamlessResumeEnabled = false
                                    } else if (Settings.canDrawOverlays(context)) {
                                        MotoHubSettings.setSeamlessResume(context, true)
                                        seamlessResumeEnabled = true
                                    } else {
                                        seamlessResumePermissionPending = true
                                        ProjectionEventLog.record(
                                            "PERMISSION",
                                            "Opening overlay permission for seamless resume."
                                        )
                                        seamlessResumePermissionLauncher.launch(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    )
                    }
                }
                // One interruption at a time, most important first; the rest wait their turn (see
                // nextOverlay). Composed independently they used to stack, least important on top.
                // Asked only of riders who never opted in; the scheduler raises this after a
                // crash and clears it on either answer.
                val crashConsentRequired by DiagnosticReportScheduler.crashConsentRequired
                    .collectAsStateWithLifecycle()
                // Swiping the wire question away puts it off to the next launch rather than
                // guessing an answer. The VM still holds the question, so the deferral lives here.
                var wireQuestionDeferred by rememberSaveable { mutableStateOf(false) }
                var shownOverlay by remember { mutableStateOf<StartupOverlay?>(null) }
                val overlay = nextOverlay(
                    shown = shownOverlay,
                    pending = buildSet {
                        if (showSafetyDisclaimer) add(StartupOverlay.SAFETY)
                        if (companionConflictGate.pending != null) add(StartupOverlay.COMPANION)
                        if (crashConsentRequired) add(StartupOverlay.CRASH_CONSENT)
                        if (state.wireQuestionFor != null && !wireQuestionDeferred) add(StartupOverlay.WIRE_VERDICT)
                        if (state.wireNeedsAndroidAutoFor != null) add(StartupOverlay.WIRE_NUDGE)
                    },
                    modalsOpen = MhModals.open
                )
                SideEffect { shownOverlay = overlay }
                // One sheet for the whole import, up while any of its states holds: a picker result
                // that lands in a recreated process, without the chooser's flag, still shows here.
                if (showQrImageSource || qrPhotoProcessing || qrImportFailure != null || qrImportDecoded != null) {
                    val (completedAttempts, totalAttempts) = qrPhotoProgress
                    QrImportSheet(
                        reading = qrPhotoProcessing,
                        progress = if (totalAttempts > 0) {
                            (completedAttempts.toFloat() / totalAttempts).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        failure = qrImportFailure,
                        decoded = qrImportDecoded,
                        // The sheet stays up under the system picker; backing out of the picker
                        // returns to it as it was.
                        onSelect = select@{ source ->
                            if (qrPickerOpen) return@select
                            qrPickerOpen = true
                            ProjectionEventLog.record("PAIRING", "QR image source chosen: $source.")
                            when (source) {
                                QrImageSource.GALLERY -> qrPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                                QrImageSource.FILES -> qrPhotoFileLauncher.launch(arrayOf("image/*"))
                            }
                        },
                        onDecoded = ::acceptQrPayload,
                        onDismiss = {
                            showQrImageSource = false
                            qrImportFailure = null
                            qrImportDecoded = null
                        }
                    )
                }
                if (showMotorcycleSwitcher) {
                    // P1: rows straight on the sheet; each runs once the sheet has gone (P10).
                    MhSheet(
                        onDismiss = { showMotorcycleSwitcher = false },
                        title = motoHubText("Switch motorcycle")
                    ) { close ->
                        val currentId = state.session.motorcycle?.id
                        state.motorcycles.forEach { profile ->
                            // Its photo where it has one, as on Garage and the Ride hero: this is
                            // where telling two motorcycles apart matters most.
                            val photo = profile.photoPath?.takeIf(String::isNotBlank)
                            MhChoiceRow(
                                title = profile.shownName(),
                                subtitle = profile.ssid,
                                icon = Icons.Rounded.TwoWheeler,
                                leading = if (photo == null) null else {
                                    { MotorcyclePhoto(photo, Modifier.size(40.dp), CircleShape) }
                                },
                                selected = profile.id == currentId,
                                onClick = { close { if (profile.id != currentId) selectMotorcycle(profile.id) } }
                            )
                        }
                        MhListRow(
                            title = motoHubText("Garage"),
                            icon = Icons.Rounded.Warehouse,
                            onClick = { close { selectedTab = HubTab.GARAGE } }
                        )
                    }
                }
                permissionPrimer?.let { primer ->
                    MhSheet(
                        onDismiss = { permissionPrimer = null },
                        title = when {
                            primer.notifications && primer.microphone ->
                                motoHubText("Allow notifications and the microphone")
                            primer.microphone -> motoHubText("Allow the microphone")
                            else -> motoHubText("Allow notifications")
                        },
                        primaryLabel = motoHubText("Continue"),
                        onPrimary = {
                            MotoHubSettings.setPermissionPrimerSeen(context, true)
                            ProjectionEventLog.record("PERMISSION", "Permission primer: continuing to the system prompts.")
                            primer.start()
                        },
                        secondaryLabel = motoHubText("Not now"),
                        onSecondary = {
                            ProjectionEventLog.record("PERMISSION", "Permission primer: Not now; the start was cancelled.")
                        }
                    ) {
                        if (primer.notifications) {
                            MhListRow(
                                title = motoHubText("Notifications"),
                                subtitle = motoHubText("Keep streaming visible and let you stop it"),
                                icon = Icons.Rounded.Notifications
                            )
                        }
                        if (primer.microphone) {
                            MhListRow(
                                title = motoHubText("Microphone"),
                                subtitle = motoHubText("For Google Assistant and calls"),
                                icon = Icons.Rounded.Mic
                            )
                        }
                    }
                }
                pendingUnverifiedQr?.let { payload ->
                    UnverifiedQrDialog(
                        payload = payload,
                        onConfirm = {
                            ProjectionEventLog.record(
                                "PAIRING",
                                "Rider confirmed the unrecognised pairing code for ssid=${payload.ssid}."
                            )
                            saveQrPairing(payload)
                        },
                        onDecline = {
                            ProjectionEventLog.record(
                                "PAIRING",
                                "Rider declined the unrecognised pairing code for ssid=${payload.ssid}."
                            )
                        },
                        onDismiss = { pendingUnverifiedQr = null }
                    )
                }
                // The rider had to be on the motorcycle to answer this, so it is asked when they
                // are back at the phone rather than the instant the session ended.
                androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refreshWireQuestion() }
                val wireQuestion = state.wireQuestionFor
                if (overlay == StartupOverlay.WIRE_VERDICT && wireQuestion != null) {
                    WireVerdictDialog(
                        motorcycleName = wireQuestion.displayName?.takeIf { it.isNotBlank() } ?: wireQuestion.ssid,
                        onAnswer = { seen ->
                            wireQuestionDeferred = false
                            viewModel.answerWireQuestion(seen)
                        },
                        // Ask again next time rather than guess an answer. The sheet closes this
                        // way before an answer as well, and the answer then takes it back.
                        onDismiss = { wireQuestionDeferred = true }
                    )
                }
                if (overlay == StartupOverlay.WIRE_NUDGE) {
                    WireNeedsAndroidAutoDialog(onDismiss = { viewModel.dismissWireAndroidAutoNudge() })
                }
                if (overlay == StartupOverlay.COMPANION) {
                    CompanionConflictGateDialog(companionConflictGate)
                }
                if (overlay == StartupOverlay.CRASH_CONSENT) {
                    var alwaysSendReports by rememberSaveable { mutableStateOf(false) }
                    var readingPrivacyNotice by rememberSaveable { mutableStateOf(false) }
                    // The notice replaces the prompt rather than stacking on top of it: the
                    // question stays pending underneath and is put back the moment it is closed,
                    // so reading the terms is never a way to accidentally answer them.
                    if (readingPrivacyNotice) {
                        PrivacyNoticeDialog(onDismiss = { readingPrivacyNotice = false })
                    } else {
                        CrashDiagnosticsConsentDialog(
                            alwaysSend = alwaysSendReports,
                            onAlwaysSendChanged = { alwaysSendReports = it },
                            onSend = {
                                DiagnosticReportScheduler.onCrashReportConsented(context, alwaysSendReports)
                            },
                            onDecline = { DiagnosticReportScheduler.onCrashReportDeclined(context) },
                            onOpenPrivacyNotice = { readingPrivacyNotice = true },
                            // Not an answer: the question comes back next launch. Only the
                            // rider's own yes or no closes it. Also runs, harmlessly, ahead of
                            // either answer.
                            onDismiss = { DiagnosticReportScheduler.dismissCrashPromptForNow() }
                        )
                    }
                }
                if (overlay == StartupOverlay.SAFETY) {
                    // On by default: a full page on every cold start is harassment, and
                    // "I understand" is still the rider's own acknowledgement.
                    var doNotShowAgain by rememberSaveable { mutableStateOf(true) }
                    SafetyDisclaimerDialog(
                        doNotShowAgain = doNotShowAgain,
                        onDoNotShowAgainChanged = { doNotShowAgain = it },
                        onContinue = {
                            if (doNotShowAgain) {
                                MotoHubSettings.setSafetyDisclaimerAcknowledged(context, true)
                            }
                            ProjectionEventLog.record(
                                "SAFETY",
                                "Startup safety disclaimer acknowledged; doNotShowAgain=$doNotShowAgain."
                            )
                            showSafetyDisclaimer = false
                        }
                    )
                }
                // Last, so it floats over every screen. Only the snackbar itself takes taps (one
                // dismisses it); the rest of the host lets them through. Lifted clear of the dock
                // while it shows, and above the keyboard when one is open. The host adds the
                // screen's pinned action (bottomClearance) itself.
                val dockVisible = hubScreen == HubScreenKey.HOME && showDock
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    MotoHubSnackbar.Host(
                        Modifier.navigationBarsPadding().padding(bottom = if (dockVisible) 72.dp else 16.dp)
                    )
                }
            }
        }
    }

  private fun startAndroidAuto() {
      if (!androidAutoLaunchPending.compareAndSet(false, true)) {
            ProjectionEventLog.warning("ANDROID_AUTO", "Start request ignored because another launch is pending.")
            return
        }
        ProjectionEventLog.record("ANDROID_AUTO", "User requested Android Auto startup.")
        AndroidAutoSessionService.start(this)
        lifecycleScope.launch {
            val state = withTimeoutOrNull(10_000L) {
                // A foreground service is started asynchronously.  Ignore terminal state left
                // by the previous launch; otherwise `first` can consume that old failure before
                // the new service has published Preparing and no self-mode trigger is sent.
                AndroidAutoRuntime.state
                    .dropWhile {
                        it is AndroidAutoRuntimeState.Idle ||
                            it is AndroidAutoRuntimeState.Stopped ||
                            it is AndroidAutoRuntimeState.Failed
                    }
                    .first {
                    it is AndroidAutoRuntimeState.ReceiverReady ||
                        it is AndroidAutoRuntimeState.Failed
                    }
            }
            when (state) {
                AndroidAutoRuntimeState.ReceiverReady -> {
                    ProjectionEventLog.record("ANDROID_AUTO", "AAP receiver is ready; waiting before self-mode trigger.")
                    delay(ANDROID_AUTO_RECEIVER_SETTLE_MS)
                    if (AndroidAutoRuntime.state.value is AndroidAutoRuntimeState.ReceiverReady) {
                        AaSelfMode.trigger(
                            context = this@MainActivity,
                            log = { ProjectionEventLog.record("AAP", it) }
                        )
                    }
                }
                is AndroidAutoRuntimeState.Failed -> Unit
                else -> {
                    ProjectionEventLog.error("ANDROID_AUTO", "Timed out while preparing Android Auto.")
                    AndroidAutoSessionService.stop(
                        this@MainActivity,
                        "Android Auto did not become ready in time."
                    )
                }
            }
            androidAutoLaunchPending.set(false)
            ProjectionEventLog.debug("ANDROID_AUTO", "Launch coordinator released.")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshAoaAccessoryConnected(intent)
        handleAndroidAutoPreviewLaunchIntent(intent)
        handleHandlebarBluetoothRequestIntent(intent)
        handleTBoxScanPermissionRequestIntent(intent)
    }

    /**
     * ACTION_USB_ACCESSORY_ATTACHED is resolved by the system straight to an activity
     * launch/onNewIntent (see the manifest's accessory_filter meta-data), never a broadcast -
     * this is the only reliable point to learn a head unit just attached. Also called from
     * onCreate to cover the cold-start case where the accessory launched MOTO-HUB directly.
     */
    private fun refreshAoaAccessoryConnected(intent: Intent?) {
        val viaAttachIntent = intent?.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED
        AoaAccessoryRuntime.publish(
            viaAttachIntent || AoaExternalService.isAccessoryConnected(this)
        )
    }

    override fun onDestroy() {
        ProjectionEventLog.record("UI", "Main activity destroyed. changingConfigurations=$isChangingConfigurations")
        if (!isChangingConfigurations) {
            androidAutoPhoneOnlyBridge.stop()
        }
        super.onDestroy()
    }

    /**
     * Puts THIS app's BLUETOOTH_CONNECT request in front of the rider because the companion app
     * asked, then closes so they land back where they tapped.
     *
     * A runtime permission belongs to a package, and the handlebar of an Android Auto session is
     * decoded here - so this app's grant is the one that decides whether a press can arrive, and
     * every screen that could ask for it is over there. The companion app checked its own grant,
     * found it, and showed a rider a handlebar that could never work: rider 315e0af3 paired the
     * motorcycle, remapped every button and ran the teaching wizard to the end across three days
     * while this app logged "capture skipped: Bluetooth is off or unavailable to this app" in
     * every single session.
     *
     * Answers nothing itself when the grant is already held: the companion asks before sending
     * anyone here, but the two checks are one process apart and the rider may have granted it in
     * between.
     */
    private fun handleHandlebarBluetoothRequestIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(IpcBridgeContract.EXTRA_REQUEST_HANDLEBAR_BLUETOOTH, false) != true) return
        // Removed so a configuration change or a later onNewIntent does not ask again: the
        // launch intent outlives the request it carried.
        intent.removeExtra(IpcBridgeContract.EXTRA_REQUEST_HANDLEBAR_BLUETOOTH)
        if (BluetoothStatus.hasConnectPermission(this)) {
            ProjectionEventLog.record(
                "PERMISSION",
                "The companion app asked for handlebar Bluetooth; this app already holds it."
            )
            finish()
            return
        }
        ProjectionEventLog.record(
            "PERMISSION",
            "Requesting handlebar Bluetooth on the companion app's behalf."
        )
        handlebarBluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    /**
     * Puts THIS app's Wi-Fi/location request in front of the rider because the companion app
     * asked, then closes so they land back where they tapped.
     *
     * The Bluetooth hand-off above exists because a permission belongs to a package and the
     * handlebar is decoded here; this one exists because a permission belongs to a package and
     * the Wi-Fi air is READ here. Every connect a companion app drives runs
     * [io.motohub.android.ipc.CoreTBoxConnector] in this process, and until now nothing on that
     * path ever asked for anything - [tboxConnectPermissions] is reached only from this app's own
     * Connect button, which a rider who drives everything from the companion app never presses.
     * Four supports (fc17a4f7, 36a3fd37, 6e77dcf7, f27f3825) show the result: an empty Wi-Fi scan
     * on every attempt for the life of the installation, and with it every piece of guidance this
     * app has about whether the dash is on the air, on its own access point, or on the wrong
     * channel. See [TBoxScanPermissions].
     *
     * Answers nothing itself when the grants are already held: the companion asks before sending
     * anyone here, but the two checks are one process apart.
     */
    private fun handleTBoxScanPermissionRequestIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(
                IpcBridgeContract.EXTRA_REQUEST_TBOX_WIFI_PERMISSIONS,
                false
            ) != true
        ) {
            return
        }
        // Removed for the reason the handlebar one is: the launch intent outlives the request.
        intent.removeExtra(IpcBridgeContract.EXTRA_REQUEST_TBOX_WIFI_PERMISSIONS)
        if (TBoxScanPermissions.heldBy(this)) {
            ProjectionEventLog.record(
                "PERMISSION",
                "The companion app asked for the Wi-Fi permissions; this app already holds them."
            )
            finish()
            return
        }
        ProjectionEventLog.record(
            "PERMISSION",
            "Requesting the Wi-Fi permissions on the companion app's behalf: " +
                TBoxScanPermissions.missingFor(this)
                    .joinToString { it.substringAfterLast('.') } + "."
        )
        tboxScanPermissionsLauncher.launch(TBoxScanPermissions.required.toTypedArray())
    }

    /** Handles the phone-only Android Auto deep-link sent by PRO. */
    private fun handleAndroidAutoPreviewLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(IpcBridgeContract.EXTRA_START_PHONE_ONLY_ANDROID_AUTO, false) != true) return
        PhoneOnlyAndroidAutoLaunchRequest.publish(
            intent.getStringExtra(IpcBridgeContract.EXTRA_ANDROID_AUTO_DISPLAY_MODE)
        )
    }

    override fun onStart() {
        super.onStart()
        ProjectionEventLog.debug("UI", "Main activity started.")
    }

    override fun onResume() {
        super.onResume()
        ProjectionEventLog.debug("UI", "Main activity resumed.")
        // A report the rider agreed to send while on the dashboard's Wi-Fi has no route out;
        // coming back to the app is the likeliest moment there is one again.
        DiagnosticReportScheduler.retryIfPending(this)
    }

    override fun onPause() {
        ProjectionEventLog.debug("UI", "Main activity paused.")
        super.onPause()
    }

    override fun onStop() {
        ProjectionEventLog.debug("UI", "Main activity stopped.")
        super.onStop()
    }

    private companion object {
        const val ANDROID_AUTO_RECEIVER_SETTLE_MS = 900L
       const val AUTO_CONNECT_START_DELAY_MS = 600L
        const val AUTO_CONNECT_RETRY_COOLDOWN_MS = 5_000L

        /**
         * How often the app re-asks for the bike while it sits on screen with nothing connected.
         *
         * One attempt can occupy 30s of Android's own timeout, so this is the pause BETWEEN
         * attempts, not their period: a failing cycle lands at roughly 45s. Short enough that a
         * rider who switches the dash on and looks at the phone sees it go by itself.
         */
        const val AUTO_CONNECT_WATCH_INTERVAL_MS = 15_000L
        const val AUTO_CONNECT_WATCH_MAX_INTERVAL_MS = 120_000L
        const val AUTOSTART_ON_CONNECT_DELAY_MS = 800L
        const val OFFICIAL_APP_CLOSE_RETRY_DELAY_MS = 1_500L
        const val FOREGROUND_SETTLE_TIMEOUT_MS = 1_000L
        const val FOREGROUND_SETTLE_POLL_MS = 50L
    }
}

/**
 * Runtime permissions a connect to [profile] needs before it can start: the Wi-Fi join set
 * always, plus the Bluetooth pair when this connection will use a radio that needs it — asked
 * together so the rider sees one permission sheet, not one per radio.
 *
 * Two things want Bluetooth. A ThinkerRide (KOVE) motorcycle pairs over it and cannot connect
 * without it at all. An EasyConn dash with the Bluetooth dash-clock setting on needs it for a
 * different reason: that clock is written over BLE to a peripheral that is usually not bonded,
 * so finding it means scanning, and without the grant the scan throws and the setting silently
 * does nothing — which is exactly how it behaved before anyone noticed.
 */
private fun tboxConnectPermissions(
    context: Context,
    profile: MotorcycleProfile?
): Array<String> {
    // The Wi-Fi half is TBoxScanPermissions': the same set the companion app now asks this app
    // about over the bridge, and the same set that decides whether anything here can read the
    // air at all. One list, so the two questions can never drift apart.
    val permissions = TBoxScanPermissions.required.toMutableList()
    if (ThinkerRideGate.requiresBle(profile) || MotoHubSettings.bluetoothClockSync(context)) {
        permissions += ThinkerRideGate.blePermissions
    }
    return permissions.toTypedArray()
}
