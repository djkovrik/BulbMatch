package com.sedsoftware.bulbmatch.data.catalog

import com.sedsoftware.bulbmatch.data.DefaultCatalogProvider
import com.sedsoftware.bulbmatch.domain.AdvisoryCheck
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogSnapshot
import com.sedsoftware.bulbmatch.domain.ClarificationReason
import com.sedsoftware.bulbmatch.domain.CompatibilityEngine
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.ConflictReason
import com.sedsoftware.bulbmatch.domain.Dimmability
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FixtureMaximumPower
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.Lumens
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import com.sedsoftware.bulbmatch.domain.Watts
import kotlinx.serialization.decodeFromString
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogSafetyFixtureTest {
    private val projectRoot = Path.of(
        requireNotNull(System.getProperty("bulbmatch.projectRoot")),
    )
    private val catalogPath = projectRoot.resolve(
        "shared/data/src/commonMain/resources/catalog/bulbmatch-catalog-production.json",
    )
    private val fixturesPath = projectRoot.resolve(
        "spec/catalog/releases/2026.08.01/fixtures/safety-fixtures.json",
    )
    private val catalogBytes = Files.readAllBytes(catalogPath)
    private val document = assertIs<CatalogLoadResult.Valid>(
        BundledCatalogLoader().load(catalogBytes, CatalogValidationMode.Development),
    ).catalog
    private val suite = catalogJson.decodeFromString<CatalogSafetyFixtureSuite>(
        Files.readString(fixturesPath, StandardCharsets.UTF_8),
    )
    private val provider = DefaultCatalogProvider(
        utf8Catalog = catalogBytes,
        mode = CatalogValidationMode.Development,
        ruleset = BundledCatalogRules.ruleset,
    )
    private val candidateSnapshot = assertIs<CatalogAvailability.Available>(
        provider.availability.value,
    ).catalog.snapshot.copy(
        enabledBaseCodes = document.entries
            .mapTo(linkedSetOf()) { entry -> baseCode(entry.canonicalCode) },
    )

    @Test
    fun fixtureSuiteHasExactApprovedVersionAndTraceability() {
        assertEquals("2026.08.01", suite.suiteVersion)
        assertEquals(document.catalogVersion, suite.catalogVersion)
        assertEquals(document.rulesetVersion, suite.rulesetVersion)
        assertEquals("Sergey V.", suite.reviewer)
        assertEquals("2026-07-31T07:21:40Z", suite.reviewedAt)
        assertEquals("APPROVED", suite.reviewDecision)
        assertTrue(suite.fixtures.isNotEmpty())
        assertEquals(
            suite.fixtures.size,
            suite.fixtures.map(CatalogSafetyFixture::fixtureId).distinct().size,
        )
        suite.fixtures.forEach { fixture ->
            assertTrue(fixture.requirements.isNotEmpty(), fixture.fixtureId)
            assertTrue(fixture.acceptanceScenarios.isNotEmpty(), fixture.fixtureId)
            assertTrue(fixture.coveredRuleCodes.isNotEmpty(), fixture.fixtureId)
            assertTrue(
                fixture.coveredRuleCodes.all(BundledCatalogRules.reviewedRuleCodes::contains),
                fixture.fixtureId,
            )
            assertEquals("APPROVED", fixture.reviewState, fixture.fixtureId)
        }
    }

    @Test
    fun allAssessmentFixturesMatchExpectedOutcomesWithoutFalsePositives() {
        suite.fixtures
            .filter { it.kind == "ASSESSMENT" }
            .forEach { fixture ->
                val result = CompatibilityEngine().assess(
                    input = fixture.input.toDomain(),
                    catalog = candidateSnapshot,
                )
                assertEquals(
                    fixture.expectedOutcome,
                    result.outcomeCode(),
                    fixture.fixtureId,
                )
                assertEquals(
                    fixture.expectedReasonCodes,
                    result.reasonCodes(),
                    fixture.fixtureId,
                )
                fixture.expectedProfileAssertions.forEach { assertion ->
                    assertProfile(assertion, result, fixture.fixtureId)
                }
                if (fixture.expectedOutcome != "COMPATIBLE") {
                    assertTrue(result !is Assessment.Compatible, fixture.fixtureId)
                }
            }
    }

    @Test
    fun allEnglishAndRussianAliasFixturesResolveToExactlyOneEntry() {
        suite.fixtures
            .filter { it.kind == "ALIAS_SEARCH" }
            .forEach { fixture ->
                val query = requireNotNull(fixture.input.searchQuery)
                val matches = provider.searchEntries(query)
                assertEquals(1, matches.size, fixture.fixtureId)
                assertEquals(
                    fixture.expectedBaseCode?.uppercase(),
                    matches.single().code.value,
                    fixture.fixtureId,
                )
            }
    }

    private fun CatalogSafetyFixtureInput.toDomain(): ConfirmedMatchInput {
        val observedKeys = when (observationDecision) {
            "REJECTED", "EDITED", "UNREVIEWED" -> setOf(FieldKey.Base)
            null, "MANUAL" -> emptySet()
            else -> error("Unsupported observationDecision: $observationDecision")
        }
        return ConfirmedMatchInput(
            base = when (baseKind) {
                "KNOWN" -> ConfirmedBase.Known(baseCode(requireNotNull(baseValue)))
                "UNKNOWN" -> requireNotNull(ConfirmedBase.Unknown.from(requireNotNull(baseValue)))
                "MISSING" -> ConfirmedBase.Missing
                else -> error("Unsupported baseKind: $baseKind")
            },
            voltage = when (voltageKind) {
                "NOMINAL", "RANGE" -> VoltageEvidence.Marking(voltageValues.single().toDomain())
                "CONTRADICTORY" -> VoltageEvidence.Contradictory(
                    voltageValues.map { value -> value.toDomain() },
                )
                "MISSING" -> VoltageEvidence.Missing
                else -> error("Unsupported voltageKind: $voltageKind")
            },
            frequency = frequencyHz?.let { requireNotNull(FrequencyMarking.from(it)) },
            sourceRatedPower = sourceRatedWatts?.let { requireNotNull(Watts.from(it)) },
            printedEquivalentPower = printedEquivalentWatts?.let { requireNotNull(Watts.from(it)) },
            luminousFlux = lumens?.let { requireNotNull(Lumens.from(it)) },
            dimmability = when (dimmability) {
                null, "UNKNOWN" -> Dimmability.Unknown
                "YES" -> Dimmability.Yes
                "NO" -> Dimmability.No
                else -> error("Unsupported dimmability: $dimmability")
            },
            fixtureMaximumPower = fixtureMaximumWatts?.let {
                FixtureMaximumPower.manual(requireNotNull(Watts.from(it)))
            },
            observationKeys = observedKeys,
            reviewedFields = if (observationDecision == "EDITED") observedKeys else emptySet(),
            rejectedObservations =
                if (observationDecision == "REJECTED") observedKeys else emptySet(),
        )
    }

    private fun CatalogVoltageFixtureValue.toDomain(): VoltageMarking =
        requireNotNull(VoltageMarking.range(minimum, maximum))

    private fun Assessment.outcomeCode(): String = when (this) {
        is Assessment.Compatible -> "COMPATIBLE"
        is Assessment.NeedClarification -> "NEED_CLARIFICATION"
        is Assessment.PotentialConflict -> "POTENTIAL_CONFLICT"
    }

    private fun Assessment.reasonCodes(): List<String> = when (this) {
        is Assessment.Compatible -> emptyList()
        is Assessment.NeedClarification -> reasons.map { reason ->
            when (reason) {
                is ClarificationReason.UnknownBase -> "UnknownBase"
                is ClarificationReason.UnsupportedBase -> "UnsupportedBase"
                is ClarificationReason.UnreviewedField -> "UnreviewedField"
                ClarificationReason.AmbiguousVoltage -> "AmbiguousVoltage"
                ClarificationReason.MissingBase -> "MissingBase"
                ClarificationReason.MissingVoltage -> "MissingVoltage"
            }
        }
        is Assessment.PotentialConflict -> reasons.map { reason ->
            when (reason) {
                is ConflictReason.FixturePowerConflict -> "FixturePowerConflict"
                ConflictReason.ContradictoryVoltage -> "ContradictoryVoltage"
                ConflictReason.OutsideElectricalScope -> "OutsideElectricalScope"
                ConflictReason.OutsideFrequencyScope -> "OutsideFrequencyScope"
            }
        }
    }

    private fun assertProfile(
        assertion: String,
        assessment: Assessment,
        fixtureId: String,
    ) {
        val compatible = assertIs<Assessment.Compatible>(assessment, fixtureId)
        val profile = compatible.profile
        when (assertion) {
            "FIXTURE_MAXIMUM_ABSENT" -> assertNull(profile.fixtureMaximumPower, fixtureId)
            "FIXTURE_MAXIMUM_40" ->
                assertEquals(40.0, assertNotNull(profile.fixtureMaximumPower).watts.value, fixtureId)
            "SOURCE_RATED_WATTS_8" ->
                assertEquals(8.0, assertNotNull(profile.sourceRatedPower).value, fixtureId)
            "PRINTED_EQUIVALENT_WATTS_60" ->
                assertEquals(60.0, assertNotNull(profile.printedEquivalentPower).value, fixtureId)
            "BRIGHTNESS_LUMENS_806" ->
                assertEquals(806.0, assertNotNull(profile.brightness).target.value, fixtureId)
            "BRIGHTNESS_ABSENT" -> assertNull(profile.brightness, fixtureId)
            "DIMMABILITY_YES" -> assertEquals(Dimmability.Yes, profile.dimmability, fixtureId)
            "DIMMABILITY_NO" -> assertEquals(Dimmability.No, profile.dimmability, fixtureId)
            "DIMMABILITY_UNKNOWN" ->
                assertEquals(Dimmability.Unknown, profile.dimmability, fixtureId)
            "VERIFY_DIMMER" ->
                assertTrue(AdvisoryCheck.VerifyDimmerRequirements in compatible.advisoryChecks)
            else -> error("Unsupported profile assertion: $assertion")
        }
    }

    private fun baseCode(raw: String): BaseCode = requireNotNull(BaseCode.from(raw))
}
