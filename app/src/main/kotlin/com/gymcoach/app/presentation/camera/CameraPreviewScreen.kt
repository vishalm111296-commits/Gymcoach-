package com.gymcoach.app.presentation.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gymcoach.app.core.ml.ExerciseConfig
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.core.ml.FeedbackTone
import com.gymcoach.app.core.ml.FormAnalyzer
import com.gymcoach.app.core.ml.PoseDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Lifecycle states for the on-demand pose model bootstrap. */
private sealed interface ModelState {
    data object Loading : ModelState
    data object Ready : ModelState
    data class Error(val message: String) : ModelState
}

/**
 * Live camera workout screen.
 *
 * Pipeline: CameraX [ImageAnalysis] (RGBA_8888, keep-latest) -> upright Bitmap ->
 * MediaPipe PoseLandmarker ([PoseDetector]) -> [FormAnalyzer] -> [CameraOverlay].
 *
 * @param exerciseType exercise whose joint-angle state machine drives rep counting.
 */
@Composable
fun CameraPreviewScreen(
    exerciseType: ExerciseType = ExerciseType.BICEP_CURL,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    // ── Camera permission ────────────────────────────────────
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var userDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) userDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // After an explicit denial the system may stop showing the rationale
    // dialog (Android 11+ "don't ask again"); deep-link to app settings.
    val permanentlyDenied = userDenied && !hasPermission &&
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
    val openAppSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    // ── Pose model bootstrap (download on first launch) ─────
    var detector by remember { mutableStateOf<PoseDetector?>(null) }
    var modelState by remember { mutableStateOf<ModelState>(ModelState.Loading) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(hasPermission, retryKey) {
        if (hasPermission && detector == null) {
            modelState = ModelState.Loading
            try {
                detector = PoseDetector.create(context)
                modelState = ModelState.Ready
            } catch (t: Throwable) {
                modelState = ModelState.Error(t.message ?: "Failed to load pose model")
            }
        }
    }

    // Always read the latest detector from frame-processing lambdas.
    val currentDetector by rememberUpdatedState(detector)

    // ── Per-exercise analyzer + live UI state ───────────────
    val formAnalyzer = remember(exerciseType) {
        FormAnalyzer(exerciseType, FormAnalyzer.Companion.defaultFor(exerciseType))
    }
    var repCount by remember { mutableIntStateOf(0) }
    var formFeedback by remember { mutableStateOf<String?>(null) }
    var feedbackTone by remember { mutableStateOf(FeedbackTone.NEUTRAL) }

    // Single-threaded executor serializes frame inference off the main thread.
    val analyzerExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val frameConverter = remember { FrameConverter() }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
            detector?.close()
            formAnalyzer.close()
        }
    }

    // ── Frame pipeline (runs on analyzerExecutor thread) ────
    fun processFrame(proxy: ImageProxy) {
        try {
            val det = currentDetector
            if (det == null) {
                formFeedback = null
                return
            }
            val bitmap = frameConverter.toUpright(proxy)
            val pose = det.detect(bitmap)
            // Compose snapshot state is thread-safe to write from background threads.
            if (pose != null) {
                val result = formAnalyzer.analyze(pose, System.currentTimeMillis())
                if (result != null) {
                    repCount = result.repCount
                    formFeedback = result.formFeedback
                    feedbackTone = result.feedbackTone
                } else {
                    // Person visible but tracked joints occluded: hold reps, hide stale cue.
                    formFeedback = null
                    feedbackTone = FeedbackTone.NEUTRAL
                }
            } else {
                formFeedback = null
                feedbackTone = FeedbackTone.NEUTRAL
            }
        } catch (t: Throwable) {
            // Never let a bad frame crash the session; surface as missing feedback.
            formFeedback = null
        } finally {
            proxy.close() // MUST always release the buffer back to CameraX.
        }
    }

    // ── UI ──────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !hasPermission -> PermissionRationale(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                permanentlyDenied = permanentlyDenied,
                onOpenSettings = openAppSettings
            )
            modelState is ModelState.Error -> ModelErrorView(
                message = (modelState as ModelState.Error).message,
                onRetry = { retryKey++ }
            )
            else -> Box(modifier = Modifier.fillMaxSize()) {
                if (modelState == ModelState.Ready) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }.also { previewView ->
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                providerFuture.addListener({
                                    val cameraProvider = providerFuture.get()

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setResolutionSelector(
                                            ResolutionSelector.Builder()
                                                .setResolutionStrategy(
                                                    ResolutionStrategy(
                                                        Size(640, 480),
                                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                                    )
                                                )
                                                .build()
                                        )
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(analyzerExecutor) { proxy ->
                                                processFrame(proxy)
                                            }
                                        }

                                    val selector = CameraSelector.Builder()
                                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                                        .build()

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        imageAnalysis
                                    )
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        }
                    )
                    CameraOverlay(
                        repCount = repCount,
                        formFeedback = formFeedback,
                        feedbackTone = feedbackTone
                    )
                } else {
                    // Model still downloading/initializing.
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Preparing AI coach\u2026")
                    }
                }
            }
        }

        // Explicit close affordance: camera is a full-screen surface and
        // users expect a visible way back, not just the system back gesture.
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Text(text = "\u2715", fontSize = 26.sp)
        }
    }
}

@Composable
private fun PermissionRationale(
    onRequest: () -> Unit,
    permanentlyDenied: Boolean,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "GymCoach needs camera access to count your reps and coach your form in real time.",
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (permanentlyDenied) {
            // The system dialog will no longer appear; the only way forward is
            // the OS app-settings screen (Android 11+ denial policy).
            Text(
                text = "Camera access is turned off. You can enable it in system settings.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onOpenSettings) { Text(text = "Open settings") }
        } else {
            Button(onClick = onRequest) { Text(text = "Grant camera access") }
        }
    }
}

@Composable
private fun ModelErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "Couldn't load the pose model:\n$message",
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(text = "Retry") }
    }
}

/**
 * Reuses buffers across frames to avoid per-frame allocations:
 * RGBA_8888 ImageProxy -> ARGB_8888 Bitmap -> rotation-corrected Bitmap.
 *
 * The rotated output is cached and redrawn each frame instead of allocating a
 * new Bitmap per frame (previously `Bitmap.createBitmap(...)` ran at every
 * frame with nonzero rotation, churning the heap at camera frame rate).
 */
private class FrameConverter {
    private var sourceBitmap: Bitmap? = null
    private var rotatedBitmap: Bitmap? = null
    private var rotatedKey: Triple<Int, Int, Float>? = null

    /** Produces an upright bitmap matching natural device orientation. */
    fun toUpright(proxy: ImageProxy): Bitmap {
        val src = sourceBitmap
            ?.takeIf { it.width == proxy.width && it.height == proxy.height }
            ?: Bitmap.createBitmap(
                proxy.width, proxy.height, Bitmap.Config.ARGB_8888
            ).also { sourceBitmap = it }

        proxy.planes[0].buffer.rewind()
        src.copyPixelsFromBuffer(proxy.planes[0].buffer)

        val rotationDegrees = proxy.imageInfo.rotationDegrees.toFloat()
        if (rotationDegrees == 0f) return src

        val key = Triple(src.width, src.height, rotationDegrees)
        val rotated = rotatedBitmap?.takeIf { rotatedKey == key }
        if (rotated == null) {
            // First frame for this (size, rotation): allocate the rotated output.
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
                .also { rotatedBitmap = it; rotatedKey = key }
        }

        // Subsequent frames: redraw the freshest src pixels through the same
        // rotation matrix into the cached bitmap — no per-frame allocation.
        val canvas = Canvas(rotated)
        canvas.drawBitmap(src, Matrix().apply { postRotate(rotationDegrees) }, null)
        return rotated
    }
}
