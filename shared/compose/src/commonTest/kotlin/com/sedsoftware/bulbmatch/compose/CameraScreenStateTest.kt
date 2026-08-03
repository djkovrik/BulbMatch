package com.sedsoftware.bulbmatch.compose

import com.sedsoftware.bulbmatch.app.CameraComponent
import com.sedsoftware.bulbmatch.app.CameraStatus
import com.sedsoftware.bulbmatch.app.ImageFailure
import com.sedsoftware.bulbmatch.compose.model.CameraState
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraScreenStateTest {
    @Test
    fun ac002CaptureKeepsContentStateAndNativePreviewLifecycleActive() {
        val capturing = CameraComponent.Model(
            status = CameraStatus.Granted,
            torchAvailable = true,
            torchEnabled = false,
            captureInProgress = true,
            error = null,
        )

        assertEquals(CameraState.Content, cameraScreenState(capturing))
    }

    @Test
    fun captureFailureLeavesContentAndShowsRecoveryState() {
        val failed = CameraComponent.Model(
            status = CameraStatus.Granted,
            torchAvailable = true,
            torchEnabled = false,
            captureInProgress = false,
            error = ImageFailure.CaptureFailed,
        )

        assertEquals(CameraState.Error, cameraScreenState(failed))
    }
}
