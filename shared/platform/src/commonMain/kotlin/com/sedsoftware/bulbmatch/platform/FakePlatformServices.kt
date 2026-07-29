package com.sedsoftware.bulbmatch.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeEphemeralImageHandle(
    override val source: ImageSource,
) : EphemeralImageHandle {
    override var isReleased: Boolean = false
        private set

    override fun release() {
        isReleased = true
    }
}

class FakeImageSourceService(
    initialPermission: CameraPermissionState = CameraPermissionState.Unknown,
) : ImageSourceService {
    private val mutablePermissionState = MutableStateFlow(initialPermission)
    override val cameraPermissionState: StateFlow<CameraPermissionState> = mutablePermissionState

    var checkedPermission: CameraPermissionState = initialPermission
    var requestedPermission: CameraPermissionState = initialPermission
    var captureResult: ImageAcquisitionResult =
        ImageAcquisitionResult.Cancelled(ImageCancellation.CAPTURE_CANCELLED)
    var pickerResult: ImageAcquisitionResult =
        ImageAcquisitionResult.Cancelled(ImageCancellation.PICKER_CANCELLED)
    var settingsCanOpen: Boolean = true

    var checkCount: Int = 0
        private set
    var requestCount: Int = 0
        private set
    var resumeCount: Int = 0
        private set

    override suspend fun checkCameraPermission(): CameraPermissionState {
        checkCount += 1
        mutablePermissionState.value = CameraPermissionState.Checking
        return checkedPermission.also { mutablePermissionState.value = it }
    }

    override suspend fun requestCameraPermission(): CameraPermissionState {
        requestCount += 1
        mutablePermissionState.value = CameraPermissionState.Requesting
        return requestedPermission.also { mutablePermissionState.value = it }
    }

    override suspend fun captureCameraImage(): ImageAcquisitionResult = captureResult

    override suspend fun pickSingleImage(): ImageAcquisitionResult = pickerResult

    override fun openAppSettings(): Boolean {
        if (settingsCanOpen) {
            mutablePermissionState.value = CameraPermissionState.SettingsPending
        }
        return settingsCanOpen
    }

    override suspend fun onForegroundResume(): CameraPermissionState {
        resumeCount += 1
        return checkCameraPermission()
    }
}

class FakeTextRecognitionService : TextRecognitionService {
    var nextResult: TextRecognitionResult =
        TextRecognitionResult.Failure(TextRecognitionFailureCode.NO_TEXT_FOUND)
    var recognizeCount: Int = 0
        private set
    var closed: Boolean = false
        private set

    override suspend fun recognize(image: EphemeralImageHandle): TextRecognitionResult {
        check(!closed)
        check(!image.isReleased)
        recognizeCount += 1
        return nextResult
    }

    override fun close() {
        closed = true
    }
}

data class RecordedCrash(
    val exceptionType: String,
    val context: CrashContext,
)

class RecordingCrashReporter : CrashReporter {
    private val mutableReports = mutableListOf<RecordedCrash>()
    val reports: List<RecordedCrash> get() = mutableReports.toList()

    override fun recordNonFatal(throwable: Throwable, context: CrashContext) {
        mutableReports += RecordedCrash(
            exceptionType = throwable::class.simpleName ?: "UnknownThrowable",
            context = context,
        )
    }
}
