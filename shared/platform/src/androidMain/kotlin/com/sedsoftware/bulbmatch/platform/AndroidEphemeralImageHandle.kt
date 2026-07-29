package com.sedsoftware.bulbmatch.platform

import android.content.ContentResolver
import android.net.Uri

internal sealed interface AndroidImagePayload {
    data class PickerUri(
        val contentResolver: ContentResolver,
        val uri: Uri,
    ) : AndroidImagePayload

    data class EncodedCameraImage(
        var bytes: ByteArray,
        val rotationDegrees: Int,
    ) : AndroidImagePayload
}

internal class AndroidEphemeralImageHandle(
    override val source: ImageSource,
    private var retainedPayload: AndroidImagePayload?,
) : EphemeralImageHandle {
    override val isReleased: Boolean
        get() = retainedPayload == null

    internal fun payloadOrNull(): AndroidImagePayload? = retainedPayload

    override fun release() {
        (retainedPayload as? AndroidImagePayload.EncodedCameraImage)?.bytes?.fill(0)
        retainedPayload = null
    }
}
