package com.sedsoftware.bulbmatch.app

import com.sedsoftware.bulbmatch.domain.AdFrequencyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHistoryComponentTest {
    private val fixture = AppComponentTestFixture()

    @BeforeTest
    fun setUp() = fixture.setUp()

    @AfterTest
    fun tearDown() = fixture.tearDown()

    @Test
    fun historyOpensStoredSnapshotAndDeletesItThroughDetail() = runTest(fixture.dispatcher) {
        val older = testSavedMatch(id = "saved-1", createdAt = 1_000L)
        val newer = testSavedMatch(id = "saved-2", createdAt = 2_000L)
        val repository = InMemorySavedMatchRepository(listOf(older, newer))
        val outputs = mutableListOf<HistoryComponent.Output>()
        val component = createComponent(repository = repository, output = outputs::add)
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertFalse(component.model.value.loading)
        assertEquals(listOf(newer.id, older.id), component.model.value.summaries.map { it.id })

        component.onSavedMatchSelected(older.id)
        fixture.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            listOf<HistoryComponent.Output>(HistoryComponent.Output.OpenSavedMatch(older.id)),
            outputs,
        )
        val detail = assertNotNull(component.detailSlot.value.child?.instance)
        assertEquals(older, requireNotNull(detail.model.value.savedMatch))

        detail.onDeleteRequested()
        assertTrue(detail.model.value.deleteConfirmationVisible)
        detail.onDeleteCancelled()
        assertFalse(detail.model.value.deleteConfirmationVisible)
        detail.onDeleteRequested()
        detail.onDeleteConfirmed()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertNull(component.detailSlot.value.child)
        assertEquals(listOf(newer), repository.snapshot())
    }

    @Test
    fun clearAllAlsoResetsAdFrequencyAndStartMatchIsAnOutput() = runTest(fixture.dispatcher) {
        val repository = InMemorySavedMatchRepository(listOf(testSavedMatch()))
        val settings = InMemorySettingsRepository(
            frequency = AdFrequencyState(4, 10_000L, 2),
        )
        val outputs = mutableListOf<HistoryComponent.Output>()
        val component = createComponent(
            repository = repository,
            settings = settings,
            output = outputs::add,
        )
        fixture.dispatcher.scheduler.advanceUntilIdle()

        component.onClearAllRequested()
        assertTrue(component.model.value.clearAllConfirmationVisible)
        component.onClearAllCancelled()
        assertFalse(component.model.value.clearAllConfirmationVisible)
        component.onClearAllRequested()
        component.onClearAllConfirmed()
        fixture.dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.snapshot().isEmpty())
        assertEquals(AdFrequencyState(0, null, 0), settings.adFrequencyState.value)

        component.onStartMatchRequested()
        assertEquals(listOf<HistoryComponent.Output>(HistoryComponent.Output.StartMatch), outputs)
    }

    private fun createComponent(
        repository: InMemorySavedMatchRepository,
        settings: InMemorySettingsRepository = InMemorySettingsRepository(),
        output: (HistoryComponent.Output) -> Unit = {},
    ) = DefaultHistoryComponent(
        componentContext = fixture.componentContext(),
        repository = repository,
        settingsRepository = settings,
        output = output,
    )
}
