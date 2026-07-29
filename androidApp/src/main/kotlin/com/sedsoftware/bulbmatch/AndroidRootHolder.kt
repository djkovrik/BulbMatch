package com.sedsoftware.bulbmatch

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.russhwolf.settings.SharedPreferencesSettings
import com.sedsoftware.bulbmatch.app.CameraComponent
import com.sedsoftware.bulbmatch.app.CameraStatus
import com.sedsoftware.bulbmatch.app.DefaultRootComponent
import com.sedsoftware.bulbmatch.app.EphemeralImage
import com.sedsoftware.bulbmatch.app.ImageActions
import com.sedsoftware.bulbmatch.app.ImageFailure
import com.sedsoftware.bulbmatch.app.InterstitialGateway
import com.sedsoftware.bulbmatch.app.MatchComponent
import com.sedsoftware.bulbmatch.app.RecognitionFailure
import com.sedsoftware.bulbmatch.app.RecognitionGateway
import com.sedsoftware.bulbmatch.app.RecognitionResult
import com.sedsoftware.bulbmatch.app.RootComponent
import com.sedsoftware.bulbmatch.data.DefaultCatalogProvider
import com.sedsoftware.bulbmatch.data.DefaultSavedMatchRepository
import com.sedsoftware.bulbmatch.data.DefaultSettingsRepository
import com.sedsoftware.bulbmatch.data.catalog.BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH
import com.sedsoftware.bulbmatch.data.catalog.CatalogValidationMode
import com.sedsoftware.bulbmatch.data.db.AndroidDatabaseDriverFactory
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseFactory
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabaseHandle
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshot
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshotCodec
import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.domain.BaseAliasIndex
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.MarkingParser
import com.sedsoftware.bulbmatch.domain.ObservationGeometry
import com.sedsoftware.bulbmatch.domain.RawTextObservation
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import com.sedsoftware.bulbmatch.platform.AndroidCameraCaptureSession
import com.sedsoftware.bulbmatch.platform.AndroidImageSourceService
import com.sedsoftware.bulbmatch.platform.AndroidTextRecognitionService
import com.sedsoftware.bulbmatch.platform.AndroidCrashReporter
import com.sedsoftware.bulbmatch.platform.CameraPermissionState
import com.sedsoftware.bulbmatch.platform.CrashContext
import com.sedsoftware.bulbmatch.platform.CrashReporter
import com.sedsoftware.bulbmatch.platform.EphemeralImageHandle
import com.sedsoftware.bulbmatch.platform.ImageAcquisitionResult
import com.sedsoftware.bulbmatch.platform.ImageFailureCode
import com.sedsoftware.bulbmatch.platform.TextRecognitionFailureCode
import com.sedsoftware.bulbmatch.platform.TextRecognitionResult
import com.sedsoftware.bulbmatch.platform.OperationCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The retained graph contains no Activity reference. Native launchers and OCR are attached to the
 * current Activity instance and replaced after configuration changes.
 */
internal class AndroidRootHolder private constructor(
    val root: RootComponent,
    val bridge: AndroidPlatformBridge,
    private val databaseHandle: BulbMatchDatabaseHandle,
) {
    companion object {
        fun create(
            componentContext: ComponentContext,
            activity: ComponentActivity,
        ): AndroidRootHolder {
            val applicationContext = activity.applicationContext
            val databaseHandle = BulbMatchDatabaseFactory.create(
                AndroidDatabaseDriverFactory(applicationContext),
            )
            val savedStore = SqlDelightSavedMatchStore<SavedAssessmentSnapshot>(
                database = databaseHandle.database,
                snapshotCodec = SavedAssessmentSnapshotCodec,
                ioDispatcher = Dispatchers.IO,
            )
            val savedRepository = DefaultSavedMatchRepository(savedStore)
            val settingsRepository = DefaultSettingsRepository(
                BulbMatchSettingsStore(
                    SharedPreferencesSettings(
                        applicationContext.getSharedPreferences(
                            "bulbmatch_settings",
                            android.content.Context.MODE_PRIVATE,
                        ),
                    ),
                ),
            )
            val catalogBytes = DefaultCatalogProvider::class.java.classLoader
                ?.getResourceAsStream(BUNDLED_DEVELOPMENT_CATALOG_RESOURCE_PATH)
                ?.use { it.readBytes() }
                ?: ByteArray(0)
            val catalogProvider = DefaultCatalogProvider(
                utf8Catalog = catalogBytes,
                mode = CatalogValidationMode.Production,
                voltageRules = emptyList(),
                targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
                targetFrequency = requireNotNull(FrequencyMarking.from(50.0)),
            )
            val bridge = AndroidPlatformBridge(
                parser = MarkingParser(),
                aliases = BaseAliasIndex.from(catalogProvider.searchEntries("")),
                crashReporter = AndroidCrashReporter.create(
                    context = applicationContext,
                    collectionEnabled =
                        applicationContext.applicationInfo.flags and
                            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0,
                ),
            )
            val root = DefaultRootComponent(
                componentContext = componentContext,
                storeFactory = DefaultStoreFactory(),
                compatibilityEngine = CompatibilityEngine(),
                catalogProvider = catalogProvider,
                settingsRepository = settingsRepository,
                savedMatchRepository = savedRepository,
                recognitionGateway = bridge,
                imageActions = bridge,
                interstitialGateway = bridge,
                nowEpochMs = System::currentTimeMillis,
                newSavedMatchId = { UUID.randomUUID().toString() },
            )
            bridge.root = root
            componentContext.lifecycle.doOnDestroy {
                bridge.close()
                databaseHandle.close()
            }
            return AndroidRootHolder(root, bridge, databaseHandle).also {
                bridge.attach(activity)
            }
        }
    }
}

internal class AndroidPlatformBridge(
    private val parser: MarkingParser,
    private val aliases: BaseAliasIndex,
    private val crashReporter: CrashReporter,
) : ImageActions, RecognitionGateway, InterstitialGateway {
    lateinit var root: RootComponent

    private var activity: ComponentActivity? = null
    private var imageSource: AndroidImageSourceService? = null
    private var recognizer: AndroidTextRecognitionService? = null
    private var cameraSession: AndroidCameraCaptureSession? = null

    var interstitialPresenter: (((Boolean) -> Unit) -> Unit)? = null

    fun attach(activity: ComponentActivity) {
        if (this.activity === activity) return
        recognizer?.close()
        this.activity = activity
        imageSource = AndroidImageSourceService(activity)
        recognizer = AndroidTextRecognitionService()
    }

    fun detach(activity: ComponentActivity) {
        if (this.activity !== activity) return
        cameraSession?.let { session ->
            imageSource?.detachCameraSession(session)
            session.close()
        }
        cameraSession = null
        recognizer?.close()
        recognizer = null
        imageSource = null
        this.activity = null
    }

    fun attachCameraSession(session: AndroidCameraCaptureSession) {
        cameraSession?.takeIf { it !== session }?.close()
        cameraSession = session
        imageSource?.attachCameraSession(session)
        currentCamera()?.onCameraStatusChanged(CameraStatus.Granted)
        currentCamera()?.onCameraCapabilitiesChanged(session.hasTorch)
    }

    fun detachCameraSession(session: AndroidCameraCaptureSession) {
        imageSource?.detachCameraSession(session)
        if (cameraSession === session) cameraSession = null
        session.close()
    }

    fun onForegroundResume() {
        launch(OperationCode.CAMERA_PERMISSION_CHECK) {
            currentCamera()?.onCameraStatusChanged(
                imageSource?.onForegroundResume().toAppStatus(),
            )
        }
    }

    override fun requestCameraPermission() {
        launch(OperationCode.CAMERA_PERMISSION_REQUEST) {
            val source = imageSource
            val status = if (source == null) {
                CameraStatus.Unavailable
            } else {
                source.requestCameraPermission().toAppStatus()
            }
            currentCamera()?.onCameraStatusChanged(status)
        }
    }

    override fun openSystemCameraSettings() {
        if (imageSource?.openAppSettings() != true) {
            currentCamera()?.onCameraStatusChanged(CameraStatus.DeniedOpenSettings)
        }
    }

    override fun openPhotoPicker() {
        launch(OperationCode.IMAGE_PICK) {
            when (val result = imageSource?.pickSingleImage()) {
                is ImageAcquisitionResult.Success ->
                    root.match.onPickedImageAvailable(AndroidEphemeralImage(result.image))
                is ImageAcquisitionResult.Cancelled, null ->
                    root.match.onImageSelectionCancelled()
                is ImageAcquisitionResult.Failure ->
                    root.match.onImageSelectionFailed(result.code.toAppFailure())
            }
        }
    }

    override fun capturePhoto() {
        launch(OperationCode.CAMERA_CAPTURE) {
            when (val result = imageSource?.captureCameraImage()) {
                is ImageAcquisitionResult.Success ->
                    root.match.onCameraImageAvailable(AndroidEphemeralImage(result.image))
                is ImageAcquisitionResult.Cancelled, null ->
                    root.match.onImageSelectionCancelled()
                is ImageAcquisitionResult.Failure ->
                    root.match.onImageSelectionFailed(result.code.toAppFailure())
            }
        }
    }

    override fun setTorch(enabled: Boolean) {
        launch(OperationCode.CAMERA_BIND) {
            val session = cameraSession
            if (session == null || !session.setTorch(enabled)) {
                currentCamera()?.onTorchChanged(false)
            }
        }
    }

    override suspend fun recognize(image: EphemeralImage): RecognitionResult {
        val handle = (image as? AndroidEphemeralImage)?.handle
            ?: return RecognitionResult.Failure(RecognitionFailure.UnsupportedImage)
        return when (val result = recognizer?.recognize(handle)) {
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
            null -> RecognitionResult.Failure(RecognitionFailure.RecognitionFailed)
        }
    }

    override fun showMatchExit(onComplete: (impressionRecorded: Boolean) -> Unit) {
        val presenter = interstitialPresenter
        if (presenter == null) onComplete(false) else presenter(onComplete)
    }

    fun close() {
        activity?.let(::detach)
        interstitialPresenter = null
    }

    private fun currentCamera(): CameraComponent? =
        (root.match.stack.value.active.instance as? MatchComponent.Child.Camera)?.component

    private fun launch(
        operation: OperationCode,
        block: suspend () -> Unit,
    ) {
        activity?.lifecycleScope?.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                crashReporter.recordNonFatal(
                    failure,
                    CrashContext(screen = null, operation = operation),
                )
                root.match.onImageSelectionFailed(ImageFailure.Unknown)
            }
        }
    }
}

internal class AndroidEphemeralImage(
    val handle: EphemeralImageHandle,
) : EphemeralImage {
    override val debugLabel: String = when (handle.source) {
        com.sedsoftware.bulbmatch.platform.ImageSource.CAMERA -> "camera-image"
        com.sedsoftware.bulbmatch.platform.ImageSource.SYSTEM_PICKER -> "picked-image"
    }

    override fun release() = handle.release()
}

private fun CameraPermissionState?.toAppStatus(): CameraStatus = when (this) {
    CameraPermissionState.Unknown -> CameraStatus.Unknown
    CameraPermissionState.Checking, CameraPermissionState.Requesting ->
        CameraStatus.Checking
    CameraPermissionState.Granted -> CameraStatus.Granted
    CameraPermissionState.DeniedCanAsk -> CameraStatus.DeniedCanAsk
    CameraPermissionState.DeniedOpenSettings, CameraPermissionState.SettingsPending ->
        CameraStatus.DeniedOpenSettings
    CameraPermissionState.Unavailable, null -> CameraStatus.Unavailable
}

private fun ImageFailureCode.toAppFailure(): ImageFailure = when (this) {
    ImageFailureCode.PERMISSION_DENIED -> ImageFailure.PermissionDenied
    ImageFailureCode.CAMERA_UNAVAILABLE,
    ImageFailureCode.CAMERA_NOT_READY,
    ImageFailureCode.CAPTURE_FAILED,
    -> ImageFailure.CameraUnavailable
    ImageFailureCode.UNREADABLE_IMAGE -> ImageFailure.UnreadableImage
}

private fun TextRecognitionFailureCode.toAppFailure(): RecognitionFailure = when (this) {
    TextRecognitionFailureCode.NO_TEXT_FOUND -> RecognitionFailure.NoTextFound
    TextRecognitionFailureCode.UNSUPPORTED_IMAGE -> RecognitionFailure.UnsupportedImage
    TextRecognitionFailureCode.RECOGNITION_FAILED -> RecognitionFailure.RecognitionFailed
}
