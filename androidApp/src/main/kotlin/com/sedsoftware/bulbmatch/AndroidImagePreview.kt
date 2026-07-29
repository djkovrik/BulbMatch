package com.sedsoftware.bulbmatch

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sedsoftware.bulbmatch.app.EphemeralImage
import com.sedsoftware.bulbmatch.platform.decodeAndroidPreviewBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AndroidImagePreview(
    image: EphemeralImage,
    modifier: Modifier = Modifier,
) {
    val handle = (image as? AndroidEphemeralImage)?.handle
    var bitmap by remember(handle) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(handle) {
        bitmap = withContext(Dispatchers.Default) {
            handle?.decodeAndroidPreviewBitmap()
        }
    }
    DisposableEffect(bitmap) {
        val current = bitmap
        onDispose {
            if (current != null && !current.isRecycled) current.recycle()
        }
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
