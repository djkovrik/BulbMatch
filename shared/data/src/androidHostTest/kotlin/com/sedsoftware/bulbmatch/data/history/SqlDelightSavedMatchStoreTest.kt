package com.sedsoftware.bulbmatch.data.history

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.russhwolf.settings.MapSettings
import com.sedsoftware.bulbmatch.data.db.BulbMatchDatabase
import com.sedsoftware.bulbmatch.data.settings.BulbMatchSettingsStore
import com.sedsoftware.bulbmatch.data.settings.LocalDataCleaner
import com.sedsoftware.bulbmatch.data.settings.StoredLocaleOverride
import com.sedsoftware.bulbmatch.data.settings.StoredThemeOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs

class SqlDelightSavedMatchStoreTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: BulbMatchDatabase

    @BeforeTest
    fun createRealSchema() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BulbMatchDatabase.Schema.create(driver)
        database = BulbMatchDatabase(driver)
    }

    @AfterTest
    fun closeDriver() {
        driver.close()
    }

    @Test
    fun crudUsesStableNewestFirstOrdering() = runTest {
        val store = store()
        store.save(write(id = "a", createdAt = 100, snapshot = """{"id":"a"}"""))
        store.save(write(id = "b", createdAt = 200, snapshot = """{"id":"b"}"""))
        store.save(write(id = "c", createdAt = 200, snapshot = """{"id":"c"}"""))

        assertEquals(
            listOf("c", "b", "a"),
            store.observeSummaries().first().map(PersistedSavedMatchSummary::id),
        )
        assertEquals(
            """{"id":"b"}""",
            assertIs<SavedMatchLookup.Available<String>>(store.get("b")).match.snapshot,
        )

        store.delete("b")
        assertEquals(listOf("c", "a"), store.observeSummaries().first().map { it.id })

        store.clearAll()
        assertEquals(emptyList(), store.observeSummaries().first())
    }

    @Test
    fun explicitDuplicateSaveUsesAnotherIdWhileRetryIsIdempotent() = runTest {
        val store = store()
        val firstCommand = write(id = "save-command-1", createdAt = 100, snapshot = "{}")

        store.save(firstCommand)
        store.save(firstCommand)
        store.save(write(id = "save-command-2", createdAt = 101, snapshot = "{}"))

        assertEquals(2L, database.savedMatchQueries.countAll().executeAsOne())
    }

    @Test
    fun immutableIdConflictRollsBackWithoutChangingStoredSnapshot() = runTest {
        val store = store()
        store.save(write(id = "fixed-id", createdAt = 100, snapshot = """{"version":1}"""))

        assertFails {
            store.save(write(id = "fixed-id", createdAt = 200, snapshot = """{"version":2}"""))
        }

        val saved = assertIs<SavedMatchLookup.Available<String>>(store.get("fixed-id"))
        assertEquals("""{"version":1}""", saved.match.snapshot)
        assertEquals(100, saved.match.summary.createdAtEpochMs)
        assertEquals(1L, database.savedMatchQueries.countAll().executeAsOne())
    }

    @Test
    fun malformedAndUnsupportedSnapshotsAreIsolatedPerRecord() = runTest {
        val store = store()
        store.save(write(id = "valid", createdAt = 100, snapshot = """{"ok":true}"""))
        database.savedMatchQueries.insertMatch(
            id = "malformed",
            created_at_epoch_ms = 200,
            display_name = null,
            status_code = "COMPATIBLE",
            base_code = "E27",
            raw_base_text = null,
            voltage_min_v = 220.0,
            voltage_max_v = 240.0,
            catalog_version = "catalog-1",
            ruleset_version = "ruleset-1",
            snapshot_schema_version = 1,
            snapshot_json = "malformed",
        )
        database.savedMatchQueries.insertMatch(
            id = "future",
            created_at_epoch_ms = 300,
            display_name = null,
            status_code = "COMPATIBLE",
            base_code = "E27",
            raw_base_text = null,
            voltage_min_v = 220.0,
            voltage_max_v = 240.0,
            catalog_version = "catalog-1",
            ruleset_version = "ruleset-1",
            snapshot_schema_version = 2,
            snapshot_json = "{}",
        )

        assertIs<SavedMatchLookup.Available<String>>(store.get("valid"))
        assertEquals(
            SnapshotDecodeFailure.MalformedRequiredFields,
            assertIs<SavedMatchLookup.Unavailable>(store.get("malformed")).failure,
        )
        assertEquals(
            SnapshotDecodeFailure.UnsupportedSchema(2),
            assertIs<SavedMatchLookup.Unavailable>(store.get("future")).failure,
        )
        assertEquals(listOf("future", "malformed", "valid"), store.observeSummaries().first().map { it.id })
    }

    @Test
    fun realDriverRollsBackFailedTransaction() {
        assertFails {
            database.transaction {
                database.savedMatchQueries.insertMatch(
                    id = "rolled-back",
                    created_at_epoch_ms = 100,
                    display_name = null,
                    status_code = "COMPATIBLE",
                    base_code = "E27",
                    raw_base_text = null,
                    voltage_min_v = 220.0,
                    voltage_max_v = 240.0,
                    catalog_version = "catalog-1",
                    ruleset_version = "ruleset-1",
                    snapshot_schema_version = 1,
                    snapshot_json = "{}",
                )
                error("force rollback")
            }
        }

        assertEquals(0L, database.savedMatchQueries.countAll().executeAsOne())
    }

    @Test
    fun clearLocalDataDeletesHistoryAndCountersButPreservesDisplayChoices() = runTest {
        val savedMatches = store()
        val settings = BulbMatchSettingsStore(MapSettings())
        savedMatches.save(write(id = "saved", createdAt = 100, snapshot = "{}"))
        settings.setLocaleOverride(StoredLocaleOverride.RU)
        settings.setThemeOverride(StoredThemeOverride.DARK)
        settings.recordCompatibleMatch()
        settings.recordInterstitialImpression(500)

        LocalDataCleaner(savedMatches, settings).clear()

        assertEquals(0L, database.savedMatchQueries.countAll().executeAsOne())
        assertEquals(StoredLocaleOverride.RU, settings.localeOverride.value)
        assertEquals(StoredThemeOverride.DARK, settings.themeOverride.value)
        assertEquals(0, settings.adFrequencyState.value.completedCompatibleMatches)
        assertEquals(0, settings.adFrequencyState.value.compatibleMatchesSinceInterstitial)
        assertEquals(null, settings.adFrequencyState.value.lastInterstitialEpochMs)
    }

    @Test
    fun driverCanBeClosedExplicitly() {
        driver.close()

        assertFails {
            database.savedMatchQueries.countAll().executeAsOne()
        }
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            BulbMatchDatabase.Schema.create(it)
        }
    }

    private fun store() = SqlDelightSavedMatchStore(
        database = database,
        snapshotCodec = StringSnapshotCodec,
        ioDispatcher = Dispatchers.Default,
    )

    private fun write(
        id: String,
        createdAt: Long,
        snapshot: String,
    ) = SavedMatchWrite(
        summary = PersistedSavedMatchSummary(
            id = id,
            createdAtEpochMs = createdAt,
            displayName = "  Kitchen  ",
            statusCode = "COMPATIBLE",
            baseCode = "E27",
            rawBaseText = null,
            voltageMinV = 220.0,
            voltageMaxV = 240.0,
            catalogVersion = "catalog-1",
            rulesetVersion = "ruleset-1",
            snapshotSchemaVersion = 1,
        ),
        snapshot = snapshot,
    )
}

private object StringSnapshotCodec : SnapshotCodec<String> {
    override val currentSchemaVersion: Int = 1

    override fun encode(value: String): String = value

    override fun decode(
        schemaVersion: Int,
        json: String,
    ): SnapshotDecodeResult<String> = when {
        schemaVersion != currentSchemaVersion -> SnapshotDecodeResult.Failure(
            SnapshotDecodeFailure.UnsupportedSchema(schemaVersion),
        )
        !json.startsWith("{") || !json.endsWith("}") -> SnapshotDecodeResult.Failure(
            SnapshotDecodeFailure.MalformedRequiredFields,
        )
        else -> SnapshotDecodeResult.Success(json)
    }
}
