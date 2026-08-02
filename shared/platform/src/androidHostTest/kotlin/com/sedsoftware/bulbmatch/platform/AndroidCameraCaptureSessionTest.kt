package com.sedsoftware.bulbmatch.platform

import androidx.camera.core.ImageCapture
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidCameraCaptureSessionTest {
    @Test
    fun imageCaptureExceptionCodesRemainTypedForDiagnostics() {
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_UNKNOWN,
            ImageCapture.ERROR_UNKNOWN.toTechnicalCode(),
        )
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_FILE_IO,
            ImageCapture.ERROR_FILE_IO.toTechnicalCode(),
        )
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_CAPTURE_FAILED,
            ImageCapture.ERROR_CAPTURE_FAILED.toTechnicalCode(),
        )
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_CAMERA_CLOSED,
            ImageCapture.ERROR_CAMERA_CLOSED.toTechnicalCode(),
        )
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_INVALID_CAMERA,
            ImageCapture.ERROR_INVALID_CAMERA.toTechnicalCode(),
        )
        assertEquals(
            ImageFailureTechnicalCode.CAMERAX_UNRECOGNIZED,
            Int.MAX_VALUE.toTechnicalCode(),
        )
    }
}
