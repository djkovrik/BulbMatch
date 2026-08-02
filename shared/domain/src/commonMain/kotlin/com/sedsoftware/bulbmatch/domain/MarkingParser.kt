package com.sedsoftware.bulbmatch.domain

data class RawTextObservation(
    val text: String,
    val geometry: ObservationGeometry? = null,
)

data class BaseAliasIndex(
    val aliases: Map<String, BaseCode>,
) {
    fun findIn(text: String): BaseCode? {
        val tokens = text.uppercase()
            .split(Regex("[^A-ZА-ЯЁ0-9.]+"))
            .filter(String::isNotBlank)
        return tokens.asSequence()
            .map(::normalizeCatalogToken)
            .mapNotNull(aliases::get)
            .firstOrNull()
    }

    companion object {
        fun from(entries: List<CatalogEntry>): BaseAliasIndex {
            val aliases = buildMap {
                entries.forEach { entry ->
                    (entry.aliasesEn + entry.aliasesRu + entry.code.value).forEach {
                        put(normalizeCatalogToken(it), entry.code)
                    }
                }
            }
            return BaseAliasIndex(aliases)
        }
    }
}

class MarkingParser(
    private val ocrNormalizer: OcrMarkingNormalizer = OcrMarkingNormalizer(),
) {
    fun parse(
        lines: List<RawTextObservation>,
        baseAliases: BaseAliasIndex,
    ): List<ObservedField> = buildList {
        lines.forEach { line ->
            val rawText = line.text.trim()
            if (rawText.isEmpty()) return@forEach
            val parseView = ocrNormalizer.normalizeForParsing(rawText)

            // Base aliases are matched only against the original OCR transcript. The parser-only
            // unit normalization must never repair mixed-script or damaged base tokens.
            baseAliases.findIn(rawText)?.let {
                add(line.field(FieldKey.Base, it.value))
            }
            parseVoltage(parseView).forEach {
                add(line.field(FieldKey.Voltage, it))
            }
            parseSingleUnit(parseView, HERTZ)?.let {
                add(line.field(FieldKey.Frequency, it))
            }
            parseSingleUnit(parseView, LUMENS)?.let {
                add(line.field(FieldKey.LuminousFlux, it))
            }
            parseSingleUnit(parseView, KELVIN)?.let {
                add(line.field(FieldKey.ColorTemperature, it))
            }
            parsePower(parseView)?.let { (field, value) ->
                add(line.field(field, value))
            }
            parseDimmability(parseView)?.let {
                add(line.field(FieldKey.Dimmability, it))
            }
        }
    }.distinctBy {
        Triple(it.fieldKey, it.parsedCandidate, it.geometry)
    }

    private fun parseVoltage(value: String): List<String> =
        VOLTAGE.findAll(value).mapNotNull { match ->
            val first = match.groupValues[1].normalizedDecimalOrNull() ?: return@mapNotNull null
            val second = match.groupValues[2]
                .takeIf(String::isNotEmpty)
                ?.normalizedDecimalOrNull()
            if (second != null && second < first) return@mapNotNull null
            if (second == null) first else "$first-$second"
        }.toList()

    private fun parseSingleUnit(
        value: String,
        regex: Regex,
    ): String? = regex.find(value)?.groupValues?.get(1)?.normalizedDecimalOrNull()

    private fun parsePower(value: String): Pair<FieldKey, String>? {
        val match = WATTS.find(value) ?: return null
        val number = match.groupValues[1].normalizedDecimalOrNull() ?: return null
        val context = value.uppercase()
        val isEquivalent = EQUIVALENT_MARKERS.any(context::contains)
        return (if (isEquivalent) {
            FieldKey.PrintedEquivalentPower
        } else {
            FieldKey.SourceRatedPower
        }) to number
    }

    private fun parseDimmability(value: String): String? {
        val normalized = value.uppercase()
        return when {
            NON_DIMMABLE_MARKERS.any(normalized::contains) -> Dimmability.No.name
            DIMMABLE_MARKERS.any(normalized::contains) -> Dimmability.Yes.name
            else -> null
        }
    }

    private fun RawTextObservation.field(
        key: FieldKey,
        parsed: String,
    ): ObservedField = ObservedField(
        fieldKey = key,
        rawText = text,
        parsedCandidate = parsed,
        confidence = null,
        geometry = geometry,
    )

    private companion object {
        val VOLTAGE = Regex(
            """(?i)(\d{1,4}(?:[.,]\d+)?)\s*(?:[-–—]\s*(\d{1,4}(?:[.,]\d+)?))?\s*V\b""",
        )
        val HERTZ = Regex("""(?i)(\d{1,4}(?:[.,]\d+)?)\s*HZ\b""")
        val WATTS = Regex("""(?i)(\d{1,5}(?:[.,]\d+)?)\s*W\b""")
        val LUMENS = Regex("""(?i)(\d{1,7}(?:[.,]\d+)?)\s*(?:LM|LUMENS?)\b""")
        val KELVIN = Regex("""(?i)(\d{3,6}(?:[.,]\d+)?)\s*K\b""")
        val EQUIVALENT_MARKERS = listOf("EQUIV", "EQUIVALENT", "ЭКВИВ")
        val NON_DIMMABLE_MARKERS = listOf("NON-DIMMABLE", "NOT DIMMABLE", "НЕ ДИММИРУ")
        val DIMMABLE_MARKERS = listOf("DIMMABLE", "ДИММИРУ")
    }
}

private fun String.normalizedDecimalOrNull(): String? {
    val normalized = replace(',', '.')
    val numeric = normalized.toDoubleOrNull() ?: return null
    if (!numeric.isFinite() || numeric <= 0.0) return null
    return normalized
}

private fun normalizeCatalogToken(value: String): String =
    value.trim().uppercase().filter { it.isLetterOrDigit() }
