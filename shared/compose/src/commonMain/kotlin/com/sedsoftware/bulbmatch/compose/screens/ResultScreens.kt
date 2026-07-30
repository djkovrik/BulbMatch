package com.sedsoftware.bulbmatch.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.AppIcon
import com.sedsoftware.bulbmatch.compose.components.AppIcons
import com.sedsoftware.bulbmatch.compose.components.AppScreenScaffold
import com.sedsoftware.bulbmatch.compose.components.BulletText
import com.sedsoftware.bulbmatch.compose.components.DestructiveAction
import com.sedsoftware.bulbmatch.compose.components.KeyValueRow
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.components.MessageTone
import com.sedsoftware.bulbmatch.compose.components.OutcomeBanner
import com.sedsoftware.bulbmatch.compose.components.PrimaryAction
import com.sedsoftware.bulbmatch.compose.components.SectionCard
import com.sedsoftware.bulbmatch.compose.components.SectionTone
import com.sedsoftware.bulbmatch.compose.components.SecondaryAction
import com.sedsoftware.bulbmatch.compose.components.TertiaryAction
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.AssessmentOutcome
import com.sedsoftware.bulbmatch.compose.model.ResultUiModel
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing

@Composable
fun ReplacementResultScreen(
    model: ResultUiModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onMatchAnother: () -> Unit,
    onReference: () -> Unit,
    inlineAdContent: (@Composable () -> Unit)? = null,
) {
    AppScreenScaffold(
        title = tr("Replacement result", "Результат подбора"),
        modifier = modifier,
        onBack = onBack,
    ) { insets ->
        when (model.loadState) {
            ScreenLoadState.Loading -> Column(
                Modifier.fillMaxSize().padding(insets),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text(
                    tr("Preparing assessment…", "Готовим оценку…"),
                    modifier = Modifier.padding(LocalAppSpacing.current.md),
                )
            }
            ScreenLoadState.Error -> Column(
                Modifier.fillMaxSize().padding(insets).padding(LocalAppSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            ) {
                OutcomeBanner(AssessmentOutcome.Unavailable)
                MessageCard(
                    tr("No recommendation is shown", "Рекомендация не показана"),
                    model.errorMessage ?: tr(
                        "The catalog or ruleset could not be verified. Edit the details or check sources and support.",
                        "Не удалось проверить каталог или набор правил. Измените данные или откройте сведения об источниках и поддержке.",
                    ),
                    isError = true,
                )
                PrimaryAction(
                    tr("Edit details", "Изменить данные"),
                    onEdit,
                    leadingIcon = { AppIcon(AppIcons.Edit, contentDescription = null) },
                )
                SecondaryAction(
                    tr("Reference and sources", "Справочник и источники"),
                    onReference,
                    leadingIcon = { AppIcon(AppIcons.MenuBook, contentDescription = null) },
                )
            }
            else -> ResultEvidenceList(
                model = model,
                insets = insets,
                showActions = true,
                onEdit = onEdit,
                onSave = onSave,
                onMatchAnother = onMatchAnother,
                onReference = onReference,
                inlineAdContent = inlineAdContent,
            )
        }
    }
}

@Composable
private fun ResultEvidenceList(
    model: ResultUiModel,
    insets: PaddingValues,
    showActions: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onMatchAnother: () -> Unit,
    onReference: () -> Unit,
    historicalHeader: (@Composable () -> Unit)? = null,
    destructiveAction: (@Composable () -> Unit)? = null,
    inlineAdContent: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LocalAppSpacing.current.md,
            top = insets.calculateTopPadding() + LocalAppSpacing.current.sm,
            end = LocalAppSpacing.current.md,
            bottom = insets.calculateBottomPadding() + LocalAppSpacing.current.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        historicalHeader?.let { header -> item(key = "historical_header") { MaxWidthItem { header() } } }
        item(key = "outcome") {
            MaxWidthItem {
                OutcomeBanner(
                    model.outcome,
                    detail = tr(
                        "This is a shopping profile from confirmed markings, not certification of the fixture.",
                        "Это профиль для покупки по подтверждённой маркировке, а не сертификация светильника.",
                    ),
                )
            }
        }
        item(key = "electrical_warning") {
            MaxWidthItem {
                MessageCard(
                    tr("Electrical scope", "Электрические ограничения"),
                    tr(
                        "Only use this profile in 220–240 V / 50 Hz regions. Check the fixture label before buying or fitting a lamp.",
                        "Используйте этот профиль только в регионах 220–240 В / 50 Гц. Перед покупкой и установкой проверьте маркировку светильника.",
                    ),
                    isError = model.outcome == AssessmentOutcome.PotentialConflict,
                    tone = MessageTone.Warning,
                )
            }
        }
        item(key = "reasons") {
            MaxWidthItem {
                SectionCard(
                    tr("Confirmed facts and reasons", "Подтверждённые данные и причины"),
                    tone = SectionTone.Information,
                    icon = AppIcons.CheckCircle,
                ) {
                    model.confirmedFacts.forEach { fact ->
                        KeyValueRow(
                            fact.label,
                            fact.value,
                            supporting = fact.source?.let {
                                "${tr("Source", "Источник")}: ${it.localizedFactSource()}"
                            },
                        )
                    }
                    model.reasons.forEach { BulletText(it) }
                }
            }
        }
        item(key = "unresolved") {
            MaxWidthItem {
                SectionCard(
                    tr("Checks that remain", "Что ещё нужно проверить"),
                    tone = if (model.outcome == AssessmentOutcome.PotentialConflict) {
                        SectionTone.Conflict
                    } else {
                        SectionTone.Warning
                    },
                    icon = if (model.outcome == AssessmentOutcome.PotentialConflict) AppIcons.Cancel else AppIcons.Warning,
                ) {
                    if (model.unresolvedChecks.isEmpty()) {
                        Text(
                            tr(
                                "No marking conflicts remain. Fixture and installation facts are still outside this assessment.",
                                "Противоречий в маркировке нет. Данные светильника и установки всё равно не входят в эту оценку.",
                            ),
                        )
                    } else {
                        model.unresolvedChecks.forEach { BulletText(it) }
                    }
                    if (showActions) {
                        TertiaryAction(
                            tr("Edit details", "Изменить данные"),
                            onEdit,
                            leadingIcon = { AppIcon(AppIcons.Edit, contentDescription = null) },
                        )
                    }
                }
            }
        }
        item(key = "profile") {
            MaxWidthItem {
                SectionCard(
                    tr("Replacement profile", "Профиль замены"),
                    tone = SectionTone.Information,
                    icon = AppIcons.Lightbulb,
                ) {
                    if (model.profile.isEmpty()) {
                        Text(
                            tr(
                                "No shopping profile can be formed until the missing or conflicting facts are resolved.",
                                "Профиль для покупки нельзя сформировать, пока не устранены пропуски или противоречия.",
                            ),
                        )
                    } else {
                        model.profile.forEach { KeyValueRow(it.label, it.value, supporting = it.source) }
                    }
                }
            }
        }
        item(key = "checklist") {
            MaxWidthItem {
                SectionCard(
                    tr("Before you buy or fit", "Перед покупкой или установкой"),
                    tone = SectionTone.Information,
                    icon = AppIcons.Warning,
                ) {
                    model.checklist.forEach { BulletText(it) }
                    SecondaryAction(
                        tr("Open base reference", "Открыть справочник цоколей"),
                        onReference,
                        leadingIcon = { AppIcon(AppIcons.MenuBook, contentDescription = null) },
                    )
                }
            }
        }
        if (showActions) {
            item(key = "actions") {
                MaxWidthItem {
                    Column(verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm)) {
                        PrimaryAction(
                            tr("Save", "Сохранить"),
                            onSave,
                            leadingIcon = { AppIcon(AppIcons.Save, contentDescription = null) },
                        )
                        SecondaryAction(
                            tr("Match another", "Подобрать ещё"),
                            onMatchAnother,
                            leadingIcon = { AppIcon(AppIcons.RestartAlt, contentDescription = null) },
                        )
                    }
                }
            }
        }
        destructiveAction?.let { action ->
            item(key = "destructive") { MaxWidthItem { action() } }
        }
        if (
            showActions &&
            model.outcome == AssessmentOutcome.Compatible &&
            model.showInlineAd &&
            inlineAdContent != null
        ) {
            item(key = "result_ad") {
                MaxWidthItem { inlineAdContent() }
            }
        }
    }
}

@Composable
private fun MaxWidthItem(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().widthIn(max = 760.dp), content = { content() })
}

@Composable
fun SaveResultScreen(
    name: String,
    summary: String,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    error: String? = null,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AppScreenScaffold(
        title = tr("Save result", "Сохранить результат"),
        modifier = modifier,
        onBack = if (saving) null else onCancel,
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).imePadding().padding(LocalAppSpacing.current.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            ) {
                Text(
                    tr("Save this snapshot locally", "Сохранить этот снимок локально"),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.codePointLength() <= 80 && it.none(Char::isISOControl)) onNameChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving,
                    label = { Text(tr("Name (optional)", "Название (необязательно)")) },
                    supportingText = { Text(tr("Up to 80 characters", "До 80 символов")) },
                    singleLine = true,
                )
                MessageCard(
                    tr("Snapshot to save", "Снимок для сохранения"),
                    summary,
                    icon = AppIcons.Save,
                )
                if (error != null) {
                    MessageCard(
                        tr("Could not save", "Не удалось сохранить"),
                        error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        isError = true,
                    )
                }
                PrimaryAction(
                    if (saving) tr("Saving…", "Сохраняем…") else tr("Save", "Сохранить"),
                    onSave,
                    enabled = !saving,
                    leadingIcon = {
                        AppIcon(
                            if (error == null) AppIcons.Save else AppIcons.RestartAlt,
                            contentDescription = null,
                        )
                    },
                )
                TertiaryAction(
                    tr("Cancel", "Отмена"),
                    onCancel,
                    enabled = !saving,
                    leadingIcon = { AppIcon(AppIcons.Close, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
fun SavedResultDetailScreen(
    name: String,
    date: String,
    model: ResultUiModel,
    modifier: Modifier = Modifier,
    malformed: Boolean = false,
    showDeleteConfirmation: Boolean = false,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onDeleteConfirmed: () -> Unit = {},
    onDeleteDismissed: () -> Unit = {},
) {
    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text(tr("Delete saved result?", "Удалить сохранённый результат?")) },
            text = {
                Text(tr("This historical snapshot will be permanently removed.", "Этот исторический снимок будет удалён безвозвратно."))
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) { Text(tr("Delete", "Удалить")) }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismissed) { Text(tr("Cancel", "Отмена")) }
            },
        )
    }
    AppScreenScaffold(
        title = tr("Saved result", "Сохранённый результат"),
        modifier = modifier,
        onBack = onBack,
    ) { insets ->
        if (malformed) {
            Column(
                Modifier.fillMaxSize().padding(insets).padding(LocalAppSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            ) {
                MessageCard(
                    tr("Saved snapshot unavailable", "Сохранённый снимок недоступен"),
                    tr(
                        "This record cannot be decoded. No recommendation has been reconstructed from its summary.",
                        "Эту запись не удалось прочитать. Рекомендация не восстанавливалась по кратким данным.",
                    ),
                    isError = true,
                )
                DestructiveAction(
                    tr("Delete record", "Удалить запись"),
                    onDelete,
                    leadingIcon = { AppIcon(AppIcons.Delete, contentDescription = null) },
                )
            }
        } else {
            ResultEvidenceList(
                model = model.copy(showInlineAd = false),
                insets = insets,
                showActions = false,
                onEdit = {},
                onSave = {},
                onMatchAnother = {},
                onReference = {},
                historicalHeader = {
                    SectionCard(
                        tr("Historical snapshot", "Исторический снимок"),
                        tone = SectionTone.Information,
                        icon = AppIcons.History,
                    ) {
                        Text(name, style = MaterialTheme.typography.headlineSmall)
                        Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            tr(
                                "Shown exactly as saved; it is not recalculated.",
                                "Показан в сохранённом виде и не пересчитывается.",
                            ),
                        )
                        KeyValueRow(
                            tr("Catalog", "Каталог"),
                            model.catalogVersion,
                        )
                        KeyValueRow(
                            tr("Ruleset", "Набор правил"),
                            model.rulesetVersion,
                        )
                    }
                },
                destructiveAction = {
                    DestructiveAction(
                        tr("Delete saved result", "Удалить сохранённый результат"),
                        onDelete,
                        leadingIcon = { AppIcon(AppIcons.Delete, contentDescription = null) },
                    )
                },
                inlineAdContent = null,
            )
        }
    }
}

private fun String.codePointLength(): Int {
    var index = 0
    var count = 0
    while (index < length) {
        val first = this[index]
        index += if (first.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) 2 else 1
        count++
    }
    return count
}

@Composable
private fun String.localizedFactSource(): String = when (this) {
    "Manual" -> tr("Manual", "Вручную")
    "Edited" -> tr("Edited", "Изменено")
    "Detected" -> tr("Detected", "Распознано")
    else -> this
}
