package com.sedsoftware.bulbmatch.data.catalog

import com.sedsoftware.bulbmatch.domain.FrequencyMarking
import com.sedsoftware.bulbmatch.domain.VoltageDisposition
import com.sedsoftware.bulbmatch.domain.VoltageFamilyRule
import com.sedsoftware.bulbmatch.domain.VoltageMarking

data class ReviewedCatalogRuleset(
    val version: String,
    val reviewedRuleCodes: List<String>,
    val voltageRules: List<VoltageFamilyRule>,
    val targetVoltage: VoltageMarking,
    val targetFrequency: FrequencyMarking,
) {
    init {
        require(version.isNotBlank())
        require(reviewedRuleCodes.isNotEmpty())
        require(reviewedRuleCodes.none(String::isBlank))
        require(reviewedRuleCodes.distinct().size == reviewedRuleCodes.size)
        require(voltageRules.isNotEmpty())
    }
}

/**
 * The single reviewed catalog ruleset shared by Android and iOS.
 *
 * These values remain pending human approval until the release bundle for [VERSION] is signed by
 * Sergey V. Values outside the explicitly covered voltage families remain ambiguous and therefore
 * cannot produce a positive assessment.
 */
object BundledCatalogRules {
    const val VERSION = "2026.08.01"

    val reviewedRuleCodes: List<String> = listOf(
        "CATALOG_KNOWN_BASE_ENABLED",
        "BASE_NO_SUBSTITUTION",
        "VOLTAGE_TARGET_220_240",
        "VOLTAGE_OTHER_MAINS_100_127",
        "VOLTAGE_LOW_1_48",
        "VOLTAGE_UNCOVERED_AMBIGUOUS",
        "VOLTAGE_CONTRADICTORY",
        "FREQUENCY_CONFIRMED_TARGET_50",
        "FIXTURE_MAXIMUM_MANUAL_ONLY",
        "SOURCE_WATTS_NOT_FIXTURE_MAXIMUM",
        "LUMENS_SEPARATE_FROM_EQUIVALENT_WATTS",
        "FIXTURE_SAFETY_DISCLAIMER",
    )

    val ruleset: ReviewedCatalogRuleset = ReviewedCatalogRuleset(
        version = VERSION,
        reviewedRuleCodes = reviewedRuleCodes,
        voltageRules = listOf(
            VoltageFamilyRule(
                minimumVolts = 220.0,
                maximumVolts = 240.0,
                disposition = VoltageDisposition.InScope,
                reasonCode = "VOLTAGE_TARGET_220_240",
            ),
            VoltageFamilyRule(
                minimumVolts = 100.0,
                maximumVolts = 127.0,
                disposition = VoltageDisposition.OutsideScope,
                reasonCode = "VOLTAGE_OTHER_MAINS_100_127",
            ),
            VoltageFamilyRule(
                minimumVolts = 1.0,
                maximumVolts = 48.0,
                disposition = VoltageDisposition.OutsideScope,
                reasonCode = "VOLTAGE_LOW_1_48",
            ),
        ),
        targetVoltage = requireNotNull(VoltageMarking.range(220.0, 240.0)),
        targetFrequency = requireNotNull(FrequencyMarking.from(50.0)),
    )
}
