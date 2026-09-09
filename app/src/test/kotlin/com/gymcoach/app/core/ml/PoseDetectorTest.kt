package com.gymcoach.app.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class PoseDetectorTest {

    @Test
    fun `model file name is pose_landmarker_lite task`() {
        assertEquals("pose_landmarker_lite.task", PoseDetector.MODEL_FILE_NAME)
    }

    @Test
    fun `model URL uses secure HTTPS connection`() {
        val field = try {
            PoseDetector::class.java.getDeclaredField("MODEL_URL")
        } catch (e: NoSuchFieldException) {
            PoseDetector.Companion::class.java.getDeclaredField("MODEL_URL")
        }
        field.isAccessible = true
        val urlString = field.get(null) as String

        assertTrue("Model URL must start with https://", urlString.startsWith("https://"))

        val url = URL(urlString)
        val connection = url.openConnection()
        assertTrue("Connection must be an instance of HttpsURLConnection", connection is HttpsURLConnection)
    }
}
