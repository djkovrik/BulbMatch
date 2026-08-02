package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.CreatedAtEpochMillis
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class DefaultMatchComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    compatibilityEngine: CompatibilityEngine,
    private val catalogProvider: CatalogProvider,
    private val settingsRepository: SettingsRepository,
    private val savedMatchRepository: SavedMatchRepository,
    private val recognitionGateway: RecognitionGateway,
    private val imageActions: ImageActions,
    private val interstitialGateway: InterstitialGateway,
    private val nowEpochMs: () -> Long,
    private val newSavedMatchId: () -> String,
    draftLostNotice: Boolean,
    private val onOpenReference: () -> Unit,
    private val output: (MatchComponent.Output) -> Unit,
) : MatchComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val navigation = StackNavigation<Config>()
    private val saveNavigation = SlotNavigation<SaveConfig>()
    private val draftLostNoticeValue = MutableValue(draftLostNotice)
    private var image: EphemeralImage? = null
    private var recognitionJob: Job? = null
    private var imageRevision: Long = 0L
    private var activeResultModel: MutableValue<ResultComponent.Model>? = null

    private val store: MatchStore =
        instanceKeeper.getStore {
            MatchStoreProvider(
                storeFactory = storeFactory,
                compatibilityEngine = compatibilityEngine,
                catalogProvider = catalogProvider,
                settingsRepository = settingsRepository,
            ).provide()
        }

    private val typedStack: Value<ChildStack<Config, MatchComponent.Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override val stack: Value<ChildStack<*, MatchComponent.Child>> = typedStack

    private val typedSaveSlot: Value<ChildSlot<SaveConfig, SaveResultComponent>> =
        childSlot(
            source = saveNavigation,
            serializer = null,
            key = "SaveResultSlot",
            handleBackButton = true,
            childFactory = { _, context ->
                DefaultSaveResultComponent(
                    componentContext = context,
                    assessment = requireNotNull(store.state.assessment),
                    catalogProvider = catalogProvider,
                    repository = savedMatchRepository,
                    nowEpochMs = nowEpochMs,
                    newSavedMatchId = newSavedMatchId,
                    onDismiss = ::dismissSaveSlot,
                )
            },
        )

    override val saveSlot: Value<ChildSlot<*, SaveResultComponent>> = typedSaveSlot

    init {
        store.labels
            .onEach { label ->
                when (label) {
                    is MatchStore.Label.AssessmentReady -> {
                        releaseImage()
                        output(MatchComponent.Output.OpenResult(label.assessment))
                        navigation.pushNew(Config.Result)
                    }
                }
            }
            .launchIn(scope)

        lifecycle.doOnDestroy {
            recognitionJob?.cancel()
            image?.release()
            image = null
            scope.cancel()
        }
    }

    override fun onCameraImageAvailable(image: EphemeralImage) {
        showImageReview(image)
    }

    override fun onPickedImageAvailable(image: EphemeralImage) {
        showImageReview(image)
    }

    override fun onImageSelectionCancelled() = Unit

    override fun onImageSelectionFailed(reason: ImageFailure) {
        currentCamera()?.setFailure(reason)
    }

    override fun hasEphemeralDraft(): Boolean = image != null || store.state.hasDraft

    fun startWithBase(baseCode: BaseCode) {
        releaseImage()
        store.accept(MatchStore.Intent.StartReferencePrefill(baseCode))
        navigation.replaceAll(Config.Home, Config.Form)
    }

    private fun createChild(
        config: Config,
        context: ComponentContext,
    ): MatchComponent.Child =
        when (config) {
            Config.Home -> MatchComponent.Child.Home(createHome())
            Config.Camera -> MatchComponent.Child.Camera(DefaultCameraComponent())
            is Config.ImageReview -> MatchComponent.Child.ImageReview(createImageReview(context))
            Config.Form -> MatchComponent.Child.Form(DefaultMatchFormComponent(context))
            Config.Result -> MatchComponent.Child.Result(createResult(context))
        }

    private fun createHome(): MatchHomeComponent =
        object : MatchHomeComponent {
            override val model: Value<MatchHomeComponent.Model> =
                MutableValue(
                    MatchHomeComponent.Model(
                        catalogAvailability = catalogProvider.availability.value,
                        draftLostNoticeVisible = draftLostNoticeValue.value,
                        cameraHint = null,
                    ),
            )

            override fun onCameraRequested() {
                if (typedStack.value.active.configuration == Config.Camera) return
                navigation.pushNew(Config.Camera)
                imageActions.requestCameraPermission()
            }

            override fun onChoosePhotoRequested() = imageActions.openPhotoPicker()

            override fun onManualEntryRequested() {
                if (typedStack.value.active.configuration == Config.Form) return
                store.accept(MatchStore.Intent.StartManual)
                navigation.pushNew(Config.Form)
            }

            override fun onDraftLostNoticeDismissed() {
                draftLostNoticeValue.value = false
                (model as? MutableValue)?.update { it.copy(draftLostNoticeVisible = false) }
            }
        }

    private fun createImageReview(context: ComponentContext): ImageReviewComponent {
        val currentImage = requireNotNull(image) { "ImageReview requires a live ephemeral image." }
        val model = MutableValue(
            ImageReviewComponent.Model(
                image = currentImage,
                recognitionState = RecognitionState.Content,
            ),
        )
        val component = object : ImageReviewComponent {
            override val model: Value<ImageReviewComponent.Model> = model

            override fun onUsePhotoRequested() {
                if (recognitionJob?.isActive == true) return
                model.update { it.copy(recognitionState = RecognitionState.ReadingOnDevice) }
                recognitionJob = scope.launch {
                    val result = try {
                        recognitionGateway.recognize(currentImage)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        RecognitionResult.Failure(RecognitionFailure.RecognitionFailed)
                    }
                    when (result) {
                        is RecognitionResult.Success -> {
                            if (result.observations.isEmpty()) {
                                model.update {
                                    it.copy(
                                        recognitionState = RecognitionState.Failed(
                                            RecognitionFailure.NoTextFound,
                                        ),
                                    )
                                }
                            } else {
                                store.accept(MatchStore.Intent.StartOcrReview(result.observations))
                                navigation.pushNew(Config.Form)
                            }
                        }
                        is RecognitionResult.Failure ->
                            model.update { it.copy(recognitionState = RecognitionState.Failed(result.reason)) }
                    }
                }
            }

            override fun onRecognitionCancelled() {
                recognitionJob?.cancel()
                recognitionJob = null
                model.update { it.copy(recognitionState = RecognitionState.Content) }
            }

            override fun onRetakeRequested() {
                releaseImage()
                navigation.replaceAll(Config.Home, Config.Camera)
                imageActions.requestCameraPermission()
            }

            override fun onChooseAnotherRequested() {
                imageActions.openPhotoPicker()
            }

            override fun onManualEntryRequested() {
                releaseImage()
                store.accept(MatchStore.Intent.StartManual)
                navigation.replaceAll(Config.Home, Config.Form)
            }

            override fun onBackRequested() {
                releaseImage()
                navigation.pop()
            }
        }
        context.backHandler.register(BackCallback(onBack = component::onBackRequested))
        return component
    }

    private fun createResult(context: ComponentContext): ResultComponent {
        val resultModel = MutableValue(resultModel())
        activeResultModel = resultModel
        val component = object : ResultComponent {
            override val model: Value<ResultComponent.Model> = resultModel

            override fun onSaveRequested() {
                handleResultOutput(ResultComponent.Output.SaveRequested)
            }

            override fun onEditDetailsRequested() = navigation.pop()

            override fun onStartAnotherMatchRequested() {
                handleResultOutput(ResultComponent.Output.StartAnotherMatch)
            }

            override fun onReferenceRequested() = onOpenReference()

            override fun onExitRequested() {
                handleResultOutput(
                    ResultComponent.Output.ExitResult(
                        outcome = store.state.assessment!!.outcome(),
                        completedMatchOrdinal = store.state.completedMatchOrdinal,
                    ),
                )
            }
        }
        context.backHandler.register(BackCallback(onBack = component::onExitRequested))
        return component
    }

    private fun resultModel(): ResultComponent.Model =
        ResultComponent.Model(
            assessment = requireNotNull(store.state.assessment),
            completedMatchOrdinal = store.state.completedMatchOrdinal,
            saveInProgress = typedSaveSlot.value.child != null,
            interstitialPending = false,
        )

    private fun handleResultOutput(resultOutput: ResultComponent.Output) {
        when (resultOutput) {
            ResultComponent.Output.SaveRequested -> {
                if (typedSaveSlot.value.child != null) return
                saveNavigation.activate(SaveConfig)
                activeResultModel?.update { it.copy(saveInProgress = true) }
            }
            ResultComponent.Output.StartAnotherMatch -> returnToHome(emitExitDraft = false)
            is ResultComponent.Output.ExitResult -> exitResult(resultOutput)
        }
    }

    private fun exitResult(resultOutput: ResultComponent.Output.ExitResult) {
        if (typedSaveSlot.value.child != null) return
        val input = InterstitialEligibilityInput(
            outcome = resultOutput.outcome,
            completedMatchOrdinal = resultOutput.completedMatchOrdinal,
            frequency = settingsRepository.adFrequencyState.value,
            nowEpochMs = nowEpochMs(),
            explicitResultExit = true,
            saveInProgress = typedSaveSlot.value.child != null,
            pendingDestructiveDialog = false,
        )
        if (!InterstitialEligibilityPolicy.isEligible(input)) {
            returnToHome(emitExitDraft = false)
            return
        }

        activeResultModel?.update { it.copy(interstitialPending = true) }
        var completed = false
        val completeNavigation: (Boolean) -> Unit = { impressionRecorded ->
            if (!completed) {
                completed = true
                activeResultModel?.update { it.copy(interstitialPending = false) }
                returnToHome(emitExitDraft = false)
                if (impressionRecorded) {
                    scope.launch { settingsRepository.recordInterstitialImpression(nowEpochMs()) }
                }
            }
        }
        try {
            interstitialGateway.showMatchExit(completeNavigation)
        } catch (_: Throwable) {
            completeNavigation(false)
        }
    }

    private fun returnToHome(emitExitDraft: Boolean) {
        dismissSaveSlot()
        recognitionJob?.cancel()
        releaseImage()
        store.accept(MatchStore.Intent.ClearSession)
        navigation.replaceAll(Config.Home)
        if (emitExitDraft) output(MatchComponent.Output.ExitDraft)
    }

    private fun dismissSaveSlot() {
        saveNavigation.dismiss()
        activeResultModel?.update { it.copy(saveInProgress = false) }
    }

    private fun currentCamera(): DefaultCameraComponent? =
        (typedStack.value.active.instance as? MatchComponent.Child.Camera)
            ?.component as? DefaultCameraComponent

    private fun replaceImage(newImage: EphemeralImage) {
        if (image !== newImage) image?.release()
        image = newImage
    }

    private fun showImageReview(newImage: EphemeralImage) {
        recognitionJob?.cancel()
        recognitionJob = null
        val replacingReview = typedStack.value.active.configuration is Config.ImageReview
        replaceImage(newImage)
        val config = Config.ImageReview(revision = ++imageRevision)
        if (replacingReview) {
            navigation.replaceCurrent(config)
        } else {
            navigation.pushNew(config)
        }
    }

    private fun releaseImage() {
        recognitionJob?.cancel()
        recognitionJob = null
        image?.release()
        image = null
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config

        @Serializable
        data object Camera : Config

        @Serializable
        data class ImageReview(val revision: Long) : Config

        @Serializable
        data object Form : Config

        @Serializable
        data object Result : Config
    }

    @Serializable
    private data object SaveConfig

    private inner class DefaultCameraComponent : CameraComponent {
        private val mutableModel = MutableValue(
            CameraComponent.Model(
                status = CameraStatus.Checking,
                torchAvailable = false,
                torchEnabled = false,
                captureInProgress = false,
                error = null,
            ),
        )
        override val model: Value<CameraComponent.Model> = mutableModel

        override fun onCloseRequested() = navigation.pop()
        override fun onPermissionRequested() = imageActions.requestCameraPermission()
        override fun onOpenSystemSettingsRequested() = imageActions.openSystemCameraSettings()
        override fun onChoosePhotoRequested() {
            if (mutableModel.value.captureInProgress) return
            imageActions.openPhotoPicker()
        }

        override fun onManualEntryRequested() {
            if (mutableModel.value.captureInProgress) return
            store.accept(MatchStore.Intent.StartManual)
            navigation.replaceAll(Config.Home, Config.Form)
        }

        override fun onShutterRequested() {
            val current = mutableModel.value
            if (
                current.captureInProgress ||
                current.status != CameraStatus.Granted ||
                current.error != null
            ) {
                return
            }
            mutableModel.update { it.copy(captureInProgress = true, error = null) }
            imageActions.capturePhoto()
        }

        override fun onTorchChanged(enabled: Boolean) {
            if (!mutableModel.value.torchAvailable || mutableModel.value.captureInProgress) return
            mutableModel.update { it.copy(torchEnabled = enabled) }
            imageActions.setTorch(enabled)
        }

        override fun onCameraStatusChanged(status: CameraStatus) {
            mutableModel.update {
                it.copy(
                    status = status,
                    captureInProgress = false,
                    error = if (status == CameraStatus.Unavailable) ImageFailure.CameraUnavailable else null,
                )
            }
        }

        override fun onCameraCapabilitiesChanged(torchAvailable: Boolean) {
            mutableModel.update {
                it.copy(
                    torchAvailable = torchAvailable,
                    torchEnabled = if (torchAvailable) it.torchEnabled else false,
                )
            }
        }

        fun setFailure(reason: ImageFailure) {
            mutableModel.update { it.copy(captureInProgress = false, error = reason) }
        }
    }

    private inner class DefaultMatchFormComponent(
        componentContext: ComponentContext,
    ) : MatchFormComponent, ComponentContext by componentContext {
        private var discardConfirmationVisible = false
        private val mutableModel = MutableValue(store.state.toFormModel())
        override val model: Value<MatchFormComponent.Model> = mutableModel

        init {
            backHandler.register(BackCallback(onBack = ::onBackRequested))
            store.states
                .onEach { mutableModel.value = it.toFormModel() }
                .launchIn(scope)
        }

        override fun onFieldTextChanged(field: FieldKey, value: String) =
            store.accept(MatchStore.Intent.FieldTextChanged(field, value))

        override fun onKnownBaseSelected(code: BaseCode) =
            store.accept(MatchStore.Intent.KnownBaseSelected(code))

        override fun onUnknownBaseSelected(rawText: String) =
            store.accept(MatchStore.Intent.UnknownBaseSelected(rawText))

        override fun onBaseCleared() = store.accept(MatchStore.Intent.BaseCleared)

        override fun onObservationConfirmed(field: FieldKey) =
            store.accept(MatchStore.Intent.ObservationConfirmed(field))

        override fun onObservationRejected(field: FieldKey) =
            store.accept(MatchStore.Intent.ObservationRejected(field))

        override fun onAssessRequested() = store.accept(MatchStore.Intent.Assess)

        override fun onBackRequested() {
            if (store.state.hasDraft) {
                discardConfirmationVisible = true
                mutableModel.value = store.state.toFormModel()
            } else {
                navigation.pop()
            }
        }

        override fun onDiscardConfirmed() {
            discardConfirmationVisible = false
            returnToHome(emitExitDraft = true)
        }

        override fun onDiscardCancelled() {
            discardConfirmationVisible = false
            mutableModel.value = store.state.toFormModel()
        }

        private fun MatchStore.State.toFormModel(): MatchFormComponent.Model {
            val unresolvedRequired = buildSet {
                if (input.base == ConfirmedBase.Missing) add(FieldKey.Base)
                if (input.voltage == VoltageEvidence.Missing) add(FieldKey.Voltage)
            }
            val unresolvedObservations = input.unhandledObservationKeys
            val fields = FieldKey.entries.associateWith { key ->
                MatchFormComponent.FieldModel(
                    rawValue = rawValues[key].orEmpty(),
                    origin = origins[key],
                    reviewDecision = when {
                        key in input.rejectedObservations -> MatchFormComponent.ReviewDecision.Rejected
                        key in input.reviewedFields -> MatchFormComponent.ReviewDecision.Confirmed
                        key in input.observationKeys -> MatchFormComponent.ReviewDecision.Pending
                        else -> MatchFormComponent.ReviewDecision.NotObserved
                    },
                    required = key == FieldKey.Base || key == FieldKey.Voltage,
                    validationErrorCode = validationErrors[key],
                )
            }
            val firstUnresolved = FieldKey.entries.firstOrNull {
                it in unresolvedObservations ||
                    it in unresolvedRequired ||
                    validationErrors[it] != null
            }
            return MatchFormComponent.Model(
                mode = mode,
                fields = fields,
                observations = observations,
                confirmedInput = input,
                unresolvedObservationKeys = unresolvedObservations,
                unresolvedRequiredKeys = unresolvedRequired,
                canAssess = catalogProvider.availability.value is CatalogAvailability.Available &&
                    unresolvedObservations.isEmpty() &&
                    unresolvedRequired.isEmpty() &&
                    validationErrors.isEmpty(),
                firstUnresolvedField = firstUnresolved,
                discardConfirmationVisible = discardConfirmationVisible,
                catalogAvailability = catalogProvider.availability.value,
            )
        }
    }
}

private class DefaultSaveResultComponent(
    componentContext: ComponentContext,
    private val assessment: Assessment,
    private val catalogProvider: CatalogProvider,
    private val repository: SavedMatchRepository,
    private val nowEpochMs: () -> Long,
    private val newSavedMatchId: () -> String,
    private val onDismiss: () -> Unit,
) : SaveResultComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val pendingId = SavedMatchId.from(newSavedMatchId())
    private val pendingCreatedAt = CreatedAtEpochMillis.from(nowEpochMs())
    private val mutableModel = MutableValue(SaveResultComponent.Model("", false, null, assessment))
    override val model: Value<SaveResultComponent.Model> = mutableModel

    init {
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun onNameChanged(value: String) {
        if (mutableModel.value.saving) return
        mutableModel.update { it.copy(name = value, errorCode = validateName(value)) }
    }

    override fun onSaveRequested() {
        val current = mutableModel.value
        val error = validateName(current.name)
        if (current.saving || error != null) {
            if (error != null) mutableModel.update { it.copy(errorCode = error) }
            return
        }
        val catalog = (catalogProvider.availability.value as? CatalogAvailability.Available)?.catalog
        if (catalog == null) {
            mutableModel.update { it.copy(errorCode = "catalog_unavailable") }
            return
        }
        val id = pendingId
        val createdAt = pendingCreatedAt
        if (id == null || createdAt == null) {
            mutableModel.update { it.copy(errorCode = "save_identity_failed") }
            return
        }
        val trimmedName = current.name.trim().ifEmpty { null }
        val saved = SavedMatch(
            id = id,
            displayName = trimmedName,
            createdAt = createdAt,
            confirmedInput = assessment.retainedConfirmedInput,
            assessment = assessment,
            catalogVersion = catalog.snapshot.catalogVersion,
            rulesetVersion = catalog.snapshot.rulesetVersion,
            snapshotSchemaVersion = 1,
        )
        mutableModel.update { it.copy(saving = true, errorCode = null) }
        scope.launch {
            when (repository.save(saved)) {
                is RepositoryResult.Success -> onDismiss()
                is RepositoryResult.Failure ->
                    mutableModel.update { it.copy(saving = false, errorCode = "save_failed") }
            }
        }
    }

    override fun onCancelRequested() {
        if (!mutableModel.value.saving) onDismiss()
    }
}

private fun validateName(value: String): String? {
    if (value.any { it.code < 0x20 || it.code in 0x7F..0x9F }) return "name_control_character"
    return if (value.trim().codePointCount() > 80) "name_too_long" else null
}

private fun String.codePointCount(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        val current = this[index]
        when {
            current.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate() -> index += 2
            current.isSurrogate() -> return Int.MAX_VALUE
            else -> index += 1
        }
        count++
    }
    return count
}
