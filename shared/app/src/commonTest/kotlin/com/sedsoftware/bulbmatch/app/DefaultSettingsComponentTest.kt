package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSettingsComponentTest {
    private val fixture = AppComponentTestFixture()

    @BeforeTest
    fun setUp() = fixture.setUp()

    @AfterTest
    fun tearDown() = fixture.tearDown()

    @Test
    fun settingsEmitChangesClearOnlyLocalDataAndDismiss() = runTest(fixture.dispatcher) {
        val settings = InMemorySettingsRepository(
            frequency = AdFrequencyState(3, 5_000L, 2),
        )
        val savedMatches = InMemorySavedMatchRepository(listOf(testSavedMatch()))
        val outputs = mutableListOf<SettingsComponent.Output>()
        var dismissed = false
        val component = DefaultSettingsComponent(
            componentContext = fixture.componentContext(),
            catalogProvider = testCatalogProvider(),
            settingsRepository = settings,
            savedMatchRepository = savedMatches,
            output = outputs::add,
            onDismiss = { dismissed = true },
        )

        component.onLanguageSelected(LocaleOverride.Russian)
        component.onThemeSelected(ThemeOverride.Dark)
        fixture.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(LocaleOverride.Russian, component.model.value.locale)
        assertEquals(ThemeOverride.Dark, component.model.value.theme)

        component.onClearLocalDataRequested()
        assertTrue(component.model.value.clearConfirmationVisible)
        component.onClearLocalDataCancelled()
        assertFalse(component.model.value.clearConfirmationVisible)
        component.onClearLocalDataRequested()
        component.onClearLocalDataConfirmed()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertTrue(savedMatches.snapshot().isEmpty())
        assertEquals(AdFrequencyState(0, null, 0), settings.adFrequencyState.value)
        assertEquals(LocaleOverride.Russian, settings.localeOverride.value)
        assertEquals(ThemeOverride.Dark, settings.themeOverride.value)
        assertEquals(
            listOf<SettingsComponent.Output>(
                SettingsComponent.Output.LanguageChanged(LocaleOverride.Russian),
                SettingsComponent.Output.ThemeChanged(ThemeOverride.Dark),
                SettingsComponent.Output.LocalDataCleared,
            ),
            outputs,
        )

        component.onBackRequested()
        assertTrue(dismissed)
    }
}
