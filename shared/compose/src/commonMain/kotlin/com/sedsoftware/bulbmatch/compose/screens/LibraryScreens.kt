package com.sedsoftware.bulbmatch.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.AppIcon
import com.sedsoftware.bulbmatch.compose.components.AppIconButton
import com.sedsoftware.bulbmatch.compose.components.AppIcons
import com.sedsoftware.bulbmatch.compose.components.AppScreenScaffold
import com.sedsoftware.bulbmatch.compose.components.BaseDiagram
import com.sedsoftware.bulbmatch.compose.components.BulletText
import com.sedsoftware.bulbmatch.compose.components.KeyValueRow
import com.sedsoftware.bulbmatch.compose.components.LoadingOrErrorState
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.components.MessageTone
import com.sedsoftware.bulbmatch.compose.components.PrimaryAction
import com.sedsoftware.bulbmatch.compose.components.SectionCard
import com.sedsoftware.bulbmatch.compose.components.SecondaryAction
import com.sedsoftware.bulbmatch.compose.localization.LocalAppLanguage
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AssessmentOutcome
import com.sedsoftware.bulbmatch.compose.model.BaseReferenceUiModel
import com.sedsoftware.bulbmatch.compose.model.HistoryItemUiModel
import com.sedsoftware.bulbmatch.compose.model.RootDestination
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing

@Composable
fun HistoryScreen(
    state: ScreenLoadState,
    items: List<HistoryItemUiModel>,
    modifier: Modifier = Modifier,
    confirmation: HistoryConfirmation? = null,
    onOpen: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onClearAllRequest: () -> Unit,
    onConfirmDestructive: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onStartMatch: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onRootDestination: (RootDestination) -> Unit,
    stickyAdContent: (@Composable () -> Unit)? = null,
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            title = {
                Text(
                    if (confirmation is HistoryConfirmation.ClearAll) {
                        tr("Clear all local data?", "Очистить все локальные данные?")
                    } else tr("Delete saved result?", "Удалить сохранённый результат?")
                )
            },
            text = {
                Text(
                    if (confirmation is HistoryConfirmation.ClearAll) {
                        tr(
                            "Saved results and local ad-frequency counters will be removed. Language and theme stay unchanged.",
                            "Сохранённые результаты и локальные счётчики частоты рекламы будут удалены. Язык и тема сохранятся.",
                        )
                    } else tr(
                        "This saved snapshot will be permanently removed.",
                        "Этот сохранённый снимок будет удалён безвозвратно.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDestructive) { Text(tr("Delete", "Удалить")) }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmation) { Text(tr("Cancel", "Отмена")) }
            },
        )
    }
    AppScreenScaffold(
        title = tr("History", "История"),
        modifier = modifier,
        onSettings = onSettings,
        topAction = {
            Box {
                AppIconButton(
                    resource = AppIcons.MoreVert,
                    contentDescription = tr("More actions", "Дополнительные действия"),
                    onClick = { overflowExpanded = true },
                    enabled = items.isNotEmpty(),
                )
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(tr("Clear all", "Очистить всё")) },
                        leadingIcon = { AppIcon(AppIcons.Delete, contentDescription = null) },
                        onClick = {
                            overflowExpanded = false
                            onClearAllRequest()
                        },
                    )
                }
            }
        },
        rootDestination = RootDestination.History,
        onRootDestination = onRootDestination,
        stickyAd = if (confirmation == null) {
            stickyAdContent
        } else null,
    ) { insets ->
        LoadingOrErrorState(
            state = if (state == ScreenLoadState.Empty) ScreenLoadState.Content else state,
            modifier = Modifier.padding(insets),
            emptyTitle = tr("Saved results appear here", "Здесь появятся сохранённые результаты"),
            emptyBody = tr(
                "Save a completed assessment to reopen it later.",
                "Сохраните завершённую оценку, чтобы вернуться к ней позже.",
            ),
            errorTitle = tr("History unavailable", "История недоступна"),
            errorBody = tr(
                "The local saved-results database could not be read.",
                "Не удалось прочитать локальную базу сохранённых результатов.",
            ),
            onRetry = onRetry,
        ) {
            if (items.isEmpty()) {
                EmptyHistory(insets, onStartMatch)
            } else {
                AdaptiveHistoryGrid(insets, items, onOpen, onDeleteRequest)
            }
        }
    }
}

sealed interface HistoryConfirmation {
    data class DeleteOne(val id: String) : HistoryConfirmation
    data object ClearAll : HistoryConfirmation
}

@Composable
private fun EmptyHistory(insets: PaddingValues, onStartMatch: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(LocalAppSpacing.current.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        ) {
            AppIcon(
                AppIcons.History,
                contentDescription = null,
                modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp),
            )
            Text(
                tr("Saved results appear here", "Здесь появятся сохранённые результаты"),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                tr("Save a completed assessment to reopen it later.", "Сохраните завершённую оценку, чтобы вернуться к ней позже."),
            )
            PrimaryAction(
                tr("Start a match", "Начать подбор"),
                onStartMatch,
                leadingIcon = { AppIcon(AppIcons.Lightbulb, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun AdaptiveHistoryGrid(
    insets: PaddingValues,
    history: List<HistoryItemUiModel>,
    onOpen: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 700.dp) 2 else 1
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LocalAppSpacing.current.md,
                top = insets.calculateTopPadding() + LocalAppSpacing.current.sm,
                end = LocalAppSpacing.current.md,
                bottom = insets.calculateBottomPadding() + LocalAppSpacing.current.md,
            ),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        ) {
            items(history, key = { "history_${it.id}" }) { item ->
                HistoryCard(item, { onOpen(item.id) }, { onDeleteRequest(item.id) })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItemUiModel,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val defaultName = tr("Saved match", "Сохранённый подбор")
    val status = outcomeText(item.outcome)
    val deleteDescription = tr("Delete saved result", "Удалить сохранённый результат")
    Card(
        modifier = Modifier.fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = deleteDescription,
                        action = { onDelete(); true },
                    ),
                )
            }
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(LocalAppSpacing.current.md),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.name?.takeIf(String::isNotBlank) ?: defaultName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                AppIcon(AppIcons.ChevronRight, contentDescription = null)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    outcomeIcon(item.outcome),
                    contentDescription = null,
                    modifier = Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp),
                )
                Text(status, style = MaterialTheme.typography.labelLarge, color = outcomeColor(item.outcome))
            }
            KeyValueRow(tr("Base", "Цоколь"), item.base, valueMaxLines = 2)
            KeyValueRow(tr("Voltage", "Напряжение"), item.voltage)
            Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    AppIcon(AppIcons.Delete, contentDescription = null)
                    Spacer(Modifier.width(LocalAppSpacing.current.xs))
                    Text(tr("Delete", "Удалить"))
                }
                TextButton(onClick = onOpen, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    AppIcon(AppIcons.ChevronRight, contentDescription = null)
                    Spacer(Modifier.width(LocalAppSpacing.current.xs))
                    Text(tr("Open", "Открыть"))
                }
            }
        }
    }
}

@Composable
private fun outcomeText(outcome: AssessmentOutcome) = when (outcome) {
    AssessmentOutcome.Compatible -> tr("Compatible profile", "Совместимый профиль")
    AssessmentOutcome.NeedClarification -> tr("Need clarification", "Нужно уточнение")
    AssessmentOutcome.PotentialConflict -> tr("Potential conflict", "Возможен конфликт")
    AssessmentOutcome.Unavailable -> tr("Unavailable", "Недоступно")
}

private fun outcomeIcon(outcome: AssessmentOutcome) = when (outcome) {
    AssessmentOutcome.Compatible -> AppIcons.CheckCircle
    AssessmentOutcome.NeedClarification -> AppIcons.Warning
    AssessmentOutcome.PotentialConflict -> AppIcons.Cancel
    AssessmentOutcome.Unavailable -> AppIcons.Error
}

@Composable
private fun outcomeColor(outcome: AssessmentOutcome) = when (outcome) {
    AssessmentOutcome.Compatible -> com.sedsoftware.bulbmatch.compose.theme.LocalStatusColors.current.success
    AssessmentOutcome.NeedClarification -> com.sedsoftware.bulbmatch.compose.theme.LocalStatusColors.current.warning
    AssessmentOutcome.PotentialConflict, AssessmentOutcome.Unavailable ->
        com.sedsoftware.bulbmatch.compose.theme.LocalStatusColors.current.conflict
}

@Composable
fun BaseReferenceListScreen(
    state: ScreenLoadState,
    entries: List<BaseReferenceUiModel>,
    query: String,
    selectedCategory: String = "all",
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onRootDestination: (RootDestination) -> Unit,
    stickyAdContent: (@Composable () -> Unit)? = null,
) {
    AppScreenScaffold(
        title = tr("Base reference", "Справочник цоколей"),
        modifier = modifier,
        onSettings = onSettings,
        rootDestination = RootDestination.Reference,
        onRootDestination = onRootDestination,
        stickyAd = stickyAdContent,
    ) { insets ->
        LoadingOrErrorState(
            state,
            Modifier.padding(insets),
            tr("No bases found", "Цоколи не найдены"),
            tr("Clear the search or browse all entries.", "Очистите поиск или просмотрите все записи."),
            tr("Reference unavailable", "Справочник недоступен"),
            tr(
                "The bundled catalog failed its integrity check. No entries are guessed or downloaded.",
                "Встроенный каталог не прошёл проверку. Записи не угадываются и не загружаются из сети.",
            ),
            onRetry,
        ) {
            Column(Modifier.fillMaxSize().padding(insets)) {
                Column(
                    Modifier.fillMaxWidth().padding(LocalAppSpacing.current.md),
                    verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("Search code or name", "Поиск по коду или названию")) },
                        leadingIcon = { AppIcon(AppIcons.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotBlank()) {
                            {
                                AppIconButton(
                                    resource = AppIcons.Clear,
                                    contentDescription = tr("Clear search", "Очистить поиск"),
                                    onClick = onClearSearch,
                                )
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
                    ) {
                        listOf(
                            "all" to tr("All", "Все"),
                            "screw" to tr("Screw", "Резьбовые"),
                            "pin" to tr("Pin", "Штырьковые"),
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = selectedCategory == id,
                                onClick = { onCategoryChange(id) },
                                label = { Text(label) },
                                modifier = Modifier.sizeIn(minHeight = 48.dp),
                            )
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.padding(LocalAppSpacing.current.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
                        ) {
                            MessageCard(
                                tr("No search results", "Ничего не найдено"),
                                tr(
                                    "This does not mean the base is unsupported until you explicitly choose Unknown base in Match.",
                                    "Это не означает, что цоколь не поддерживается, пока вы явно не выберете «Неизвестный цоколь» в подборе.",
                                ),
                            )
                            SecondaryAction(
                                tr("Clear search", "Очистить поиск"),
                                onClearSearch,
                                leadingIcon = { AppIcon(AppIcons.Clear, contentDescription = null) },
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (maxWidth >= 700.dp) 2 else 1),
                            contentPadding = PaddingValues(LocalAppSpacing.current.md),
                            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
                            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
                        ) {
                            items(entries, key = { "base_${it.id}" }) { entry ->
                                BaseReferenceCard(entry, { onOpen(entry.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseReferenceCard(entry: BaseReferenceUiModel, onOpen: () -> Unit) {
    val isRu = LocalAppLanguage.current == AppLanguage.Russian
    val name = if (isRu) entry.nameRu else entry.nameEn
    val hint = if (isRu) entry.hintRu else entry.hintEn
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(LocalAppSpacing.current.md),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaseDiagram(entry.code, hint, Modifier.weight(.42f))
            Column(Modifier.weight(.52f), verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm)) {
                Text(entry.code, style = MaterialTheme.typography.headlineSmall)
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(hint, style = MaterialTheme.typography.bodyMedium)
                Text(
                    tr("Informational only", "Только справочная информация"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIcon(AppIcons.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun BaseReferenceDetailScreen(
    entry: BaseReferenceUiModel?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onUseBase: (String) -> Unit,
    stickyAdContent: (@Composable () -> Unit)? = null,
) {
    AppScreenScaffold(
        title = tr("Base details", "Описание цоколя"),
        modifier = modifier,
        onBack = onBack,
        stickyAd = stickyAdContent,
    ) { insets ->
        if (entry == null) {
            Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                MessageCard(
                    tr("Entry unavailable", "Запись недоступна"),
                    tr("Return to the reference list and choose another entry.", "Вернитесь к списку и выберите другую запись."),
                    Modifier.padding(LocalAppSpacing.current.md),
                    isError = true,
                )
            }
            return@AppScreenScaffold
        }
        val isRu = LocalAppLanguage.current == AppLanguage.Russian
        val name = if (isRu) entry.nameRu else entry.nameEn
        val hint = if (isRu) entry.hintRu else entry.hintEn
        val features = if (isRu) entry.featuresRu else entry.featuresEn
        val typical = if (isRu) entry.typicalUseRu else entry.typicalUseEn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LocalAppSpacing.current.md,
                top = insets.calculateTopPadding() + LocalAppSpacing.current.sm,
                end = LocalAppSpacing.current.md,
                bottom = insets.calculateBottomPadding() + LocalAppSpacing.current.md,
            ),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
                ) {
                    Text(entry.code, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.displaySmall)
                    Text(name, style = MaterialTheme.typography.headlineSmall)
                    BaseDiagram(entry.code, hint)
                    SectionCard(tr("Distinguishing features", "Отличительные признаки")) {
                        features.forEach { BulletText(it) }
                    }
                    SectionCard(tr("Typical use", "Типичное применение")) {
                        Text(typical)
                    }
                    MessageCard(
                        tr("Check the printed voltage", "Проверьте указанное напряжение"),
                        tr(
                            "Base shape does not establish voltage, power, physical clearance, or fixture suitability.",
                            "Форма цоколя не определяет напряжение, мощность, габариты или пригодность светильника.",
                        ),
                        tone = MessageTone.Warning,
                    )
                    PrimaryAction(
                        tr("Use this base", "Использовать этот цоколь"),
                        { onUseBase(entry.code) },
                        leadingIcon = { AppIcon(AppIcons.CheckCircle, contentDescription = null) },
                    )
                }
            }
        }
    }
}
