package com.sedsoftware.bulbmatch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkingParserTest {
    private val e27 = requireNotNull(BaseCode.from("E27"))
    private val parser = MarkingParser()
    private val aliases = BaseAliasIndex(mapOf("E27" to e27))

    @Test
    fun parsesLocaleIndependentElectricalMarking() {
        val result = parser.parse(
            lines = listOf(RawTextObservation("E27 220–240 V 50 Hz 8,5 W 806 lm 2700 K")),
            baseAliases = aliases,
        )
        assertEquals("E27", result.single { it.fieldKey == FieldKey.Base }.parsedCandidate)
        assertEquals("220-240", result.single { it.fieldKey == FieldKey.Voltage }.parsedCandidate)
        assertEquals("50", result.single { it.fieldKey == FieldKey.Frequency }.parsedCandidate)
        assertEquals("8.5", result.single { it.fieldKey == FieldKey.SourceRatedPower }.parsedCandidate)
        assertEquals("806", result.single { it.fieldKey == FieldKey.LuminousFlux }.parsedCandidate)
        assertEquals("2700", result.single { it.fieldKey == FieldKey.ColorTemperature }.parsedCandidate)
    }

    @Test
    fun printedEquivalentIsNotActualPower() {
        val result = parser.parse(
            lines = listOf(
                RawTextObservation("8 W"),
                RawTextObservation("60 W equivalent"),
            ),
            baseAliases = aliases,
        )
        assertEquals("8", result.single {
            it.fieldKey == FieldKey.SourceRatedPower
        }.parsedCandidate)
        assertEquals("60", result.single {
            it.fieldKey == FieldKey.PrintedEquivalentPower
        }.parsedCandidate)
    }

    @Test
    fun competingVoltagesRemainVisibleAndFixtureLimitIsNeverCreated() {
        val result = parser.parse(
            lines = listOf(
                RawTextObservation("120 V"),
                RawTextObservation("230 V 9 W"),
            ),
            baseAliases = aliases,
        )
        assertEquals(
            listOf("120", "230"),
            result.filter { it.fieldKey == FieldKey.Voltage }.map { it.parsedCandidate },
        )
        assertFalse(result.any { it.fieldKey == FieldKey.FixtureMaximumPower })
    }

    @Test
    fun nonDimmableWinsOverContainedDimmableToken() {
        val result = parser.parse(
            lines = listOf(RawTextObservation("NON-DIMMABLE")),
            baseAliases = aliases,
        )
        assertTrue(result.any {
            it.fieldKey == FieldKey.Dimmability &&
                it.parsedCandidate == Dimmability.No.name
        })
    }
}
