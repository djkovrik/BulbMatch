package com.sedsoftware.bulbmatch.domain

enum class VoltageDisposition {
    InScope,
    OutsideScope,
    Ambiguous,
}

data class VoltageFamilyRule(
    val minimumVolts: Double,
    val maximumVolts: Double,
    val disposition: VoltageDisposition,
    val reasonCode: String,
) {
    init {
        require(minimumVolts.isFinite() && maximumVolts.isFinite())
        require(minimumVolts > 0.0 && minimumVolts <= maximumVolts)
        require(reasonCode.isNotBlank())
    }

    fun fullyContains(marking: VoltageMarking): Boolean =
        marking.minimumVolts >= minimumVolts && marking.maximumVolts <= maximumVolts
}

data class CatalogSnapshot(
    val catalogVersion: String,
    val rulesetVersion: String,
    val enabledBaseCodes: Set<BaseCode>,
    val voltageRules: List<VoltageFamilyRule>,
    val targetVoltage: VoltageMarking,
    val targetFrequency: FrequencyMarking,
) {
    init {
        require(catalogVersion.isNotBlank())
        require(rulesetVersion.isNotBlank())
        require(voltageRules.isNotEmpty())
    }

    fun classify(marking: VoltageMarking): VoltageDisposition {
        val matches = voltageRules.filter { it.fullyContains(marking) }
        if (matches.size != 1) return VoltageDisposition.Ambiguous
        return matches.single().disposition
    }
}
