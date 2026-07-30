package com.sedsoftware.bulbmatch.screenshottests

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import app.cash.paparazzi.TestName
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.Density
import com.android.resources.NightMode
import com.android.resources.ScreenRatio
import com.android.resources.ScreenRound
import com.android.resources.ScreenSize
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.device.DevicePreviewInfoParser
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import java.security.MessageDigest
import kotlin.math.ceil

internal object BulbMatchPaparazziPreviewRule {
    fun createFor(
        preview: ComposablePreview<AndroidPreviewInfo>,
        compileSdkVersion: Int,
    ): Paparazzi {
        val previewInfo = preview.previewInfo
        val tolerance = 0.0

        return Paparazzi(
            environment = detectEnvironment().copy(compileSdkVersion = compileSdkVersion),
            deviceConfig = BulbMatchDeviceConfigBuilder.build(previewInfo),
            renderingMode = when (previewInfo.widthDp > 0 && previewInfo.heightDp > 0) {
                true -> SessionParams.RenderingMode.FULL_EXPAND
                false -> SessionParams.RenderingMode.SHRINK
            },
            supportsRtl = true,
            showSystemUi = previewInfo.showSystemUi,
            maxPercentDifference = tolerance,
            snapshotHandler = bulbMatchSnapshotHandler(tolerance),
        )
    }
}

internal object BulbMatchSnapshotId {
    fun create(preview: ComposablePreview<AndroidPreviewInfo>): String {
        val encodedId = AndroidPreviewScreenshotIdBuilder(preview)
            .doNotIgnoreMethodParametersType()
            .encodeUnsafeCharacters()
            .build()
            .removePrefix("$PREVIEW_PACKAGE.")

        if (encodedId.length <= MAX_ID_LENGTH) {
            return encodedId
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(encodedId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
            .take(HASH_LENGTH)
        return "${encodedId.take(MAX_ID_LENGTH - HASH_LENGTH - 1)}_$digest"
    }

    private const val PREVIEW_PACKAGE = "com.sedsoftware.bulbmatch.compose"
    private const val MAX_ID_LENGTH = 120
    private const val HASH_LENGTH = 16
}

@Composable
internal fun BulbMatchPreviewContent(
    previewInfo: AndroidPreviewInfo,
    content: @Composable () -> Unit,
) {
    ResizeComposable(
        widthInDp = previewInfo.widthDp,
        heightInDp = previewInfo.heightDp,
    ) {
        PreviewBackground(
            showBackground = previewInfo.showBackground,
            backgroundColor = previewInfo.backgroundColor,
            content = content,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
internal fun Paparazzi.configureComposeResources() {
    setResourceReaderAndroidContext(context)
}

@Composable
private fun PreviewBackground(
    showBackground: Boolean,
    backgroundColor: Long,
    content: @Composable () -> Unit,
) {
    if (!showBackground) {
        content()
        return
    }

    val color = when (backgroundColor == 0L) {
        true -> Color.White
        false -> Color(backgroundColor)
    }

    Box(Modifier.background(color)) {
        content()
    }
}

@Composable
private fun ResizeComposable(
    widthInDp: Int,
    heightInDp: Int,
    content: @Composable () -> Unit,
) {
    val modifier = when {
        widthInDp > 0 && heightInDp > 0 -> Modifier.size(width = widthInDp.dp, height = heightInDp.dp)
        widthInDp > 0 -> Modifier.width(widthInDp.dp)
        heightInDp > 0 -> Modifier.height(heightInDp.dp)
        else -> Modifier
    }

    Box(modifier = modifier) {
        content()
    }
}

private object BulbMatchDeviceConfigBuilder {
    fun build(previewInfo: AndroidPreviewInfo): DeviceConfig =
        baseDevice(previewInfo.device)
            .resizeToPreview(previewInfo.widthDp, previewInfo.heightDp)
            .copy(
                locale = previewInfo.locale.ifBlank { "en" },
                fontScale = previewInfo.fontScale,
                nightMode = when (previewInfo.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES) {
                    true -> NightMode.NIGHT
                    false -> NightMode.NOTNIGHT
                },
            )

    private fun baseDevice(previewDevice: String): DeviceConfig {
        val device = DevicePreviewInfoParser.parse(previewDevice) ?: return DeviceConfig.PIXEL_5

        return DeviceConfig(
            screenHeight = device.dimensions.height.toInt(),
            screenWidth = device.dimensions.width.toInt(),
            xdpi = device.densityDpi,
            ydpi = device.densityDpi,
            ratio = ScreenRatio.valueOf(device.screenRatio.name),
            size = ScreenSize.valueOf(device.screenSize.name),
            density = Density(device.densityDpi),
            screenRound = ScreenRound.valueOf(device.shape.name),
        )
    }

    private fun DeviceConfig.resizeToPreview(
        widthDp: Int,
        heightDp: Int,
    ): DeviceConfig {
        val conversionFactor = density.dpiValue / DEFAULT_DENSITY_DPI
        val previewWidthPx = ceil(widthDp * conversionFactor).toInt()
        val previewHeightPx = ceil(heightDp * conversionFactor).toInt()

        return copy(
            screenWidth = when (widthDp > 0) {
                true -> previewWidthPx
                false -> screenWidth
            },
            screenHeight = when (heightDp > 0) {
                true -> previewHeightPx
                false -> screenHeight
            },
        )
    }

    private const val DEFAULT_DENSITY_DPI = 160f
}

private fun bulbMatchSnapshotHandler(tolerance: Double): SnapshotHandler =
    when (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) {
        true -> PreviewSnapshotVerifier(tolerance)
        false -> PreviewHtmlReportWriter()
    }

private val paparazziPreviewTestName = TestName(
    packageName = "Paparazzi",
    className = "Preview",
    methodName = "Test",
)

private class PreviewSnapshotVerifier(
    maxPercentDifference: Double,
) : SnapshotHandler {
    private val snapshotHandler = SnapshotVerifier(maxPercentDifference)

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int,
    ): SnapshotHandler.FrameHandler =
        snapshotHandler.newFrameHandler(
            snapshot = snapshot.copy(testName = paparazziPreviewTestName),
            frameCount = frameCount,
            fps = fps,
        )

    override fun close() {
        snapshotHandler.close()
    }
}

private class PreviewHtmlReportWriter : SnapshotHandler {
    private val snapshotHandler = HtmlReportWriter()

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int,
    ): SnapshotHandler.FrameHandler =
        snapshotHandler.newFrameHandler(
            snapshot = snapshot.copy(testName = paparazziPreviewTestName),
            frameCount = frameCount,
            fps = fps,
        )

    override fun close() {
        snapshotHandler.close()
    }
}
