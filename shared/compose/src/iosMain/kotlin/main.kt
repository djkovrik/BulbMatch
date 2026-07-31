import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.russhwolf.settings.Settings
import com.sedsoftware.bulbmatch.app.CameraComponent
import com.sedsoftware.bulbmatch.app.CameraStatus
import com.sedsoftware.bulbmatch.app.DefaultRootComponent
import com.sedsoftware.bulbmatch.app.EphemeralImage
import com.sedsoftware.bulbmatch.app.ImageActions
import com.sedsoftware.bulbmatch.app.InterstitialGateway
import com.sedsoftware.bulbmatch.app.MatchComponent
import com.sedsoftware.bulbmatch.app.RecognitionFailure
import com.sedsoftware.bulbmatch.app.RecognitionGateway
import com.sedsoftware.bulbmatch.app.RecognitionResult
import com.sedsoftware.bulbmatch.app.RootComponent
import com.sedsoftware.bulbmatch.ads.AdBuildMode
import com.sedsoftware.bulbmatch.ads.AdOutcome
import com.sedsoftware.bulbmatch.ads.AdPlacement
import com.sedsoftware.bulbmatch.ads.AdPlatform
import com.sedsoftware.bulbmatch.ads.BulbMatchAdConfiguration
import com.sedsoftware.bulbmatch.ads.BulbMatchAdsInitializer
import com.sedsoftware.bulbmatch.ads.BulbMatchBanner
import com.sedsoftware.bulbmatch.ads.rememberBulbMatchInterstitialController
import com.sedsoftware.bulbmatch.compose.App
import com.sedsoftware.bulbmatch.compose.BulbMatchSlots
import com.sedsoftware.bulbmatch.data.DefaultCatalogProvider
import com.sedsoftware.bulbmatch.data.DefaultSavedMatchRepository
import com.sedsoftware.bulbmatch.data.DefaultSettingsRepository
import com.sedsoftware.bulbmatch.data.catalog.BundledCatalogRules
import com.sedsoftware.bulbmatch.data.catalog.CatalogValidationMode
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseFactory
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseHandle
import com.sedsoftware.bulbmatch.data.db.IosDatabaseDriverFactory
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshot
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshotCodec
import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.domain.BaseAliasIndex
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.MarkingParser
import com.sedsoftware.bulbmatch.domain.ObservationGeometry
import com.sedsoftware.bulbmatch.domain.RawTextObservation
import com.sedsoftware.bulbmatch.platform.CameraPermissionState
import com.sedsoftware.bulbmatch.platform.CrashContext
import com.sedsoftware.bulbmatch.platform.CrashReporter
import com.sedsoftware.bulbmatch.platform.EphemeralImageHandle
import com.sedsoftware.bulbmatch.platform.ImageAcquisitionResult
import com.sedsoftware.bulbmatch.platform.ImageFailureCode
import com.sedsoftware.bulbmatch.platform.IosCrashReporter
import com.sedsoftware.bulbmatch.platform.IosCrashReportingHost
import com.sedsoftware.bulbmatch.platform.IosHostImageResult
import com.sedsoftware.bulbmatch.platform.IosHostRecognitionResult
import com.sedsoftware.bulbmatch.platform.IosImageSourceHost
import com.sedsoftware.bulbmatch.platform.IosImageSourceService
import com.sedsoftware.bulbmatch.platform.IosTextRecognitionHost
import com.sedsoftware.bulbmatch.platform.IosTextRecognitionService
import com.sedsoftware.bulbmatch.platform.NormalizedRect
import com.sedsoftware.bulbmatch.platform.OperationCode
import com.sedsoftware.bulbmatch.platform.TextObservation
import com.sedsoftware.bulbmatch.platform.TextRecognitionFailureCode
import com.sedsoftware.bulbmatch.platform.TextRecognitionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.get
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlin.time.Clock
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle

/**
 * Swift-facing constructors for Kotlin sealed results and singleton permission states.
 * Keeping construction here avoids exposing implementation subclasses as host contracts.
 */
class IosBridgeFactory {
    fun permissionUnknown(): CameraPermissionState = CameraPermissionState.Unknown
    fun permissionGranted(): CameraPermissionState = CameraPermissionState.Granted
    fun permissionDeniedCanAsk(): CameraPermissionState = CameraPermissionState.DeniedCanAsk
    fun permissionDeniedOpenSettings(): CameraPermissionState =
        CameraPermissionState.DeniedOpenSettings
    fun permissionUnavailable(): CameraPermissionState = CameraPermissionState.Unavailable

    fun imageSuccess(encodedImage: ByteArray): IosHostImageResult =
        IosHostImageResult.Success(encodedImage)

    fun imageCancelled(): IosHostImageResult = IosHostImageResult.Cancelled

    fun imageFailure(code: ImageFailureCode): IosHostImageResult =
        IosHostImageResult.Failure(code)

    fun textObservation(
        text: String,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): TextObservation = TextObservation(
        text = text,
        bounds = NormalizedRect(left, top, right, bottom),
    )

    fun recognitionSuccess(observations: List<TextObservation>): IosHostRecognitionResult =
        IosHostRecognitionResult.Success(observations)

    fun recognitionFailure(code: TextRecognitionFailureCode): IosHostRecognitionResult =
        IosHostRecognitionResult.Failure(code)
}

/**
 * Retained iOS composition boundary. Swift owns one instance for the lifetime of
 * the root controller and forwards foreground activation after returning from Settings.
 */
class IosAppController(
    imageSourceHost: IosImageSourceHost,
    textRecognitionHost: IosTextRecognitionHost,
    crashReportingHost: IosCrashReportingHost,
) {
    private val holder = IosRootHolder.create(
        imageSourceHost = imageSourceHost,
        textRecognitionHost = textRecognitionHost,
        crashReportingHost = crashReportingHost,
    )
    val viewController: UIViewController = ComposeUIViewController {
        IosBulbMatchApp(holder)
    }

    fun onForegroundResume() {
        holder.platform.onForegroundResume()
    }

    fun close() {
        holder.close()
    }
}

@Composable
@OptIn(ExperimentalNativeApi::class)
private fun IosBulbMatchApp(holder: IosRootHolder) {
    val configuration = remember {
        BulbMatchAdConfiguration.forBuild(
            platform = AdPlatform.Ios,
            mode = if (Platform.isDebugBinary) {
                AdBuildMode.DebugDevice
            } else {
                AdBuildMode.Release
            },
        )
    }
    val interstitial = rememberBulbMatchInterstitialController(configuration)
    val scope = rememberCoroutineScope()
    DisposableEffect(holder) {
        onDispose(holder::close)
    }
    BulbMatchAdsInitializer(configuration)
    LaunchedEffect(interstitial) {
        interstitial.preload()
    }
    DisposableEffect(holder.platform, interstitial) {
        holder.platform.interstitialPresenter = { completion ->
            interstitial.showOrContinue(
                onImpression = {},
                onComplete = { outcome ->
                    completion(outcome == AdOutcome.Impression)
                    scope.launch { interstitial.preload() }
                },
            )
        }
        onDispose {
            holder.platform.interstitialPresenter = null
            interstitial.cancelLoading()
        }
    }
    App(
        root = holder.root,
        onThemeChanged = { ThemeChanged(it) },
        slots = BulbMatchSlots(
            resultBanner = {
                BulbMatchBanner(configuration, AdPlacement.ResultInline)
            },
            historyBanner = {
                BulbMatchBanner(configuration, AdPlacement.HistorySticky)
            },
            referenceBanner = {
                BulbMatchBanner(configuration, AdPlacement.ReferenceSticky)
            },
        ),
    )
}

/**
 * Kotlin owns application state while the retained Swift composition owns native presenters and
 * SDK objects. No image or recognized text crosses into persistence or crash-report metadata.
 */
private class IosRootHolder private constructor(
    val root: RootComponent,
    val platform: IosPlatformBridge,
    private val lifecycle: LifecycleRegistry,
    private val database: BulbMatchDatabaseHandle,
) {
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        lifecycle.destroy()
        platform.close()
        database.close()
    }

    companion object {
        @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
        fun create(
            imageSourceHost: IosImageSourceHost,
            textRecognitionHost: IosTextRecognitionHost,
            crashReportingHost: IosCrashReportingHost,
        ): IosRootHolder {
            val lifecycle = LifecycleRegistry()
            lifecycle.create()
            val database = BulbMatchDatabaseFactory.create(IosDatabaseDriverFactory())
            val savedRepository = DefaultSavedMatchRepository(
                SqlDelightSavedMatchStore<SavedAssessmentSnapshot>(
                    database = database.database,
                    snapshotCodec = SavedAssessmentSnapshotCodec,
                    ioDispatcher = Dispatchers.Default,
                ),
            )
            val settingsRepository = DefaultSettingsRepository(
                BulbMatchSettingsStore(Settings()),
            )
            val catalogProvider = DefaultCatalogProvider(
                utf8Catalog = loadIosCatalogBytes(),
                mode = CatalogValidationMode.Production,
                ruleset = BundledCatalogRules.ruleset,
            )
            val platform = IosPlatformBridge(
                imageSource = IosImageSourceService(imageSourceHost),
                recognitionService = IosTextRecognitionService(textRecognitionHost),
                parser = MarkingParser(),
                aliases = BaseAliasIndex.from(catalogProvider.searchEntries("")),
                crashReporter = IosCrashReporter(
                    host = crashReportingHost,
                    collectionEnabled = !Platform.isDebugBinary,
                ),
            )
            val root = DefaultRootComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
                storeFactory = DefaultStoreFactory(),
                compatibilityEngine = CompatibilityEngine(),
                catalogProvider = catalogProvider,
                settingsRepository = settingsRepository,
                savedMatchRepository = savedRepository,
                recognitionGateway = platform,
                imageActions = platform,
                interstitialGateway = platform,
                nowEpochMs = { Clock.System.now().toEpochMilliseconds() },
                newSavedMatchId = { NSUUID().UUIDString() },
            )
            platform.root = root
            lifecycle.start()
            lifecycle.resume()
            return IosRootHolder(root, platform, lifecycle, database)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadIosCatalogBytes(): ByteArray {
    val path = NSBundle.mainBundle.pathForResource(
        name = "bulbmatch-catalog-production",
        ofType = "json",
        inDirectory = "catalog",
    ) ?: return ByteArray(0)
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return ByteArray(0)
    val length = data.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = data.bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
    return ByteArray(length) { index -> source[index] }
}

private class IosPlatformBridge(
    private val imageSource: IosImageSourceService,
    private val recognitionService: IosTextRecognitionService,
    private val parser: MarkingParser,
    private val aliases: BaseAliasIndex,
    private val crashReporter: CrashReporter,
) :
    ImageActions,
    RecognitionGateway,
    InterstitialGateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var root: RootComponent
    var interstitialPresenter: (((Boolean) -> Unit) -> Unit)? = null

    override fun requestCameraPermission() {
        launch(OperationCode.CAMERA_PERMISSION_REQUEST) {
            val status = imageSource.requestCameraPermission().toAppStatus()
            currentCamera()?.onCameraStatusChanged(status)
            currentCamera()?.onCameraCapabilitiesChanged(false)
        }
    }

    override fun openSystemCameraSettings() {
        if (!imageSource.openAppSettings()) {
            currentCamera()?.onCameraStatusChanged(CameraStatus.DeniedOpenSettings)
        }
    }

    override fun openPhotoPicker() {
        launch(OperationCode.IMAGE_PICK) {
            when (val result = imageSource.pickSingleImage()) {
                is ImageAcquisitionResult.Success ->
                    root.match.onPickedImageAvailable(IosEphemeralImage(result.image))
                is ImageAcquisitionResult.Cancelled ->
                    root.match.onImageSelectionCancelled()
                is ImageAcquisitionResult.Failure ->
                    root.match.onImageSelectionFailed(result.code.toAppFailure())
            }
        }
    }

    override fun capturePhoto() {
        launch(OperationCode.CAMERA_CAPTURE) {
            when (val result = imageSource.captureCameraImage()) {
                is ImageAcquisitionResult.Success ->
                    root.match.onCameraImageAvailable(IosEphemeralImage(result.image))
                is ImageAcquisitionResult.Cancelled ->
                    root.match.onImageSelectionCancelled()
                is ImageAcquisitionResult.Failure ->
                    root.match.onImageSelectionFailed(result.code.toAppFailure())
            }
        }
    }

    override fun setTorch(enabled: Boolean) = Unit

    override suspend fun recognize(image: EphemeralImage): RecognitionResult {
        val handle = (image as? IosEphemeralImage)?.handle
            ?: return RecognitionResult.Failure(RecognitionFailure.UnsupportedImage)
        return when (val result = recognitionService.recognize(handle)) {
            is TextRecognitionResult.Success -> {
                val observations = parser.parse(
                    lines = result.observations.map { observation ->
                        RawTextObservation(
                            text = observation.text,
                            geometry = observation.bounds?.let {
                                ObservationGeometry(it.left, it.top, it.right, it.bottom)
                            },
                        )
                    },
                    baseAliases = aliases,
                )
                if (observations.isEmpty()) {
                    RecognitionResult.Failure(RecognitionFailure.NoTextFound)
                } else {
                    RecognitionResult.Success(observations)
                }
            }
            is TextRecognitionResult.Failure ->
                RecognitionResult.Failure(result.code.toAppFailure())
        }
    }

    override fun showMatchExit(onComplete: (impressionRecorded: Boolean) -> Unit) {
        val presenter = interstitialPresenter
        if (presenter == null) onComplete(false) else presenter(onComplete)
    }

    private fun currentCamera(): CameraComponent? =
        (root.match.stack.value.active.instance as? MatchComponent.Child.Camera)?.component

    fun onForegroundResume() {
        launch(OperationCode.CAMERA_PERMISSION_CHECK) {
            currentCamera()?.onCameraStatusChanged(
                imageSource.onForegroundResume().toAppStatus(),
            )
        }
    }

    fun close() {
        interstitialPresenter = null
        recognitionService.close()
        scope.cancel()
    }

    private fun launch(
        operation: OperationCode,
        block: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                crashReporter.recordNonFatal(
                    failure,
                    CrashContext(screen = null, operation = operation),
                )
                root.match.onImageSelectionFailed(
                    com.sedsoftware.bulbmatch.app.ImageFailure.Unknown,
                )
            }
        }
    }
}

private class IosEphemeralImage(
    val handle: EphemeralImageHandle,
) : EphemeralImage {
    override val debugLabel: String = "ios-ephemeral-image"

    override fun release() = handle.release()
}

private fun CameraPermissionState.toAppStatus(): CameraStatus = when (this) {
    CameraPermissionState.Unknown,
    CameraPermissionState.Checking,
    CameraPermissionState.Requesting,
    -> CameraStatus.Checking
    CameraPermissionState.Granted -> CameraStatus.Granted
    CameraPermissionState.DeniedCanAsk -> CameraStatus.DeniedCanAsk
    CameraPermissionState.DeniedOpenSettings,
    CameraPermissionState.SettingsPending,
    -> CameraStatus.DeniedOpenSettings
    CameraPermissionState.Unavailable -> CameraStatus.Unavailable
}

private fun ImageFailureCode.toAppFailure():
    com.sedsoftware.bulbmatch.app.ImageFailure = when (this) {
    ImageFailureCode.PERMISSION_DENIED ->
        com.sedsoftware.bulbmatch.app.ImageFailure.PermissionDenied
    ImageFailureCode.CAMERA_UNAVAILABLE,
    ImageFailureCode.CAMERA_NOT_READY,
    ImageFailureCode.CAPTURE_FAILED,
    -> com.sedsoftware.bulbmatch.app.ImageFailure.CameraUnavailable
    ImageFailureCode.UNREADABLE_IMAGE ->
        com.sedsoftware.bulbmatch.app.ImageFailure.UnreadableImage
}

private fun TextRecognitionFailureCode.toAppFailure(): RecognitionFailure = when (this) {
    TextRecognitionFailureCode.NO_TEXT_FOUND -> RecognitionFailure.NoTextFound
    TextRecognitionFailureCode.UNSUPPORTED_IMAGE -> RecognitionFailure.UnsupportedImage
    TextRecognitionFailureCode.RECOGNITION_FAILED -> RecognitionFailure.RecognitionFailed
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent,
        )
    }
}
