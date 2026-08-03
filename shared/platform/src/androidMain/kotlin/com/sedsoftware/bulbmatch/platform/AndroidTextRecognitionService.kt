package com.sedsoftware.bulbmatch.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.paddle.ocr.EngineConfig
import com.paddle.ocr.PaddleOCR
import com.paddle.ocr.PaddleOCRConfig
import com.paddle.ocr.model.OCRRunResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.opencv.android.OpenCVLoader
import kotlin.math.max

/**
 * Offline PaddleOCR pipeline backed by bundled ONNX models. Model initialization and
 * recognition are serialized because ONNX Runtime sessions are shared across calls.
 */
class AndroidTextRecognitionService(
    context: Context,
    private val maxImageDimension: Int = 2048,
) : TextRecognitionService {
    private val applicationContext = context.applicationContext
    private val mutex = Mutex()
    @Volatile
    private var closed = false
    private var recognizer: PaddleOCR? = null

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

        return try {
            mutex.withLock {
                if (closed) {
                    TextRecognitionResult.Failure(TextRecognitionFailureCode.UNSUPPORTED_IMAGE)
                } else {
                    OpenCvRuntime.ensureLoaded()
                    val paddle = recognizer ?: createRecognizer().also { recognizer = it }
                    paddle.recognize(bitmap).toPlatformResult(bitmap.width, bitmap.height)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            TextRecognitionResult.Failure(
                TextRecognitionFailureCode.RECOGNITION_FAILED,
            )
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runBlocking {
            mutex.withLock {
                recognizer?.release()
                recognizer = null
            }
        }
    }

    private suspend fun createRecognizer(): PaddleOCR = PaddleOCR.create(
        context = applicationContext,
        config = PaddleOCRConfig(
            detLimitSideLen = DETECTION_LIMIT_PX,
            detLimitType = "max",
            detMaxCandidates = 1000,
            recScoreThresh = MIN_RECOGNITION_CONFIDENCE,
        ),
        engineConfig = EngineConfig(),
        detModelAssetPath = DETECTION_MODEL_ASSET,
        recModelAssetPath = RECOGNITION_MODEL_ASSET,
        recConfigAssetPath = RECOGNITION_CONFIG_ASSET,
    )

    private fun OCRRunResult.toPlatformResult(width: Int, height: Int): TextRecognitionResult {
        val observations = results.mapNotNull { result ->
            val value = result.text.trim()
            if (value.isEmpty()) {
                null
            } else {
                val xs = result.box.points.map { it.x }
                val ys = result.box.points.map { it.y }
                TextObservation(
                    text = value,
                    bounds = NormalizedRect(
                        left = (xs.minOrNull().orZero() / width).coerceIn(0f, 1f),
                        top = (ys.minOrNull().orZero() / height).coerceIn(0f, 1f),
                        right = (xs.maxOrNull().orZero() / width).coerceIn(0f, 1f),
                        bottom = (ys.maxOrNull().orZero() / height).coerceIn(0f, 1f),
                    ),
                )
            }
        }
        return if (observations.isEmpty()) {
            TextRecognitionResult.Failure(TextRecognitionFailureCode.NO_TEXT_FOUND)
        } else {
            TextRecognitionResult.Success(observations)
        }
    }

    private fun Float?.orZero(): Float = this ?: 0f

    private companion object {
        const val DETECTION_MODEL_ASSET = "Models/det/inference.onnx"
        const val RECOGNITION_MODEL_ASSET = "Models/rec/inference.onnx"
        const val RECOGNITION_CONFIG_ASSET = "Models/rec/inference.yml"
        const val DETECTION_LIMIT_PX = 960
        const val MIN_RECOGNITION_CONFIDENCE = 0.35f
    }
}

/** OpenCV JNI is process-global and must be loaded before the first native Mat call. */
private object OpenCvRuntime {
    @Volatile
    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            check(OpenCVLoader.initLocal()) {
                "Bundled OpenCV native runtime initialization failed"
            }
            loaded = true
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
