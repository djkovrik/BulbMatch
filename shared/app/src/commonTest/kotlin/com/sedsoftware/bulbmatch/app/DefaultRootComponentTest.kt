package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FieldOrigin
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRootComponentTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var lifecycle: LifecycleRegistry

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        lifecycle = LifecycleRegistry().also {
            it.create()
            it.start()
            it.resume()
        }
    }

    @AfterTest
    fun tearDown() {
        lifecycle.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun rootKeepsTabsAliveAndSettingsIsAnOverlay() = runTest(dispatcher) {
        val actions = RecordingImageActions()
        val settings = InMemorySettingsRepository()
        val root = createRoot(actions, settings)

        assertEquals(RootDestination.Match, root.selectedDestination.value)
        root.selectDestination(RootDestination.History)
        assertEquals(RootDestination.History, root.selectedDestination.value)
        root.openSettings()

        val settingsComponent = assertNotNull(root.settingsSlot.value.child?.instance)
        settingsComponent.onLanguageSelected(LocaleOverride.Russian)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocaleOverride.Russian, root.localeOverride.value)
        assertEquals(RootDestination.History, root.selectedDestination.value)
        root.closeSettings()
        assertNull(root.settingsSlot.value.child)

        root.selectDestination(RootDestination.Match)
        val home = assertIs<MatchComponent.Child.Home>(
            root.match.stack.value.active.instance,
        ).component
        home.onCameraRequested()

        assertEquals(listOf("requestCameraPermission"), actions.calls)
        assertIs<MatchComponent.Child.Camera>(root.match.stack.value.active.instance)
    }

    @Test
    fun childOutputsAreInterpretedByTheRootOwner() = runTest(dispatcher) {
        val root = createRoot(
            actions = RecordingImageActions(),
            settings = InMemorySettingsRepository(),
            catalogProvider = testCatalogProvider(),
        )
        dispatcher.scheduler.runCurrent()

        root.selectDestination(RootDestination.Reference)
        root.reference.onEntrySelected(testBaseCode())
        val detail = assertNotNull(root.reference.detailSlot.value.child?.instance)
        detail.onUseBaseRequested()

        assertEquals(RootDestination.Match, root.selectedDestination.value)
        val form = assertIs<MatchComponent.Child.Form>(root.match.stack.value.active.instance).component
        assertEquals(MatchFormComponent.Mode.ReferencePrefill, form.model.value.mode)
        assertEquals("E27", form.model.value.fields.getValue(FieldKey.Base).rawValue)
        assertEquals(FieldOrigin.Manual, form.model.value.fields.getValue(FieldKey.Base).origin)

        root.selectDestination(RootDestination.History)
        root.history.onStartMatchRequested()
        assertEquals(RootDestination.Match, root.selectedDestination.value)

        root.openSettings()
        assertNotNull(root.settingsSlot.value.child?.instance).onBackRequested()
        assertNull(root.settingsSlot.value.child)
    }

    private fun createRoot(
        actions: RecordingImageActions,
        settings: InMemorySettingsRepository,
        catalogProvider: CatalogProvider = InMemoryCatalogProvider(
            CatalogAvailability.Invalid("test_catalog_unavailable"),
        ),
    ): DefaultRootComponent = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
        storeFactory = DefaultStoreFactory(),
        compatibilityEngine = CompatibilityEngine(),
        catalogProvider = catalogProvider,
        settingsRepository = settings,
        savedMatchRepository = InMemorySavedMatchRepository(),
        recognitionGateway = FakeRecognitionGateway(
            RecognitionResult.Failure(RecognitionFailure.NoTextFound),
        ),
        imageActions = actions,
        interstitialGateway = FakeInterstitialGateway(),
        nowEpochMs = { 1_000L },
        newSavedMatchId = { "saved-1" },
    )
}
