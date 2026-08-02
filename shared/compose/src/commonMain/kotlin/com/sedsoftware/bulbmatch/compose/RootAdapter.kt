package com.sedsoftware.bulbmatch.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.sedsoftware.bulbmatch.app.CameraComponent
import com.sedsoftware.bulbmatch.app.CameraStatus
import com.sedsoftware.bulbmatch.app.HistoryComponent
import com.sedsoftware.bulbmatch.app.ImageReviewComponent
import com.sedsoftware.bulbmatch.app.MatchComponent
import com.sedsoftware.bulbmatch.app.MatchFormComponent
import com.sedsoftware.bulbmatch.app.ReferenceComponent
import com.sedsoftware.bulbmatch.app.ResultComponent
import com.sedsoftware.bulbmatch.app.RootComponent
import com.sedsoftware.bulbmatch.app.RootDestination as AppRootDestination
import com.sedsoftware.bulbmatch.app.SaveResultComponent
import com.sedsoftware.bulbmatch.app.SavedResultComponent
import com.sedsoftware.bulbmatch.app.SettingsComponent
import com.sedsoftware.bulbmatch.app.RecognitionFailure
import com.sedsoftware.bulbmatch.app.RecognitionState
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.AssessmentOutcome
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode
import com.sedsoftware.bulbmatch.compose.model.BaseReferenceUiModel
import com.sedsoftware.bulbmatch.compose.model.CameraState
import com.sedsoftware.bulbmatch.compose.model.FactUiModel
import com.sedsoftware.bulbmatch.compose.model.FieldOrigin
import com.sedsoftware.bulbmatch.compose.model.FieldUiModel
import com.sedsoftware.bulbmatch.compose.model.HistoryItemUiModel
import com.sedsoftware.bulbmatch.compose.model.ResultUiModel
import com.sedsoftware.bulbmatch.compose.model.ReviewDecision
import com.sedsoftware.bulbmatch.compose.model.ReviewUiModel
import com.sedsoftware.bulbmatch.compose.model.RootDestination
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.screens.BaseReferenceDetailScreen
import com.sedsoftware.bulbmatch.compose.screens.BaseReferenceListScreen
import com.sedsoftware.bulbmatch.compose.screens.CameraCaptureScreen
import com.sedsoftware.bulbmatch.compose.screens.DataReviewScreen
import com.sedsoftware.bulbmatch.compose.screens.HistoryConfirmation
import com.sedsoftware.bulbmatch.compose.screens.HistoryScreen
import com.sedsoftware.bulbmatch.compose.screens.ImageReviewScreen
import com.sedsoftware.bulbmatch.compose.screens.MatchHomeScreen
import com.sedsoftware.bulbmatch.compose.screens.ReplacementResultScreen
import com.sedsoftware.bulbmatch.compose.screens.SaveResultScreen
import com.sedsoftware.bulbmatch.compose.screens.SavedResultDetailScreen
import com.sedsoftware.bulbmatch.compose.screens.SettingsScreen
import com.sedsoftware.bulbmatch.compose.theme.BulbMatchTheme
import com.sedsoftware.bulbmatch.domain.AdvisoryCheck
import com.sedsoftware.bulbmatch.domain.Assessment
import com.sedsoftware.bulbmatch.domain.BaseCode
import com.sedsoftware.bulbmatch.domain.CatalogAvailability
import com.sedsoftware.bulbmatch.domain.CatalogEntry
import com.sedsoftware.bulbmatch.domain.ClarificationReason
import com.sedsoftware.bulbmatch.domain.ConflictReason
import com.sedsoftware.bulbmatch.domain.ConfirmedBase
import com.sedsoftware.bulbmatch.domain.ConfirmedMatchInput
import com.sedsoftware.bulbmatch.domain.Dimmability
import com.sedsoftware.bulbmatch.domain.ExplanationCode
import com.sedsoftware.bulbmatch.domain.FieldKey
import com.sedsoftware.bulbmatch.domain.LocaleOverride
import com.sedsoftware.bulbmatch.domain.SavedMatch
import com.sedsoftware.bulbmatch.domain.SavedMatchSummary
import com.sedsoftware.bulbmatch.domain.ThemeOverride
import com.sedsoftware.bulbmatch.domain.VoltageEvidence
import com.sedsoftware.bulbmatch.domain.AssessmentOutcome as DomainOutcome
import com.sedsoftware.bulbmatch.domain.FieldOrigin as DomainFieldOrigin

class BulbMatchSlots(
    val cameraPreview: (@Composable () -> Unit)? = null,
    val imagePreview: (@Composable (com.sedsoftware.bulbmatch.app.EphemeralImage) -> Unit)? = null,
    val resultBanner: (@Composable () -> Unit)? = null,
    val historyBanner: (@Composable () -> Unit)? = null,
    val referenceBanner: (@Composable () -> Unit)? = null,
)

@Composable
fun BulbMatchRoot(
    root: RootComponent,
    slots: BulbMatchSlots = BulbMatchSlots(),
    formatEpochMillis: (Long) -> String = { it.toString() },
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenSourceSummary: () -> Unit = {},
    onEmailSupport: () -> Unit = {},
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    val locale by root.localeOverride.subscribeAsState()
    val theme by root.themeOverride.subscribeAsState()
    var sourceSummaryVisible by remember { mutableStateOf(false) }
    BulbMatchTheme(
        themeMode = when (theme) {
            ThemeOverride.System -> AppThemeMode.System
            ThemeOverride.Light -> AppThemeMode.Light
            ThemeOverride.Dark -> AppThemeMode.Dark
        },
        language = when (locale) {
            LocaleOverride.English -> AppLanguage.English
            LocaleOverride.Russian -> AppLanguage.Russian
        },
        onThemeChanged = onThemeChanged,
    ) {
        BulbMatchRootContent(
            root = root,
            slots = slots,
            formatEpochMillis = formatEpochMillis,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenSourceSummary = {
                sourceSummaryVisible = true
                onOpenSourceSummary()
            },
            onEmailSupport = onEmailSupport,
        )
        if (sourceSummaryVisible) {
            AlertDialog(
                onDismissRequest = { sourceSummaryVisible = false },
                confirmButton = {
                    TextButton(onClick = { sourceSummaryVisible = false }) {
                        Text(tr("OK", "ОК"))
                    }
                },
                title = { Text(tr("Sources and licenses", "Источники и лицензии")) },
                text = {
                    Text(
                        tr(
                            "Catalog candidates and rules remain disabled until the exact " +
                                "version is approved by Sergey V. Source review covers IEC " +
                                "lamp-cap identifiers, EU light-source terminology, platform " +
                                "privacy documentation, bundled ML Kit OCR, Yandex Mobile Ads, " +
                                "and Firebase Crashlytics. No standards text or third-party " +
                                "diagrams are packaged.",
                            "Кандидаты каталога и правила отключены, пока точную версию не " +
                                "утвердит Sergey V. Проверяемые источники охватывают обозначения " +
                                "цоколей IEC, терминологию ЕС для источников света, документацию " +
                                "платформ о конфиденциальности, встроенное OCR ML Kit, Yandex " +
                                "Mobile Ads и Firebase Crashlytics. Тексты стандартов и сторонние " +
                                "схемы в приложение не включены.",
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun BulbMatchRootContent(
    root: RootComponent,
    slots: BulbMatchSlots,
    formatEpochMillis: (Long) -> String,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSourceSummary: () -> Unit,
    onEmailSupport: () -> Unit,
) {
    val settingsSlot by root.settingsSlot.subscribeAsState()
    val settings = settingsSlot.child?.instance
    if (settings != null) {
        SettingsContent(
            component = settings,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onOpenSourceSummary = onOpenSourceSummary,
            onEmailSupport = onEmailSupport,
        )
        return
    }

    val selected by root.selectedDestination.subscribeAsState()
    val selectRoot: (RootDestination) -> Unit = {
        root.selectDestination(
            when (it) {
                RootDestination.Match -> AppRootDestination.Match
                RootDestination.History -> AppRootDestination.History
                RootDestination.Reference -> AppRootDestination.Reference
            },
        )
    }
    when (selected) {
        AppRootDestination.Match -> MatchContent(root.match, root::openSettings, selectRoot, slots)
        AppRootDestination.History -> HistoryContent(root.history, root::openSettings, selectRoot, slots, formatEpochMillis)
        AppRootDestination.Reference -> ReferenceContent(root.reference, root::openSettings, selectRoot, slots)
    }
}

@Composable
private fun MatchContent(
    component: MatchComponent,
    onSettings: () -> Unit,
    onRoot: (RootDestination) -> Unit,
    slots: BulbMatchSlots,
) {
    val saveSlot by component.saveSlot.subscribeAsState()
    saveSlot.child?.instance?.let {
        SaveContent(it)
        return
    }
    val stack by component.stack.subscribeAsState()
    when (val child = stack.active.instance) {
        is MatchComponent.Child.Home -> {
            val model by child.component.model.subscribeAsState()
            MatchHomeScreen(
                catalogAvailable = model.catalogAvailability is CatalogAvailability.Available,
                cameraGuidance = model.cameraHint?.cameraHint(),
                unfinishedDraftMessage = model.draftLostNoticeVisible,
                onCamera = child.component::onCameraRequested,
                onChoosePhoto = child.component::onChoosePhotoRequested,
                onManual = child.component::onManualEntryRequested,
                onSettings = onSettings,
                onRootDestination = onRoot,
            )
        }
        is MatchComponent.Child.Camera -> CameraContent(child.component, slots.cameraPreview)
        is MatchComponent.Child.ImageReview -> ImageReviewContent(child.component, slots.imagePreview)
        is MatchComponent.Child.Form -> FormContent(child.component)
        is MatchComponent.Child.Result -> ResultContent(child.component, slots.resultBanner)
    }
}

@Composable
private fun CameraContent(component: CameraComponent, cameraPreview: (@Composable () -> Unit)?) {
    val model by component.model.subscribeAsState()
    val state = cameraScreenState(model)
    CameraCaptureScreen(
        state = state,
        captureInProgress = model.captureInProgress,
        failure = model.error,
        torchSupported = model.torchAvailable,
        torchEnabled = model.torchEnabled,
        onClose = component::onCloseRequested,
        onShutter = component::onShutterRequested,
        onToggleTorch = { component.onTorchChanged(!model.torchEnabled) },
        onTryAgain = component::onPermissionRequested,
        onOpenSettings = component::onOpenSystemSettingsRequested,
        onChoosePhoto = component::onChoosePhotoRequested,
        onManual = component::onManualEntryRequested,
        cameraPreview = cameraPreview,
    )
}

internal fun cameraScreenState(model: CameraComponent.Model): CameraState = when {
    model.requiresActiveCameraSession -> CameraState.Content
    model.error != null -> CameraState.Error
    else -> when (model.status) {
        CameraStatus.Unknown, CameraStatus.Checking -> CameraState.Opening
        CameraStatus.Granted -> CameraState.Content
        CameraStatus.DeniedCanAsk -> CameraState.DeniedCanAsk
        CameraStatus.DeniedOpenSettings -> CameraState.DeniedOpenSettings
        CameraStatus.Unavailable -> CameraState.Unavailable
    }
}

@Composable
private fun ImageReviewContent(
    component: ImageReviewComponent,
    imagePreview: (@Composable (com.sedsoftware.bulbmatch.app.EphemeralImage) -> Unit)?,
) {
    val model by component.model.subscribeAsState()
    val state = when (model.recognitionState) {
        RecognitionState.Content -> ScreenLoadState.Content
        RecognitionState.ReadingOnDevice -> ScreenLoadState.Loading
        is RecognitionState.Failed -> ScreenLoadState.Error
    }
    val error = (model.recognitionState as? RecognitionState.Failed)?.reason?.let {
        when (it) {
            RecognitionFailure.NoTextFound -> tr("No readable text was found.", "Читаемый текст не найден.")
            RecognitionFailure.UnsupportedImage -> tr("This image format cannot be decoded.", "Этот формат изображения не поддерживается.")
            RecognitionFailure.RecognitionFailed -> tr("On-device recognition failed.", "Ошибка распознавания на устройстве.")
            RecognitionFailure.UnreadableImage -> tr("The selected image is not readable.", "Выбранное изображение не читается.")
        }
    }
    ImageReviewScreen(
        state = state,
        errorMessage = error,
        onBack = component::onBackRequested,
        onUsePhoto = component::onUsePhotoRequested,
        onRetake = component::onRetakeRequested,
        onChooseAnother = component::onChooseAnotherRequested,
        onManual = component::onManualEntryRequested,
        onCancelRecognition = component::onRecognitionCancelled,
        previewContent = imagePreview?.let { preview -> { preview(model.image) } },
    )
}

@Composable
private fun FormContent(component: MatchFormComponent) {
    val model by component.model.subscribeAsState()
    val fields = FieldKey.entries.map { key ->
        val field = model.fields[key] ?: MatchFormComponent.FieldModel(
            rawValue = "",
            origin = null,
            reviewDecision = MatchFormComponent.ReviewDecision.NotObserved,
            required = key == FieldKey.Base || key == FieldKey.Voltage,
            validationErrorCode = null,
        )
        val validationCode = field.validationErrorCode
        val fieldError = if (validationCode != null) validationError(validationCode) else null
        FieldUiModel(
            id = key.uiId(),
            label = key.uiLabel(),
            value = field.rawValue,
            example = key.uiExample(),
            origin = when (field.origin) {
                DomainFieldOrigin.Detected -> FieldOrigin.Detected
                DomainFieldOrigin.Edited -> FieldOrigin.Edited
                DomainFieldOrigin.Manual, null -> FieldOrigin.Manual
            },
            decision = when (field.reviewDecision) {
                MatchFormComponent.ReviewDecision.Pending -> ReviewDecision.Unreviewed
                MatchFormComponent.ReviewDecision.Confirmed -> ReviewDecision.Confirmed
                MatchFormComponent.ReviewDecision.Rejected -> ReviewDecision.Rejected
                MatchFormComponent.ReviewDecision.NotObserved -> ReviewDecision.Confirmed
            },
            required = field.required,
            error = fieldError,
        )
    }
    DataReviewScreen(
        model = ReviewUiModel(
            fromOcr = model.mode == MatchFormComponent.Mode.OcrReview,
            fields = fields,
            unresolvedCount = model.unresolvedObservationKeys.size + model.unresolvedRequiredKeys.size,
            canAssess = model.canAssess,
        ),
        onBack = component::onBackRequested,
        onValueChange = { id, value ->
            FieldKey.entries.firstOrNull { it.uiId() == id }?.let { component.onFieldTextChanged(it, value) }
        },
        onDecision = { id, decision ->
            FieldKey.entries.firstOrNull { it.uiId() == id }?.let {
                when (decision) {
                    ReviewDecision.Confirmed -> component.onObservationConfirmed(it)
                    ReviewDecision.Rejected -> component.onObservationRejected(it)
                    ReviewDecision.Unreviewed -> Unit
                }
            }
        },
        onAssess = component::onAssessRequested,
        showDiscardConfirmation = model.discardConfirmationVisible,
        onDiscardConfirmed = component::onDiscardConfirmed,
        onDiscardDismissed = component::onDiscardCancelled,
    )
}

@Composable
private fun ResultContent(component: ResultComponent, banner: (@Composable () -> Unit)?) {
    val model by component.model.subscribeAsState()
    ReplacementResultScreen(
        model = model.assessment.toUiResult(showAd = banner != null),
        onBack = component::onExitRequested,
        onEdit = component::onEditDetailsRequested,
        onSave = component::onSaveRequested,
        onMatchAnother = component::onStartAnotherMatchRequested,
        onReference = component::onReferenceRequested,
        inlineAdContent = banner,
    )
}

@Composable
private fun SaveContent(component: SaveResultComponent) {
    val model by component.model.subscribeAsState()
    val saveErrorCode = model.errorCode
    val saveError = if (saveErrorCode != null) validationError(saveErrorCode) else null
    SaveResultScreen(
        name = model.name,
        summary = model.assessment.summaryLine(),
        saving = model.saving,
        error = saveError,
        onNameChange = component::onNameChanged,
        onSave = component::onSaveRequested,
        onCancel = component::onCancelRequested,
    )
}

@Composable
private fun HistoryContent(
    component: HistoryComponent,
    onSettings: () -> Unit,
    onRoot: (RootDestination) -> Unit,
    slots: BulbMatchSlots,
    formatEpochMillis: (Long) -> String,
) {
    val detailSlot by component.detailSlot.subscribeAsState()
    detailSlot.child?.instance?.let {
        SavedDetailContent(it, formatEpochMillis)
        return
    }
    val model by component.model.subscribeAsState()
    val confirmation = when {
        model.clearAllConfirmationVisible -> HistoryConfirmation.ClearAll
        model.pendingDelete != null -> HistoryConfirmation.DeleteOne(model.pendingDelete!!.value)
        else -> null
    }
    val state = when {
        model.loading -> ScreenLoadState.Loading
        model.readError -> ScreenLoadState.Error
        model.summaries.isEmpty() -> ScreenLoadState.Empty
        else -> ScreenLoadState.Content
    }
    HistoryScreen(
        state = state,
        items = model.summaries.map { it.toUi(formatEpochMillis) },
        confirmation = confirmation,
        onOpen = { id ->
            model.summaries.firstOrNull { it.id.value == id }?.let { component.onSavedMatchSelected(it.id) }
        },
        onDeleteRequest = { id ->
            model.summaries.firstOrNull { it.id.value == id }?.let { component.onDeleteRequested(it.id) }
        },
        onClearAllRequest = component::onClearAllRequested,
        onConfirmDestructive = {
            if (model.clearAllConfirmationVisible) component.onClearAllConfirmed() else component.onDeleteConfirmed()
        },
        onDismissConfirmation = {
            if (model.clearAllConfirmationVisible) component.onClearAllCancelled() else component.onDeleteCancelled()
        },
        onStartMatch = component::onStartMatchRequested,
        onRetry = component::onRetryRequested,
        onSettings = onSettings,
        onRootDestination = onRoot,
        stickyAdContent = slots.historyBanner,
    )
}

@Composable
private fun SavedDetailContent(component: SavedResultComponent, formatEpochMillis: (Long) -> String) {
    val model by component.model.subscribeAsState()
    val saved = model.savedMatch
    if (model.loading) {
        SavedResultDetailScreen(
            name = tr("Saved result", "Сохранённый результат"),
            date = "",
            model = unavailableUiResult(),
            malformed = true,
            onBack = component::onBackRequested,
            onDelete = component::onDeleteRequested,
        )
        return
    }
    SavedResultDetailScreen(
        name = saved?.displayName ?: tr("Saved match", "Сохранённый подбор"),
        date = saved?.createdAt?.value?.let(formatEpochMillis).orEmpty(),
        model = saved?.toUiResult() ?: unavailableUiResult(),
        malformed = model.unavailable || saved == null,
        showDeleteConfirmation = model.deleteConfirmationVisible,
        onBack = component::onBackRequested,
        onDelete = component::onDeleteRequested,
        onDeleteConfirmed = component::onDeleteConfirmed,
        onDeleteDismissed = component::onDeleteCancelled,
    )
}

@Composable
private fun ReferenceContent(
    component: ReferenceComponent,
    onSettings: () -> Unit,
    onRoot: (RootDestination) -> Unit,
    slots: BulbMatchSlots,
) {
    val detailSlot by component.detailSlot.subscribeAsState()
    detailSlot.child?.instance?.let { detail ->
        BaseReferenceDetailScreen(
            entry = detail.entry.toUi(),
            onBack = detail::onBackRequested,
            onUseBase = { detail.onUseBaseRequested() },
            stickyAdContent = slots.referenceBanner,
        )
        return
    }
    val model by component.model.subscribeAsState()
    var category by remember { mutableStateOf("all") }
    val entries = model.entries.filter {
        when (category) {
            "screw" -> it.code.value.startsWith("E")
            "pin" -> !it.code.value.startsWith("E")
            else -> true
        }
    }
    BaseReferenceListScreen(
        state = if (model.catalogAvailability is CatalogAvailability.Available) ScreenLoadState.Content else ScreenLoadState.Error,
        entries = entries.map(CatalogEntry::toUi),
        query = model.query,
        selectedCategory = category,
        onQueryChange = component::onQueryChanged,
        onCategoryChange = { category = it },
        onOpen = { id ->
            model.entries.firstOrNull { it.code.value == id }?.let { component.onEntrySelected(it.code) }
        },
        onClearSearch = component::onClearQueryRequested,
        onRetry = {},
        onSettings = onSettings,
        onRootDestination = onRoot,
        stickyAdContent = slots.referenceBanner,
    )
}

@Composable
private fun SettingsContent(
    component: SettingsComponent,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenSourceSummary: () -> Unit,
    onEmailSupport: () -> Unit,
) {
    val model by component.model.subscribeAsState()
    val catalog = (model.catalogAvailability as? CatalogAvailability.Available)?.catalog
    val invalidCatalog = model.catalogAvailability as? CatalogAvailability.Invalid
    val settingsErrorCode = model.errorCode
    val settingsError = if (settingsErrorCode != null) validationError(settingsErrorCode) else null
    SettingsScreen(
        language = when (model.locale) {
            LocaleOverride.English -> com.sedsoftware.bulbmatch.compose.model.AppLanguage.English
            LocaleOverride.Russian -> com.sedsoftware.bulbmatch.compose.model.AppLanguage.Russian
        },
        themeMode = when (model.theme) {
            ThemeOverride.System -> com.sedsoftware.bulbmatch.compose.model.AppThemeMode.System
            ThemeOverride.Light -> com.sedsoftware.bulbmatch.compose.model.AppThemeMode.Light
            ThemeOverride.Dark -> com.sedsoftware.bulbmatch.compose.model.AppThemeMode.Dark
        },
        catalogVersion = catalog?.snapshot?.catalogVersion
            ?: invalidCatalog?.catalogVersion
            ?: tr("Unavailable", "Недоступно"),
        rulesetVersion = catalog?.snapshot?.rulesetVersion
            ?: invalidCatalog?.rulesetVersion
            ?: tr("Unavailable", "Недоступно"),
        catalogApproved = catalog != null,
        showClearConfirmation = model.clearConfirmationVisible,
        message = settingsError,
        onBack = component::onBackRequested,
        onLanguageChange = {
            component.onLanguageSelected(
                when (it) {
                    com.sedsoftware.bulbmatch.compose.model.AppLanguage.English -> LocaleOverride.English
                    com.sedsoftware.bulbmatch.compose.model.AppLanguage.Russian -> LocaleOverride.Russian
                },
            )
        },
        onThemeChange = {
            component.onThemeSelected(
                when (it) {
                    com.sedsoftware.bulbmatch.compose.model.AppThemeMode.System -> ThemeOverride.System
                    com.sedsoftware.bulbmatch.compose.model.AppThemeMode.Light -> ThemeOverride.Light
                    com.sedsoftware.bulbmatch.compose.model.AppThemeMode.Dark -> ThemeOverride.Dark
                },
            )
        },
        onOpenPrivacy = onOpenPrivacyPolicy,
        onOpenSources = onOpenSourceSummary,
        onEmailSupport = onEmailSupport,
        onClearRequest = component::onClearLocalDataRequested,
        onClearConfirm = component::onClearLocalDataConfirmed,
        onClearDismiss = component::onClearLocalDataCancelled,
    )
}

@Composable
private fun Assessment.toUiResult(showAd: Boolean = false): ResultUiModel {
    val input = retainedConfirmedInput
    val reasons = when (this) {
        is Assessment.Compatible -> explanations.map { it.explanationText() }
        is Assessment.NeedClarification -> listOf(
            tr(
                "A compatible profile is not shown until every required fact is confirmed.",
                "Совместимый профиль не показывается, пока не подтверждены все обязательные данные.",
            ),
        )
        is Assessment.PotentialConflict -> listOf(
            tr(
                "A confirmed fact conflicts with the supported electrical scope or fixture limit.",
                "Подтверждённые данные противоречат поддерживаемому диапазону или ограничению светильника.",
            ),
        )
    }
    val unresolved = when (this) {
        is Assessment.Compatible -> advisoryChecks.map { it.advisoryText() }
        is Assessment.NeedClarification -> this.reasons.map { it.clarificationText() }
        is Assessment.PotentialConflict -> this.reasons.map { it.conflictText() }
    }
    val profileFacts = (this as? Assessment.Compatible)?.profile?.let { profile ->
        buildList {
            add(FactUiModel(tr("Base", "Цоколь"), profile.exactBase.value))
            add(FactUiModel(tr("Voltage", "Напряжение"), profile.requiredVoltage.format()))
            add(FactUiModel(tr("Frequency", "Частота"), "${profile.requiredFrequency.hertz.number()} Hz"))
            profile.sourceRatedPower?.let { add(FactUiModel(tr("Lamp power", "Мощность лампы"), "${it.value.number()} W")) }
            profile.printedEquivalentPower?.let {
                add(FactUiModel(tr("Printed equivalent", "Указанный эквивалент"), "${it.value.number()} W"))
            }
            profile.brightness?.let {
                val low = it.target.value * (100 - it.tolerancePercent) / 100
                val high = it.target.value * (100 + it.tolerancePercent) / 100
                add(FactUiModel(tr("Brightness", "Яркость"), "${low.number()}–${high.number()} lm"))
            }
            profile.colorTemperature?.let {
                add(FactUiModel(tr("Colour temperature", "Цветовая температура"), "${it.value.number()} K"))
            }
            profile.shape?.let { add(FactUiModel(tr("Shape", "Форма"), it.value)) }
            add(FactUiModel(tr("Dimmable", "Диммируемая"), profile.dimmability.uiText()))
            profile.fixtureMaximumPower?.let {
                add(FactUiModel(tr("Fixture max wattage", "Макс. мощность светильника"), "${it.watts.value.number()} W"))
            }
        }
    }.orEmpty()
    return ResultUiModel(
        outcome = when (this) {
            is Assessment.Compatible -> AssessmentOutcome.Compatible
            is Assessment.NeedClarification -> AssessmentOutcome.NeedClarification
            is Assessment.PotentialConflict -> AssessmentOutcome.PotentialConflict
        },
        confirmedFacts = input.confirmedFacts(),
        reasons = reasons,
        unresolvedChecks = unresolved,
        profile = profileFacts,
        checklist = safetyChecklist(),
        showInlineAd = showAd && this is Assessment.Compatible,
    )
}

@Composable
private fun ConfirmedMatchInput.confirmedFacts(): List<FactUiModel> = buildList {
    when (val baseValue = base) {
        is ConfirmedBase.Known -> add(FactUiModel(tr("Base", "Цоколь"), baseValue.code.value))
        is ConfirmedBase.Unknown -> add(FactUiModel(tr("Unknown base", "Неизвестный цоколь"), baseValue.rawText))
        ConfirmedBase.Missing -> Unit
    }
    when (val voltageValue = voltage) {
        is VoltageEvidence.Marking -> add(FactUiModel(tr("Voltage", "Напряжение"), voltageValue.value.format()))
        is VoltageEvidence.Contradictory -> add(FactUiModel(tr("Voltage", "Напряжение"), tr("Contradictory markings", "Противоречивые значения")))
        VoltageEvidence.Missing -> Unit
    }
    sourceRatedPower?.let { add(FactUiModel(tr("Source lamp power", "Мощность старой лампы"), "${it.value.number()} W")) }
    printedEquivalentPower?.let { add(FactUiModel(tr("Printed equivalent", "Указанный эквивалент"), "${it.value.number()} W")) }
    luminousFlux?.let { add(FactUiModel(tr("Light output", "Световой поток"), "${it.value.number()} lm")) }
    colorTemperature?.let { add(FactUiModel(tr("Colour temperature", "Цветовая температура"), "${it.value.number()} K")) }
    shape?.let { add(FactUiModel(tr("Shape", "Форма"), it.value)) }
    if (dimmability != Dimmability.Unknown) add(FactUiModel(tr("Dimmable", "Диммируемая"), dimmability.uiText()))
    fixtureMaximumPower?.let {
        add(FactUiModel(tr("Fixture max wattage", "Макс. мощность светильника"), "${it.watts.value.number()} W", tr("Manual", "Вручную")))
    }
}

@Composable
private fun Assessment.summaryLine(): String {
    val facts = retainedConfirmedInput.confirmedFacts().take(2).joinToString(" · ") { it.value }
    val outcome = when (this) {
        is Assessment.Compatible -> tr("Compatible profile", "Совместимый профиль")
        is Assessment.NeedClarification -> tr("Need clarification", "Нужно уточнение")
        is Assessment.PotentialConflict -> tr("Potential conflict", "Возможен конфликт")
    }
    return listOf(facts, outcome).filter(String::isNotBlank).joinToString(" · ")
}

@Composable
private fun SavedMatch.toUiResult(): ResultUiModel = assessment.toUiResult().copy(
    catalogVersion = catalogVersion,
    rulesetVersion = rulesetVersion,
)

@Composable
private fun SavedMatchSummary.toUi(formatEpochMillis: (Long) -> String) = HistoryItemUiModel(
    id = id.value,
    name = displayName,
    outcome = when (outcome) {
        DomainOutcome.Compatible -> AssessmentOutcome.Compatible
        DomainOutcome.NeedClarification -> AssessmentOutcome.NeedClarification
        DomainOutcome.PotentialConflict -> AssessmentOutcome.PotentialConflict
    },
    base = baseCode?.value ?: rawBaseText ?: tr("Not provided", "Не указано"),
    voltage = tr("Open snapshot to view", "Откройте снимок для просмотра"),
    date = formatEpochMillis(createdAt.value),
)

private fun CatalogEntry.toUi() = BaseReferenceUiModel(
    id = code.value,
    code = code.value,
    nameEn = commonNameEn,
    nameRu = commonNameRu,
    hintEn = distinguishingHintEn,
    hintRu = distinguishingHintRu,
    featuresEn = listOf(distinguishingHintEn),
    featuresRu = listOf(distinguishingHintRu),
    typicalUseEn = "Identification reference only. Typical use does not establish voltage.",
    typicalUseRu = "Только для идентификации. Типичное применение не определяет напряжение.",
)

@Composable
private fun FieldKey.uiLabel() = when (this) {
    FieldKey.Base -> tr("Base", "Цоколь")
    FieldKey.Voltage -> tr("Voltage", "Напряжение")
    FieldKey.Frequency -> tr("Frequency", "Частота")
    FieldKey.SourceRatedPower -> tr("Source lamp power", "Мощность старой лампы")
    FieldKey.PrintedEquivalentPower -> tr("Printed equivalent", "Указанный эквивалент")
    FieldKey.LuminousFlux -> tr("Luminous flux", "Световой поток")
    FieldKey.ColorTemperature -> tr("Colour temperature", "Цветовая температура")
    FieldKey.Shape -> tr("Shape", "Форма")
    FieldKey.Dimmability -> tr("Dimmable", "Диммируемая")
    FieldKey.FixtureMaximumPower -> tr("Fixture max wattage", "Макс. мощность светильника")
}

private fun FieldKey.uiId() = when (this) {
    FieldKey.Base -> "base_code"
    FieldKey.Voltage -> "electrical_voltage"
    FieldKey.Frequency -> "electrical_frequency"
    FieldKey.SourceRatedPower -> "electrical_source_watts"
    FieldKey.PrintedEquivalentPower -> "electrical_printed_equivalent"
    FieldKey.LuminousFlux -> "light_lumens"
    FieldKey.ColorTemperature -> "appearance_kelvin"
    FieldKey.Shape -> "appearance_shape"
    FieldKey.Dimmability -> "appearance_dimmable"
    FieldKey.FixtureMaximumPower -> "fixture_max_watts"
}

@Composable
private fun FieldKey.uiExample() = when (this) {
    FieldKey.Base -> "E27"
    FieldKey.Voltage -> tr("230 V or 220–240 V", "230 В или 220–240 В")
    FieldKey.Frequency -> "50 Hz"
    FieldKey.SourceRatedPower -> "8 W"
    FieldKey.PrintedEquivalentPower -> "60 W equivalent"
    FieldKey.LuminousFlux -> "806 lm"
    FieldKey.ColorTemperature -> "2700 K"
    FieldKey.Shape -> "A60"
    FieldKey.Dimmability -> tr("Yes / No / Unknown", "Да / Нет / Неизвестно")
    FieldKey.FixtureMaximumPower -> tr("Read from fixture label", "Возьмите с маркировки светильника")
}

@Composable
private fun ExplanationCode.explanationText() = when (this) {
    ExplanationCode.KnownBaseConfirmed -> tr("The confirmed base is supported by the reviewed catalog.", "Подтверждённый цоколь поддерживается проверенным каталогом.")
    ExplanationCode.VoltageInScope -> tr("The confirmed voltage is within the supported 220–240 V family.", "Подтверждённое напряжение входит в поддерживаемый диапазон 220–240 В.")
    ExplanationCode.FixtureLimitConfirmed -> tr("The manually entered fixture limit does not conflict with source lamp power.", "Введённое вручную ограничение светильника не конфликтует с мощностью старой лампы.")
    ExplanationCode.FixtureLimitUnresolved -> tr("No fixture maximum wattage was entered; it remains a separate check.", "Максимальная мощность светильника не указана и остаётся отдельной проверкой.")
}

@Composable
private fun ClarificationReason.clarificationText() = when (this) {
    is ClarificationReason.UnreviewedField -> tr("Review the detected ${field.name} value.", "Проверьте распознанное поле ${field.name}.")
    ClarificationReason.MissingBase -> tr("Base is required.", "Нужно указать цоколь.")
    is ClarificationReason.UnknownBase -> tr("The base “$rawText” is not confirmed in the catalog.", "Цоколь «$rawText» не подтверждён каталогом.")
    is ClarificationReason.UnsupportedBase -> tr("Base ${code.value} is not enabled in the reviewed catalog.", "Цоколь ${code.value} не включён в проверенный каталог.")
    ClarificationReason.MissingVoltage -> tr("A printed voltage marking is required.", "Нужно указать напряжение с маркировки.")
    ClarificationReason.AmbiguousVoltage -> tr("The voltage marking cannot be classified safely.", "Напряжение нельзя безопасно классифицировать.")
}

@Composable
private fun ConflictReason.conflictText() = when (this) {
    ConflictReason.ContradictoryVoltage -> tr("The confirmed voltage markings contradict each other.", "Подтверждённые значения напряжения противоречат друг другу.")
    ConflictReason.OutsideElectricalScope -> tr("The confirmed voltage is outside the supported 220–240 V supply family.", "Подтверждённое напряжение вне поддерживаемого диапазона 220–240 В.")
    ConflictReason.OutsideFrequencyScope -> tr(
        "The confirmed frequency is outside the supported 50 Hz scope.",
        "Подтверждённая частота не соответствует поддерживаемому значению 50 Гц.",
    )
    is ConflictReason.FixturePowerConflict -> tr(
        "Source lamp power ${sourcePower.value.number()} W exceeds the manually entered fixture limit ${fixtureMaximumPower.watts.value.number()} W.",
        "Мощность старой лампы ${sourcePower.value.number()} Вт превышает введённый предел светильника ${fixtureMaximumPower.watts.value.number()} Вт.",
    )
}

@Composable
private fun AdvisoryCheck.advisoryText() = when (this) {
    AdvisoryCheck.ThisDoesNotCertifyFixture -> tr("This result does not certify the fixture.", "Результат не сертифицирует светильник.")
    AdvisoryCheck.SwitchPowerOff -> tr("Switch power off before removing or fitting a lamp.", "Отключите питание перед снятием или установкой лампы.")
    AdvisoryCheck.CheckFixtureLabel -> tr("Check the fixture label and maximum wattage separately.", "Отдельно проверьте маркировку светильника и максимальную мощность.")
    AdvisoryCheck.VerifyDimensionsAndEnclosure -> tr("Verify physical size, enclosure, heat, moisture, and wiring suitability.", "Проверьте габариты, корпус, нагрев, влагу и состояние проводки.")
    AdvisoryCheck.VerifyDimmerRequirements -> tr("Verify dimmer compatibility where applicable.", "При необходимости проверьте совместимость с диммером.")
    AdvisoryCheck.BrightnessUnresolved -> tr("Brightness was not confirmed.", "Яркость не подтверждена.")
}

@Composable
private fun safetyChecklist() = listOf(
    tr("Switch power off before removing or fitting a lamp.", "Отключите питание перед снятием или установкой лампы."),
    tr("Check the fixture label and maximum wattage separately.", "Отдельно проверьте маркировку светильника и максимальную мощность."),
    tr("Confirm physical size, enclosure, dimmer, heat, moisture, and wiring suitability.", "Проверьте габариты, корпус, диммер, нагрев, влагу и состояние проводки."),
    tr("Ask a qualified person if anything is damaged, hot, wet, loose, or unclear.", "Обратитесь к специалисту, если что-либо повреждено, нагревается, мокнет, шатается или вызывает сомнения."),
)

@Composable
private fun Dimmability.uiText() = when (this) {
    Dimmability.Yes -> tr("Yes", "Да")
    Dimmability.No -> tr("No", "Нет")
    Dimmability.Unknown -> tr("Unknown", "Неизвестно")
}

@Composable
private fun CameraStatus.cameraHint(): String? = when (this) {
    CameraStatus.DeniedCanAsk -> tr("Camera access was denied. You can try again after choosing Camera.", "Доступ к камере отклонён. Повторите после выбора «Камера».")
    CameraStatus.DeniedOpenSettings -> tr("Camera access is off in system Settings.", "Доступ к камере отключён в системных настройках.")
    CameraStatus.Unavailable -> tr("No camera is available; choose a photo or enter manually.", "Камера недоступна: выберите фото или введите данные вручную.")
    else -> null
}

@Composable
private fun validationError(code: String): String = when (code) {
    "required" -> tr("This field is required.", "Обязательное поле.")
    "invalid" -> tr("Check the printed value and unit.", "Проверьте значение и единицу измерения.")
    else -> tr("Could not complete this local operation ($code).", "Не удалось выполнить локальную операцию ($code).")
}

private fun com.sedsoftware.bulbmatch.domain.VoltageMarking.format(): String =
    if (minimumVolts == maximumVolts) "${minimumVolts.number()} V"
    else "${minimumVolts.number()}–${maximumVolts.number()} V"

private fun Double.number(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Composable
private fun unavailableUiResult() = ResultUiModel(
    outcome = AssessmentOutcome.Unavailable,
    confirmedFacts = emptyList(),
    reasons = emptyList(),
    unresolvedChecks = emptyList(),
    profile = emptyList(),
    checklist = safetyChecklist(),
    loadState = ScreenLoadState.Error,
)
