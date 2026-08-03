package com.sedsoftware.bulbmatch.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlatformServicesTest {
    @Test
    fun permissionCheckAndSettingsResumeAreSeparateTransitions() = runTest {
        val service = FakeImageSourceService(
            initialPermission = CameraPermissionState.Unknown,
        ).apply {
            checkedPermission = CameraPermissionState.DeniedOpenSettings
        }

        assertEquals(CameraPermissionState.DeniedOpenSettings, service.checkCameraPermission())
        assertEquals(1, service.checkCount)

        assertTrue(service.openAppSettings())
        assertEquals(CameraPermissionState.SettingsPending, service.cameraPermissionState.value)

        service.checkedPermission = CameraPermissionState.Granted
        assertEquals(CameraPermissionState.Granted, service.onForegroundResume())
        assertEquals(1, service.resumeCount)
        assertEquals(2, service.checkCount)
    }

    @Test
    fun cancellationIsNotRepresentedAsFailure() = runTest {
        val service = FakeImageSourceService()

        assertIs<ImageAcquisitionResult.Cancelled>(service.captureCameraImage())
        assertIs<ImageAcquisitionResult.Cancelled>(service.pickSingleImage())
    }

    @Test
    fun ephemeralHandleReleaseIsIdempotent() {
        val handle = FakeEphemeralImageHandle(ImageSource.CAMERA)

        assertFalse(handle.isReleased)
        handle.release()
        handle.release()

        assertTrue(handle.isReleased)
    }

    @Test
    fun recognizerRejectsReleasedImageWithoutReturningText() = runTest {
        val handle = FakeEphemeralImageHandle(ImageSource.SYSTEM_PICKER)
        val service = FakeTextRecognitionService()
        handle.release()

        val error = runCatching { service.recognize(handle) }.exceptionOrNull()

        assertIs<IllegalStateException>(error)
        assertEquals(0, service.recognizeCount)
    }

    @Test
    fun crashRecorderStoresNoThrowableMessage() {
        val reporter = RecordingCrashReporter()
        reporter.recordNonFatal(
            throwable = IllegalStateException("CANARY_USER_OCR_VALUE"),
            context = CrashContext(
                screen = ScreenCode.IMAGE_REVIEW,
                operation = OperationCode.TEXT_RECOGNITION,
                imageFailureTechnicalCode = ImageFailureTechnicalCode.CAMERAX_CAMERA_CLOSED,
            ),
        )

        assertEquals(
            listOf(
                RecordedCrash(
                    exceptionType = "IllegalStateException",
                    context = CrashContext(
                        screen = ScreenCode.IMAGE_REVIEW,
                        operation = OperationCode.TEXT_RECOGNITION,
                        imageFailureTechnicalCode = ImageFailureTechnicalCode.CAMERAX_CAMERA_CLOSED,
                    ),
                ),
            ),
            reporter.reports,
        )
        assertFalse(reporter.reports.toString().contains("CANARY_USER_OCR_VALUE"))
    }
}
