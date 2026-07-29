package com.sedsoftware.bulbmatch.data

import com.sedsoftware.bulbmatch.data.catalog.BundledCatalogEntry
import com.sedsoftware.bulbmatch.data.catalog.BundledCatalogLoader
import com.sedsoftware.bulbmatch.data.catalog.CatalogLoadResult
import com.sedsoftware.bulbmatch.data.catalog.CatalogValidationMode
import com.sedsoftware.bulbmatch.data.catalog.normalizeSearchTerm
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshot
import com.sedsoftware.bulbmatch.data.history.SavedMatchLookup
import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore
import com.sedsoftware.bulbmatch.data.history.toPersistedWrite
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.data.settings.StoredLocaleOverride
import com.sedsoftware.bulbmatch.data.settings.StoredThemeOverride
import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogBundle
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.CatalogProvider
import com.sedsoftware.bulbmatch.domain.CatalogSnapshot
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.CreatedAtEpochMillis
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.RepositoryFailure
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.SavedMatchRepository
import com.sedsoftware.bulbmatch.domain.SavedMatchSummary
import com.sedsoftware.bulbmatch.domain.SettingsRepository
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import com.sedsoftware.bulbmatch.domain.VoltageFamilyRule
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class DefaultSavedMatchRepository(
    private val store: SqlDelightSavedMatchStore<SavedAssessmentSnapshot>,
) : SavedMatchRepository {
    override fun observeSummaries(): Flow<List<SavedMatchSummary>> =
        store.observeSummaries().map { rows ->
            rows.map { row ->
                SavedMatchSummary(
                    id = requireNotNull(SavedMatchId.from(row.id)),
                    displayName = row.displayName,
                    createdAt = requireNotNull(CreatedAtEpochMillis.from(row.createdAtEpochMs)),
                    outcome = row.statusCode.toAssessmentOutcome(),
                    baseCode = row.baseCode?.let(BaseCode::from),
                    rawBaseText = row.rawBaseText,
                    catalogVersion = row.catalogVersion,
                    rulesetVersion = row.rulesetVersion,
                    available = true,
                )
            }
        }

    override suspend fun get(id: SavedMatchId): RepositoryResult<SavedMatch?> =
        readBoundary {
            when (val lookup = store.get(id.value)) {
                is SavedMatchLookup.Available -> {
                    val persisted = lookup.match
                    val snapshot = persisted.snapshot
                    persisted.summary.toDomain(snapshot)
                }
                is SavedMatchLookup.Unavailable -> throw UnavailableSnapshotException()
                SavedMatchLookup.Missing -> null
            }
        }

    override suspend fun save(match: SavedMatch): RepositoryResult<Unit> =
        writeBoundary {
            store.save(
                SavedAssessmentSnapshot(
                    confirmedInput = match.confirmedInput,
                    assessment = match.assessment,
                ).toPersistedWrite(
                    id = match.id.value,
                    createdAtEpochMs = match.createdAt.value,
                    displayName = match.displayName,
                    catalogVersion = match.catalogVersion,
                    rulesetVersion = match.rulesetVersion,
                ),
            )
        }

    override suspend fun delete(id: SavedMatchId): RepositoryResult<Unit> =
        writeBoundary { store.delete(id.value) }

    override suspend fun clearAll(): RepositoryResult<Unit> =
        writeBoundary { store.clearAll() }
}

class DefaultSettingsRepository(
    private val store: BulbMatchSettingsStore,
) : SettingsRepository {
    override val localeOverride: StateFlow<LocaleOverride> =
        store.localeOverride.mapState(StoredLocaleOverride::toDomain)
    override val themeOverride: StateFlow<ThemeOverride> =
        store.themeOverride.mapState(StoredThemeOverride::toDomain)
    override val adFrequencyState: StateFlow<AdFrequencyState> =
        store.adFrequencyState.mapState {
            AdFrequencyState(
                completedCompatibleMatches = it.completedCompatibleMatches,
                lastInterstitialEpochMs = it.lastInterstitialEpochMs,
                compatibleMatchesSinceInterstitial = it.compatibleMatchesSinceInterstitial,
            )
        }

    override suspend fun setLocaleOverride(
        value: LocaleOverride,
    ): RepositoryResult<Unit> = writeBoundary {
        store.setLocaleOverride(value.toStored())
    }

    override suspend fun setThemeOverride(
        value: ThemeOverride,
    ): RepositoryResult<Unit> = writeBoundary {
        store.setThemeOverride(value.toStored())
    }

    override suspend fun recordCompatibleMatch(): RepositoryResult<AdFrequencyState> =
        writeBoundary {
            store.recordCompatibleMatch().let {
                AdFrequencyState(
                    completedCompatibleMatches = it.completedCompatibleMatches,
                    lastInterstitialEpochMs = it.lastInterstitialEpochMs,
                    compatibleMatchesSinceInterstitial = it.compatibleMatchesSinceInterstitial,
                )
            }
        }

    override suspend fun recordInterstitialImpression(
        epochMs: Long,
    ): RepositoryResult<AdFrequencyState> = writeBoundary {
        store.recordInterstitialImpression(epochMs).let {
            AdFrequencyState(
                completedCompatibleMatches = it.completedCompatibleMatches,
                lastInterstitialEpochMs = it.lastInterstitialEpochMs,
                compatibleMatchesSinceInterstitial = it.compatibleMatchesSinceInterstitial,
            )
        }
    }

    override suspend fun resetAdFrequency(): RepositoryResult<Unit> =
        writeBoundary { store.resetAdFrequency() }
}

class DefaultCatalogProvider(
    utf8Catalog: ByteArray,
    mode: CatalogValidationMode,
    voltageRules: List<VoltageFamilyRule>,
    targetVoltage: VoltageMarking,
    targetFrequency: FrequencyMarking,
    loader: BundledCatalogLoader = BundledCatalogLoader(),
) : CatalogProvider {
    private val entries: List<CatalogEntry>
    override val availability: StateFlow<CatalogAvailability>

    init {
        val loaded = loader.load(utf8Catalog, mode)
        val initial = when (loaded) {
            is CatalogLoadResult.Invalid -> {
                entries = emptyList()
                val inspectable = (loader.load(
                    utf8Catalog,
                    CatalogValidationMode.Development,
                ) as? CatalogLoadResult.Valid)?.catalog
                CatalogAvailability.Invalid(
                    reasonCode = loaded.failure::class.simpleName ?: "CatalogInvalid",
                    catalogVersion = inspectable?.catalogVersion,
                    rulesetVersion = inspectable?.rulesetVersion,
                    humanApprovalPending =
                        inspectable?.release?.requiredReviewer == "Sergey V." &&
                            inspectable.release.decision == "PENDING",
                )
            }
            is CatalogLoadResult.Valid -> {
                val document = loaded.catalog
                entries = document.entries.mapNotNull(BundledCatalogEntry::toDomain)
                if (voltageRules.isEmpty()) {
                    CatalogAvailability.Invalid("ReviewedVoltageRulesMissing")
                } else {
                    CatalogAvailability.Available(
                        CatalogBundle(
                            snapshot = CatalogSnapshot(
                                catalogVersion = document.catalogVersion,
                                rulesetVersion = document.rulesetVersion,
                                enabledBaseCodes = entries
                                    .filter(CatalogEntry::enabledForAssessment)
                                    .mapTo(linkedSetOf(), CatalogEntry::code),
                                voltageRules = voltageRules,
                                targetVoltage = targetVoltage,
                                targetFrequency = targetFrequency,
                            ),
                            entries = entries,
                            publishedAt = document.publishedAt,
                            schemaVersion = document.schemaVersion,
                            sourceManifestVersion = document.sourceManifestVersion,
                            contentHash = document.contentHash,
                        ),
                    )
                }
            }
        }
        availability = kotlinx.coroutines.flow.MutableStateFlow(initial)
    }

    override fun searchEntries(query: String): List<CatalogEntry> {
        val normalized = normalizeSearchTerm(query)
        if (normalized.isEmpty()) return entries
        return entries.filter { entry ->
            sequenceOf(entry.code.value, entry.commonNameEn, entry.commonNameRu)
                .plus(entry.aliasesEn.asSequence())
                .plus(entry.aliasesRu.asSequence())
                .any { normalizeSearchTerm(it).contains(normalized) }
        }
    }

    override fun entry(code: BaseCode): CatalogEntry? =
        entries.firstOrNull { it.code == code }
}

private fun BundledCatalogEntry.toDomain(): CatalogEntry? {
    val code = BaseCode.from(canonicalCode) ?: return null
    return CatalogEntry(
        code = code,
        commonNameEn = commonNameEn,
        commonNameRu = commonNameRu,
        aliasesEn = aliasesEn,
        aliasesRu = aliasesRu,
        diagramId = diagramId,
        distinguishingHintEn = distinguishingHintEn,
        distinguishingHintRu = distinguishingHintRu,
        enabledForAssessment = enabledForAssessment,
    )
}

private fun com.sedsoftware.bulbmatch.data.history.PersistedSavedMatchSummary.toDomain(
    snapshot: SavedAssessmentSnapshot,
): SavedMatch = SavedMatch(
    id = requireNotNull(SavedMatchId.from(id)),
    displayName = displayName,
    createdAt = requireNotNull(CreatedAtEpochMillis.from(createdAtEpochMs)),
    confirmedInput = snapshot.confirmedInput,
    assessment = snapshot.assessment,
    catalogVersion = catalogVersion,
    rulesetVersion = rulesetVersion,
    snapshotSchemaVersion = snapshotSchemaVersion,
)

private fun String.toAssessmentOutcome(): AssessmentOutcome = when (this) {
    "COMPATIBLE" -> AssessmentOutcome.Compatible
    "NEED_CLARIFICATION" -> AssessmentOutcome.NeedClarification
    "POTENTIAL_CONFLICT" -> AssessmentOutcome.PotentialConflict
    else -> error("Unknown stored assessment outcome.")
}

private fun StoredLocaleOverride.toDomain(): LocaleOverride = when (this) {
    StoredLocaleOverride.SYSTEM -> LocaleOverride.System
    StoredLocaleOverride.EN -> LocaleOverride.English
    StoredLocaleOverride.RU -> LocaleOverride.Russian
}

private fun LocaleOverride.toStored(): StoredLocaleOverride = when (this) {
    LocaleOverride.System -> StoredLocaleOverride.SYSTEM
    LocaleOverride.English -> StoredLocaleOverride.EN
    LocaleOverride.Russian -> StoredLocaleOverride.RU
}

private fun StoredThemeOverride.toDomain(): ThemeOverride = when (this) {
    StoredThemeOverride.SYSTEM -> ThemeOverride.System
    StoredThemeOverride.LIGHT -> ThemeOverride.Light
    StoredThemeOverride.DARK -> ThemeOverride.Dark
}

private fun ThemeOverride.toStored(): StoredThemeOverride = when (this) {
    ThemeOverride.System -> StoredThemeOverride.SYSTEM
    ThemeOverride.Light -> StoredThemeOverride.LIGHT
    ThemeOverride.Dark -> StoredThemeOverride.DARK
}

private suspend inline fun <T> readBoundary(
    block: () -> T,
): RepositoryResult<T> = try {
    RepositoryResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    RepositoryResult.Failure(RepositoryFailure.ReadFailed(failure))
}

private suspend inline fun <T> writeBoundary(
    block: () -> T,
): RepositoryResult<T> = try {
    RepositoryResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    RepositoryResult.Failure(RepositoryFailure.WriteFailed(failure))
}

private class UnavailableSnapshotException : Exception()

private fun <T, R> StateFlow<T>.mapState(
    transform: (T) -> R,
): StateFlow<R> = MappedStateFlow(this, transform)

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val replayCache: List<R>
        get() = source.replayCache.map(transform)
    override val value: R
        get() = transform(source.value)

    override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<R>): Nothing {
        source.collect { collector.emit(transform(it)) }
    }
}
