package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable

class DefaultRootComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    compatibilityEngine: CompatibilityEngine,
    catalogProvider: CatalogProvider,
    settingsRepository: SettingsRepository,
    savedMatchRepository: SavedMatchRepository,
    recognitionGateway: RecognitionGateway,
    imageActions: ImageActions,
    interstitialGateway: InterstitialGateway,
    nowEpochMs: () -> Long,
    newSavedMatchId: () -> String,
) : RootComponent, ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val selected = MutableValue(RootDestination.Match)
    override val selectedDestination: Value<RootDestination> = selected
    private val locale = MutableValue(settingsRepository.localeOverride.value)
    override val localeOverride: Value<LocaleOverride> = locale
    private val theme = MutableValue(settingsRepository.themeOverride.value)
    override val themeOverride: Value<ThemeOverride> = theme

    private val restoredEphemeralMarker =
        stateKeeper.consume(key = EPHEMERAL_MARKER_KEY, strategy = EphemeralMarker.serializer())

    override val match: DefaultMatchComponent =
        DefaultMatchComponent(
            componentContext = childContext(key = "RootMatch"),
            storeFactory = storeFactory,
            compatibilityEngine = compatibilityEngine,
            catalogProvider = catalogProvider,
            settingsRepository = settingsRepository,
            savedMatchRepository = savedMatchRepository,
            recognitionGateway = recognitionGateway,
            imageActions = imageActions,
            interstitialGateway = interstitialGateway,
            nowEpochMs = nowEpochMs,
            newSavedMatchId = newSavedMatchId,
            draftLostNotice = restoredEphemeralMarker?.hadDraft == true,
            onOpenReference = { selectDestination(RootDestination.Reference) },
            output = { /* Root owns navigation; typed Match outputs stay observable at this boundary. */ },
        )

    override val history: HistoryComponent =
        DefaultHistoryComponent(
            componentContext = childContext(key = "RootHistory"),
            repository = savedMatchRepository,
            settingsRepository = settingsRepository,
            output = { output ->
                if (output == HistoryComponent.Output.StartMatch) {
                    selectDestination(RootDestination.Match)
                }
            },
        )

    override val reference: ReferenceComponent =
        DefaultReferenceComponent(
            componentContext = childContext(key = "RootReference"),
            catalogProvider = catalogProvider,
            output = { output ->
                when (output) {
                    is ReferenceComponent.Output.UseBase -> {
                        match.startWithBase(output.baseCode)
                        selectDestination(RootDestination.Match)
                    }
                }
            },
        )

    private val settingsNavigation = SlotNavigation<SettingsConfig>()
    private val typedSettingsSlot: Value<ChildSlot<SettingsConfig, SettingsComponent>> =
        childSlot(
            source = settingsNavigation,
            serializer = null,
            key = "RootSettings",
            handleBackButton = true,
            childFactory = { _, context ->
                DefaultSettingsComponent(
                    componentContext = context,
                    catalogProvider = catalogProvider,
                    settingsRepository = settingsRepository,
                    savedMatchRepository = savedMatchRepository,
                    output = { /* Locale/theme flows are observed by the UI composition root. */ },
                    onDismiss = settingsNavigation::dismiss,
                )
            },
        )

    override val settingsSlot: Value<ChildSlot<*, SettingsComponent>> = typedSettingsSlot

    init {
        settingsRepository.localeOverride
            .onEach { locale.value = it }
            .launchIn(scope)
        settingsRepository.themeOverride
            .onEach { theme.value = it }
            .launchIn(scope)
        stateKeeper.register(key = EPHEMERAL_MARKER_KEY, strategy = EphemeralMarker.serializer()) {
            EphemeralMarker(hadDraft = match.hasEphemeralDraft())
        }
        lifecycle.doOnDestroy(scope::cancel)
    }

    override fun selectDestination(destination: RootDestination) {
        selected.value = destination
    }

    override fun openSettings() {
        settingsNavigation.activate(SettingsConfig)
    }

    override fun closeSettings() {
        settingsNavigation.dismiss()
    }

    @Serializable
    private data class EphemeralMarker(val hadDraft: Boolean)

    @Serializable
    private data object SettingsConfig

    private companion object {
        const val EPHEMERAL_MARKER_KEY = "BulbMatchEphemeralMarker"
    }
}
