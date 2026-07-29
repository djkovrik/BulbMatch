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
import com.sedsoftware.bulbmatch.data.catalog.CatalogValidationMode
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseFactory
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseHandle
import com.sedsoftware.bulbmatch.data.db.IosDatabaseDriverFactory
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshot
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshotCodec
import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import kotlinx.coroutines.Dispatchers
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

fun MainViewController(): UIViewController {
    val holder = IosRootHolder.create()
    return ComposeUIViewController {
        IosBulbMatchApp(holder)
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
 * The checked-in host remains safe and usable for manual/history/settings on iOS. Camera, picker,
 * bundled OCR, ads, and Crashlytics are activated by the Swift host bridges after CocoaPods are
 * installed on macOS; until then the camera path fails explicitly instead of using demo data.
 */
private class IosRootHolder private constructor(
    val root: RootComponent,
    val platform: IosUnavailablePlatformBridge,
    private val lifecycle: LifecycleRegistry,
    private val database: BulbMatchDatabaseHandle,
) {
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        lifecycle.destroy()
        database.close()
    }

    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun create(): IosRootHolder {
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
                voltageRules = emptyList(),
                targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
                targetFrequency = requireNotNull(FrequencyMarking.from(50.0)),
            )
            val platform = IosUnavailablePlatformBridge()
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
        name = "bulbmatch-catalog-development",
        ofType = "json",
        inDirectory = "catalog",
    ) ?: return ByteArray(0)
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return ByteArray(0)
    val length = data.length.toInt()
    if (length == 0) return ByteArray(0)
    val source = data.bytes?.reinterpret<ByteVar>() ?: return ByteArray(0)
    return ByteArray(length) { index -> source[index] }
}

private class IosUnavailablePlatformBridge :
    ImageActions,
    RecognitionGateway,
    InterstitialGateway {
    lateinit var root: RootComponent
    var interstitialPresenter: (((Boolean) -> Unit) -> Unit)? = null

    override fun requestCameraPermission() {
        currentCamera()?.onCameraStatusChanged(CameraStatus.Unavailable)
    }

    override fun openSystemCameraSettings() = Unit

    override fun openPhotoPicker() {
        root.match.onImageSelectionFailed(
            com.sedsoftware.bulbmatch.app.ImageFailure.CameraUnavailable,
        )
    }

    override fun capturePhoto() {
        root.match.onImageSelectionFailed(
            com.sedsoftware.bulbmatch.app.ImageFailure.CameraUnavailable,
        )
    }

    override fun setTorch(enabled: Boolean) = Unit

    override suspend fun recognize(image: EphemeralImage): RecognitionResult =
        RecognitionResult.Failure(RecognitionFailure.RecognitionFailed)

    override fun showMatchExit(onComplete: (impressionRecorded: Boolean) -> Unit) {
        val presenter = interstitialPresenter
        if (presenter == null) onComplete(false) else presenter(onComplete)
    }

    private fun currentCamera(): CameraComponent? =
        (root.match.stack.value.active.instance as? MatchComponent.Child.Camera)?.component
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent,
        )
    }
}
