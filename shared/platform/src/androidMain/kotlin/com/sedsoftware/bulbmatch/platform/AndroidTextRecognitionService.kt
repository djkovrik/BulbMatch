package com.sedsoftware.bulbmatch.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * Bundled Latin-script Text Recognition v2. The `com.google.mlkit` artifact
 * includes its model in the application and does not perform a first-run model
 * download.
 */
class AndroidTextRecognitionService(
    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val maxImageDimension: Int = 2048,
) : TextRecognitionService {
    private var closed = false

    override suspend fun recognize(image: EphemeralImageHandle): TextRecognitionResult {
        if (closed || image.isReleased) {
            return TextRecognitionResult.Failure(
                TextRecognitionFailureCode.UNSUPPORTED_IMAGE,
            )
        }
        val androidHandle = image as? AndroidEphemeralImageHandle
            ?: return TextRecognitionResult.Failure(
                TextRecognitionFailureCode.UNSUPPORTED_IMAGE,
            )
        val bitmap = androidHandle.decodeDownsampledBitmap(maxImageDimension)
            ?: return TextRecognitionResult.Failure(
                TextRecognitionFailureCode.UNSUPPORTED_IMAGE,
            )

        val task = recognizer.process(InputImage.fromBitmap(bitmap, 0))
        var recycleAfterTask = false
        return try {
            val recognized = task.await()
            recognized.toPlatformResult(bitmap.width, bitmap.height)
        } catch (cancelled: CancellationException) {
            recycleAfterTask = true
            task.addOnCompleteListener { bitmap.recycle() }
            throw cancelled
        } catch (_: Throwable) {
            TextRecognitionResult.Failure(
                TextRecognitionFailureCode.RECOGNITION_FAILED,
            )
        } finally {
            if (!recycleAfterTask) bitmap.recycle()
        }
    }

    override fun close() {
        if (!closed) {
            closed = true
            recognizer.close()
        }
    }

    private fun Text.toPlatformResult(width: Int, height: Int): TextRecognitionResult {
        val observations = textBlocks
            .asSequence()
            .flatMap { block -> block.lines.asSequence() }
            .mapNotNull { line ->
                val value = line.text.trim()
                if (value.isEmpty()) null else {
                    TextObservation(
                        text = value,
                        bounds = line.boundingBox?.let { rect ->
                            NormalizedRect(
                                left = (rect.left.toFloat() / width).coerceIn(0f, 1f),
                                top = (rect.top.toFloat() / height).coerceIn(0f, 1f),
                                right = (rect.right.toFloat() / width).coerceIn(0f, 1f),
                                bottom = (rect.bottom.toFloat() / height).coerceIn(0f, 1f),
                            )
                        },
                    )
                }
            }
            .toList()
        return if (observations.isEmpty()) {
            TextRecognitionResult.Failure(TextRecognitionFailureCode.NO_TEXT_FOUND)
        } else {
            TextRecognitionResult.Success(observations)
        }
    }
}

private fun AndroidEphemeralImageHandle.decodeDownsampledBitmap(
    maxDimension: Int,
): Bitmap? {
    require(maxDimension > 0)
    return when (val payload = payloadOrNull()) {
        is AndroidImagePayload.EncodedCameraImage ->
            decodeBytes(payload.bytes, payload.rotationDegrees, maxDimension)
        is AndroidImagePayload.PickerUri -> {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            payload.contentResolver.openInputStream(payload.uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    maxDimension = maxDimension,
                )
            }
            payload.contentResolver.openInputStream(payload.uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
        null -> null
    }
}

/**
 * Decodes a bounded host-only preview without exposing a URI/path or persisting image content.
 * The caller owns and must recycle the returned bitmap.
 */
fun EphemeralImageHandle.decodeAndroidPreviewBitmap(
    maxDimension: Int = 1600,
): Bitmap? = (this as? AndroidEphemeralImageHandle)
    ?.decodeDownsampledBitmap(maxDimension)

private fun decodeBytes(
    bytes: ByteArray,
    rotationDegrees: Int,
    maxDimension: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = maxDimension,
        )
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    if (rotationDegrees % 360 == 0) return decoded
    val rotated = Bitmap.createBitmap(
        decoded,
        0,
        0,
        decoded.width,
        decoded.height,
        Matrix().apply { postRotate(rotationDegrees.toFloat()) },
        true,
    )
    if (rotated !== decoded) decoded.recycle()
    return rotated
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    while (max(width / sample, height / sample) > maxDimension && sample <= 64) {
        sample *= 2
    }
    return sample
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation: CancellableContinuation<T> ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener {
            if (continuation.isActive) {
                continuation.resumeWith(
                    Result.failure(it),
                )
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
