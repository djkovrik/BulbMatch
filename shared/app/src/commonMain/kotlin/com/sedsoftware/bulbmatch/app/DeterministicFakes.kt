package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SavedMatchSummary
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeEphemeralImage(
    override val debugLabel: String = "fake-ephemeral-image",
) : EphemeralImage {
    var released: Boolean = false
        private set

    override fun release() {
        released = true
    }
}

class RecordingImageActions : ImageActions {
    val calls: MutableList<String> = mutableListOf()

    override fun requestCameraPermission() {
        calls += "requestCameraPermission"
    }

    override fun openSystemCameraSettings() {
        calls += "openSystemCameraSettings"
    }

    override fun openPhotoPicker() {
        calls += "openPhotoPicker"
    }

    override fun capturePhoto() {
        calls += "capturePhoto"
    }

    override fun setTorch(enabled: Boolean) {
        calls += "setTorch:$enabled"
    }
}

class FakeRecognitionGateway(
    var result: RecognitionResult,
) : RecognitionGateway {
    var invocationCount: Int = 0
        private set

    override suspend fun recognize(image: EphemeralImage): RecognitionResult {
        invocationCount += 1
        return result
    }
}

class FakeInterstitialGateway(
    var nextImpressionRecorded: Boolean = false,
    var throwOnShow: Boolean = false,
) : InterstitialGateway {
    var showCount: Int = 0
        private set

    override fun showMatchExit(onComplete: (impressionRecorded: Boolean) -> Unit) {
        showCount += 1
        if (throwOnShow) error("Synthetic interstitial failure")
        onComplete(nextImpressionRecorded)
    }
}

class InMemorySavedMatchRepository(
    initialMatches: List<SavedMatch> = emptyList(),
) : SavedMatchRepository {
    private val matches = MutableStateFlow(initialMatches.sortedForHistory())

    override fun observeSummaries(): Flow<List<SavedMatchSummary>> =
        matches.map { current -> current.map(SavedMatch::toSummary) }

    override suspend fun get(id: SavedMatchId): RepositoryResult<SavedMatch?> =
        RepositoryResult.Success(matches.value.firstOrNull { it.id == id })

    override suspend fun save(match: SavedMatch): RepositoryResult<Unit> {
        matches.update { (it + match).sortedForHistory() }
        return RepositoryResult.Success(Unit)
    }

    override suspend fun delete(id: SavedMatchId): RepositoryResult<Unit> {
        matches.update { current -> current.filterNot { it.id == id } }
        return RepositoryResult.Success(Unit)
    }

    override suspend fun clearAll(): RepositoryResult<Unit> {
        matches.value = emptyList()
        return RepositoryResult.Success(Unit)
    }

    fun snapshot(): List<SavedMatch> = matches.value
}

class InMemorySettingsRepository(
    locale: LocaleOverride = LocaleOverride.System,
    theme: ThemeOverride = ThemeOverride.System,
    frequency: AdFrequencyState = AdFrequencyState(0, null, 0),
) : SettingsRepository {
    private val mutableLocale = MutableStateFlow(locale)
    private val mutableTheme = MutableStateFlow(theme)
    private val mutableFrequency = MutableStateFlow(frequency)

    override val localeOverride: StateFlow<LocaleOverride> = mutableLocale.asStateFlow()
    override val themeOverride: StateFlow<ThemeOverride> = mutableTheme.asStateFlow()
    override val adFrequencyState: StateFlow<AdFrequencyState> = mutableFrequency.asStateFlow()

    override suspend fun setLocaleOverride(value: LocaleOverride): RepositoryResult<Unit> {
        mutableLocale.value = value
        return RepositoryResult.Success(Unit)
    }

    override suspend fun setThemeOverride(value: ThemeOverride): RepositoryResult<Unit> {
        mutableTheme.value = value
        return RepositoryResult.Success(Unit)
    }

    override suspend fun recordCompatibleMatch(): RepositoryResult<AdFrequencyState> {
        mutableFrequency.update {
            it.copy(
                completedCompatibleMatches = it.completedCompatibleMatches + 1,
                compatibleMatchesSinceInterstitial = it.compatibleMatchesSinceInterstitial + 1,
            )
        }
        return RepositoryResult.Success(mutableFrequency.value)
    }

    override suspend fun recordInterstitialImpression(epochMs: Long): RepositoryResult<AdFrequencyState> {
        mutableFrequency.update {
            it.copy(lastInterstitialEpochMs = epochMs, compatibleMatchesSinceInterstitial = 0)
        }
        return RepositoryResult.Success(mutableFrequency.value)
    }

    override suspend fun resetAdFrequency(): RepositoryResult<Unit> {
        mutableFrequency.value = AdFrequencyState(0, null, 0)
        return RepositoryResult.Success(Unit)
    }
}

class InMemoryCatalogProvider(
    availability: CatalogAvailability,
) : CatalogProvider {
    private val mutableAvailability = MutableStateFlow(availability)
    override val availability: StateFlow<CatalogAvailability> = mutableAvailability.asStateFlow()

    override fun searchEntries(query: String): List<CatalogEntry> {
        val catalog = (mutableAvailability.value as? CatalogAvailability.Available)?.catalog ?: return emptyList()
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return catalog.entries
        return catalog.entries.filter { entry ->
            entry.code.value.lowercase().contains(normalized) ||
                entry.commonNameEn.lowercase().contains(normalized) ||
                entry.commonNameRu.lowercase().contains(normalized) ||
                entry.aliasesEn.any { it.lowercase().contains(normalized) } ||
                entry.aliasesRu.any { it.lowercase().contains(normalized) }
        }
    }

    override fun entry(code: BaseCode): CatalogEntry? =
        (mutableAvailability.value as? CatalogAvailability.Available)
            ?.catalog
            ?.entries
            ?.firstOrNull { it.code == code }

    fun setAvailability(value: CatalogAvailability) {
        mutableAvailability.value = value
    }
}

private fun List<SavedMatch>.sortedForHistory(): List<SavedMatch> =
    sortedWith(compareByDescending<SavedMatch> { it.createdAt.value }.thenByDescending { it.id.value })

private fun SavedMatch.toSummary(): SavedMatchSummary {
    val knownBase = confirmedInput.base as? com.sedsoftware.bulbmatch.domain.ConfirmedBase.Known
    val unknownBase = confirmedInput.base as? com.sedsoftware.bulbmatch.domain.ConfirmedBase.Unknown
    return SavedMatchSummary(
        id = id,
        displayName = displayName,
        createdAt = createdAt,
        outcome = assessment.outcome(),
        baseCode = knownBase?.code,
        rawBaseText = unknownBase?.rawText,
        catalogVersion = catalogVersion,
        rulesetVersion = rulesetVersion,
        available = true,
    )
}
