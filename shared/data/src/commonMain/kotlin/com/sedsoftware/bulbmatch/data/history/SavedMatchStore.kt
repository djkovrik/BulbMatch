package com.sedsoftware.bulbmatch.data.history

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

const val SAVED_MATCH_SNAPSHOT_SCHEMA_VERSION = 1

data class PersistedSavedMatchSummary(
    val id: String,
    val createdAtEpochMs: Long,
    val displayName: String?,
    val statusCode: String,
    val baseCode: String?,
    val rawBaseText: String?,
    val voltageMinV: Double?,
    val voltageMaxV: Double?,
    val catalogVersion: String,
    val rulesetVersion: String,
    val snapshotSchemaVersion: Int,
)

data class SavedMatchWrite<T : Any>(
    val summary: PersistedSavedMatchSummary,
    val snapshot: T,
)

data class PersistedSavedMatch<T : Any>(
    val summary: PersistedSavedMatchSummary,
    val snapshot: T,
)

sealed interface SavedMatchLookup<out T : Any> {
    data class Available<T : Any>(
        val match: PersistedSavedMatch<T>,
    ) : SavedMatchLookup<T>

    data class Unavailable(
        val summary: PersistedSavedMatchSummary,
        val failure: SnapshotDecodeFailure,
    ) : SavedMatchLookup<Nothing>

    data object Missing : SavedMatchLookup<Nothing>
}

sealed interface SnapshotDecodeResult<out T : Any> {
    data class Success<T : Any>(val value: T) : SnapshotDecodeResult<T>
    data class Failure(val reason: SnapshotDecodeFailure) : SnapshotDecodeResult<Nothing>
}

sealed interface SnapshotDecodeFailure {
    data class UnsupportedSchema(val schemaVersion: Int) : SnapshotDecodeFailure
    data object MalformedRequiredFields : SnapshotDecodeFailure
}

interface SnapshotCodec<T : Any> {
    val currentSchemaVersion: Int

    fun encode(value: T): String

    fun decode(
        schemaVersion: Int,
        json: String,
    ): SnapshotDecodeResult<T>
}

class SqlDelightSavedMatchStore<T : Any>(
    private val database: BulbMatchDatabase,
    private val snapshotCodec: SnapshotCodec<T>,
    private val ioDispatcher: CoroutineDispatcher,
) {
    init {
        require(snapshotCodec.currentSchemaVersion == SAVED_MATCH_SNAPSHOT_SCHEMA_VERSION) {
            "MVP supports snapshot schema 1 only."
        }
    }

    fun observeSummaries(): Flow<List<PersistedSavedMatchSummary>> =
        database.savedMatchQueries
            .selectSummaries(::mapSummary)
            .asFlow()
            .mapToList(ioDispatcher)

    suspend fun get(id: String): SavedMatchLookup<T> = withContext(ioDispatcher) {
        val row = database.savedMatchQueries
            .selectById(id, ::mapRow)
            .executeAsOneOrNull()
            ?: return@withContext SavedMatchLookup.Missing

        val decoded = try {
            snapshotCodec.decode(
                schemaVersion = row.summary.snapshotSchemaVersion,
                json = row.snapshotJson,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            SnapshotDecodeResult.Failure(SnapshotDecodeFailure.MalformedRequiredFields)
        }
        when (decoded) {
            is SnapshotDecodeResult.Success -> SavedMatchLookup.Available(
                PersistedSavedMatch(
                    summary = row.summary,
                    snapshot = decoded.value,
                ),
            )
            is SnapshotDecodeResult.Failure -> SavedMatchLookup.Unavailable(
                summary = row.summary,
                failure = decoded.reason,
            )
        }
    }

    suspend fun save(match: SavedMatchWrite<T>) = withContext(ioDispatcher) {
        val normalizedSummary = match.summary.copy(
            id = match.summary.id.requireNotBlank("id"),
            displayName = normalizeDisplayName(match.summary.displayName),
            statusCode = match.summary.statusCode.requireNotBlank("statusCode"),
            baseCode = match.summary.baseCode?.requireNotBlank("baseCode"),
            rawBaseText = normalizeRawBaseText(match.summary.rawBaseText),
            catalogVersion = match.summary.catalogVersion.requireNotBlank("catalogVersion"),
            rulesetVersion = match.summary.rulesetVersion.requireNotBlank("rulesetVersion"),
        )
        require(normalizedSummary.statusCode in SAVED_STATUS_CODES) {
            "statusCode is not a supported stable assessment code."
        }
        require(
            normalizedSummary.baseCode == null || normalizedSummary.rawBaseText == null,
        ) {
            "A summary cannot contain both a canonical and an unknown raw base."
        }
        normalizedSummary.baseCode?.let { baseCode ->
            require(baseCode.length <= 32 && !baseCode.any(Char::isWhitespace)) {
                "baseCode must be a canonical code."
            }
        }
        require(normalizedSummary.createdAtEpochMs >= 0) {
            "createdAtEpochMs must not be negative."
        }
        require(
            normalizedSummary.snapshotSchemaVersion == snapshotCodec.currentSchemaVersion,
        ) {
            "Snapshot schema does not match the codec."
        }
        requireValidVoltageRange(
            normalizedSummary.voltageMinV,
            normalizedSummary.voltageMaxV,
        )
        val snapshotJson = snapshotCodec.encode(match.snapshot)
        require(snapshotJson.isNotBlank()) { "Snapshot JSON must not be blank." }

        database.transaction {
            database.savedMatchQueries.insertMatch(
                id = normalizedSummary.id,
                created_at_epoch_ms = normalizedSummary.createdAtEpochMs,
                display_name = normalizedSummary.displayName,
                status_code = normalizedSummary.statusCode,
                base_code = normalizedSummary.baseCode,
                raw_base_text = normalizedSummary.rawBaseText,
                voltage_min_v = normalizedSummary.voltageMinV,
                voltage_max_v = normalizedSummary.voltageMaxV,
                catalog_version = normalizedSummary.catalogVersion,
                ruleset_version = normalizedSummary.rulesetVersion,
                snapshot_schema_version = normalizedSummary.snapshotSchemaVersion.toLong(),
                snapshot_json = snapshotJson,
            )
            val stored = database.savedMatchQueries
                .selectById(normalizedSummary.id, ::mapRow)
                .executeAsOne()
            check(stored == StoredRow(normalizedSummary, snapshotJson)) {
                "A saved match ID cannot be reused for different immutable content."
            }
        }
    }

    suspend fun delete(id: String) = withContext(ioDispatcher) {
        database.transaction {
            database.savedMatchQueries.deleteById(id)
        }
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        database.transaction {
            database.savedMatchQueries.clearAll()
        }
    }

    private data class StoredRow(
        val summary: PersistedSavedMatchSummary,
        val snapshotJson: String,
    )

    private fun mapSummary(
        id: String,
        createdAtEpochMs: Long,
        displayName: String?,
        statusCode: String,
        baseCode: String?,
        rawBaseText: String?,
        voltageMinV: Double?,
        voltageMaxV: Double?,
        catalogVersion: String,
        rulesetVersion: String,
        snapshotSchemaVersion: Long,
    ): PersistedSavedMatchSummary = PersistedSavedMatchSummary(
        id = id,
        createdAtEpochMs = createdAtEpochMs,
        displayName = displayName,
        statusCode = statusCode,
        baseCode = baseCode,
        rawBaseText = rawBaseText,
        voltageMinV = voltageMinV,
        voltageMaxV = voltageMaxV,
        catalogVersion = catalogVersion,
        rulesetVersion = rulesetVersion,
        snapshotSchemaVersion = snapshotSchemaVersion.toInt(),
    )

    private fun mapRow(
        id: String,
        createdAtEpochMs: Long,
        displayName: String?,
        statusCode: String,
        baseCode: String?,
        rawBaseText: String?,
        voltageMinV: Double?,
        voltageMaxV: Double?,
        catalogVersion: String,
        rulesetVersion: String,
        snapshotSchemaVersion: Long,
        snapshotJson: String,
    ): StoredRow = StoredRow(
        summary = mapSummary(
            id = id,
            createdAtEpochMs = createdAtEpochMs,
            displayName = displayName,
            statusCode = statusCode,
            baseCode = baseCode,
            rawBaseText = rawBaseText,
            voltageMinV = voltageMinV,
            voltageMaxV = voltageMaxV,
            catalogVersion = catalogVersion,
            rulesetVersion = rulesetVersion,
            snapshotSchemaVersion = snapshotSchemaVersion,
        ),
        snapshotJson = snapshotJson,
    )
}

private fun normalizeDisplayName(value: String?): String? {
    val normalized = value?.trim()?.takeUnless(String::isEmpty) ?: return null
    require(!normalized.any(Char::isISOControl)) {
        "displayName must not contain control characters."
    }
    require(normalized.unicodeCodePointCount() <= 80) {
        "displayName must contain at most 80 Unicode code points."
    }
    return normalized
}

private fun normalizeRawBaseText(value: String?): String? {
    val normalized = value?.trim()?.takeUnless(String::isEmpty) ?: return null
    require(!normalized.any(Char::isISOControl)) {
        "rawBaseText must not contain control characters."
    }
    require(normalized.unicodeCodePointCount() <= 80) {
        "rawBaseText must contain at most 80 Unicode code points."
    }
    return normalized
}

private fun String.unicodeCodePointCount(): Int {
    var result = 0
    var index = 0
    while (index < length) {
        val current = this[index]
        if (current.isHighSurrogate()) {
            require(index + 1 < length && this[index + 1].isLowSurrogate()) {
                "Text must not contain an unpaired surrogate."
            }
            index += 2
        } else {
            require(!current.isLowSurrogate()) {
                "Text must not contain an unpaired surrogate."
            }
            index += 1
        }
        result += 1
    }
    return result
}

private fun requireValidVoltageRange(
    minimum: Double?,
    maximum: Double?,
) {
    require((minimum == null) == (maximum == null)) {
        "Voltage summary must contain both range bounds or neither."
    }
    if (minimum != null && maximum != null) {
        require(minimum.isFinite() && maximum.isFinite()) {
            "Voltage summary must be finite."
        }
        require(minimum > 0.0 && maximum >= minimum) {
            "Voltage summary must be a positive inclusive range."
        }
    }
}

private fun String.requireNotBlank(field: String): String = also {
    require(it.isNotBlank()) { "$field must not be blank." }
}

private val SAVED_STATUS_CODES = setOf(
    "COMPATIBLE",
    "NEED_CLARIFICATION",
    "POTENTIAL_CONFLICT",
)
