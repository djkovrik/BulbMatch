package com.sedsoftware.bulbmatch.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.AppScreenScaffold
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.components.PrimaryAction
import com.sedsoftware.bulbmatch.compose.components.SectionCard
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.FieldOrigin
import com.sedsoftware.bulbmatch.compose.model.FieldUiModel
import com.sedsoftware.bulbmatch.compose.model.ReviewDecision
import com.sedsoftware.bulbmatch.compose.model.ReviewUiModel
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing

@Composable
fun DataReviewScreen(
    model: ReviewUiModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onValueChange: (fieldId: String, value: String) -> Unit,
    onDecision: (fieldId: String, decision: ReviewDecision) -> Unit,
    onAssess: () -> Unit,
    showDiscardConfirmation: Boolean = false,
    onDiscardConfirmed: () -> Unit = {},
    onDiscardDismissed: () -> Unit = {},
) {
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onDiscardDismissed,
            title = { Text(tr("Discard entered details?", "Отменить введённые данные?")) },
            text = {
                Text(
                    tr(
                        "This in-memory draft will be lost.",
                        "Этот черновик в памяти будет удалён.",
                    ),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = onDiscardConfirmed) {
                    Text(tr("Discard", "Удалить"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = onDiscardDismissed) {
                    Text(tr("Keep editing", "Продолжить ввод"))
                }
            },
        )
    }
    val groups = listOf(
        "base" to tr("Base", "Цоколь"),
        "electrical" to tr("Electrical", "Электрические параметры"),
        "light" to tr("Light output", "Световой поток"),
        "appearance" to tr("Appearance", "Характеристики света"),
        "fixture" to tr("Fixture", "Светильник"),
    )
    AppScreenScaffold(
        title = tr("Review details", "Проверьте данные"),
        modifier = modifier,
        onBack = onBack,
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(LocalAppSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            ) {
                if (model.fromOcr) {
                    item(key = "review_notice") {
                        MessageCard(
                            tr("Review every detected field", "Проверьте каждое распознанное поле"),
                            tr(
                                "Recognition is only a draft. Confirm, edit, or reject every detected value.",
                                "Распознавание — только черновик. Подтвердите, измените или отклоните каждое значение.",
                            ),
                        )
                    }
                }
                item(key = "unresolved") {
                    val summary = if (model.unresolvedCount == 0) {
                        tr("All observations have been handled.", "Все найденные значения проверены.")
                    } else {
                        tr(
                            "${model.unresolvedCount} fields still need a decision.",
                            "Ещё полей без решения: ${model.unresolvedCount}.",
                        )
                    }
                    MessageCard(
                        tr("Review summary", "Итоги проверки"),
                        model.message ?: summary,
                        isError = model.unresolvedCount > 0,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = summary
                        },
                    )
                }
                groups.forEach { (prefix, title) ->
                    val fields = model.fields.filter { it.id.startsWith(prefix) }
                    if (fields.isNotEmpty()) {
                        item(key = "section_$prefix") {
                            SectionCard(title) {
                                fields.forEach { field ->
                                    ReviewField(
                                        field = field,
                                        onValueChange = { onValueChange(field.id, it) },
                                        onDecision = { onDecision(field.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (model.fields.isEmpty()) {
                    item(key = "blank_help") {
                        MessageCard(
                            tr("Enter what is printed", "Введите маркировку"),
                            tr(
                                "Base and voltage are required for a compatible profile. Optional missing values remain “Not provided”, never zero.",
                                "Для совместимого профиля нужны цоколь и напряжение. Отсутствующие необязательные значения остаются «Не указано», а не нулём.",
                            ),
                        )
                    }
                }
                item(key = "safety") {
                    MessageCard(
                        tr("Fixture facts stay separate", "Данные светильника вводятся отдельно"),
                        tr(
                            "Fixture max wattage must come from the fixture label and can only be entered manually. It is never inferred from the old lamp.",
                            "Максимальную мощность берите только с маркировки светильника и вводите вручную. Она не определяется по старой лампе.",
                        ),
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                PrimaryAction(
                    text = tr("Assess replacement profile", "Оценить профиль замены"),
                    onClick = onAssess,
                    enabled = model.canAssess,
                    modifier = Modifier.padding(LocalAppSpacing.current.md).imePadding(),
                )
            }
        }
    }
}

@Composable
private fun ReviewField(
    field: FieldUiModel,
    onValueChange: (String) -> Unit,
    onDecision: (ReviewDecision) -> Unit,
) {
    val requiredText = if (field.required) tr("Required", "Обязательно") else tr("Optional", "Необязательно")
    val origin = when (field.origin) {
        FieldOrigin.Detected -> tr("Detected", "Распознано")
        FieldOrigin.Edited -> tr("Edited", "Изменено")
        FieldOrigin.Manual -> tr("Manual", "Вручную")
    }
    val notProvided = tr("Not provided", "Не указано")
    Column(
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = buildString {
                append(field.label)
                append(". ")
                append(field.value.ifBlank { notProvided })
                append(". ")
                append(origin)
                append(". ")
                append(requiredText)
                field.error?.let { append(". "); append(it) }
            }
        },
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
    ) {
        OutlinedTextField(
            value = field.value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(field.label) },
            placeholder = { if (field.example.isNotEmpty()) Text(field.example) },
            supportingText = {
                Text(field.error ?: "$requiredText · $origin")
            },
            isError = field.error != null,
            singleLine = false,
            keyboardOptions = KeyboardOptions.Default,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(origin) },
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            )
            if (field.origin == FieldOrigin.Detected || field.origin == FieldOrigin.Edited) {
                FilterChip(
                    selected = field.decision == ReviewDecision.Confirmed,
                    onClick = { onDecision(ReviewDecision.Confirmed) },
                    label = { Text(tr("Confirm", "Подтвердить")) },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                )
                FilterChip(
                    selected = field.decision == ReviewDecision.Rejected,
                    onClick = { onDecision(ReviewDecision.Rejected) },
                    label = { Text(tr("Reject", "Отклонить")) },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                )
            }
        }
    }
}
