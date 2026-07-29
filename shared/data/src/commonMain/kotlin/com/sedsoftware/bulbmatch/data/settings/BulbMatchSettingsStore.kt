package com.sedsoftware.bulbmatch.data.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class StoredLocaleOverride {
    SYSTEM,
    EN,
    RU,
}

enum class StoredThemeOverride {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AdFrequencyState(
    val completedCompatibleMatches: Int,
    val lastInterstitialEpochMs: Long?,
    val compatibleMatchesSinceInterstitial: Int,
)

class BulbMatchSettingsStore(
    private val settings: Settings,
) {
    private val writeMutex = Mutex()
    private val mutableLocaleOverride = MutableStateFlow(readLocaleOverride())
    private val mutableThemeOverride = MutableStateFlow(readThemeOverride())
    private val mutableAdFrequencyState = MutableStateFlow(readAdFrequencyState())

    val localeOverride: StateFlow<StoredLocaleOverride> = mutableLocaleOverride.asStateFlow()
    val themeOverride: StateFlow<StoredThemeOverride> = mutableThemeOverride.asStateFlow()
    val adFrequencyState: StateFlow<AdFrequencyState> =
        mutableAdFrequencyState.asStateFlow()

    suspend fun setLocaleOverride(value: StoredLocaleOverride) {
        writeMutex.withLock {
            settings.putString(SettingKeys.LocaleOverride, value.name)
            mutableLocaleOverride.value = value
        }
    }

    suspend fun setThemeOverride(value: StoredThemeOverride) {
        writeMutex.withLock {
            settings.putString(SettingKeys.ThemeOverride, value.name)
            mutableThemeOverride.value = value
        }
    }

    suspend fun recordCompatibleMatch(): AdFrequencyState = writeMutex.withLock {
        val current = readAdFrequencyState()
        val updated = current.copy(
            completedCompatibleMatches = current.completedCompatibleMatches.incrementSafely(),
            compatibleMatchesSinceInterstitial =
                current.compatibleMatchesSinceInterstitial.incrementSafely(),
        )
        writeAdFrequencyState(updated)
        updated
    }

    suspend fun recordInterstitialImpression(epochMs: Long): AdFrequencyState =
        writeMutex.withLock {
            require(epochMs >= 0L) { "Interstitial impression time must not be negative." }
            val current = readAdFrequencyState()
            val updated = current.copy(
                lastInterstitialEpochMs = epochMs,
                compatibleMatchesSinceInterstitial = 0,
            )
            writeAdFrequencyState(updated)
            updated
        }

    suspend fun resetAdFrequency() {
        writeMutex.withLock {
            settings.remove(SettingKeys.CompletedCompatibleMatches)
            settings.remove(SettingKeys.LastInterstitialEpochMs)
            settings.remove(SettingKeys.CompatibleMatchesSinceInterstitial)
            mutableAdFrequencyState.value = DEFAULT_AD_FREQUENCY_STATE
        }
    }

    private fun readLocaleOverride(): StoredLocaleOverride =
        settings.getStringOrNull(SettingKeys.LocaleOverride)
            ?.let { stored -> StoredLocaleOverride.entries.firstOrNull { it.name == stored } }
            ?: StoredLocaleOverride.SYSTEM

    private fun readThemeOverride(): StoredThemeOverride =
        settings.getStringOrNull(SettingKeys.ThemeOverride)
            ?.let { stored -> StoredThemeOverride.entries.firstOrNull { it.name == stored } }
            ?: StoredThemeOverride.SYSTEM

    private fun readAdFrequencyState(): AdFrequencyState = AdFrequencyState(
        completedCompatibleMatches = settings
            .getInt(SettingKeys.CompletedCompatibleMatches, 0)
            .coerceAtLeast(0),
        lastInterstitialEpochMs = settings
            .getLongOrNull(SettingKeys.LastInterstitialEpochMs)
            ?.takeIf { it >= 0L },
        compatibleMatchesSinceInterstitial = settings
            .getInt(SettingKeys.CompatibleMatchesSinceInterstitial, 0)
            .coerceAtLeast(0),
    )

    private fun writeAdFrequencyState(value: AdFrequencyState) {
        settings.putInt(
            SettingKeys.CompletedCompatibleMatches,
            value.completedCompatibleMatches,
        )
        value.lastInterstitialEpochMs?.let {
            settings.putLong(SettingKeys.LastInterstitialEpochMs, it)
        } ?: settings.remove(SettingKeys.LastInterstitialEpochMs)
        settings.putInt(
            SettingKeys.CompatibleMatchesSinceInterstitial,
            value.compatibleMatchesSinceInterstitial,
        )
        mutableAdFrequencyState.value = value
    }
}

internal object SettingKeys {
    const val LocaleOverride = "locale_override_v1"
    const val ThemeOverride = "theme_override_v1"
    const val CompletedCompatibleMatches = "completed_compatible_matches_v1"
    const val LastInterstitialEpochMs = "last_interstitial_epoch_ms_v1"
    const val CompatibleMatchesSinceInterstitial =
        "compatible_matches_since_interstitial_v1"
}

private val DEFAULT_AD_FREQUENCY_STATE = AdFrequencyState(
    completedCompatibleMatches = 0,
    lastInterstitialEpochMs = null,
    compatibleMatchesSinceInterstitial = 0,
)

private fun Int.incrementSafely(): Int =
    if (this == Int.MAX_VALUE) Int.MAX_VALUE else this + 1

