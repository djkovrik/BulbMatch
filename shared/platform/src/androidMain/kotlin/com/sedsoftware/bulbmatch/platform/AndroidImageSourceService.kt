package com.sedsoftware.bulbmatch.platform

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Registers activity-result launchers at construction time. Create this in the
 * host Activity's onCreate, before it reaches STARTED.
 *
 * Camera UI is owned by the host. Attach the active in-memory CameraX session
 * while SCREEN-002 is visible; picker and permission prompts remain system UI.
 */
class AndroidImageSourceService(
    private val activity: ComponentActivity,
) : ImageSourceService {
    private val mutablePermissionState =
        MutableStateFlow<CameraPermissionState>(CameraPermissionState.Unknown)
    override val cameraPermissionState: StateFlow<CameraPermissionState> = mutablePermissionState

    private var permissionContinuation: CancellableContinuation<CameraPermissionState>? = null
    private var pickerContinuation: CancellableContinuation<ImageAcquisitionResult>? = null
    private var cameraSession: AndroidCameraCaptureSession? = null
    private val permissionHistory = activity.getSharedPreferences(
        "bulbmatch_platform_permission_history",
        Context.MODE_PRIVATE,
    )

    private val cameraPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val state = if (granted) {
                CameraPermissionState.Granted
            } else {
                deniedState()
            }
            mutablePermissionState.value = state
            permissionContinuation?.takeIf { it.isActive }?.resume(state)
            permissionContinuation = null
        }

    private val photoPickerLauncher =
        activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            val result = if (uri == null) {
                ImageAcquisitionResult.Cancelled(ImageCancellation.PICKER_CANCELLED)
            } else {
                ImageAcquisitionResult.Success(
                    AndroidEphemeralImageHandle(
                        source = ImageSource.SYSTEM_PICKER,
                        retainedPayload = AndroidImagePayload.PickerUri(
                            contentResolver = activity.contentResolver,
                            uri = uri,
                        ),
                    ),
                )
            }
            val continuation = pickerContinuation
            if (continuation?.isActive == true) {
                continuation.resume(result)
            } else {
                (result as? ImageAcquisitionResult.Success)?.image?.release()
            }
            pickerContinuation = null
        }

    fun attachCameraSession(session: AndroidCameraCaptureSession) {
        cameraSession = session
    }

    fun detachCameraSession(session: AndroidCameraCaptureSession) {
        if (cameraSession === session) {
            cameraSession = null
        }
    }

    override suspend fun checkCameraPermission(): CameraPermissionState {
        mutablePermissionState.value = CameraPermissionState.Checking
        val state = terminalPermissionState()
        mutablePermissionState.value = state
        return state
    }

    override suspend fun requestCameraPermission(): CameraPermissionState {
        val checked = terminalPermissionState()
        if (checked == CameraPermissionState.Granted ||
            checked == CameraPermissionState.Unavailable ||
            checked == CameraPermissionState.DeniedOpenSettings
        ) {
            mutablePermissionState.value = checked
            return checked
        }

        check(permissionContinuation == null) { "A camera permission request is already active" }
        mutablePermissionState.value = CameraPermissionState.Requesting
        return suspendCancellableCoroutine { continuation ->
            permissionContinuation = continuation
            continuation.invokeOnCancellation {
                if (permissionContinuation === continuation) {
                    permissionContinuation = null
                    mutablePermissionState.value = terminalPermissionState()
                }
            }
            permissionHistory.edit().putBoolean(CAMERA_PERMISSION_REQUESTED, true).apply()
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override suspend fun captureCameraImage(): ImageAcquisitionResult {
        return when (terminalPermissionState()) {
            CameraPermissionState.Granted ->
                cameraSession?.capture()
                    ?: ImageAcquisitionResult.Failure(ImageFailureCode.CAMERA_NOT_READY)
            CameraPermissionState.Unavailable ->
                ImageAcquisitionResult.Failure(ImageFailureCode.CAMERA_UNAVAILABLE)
            else ->
                ImageAcquisitionResult.Failure(ImageFailureCode.PERMISSION_DENIED)
        }
    }

    override suspend fun pickSingleImage(): ImageAcquisitionResult {
        check(pickerContinuation == null) { "A system photo picker request is already active" }
        return suspendCancellableCoroutine { continuation ->
            pickerContinuation = continuation
            continuation.invokeOnCancellation {
                if (pickerContinuation === continuation) {
                    pickerContinuation = null
                }
            }
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    override fun openAppSettings(): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        return runCatching {
            mutablePermissionState.value = CameraPermissionState.SettingsPending
            activity.startActivity(intent)
            true
        }.getOrElse {
            mutablePermissionState.value = terminalPermissionState()
            false
        }
    }

    override suspend fun onForegroundResume(): CameraPermissionState = checkCameraPermission()

    private fun terminalPermissionState(): CameraPermissionState {
        if (!hasCamera()) return CameraPermissionState.Unavailable
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return CameraPermissionState.Granted
        }
        return deniedState()
    }

    private fun hasCamera(): Boolean =
        activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    private fun deniedState(): CameraPermissionState {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA,
            )
        ) {
            return CameraPermissionState.DeniedCanAsk
        }

        val hasUserDecision = permissionHistory.getBoolean(
            CAMERA_PERMISSION_REQUESTED,
            false,
        )
        return if (hasUserDecision) {
            CameraPermissionState.DeniedOpenSettings
        } else {
            CameraPermissionState.DeniedCanAsk
        }
    }

    private companion object {
        const val CAMERA_PERMISSION_REQUESTED = "camera_permission_requested_v1"
    }
}
