package com.sedsoftware.bulbmatch.data.history

import com.sedsoftware.bulbmatch.domain.AdvisoryCheck
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.BrightnessPreference
import com.sedsoftware.bulbmatch.domain.CompatibleProfile
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.ConflictReason
import com.sedsoftware.bulbmatch.domain.Dimmability
import com.sedsoftware.bulbmatch.domain.ExplanationCode
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.FixtureMaximumPower
import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.Kelvin
import com.sedsoftware.bulbmatch.domain.Lumens
import com.sedsoftware.bulbmatch.domain.ShapeCode
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import com.sedsoftware.bulbmatch.domain.VoltageMarking
import com.sedsoftware.bulbmatch.domain.Watts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SavedAssessmentSnapshotCodecTest {
    @Test
    fun completeConfirmedInputAndAssessmentRoundTrip() {
        val input = completeInput()
        val assessment = Assessment.Compatible(
            profile = CompatibleProfile(
                exactBase = baseCode("E27"),
                requiredVoltage = voltage(220.0, 240.0),
                requiredFrequency = frequency(50.0),
                fixtureMaximumPower = fixturePower(12.0),
                sourceRatedPower = watts(8.0),
                printedEquivalentPower = watts(60.0),
                brightness = BrightnessPreference(lumens(806.0), 10),
                colorTemperature = kelvin(2700.0),
                shape = shape("A60"),
                dimmability = Dimmability.Yes,
            ),
            explanations = listOf(
                ExplanationCode.KnownBaseConfirmed,
                ExplanationCode.VoltageInScope,
                ExplanationCode.FixtureLimitConfirmed,
            ),
            advisoryChecks = listOf(
                AdvisoryCheck.ThisDoesNotCertifyFixture,
                AdvisoryCheck.SwitchPowerOff,
                AdvisoryCheck.VerifyDimensionsAndEnclosure,
            ),
            retainedConfirmedInput = input,
        )
        val snapshot = SavedAssessmentSnapshot(input, assessment)

        val encoded = SavedAssessmentSnapshotCodec.encode(snapshot)
        val decoded = SavedAssessmentSnapshotCodec.decode(1, encoded)

        assertEquals(
            snapshot,
            assertIs<SnapshotDecodeResult.Success<SavedAssessmentSnapshot>>(decoded).value,
        )
        assertFalse(encoded.contains("displayName"))
        assertFalse(encoded.contains("ocr", ignoreCase = true))
        assertFalse(encoded.contains("photo", ignoreCase = true))
    }

    @Test
    fun summaryIsDerivedWithoutRecalculatingSnapshot() {
        val input = ConfirmedMatchInput(
            base = ConfirmedBase.Unknown.from("custom cap")!!,
            voltage = VoltageEvidence.Missing,
        )
        val assessment = Assessment.NeedClarification(
            reasons = listOf(com.sedsoftware.bulbmatch.domain.ClarificationReason.UnknownBase("custom cap")),
            retainedConfirmedInput = input,
        )

        val write = SavedAssessmentSnapshot(input, assessment).toPersistedWrite(
            id = "saved-1",
            createdAtEpochMs = 100,
            displayName = null,
            catalogVersion = "catalog-1",
            rulesetVersion = "rules-1",
        )

        assertEquals("NEED_CLARIFICATION", write.summary.statusCode)
        assertEquals("custom cap", write.summary.rawBaseText)
        assertEquals(null, write.summary.baseCode)
        assertEquals(1, write.summary.snapshotSchemaVersion)
    }

    @Test
    fun outsideFrequencyConflictRoundTripsWithItsStableReasonCode() {
        val input = completeInput().copy(frequency = frequency(60.0))
        val assessment = Assessment.PotentialConflict(
            reasons = listOf(ConflictReason.OutsideFrequencyScope),
            retainedConfirmedInput = input,
        )
        val snapshot = SavedAssessmentSnapshot(input, assessment)

        val encoded = SavedAssessmentSnapshotCodec.encode(snapshot)
        val decoded = SavedAssessmentSnapshotCodec.decode(1, encoded)

        assertEquals(
            snapshot,
            assertIs<SnapshotDecodeResult.Success<SavedAssessmentSnapshot>>(decoded).value,
        )
        assertEquals(true, encoded.contains("OUTSIDE_FREQUENCY_SCOPE"))
    }

    @Test
    fun malformedOrForwardSchemaIsIsolated() {
        assertEquals(
            SnapshotDecodeFailure.MalformedRequiredFields,
            assertIs<SnapshotDecodeResult.Failure>(
                SavedAssessmentSnapshotCodec.decode(1, "{}"),
            ).reason,
        )
        assertEquals(
            SnapshotDecodeFailure.UnsupportedSchema(2),
            assertIs<SnapshotDecodeResult.Failure>(
                SavedAssessmentSnapshotCodec.decode(2, "{}"),
            ).reason,
        )
    }

    private fun completeInput() = ConfirmedMatchInput(
        base = ConfirmedBase.Known(baseCode("E27")),
        voltage = VoltageEvidence.Marking(voltage(230.0, 230.0)),
        frequency = frequency(50.0),
        sourceRatedPower = watts(8.0),
        printedEquivalentPower = watts(60.0),
        luminousFlux = lumens(806.0),
        colorTemperature = kelvin(2700.0),
        shape = shape("A60"),
        dimmability = Dimmability.Yes,
        fixtureMaximumPower = fixturePower(12.0),
        observationKeys = setOf(FieldKey.Base, FieldKey.Voltage),
        reviewedFields = setOf(FieldKey.Base, FieldKey.Voltage),
        rejectedObservations = emptySet(),
    )
}

private fun baseCode(value: String): BaseCode = BaseCode.from(value)!!
private fun voltage(minimum: Double, maximum: Double): VoltageMarking =
    VoltageMarking.range(minimum, maximum)!!
private fun frequency(value: Double): FrequencyMarking = FrequencyMarking.from(value)!!
private fun watts(value: Double): Watts = Watts.from(value)!!
private fun fixturePower(value: Double): FixtureMaximumPower =
    FixtureMaximumPower.manual(watts(value))
private fun lumens(value: Double): Lumens = Lumens.from(value)!!
private fun kelvin(value: Double): Kelvin = Kelvin.from(value)!!
private fun shape(value: String): ShapeCode = ShapeCode.from(value)!!

