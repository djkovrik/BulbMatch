package com.sedsoftware.bulbmatch.domain

import kotlin.jvm.JvmInline

private const val MAX_RAW_BASE_CODE_POINTS = 80

enum class FieldKey {
    Base,
    Voltage,
    Frequency,
    SourceRatedPower,
    PrintedEquivalentPower,
    LuminousFlux,
    ColorTemperature,
    Shape,
    Dimmability,
    FixtureMaximumPower,
}

enum class FieldOrigin {
    Detected,
    Edited,
    Manual,
}

data class ObservationGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class ObservedField(
    val fieldKey: FieldKey,
    val rawText: String,
    val parsedCandidate: String?,
    val confidence: Float?,
    val geometry: ObservationGeometry?,
)

@JvmInline
value class BaseCode private constructor(val value: String) {
    companion object {
        fun from(raw: String): BaseCode? {
            val value = raw.trim()
            if (value.isEmpty() || value.length > 32 || value.any(Char::isWhitespace)) return null
            return BaseCode(value.uppercase())
        }
    }
}

sealed interface ConfirmedBase {
    data class Known(val code: BaseCode) : ConfirmedBase
    class Unknown private constructor(val rawText: String) : ConfirmedBase {
        companion object {
            fun from(raw: String): Unknown? {
                val value = raw.trim()
                if (value.isEmpty() || value.codePointCount() > MAX_RAW_BASE_CODE_POINTS) return null
                return Unknown(value)
            }
        }

        override fun equals(other: Any?): Boolean = other is Unknown && rawText == other.rawText
        override fun hashCode(): Int = rawText.hashCode()
        override fun toString(): String = "Unknown(rawText=$rawText)"
    }

    data object Missing : ConfirmedBase
}

class VoltageMarking private constructor(
    val minimumVolts: Double,
    val maximumVolts: Double,
) {
    companion object {
        fun nominal(volts: Double): VoltageMarking? = range(volts, volts)

        fun range(minimumVolts: Double, maximumVolts: Double): VoltageMarking? {
            if (!minimumVolts.isFinite() || !maximumVolts.isFinite()) return null
            if (minimumVolts <= 0.0 || maximumVolts <= 0.0) return null
            if (minimumVolts > maximumVolts || maximumVolts > 10_000.0) return null
            return VoltageMarking(minimumVolts, maximumVolts)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is VoltageMarking &&
            minimumVolts == other.minimumVolts &&
            maximumVolts == other.maximumVolts

    override fun hashCode(): Int = 31 * minimumVolts.hashCode() + maximumVolts.hashCode()
    override fun toString(): String =
        "VoltageMarking(minimumVolts=$minimumVolts, maximumVolts=$maximumVolts)"
}

sealed interface VoltageEvidence {
    data object Missing : VoltageEvidence
    data class Marking(val value: VoltageMarking) : VoltageEvidence
    data class Contradictory(val values: List<VoltageMarking>) : VoltageEvidence {
        init {
            require(values.size >= 2)
        }
    }
}

@JvmInline
value class FrequencyMarking private constructor(val hertz: Double) {
    companion object {
        fun from(hertz: Double): FrequencyMarking? =
            if (hertz.isFinite() && hertz > 0.0 && hertz <= 1_000.0) {
                FrequencyMarking(hertz)
            } else {
                null
            }
    }
}

@JvmInline
value class Watts private constructor(val value: Double) {
    companion object {
        fun from(value: Double): Watts? =
            if (value.isFinite() && value > 0.0 && value <= 100_000.0) Watts(value) else null
    }
}

class FixtureMaximumPower private constructor(val watts: Watts) {
    companion object {
        fun manual(watts: Watts): FixtureMaximumPower = FixtureMaximumPower(watts)
    }

    override fun equals(other: Any?): Boolean =
        other is FixtureMaximumPower && watts == other.watts

    override fun hashCode(): Int = watts.hashCode()
    override fun toString(): String = "FixtureMaximumPower(watts=$watts)"
}

@JvmInline
value class Lumens private constructor(val value: Double) {
    companion object {
        fun from(value: Double): Lumens? =
            if (value.isFinite() && value > 0.0 && value <= 1_000_000.0) Lumens(value) else null
    }
}

@JvmInline
value class Kelvin private constructor(val value: Double) {
    companion object {
        fun from(value: Double): Kelvin? =
            if (value.isFinite() && value in 1_000.0..100_000.0) Kelvin(value) else null
    }
}

@JvmInline
value class ShapeCode private constructor(val value: String) {
    companion object {
        fun from(raw: String): ShapeCode? {
            val value = raw.trim()
            return if (value.isNotEmpty() && value.length <= 32) ShapeCode(value.uppercase()) else null
        }
    }
}

enum class Dimmability {
    Yes,
    No,
    Unknown,
}

data class ConfirmedMatchInput(
    val base: ConfirmedBase = ConfirmedBase.Missing,
    val voltage: VoltageEvidence = VoltageEvidence.Missing,
    val frequency: FrequencyMarking? = null,
    val sourceRatedPower: Watts? = null,
    val printedEquivalentPower: Watts? = null,
    val luminousFlux: Lumens? = null,
    val colorTemperature: Kelvin? = null,
    val shape: ShapeCode? = null,
    val dimmability: Dimmability = Dimmability.Unknown,
    val fixtureMaximumPower: FixtureMaximumPower? = null,
    val observationKeys: Set<FieldKey> = emptySet(),
    val reviewedFields: Set<FieldKey> = emptySet(),
    val rejectedObservations: Set<FieldKey> = emptySet(),
) {
    val unhandledObservationKeys: Set<FieldKey>
        get() = observationKeys - reviewedFields - rejectedObservations
}

typealias MatchDraft = ConfirmedMatchInput

private fun String.codePointCount(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        val current = this[index]
        when {
            current.isHighSurrogate() -> {
                if (index + 1 >= length || !this[index + 1].isLowSurrogate()) {
                    return Int.MAX_VALUE
                }
                index += 2
            }
            current.isLowSurrogate() -> return Int.MAX_VALUE
            else -> index += 1
        }
        count++
    }
    return count
}
