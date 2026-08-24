package com.gymcoach.app.core.ml

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bridges CameraX frames to MediaPipe PoseLandmarker and converts raw
 * MediaPipe results into GymCoach [Pose] objects consumable by [FormAnalyzer].
 *
 * The underlying pose model (pose_landmarker_lite.task) is downloaded on first
 * launch and cached in the app's private storage, keeping the APK lean.
 *
 * Thread-safety: [detect] is intended to be called sequentially from a single
 * CameraX analyzer executor thread.
 */
class PoseDetector private constructor(
    private val landmarker: PoseLandmarker
) {

    /**
     * Runs synchronous pose detection on an upright RGB bitmap.
     *
     * @return a [Pose] with 33 normalized landmarks, or null when no person
     *         was detected in the frame.
     */
    fun detect(bitmap: Bitmap): Pose? {
        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detect(mpImage)
        val poseLandmarks = result.landmarks().firstOrNull() ?: return null
        val gymLandmarks = poseLandmarks.map { lm ->
            NormalizedLandmark(x = lm.x(), y = lm.y(), z = lm.z())
        }
        val visibility = poseLandmarks.map { lm ->
            lm.visibility().orElse(0f)
        }
        return Pose(landmarks = gymLandmarks, visibility = visibility)
    }

    /** Releases native MediaPipe resources. Must be called when done. */
    fun close() {
        landmarker.close()
    }

    companion object {
        const val MODEL_FILE_NAME = "pose_landmarker_lite.task"

        // Official MediaPipe-hosted float16 lite model (~5 MB).
        private const val MODEL_URL =
            "https://storage.googleapis.com/mediapipe-models/pose_landmarker/" +
                "pose_landmarker_lite/float16/latest/pose_landmarker_lite.task"

        // Sanity threshold: the real model is several MB; anything smaller is corrupt/incomplete.
        private const val MIN_VALID_MODEL_BYTES = 1024L * 1024L

        /**
         * Ensures the pose model exists locally (downloading it on first launch),
         * then builds a ready-to-use [PoseDetector].
         *
         * @throws Exception when download or initialization fails; callers should
         *                   surface the message and offer a retry.
         */
        suspend fun create(context: Context): PoseDetector = withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val modelFile = ensureModelFile(appContext)

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelFile.absolutePath)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            PoseDetector(PoseLandmarker.createFromOptions(appContext, options))
        }

        /**
         * Returns a validated local model file, downloading it via HTTPS with an
         * atomic rename if missing or truncated.
         */
        private fun ensureModelFile(context: Context): File {
            val target = File(context.filesDir, MODEL_FILE_NAME)
            if (target.exists() && target.length() >= MIN_VALID_MODEL_BYTES) return target

            val tmp = File(context.filesDir, "$MODEL_FILE_NAME.tmp")
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                }
                val code = connection.responseCode
                check(code in 200..299) { "Model download failed: HTTP $code" }
                connection.inputStream.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE * 16) }
                }
                check(tmp.length() >= MIN_VALID_MODEL_BYTES) {
                    "Downloaded model incomplete (${tmp.length()} bytes)"
                }
                if (target.exists()) target.delete()
                check(tmp.renameTo(target)) { "Could not finalize model file" }
                return target
            } finally {
                tmp.delete()
                connection?.disconnect()
            }
        }
    }
}
