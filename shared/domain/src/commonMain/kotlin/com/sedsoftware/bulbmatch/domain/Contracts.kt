package com.sedsoftware.bulbmatch.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

@JvmInline
value class SavedMatchId private constructor(val value: String) {
    companion object {
        fun from(value: String): SavedMatchId? =
            value.trim().takeIf(String::isNotEmpty)?.let(::SavedMatchId)
    }
}

@JvmInline
value class CreatedAtEpochMillis private constructor(val value: Long) {
    companion object {
        fun from(value: Long): CreatedAtEpochMillis? =
            value.takeIf { it >= 0L }?.let(::CreatedAtEpochMillis)
    }
}

data class SavedMatch(
    val id: SavedMatchId,
    val displayName: String?,
    val createdAt: CreatedAtEpochMillis,
    val confirmedInput: ConfirmedMatchInput,
    val assessment: Assessment,
    val catalogVersion: String,
    val rulesetVersion: String,
    val snapshotSchemaVersion: Int,
) {
    init {
        require(displayName == displayName?.trim())
        require(displayName == null || displayName.unicodeCodePointCount() in 1..80)
        require(assessment.retainedConfirmedInput == confirmedInput)
        require(catalogVersion.isNotBlank())
        require(rulesetVersion.isNotBlank())
        require(snapshotSchemaVersion > 0)
    }
}

data class SavedMatchSummary(
    val id: SavedMatchId,
    val displayName: String?,
    val createdAt: CreatedAtEpochMillis,
    val outcome: AssessmentOutcome,
    val baseCode: BaseCode?,
    val rawBaseText: String?,
    val catalogVersion: String,
    val rulesetVersion: String,
    val available: Boolean,
)

enum class AssessmentOutcome {
    Compatible,
    NeedClarification,
    PotentialConflict,
}

sealed interface RepositoryFailure {
    val cause: Throwable

    data class ReadFailed(override val cause: Throwable) : RepositoryFailure
    data class WriteFailed(override val cause: Throwable) : RepositoryFailure
}

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(val reason: RepositoryFailure) : RepositoryResult<Nothing>
}

interface SavedMatchRepository {
    fun observeSummaries(): Flow<List<SavedMatchSummary>>

    suspend fun get(id: SavedMatchId): RepositoryResult<SavedMatch?>

    suspend fun save(match: SavedMatch): RepositoryResult<Unit>

    suspend fun delete(id: SavedMatchId): RepositoryResult<Unit>

    suspend fun clearAll(): RepositoryResult<Unit>
}

data class CatalogEntry(
    val code: BaseCode,
    val commonNameEn: String,
    val commonNameRu: String,
    val aliasesEn: List<String>,
    val aliasesRu: List<String>,
    val diagramId: String,
    val distinguishingHintEn: String,
    val distinguishingHintRu: String,
    val enabledForAssessment: Boolean,
)

data class CatalogBundle(
    val snapshot: CatalogSnapshot,
    val entries: List<CatalogEntry>,
    val publishedAt: String,
    val schemaVersion: Int,
    val sourceManifestVersion: String,
    val contentHash: String,
)

sealed interface CatalogAvailability {
    data class Available(val catalog: CatalogBundle) : CatalogAvailability
    data class Invalid(
        val reasonCode: String,
        val catalogVersion: String? = null,
        val rulesetVersion: String? = null,
        val humanApprovalPending: Boolean = false,
    ) : CatalogAvailability
}

interface CatalogProvider {
    val availability: StateFlow<CatalogAvailability>

    fun searchEntries(query: String): List<CatalogEntry>

    fun entry(code: BaseCode): CatalogEntry?
}

enum class LocaleOverride {
    English,
    Russian,
}

enum class ThemeOverride {
    System,
    Light,
    Dark,
}

data class AdFrequencyState(
    val completedCompatibleMatches: Int,
    val lastInterstitialEpochMs: Long?,
    val compatibleMatchesSinceInterstitial: Int,
)

interface SettingsRepository {
    val localeOverride: StateFlow<LocaleOverride>
    val themeOverride: StateFlow<ThemeOverride>
    val adFrequencyState: StateFlow<AdFrequencyState>

    suspend fun setLocaleOverride(value: LocaleOverride): RepositoryResult<Unit>

    suspend fun setThemeOverride(value: ThemeOverride): RepositoryResult<Unit>

    suspend fun recordCompatibleMatch(): RepositoryResult<AdFrequencyState>

    suspend fun recordInterstitialImpression(
        epochMs: Long,
    ): RepositoryResult<AdFrequencyState>

    suspend fun resetAdFrequency(): RepositoryResult<Unit>
}

private fun String.unicodeCodePointCount(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        val current = this[index]
        when {
            current.isHighSurrogate() -> {
                require(index + 1 < length && this[index + 1].isLowSurrogate())
                index += 2
            }
            current.isLowSurrogate() -> error("Unpaired Unicode surrogate.")
            else -> index += 1
        }
        count += 1
    }
    return count
}
