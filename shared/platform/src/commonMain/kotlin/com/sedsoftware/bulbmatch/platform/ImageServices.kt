package com.sedsoftware.bulbmatch.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * A process-local reference to image content.
 *
 * Implementations must not expose a path or URI, persist their payload, or retain
 * it after [release]. Releasing an already released handle is a no-op.
 */
interface EphemeralImageHandle {
    val source: ImageSource
    val isReleased: Boolean

    fun release()
}

enum class ImageSource {
    CAMERA,
    SYSTEM_PICKER,
}

sealed interface CameraPermissionState {
    data object Unknown : CameraPermissionState
    data object Checking : CameraPermissionState
    data object Requesting : CameraPermissionState
    data object Granted : CameraPermissionState
    data object DeniedCanAsk : CameraPermissionState
    data object DeniedOpenSettings : CameraPermissionState
    data object SettingsPending : CameraPermissionState
    data object Unavailable : CameraPermissionState
}

enum class ImageCancellation {
    CAPTURE_CANCELLED,
    PICKER_CANCELLED,
}

enum class ImageFailureCode {
    PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    CAMERA_NOT_READY,
    CAPTURE_FAILED,
    UNREADABLE_IMAGE,
}

sealed interface ImageAcquisitionResult {
    data class Success(val image: EphemeralImageHandle) : ImageAcquisitionResult
    data class Cancelled(val reason: ImageCancellation) : ImageAcquisitionResult
    data class Failure(val code: ImageFailureCode) : ImageAcquisitionResult
}

/**
 * UI-neutral image acquisition boundary.
 *
 * Permission checks and permission requests are intentionally separate. Platform
 * hosts attach their system picker and camera presentation before invoking the
 * corresponding suspend operation.
 */
interface ImageSourceService {
    val cameraPermissionState: StateFlow<CameraPermissionState>

    suspend fun checkCameraPermission(): CameraPermissionState

    suspend fun requestCameraPermission(): CameraPermissionState

    suspend fun captureCameraImage(): ImageAcquisitionResult

    suspend fun pickSingleImage(): ImageAcquisitionResult

    /**
     * Opens this app's settings and marks the permission state as pending.
     * The caller must invoke [onForegroundResume] from its foreground callback.
     */
    fun openAppSettings(): Boolean

    suspend fun onForegroundResume(): CameraPermissionState
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f)
        require(top in 0f..1f)
        require(right in 0f..1f)
        require(bottom in 0f..1f)
        require(left <= right)
        require(top <= bottom)
    }
}

/**
 * Ephemeral recognizer output. Text remains untrusted until common product logic
 * parses it into field candidates and the user reviews every candidate.
 */
data class TextObservation(
    val text: String,
    val bounds: NormalizedRect?,
)

enum class TextRecognitionFailureCode {
    NO_TEXT_FOUND,
    UNSUPPORTED_IMAGE,
    RECOGNITION_FAILED,
}

sealed interface TextRecognitionResult {
    data class Success(val observations: List<TextObservation>) : TextRecognitionResult
    data class Failure(val code: TextRecognitionFailureCode) : TextRecognitionResult
}

interface TextRecognitionService {
    suspend fun recognize(image: EphemeralImageHandle): TextRecognitionResult

    fun close()
}
