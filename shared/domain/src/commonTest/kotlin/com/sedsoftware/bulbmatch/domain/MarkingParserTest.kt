package com.sedsoftware.bulbmatch.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkingParserTest {
    private val e27 = requireNotNull(BaseCode.from("E27"))
    private val parser = MarkingParser()
    private val aliases = BaseAliasIndex(mapOf("E27" to e27))

    @Test
    fun exactAliasLookupAcceptsCataloguedCyrillicSpellingOnly() {
        val exactAliases = BaseAliasIndex(
            mapOf(
                "E27" to e27,
                "Е27" to e27,
            ),
        )

        assertEquals(e27, exactAliases.findExact(" Е27 "))
        assertEquals(e27, exactAliases.findExact("E27"))
        assertNull(exactAliases.findExact("Е27 extra"))
    }

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

    @Test
    fun parsesExactCyrillicElectricalUnitsWithoutChangingRawText() {
        val raw = "Е27 220–240 В 50 Гц 8,5 Вт 806 лм 2700 К"
        val cyrillicAliases = BaseAliasIndex(mapOf("Е27" to e27))

        val result = parser.parse(
            lines = listOf(RawTextObservation(raw)),
            baseAliases = cyrillicAliases,
        )

        assertEquals("E27", result.single { it.fieldKey == FieldKey.Base }.parsedCandidate)
        assertEquals("220-240", result.single { it.fieldKey == FieldKey.Voltage }.parsedCandidate)
        assertEquals("50", result.single { it.fieldKey == FieldKey.Frequency }.parsedCandidate)
        assertEquals("8.5", result.single { it.fieldKey == FieldKey.SourceRatedPower }.parsedCandidate)
        assertEquals("806", result.single { it.fieldKey == FieldKey.LuminousFlux }.parsedCandidate)
        assertEquals("2700", result.single { it.fieldKey == FieldKey.ColorTemperature }.parsedCandidate)
        assertTrue(result.all { it.rawText == raw })
    }

    @Test
    fun parsesSupportedCyrillicUnitWords() {
        val result = parser.parse(
            lines = listOf(
                RawTextObservation("230 вольт"),
                RawTextObservation("9 ватт"),
                RawTextObservation("50 герц"),
                RawTextObservation("806 люменов"),
                RawTextObservation("3000 кельвинов"),
            ),
            baseAliases = aliases,
        )

        assertEquals("230", result.single { it.fieldKey == FieldKey.Voltage }.parsedCandidate)
        assertEquals("9", result.single { it.fieldKey == FieldKey.SourceRatedPower }.parsedCandidate)
        assertEquals("50", result.single { it.fieldKey == FieldKey.Frequency }.parsedCandidate)
        assertEquals("806", result.single { it.fieldKey == FieldKey.LuminousFlux }.parsedCandidate)
        assertEquals("3000", result.single { it.fieldKey == FieldKey.ColorTemperature }.parsedCandidate)
    }

    @Test
    fun doesNotRepairMixedScriptOrConfusableBaseTokens() {
        val cyrillicAliases = BaseAliasIndex(
            mapOf(
                "ГУ10" to requireNotNull(BaseCode.from("GU10")),
                "Е27" to e27,
            ),
        )

        listOf("GУ1О", "E2О", "ГY10").forEach { damaged ->
            val result = parser.parse(listOf(RawTextObservation(damaged)), cyrillicAliases)
            assertNull(result.singleOrNull { it.fieldKey == FieldKey.Base }, damaged)
        }
    }

    @Test
    fun normalizerDoesNotCreateBaseOrVoltageWithoutAnExactUnitToken() {
        val cyrillicAliases = BaseAliasIndex(mapOf("Е27" to e27))
        val negatives = listOf(
            "220 Вт",
            "220 Вольфрам",
            "GУ1О 220",
            "Е2О 240",
            "50 Гц 9 Вт",
            "arbitrary 230 text",
        )

        negatives.forEach { raw ->
            val result = parser.parse(listOf(RawTextObservation(raw)), cyrillicAliases)
            assertFalse(result.any { it.fieldKey == FieldKey.Base }, raw)
            assertFalse(result.any { it.fieldKey == FieldKey.Voltage }, raw)
        }
    }
}
