package com.sedsoftware.bulbmatch.compose.previews

import androidx.compose.runtime.Composable
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

@Preview(name = "SCREEN-001 Match EN", widthDp = 390, heightDp = 844)
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

@Preview(name = "SCREEN-002 Permission RU", widthDp = 390, heightDp = 844)
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

@Preview(name = "SCREEN-003 Processing dark", widthDp = 390, heightDp = 844)
@Composable
private fun ImageProcessingPreview() = PreviewTheme(theme = AppThemeMode.Dark) {
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

@Preview(name = "SCREEN-004 OCR review", widthDp = 390, heightDp = 844)
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

@Preview(name = "SCREEN-005 Compatible", widthDp = 390, heightDp = 1000)
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

@Preview(name = "SCREEN-005 Conflict RU dark", widthDp = 390, heightDp = 1000)
@Composable
private fun ConflictResultPreview() = PreviewTheme(AppThemeMode.Dark, AppLanguage.Russian) {
    ReplacementResultScreen(
        previewCompatibleResult().copy(
            outcome = AssessmentOutcome.PotentialConflict,
            reasons = listOf("Confirmed voltage is outside the supported regional supply family."),
            unresolvedChecks = listOf("The lamp is marked 110–120 V. Do not use this shopping profile on 220–240 V supply."),
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

@Preview(name = "SCREEN-006 Save error", widthDp = 390, heightDp = 700)
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

@Preview(name = "SCREEN-007 History", widthDp = 700, heightDp = 900)
@Composable
private fun HistoryPreview() = PreviewTheme {
    HistoryScreen(
        state = ScreenLoadState.Content,
        items = listOf(
            HistoryItemUiModel("1", null, AssessmentOutcome.Compatible, "E27", "220–240 V", "29 Jul 2026"),
            HistoryItemUiModel("2", "Kitchen", AssessmentOutcome.NeedClarification, "Unknown GX…", "Not provided", "27 Jul 2026"),
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

@Preview(name = "SCREEN-008 Saved detail", widthDp = 390, heightDp = 1000)
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

@Preview(name = "SCREEN-009 Reference", widthDp = 700, heightDp = 900)
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

@Preview(name = "SCREEN-010 Reference detail RU", widthDp = 390, heightDp = 900)
@Composable
private fun ReferenceDetailPreview() = PreviewTheme(language = AppLanguage.Russian) {
    BaseReferenceDetailScreen(
        PreviewBases.first(),
        onBack = {},
        onUseBase = {},
        stickyAdContent = { AdvertisementSlot(true) },
    )
}

@Preview(name = "SCREEN-011 Settings dark", widthDp = 390, heightDp = 1000)
@Composable
private fun SettingsPreview() = PreviewTheme(theme = AppThemeMode.Dark) {
    SettingsScreen(
        language = AppLanguage.System,
        themeMode = AppThemeMode.Dark,
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

@Composable
private fun PreviewTheme(
    theme: AppThemeMode = AppThemeMode.Light,
    language: AppLanguage = AppLanguage.English,
    content: @Composable () -> Unit,
) = BulbMatchTheme(themeMode = theme, language = language, content = content)

private fun previewReview() = ReviewUiModel(
    fromOcr = true,
    unresolvedCount = 2,
    canAssess = false,
    fields = listOf(
        FieldUiModel("base_code", "Base", "E27", "E27", FieldOrigin.Detected, ReviewDecision.Confirmed, true),
        FieldUiModel("electrical_voltage", "Voltage", "220–240 V", "230 V", FieldOrigin.Detected, ReviewDecision.Unreviewed, true),
        FieldUiModel("electrical_source_watts", "Source lamp power", "8 W", "8 W", FieldOrigin.Detected, ReviewDecision.Unreviewed),
        FieldUiModel("light_lumens", "Luminous flux", "806 lm", "806 lm", FieldOrigin.Detected, ReviewDecision.Confirmed),
        FieldUiModel("fixture_max_watts", "Fixture max wattage", "", "Read from fixture label", FieldOrigin.Manual, ReviewDecision.Confirmed),
    ),
)
