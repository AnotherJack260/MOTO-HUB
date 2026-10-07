// SPDX-License-Identifier: AGPL-3.0-only
// Copyright (C) 2026 Vincenzo Buonomano and the MOTO-HUB contributors.
// Part of MOTO-HUB. Free software under the GNU AGPL v3; see LICENSE.
package io.motohub.android.feature.pairing

import io.motohub.android.i18n.motoHubText

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import io.motohub.android.ui.components.MhMotion
import io.motohub.android.ui.components.MhNavIcon
import io.motohub.android.ui.components.MhSecondaryButton
import io.motohub.android.ui.components.MhTopBar
import io.motohub.android.ui.components.MhTopBarAction
import io.motohub.android.ui.theme.MotoHubColors

@Composable
fun TBoxQrScannerScreen(
    onPayload: (TBoxQrPayload) -> Unit,
    onManualPairing: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val activity = context.findActivity()
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        if (activity != null) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    var scanStatus by remember { mutableStateOf(motoHubText("Point at the QR code on your dashboard")) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var torchAvailable by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    // A code that was read: the frame turns lime and dips, the phone confirms, and only then,
    // 250 ms later, does the screen go. The rider holding the phone at the dashboard gets an
    // unmistakable "got it" instead of the camera simply vanishing.
    var found by remember { mutableStateOf<TBoxQrPayload?>(null) }
    val view = LocalView.current
    val currentOnPayload by rememberUpdatedState(onPayload)
    LaunchedEffect(found) {
        val payload = found ?: return@LaunchedEffect
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        delay(FOUND_HOLD_MILLIS)
        currentOnPayload(payload)
    }
    val frameColor by animateColorAsState(
        if (found != null) MotoHubColors.Lime else Color.White.copy(alpha = 0.9f),
        tween(MhMotion.FAST),
        label = "qr-frame"
    )
    val frameScale by animateFloatAsState(if (found != null) 0.96f else 1f, MhMotion.pop(), label = "qr-scale")
    fun setZoom(requestedRatio: Float) {
        val value = requestedRatio.coerceIn(minZoomRatio, maxZoomRatio)
        zoomRatio = value
        camera?.cameraControl?.setZoomRatio(value)
    }

    DisposableEffect(cameraProviderFuture, scanner) {
        onDispose {
            scanner.close()
            cameraProviderFuture.addListener({
                runCatching { cameraProviderFuture.get().unbindAll() }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).also { view ->
                    previewView = view
                    view.apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    cameraProviderFuture.addListener({
                        val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull()
                            ?: return@addListener
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(android.util.Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    ContextCompat.getMainExecutor(viewContext),
                                    TBoxQrAnalyzer(
                                        scanner = scanner,
                                        onPayload = { found = it },
                                        onStatus = { scanStatus = it },
                                        onWrongCode = { view.performHapticFeedback(HapticFeedbackConstants.REJECT) }
                                    )
                                )
                            }
                        runCatching {
                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            camera = boundCamera
                            boundCamera.cameraInfo.zoomState.value?.let { zoomState ->
                                minZoomRatio = zoomState.minZoomRatio
                                maxZoomRatio = zoomState.maxZoomRatio
                                zoomRatio = zoomState.zoomRatio
                            }
                            torchAvailable = boundCamera.cameraInfo.hasFlashUnit()
                        }
                    }, ContextCompat.getMainExecutor(viewContext))
                }
                }
            }
        )

        // Tap anywhere in the camera image to focus on the QR code. This is especially useful
        // when the code is displayed behind TFT glass or at an angle.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera, previewView) {
                    detectTapGestures { offset ->
                        val activeCamera = camera ?: return@detectTapGestures
                        val activePreview = previewView ?: return@detectTapGestures
                        val point = activePreview.meteringPointFactory.createPoint(offset.x, offset.y)
                        activeCamera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                        )
                        scanStatus = motoHubText("Hold steady")
                    }
                }
                // Pinch for fine zoom. A separate detector: it only takes over once a touch moves
                // past the slop, so a tap still reaches the focus detector above.
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) setZoom(zoomRatio * zoom)
                    }
                }
        )

        // The close glyph is the plain one every screen has; the shade behind it keeps it readable
        // over a bright camera image.
        MhTopBar(
            onBack = onClose,
            navIcon = MhNavIcon.CLOSE,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))),
            actions = {
                if (torchAvailable) {
                    MhTopBarAction(
                        icon = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = if (torchEnabled) motoHubText("Turn off flash") else motoHubText("Turn on flash"),
                        active = torchEnabled,
                        onClick = {
                            val enabled = !torchEnabled
                            camera?.cameraControl?.enableTorch(enabled)
                            torchEnabled = enabled
                        }
                    )
                }
            }
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Still while searching - the camera image already moves. Both values are read where
            // they draw, so the "got it" repaints the frame without recomposing the screen.
            Box(
                modifier = Modifier
                    .size(268.dp)
                    .graphicsLayer {
                        scaleX = frameScale
                        scaleY = frameScale
                    }
                    .drawBehind {
                        val stroke = 2.dp.toPx()
                        drawRoundRect(
                            color = frameColor,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(28.dp.toPx() - stroke / 2),
                            style = Stroke(stroke)
                        )
                    }
            )
            // No line limit: the parser's own verdict on an unusable code runs to three sentences.
            // The line swaps with a quick fade and its box follows on the shared clock; the live
            // region sits on the box, so TalkBack reads each new line once.
            AnimatedContent(
                targetState = scanStatus,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                transitionSpec = {
                    fadeIn(tween(MhMotion.FAST)) togetherWith fadeOut(tween(MhMotion.FAST)) using
                        SizeTransform(clip = false) { _, _ -> tween(MhMotion.BASE, easing = MhMotion.Standard) }
                },
                contentAlignment = Alignment.Center,
                label = "qr-status"
            ) { status ->
                Text(
                    status,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 320.dp)
                        .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (maxZoomRatio > minZoomRatio + 0.01f) {
                // The presets are the glove path; pinch does the rest.
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .padding(4.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ZoomButton("1×", minZoomRatio, zoomRatio, ::setZoom)
                    ZoomButton("2×", 2f, zoomRatio, ::setZoom)
                    ZoomButton(motoHubText("Max"), maxZoomRatio, zoomRatio, ::setZoom)
                }
            }
            // Some dashes print the SSID and passphrase instead of a code, and a code the
            // parser cannot use leaves this screen scanning indefinitely. Without a way out
            // from here the rider has to guess that the home screen offers one.
            MhSecondaryButton(motoHubText("Enter details manually"), onManualPairing)
        }
    }
}

@Composable
private fun ZoomButton(
    label: String,
    requestedRatio: Float,
    currentRatio: Float,
    onZoom: (Float) -> Unit
) {
    val selected = kotlin.math.abs(currentRatio - requestedRatio) < 0.08f
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 64.dp)
            .clip(CircleShape)
            .background(if (selected) MotoHubColors.SurfaceHighest else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = { onZoom(requestedRatio) }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private class TBoxQrAnalyzer(
    private val scanner: BarcodeScanner,
    private val onPayload: (TBoxQrPayload) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onWrongCode: () -> Unit
) : ImageAnalysis.Analyzer {
    private val processing = AtomicBoolean(false)
    private val delivered = AtomicBoolean(false)
    private val rejects = RejectThrottle()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        if (!processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes ->
                val rawValue = codes.firstOrNull { it.rawValue != null }?.rawValue
                if (rawValue == null) return@addOnSuccessListener
                onStatus(motoHubText("Reading the code…"))
                val payload = TBoxQrParser.parse(rawValue).getOrElse { failure ->
                    // The parser names what it actually read (vehicle-info code, a bare web
                    // address, the wrong Moto Morini screen), so its own words beat a generic
                    // "unrecognized" that leaves the rider polishing the display.
                    onStatus(
                        failure.message?.takeIf(String::isNotBlank)
                            ?: motoHubText("That's not a dashboard QR code")
                    )
                    if (rejects.shouldBuzz(rawValue, SystemClock.uptimeMillis())) onWrongCode()
                    return@addOnSuccessListener
                }
                if (delivered.compareAndSet(false, true)) onPayload(payload)
            }
            .addOnFailureListener {
                onStatus(motoHubText("Couldn't read it. Hold steady and try again."))
            }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }
}

/**
 * When a wrong code earns a REJECT buzz. The analyzer reads the same code frame after frame, so a
 * read can't be the trigger, and the status text can't either (two different codes can share a
 * verdict). Keyed on the raw payload: a code buzzes once while the camera keeps seeing it, a
 * different code buzzes at once, and the same code buzzes again only after it has been out of
 * sight for [quietMs].
 */
internal class RejectThrottle(private val quietMs: Long = 2_000) {
    private var lastRaw: String? = null
    private var lastSeenAt = 0L

    fun shouldBuzz(raw: String, nowMs: Long): Boolean {
        val buzz = raw != lastRaw || nowMs - lastSeenAt >= quietMs
        lastRaw = raw
        lastSeenAt = nowMs
        return buzz
    }
}

private const val FOUND_HOLD_MILLIS = 250L
