package com.sedsoftware.bulbmatch.app

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogBundle
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.CatalogSnapshot
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.ObservedField
import com.sedsoftware.bulbmatch.domain.VoltageDisposition
import com.sedsoftware.bulbmatch.domain.VoltageFamilyRule
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MatchStoreTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun everyOcrFieldMustBeConfirmedOrRejected() = runTest(dispatcher) {
        val store = createStore()
        store.accept(
            MatchStore.Intent.StartOcrReview(
                listOf(
                    observation(FieldKey.Base, "E27"),
                    observation(FieldKey.Voltage, "220-240 V"),
                    observation(FieldKey.LuminousFlux, "806 lm"),
                ),
            ),
        )

        store.accept(MatchStore.Intent.KnownBaseSelected(requireNotNull(BaseCode.from("E27"))))
        store.accept(MatchStore.Intent.ObservationConfirmed(FieldKey.Base))
        store.accept(MatchStore.Intent.ObservationConfirmed(FieldKey.Voltage))

        assertEquals(setOf(FieldKey.LuminousFlux), store.state.input.unhandledObservationKeys)

        store.accept(MatchStore.Intent.ObservationRejected(FieldKey.LuminousFlux))
        assertTrue(store.state.input.unhandledObservationKeys.isEmpty())
        store.dispose()
    }

    @Test
    fun confirmingCyrillicOcrBaseResolvesTheExactCatalogAlias() = runTest(dispatcher) {
        val store = createStore()
        store.accept(
            MatchStore.Intent.StartOcrReview(
                listOf(observation(FieldKey.Base, "Е14")),
            ),
        )

        store.accept(MatchStore.Intent.ObservationConfirmed(FieldKey.Base))

        assertEquals(
            ConfirmedBase.Known(requireNotNull(BaseCode.from("E14"))),
            store.state.input.base,
        )
        assertTrue(FieldKey.Base in store.state.input.reviewedFields)
        assertFalse(FieldKey.Base in store.state.validationErrors)
        store.dispose()
    }

    @Test
    fun manualCyrillicBaseTextResolvesWithoutLocalOperationError() = runTest(dispatcher) {
        val store = createStore()
        store.accept(MatchStore.Intent.StartManual)

        store.accept(MatchStore.Intent.FieldTextChanged(FieldKey.Base, "Е14"))

        assertEquals(
            ConfirmedBase.Known(requireNotNull(BaseCode.from("E14"))),
            store.state.input.base,
        )
        assertFalse(FieldKey.Base in store.state.validationErrors)
        store.dispose()
    }

    @Test
    fun ocrCanNeverConfirmFixtureMaximum() = runTest(dispatcher) {
        val store = createStore()
        store.accept(
            MatchStore.Intent.StartOcrReview(
                listOf(observation(FieldKey.FixtureMaximumPower, "60 W")),
            ),
        )
        store.accept(MatchStore.Intent.FieldTextChanged(FieldKey.FixtureMaximumPower, "60 W"))
        store.accept(MatchStore.Intent.ObservationConfirmed(FieldKey.FixtureMaximumPower))

        assertFalse(FieldKey.FixtureMaximumPower in store.state.input.reviewedFields)
        assertEquals(
            "fixture_maximum_manual_only",
            store.state.validationErrors[FieldKey.FixtureMaximumPower],
        )
        store.dispose()
    }

    @Test
    fun compatibleAssessmentRecordsOneCompletedOrdinal() = runTest(dispatcher) {
        val settings = InMemorySettingsRepository()
        val store = createStore(settings)
        store.accept(MatchStore.Intent.StartManual)
        store.accept(MatchStore.Intent.KnownBaseSelected(requireNotNull(BaseCode.from("E27"))))
        store.accept(MatchStore.Intent.FieldTextChanged(FieldKey.Voltage, "220-240 V"))
        store.accept(MatchStore.Intent.Assess)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, store.state.completedMatchOrdinal)
        assertEquals(1, settings.adFrequencyState.value.completedCompatibleMatches)
        store.dispose()
    }

    private fun createStore(
        settings: InMemorySettingsRepository = InMemorySettingsRepository(),
    ): MatchStore =
        MatchStoreProvider(
            storeFactory = DefaultStoreFactory(),
            compatibilityEngine = CompatibilityEngine(),
            catalogProvider = catalogProvider(),
            settingsRepository = settings,
        ).provide()

    private fun catalogProvider(): InMemoryCatalogProvider {
        val e27 = requireNotNull(BaseCode.from("E27"))
        val e14 = requireNotNull(BaseCode.from("E14"))
        val catalog = CatalogBundle(
            snapshot = CatalogSnapshot(
                catalogVersion = "test-catalog",
                rulesetVersion = "test-rules",
                enabledBaseCodes = setOf(e27, e14),
                voltageRules = listOf(
                    VoltageFamilyRule(200.0, 250.0, VoltageDisposition.InScope, "target"),
                    VoltageFamilyRule(100.0, 127.0, VoltageDisposition.OutsideScope, "other"),
                ),
                targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
                targetFrequency = requireNotNull(FrequencyMarking.from(50.0)),
            ),
            entries = listOf(
                CatalogEntry(
                    code = e27,
                    commonNameEn = "Edison screw",
                    commonNameRu = "Резьбовой цоколь",
                    aliasesEn = emptyList(),
                    aliasesRu = listOf("Е27"),
                    diagramId = "base_e27",
                    distinguishingHintEn = "27 mm screw",
                    distinguishingHintRu = "резьба 27 мм",
                    enabledForAssessment = true,
                ),
                CatalogEntry(
                    code = e14,
                    commonNameEn = "Small Edison screw",
                    commonNameRu = "Малый резьбовой цоколь",
                    aliasesEn = emptyList(),
                    aliasesRu = listOf("Е14"),
                    diagramId = "base_e14",
                    distinguishingHintEn = "14 mm screw",
                    distinguishingHintRu = "резьба 14 мм",
                    enabledForAssessment = true,
                ),
            ),
            publishedAt = "2026-07-29",
            schemaVersion = 1,
            sourceManifestVersion = "test",
            contentHash = "test-hash",
        )
        return InMemoryCatalogProvider(CatalogAvailability.Available(catalog))
    }

    private fun observation(field: FieldKey, text: String): ObservedField =
        ObservedField(
            fieldKey = field,
            rawText = text,
            parsedCandidate = text,
            confidence = null,
            geometry = null,
        )
}
