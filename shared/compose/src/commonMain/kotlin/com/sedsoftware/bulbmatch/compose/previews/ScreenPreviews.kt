package com.sedsoftware.bulbmatch.compose.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode
import com.sedsoftware.bulbmatch.compose.model.AssessmentOutcome
import com.sedsoftware.bulbmatch.compose.model.CameraState
import com.sedsoftware.bulbmatch.compose.model.FieldOrigin
import com.sedsoftware.bulbmatch.compose.model.FieldUiModel
import com.sedsoftware.bulbmatch.compose.model.HistoryItemUiModel
import com.sedsoftware.bulbmatch.compose.model.PreviewBases
import com.sedsoftware.bulbmatch.compose.model.ReviewDecision
import com.sedsoftware.bulbmatch.compose.model.ReviewUiModel
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.model.previewCompatibleResult
import com.sedsoftware.bulbmatch.compose.components.AdvertisementSlot
import com.sedsoftware.bulbmatch.compose.components.BaseDiagram
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.screens.BaseReferenceDetailScreen
import com.sedsoftware.bulbmatch.compose.screens.BaseReferenceListScreen
import com.sedsoftware.bulbmatch.compose.screens.CameraCaptureScreen
import com.sedsoftware.bulbmatch.compose.screens.DataReviewScreen
import com.sedsoftware.bulbmatch.compose.screens.HistoryScreen
import com.sedsoftware.bulbmatch.compose.screens.ImageReviewScreen
import com.sedsoftware.bulbmatch.compose.screens.MatchHomeScreen
import com.sedsoftware.bulbmatch.compose.screens.ReplacementResultScreen
import com.sedsoftware.bulbmatch.compose.screens.SaveResultScreen
import com.sedsoftware.bulbmatch.compose.screens.SavedResultDetailScreen
import com.sedsoftware.bulbmatch.compose.screens.SettingsScreen
import com.sedsoftware.bulbmatch.compose.theme.BulbMatchTheme
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "SCREEN-001 Match EN light", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-001 Match EN dark", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-001 Match EN 200 light", widthDp = 390, heightDp = 844, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-001 Match EN 200 dark", widthDp = 390, heightDp = 844, fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MatchHomePreview() = PreviewTheme {
    MatchHomeScreen(
        unfinishedDraftMessage = true,
        onCamera = {},
        onChoosePhoto = {},
        onManual = {},
        onSettings = {},
        onRootDestination = {},
    )
}

@Preview(name = "SCREEN-002 Permission RU light", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-002 Permission RU dark", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CameraPermissionPreview() = PreviewTheme(language = AppLanguage.Russian) {
    CameraCaptureScreen(
        state = CameraState.DeniedOpenSettings,
        onClose = {},
        onShutter = {},
        onToggleTorch = {},
        onTryAgain = {},
        onOpenSettings = {},
        onChoosePhoto = {},
        onManual = {},
    )
}

@Preview(name = "SCREEN-003 Processing light", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-003 Processing dark", widthDp = 390, heightDp = 844, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ImageProcessingPreview() = PreviewTheme {
    ImageReviewScreen(
        state = ScreenLoadState.Loading,
        onBack = {},
        onUsePhoto = {},
        onRetake = {},
        onChooseAnother = {},
        onManual = {},
        onCancelRecognition = {},
    )
}

@Preview(name = "SCREEN-004 OCR review light", widthDp = 390, heightDp = 1400, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-004 OCR review dark", widthDp = 390, heightDp = 1400, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DataReviewPreview() = PreviewTheme {
    DataReviewScreen(
        model = previewReview(),
        onBack = {},
        onValueChange = { _, _ -> },
        onDecision = { _, _ -> },
        onAssess = {},
    )
}

@Preview(name = "SCREEN-004 OCR review RU 200 light", widthDp = 390, heightDp = 844, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun DataReviewLargeTextPreview() = PreviewTheme(language = AppLanguage.Russian) {
    DataReviewScreen(
        model = previewReview(),
        onBack = {},
        onValueChange = { _, _ -> },
        onDecision = { _, _ -> },
        onAssess = {},
    )
}

@Preview(name = "SCREEN-005 Compatible light", widthDp = 390, heightDp = 2000, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-005 Compatible dark", widthDp = 390, heightDp = 2000, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-005 Compatible 200 light", widthDp = 390, heightDp = 2800, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun CompatibleResultPreview() = PreviewTheme {
    ReplacementResultScreen(
        previewCompatibleResult(),
        onBack = {},
        onEdit = {},
        onSave = {},
        onMatchAnother = {},
        onReference = {},
        inlineAdContent = { AdvertisementSlot(true) },
    )
}

@Preview(name = "SCREEN-005 Conflict RU light", widthDp = 390, heightDp = 1800, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-005 Conflict RU dark", widthDp = 390, heightDp = 1800, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-005 Conflict RU 200 dark", widthDp = 390, heightDp = 1800, fontScale = 2f, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ConflictResultPreview() = PreviewTheme(language = AppLanguage.Russian) {
    ReplacementResultScreen(
        previewCompatibleResult().copy(
            outcome = AssessmentOutcome.PotentialConflict,
            reasons = listOf(
                tr(
                    "Confirmed voltage is outside the supported regional supply family.",
                    "Подтверждённое напряжение выходит за пределы поддерживаемого регионального диапазона.",
                ),
            ),
            unresolvedChecks = listOf(
                tr(
                    "The lamp is marked 110–120 V. Do not use this shopping profile on 220–240 V supply.",
                    "На лампе указано 110–120 В. Не используйте этот профиль для сети 220–240 В.",
                ),
            ),
            profile = emptyList(),
            showInlineAd = false,
        ),
        onBack = {},
        onEdit = {},
        onSave = {},
        onMatchAnother = {},
        onReference = {},
    )
}

@Preview(name = "SCREEN-006 Save error light", widthDp = 390, heightDp = 700, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-006 Save error dark", widthDp = 390, heightDp = 700, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SavePreview() = PreviewTheme {
    SaveResultScreen(
        name = "Hall ceiling",
        summary = "E27 · 220–240 V · Compatible profile",
        error = "The local database could not save this result.",
        onNameChange = {},
        onSave = {},
        onCancel = {},
    )
}

@Preview(name = "SCREEN-007 History light", widthDp = 700, heightDp = 900, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-007 History dark", widthDp = 700, heightDp = 900, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-007 History phone light", widthDp = 390, heightDp = 900, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-007 History phone dark", widthDp = 390, heightDp = 900, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-007 History phone 200 light", widthDp = 390, heightDp = 900, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun HistoryPreview() = PreviewTheme {
    HistoryScreen(
        state = ScreenLoadState.Content,
        items = listOf(
            HistoryItemUiModel("1", null, AssessmentOutcome.Compatible, "E27", "220–240 V", "29 Jul 2026"),
            HistoryItemUiModel(
                "2",
                "Kitchen",
                AssessmentOutcome.NeedClarification,
                "Unknown GX24q-3 base marking",
                "Not provided",
                "27 Jul 2026",
            ),
        ),
        onOpen = {},
        onDeleteRequest = {},
        onClearAllRequest = {},
        onConfirmDestructive = {},
        onDismissConfirmation = {},
        onStartMatch = {},
        onRetry = {},
        onSettings = {},
        onRootDestination = {},
        stickyAdContent = { AdvertisementSlot(true) },
    )
}

@Preview(name = "SCREEN-008 Saved detail light", widthDp = 390, heightDp = 2100, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-008 Saved detail dark", widthDp = 390, heightDp = 2100, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-008 Saved detail 200 light", widthDp = 390, heightDp = 2100, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SavedDetailPreview() = PreviewTheme {
    SavedResultDetailScreen(
        name = "Hall ceiling",
        date = "29 July 2026",
        model = previewCompatibleResult(),
        onBack = {},
        onDelete = {},
    )
}

@Preview(name = "SCREEN-009 Reference light", widthDp = 700, heightDp = 900, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-009 Reference dark", widthDp = 700, heightDp = 900, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-009 Reference phone light", widthDp = 390, heightDp = 900, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-009 Reference phone dark", widthDp = 390, heightDp = 900, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ReferenceListPreview() = PreviewTheme {
    BaseReferenceListScreen(
        state = ScreenLoadState.Content,
        entries = PreviewBases,
        query = "",
        onQueryChange = {},
        onCategoryChange = {},
        onOpen = {},
        onClearSearch = {},
        onRetry = {},
        onSettings = {},
        onRootDestination = {},
        stickyAdContent = { AdvertisementSlot(true) },
    )
}

@Preview(name = "SCREEN-010 Reference detail RU light", widthDp = 390, heightDp = 940, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-010 Reference detail RU dark", widthDp = 390, heightDp = 940, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-010 Reference detail RU 200 light", widthDp = 390, heightDp = 1500, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun ReferenceDetailPreview() = PreviewTheme(language = AppLanguage.Russian) {
    BaseReferenceDetailScreen(
        PreviewBases.first(),
        onBack = {},
        onUseBase = {},
        stickyAdContent = { AdvertisementSlot(true) },
    )
}

@Preview(name = "SCREEN-010 Catalog diagrams light", widthDp = 700, heightDp = 620, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-010 Catalog diagrams dark", widthDp = 700, heightDp = 620, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CatalogDiagramBoardPreview() = PreviewTheme {
    val rows = listOf(
        listOf("E27", "E14"),
        listOf("B22d", "GU10"),
        listOf("G9", "R7s"),
    )
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rows.forEach { codes ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    codes.forEach { code ->
                        Column(Modifier.weight(1f)) {
                            Text(code, style = MaterialTheme.typography.titleMedium)
                            BaseDiagram(
                                code = code,
                                alternativeText = "Original identification diagram for $code",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "SCREEN-011 Settings light", widthDp = 390, heightDp = 1000, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-011 Settings dark", widthDp = 390, heightDp = 1000, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() = PreviewTheme {
    SettingsScreen(
        language = AppLanguage.System,
        themeMode = AppThemeMode.System,
        catalogVersion = "Development catalog",
        rulesetVersion = "1",
        catalogApproved = false,
        onBack = {},
        onLanguageChange = {},
        onThemeChange = {},
        onOpenPrivacy = {},
        onOpenSources = {},
        onEmailSupport = {},
        onClearRequest = {},
        onClearConfirm = {},
        onClearDismiss = {},
    )
}

@Preview(name = "SCREEN-011 Settings lower RU light", widthDp = 390, heightDp = 1000, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-011 Settings lower RU dark", widthDp = 390, heightDp = 1000, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "SCREEN-011 Settings lower RU 200 light", widthDp = 390, heightDp = 1000, fontScale = 2f, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun SettingsLowerContentPreview() = PreviewTheme(language = AppLanguage.Russian) {
    SettingsScreen(
        language = AppLanguage.Russian,
        themeMode = AppThemeMode.System,
        catalogVersion = "Каталог для разработки 2026.07",
        rulesetVersion = "Набор правил 1",
        catalogApproved = false,
        initialListIndex = 3,
        onBack = {},
        onLanguageChange = {},
        onThemeChange = {},
        onOpenPrivacy = {},
        onOpenSources = {},
        onEmailSupport = {},
        onClearRequest = {},
        onClearConfirm = {},
        onClearDismiss = {},
    )
}

@Preview(name = "SCREEN-011 Settings destructive RU light", widthDp = 390, heightDp = 700, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "SCREEN-011 Settings destructive RU dark", widthDp = 390, heightDp = 700, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsDestructivePreview() = PreviewTheme(language = AppLanguage.Russian) {
    SettingsScreen(
        language = AppLanguage.Russian,
        themeMode = AppThemeMode.System,
        catalogVersion = "Каталог для разработки 2026.07",
        rulesetVersion = "Набор правил 1",
        catalogApproved = false,
        initialListIndex = 5,
        onBack = {},
        onLanguageChange = {},
        onThemeChange = {},
        onOpenPrivacy = {},
        onOpenSources = {},
        onEmailSupport = {},
        onClearRequest = {},
        onClearConfirm = {},
        onClearDismiss = {},
    )
}

@Composable
private fun PreviewTheme(
    language: AppLanguage = AppLanguage.English,
    content: @Composable () -> Unit,
) = BulbMatchTheme(themeMode = AppThemeMode.System, language = language, content = content)

@Composable
private fun previewReview() = ReviewUiModel(
    fromOcr = true,
    unresolvedCount = 2,
    canAssess = false,
    fields = listOf(
        FieldUiModel(
            "base_code",
            tr("Base", "Цоколь"),
            "E27",
            "E27",
            FieldOrigin.Detected,
            ReviewDecision.Confirmed,
            true,
        ),
        FieldUiModel(
            "electrical_voltage",
            tr("Voltage", "Напряжение"),
            "220–240 V",
            "230 V",
            FieldOrigin.Detected,
            ReviewDecision.Unreviewed,
            true,
        ),
        FieldUiModel(
            "electrical_source_watts",
            tr("Source lamp power", "Мощность старой лампы"),
            "8 W",
            "8 W",
            FieldOrigin.Detected,
            ReviewDecision.Unreviewed,
        ),
        FieldUiModel(
            "light_lumens",
            tr("Luminous flux", "Световой поток"),
            "806 lm",
            "806 lm",
            FieldOrigin.Detected,
            ReviewDecision.Confirmed,
        ),
        FieldUiModel(
            "fixture_max_watts",
            tr("Fixture max wattage", "Макс. мощность светильника"),
            "",
            tr("Read from fixture label", "Смотрите маркировку светильника"),
            FieldOrigin.Manual,
            ReviewDecision.Confirmed,
        ),
    ),
)

private const val UI_MODE_NIGHT_NO = 0x10
private const val UI_MODE_NIGHT_YES = 0x20
