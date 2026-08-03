package com.sedsoftware.bulbmatch.data.settings

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BulbMatchSettingsStoreTest {
    @Test
    fun missingValuesUseContractDefaults() {
        val store = BulbMatchSettingsStore(MapSettings())

        assertEquals(StoredLocaleOverride.EN, store.localeOverride.value)
        assertEquals(StoredThemeOverride.SYSTEM, store.themeOverride.value)
        assertEquals(
            AdFrequencyState(
                completedCompatibleMatches = 0,
                lastInterstitialEpochMs = null,
                compatibleMatchesSinceInterstitial = 0,
            ),
            store.adFrequencyState.value,
        )
    }

    @Test
    fun malformedEnumAndCounterValuesFallBackToDefaults() {
        val settings = MapSettings(
            SettingKeys.LocaleOverride to "DE",
            SettingKeys.ThemeOverride to "BLUE",
            SettingKeys.CompletedCompatibleMatches to -8,
            SettingKeys.LastInterstitialEpochMs to -1L,
            SettingKeys.CompatibleMatchesSinceInterstitial to -4,
        )

        val store = BulbMatchSettingsStore(settings)

        assertEquals(StoredLocaleOverride.EN, store.localeOverride.value)
        assertEquals(StoredThemeOverride.SYSTEM, store.themeOverride.value)
        assertEquals(
            AdFrequencyState(0, null, 0),
            store.adFrequencyState.value,
        )
    }

    @Test
    fun legacySystemLocaleFallsBackToEnglish() {
        val store = BulbMatchSettingsStore(
            MapSettings(SettingKeys.LocaleOverride to "SYSTEM"),
        )

        assertEquals(StoredLocaleOverride.EN, store.localeOverride.value)
    }

    @Test
    fun displayOverridesPersistWithStableCodes() = runTest {
        val settings = MapSettings()
        val store = BulbMatchSettingsStore(settings)

        store.setLocaleOverride(StoredLocaleOverride.RU)
        store.setThemeOverride(StoredThemeOverride.DARK)

        val restored = BulbMatchSettingsStore(settings)
        assertEquals(StoredLocaleOverride.RU, restored.localeOverride.value)
        assertEquals(StoredThemeOverride.DARK, restored.themeOverride.value)
        assertEquals("RU", settings.getString(SettingKeys.LocaleOverride, ""))
        assertEquals("DARK", settings.getString(SettingKeys.ThemeOverride, ""))
    }

    @Test
    fun onlyConfirmedImpressionResetsSinceCounter() = runTest {
        val store = BulbMatchSettingsStore(MapSettings())

        store.recordCompatibleMatch()
        store.recordCompatibleMatch()
        store.recordCompatibleMatch()
        val afterImpression = store.recordInterstitialImpression(123_456L)

        assertEquals(3, afterImpression.completedCompatibleMatches)
        assertEquals(0, afterImpression.compatibleMatchesSinceInterstitial)
        assertEquals(123_456L, afterImpression.lastInterstitialEpochMs)
    }

    @Test
    fun adResetPreservesLocaleAndTheme() = runTest {
        val settings = MapSettings()
        val store = BulbMatchSettingsStore(settings)
        store.setLocaleOverride(StoredLocaleOverride.EN)
        store.setThemeOverride(StoredThemeOverride.LIGHT)
        store.recordCompatibleMatch()
        store.recordInterstitialImpression(500L)

        store.resetAdFrequency()

        assertEquals(StoredLocaleOverride.EN, store.localeOverride.value)
        assertEquals(StoredThemeOverride.LIGHT, store.themeOverride.value)
        assertEquals(AdFrequencyState(0, null, 0), store.adFrequencyState.value)
        assertEquals("EN", settings.getString(SettingKeys.LocaleOverride, ""))
        assertEquals("LIGHT", settings.getString(SettingKeys.ThemeOverride, ""))
        assertNull(settings.getLongOrNull(SettingKeys.LastInterstitialEpochMs))
    }
}
