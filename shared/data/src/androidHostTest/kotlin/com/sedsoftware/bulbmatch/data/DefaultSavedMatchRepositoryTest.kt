package com.sedsoftware.bulbmatch.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabase
import com.sedsoftware.bulbmatch.data.history.SavedAssessmentSnapshotCodec
import com.sedsoftware.bulbmatch.data.history.SqlDelightSavedMatchStore
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome
import com.sedsoftware.bulbmatch.domain.ClarificationReason
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.CreatedAtEpochMillis
import com.sedsoftware.bulbmatch.domain.RepositoryFailure
import com.sedsoftware.bulbmatch.domain.RepositoryResult
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultSavedMatchRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: BulbMatchDatabase
    private lateinit var repository: DefaultSavedMatchRepository

    @BeforeTest
    fun createRepository() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BulbMatchDatabase.Schema.create(driver)
        database = BulbMatchDatabase(driver)
        repository = DefaultSavedMatchRepository(
            SqlDelightSavedMatchStore(
                database = database,
                snapshotCodec = SavedAssessmentSnapshotCodec,
                ioDispatcher = Dispatchers.Default,
            ),
        )
    }

    @AfterTest
    fun closeDriver() {
        driver.close()
    }

    @Test
    fun saveObserveReadDeleteAndClearPreserveImmutableSnapshot() = runTest {
        val saved = savedMatch("saved-1", createdAt = 100L)

        assertIs<RepositoryResult.Success<Unit>>(repository.save(saved))
        val summary = repository.observeSummaries().first().single()
        assertEquals(saved.id, summary.id)
        assertEquals(saved.displayName, summary.displayName)
        assertEquals(AssessmentOutcome.NeedClarification, summary.outcome)
        assertEquals("custom cap", summary.rawBaseText)
        assertEquals(saved, assertIs<RepositoryResult.Success<SavedMatch?>>(repository.get(saved.id)).value)

        assertIs<RepositoryResult.Success<Unit>>(repository.delete(saved.id))
        assertNull(assertIs<RepositoryResult.Success<SavedMatch?>>(repository.get(saved.id)).value)

        assertIs<RepositoryResult.Success<Unit>>(repository.save(savedMatch("saved-2", 200L)))
        assertIs<RepositoryResult.Success<Unit>>(repository.clearAll())
        assertEquals(emptyList(), repository.observeSummaries().first())
    }

    @Test
    fun summaryMapsAllStableOutcomeCodes() = runTest {
        insertSummary("compatible", "COMPATIBLE", 300L)
        insertSummary("clarification", "NEED_CLARIFICATION", 200L)
        insertSummary("conflict", "POTENTIAL_CONFLICT", 100L)

        assertEquals(
            listOf(
                AssessmentOutcome.Compatible,
                AssessmentOutcome.NeedClarification,
                AssessmentOutcome.PotentialConflict,
            ),
            repository.observeSummaries().first().map { it.outcome },
        )
    }

    @Test
    fun unavailableSnapshotAndImmutableIdConflictUseTypedBoundaries() = runTest {
        insertSummary("malformed", "COMPATIBLE", 100L, snapshotJson = "not-json")

        val readFailure = assertIs<RepositoryResult.Failure>(
            repository.get(savedMatchId("malformed")),
        )
        assertIs<RepositoryFailure.ReadFailed>(readFailure.reason)

        val original = savedMatch("fixed", 200L)
        assertIs<RepositoryResult.Success<Unit>>(repository.save(original))
        val conflict = original.copy(createdAt = createdAt(201L))
        val writeFailure = assertIs<RepositoryResult.Failure>(repository.save(conflict))
        assertIs<RepositoryFailure.WriteFailed>(writeFailure.reason)
        assertEquals(
            original,
            assertIs<RepositoryResult.Success<SavedMatch?>>(repository.get(original.id)).value,
        )
    }

    private fun savedMatch(
        id: String,
        createdAt: Long,
    ): SavedMatch {
        val input = ConfirmedMatchInput(
            base = requireNotNull(ConfirmedBase.Unknown.from("custom cap")),
            voltage = VoltageEvidence.Missing,
        )
        return SavedMatch(
            id = savedMatchId(id),
            displayName = "Kitchen",
            createdAt = createdAt(createdAt),
            confirmedInput = input,
            assessment = Assessment.NeedClarification(
                reasons = listOf(ClarificationReason.UnknownBase("custom cap")),
                retainedConfirmedInput = input,
            ),
            catalogVersion = "catalog-test",
            rulesetVersion = "rules-test",
            snapshotSchemaVersion = SavedAssessmentSnapshotCodec.currentSchemaVersion,
        )
    }

    private fun insertSummary(
        id: String,
        outcome: String,
        createdAt: Long,
        snapshotJson: String = "{}",
    ) {
        database.savedMatchQueries.insertMatch(
            id = id,
            created_at_epoch_ms = createdAt,
            display_name = null,
            status_code = outcome,
            base_code = "E27",
            raw_base_text = null,
            voltage_min_v = 220.0,
            voltage_max_v = 240.0,
            catalog_version = "catalog-test",
            ruleset_version = "rules-test",
            snapshot_schema_version = SavedAssessmentSnapshotCodec.currentSchemaVersion.toLong(),
            snapshot_json = snapshotJson,
        )
    }

    private fun savedMatchId(value: String): SavedMatchId =
        requireNotNull(SavedMatchId.from(value))

    private fun createdAt(value: Long): CreatedAtEpochMillis =
        requireNotNull(CreatedAtEpochMillis.from(value))
}
