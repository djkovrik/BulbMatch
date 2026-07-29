package com.sedsoftware.bulbmatch.app

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogBundle
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.CatalogSnapshot
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.CreatedAtEpochMillis
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchId
import com.sedsoftware.bulbmatch.domain.VoltageDisposition
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import com.sedsoftware.bulbmatch.domain.VoltageFamilyRule
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class AppComponentTestFixture {
    val dispatcher = StandardTestDispatcher()
    lateinit var lifecycle: LifecycleRegistry
        private set

    fun setUp() {
        Dispatchers.setMain(dispatcher)
        lifecycle = LifecycleRegistry().also {
            it.create()
            it.start()
            it.resume()
        }
    }

    fun tearDown() {
        lifecycle.destroy()
        Dispatchers.resetMain()
    }

    fun componentContext() = DefaultComponentContext(lifecycle = lifecycle)
}

internal fun testCatalogProvider(): InMemoryCatalogProvider =
    InMemoryCatalogProvider(CatalogAvailability.Available(testCatalogBundle()))

internal fun testCatalogBundle(): CatalogBundle {
    val e27 = testBaseCode()
    return CatalogBundle(
        snapshot = CatalogSnapshot(
            catalogVersion = "test-catalog",
            rulesetVersion = "test-rules",
            enabledBaseCodes = setOf(e27),
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
                aliasesEn = listOf("standard screw"),
                aliasesRu = listOf("эдисон"),
                diagramId = "base_e27",
                distinguishingHintEn = "27 mm screw",
                distinguishingHintRu = "резьба 27 мм",
                enabledForAssessment = true,
            ),
        ),
        publishedAt = "2026-07-29",
        schemaVersion = 1,
        sourceManifestVersion = "test",
        contentHash = "test-hash",
    )
}

internal fun testBaseCode(): BaseCode = requireNotNull(BaseCode.from("E27"))

internal fun testCompatibleInput(): ConfirmedMatchInput =
    ConfirmedMatchInput(
        base = ConfirmedBase.Known(testBaseCode()),
        voltage = VoltageEvidence.Marking(requireNotNull(VoltageMarking.nominal(230.0))),
    )

internal fun testCompatibleAssessment(): Assessment =
    CompatibilityEngine().assess(testCompatibleInput(), testCatalogBundle().snapshot)

internal fun testSavedMatch(
    id: String = "saved-1",
    createdAt: Long = 1_000L,
    displayName: String? = "Kitchen",
): SavedMatch {
    val assessment = testCompatibleAssessment()
    return SavedMatch(
        id = requireNotNull(SavedMatchId.from(id)),
        displayName = displayName,
        createdAt = requireNotNull(CreatedAtEpochMillis.from(createdAt)),
        confirmedInput = assessment.retainedConfirmedInput,
        assessment = assessment,
        catalogVersion = "test-catalog",
        rulesetVersion = "test-rules",
        snapshotSchemaVersion = 1,
    )
}
