package com.sedsoftware.bulbmatch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CompatibilityEngineTest {
    private val e27 = requireNotNull(BaseCode.from("E27"))
    private val catalog = CatalogSnapshot(
        catalogVersion = "test-catalog-1",
        rulesetVersion = "test-rules-1",
        enabledBaseCodes = setOf(e27),
        voltageRules = listOf(
            VoltageFamilyRule(220.0, 240.0, VoltageDisposition.InScope, "TARGET_MAINS"),
            VoltageFamilyRule(100.0, 127.0, VoltageDisposition.OutsideScope, "OTHER_MAINS"),
            VoltageFamilyRule(1.0, 48.0, VoltageDisposition.OutsideScope, "LOW_VOLTAGE"),
        ),
        targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
        targetFrequency = requireNotNull(FrequencyMarking.from(50.0)),
    )
    private val engine = CompatibilityEngine()

    @Test
    fun valueObjectsRejectInvalidBoundaries() {
        assertNull(VoltageMarking.nominal(Double.NaN))
        assertNull(VoltageMarking.range(240.0, 220.0))
        assertNull(Watts.from(0.0))
        assertNull(Lumens.from(Double.POSITIVE_INFINITY))
        assertNull(Kelvin.from(999.0))
        assertNotNull(VoltageMarking.nominal(230.0))
        assertNotNull(Watts.from(8.0))
        assertNotNull(Kelvin.from(2700.0))
    }

    @Test
    fun everyObservedFieldRequiresAnExplicitDecision() {
        val result = engine.assess(
            compatibleInput().copy(observationKeys = setOf(FieldKey.Base)),
            catalog,
        )
        val clarification = assertIs<Assessment.NeedClarification>(result)
        assertEquals(
            listOf(ClarificationReason.UnreviewedField(FieldKey.Base)),
            clarification.reasons,
        )
    }

    @Test
    fun missingUnknownAndUnsupportedBaseNeverProduceCompatible() {
        val cases = listOf(
            ConfirmedBase.Missing,
            requireNotNull(ConfirmedBase.Unknown.from("mystery")),
            ConfirmedBase.Known(requireNotNull(BaseCode.from("GX53"))),
        )
        cases.forEach { base ->
            assertIs<Assessment.NeedClarification>(
                engine.assess(compatibleInput().copy(base = base), catalog),
            )
        }
    }

    @Test
    fun missingContradictoryOutsideAndAmbiguousVoltageNeverProduceCompatible() {
        val outside = requireNotNull(VoltageMarking.range(110.0, 120.0))
        val ambiguous = requireNotNull(VoltageMarking.range(200.0, 240.0))
        val cases = listOf(
            VoltageEvidence.Missing,
            VoltageEvidence.Contradictory(
                listOf(
                    requireNotNull(VoltageMarking.nominal(120.0)),
                    requireNotNull(VoltageMarking.nominal(230.0)),
                ),
            ),
            VoltageEvidence.Marking(outside),
            VoltageEvidence.Marking(ambiguous),
        )
        cases.forEach { voltage ->
            val result = engine.assess(compatibleInput().copy(voltage = voltage), catalog)
            check(result !is Assessment.Compatible)
        }
    }

    @Test
    fun confirmedFrequencyMustMatchTheCatalogTargetWhileMissingFrequencyRemainsOptional() {
        val targetFrequency = requireNotNull(FrequencyMarking.from(50.0))
        val outsideFrequency = requireNotNull(FrequencyMarking.from(60.0))

        assertIs<Assessment.Compatible>(
            engine.assess(compatibleInput().copy(frequency = targetFrequency), catalog),
        )
        assertIs<Assessment.Compatible>(
            engine.assess(compatibleInput().copy(frequency = null), catalog),
        )
        val conflict = assertIs<Assessment.PotentialConflict>(
            engine.assess(compatibleInput().copy(frequency = outsideFrequency), catalog),
        )
        assertEquals(listOf(ConflictReason.OutsideFrequencyScope), conflict.reasons)
    }

    @Test
    fun sourcePowerDoesNotBecomeFixtureLimit() {
        val result = assertIs<Assessment.Compatible>(
            engine.assess(
                compatibleInput().copy(sourceRatedPower = requireNotNull(Watts.from(8.0))),
                catalog,
            ),
        )
        assertNull(result.profile.fixtureMaximumPower)
        assertEquals(ExplanationCode.FixtureLimitUnresolved, result.explanations.last())
        assertEquals(true, AdvisoryCheck.CheckFixtureLabel in result.advisoryChecks)
    }

    @Test
    fun manualFixtureLimitBelowSourcePowerIsAConflict() {
        val result = engine.assess(
            compatibleInput().copy(
                sourceRatedPower = requireNotNull(Watts.from(60.0)),
                fixtureMaximumPower = FixtureMaximumPower.manual(requireNotNull(Watts.from(40.0))),
            ),
            catalog,
        )
        assertIs<Assessment.PotentialConflict>(result)
    }

    @Test
    fun printedEquivalentAndActualPowerRemainSeparate() {
        val actual = requireNotNull(Watts.from(8.0))
        val equivalent = requireNotNull(Watts.from(60.0))
        val result = assertIs<Assessment.Compatible>(
            engine.assess(
                compatibleInput().copy(
                    sourceRatedPower = actual,
                    printedEquivalentPower = equivalent,
                ),
                catalog,
            ),
        )
        assertEquals(actual, result.profile.sourceRatedPower)
        assertEquals(equivalent, result.profile.printedEquivalentPower)
        assertNull(result.profile.fixtureMaximumPower)
    }

    @Test
    fun sameInputAndCatalogAlwaysReturnSameAssessment() {
        val input = compatibleInput().copy(
            luminousFlux = requireNotNull(Lumens.from(806.0)),
            colorTemperature = requireNotNull(Kelvin.from(2700.0)),
        )
        assertEquals(engine.assess(input, catalog), engine.assess(input, catalog))
    }

    @Test
    fun exactKnownBaseAndInScopeVoltageProduceConservativeProfile() {
        val result = assertIs<Assessment.Compatible>(engine.assess(compatibleInput(), catalog))
        assertEquals(e27, result.profile.exactBase)
        assertEquals(catalog.targetVoltage, result.profile.requiredVoltage)
        assertEquals(true, AdvisoryCheck.ThisDoesNotCertifyFixture in result.advisoryChecks)
    }

    private fun compatibleInput(): ConfirmedMatchInput = ConfirmedMatchInput(
        base = ConfirmedBase.Known(e27),
        voltage = VoltageEvidence.Marking(requireNotNull(VoltageMarking.nominal(230.0))),
    )
}
