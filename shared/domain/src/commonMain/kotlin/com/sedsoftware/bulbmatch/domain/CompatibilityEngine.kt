package com.sedsoftware.bulbmatch.domain

sealed interface ClarificationReason {
    data class UnreviewedField(val field: FieldKey) : ClarificationReason
    data object MissingBase : ClarificationReason
    data class UnknownBase(val rawText: String) : ClarificationReason
    data class UnsupportedBase(val code: BaseCode) : ClarificationReason
    data object MissingVoltage : ClarificationReason
    data object AmbiguousVoltage : ClarificationReason
}

sealed interface ConflictReason {
    data object ContradictoryVoltage : ConflictReason
    data object OutsideElectricalScope : ConflictReason
    data object OutsideFrequencyScope : ConflictReason
    data class FixturePowerConflict(
        val sourcePower: Watts,
        val fixtureMaximumPower: FixtureMaximumPower,
    ) : ConflictReason
}

enum class ExplanationCode {
    KnownBaseConfirmed,
    VoltageInScope,
    FixtureLimitConfirmed,
    FixtureLimitUnresolved,
}

enum class AdvisoryCheck {
    ThisDoesNotCertifyFixture,
    SwitchPowerOff,
    CheckFixtureLabel,
    VerifyDimensionsAndEnclosure,
    VerifyDimmerRequirements,
    BrightnessUnresolved,
}

data class BrightnessPreference(
    val target: Lumens,
    val tolerancePercent: Int = 10,
)

data class CompatibleProfile(
    val exactBase: BaseCode,
    val requiredVoltage: VoltageMarking,
    val requiredFrequency: FrequencyMarking,
    val fixtureMaximumPower: FixtureMaximumPower?,
    val sourceRatedPower: Watts?,
    val printedEquivalentPower: Watts?,
    val brightness: BrightnessPreference?,
    val colorTemperature: Kelvin?,
    val shape: ShapeCode?,
    val dimmability: Dimmability,
)

sealed interface Assessment {
    val retainedConfirmedInput: ConfirmedMatchInput

    data class Compatible(
        val profile: CompatibleProfile,
        val explanations: List<ExplanationCode>,
        val advisoryChecks: List<AdvisoryCheck>,
        override val retainedConfirmedInput: ConfirmedMatchInput,
    ) : Assessment

    data class NeedClarification(
        val reasons: List<ClarificationReason>,
        override val retainedConfirmedInput: ConfirmedMatchInput,
    ) : Assessment

    data class PotentialConflict(
        val reasons: List<ConflictReason>,
        override val retainedConfirmedInput: ConfirmedMatchInput,
    ) : Assessment
}

class CompatibilityEngine {
    fun assess(
        input: ConfirmedMatchInput,
        catalog: CatalogSnapshot,
    ): Assessment {
        input.unhandledObservationKeys.firstOrNull()?.let {
            return Assessment.NeedClarification(
                reasons = listOf(ClarificationReason.UnreviewedField(it)),
                retainedConfirmedInput = input,
            )
        }

        val baseCode = when (val base = input.base) {
            ConfirmedBase.Missing -> {
                return Assessment.NeedClarification(
                    reasons = listOf(ClarificationReason.MissingBase),
                    retainedConfirmedInput = input,
                )
            }
            is ConfirmedBase.Unknown -> {
                return Assessment.NeedClarification(
                    reasons = listOf(ClarificationReason.UnknownBase(base.rawText)),
                    retainedConfirmedInput = input,
                )
            }
            is ConfirmedBase.Known -> base.code
        }
        if (baseCode !in catalog.enabledBaseCodes) {
            return Assessment.NeedClarification(
                reasons = listOf(ClarificationReason.UnsupportedBase(baseCode)),
                retainedConfirmedInput = input,
            )
        }

        val voltage = when (val evidence = input.voltage) {
            VoltageEvidence.Missing -> {
                return Assessment.NeedClarification(
                    reasons = listOf(ClarificationReason.MissingVoltage),
                    retainedConfirmedInput = input,
                )
            }
            is VoltageEvidence.Contradictory -> {
                return Assessment.PotentialConflict(
                    reasons = listOf(ConflictReason.ContradictoryVoltage),
                    retainedConfirmedInput = input,
                )
            }
            is VoltageEvidence.Marking -> evidence.value
        }
        when (catalog.classify(voltage)) {
            VoltageDisposition.OutsideScope -> {
                return Assessment.PotentialConflict(
                    reasons = listOf(ConflictReason.OutsideElectricalScope),
                    retainedConfirmedInput = input,
                )
            }
            VoltageDisposition.Ambiguous -> {
                return Assessment.NeedClarification(
                    reasons = listOf(ClarificationReason.AmbiguousVoltage),
                    retainedConfirmedInput = input,
                )
            }
            VoltageDisposition.InScope -> Unit
        }

        if (input.frequency != null && input.frequency != catalog.targetFrequency) {
            return Assessment.PotentialConflict(
                reasons = listOf(ConflictReason.OutsideFrequencyScope),
                retainedConfirmedInput = input,
            )
        }

        val fixtureMaximum = input.fixtureMaximumPower
        val sourcePower = input.sourceRatedPower
        if (fixtureMaximum != null && sourcePower != null && sourcePower.value > fixtureMaximum.watts.value) {
            return Assessment.PotentialConflict(
                reasons = listOf(
                    ConflictReason.FixturePowerConflict(
                        sourcePower = sourcePower,
                        fixtureMaximumPower = fixtureMaximum,
                    ),
                ),
                retainedConfirmedInput = input,
            )
        }

        return Assessment.Compatible(
            profile = CompatibleProfile(
                exactBase = baseCode,
                requiredVoltage = catalog.targetVoltage,
                requiredFrequency = catalog.targetFrequency,
                fixtureMaximumPower = fixtureMaximum,
                sourceRatedPower = sourcePower,
                printedEquivalentPower = input.printedEquivalentPower,
                brightness = input.luminousFlux?.let(::BrightnessPreference),
                colorTemperature = input.colorTemperature,
                shape = input.shape,
                dimmability = input.dimmability,
            ),
            explanations = listOf(
                ExplanationCode.KnownBaseConfirmed,
                ExplanationCode.VoltageInScope,
                if (fixtureMaximum == null) {
                    ExplanationCode.FixtureLimitUnresolved
                } else {
                    ExplanationCode.FixtureLimitConfirmed
                },
            ),
            advisoryChecks = buildList {
                add(AdvisoryCheck.ThisDoesNotCertifyFixture)
                add(AdvisoryCheck.SwitchPowerOff)
                if (fixtureMaximum == null) add(AdvisoryCheck.CheckFixtureLabel)
                add(AdvisoryCheck.VerifyDimensionsAndEnclosure)
                if (input.dimmability != Dimmability.Yes) add(AdvisoryCheck.VerifyDimmerRequirements)
                if (input.luminousFlux == null) add(AdvisoryCheck.BrightnessUnresolved)
            },
            retainedConfirmedInput = input,
        )
    }
}
