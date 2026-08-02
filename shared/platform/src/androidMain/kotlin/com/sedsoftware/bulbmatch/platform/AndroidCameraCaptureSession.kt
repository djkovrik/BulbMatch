package com.sedsoftware.bulbmatch.platform

import android.content.Context
import android.graphics.ImageFormat
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX controller for SCREEN-002. The host supplies a Preview SurfaceProvider
 * from its Android view; no Compose dependency enters the platform module.
 */
class AndroidCameraCaptureSession private constructor(
    private val cameraProvider: ProcessCameraProvider,
    private val imageCapture: ImageCapture,
    private val camera: Camera,
    private val callbackExecutor: Executor,
) {
    val hasTorch: Boolean
        get() = camera.cameraInfo.hasFlashUnit()

    suspend fun setTorch(enabled: Boolean): Boolean =
        suspendCancellableCoroutine { continuation ->
            val future = camera.cameraControl.enableTorch(enabled && hasTorch)
            future.addListener(
                {
                    if (!continuation.isActive) return@addListener
                    runCatching { future.get() }
                        .onSuccess { continuation.resume(true) }
                        .onFailure { continuation.resume(false) }
                },
                callbackExecutor,
            )
        }

    suspend fun capture(): ImageAcquisitionResult =
        suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                callbackExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        if (!continuation.isActive) {
                            image.close()
                            return
                        }
                        continuation.resume(image.toResult())
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) {
                            continuation.resume(
                                ImageAcquisitionResult.Failure(
                                    ImageFailureCode.CAPTURE_FAILED,
                                    technicalCode = exception.imageCaptureError.toTechnicalCode(),
                                    cause = exception,
                                ),
                            )
                        }
                    }
                },
            )
        }

    fun close() {
        cameraProvider.unbindAll()
    }

    private fun ImageProxy.toResult(): ImageAcquisitionResult {
        return try {
            if (format != ImageFormat.JPEG && format != ImageFormat.JPEG_R) {
                return ImageAcquisitionResult.Failure(ImageFailureCode.UNREADABLE_IMAGE)
            }
            val buffer = planes.firstOrNull()?.buffer
                ?: return ImageAcquisitionResult.Failure(ImageFailureCode.UNREADABLE_IMAGE)
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            if (bytes.isEmpty()) {
                ImageAcquisitionResult.Failure(ImageFailureCode.UNREADABLE_IMAGE)
            } else {
                ImageAcquisitionResult.Success(
                    AndroidEphemeralImageHandle(
                        source = ImageSource.CAMERA,
                        retainedPayload = AndroidImagePayload.EncodedCameraImage(
                            bytes = bytes,
                            rotationDegrees = imageInfo.rotationDegrees,
                        ),
                    ),
                )
            }
        } finally {
            close()
        }
    }

    companion object {
        suspend fun open(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            surfaceProvider: Preview.SurfaceProvider,
        ): AndroidCameraSessionOpenResult {
            return try {
                val executor = ContextCompat.getMainExecutor(context)
                val provider = ProcessCameraProvider.getInstance(context).await(executor)
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = surfaceProvider
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setBufferFormat(ImageFormat.JPEG)
                    .build()
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
                AndroidCameraSessionOpenResult.Success(
                    AndroidCameraCaptureSession(
                        cameraProvider = provider,
                        imageCapture = imageCapture,
                        camera = camera,
                        callbackExecutor = executor,
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                AndroidCameraSessionOpenResult.Failure(ImageFailureCode.CAMERA_UNAVAILABLE)
            }
        }
    }
}

sealed interface AndroidCameraSessionOpenResult {
    data class Success(
        val session: AndroidCameraCaptureSession,
    ) : AndroidCameraSessionOpenResult

    data class Failure(
        val code: ImageFailureCode,
    ) : AndroidCameraSessionOpenResult
}

internal fun Int.toTechnicalCode(): ImageFailureTechnicalCode = when (this) {
    ImageCapture.ERROR_UNKNOWN -> ImageFailureTechnicalCode.CAMERAX_UNKNOWN
    ImageCapture.ERROR_FILE_IO -> ImageFailureTechnicalCode.CAMERAX_FILE_IO
    ImageCapture.ERROR_CAPTURE_FAILED -> ImageFailureTechnicalCode.CAMERAX_CAPTURE_FAILED
    ImageCapture.ERROR_CAMERA_CLOSED -> ImageFailureTechnicalCode.CAMERAX_CAMERA_CLOSED
    ImageCapture.ERROR_INVALID_CAMERA -> ImageFailureTechnicalCode.CAMERAX_INVALID_CAMERA
    else -> ImageFailureTechnicalCode.CAMERAX_UNRECOGNIZED
}

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(
    executor: Executor,
): T = suspendCancellableCoroutine { continuation: CancellableContinuation<T> ->
    addListener(
        {
            if (!continuation.isActive) return@addListener
            runCatching { get() }
                .onSuccess(continuation::resume)
                .onFailure(continuation::resumeWithException)
        },
        executor,
    )
    continuation.invokeOnCancellation { cancel(true) }
}
