package com.sedsoftware.bulbmatch.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.AppIcon
import com.sedsoftware.bulbmatch.compose.components.AppIcons
import com.sedsoftware.bulbmatch.compose.components.AppScreenScaffold
import com.sedsoftware.bulbmatch.compose.components.BulletText
import com.sedsoftware.bulbmatch.compose.components.DestructiveAction
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.components.SectionCard
import com.sedsoftware.bulbmatch.compose.components.SectionTone
import com.sedsoftware.bulbmatch.compose.localization.displayName
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun SettingsScreen(
    language: AppLanguage,
    themeMode: AppThemeMode,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    catalogVersion: String,
    rulesetVersion: String,
    catalogApproved: Boolean,
    showClearConfirmation: Boolean = false,
    message: String? = null,
    initialListIndex: Int = 0,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSources: () -> Unit,
    onEmailSupport: () -> Unit,
    onClearRequest: () -> Unit,
    onClearConfirm: () -> Unit,
    onClearDismiss: () -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex)
    val clearDescription = tr(
        "Clear local data. Destructive action.",
        "Очистить локальные данные. Опасное действие.",
    )
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = onClearDismiss,
            title = { Text(tr("Clear local data?", "Очистить локальные данные?")) },
            text = {
                Text(
                    tr(
                        "Saved results, drafts, and local ad-frequency counters will be removed. Language and theme remain unchanged.",
                        "Сохранённые результаты, черновики и локальные счётчики частоты рекламы будут удалены. Язык и тема сохранятся.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onClearConfirm) { Text(tr("Clear", "Очистить")) }
            },
            dismissButton = {
                TextButton(onClick = onClearDismiss) { Text(tr("Cancel", "Отмена")) }
            },
        )
    }
    AppScreenScaffold(
        title = tr("Settings and privacy", "Настройки и конфиденциальность"),
        modifier = modifier,
        onBack = onBack,
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = LocalAppSpacing.current.md,
                top = insets.calculateTopPadding() + LocalAppSpacing.current.sm,
                end = LocalAppSpacing.current.md,
                bottom = insets.calculateBottomPadding() + LocalAppSpacing.current.lg,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        ) {
            if (message != null) {
                item(key = "message") {
                    SettingsWidth { MessageCard(tr("Notice", "Сообщение"), message) }
                }
            }
            item(key = "language") {
                SettingsWidth {
                    SectionCard(
                        tr("Language", "Язык"),
                        icon = AppIcons.Language,
                    ) {
                        AppLanguage.entries.forEach { candidate ->
                            SelectionRow(
                                label = candidate.displayName(),
                                selected = language == candidate,
                                onClick = { onLanguageChange(candidate) },
                            )
                        }
                    }
                }
            }
            item(key = "theme") {
                SettingsWidth {
                    SectionCard(
                        tr("Theme", "Тема"),
                        icon = AppIcons.Palette,
                    ) {
                        AppThemeMode.entries.forEach { candidate ->
                            val label = when (candidate) {
                                AppThemeMode.System -> tr("System", "Системная")
                                AppThemeMode.Light -> tr("Light", "Светлая")
                                AppThemeMode.Dark -> tr("Dark", "Тёмная")
                            }
                            SelectionRow(label, themeMode == candidate) { onThemeChange(candidate) }
                        }
                    }
                }
            }
            item(key = "privacy") {
                SettingsWidth {
                    SectionCard(
                        tr("Privacy and network services", "Конфиденциальность и сетевые сервисы"),
                        tone = SectionTone.Information,
                        icon = AppIcons.Shield,
                    ) {
                        BulletText(
                            tr(
                                "Photos and OCR text stay on device and are discarded when the in-memory flow ends.",
                                "Фото и текст распознавания остаются на устройстве и удаляются после завершения текущего процесса.",
                            ),
                        )
                        BulletText(
                            tr(
                                "Firebase Crashlytics receives crash diagnostics without photos, OCR text, confirmed values, names, or a stable user ID.",
                                "Firebase Crashlytics получает диагностику сбоев без фото, текста OCR, подтверждённых значений, названий и постоянного ID пользователя.",
                            ),
                        )
                        BulletText(
                            tr(
                                "Yandex Mobile Ads is configured without location, custom targeting, ATT prompt, or an advertising-ID permission.",
                                "Yandex Mobile Ads настроен без геолокации, пользовательского таргетинга, запроса ATT и разрешения на рекламный идентификатор.",
                            ),
                        )
                        LinkRow(
                            tr("Privacy policy", "Политика конфиденциальности"),
                            if (offline) tr("Internet required", "Требуется интернет") else "sedsoftware.com",
                            enabled = !offline,
                            onClick = onOpenPrivacy,
                            trailingIcon = AppIcons.OpenInNew,
                        )
                    }
                }
            }
            item(key = "catalog") {
                SettingsWidth {
                    SectionCard(
                        tr("Catalog and sources", "Каталог и источники"),
                        tone = SectionTone.Information,
                        icon = AppIcons.Source,
                    ) {
                        InfoRow(tr("Catalog version", "Версия каталога"), catalogVersion)
                        InfoRow(tr("Ruleset version", "Версия правил"), rulesetVersion)
                        MessageCard(
                            if (catalogApproved) tr("Reviewed catalog", "Каталог проверен")
                            else tr("Release approval pending", "Ожидается подтверждение для выпуска"),
                            if (catalogApproved) {
                                tr("This packaged catalog has recorded human approval.", "Для этого каталога зафиксировано подтверждение человеком.")
                            } else {
                                tr(
                                    "Development candidates are not enabled for production assessment until Sergey V. records approval.",
                                    "Кандидаты для разработки не включаются в производственную оценку, пока Sergey V. не зафиксирует подтверждение.",
                                )
                            },
                            isError = !catalogApproved,
                        )
                        LinkRow(
                            tr("Open packaged source summary", "Открыть сведения об источниках"),
                            catalogVersion,
                            enabled = true,
                            onClick = onOpenSources,
                            trailingIcon = AppIcons.ChevronRight,
                        )
                    }
                }
            }
            item(key = "safety") {
                SettingsWidth {
                    SectionCard(
                        tr("Safety disclaimer", "Предупреждение о безопасности"),
                        tone = SectionTone.Warning,
                        icon = AppIcons.Warning,
                    ) {
                        Text(
                            tr(
                                "BulbMatch creates a conservative shopping profile from confirmed markings. It does not inspect wiring, certify a fixture, or guarantee physical fit.",
                                "BulbMatch создаёт консервативный профиль для покупки по подтверждённой маркировке. Приложение не проверяет проводку, не сертифицирует светильник и не гарантирует физическую совместимость.",
                            ),
                        )
                        Text(
                            tr(
                                "Switch power off and ask a qualified person when damage, heat, moisture, enclosure, dimmer support, or wiring is uncertain.",
                                "Отключите питание и обратитесь к специалисту при повреждении, нагреве, влаге или сомнениях в корпусе, диммере либо проводке.",
                            ),
                        )
                    }
                }
            }
            item(key = "about") {
                SettingsWidth {
                    SectionCard(
                        tr("About and support", "О приложении и поддержка"),
                        icon = AppIcons.Mail,
                    ) {
                        InfoRow(tr("Publisher", "Издатель"), "Sergey V.")
                        LinkRow(
                            tr("Support", "Поддержка"),
                            "info@sedsoftware.com",
                            true,
                            onEmailSupport,
                            trailingIcon = AppIcons.Mail,
                        )
                    }
                }
            }
            item(key = "clear") {
                SettingsWidth {
                    DestructiveAction(
                        text = tr("Clear local data", "Очистить локальные данные"),
                        onClick = onClearRequest,
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = clearDescription
                        },
                        leadingIcon = { AppIcon(AppIcons.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsWidth(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), content = { content() })
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).sizeIn(minHeight = 56.dp)
            .semantics(mergeDescendants = true) { role = Role.RadioButton },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val stackContent = LocalDensity.current.fontScale >= 1.5f
    if (stackContent) {
        Column(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            verticalAlignment = Alignment.Top,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    trailingIcon: DrawableResource = AppIcons.ChevronRight,
) {
    val stackContent = LocalDensity.current.fontScale >= 1.5f
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).sizeIn(minHeight = 56.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
        horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stackContent) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppIcon(trailingIcon, contentDescription = null)
    }
}
