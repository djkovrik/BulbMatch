package com.sedsoftware.bulbmatch

import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sedsoftware.bulbmatch.app.CameraStatus
import com.sedsoftware.bulbmatch.platform.AndroidCameraCaptureSession
import com.sedsoftware.bulbmatch.platform.AndroidCameraSessionOpenResult

@Composable
internal fun AndroidCameraPreview(
    bridge: AndroidPlatformBridge,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = context as ComponentActivity
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var session by remember { mutableStateOf<AndroidCameraCaptureSession?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize(),
    )

    LaunchedEffect(previewView, lifecycleOwner, bridge) {
        when (
            val result = AndroidCameraCaptureSession.open(
                context = context,
                lifecycleOwner = lifecycleOwner,
                surfaceProvider = previewView.surfaceProvider,
            )
        ) {
            is AndroidCameraSessionOpenResult.Success -> {
                session = result.session
                bridge.attachCameraSession(result.session)
            }
            is AndroidCameraSessionOpenResult.Failure -> {
                bridge.root.match.stack.value.active.instance.let { active ->
                    (active as? com.sedsoftware.bulbmatch.app.MatchComponent.Child.Camera)
                        ?.component
                        ?.onCameraStatusChanged(CameraStatus.Unavailable)
                }
            }
        }
    }

    DisposableEffect(bridge, session) {
        val current = session
        onDispose {
            if (current != null) bridge.detachCameraSession(current)
        }
    }
}
