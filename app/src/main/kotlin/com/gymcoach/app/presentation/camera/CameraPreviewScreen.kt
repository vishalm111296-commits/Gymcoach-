package com.gymcoach.app.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
    exerciseType: ExerciseType = ExerciseType.BICEP_CURL
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Camera permission ────────────────────────────────────
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
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
        FormAnalyzer(exerciseType, ExerciseConfig.defaultFor(exerciseType))
    }
    var repCount by remember { mutableIntStateOf(0) }
    var formFeedback by remember { mutableStateOf<String?>(null) }

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
                } else {
                    // Person visible but tracked joints occluded: hold reps, hide stale cue.
                    formFeedback = null
                }
            } else {
                formFeedback = null
            }
        } catch (t: Throwable) {
            // Never let a bad frame crash the session; surface as missing feedback.
            formFeedback = null
        } finally {
            proxy.close() // MUST always release the buffer back to CameraX.
        }
    }

    // ── UI ──────────────────────────────────────────────────
    when {
        !hasPermission -> PermissionRationale(
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
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
                                    .setTargetResolution(Size(640, 480))
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
                    formFeedback = formFeedback
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
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
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
        Button(onClick = onRequest) { Text(text = "Grant camera access") }
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
 */
private class FrameConverter {
    private var sourceBitmap: Bitmap? = null

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

        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        // Note: interior joint angles computed downstream are invariant to mirroring,
        // so no front-camera flip is required for coaching correctness.
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
