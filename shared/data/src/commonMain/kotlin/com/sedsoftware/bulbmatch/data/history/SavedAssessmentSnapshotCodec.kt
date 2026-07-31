package com.sedsoftware.bulbmatch.data.history

import com.sedsoftware.bulbmatch.domain.AdvisoryCheck
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.BrightnessPreference
import com.sedsoftware.bulbmatch.domain.ClarificationReason
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SavedAssessmentSnapshot(
    val confirmedInput: ConfirmedMatchInput,
    val assessment: Assessment,
) {
    init {
        require(assessment.retainedConfirmedInput == confirmedInput) {
            "Assessment must retain the exact confirmed input being saved."
        }
    }
}

object SavedAssessmentSnapshotCodec : SnapshotCodec<SavedAssessmentSnapshot> {
    override val currentSchemaVersion: Int = SAVED_MATCH_SNAPSHOT_SCHEMA_VERSION

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
        isLenient = false
    }

    override fun encode(value: SavedAssessmentSnapshot): String =
        json.encodeToString(value.toDto())

    override fun decode(
        schemaVersion: Int,
        json: String,
    ): SnapshotDecodeResult<SavedAssessmentSnapshot> {
        if (schemaVersion != currentSchemaVersion) {
            return SnapshotDecodeResult.Failure(
                SnapshotDecodeFailure.UnsupportedSchema(schemaVersion),
            )
        }
        return try {
            val dto = this.json.decodeFromString<SavedMatchSnapshotV1Dto>(json)
            if (dto.schemaVersion != schemaVersion) {
                SnapshotDecodeResult.Failure(SnapshotDecodeFailure.MalformedRequiredFields)
            } else {
                SnapshotDecodeResult.Success(dto.toDomain())
            }
        } catch (_: SerializationException) {
            SnapshotDecodeResult.Failure(SnapshotDecodeFailure.MalformedRequiredFields)
        } catch (_: IllegalArgumentException) {
            SnapshotDecodeResult.Failure(SnapshotDecodeFailure.MalformedRequiredFields)
        }
    }
}

fun SavedAssessmentSnapshot.toPersistedWrite(
    id: String,
    createdAtEpochMs: Long,
    displayName: String?,
    catalogVersion: String,
    rulesetVersion: String,
): SavedMatchWrite<SavedAssessmentSnapshot> {
    val input = confirmedInput
    val voltage = (input.voltage as? VoltageEvidence.Marking)?.value
    val base = input.base
    return SavedMatchWrite(
        summary = PersistedSavedMatchSummary(
            id = id,
            createdAtEpochMs = createdAtEpochMs,
            displayName = displayName,
            statusCode = assessment.statusCode(),
            baseCode = (base as? ConfirmedBase.Known)?.code?.value,
            rawBaseText = (base as? ConfirmedBase.Unknown)?.rawText,
            voltageMinV = voltage?.minimumVolts,
            voltageMaxV = voltage?.maximumVolts,
            catalogVersion = catalogVersion,
            rulesetVersion = rulesetVersion,
            snapshotSchemaVersion = SAVED_MATCH_SNAPSHOT_SCHEMA_VERSION,
        ),
        snapshot = this,
    )
}

private fun Assessment.statusCode(): String = when (this) {
    is Assessment.Compatible -> "COMPATIBLE"
    is Assessment.NeedClarification -> "NEED_CLARIFICATION"
    is Assessment.PotentialConflict -> "POTENTIAL_CONFLICT"
}

@Serializable
private data class SavedMatchSnapshotV1Dto(
    val schemaVersion: Int,
    val confirmedInput: ConfirmedInputDto,
    val assessment: AssessmentDto,
)

@Serializable
private data class ConfirmedInputDto(
    val base: BaseDto,
    val voltage: VoltageEvidenceDto,
    val frequencyHz: Double?,
    val sourceRatedPowerW: Double?,
    val printedEquivalentPowerW: Double?,
    val luminousFluxLm: Double?,
    val colorTemperatureK: Double?,
    val shapeCode: String?,
    val dimmabilityCode: String,
    val fixtureMaximumPowerW: Double?,
    val observationKeys: List<String>,
    val reviewedFields: List<String>,
    val rejectedObservations: List<String>,
)

@Serializable
private data class BaseDto(
    val kind: String,
    val value: String?,
)

@Serializable
private data class VoltageEvidenceDto(
    val kind: String,
    val ranges: List<VoltageRangeDto>,
)

@Serializable
private data class VoltageRangeDto(
    val minimumVolts: Double,
    val maximumVolts: Double,
)

@Serializable
private data class AssessmentDto(
    val outcomeCode: String,
    val clarificationReasons: List<ReasonDto>,
    val conflictReasons: List<ReasonDto>,
    val profile: CompatibleProfileDto?,
    val explanationCodes: List<String>,
    val advisoryCheckCodes: List<String>,
)

@Serializable
private data class ReasonDto(
    val code: String,
    val fieldKey: String? = null,
    val rawText: String? = null,
    val baseCode: String? = null,
    val sourcePowerW: Double? = null,
    val fixtureMaximumPowerW: Double? = null,
)

@Serializable
private data class CompatibleProfileDto(
    val exactBaseCode: String,
    val requiredVoltage: VoltageRangeDto,
    val requiredFrequencyHz: Double,
    val fixtureMaximumPowerW: Double?,
    val sourceRatedPowerW: Double?,
    val printedEquivalentPowerW: Double?,
    val brightnessTargetLm: Double?,
    val brightnessTolerancePercent: Int?,
    val colorTemperatureK: Double?,
    val shapeCode: String?,
    val dimmabilityCode: String,
)

private fun SavedAssessmentSnapshot.toDto() = SavedMatchSnapshotV1Dto(
    schemaVersion = SAVED_MATCH_SNAPSHOT_SCHEMA_VERSION,
    confirmedInput = confirmedInput.toDto(),
    assessment = assessment.toDto(),
)

private fun ConfirmedMatchInput.toDto() = ConfirmedInputDto(
    base = when (val current = base) {
        is ConfirmedBase.Known -> BaseDto("KNOWN", current.code.value)
        is ConfirmedBase.Unknown -> BaseDto("UNKNOWN", current.rawText)
        ConfirmedBase.Missing -> BaseDto("MISSING", null)
    },
    voltage = when (val current = voltage) {
        is VoltageEvidence.Marking -> VoltageEvidenceDto(
            kind = "MARKING",
            ranges = listOf(current.value.toDto()),
        )
        is VoltageEvidence.Contradictory -> VoltageEvidenceDto(
            kind = "CONTRADICTORY",
            ranges = current.values.map(VoltageMarking::toDto),
        )
        VoltageEvidence.Missing -> VoltageEvidenceDto("MISSING", emptyList())
    },
    frequencyHz = frequency?.hertz,
    sourceRatedPowerW = sourceRatedPower?.value,
    printedEquivalentPowerW = printedEquivalentPower?.value,
    luminousFluxLm = luminousFlux?.value,
    colorTemperatureK = colorTemperature?.value,
    shapeCode = shape?.value,
    dimmabilityCode = dimmability.toStableCode(),
    fixtureMaximumPowerW = fixtureMaximumPower?.watts?.value,
    observationKeys = observationKeys.map(FieldKey::toStableCode).sorted(),
    reviewedFields = reviewedFields.map(FieldKey::toStableCode).sorted(),
    rejectedObservations = rejectedObservations.map(FieldKey::toStableCode).sorted(),
)

private fun Assessment.toDto(): AssessmentDto = when (this) {
    is Assessment.Compatible -> AssessmentDto(
        outcomeCode = "COMPATIBLE",
        clarificationReasons = emptyList(),
        conflictReasons = emptyList(),
        profile = profile.toDto(),
        explanationCodes = explanations.map(ExplanationCode::toStableCode),
        advisoryCheckCodes = advisoryChecks.map(AdvisoryCheck::toStableCode),
    )
    is Assessment.NeedClarification -> AssessmentDto(
        outcomeCode = "NEED_CLARIFICATION",
        clarificationReasons = reasons.map(ClarificationReason::toDto),
        conflictReasons = emptyList(),
        profile = null,
        explanationCodes = emptyList(),
        advisoryCheckCodes = emptyList(),
    )
    is Assessment.PotentialConflict -> AssessmentDto(
        outcomeCode = "POTENTIAL_CONFLICT",
        clarificationReasons = emptyList(),
        conflictReasons = reasons.map(ConflictReason::toDto),
        profile = null,
        explanationCodes = emptyList(),
        advisoryCheckCodes = emptyList(),
    )
}

private fun CompatibleProfile.toDto() = CompatibleProfileDto(
    exactBaseCode = exactBase.value,
    requiredVoltage = requiredVoltage.toDto(),
    requiredFrequencyHz = requiredFrequency.hertz,
    fixtureMaximumPowerW = fixtureMaximumPower?.watts?.value,
    sourceRatedPowerW = sourceRatedPower?.value,
    printedEquivalentPowerW = printedEquivalentPower?.value,
    brightnessTargetLm = brightness?.target?.value,
    brightnessTolerancePercent = brightness?.tolerancePercent,
    colorTemperatureK = colorTemperature?.value,
    shapeCode = shape?.value,
    dimmabilityCode = dimmability.toStableCode(),
)

private fun VoltageMarking.toDto() = VoltageRangeDto(minimumVolts, maximumVolts)

private fun ClarificationReason.toDto(): ReasonDto = when (this) {
    is ClarificationReason.UnreviewedField ->
        ReasonDto("UNREVIEWED_FIELD", fieldKey = field.toStableCode())
    ClarificationReason.MissingBase -> ReasonDto("MISSING_BASE")
    is ClarificationReason.UnknownBase -> ReasonDto("UNKNOWN_BASE", rawText = rawText)
    is ClarificationReason.UnsupportedBase ->
        ReasonDto("UNSUPPORTED_BASE", baseCode = code.value)
    ClarificationReason.MissingVoltage -> ReasonDto("MISSING_VOLTAGE")
    ClarificationReason.AmbiguousVoltage -> ReasonDto("AMBIGUOUS_VOLTAGE")
}

private fun ConflictReason.toDto(): ReasonDto = when (this) {
    ConflictReason.ContradictoryVoltage -> ReasonDto("CONTRADICTORY_VOLTAGE")
    ConflictReason.OutsideElectricalScope -> ReasonDto("OUTSIDE_ELECTRICAL_SCOPE")
    ConflictReason.OutsideFrequencyScope -> ReasonDto("OUTSIDE_FREQUENCY_SCOPE")
    is ConflictReason.FixturePowerConflict -> ReasonDto(
        code = "FIXTURE_POWER_CONFLICT",
        sourcePowerW = sourcePower.value,
        fixtureMaximumPowerW = fixtureMaximumPower.watts.value,
    )
}

private fun SavedMatchSnapshotV1Dto.toDomain(): SavedAssessmentSnapshot {
    val input = confirmedInput.toDomain()
    return SavedAssessmentSnapshot(
        confirmedInput = input,
        assessment = assessment.toDomain(input),
    )
}

private fun ConfirmedInputDto.toDomain(): ConfirmedMatchInput = ConfirmedMatchInput(
    base = base.toDomain(),
    voltage = voltage.toDomain(),
    frequency = frequencyHz?.let {
        FrequencyMarking.from(it) ?: malformed()
    },
    sourceRatedPower = sourceRatedPowerW?.let { Watts.from(it) ?: malformed() },
    printedEquivalentPower = printedEquivalentPowerW?.let { Watts.from(it) ?: malformed() },
    luminousFlux = luminousFluxLm?.let { Lumens.from(it) ?: malformed() },
    colorTemperature = colorTemperatureK?.let { Kelvin.from(it) ?: malformed() },
    shape = shapeCode?.let { ShapeCode.from(it) ?: malformed() },
    dimmability = dimmabilityCode.toDimmability(),
    fixtureMaximumPower = fixtureMaximumPowerW?.let {
        FixtureMaximumPower.manual(Watts.from(it) ?: malformed())
    },
    observationKeys = observationKeys.toFieldKeySet(),
    reviewedFields = reviewedFields.toFieldKeySet(),
    rejectedObservations = rejectedObservations.toFieldKeySet(),
)

private fun BaseDto.toDomain(): ConfirmedBase = when (kind) {
    "KNOWN" -> ConfirmedBase.Known(BaseCode.from(value ?: malformed()) ?: malformed())
    "UNKNOWN" -> ConfirmedBase.Unknown.from(value ?: malformed()) ?: malformed()
    "MISSING" -> {
        require(value == null)
        ConfirmedBase.Missing
    }
    else -> malformed()
}

private fun VoltageEvidenceDto.toDomain(): VoltageEvidence = when (kind) {
    "MARKING" -> {
        require(ranges.size == 1)
        VoltageEvidence.Marking(ranges.single().toDomain())
    }
    "CONTRADICTORY" -> {
        require(ranges.size >= 2)
        VoltageEvidence.Contradictory(ranges.map(VoltageRangeDto::toDomain))
    }
    "MISSING" -> {
        require(ranges.isEmpty())
        VoltageEvidence.Missing
    }
    else -> malformed()
}

private fun VoltageRangeDto.toDomain(): VoltageMarking =
    VoltageMarking.range(minimumVolts, maximumVolts) ?: malformed()

private fun AssessmentDto.toDomain(input: ConfirmedMatchInput): Assessment = when (outcomeCode) {
    "COMPATIBLE" -> {
        require(clarificationReasons.isEmpty() && conflictReasons.isEmpty())
        require(explanationCodes.isNotEmpty() && advisoryCheckCodes.isNotEmpty())
        Assessment.Compatible(
            profile = (profile ?: malformed()).toDomain(),
            explanations = explanationCodes.map(String::toExplanationCode),
            advisoryChecks = advisoryCheckCodes.map(String::toAdvisoryCheck),
            retainedConfirmedInput = input,
        )
    }
    "NEED_CLARIFICATION" -> {
        require(profile == null && conflictReasons.isEmpty())
        require(explanationCodes.isEmpty() && advisoryCheckCodes.isEmpty())
        require(clarificationReasons.isNotEmpty())
        Assessment.NeedClarification(
            reasons = clarificationReasons.map(ReasonDto::toClarificationReason),
            retainedConfirmedInput = input,
        )
    }
    "POTENTIAL_CONFLICT" -> {
        require(profile == null && clarificationReasons.isEmpty())
        require(explanationCodes.isEmpty() && advisoryCheckCodes.isEmpty())
        require(conflictReasons.isNotEmpty())
        Assessment.PotentialConflict(
            reasons = conflictReasons.map(ReasonDto::toConflictReason),
            retainedConfirmedInput = input,
        )
    }
    else -> malformed()
}

private fun CompatibleProfileDto.toDomain(): CompatibleProfile {
    require((brightnessTargetLm == null) == (brightnessTolerancePercent == null))
    return CompatibleProfile(
        exactBase = BaseCode.from(exactBaseCode) ?: malformed(),
        requiredVoltage = requiredVoltage.toDomain(),
        requiredFrequency = FrequencyMarking.from(requiredFrequencyHz) ?: malformed(),
        fixtureMaximumPower = fixtureMaximumPowerW?.let {
            FixtureMaximumPower.manual(Watts.from(it) ?: malformed())
        },
        sourceRatedPower = sourceRatedPowerW?.let { Watts.from(it) ?: malformed() },
        printedEquivalentPower = printedEquivalentPowerW?.let { Watts.from(it) ?: malformed() },
        brightness = brightnessTargetLm?.let { target ->
            val tolerance = brightnessTolerancePercent ?: malformed()
            require(tolerance in 0..100)
            BrightnessPreference(
                target = Lumens.from(target) ?: malformed(),
                tolerancePercent = tolerance,
            )
        },
        colorTemperature = colorTemperatureK?.let { Kelvin.from(it) ?: malformed() },
        shape = shapeCode?.let { ShapeCode.from(it) ?: malformed() },
        dimmability = dimmabilityCode.toDimmability(),
    )
}

private fun ReasonDto.toClarificationReason(): ClarificationReason = when (code) {
    "UNREVIEWED_FIELD" ->
        ClarificationReason.UnreviewedField((fieldKey ?: malformed()).toFieldKey())
    "MISSING_BASE" -> ClarificationReason.MissingBase
    "UNKNOWN_BASE" -> ClarificationReason.UnknownBase(rawText ?: malformed())
    "UNSUPPORTED_BASE" ->
        ClarificationReason.UnsupportedBase(BaseCode.from(baseCode ?: malformed()) ?: malformed())
    "MISSING_VOLTAGE" -> ClarificationReason.MissingVoltage
    "AMBIGUOUS_VOLTAGE" -> ClarificationReason.AmbiguousVoltage
    else -> malformed()
}

private fun ReasonDto.toConflictReason(): ConflictReason = when (code) {
    "CONTRADICTORY_VOLTAGE" -> ConflictReason.ContradictoryVoltage
    "OUTSIDE_ELECTRICAL_SCOPE" -> ConflictReason.OutsideElectricalScope
    "OUTSIDE_FREQUENCY_SCOPE" -> ConflictReason.OutsideFrequencyScope
    "FIXTURE_POWER_CONFLICT" -> ConflictReason.FixturePowerConflict(
        sourcePower = Watts.from(sourcePowerW ?: malformed()) ?: malformed(),
        fixtureMaximumPower = FixtureMaximumPower.manual(
            Watts.from(fixtureMaximumPowerW ?: malformed()) ?: malformed(),
        ),
    )
    else -> malformed()
}

private fun FieldKey.toStableCode(): String = when (this) {
    FieldKey.Base -> "BASE"
    FieldKey.Voltage -> "VOLTAGE"
    FieldKey.Frequency -> "FREQUENCY"
    FieldKey.SourceRatedPower -> "SOURCE_RATED_POWER"
    FieldKey.PrintedEquivalentPower -> "PRINTED_EQUIVALENT_POWER"
    FieldKey.LuminousFlux -> "LUMINOUS_FLUX"
    FieldKey.ColorTemperature -> "COLOR_TEMPERATURE"
    FieldKey.Shape -> "SHAPE"
    FieldKey.Dimmability -> "DIMMABILITY"
    FieldKey.FixtureMaximumPower -> "FIXTURE_MAXIMUM_POWER"
}

private fun String.toFieldKey(): FieldKey = when (this) {
    "BASE" -> FieldKey.Base
    "VOLTAGE" -> FieldKey.Voltage
    "FREQUENCY" -> FieldKey.Frequency
    "SOURCE_RATED_POWER" -> FieldKey.SourceRatedPower
    "PRINTED_EQUIVALENT_POWER" -> FieldKey.PrintedEquivalentPower
    "LUMINOUS_FLUX" -> FieldKey.LuminousFlux
    "COLOR_TEMPERATURE" -> FieldKey.ColorTemperature
    "SHAPE" -> FieldKey.Shape
    "DIMMABILITY" -> FieldKey.Dimmability
    "FIXTURE_MAXIMUM_POWER" -> FieldKey.FixtureMaximumPower
    else -> malformed()
}

private fun List<String>.toFieldKeySet(): Set<FieldKey> {
    require(size == distinct().size)
    return map(String::toFieldKey).toSet()
}

private fun Dimmability.toStableCode(): String = when (this) {
    Dimmability.Yes -> "YES"
    Dimmability.No -> "NO"
    Dimmability.Unknown -> "UNKNOWN"
}

private fun String.toDimmability(): Dimmability = when (this) {
    "YES" -> Dimmability.Yes
    "NO" -> Dimmability.No
    "UNKNOWN" -> Dimmability.Unknown
    else -> malformed()
}

private fun ExplanationCode.toStableCode(): String = when (this) {
    ExplanationCode.KnownBaseConfirmed -> "KNOWN_BASE_CONFIRMED"
    ExplanationCode.VoltageInScope -> "VOLTAGE_IN_SCOPE"
    ExplanationCode.FixtureLimitConfirmed -> "FIXTURE_LIMIT_CONFIRMED"
    ExplanationCode.FixtureLimitUnresolved -> "FIXTURE_LIMIT_UNRESOLVED"
}

private fun String.toExplanationCode(): ExplanationCode = when (this) {
    "KNOWN_BASE_CONFIRMED" -> ExplanationCode.KnownBaseConfirmed
    "VOLTAGE_IN_SCOPE" -> ExplanationCode.VoltageInScope
    "FIXTURE_LIMIT_CONFIRMED" -> ExplanationCode.FixtureLimitConfirmed
    "FIXTURE_LIMIT_UNRESOLVED" -> ExplanationCode.FixtureLimitUnresolved
    else -> malformed()
}

private fun AdvisoryCheck.toStableCode(): String = when (this) {
    AdvisoryCheck.ThisDoesNotCertifyFixture -> "THIS_DOES_NOT_CERTIFY_FIXTURE"
    AdvisoryCheck.SwitchPowerOff -> "SWITCH_POWER_OFF"
    AdvisoryCheck.CheckFixtureLabel -> "CHECK_FIXTURE_LABEL"
    AdvisoryCheck.VerifyDimensionsAndEnclosure -> "VERIFY_DIMENSIONS_AND_ENCLOSURE"
    AdvisoryCheck.VerifyDimmerRequirements -> "VERIFY_DIMMER_REQUIREMENTS"
    AdvisoryCheck.BrightnessUnresolved -> "BRIGHTNESS_UNRESOLVED"
}

private fun String.toAdvisoryCheck(): AdvisoryCheck = when (this) {
    "THIS_DOES_NOT_CERTIFY_FIXTURE" -> AdvisoryCheck.ThisDoesNotCertifyFixture
    "SWITCH_POWER_OFF" -> AdvisoryCheck.SwitchPowerOff
    "CHECK_FIXTURE_LABEL" -> AdvisoryCheck.CheckFixtureLabel
    "VERIFY_DIMENSIONS_AND_ENCLOSURE" -> AdvisoryCheck.VerifyDimensionsAndEnclosure
    "VERIFY_DIMMER_REQUIREMENTS" -> AdvisoryCheck.VerifyDimmerRequirements
    "BRIGHTNESS_UNRESOLVED" -> AdvisoryCheck.BrightnessUnresolved
    else -> malformed()
}

private fun malformed(): Nothing = throw IllegalArgumentException("Malformed snapshot")
