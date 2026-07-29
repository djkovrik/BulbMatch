package com.sedsoftware.bulbmatch.data

import com.russhwolf.settings.MapSettings
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultSettingsRepositoryTest {
    @Test
    fun mapsDisplayOverridesAndPropagatesStateFlowUpdates() = runTest {
        val repository = DefaultSettingsRepository(BulbMatchSettingsStore(MapSettings()))
        val localeValues = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.localeOverride.take(2).toList()
        }

        assertEquals(LocaleOverride.System, repository.localeOverride.value)
        assertEquals(ThemeOverride.System, repository.themeOverride.value)
        assertEquals(listOf(LocaleOverride.System), repository.localeOverride.replayCache)

        assertIs<RepositoryResult.Success<Unit>>(
            repository.setLocaleOverride(LocaleOverride.Russian),
        )
        assertIs<RepositoryResult.Success<Unit>>(
            repository.setThemeOverride(ThemeOverride.Dark),
        )
        runCurrent()

        assertEquals(
            listOf(LocaleOverride.System, LocaleOverride.Russian),
            localeValues.await(),
        )
        assertEquals(LocaleOverride.Russian, repository.localeOverride.value)
        assertEquals(ThemeOverride.Dark, repository.themeOverride.value)
    }

    @Test
    fun mapsAdFrequencyMutationsAndWriteFailures() = runTest {
        val repository = DefaultSettingsRepository(BulbMatchSettingsStore(MapSettings()))

        assertEquals(
            AdFrequencyState(0, null, 0),
            repository.adFrequencyState.value,
        )
        assertEquals(
            AdFrequencyState(1, null, 1),
            assertIs<RepositoryResult.Success<AdFrequencyState>>(
                repository.recordCompatibleMatch(),
            ).value,
        )
        assertEquals(
            AdFrequencyState(1, 5_000L, 0),
            assertIs<RepositoryResult.Success<AdFrequencyState>>(
                repository.recordInterstitialImpression(5_000L),
            ).value,
        )

        assertIs<RepositoryResult.Failure>(
            repository.recordInterstitialImpression(-1L),
        )
        assertIs<RepositoryResult.Success<Unit>>(repository.resetAdFrequency())
        assertEquals(AdFrequencyState(0, null, 0), repository.adFrequencyState.value)
    }
}
