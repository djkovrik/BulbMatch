package com.sedsoftware.bulbmatch.platform

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Swift/Objective-C presentation bridge.
 *
 * The iOS host must use UIImagePickerController for camera and PHPicker with a
 * single image filter for import. It must not request photo-library
 * authorization and must return encoded bytes directly from the selected item
 * without writing an application file.
 */
interface IosImageSourceHost {
    fun currentCameraPermission(): CameraPermissionState

    fun requestCameraPermission(completion: (CameraPermissionState) -> Unit)

    fun presentCamera(completion: (IosHostImageResult) -> Unit)

    fun presentSingleImagePicker(completion: (IosHostImageResult) -> Unit)

    fun openApplicationSettings(): Boolean
}

sealed interface IosHostImageResult {
    data class Success(val encodedImage: ByteArray) : IosHostImageResult
    data object Cancelled : IosHostImageResult
    data class Failure(val code: ImageFailureCode) : IosHostImageResult
}

class IosImageSourceService(
    private val host: IosImageSourceHost,
) : ImageSourceService {
    private val mutablePermissionState =
        MutableStateFlow<CameraPermissionState>(CameraPermissionState.Unknown)
    override val cameraPermissionState: StateFlow<CameraPermissionState> = mutablePermissionState

    override suspend fun checkCameraPermission(): CameraPermissionState {
        mutablePermissionState.value = CameraPermissionState.Checking
        return host.currentCameraPermission().also {
            mutablePermissionState.value = it
        }
    }

    override suspend fun requestCameraPermission(): CameraPermissionState {
        mutablePermissionState.value = CameraPermissionState.Requesting
        return suspendCancellableCoroutine { continuation ->
            host.requestCameraPermission { state ->
                if (continuation.isActive) {
                    mutablePermissionState.value = state
                    continuation.resume(state)
                }
            }
        }
    }

    override suspend fun captureCameraImage(): ImageAcquisitionResult =
        awaitImage(ImageSource.CAMERA, ImageCancellation.CAPTURE_CANCELLED) {
            host.presentCamera(it)
        }

    override suspend fun pickSingleImage(): ImageAcquisitionResult =
        awaitImage(ImageSource.SYSTEM_PICKER, ImageCancellation.PICKER_CANCELLED) {
            host.presentSingleImagePicker(it)
        }

    override fun openAppSettings(): Boolean =
        host.openApplicationSettings().also { opened ->
            if (opened) mutablePermissionState.value = CameraPermissionState.SettingsPending
        }

    override suspend fun onForegroundResume(): CameraPermissionState = checkCameraPermission()

    private suspend fun awaitImage(
        source: ImageSource,
        cancellation: ImageCancellation,
        present: ((IosHostImageResult) -> Unit) -> Unit,
    ): ImageAcquisitionResult = suspendCancellableCoroutine { continuation ->
        present { result ->
            if (!continuation.isActive) {
                (result as? IosHostImageResult.Success)?.encodedImage?.fill(0)
                return@present
            }
            continuation.resume(
                when (result) {
                    is IosHostImageResult.Success -> ImageAcquisitionResult.Success(
                        IosEphemeralImageHandle(source, result.encodedImage),
                    )
                    IosHostImageResult.Cancelled ->
                        ImageAcquisitionResult.Cancelled(cancellation)
                    is IosHostImageResult.Failure ->
                        ImageAcquisitionResult.Failure(result.code)
                },
            )
        }
    }
}

internal class IosEphemeralImageHandle(
    override val source: ImageSource,
    private var retainedBytes: ByteArray?,
) : EphemeralImageHandle {
    override val isReleased: Boolean
        get() = retainedBytes == null

    internal fun bytesOrNull(): ByteArray? = retainedBytes

    override fun release() {
        retainedBytes?.fill(0)
        retainedBytes = null
    }
}

/**
 * Swift host implementation owns the bundled PaddleOCR/ONNX Runtime pipeline and invokes
 * completion after on-device recognition. It must not log or retain the
 * supplied bytes or recognized text.
 */
interface IosTextRecognitionHost {
    fun recognize(
        encodedImage: ByteArray,
        completion: (IosHostRecognitionResult) -> Unit,
    )

    fun close()
}

sealed interface IosHostRecognitionResult {
    data class Success(val observations: List<TextObservation>) : IosHostRecognitionResult
    data class Failure(val code: TextRecognitionFailureCode) : IosHostRecognitionResult
}

class IosTextRecognitionService(
    private val host: IosTextRecognitionHost,
) : TextRecognitionService {
    private var closed = false

    override suspend fun recognize(image: EphemeralImageHandle): TextRecognitionResult {
        if (closed || image.isReleased) {
            return TextRecognitionResult.Failure(
                TextRecognitionFailureCode.UNSUPPORTED_IMAGE,
            )
        }
        val bytes = (image as? IosEphemeralImageHandle)?.bytesOrNull()
            ?: return TextRecognitionResult.Failure(
                TextRecognitionFailureCode.UNSUPPORTED_IMAGE,
            )
        return suspendCancellableCoroutine { continuation ->
            host.recognize(bytes) { result ->
                if (continuation.isActive) {
                    continuation.resume(
                        when (result) {
                            is IosHostRecognitionResult.Success ->
                                if (result.observations.isEmpty()) {
                                    TextRecognitionResult.Failure(
                                        TextRecognitionFailureCode.NO_TEXT_FOUND,
                                    )
                                } else {
                                    TextRecognitionResult.Success(result.observations)
                                }
                            is IosHostRecognitionResult.Failure ->
                                TextRecognitionResult.Failure(result.code)
                        },
                    )
                }
            }
        }
    }

    override fun close() {
        if (!closed) {
            closed = true
            host.close()
        }
    }
}

/**
 * Implement this in the Swift host with FirebaseCrashlytics. The host receives
 * only a sanitized exception type plus closed enum values; no Throwable message
 * or product/user string crosses the bridge.
 */
interface IosCrashReportingHost {
    fun recordNonFatal(
        exceptionType: String,
        screenCode: ScreenCode?,
        operationCode: OperationCode,
    )
}

class IosCrashReporter(
    private val host: IosCrashReportingHost,
    private val collectionEnabled: Boolean,
) : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, context: CrashContext) {
        if (!collectionEnabled) return
        host.recordNonFatal(
            exceptionType = (throwable::class.qualifiedName ?: "UnknownThrowable")
                .filter { it.isLetterOrDigit() || it == '.' || it == '_' }
                .take(160),
            screenCode = context.screen,
            operationCode = context.operation,
        )
    }
}
