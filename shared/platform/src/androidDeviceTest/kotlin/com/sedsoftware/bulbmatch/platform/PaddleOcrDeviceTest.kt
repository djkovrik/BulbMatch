package com.sedsoftware.bulbmatch.platform

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaddleOcrDeviceTest {
    @Test
    fun bundledModelsRecognizeCyrillicAndLatinMarkingOffline() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bytes = instrumentation.context.assets.open("cyrillic_e27_clean.png").use { it.readBytes() }
        val handle = AndroidEphemeralImageHandle(
            source = ImageSource.CAMERA,
            retainedPayload = AndroidImagePayload.EncodedCameraImage(bytes, rotationDegrees = 0),
        )
        val service = AndroidTextRecognitionService(instrumentation.targetContext)

        try {
            val success = assertIs<TextRecognitionResult.Success>(service.recognize(handle))
            val transcript = success.observations.joinToString(" ") { it.text }.uppercase()
            assertTrue("Е27" in transcript || "E27" in transcript, transcript)
            assertTrue("220" in transcript, transcript)
        } finally {
            service.close()
            handle.release()
        }
    }
}
