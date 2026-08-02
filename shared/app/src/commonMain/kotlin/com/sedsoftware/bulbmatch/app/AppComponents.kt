package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FieldOrigin
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.ObservedField
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.SavedMatchSummary
import com.sedsoftware.bulbmatch.domain.ThemeOverride

enum class RootDestination {
    Match,
    History,
    Reference,
}

interface RootComponent {
    val selectedDestination: Value<RootDestination>
    val localeOverride: Value<LocaleOverride>
    val themeOverride: Value<ThemeOverride>
    val match: MatchComponent
    val history: HistoryComponent
    val reference: ReferenceComponent
    val settingsSlot: Value<ChildSlot<*, SettingsComponent>>

    fun selectDestination(destination: RootDestination)
    fun openSettings()
    fun closeSettings()
}

interface MatchComponent {
    val stack: Value<ChildStack<*, Child>>
    val saveSlot: Value<ChildSlot<*, SaveResultComponent>>

    fun onCameraImageAvailable(image: EphemeralImage)
    fun onPickedImageAvailable(image: EphemeralImage)
    fun onImageSelectionCancelled()
    fun onImageSelectionFailed(reason: ImageFailure)
    fun hasEphemeralDraft(): Boolean

    sealed interface Child {
        data class Home(val component: MatchHomeComponent) : Child
        data class Camera(val component: CameraComponent) : Child
        data class ImageReview(val component: ImageReviewComponent) : Child
        data class Form(val component: MatchFormComponent) : Child
        data class Result(val component: ResultComponent) : Child
    }

    sealed interface Output {
        data class OpenResult(val assessment: Assessment) : Output
        data object ExitDraft : Output
    }
}

interface MatchHomeComponent {
    val model: Value<Model>

    fun onCameraRequested()
    fun onChoosePhotoRequested()
    fun onManualEntryRequested()
    fun onDraftLostNoticeDismissed()

    data class Model(
        val catalogAvailability: CatalogAvailability,
        val draftLostNoticeVisible: Boolean,
        val cameraHint: CameraStatus?,
    )
}

enum class CameraStatus {
    Unknown,
    Checking,
    Granted,
    DeniedCanAsk,
    DeniedOpenSettings,
    Unavailable,
}

interface CameraComponent {
    val model: Value<Model>

    fun onCloseRequested()
    fun onPermissionRequested()
    fun onOpenSystemSettingsRequested()
    fun onChoosePhotoRequested()
    fun onManualEntryRequested()
    fun onShutterRequested()
    fun onTorchChanged(enabled: Boolean)
    fun onCameraStatusChanged(status: CameraStatus)
    fun onCameraCapabilitiesChanged(torchAvailable: Boolean)

    data class Model(
        val status: CameraStatus,
        val torchAvailable: Boolean,
        val torchEnabled: Boolean,
        val captureInProgress: Boolean,
        val error: ImageFailure?,
    ) {
        /** Keep the native preview/session alive until an in-flight capture completes. */
        val requiresActiveCameraSession: Boolean
            get() = status == CameraStatus.Granted && error == null
    }
}

interface ImageReviewComponent {
    val model: Value<Model>

    fun onUsePhotoRequested()
    fun onRecognitionCancelled()
    fun onRetakeRequested()
    fun onChooseAnotherRequested()
    fun onManualEntryRequested()
    fun onBackRequested()

    data class Model(
        val image: EphemeralImage,
        val recognitionState: RecognitionState,
    )
}

sealed interface RecognitionState {
    data object Content : RecognitionState
    data object ReadingOnDevice : RecognitionState
    data class Failed(val reason: RecognitionFailure) : RecognitionState
}

enum class RecognitionFailure {
    NoTextFound,
    UnsupportedImage,
    RecognitionFailed,
    UnreadableImage,
}

enum class ImageFailure {
    PermissionDenied,
    CameraUnavailable,
    CaptureFailed,
    UnreadableImage,
    Unknown,
}

interface MatchFormComponent {
    val model: Value<Model>

    fun onFieldTextChanged(field: FieldKey, value: String)
    fun onKnownBaseSelected(code: BaseCode)
    fun onUnknownBaseSelected(rawText: String)
    fun onBaseCleared()
    fun onObservationConfirmed(field: FieldKey)
    fun onObservationRejected(field: FieldKey)
    fun onAssessRequested()
    fun onBackRequested()
    fun onDiscardConfirmed()
    fun onDiscardCancelled()

    data class Model(
        val mode: Mode,
        val fields: Map<FieldKey, FieldModel>,
        val observations: List<ObservedField>,
        val confirmedInput: ConfirmedMatchInput,
        val unresolvedObservationKeys: Set<FieldKey>,
        val unresolvedRequiredKeys: Set<FieldKey>,
        val canAssess: Boolean,
        val firstUnresolvedField: FieldKey?,
        val discardConfirmationVisible: Boolean,
        val catalogAvailability: CatalogAvailability,
    )

    enum class Mode {
        Manual,
        OcrReview,
        ReferencePrefill,
    }

    data class FieldModel(
        val rawValue: String,
        val origin: FieldOrigin?,
        val reviewDecision: ReviewDecision,
        val required: Boolean,
        val validationErrorCode: String?,
    )

    enum class ReviewDecision {
        NotObserved,
        Pending,
        Confirmed,
        Rejected,
    }
}

interface ResultComponent {
    val model: Value<Model>

    fun onSaveRequested()
    fun onEditDetailsRequested()
    fun onStartAnotherMatchRequested()
    fun onReferenceRequested()
    fun onExitRequested()

    data class Model(
        val assessment: Assessment,
        val completedMatchOrdinal: Int?,
        val saveInProgress: Boolean,
        val interstitialPending: Boolean,
    )

    sealed interface Output {
        data object SaveRequested : Output
        data object StartAnotherMatch : Output
        data class ExitResult(
            val outcome: AssessmentOutcome,
            val completedMatchOrdinal: Int?,
        ) : Output
    }
}

interface SaveResultComponent {
    val model: Value<Model>

    fun onNameChanged(value: String)
    fun onSaveRequested()
    fun onCancelRequested()

    data class Model(
        val name: String,
        val saving: Boolean,
        val errorCode: String?,
        val assessment: Assessment,
    )
}

interface HistoryComponent {
    val model: Value<Model>
    val detailSlot: Value<ChildSlot<*, SavedResultComponent>>

    fun onRetryRequested()
    fun onSavedMatchSelected(id: SavedMatchId)
    fun onDeleteRequested(id: SavedMatchId)
    fun onDeleteConfirmed()
    fun onDeleteCancelled()
    fun onClearAllRequested()
    fun onClearAllConfirmed()
    fun onClearAllCancelled()
    fun onStartMatchRequested()

    data class Model(
        val loading: Boolean,
        val summaries: List<SavedMatchSummary>,
        val readError: Boolean,
        val pendingDelete: SavedMatchId?,
        val clearAllConfirmationVisible: Boolean,
    )

    sealed interface Output {
        data class OpenSavedMatch(val id: SavedMatchId) : Output
        data object StartMatch : Output
    }
}

interface SavedResultComponent {
    val model: Value<Model>
    fun onBackRequested()
    fun onDeleteRequested()
    fun onDeleteConfirmed()
    fun onDeleteCancelled()

    data class Model(
        val loading: Boolean,
        val savedMatch: SavedMatch?,
        val unavailable: Boolean,
        val deleteConfirmationVisible: Boolean,
    )
}

interface ReferenceComponent {
    val model: Value<Model>
    val detailSlot: Value<ChildSlot<*, ReferenceDetailComponent>>

    fun onQueryChanged(query: String)
    fun onEntrySelected(code: BaseCode)
    fun onClearQueryRequested()

    data class Model(
        val query: String,
        val entries: List<CatalogEntry>,
        val catalogAvailability: CatalogAvailability,
    )

    sealed interface Output {
        data class UseBase(val baseCode: BaseCode) : Output
    }
}

interface ReferenceDetailComponent {
    val entry: CatalogEntry
    fun onBackRequested()
    fun onUseBaseRequested()
}

interface SettingsComponent {
    val model: Value<Model>

    fun onLanguageSelected(value: LocaleOverride)
    fun onThemeSelected(value: ThemeOverride)
    fun onClearLocalDataRequested()
    fun onClearLocalDataConfirmed()
    fun onClearLocalDataCancelled()
    fun onBackRequested()

    data class Model(
        val locale: LocaleOverride,
        val theme: ThemeOverride,
        val clearing: Boolean,
        val clearConfirmationVisible: Boolean,
        val errorCode: String?,
        val catalogAvailability: CatalogAvailability,
    )

    sealed interface Output {
        data class LanguageChanged(val value: LocaleOverride) : Output
        data class ThemeChanged(val value: ThemeOverride) : Output
        data object LocalDataCleared : Output
    }
}

/**
 * The app layer deliberately knows only this memory-owned abstraction. Platform handles are
 * adapted at the composition root and must release native image resources from [release].
 */
interface EphemeralImage {
    val debugLabel: String
    fun release()
}

interface ImageActions {
    fun requestCameraPermission()
    fun openSystemCameraSettings()
    fun openPhotoPicker()
    fun capturePhoto()
    fun setTorch(enabled: Boolean)
}

interface RecognitionGateway {
    suspend fun recognize(image: EphemeralImage): RecognitionResult
}

sealed interface RecognitionResult {
    data class Success(val observations: List<ObservedField>) : RecognitionResult
    data class Failure(val reason: RecognitionFailure) : RecognitionResult
}

interface InterstitialGateway {
    /**
     * Completion must be called for success, unavailability, load failure, and show failure.
     * Product navigation never waits for a successful advertisement.
     */
    fun showMatchExit(onComplete: (impressionRecorded: Boolean) -> Unit)
}

fun Assessment.outcome(): AssessmentOutcome =
    when (this) {
        is Assessment.Compatible -> AssessmentOutcome.Compatible
        is Assessment.NeedClarification -> AssessmentOutcome.NeedClarification
        is Assessment.PotentialConflict -> AssessmentOutcome.PotentialConflict
    }

fun ConfirmedMatchInput.knownBaseOrNull(): BaseCode? =
    (base as? ConfirmedBase.Known)?.code
