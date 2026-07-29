package com.sedsoftware.bulbmatch.compose.model

import androidx.compose.runtime.Immutable

enum class AppLanguage { System, English, Russian }
enum class AppThemeMode { System, Light, Dark }
enum class RootDestination { Match, History, Reference }
enum class ScreenLoadState { Content, Loading, Empty, Error }
enum class FieldOrigin { Detected, Edited, Manual }
enum class ReviewDecision { Unreviewed, Confirmed, Rejected }
enum class AssessmentOutcome { Compatible, NeedClarification, PotentialConflict, Unavailable }
enum class CameraState { Content, Opening, DeniedCanAsk, DeniedOpenSettings, Unavailable, Error }

@Immutable
data class FieldUiModel(
    val id: String,
    val label: String,
    val value: String = "",
    val example: String = "",
    val origin: FieldOrigin = FieldOrigin.Manual,
    val decision: ReviewDecision = ReviewDecision.Confirmed,
    val required: Boolean = false,
    val error: String? = null,
)

@Immutable
data class ReviewUiModel(
    val fromOcr: Boolean = false,
    val fields: List<FieldUiModel> = emptyList(),
    val unresolvedCount: Int = 0,
    val canAssess: Boolean = false,
    val loadState: ScreenLoadState = ScreenLoadState.Content,
    val message: String? = null,
)

@Immutable
data class FactUiModel(val label: String, val value: String, val source: String? = null)

@Immutable
data class ResultUiModel(
    val outcome: AssessmentOutcome,
    val confirmedFacts: List<FactUiModel>,
    val reasons: List<String>,
    val unresolvedChecks: List<String>,
    val profile: List<FactUiModel>,
    val checklist: List<String>,
    val catalogVersion: String = "Catalog pending review",
    val rulesetVersion: String = "Ruleset 1",
    val showInlineAd: Boolean = false,
    val loadState: ScreenLoadState = ScreenLoadState.Content,
    val errorMessage: String? = null,
)

@Immutable
data class HistoryItemUiModel(
    val id: String,
    val name: String?,
    val outcome: AssessmentOutcome,
    val base: String,
    val voltage: String,
    val date: String,
)

@Immutable
data class BaseReferenceUiModel(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameRu: String,
    val hintEn: String,
    val hintRu: String,
    val featuresEn: List<String>,
    val featuresRu: List<String>,
    val typicalUseEn: String,
    val typicalUseRu: String,
)

internal val PreviewBases = listOf(
    BaseReferenceUiModel(
        id = "base_e27",
        code = "E27",
        nameEn = "Edison screw",
        nameRu = "Резьбовой цоколь",
        hintEn = "Wide screw base",
        hintRu = "Широкий резьбовой цоколь",
        featuresEn = listOf("Threaded metal shell", "Single contact at the tip"),
        featuresRu = listOf("Резьбовая металлическая гильза", "Один контакт на конце"),
        typicalUseEn = "Often used in household lamps. Typical use does not establish voltage.",
        typicalUseRu = "Часто используется в бытовых лампах. Типичное применение не определяет напряжение.",
    ),
    BaseReferenceUiModel(
        id = "base_e14",
        code = "E14",
        nameEn = "Small Edison screw",
        nameRu = "Малый резьбовой цоколь",
        hintEn = "Narrow screw base",
        hintRu = "Узкий резьбовой цоколь",
        featuresEn = listOf("Narrow threaded shell", "Single contact at the tip"),
        featuresRu = listOf("Узкая резьбовая гильза", "Один контакт на конце"),
        typicalUseEn = "Often used in compact fixtures. Check the printed voltage.",
        typicalUseRu = "Часто используется в компактных светильниках. Проверьте указанное напряжение.",
    ),
    BaseReferenceUiModel(
        id = "base_gu10",
        code = "GU10",
        nameEn = "Twist-lock base",
        nameRu = "Поворотно-штырьковый цоколь",
        hintEn = "Two short headed pins",
        hintRu = "Два коротких штырька с утолщениями",
        featuresEn = listOf("Two short pins", "Push-and-twist locking"),
        featuresRu = listOf("Два коротких штырька", "Фиксация нажатием и поворотом"),
        typicalUseEn = "Common in spot lamps. Base shape alone does not establish voltage.",
        typicalUseRu = "Часто встречается в точечных лампах. Форма цоколя не определяет напряжение.",
    ),
    BaseReferenceUiModel(
        id = "base_g9",
        code = "G9",
        nameEn = "Loop-pin base",
        nameRu = "Петлевой штырьковый цоколь",
        hintEn = "Two looped wire contacts",
        hintRu = "Два петлевых проволочных контакта",
        featuresEn = listOf("Two wire loops", "Push-fit connection"),
        featuresRu = listOf("Две проволочные петли", "Вставное соединение"),
        typicalUseEn = "Used in some compact luminaires. Always read the voltage marking.",
        typicalUseRu = "Используется в некоторых компактных светильниках. Всегда проверяйте напряжение.",
    ),
)

internal fun previewCompatibleResult() = ResultUiModel(
    outcome = AssessmentOutcome.Compatible,
    confirmedFacts = listOf(
        FactUiModel("Base", "E27", "Manual"),
        FactUiModel("Voltage", "220–240 V", "Edited"),
        FactUiModel("Source lamp power", "8 W", "Detected"),
        FactUiModel("Light output", "806 lm", "Detected"),
    ),
    reasons = listOf(
        "The confirmed base is present in the reviewed catalog.",
        "The confirmed voltage marking is within the supported 220–240 V supply family.",
    ),
    unresolvedChecks = listOf(
        "Physical clearance, enclosure rating, dimmer support, and fixture condition are not established.",
    ),
    profile = listOf(
        FactUiModel("Base", "E27"),
        FactUiModel("Voltage", "220–240 V"),
        FactUiModel("Lamp power", "8 W"),
        FactUiModel("Brightness", "725–887 lm"),
        FactUiModel("Colour temperature", "2700 K"),
    ),
    checklist = listOf(
        "Switch power off before removing or fitting a lamp.",
        "Check the fixture label and its maximum wattage separately.",
        "Confirm physical size, enclosure, dimmer, heat, moisture, and wiring suitability.",
        "Ask a qualified person if anything is damaged, hot, wet, loose, or unclear.",
    ),
    catalogVersion = "Development catalog · pending Sergey V. approval",
    rulesetVersion = "Ruleset 1",
    showInlineAd = true,
)
