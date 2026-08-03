package com.sedsoftware.bulbmatch.app

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.ObservedField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMatchComponentTest {
    private val fixture = AppComponentTestFixture()

    @BeforeTest
    fun setUp() = fixture.setUp()

    @AfterTest
    fun tearDown() = fixture.tearDown()

    @Test
    fun cameraFlowIsDuplicateSafeAndExposesRecoveryActions() = runTest(fixture.dispatcher) {
        val actions = RecordingImageActions()
        val component = createComponent(imageActions = actions)
        val home = activeHome(component)

        home.onCameraRequested()
        home.onCameraRequested()

        assertEquals(listOf("requestCameraPermission"), actions.calls)
        val camera = assertIs<MatchComponent.Child.Camera>(component.stack.value.active.instance).component
        camera.onCameraStatusChanged(CameraStatus.Granted)
        camera.onCameraCapabilitiesChanged(torchAvailable = true)
        camera.onTorchChanged(enabled = true)
        camera.onShutterRequested()
        camera.onShutterRequested()
        camera.onChoosePhotoRequested()

        assertTrue(camera.model.value.torchEnabled)
        assertTrue(camera.model.value.captureInProgress)
        assertTrue(camera.model.value.requiresActiveCameraSession)
        assertEquals(0, actions.calls.count { it == "openPhotoPicker" })

        component.onImageSelectionFailed(ImageFailure.CaptureFailed)
        camera.onOpenSystemSettingsRequested()
        camera.onChoosePhotoRequested()

        assertEquals(
            listOf(
                "requestCameraPermission",
                "setTorch:true",
                "capturePhoto",
                "openSystemCameraSettings",
                "openPhotoPicker",
            ),
            actions.calls,
        )

        assertEquals(ImageFailure.CaptureFailed, camera.model.value.error)
        assertFalse(camera.model.value.captureInProgress)
        assertFalse(camera.model.value.requiresActiveCameraSession)

        camera.onCameraStatusChanged(CameraStatus.Unavailable)
        assertEquals(ImageFailure.CameraUnavailable, camera.model.value.error)
        camera.onCameraCapabilitiesChanged(torchAvailable = false)
        camera.onTorchChanged(enabled = true)
        assertFalse(camera.model.value.torchAvailable)
        assertFalse(camera.model.value.torchEnabled)
    }

    @Test
    fun ac002CameraSessionRemainsRequiredUntilCaptureFinishes() = runTest(fixture.dispatcher) {
        val actions = RecordingImageActions()
        val component = createComponent(imageActions = actions)

        activeHome(component).onCameraRequested()
        val camera = assertIs<MatchComponent.Child.Camera>(
            component.stack.value.active.instance,
        ).component
        camera.onCameraStatusChanged(CameraStatus.Granted)

        camera.onShutterRequested()
        camera.onShutterRequested()
        camera.onChoosePhotoRequested()
        camera.onManualEntryRequested()

        assertTrue(camera.model.value.captureInProgress)
        assertTrue(camera.model.value.requiresActiveCameraSession)
        assertEquals(1, actions.calls.count { it == "capturePhoto" })
        assertEquals(0, actions.calls.count { it == "openPhotoPicker" })
        assertIs<MatchComponent.Child.Camera>(component.stack.value.active.instance)

        component.onImageSelectionFailed(ImageFailure.CaptureFailed)

        assertFalse(camera.model.value.captureInProgress)
        assertFalse(camera.model.value.requiresActiveCameraSession)
    }

    @Test
    fun successfulOcrRequiresEveryDecisionAndReleasesImageAtResult() = runTest(fixture.dispatcher) {
        val image = FakeEphemeralImage()
        val outputs = mutableListOf<MatchComponent.Output>()
        val component = createComponent(
            recognitionGateway = FakeRecognitionGateway(
                RecognitionResult.Success(
                    listOf(
                        observation(FieldKey.Base, "E27"),
                        observation(FieldKey.Voltage, "220-240 V"),
                        observation(FieldKey.LuminousFlux, "806 lm"),
                    ),
                ),
            ),
            output = outputs::add,
        )

        component.onPickedImageAvailable(image)
        val review = assertIs<MatchComponent.Child.ImageReview>(
            component.stack.value.active.instance,
        ).component
        review.onUsePhotoRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        val form = assertIs<MatchComponent.Child.Form>(component.stack.value.active.instance).component
        assertEquals(MatchFormComponent.Mode.OcrReview, form.model.value.mode)
        assertEquals(
            setOf(FieldKey.Base, FieldKey.Voltage, FieldKey.LuminousFlux),
            form.model.value.unresolvedObservationKeys,
        )
        assertFalse(form.model.value.canAssess)

        form.onObservationConfirmed(FieldKey.Base)
        form.onObservationConfirmed(FieldKey.Voltage)
        form.onObservationRejected(FieldKey.LuminousFlux)
        fixture.dispatcher.scheduler.runCurrent()
        assertTrue(form.model.value.canAssess)

        form.onAssessRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertIs<MatchComponent.Child.Result>(component.stack.value.active.instance)
        assertTrue(image.released)
        assertIs<MatchComponent.Output.OpenResult>(outputs.single())
    }

    @Test
    fun recognitionCancellationAndFailureKeepTheReviewRecoverable() = runTest(fixture.dispatcher) {
        val recognition = FakeRecognitionGateway(
            RecognitionResult.Failure(RecognitionFailure.UnsupportedImage),
        )
        val component = createComponent(recognitionGateway = recognition)
        component.onCameraImageAvailable(FakeEphemeralImage())
        val review = assertIs<MatchComponent.Child.ImageReview>(
            component.stack.value.active.instance,
        ).component

        review.onUsePhotoRequested()
        assertIs<RecognitionState.ReadingOnDevice>(review.model.value.recognitionState)
        review.onRecognitionCancelled()
        fixture.dispatcher.scheduler.advanceUntilIdle()
        assertIs<RecognitionState.Content>(review.model.value.recognitionState)
        assertEquals(0, recognition.invocationCount)

        review.onUsePhotoRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()
        val failed = assertIs<RecognitionState.Failed>(review.model.value.recognitionState)
        assertEquals(RecognitionFailure.UnsupportedImage, failed.reason)
        assertEquals(1, recognition.invocationCount)
    }

    @Test
    fun manualBaseTextUsesTheCatalogSelectionPath() = runTest(fixture.dispatcher) {
        val component = createComponent()
        activeHome(component).onManualEntryRequested()
        val form = assertIs<MatchComponent.Child.Form>(component.stack.value.active.instance).component

        form.onFieldTextChanged(FieldKey.Base, "E27")
        fixture.dispatcher.scheduler.runCurrent()

        assertEquals(testBaseCode(), form.model.value.confirmedInput.knownBaseOrNull())
        assertNull(form.model.value.fields.getValue(FieldKey.Base).validationErrorCode)
    }

    @Test
    fun savingValidatesNameAndPersistsExactlyOneImmutableSnapshot() = runTest(fixture.dispatcher) {
        val repository = InMemorySavedMatchRepository()
        val component = createComponent(savedMatches = repository)
        val result = completeManualCompatibleMatch(component)

        result.onSaveRequested()
        val save = assertNotNull(component.saveSlot.value.child?.instance)
        save.onNameChanged("x".repeat(81))
        assertEquals("name_too_long", save.model.value.errorCode)
        save.onSaveRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repository.snapshot().isEmpty())

        save.onNameChanged("  Kitchen  ")
        save.onSaveRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertNull(component.saveSlot.value.child)
        val stored = repository.snapshot().single()
        assertEquals("Kitchen", stored.displayName)
        assertEquals(testCompatibleInput(), stored.confirmedInput)
        assertIs<Assessment.Compatible>(stored.assessment)
    }

    @Test
    fun draftBackRequiresConfirmationAndEmitsExitDraft() = runTest(fixture.dispatcher) {
        val outputs = mutableListOf<MatchComponent.Output>()
        val component = createComponent(output = outputs::add)
        val home = activeHome(component)
        home.onManualEntryRequested()
        val form = assertIs<MatchComponent.Child.Form>(component.stack.value.active.instance).component
        form.onFieldTextChanged(FieldKey.Voltage, "230")

        form.onBackRequested()
        assertTrue(form.model.value.discardConfirmationVisible)
        form.onDiscardCancelled()
        assertFalse(form.model.value.discardConfirmationVisible)
        form.onBackRequested()
        form.onDiscardConfirmed()

        assertIs<MatchComponent.Child.Home>(component.stack.value.active.instance)
        assertEquals(listOf<MatchComponent.Output>(MatchComponent.Output.ExitDraft), outputs)
        assertFalse(component.hasEphemeralDraft())
    }

    @Test
    fun eligibleExitRecordsOnlyAConfirmedInterstitialImpression() = runTest(fixture.dispatcher) {
        val now = 700_000L
        val settings = InMemorySettingsRepository(
            frequency = AdFrequencyState(
                completedCompatibleMatches = 2,
                lastInterstitialEpochMs = null,
                compatibleMatchesSinceInterstitial = 2,
            ),
        )
        val interstitial = FakeInterstitialGateway(nextImpressionRecorded = true)
        val component = createComponent(
            settings = settings,
            interstitialGateway = interstitial,
            nowEpochMs = { now },
        )
        val result = completeManualCompatibleMatch(component)

        result.onExitRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, interstitial.showCount)
        assertIs<MatchComponent.Child.Home>(component.stack.value.active.instance)
        assertEquals(now, settings.adFrequencyState.value.lastInterstitialEpochMs)
        assertEquals(0, settings.adFrequencyState.value.compatibleMatchesSinceInterstitial)
    }

    private suspend fun completeManualCompatibleMatch(
        component: DefaultMatchComponent,
    ): ResultComponent {
        fixture.dispatcher.scheduler.runCurrent()
        activeHome(component).onManualEntryRequested()
        val form = assertIs<MatchComponent.Child.Form>(component.stack.value.active.instance).component
        form.onKnownBaseSelected(testBaseCode())
        form.onFieldTextChanged(FieldKey.Voltage, "230 V")
        fixture.dispatcher.scheduler.runCurrent()
        form.onAssessRequested()
        fixture.dispatcher.scheduler.advanceUntilIdle()
        return assertIs<MatchComponent.Child.Result>(component.stack.value.active.instance).component
    }

    private fun activeHome(component: DefaultMatchComponent): MatchHomeComponent =
        assertIs<MatchComponent.Child.Home>(component.stack.value.active.instance).component

    private fun createComponent(
        settings: InMemorySettingsRepository = InMemorySettingsRepository(),
        savedMatches: InMemorySavedMatchRepository = InMemorySavedMatchRepository(),
        recognitionGateway: RecognitionGateway = FakeRecognitionGateway(
            RecognitionResult.Failure(RecognitionFailure.NoTextFound),
        ),
        imageActions: ImageActions = RecordingImageActions(),
        interstitialGateway: InterstitialGateway = FakeInterstitialGateway(),
        nowEpochMs: () -> Long = { 1_000L },
        onOpenReference: () -> Unit = {},
        output: (MatchComponent.Output) -> Unit = {},
    ) = DefaultMatchComponent(
        componentContext = fixture.componentContext(),
        storeFactory = DefaultStoreFactory(),
        compatibilityEngine = CompatibilityEngine(),
        catalogProvider = testCatalogProvider(),
        settingsRepository = settings,
        savedMatchRepository = savedMatches,
        recognitionGateway = recognitionGateway,
        imageActions = imageActions,
        interstitialGateway = interstitialGateway,
        nowEpochMs = nowEpochMs,
        newSavedMatchId = { "saved-1" },
        draftLostNotice = false,
        onOpenReference = onOpenReference,
        output = output,
    )

    private fun observation(field: FieldKey, text: String): ObservedField =
        ObservedField(
            fieldKey = field,
            rawText = text,
            parsedCandidate = text,
            confidence = null,
            geometry = null,
        )
}
