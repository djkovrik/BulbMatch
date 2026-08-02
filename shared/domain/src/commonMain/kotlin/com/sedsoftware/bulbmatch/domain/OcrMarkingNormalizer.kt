package com.sedsoftware.bulbmatch.domain

/**
 * Builds a parser-only view of OCR text without changing the transcript shown to the user.
 *
 * The replacements are deliberately limited to electrical units written after a number.
 * Cyrillic/Latin lookalikes in product or base tokens are never transliterated here.
 */
class OcrMarkingNormalizer {
    fun normalizeForParsing(rawText: String): String = UNIT_NORMALIZATIONS.fold(rawText.trim()) { text, rule ->
        rule.pattern.replace(text) { match ->
            "${match.groupValues[1]} ${rule.canonicalUnit}"
        }
    }

    private data class UnitNormalization(
        val pattern: Regex,
        val canonicalUnit: String,
    )

    private companion object {
        private const val NUMBER = "(\\d{1,7}(?:[.,]\\d+)?)"

        // Longer tokens are listed first to keep the boundary explicit and auditable.
        val UNIT_NORMALIZATIONS = listOf(
            UnitNormalization(
                Regex("$NUMBER\\s*(?:ВТ|ВАТТ(?:А|ОВ)?)(?=\\s|$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
                "W",
            ),
            UnitNormalization(
                Regex("$NUMBER\\s*(?:ГЦ|ГЕРЦ(?:А|ОВ)?)(?=\\s|$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
                "Hz",
            ),
            UnitNormalization(
                Regex("$NUMBER\\s*(?:ЛМ|ЛЮМЕН(?:А|ОВ)?)(?=\\s|$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
                "lm",
            ),
            UnitNormalization(
                Regex("$NUMBER\\s*(?:К|КЕЛЬВИН(?:А|ОВ)?)(?=\\s|$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
                "K",
            ),
            UnitNormalization(
                Regex("$NUMBER\\s*(?:В|ВОЛЬТ(?:А|ОВ)?)(?=\\s|$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE),
                "V",
            ),
        )
    }
}
