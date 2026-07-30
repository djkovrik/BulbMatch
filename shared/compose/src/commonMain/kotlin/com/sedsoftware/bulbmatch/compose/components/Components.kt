package com.sedsoftware.bulbmatch.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.AssessmentOutcome
import com.sedsoftware.bulbmatch.compose.model.RootDestination
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing
import com.sedsoftware.bulbmatch.compose.theme.LocalStatusColors
import org.jetbrains.compose.resources.DrawableResource

enum class AppNavigationIcon {
    Back,
    Close,
}

enum class SectionTone {
    Neutral,
    Information,
    Warning,
    Success,
    Conflict,
}

enum class MessageTone {
    Information,
    Warning,
    Success,
    Error,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: AppNavigationIcon = AppNavigationIcon.Back,
    onSettings: (() -> Unit)? = null,
    topAction: (@Composable () -> Unit)? = null,
    rootDestination: RootDestination? = null,
    onRootDestination: (RootDestination) -> Unit = {},
    stickyAd: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        val isClose = navigationIcon == AppNavigationIcon.Close
                        AppIconButton(
                            resource = if (isClose) AppIcons.Close else AppIcons.ArrowBack,
                            contentDescription = if (isClose) tr("Close", "Закрыть") else tr("Back", "Назад"),
                            onClick = onBack,
                        )
                    }
                },
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    topAction?.invoke()
                    if (onSettings != null) {
                        AppIconButton(
                            resource = AppIcons.Settings,
                            contentDescription = tr("Settings", "Настройки"),
                            onClick = onSettings,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column {
                stickyAd?.invoke()
                if (rootDestination != null) {
                    RootNavigationBar(rootDestination, onRootDestination)
                }
            }
        },
        content = content,
    )
}

@Composable
fun RootNavigationBar(
    selected: RootDestination,
    onSelect: (RootDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = listOf(
        Triple(RootDestination.Match, AppIcons.Lightbulb, tr("Match", "Подбор")),
        Triple(RootDestination.History, AppIcons.History, tr("History", "История")),
        Triple(RootDestination.Reference, AppIcons.MenuBook, tr("Reference", "Справочник")),
    )
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        entries.forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { AppIcon(icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun ReadableContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
        ) {
            content()
        }
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().sizeIn(minHeight = 52.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(LocalAppSpacing.current.sm))
        }
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().sizeIn(minHeight = 52.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(LocalAppSpacing.current.sm))
        }
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
fun TertiaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(LocalAppSpacing.current.sm))
        }
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
fun DestructiveAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().sizeIn(minHeight = 52.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(LocalAppSpacing.current.sm))
        }
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun AppIconButton(
    resource: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        AppIcon(resource, contentDescription, Modifier.size(24.dp))
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    tone: SectionTone = SectionTone.Neutral,
    icon: DrawableResource? = null,
    content: @Composable () -> Unit,
) {
    val status = LocalStatusColors.current
    val (containerColor, contentColor) = when (tone) {
        SectionTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurface
        SectionTone.Information -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface
        SectionTone.Warning -> status.warningContainer to status.onWarningContainer
        SectionTone.Success -> status.successContainer to status.onSuccessContainer
        SectionTone.Conflict -> status.conflictContainer to status.onConflictContainer
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LocalAppSpacing.current.md),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) AppIcon(icon, contentDescription = null, Modifier.size(22.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            content()
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    valueMaxLines: Int = Int.MAX_VALUE,
) {
    Column(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = valueMaxLines,
            overflow = if (valueMaxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BulletText(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm)) {
        Text("•", style = MaterialTheme.typography.bodyLarge)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun OutcomeBanner(
    outcome: AssessmentOutcome,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val status = LocalStatusColors.current
    val (icon, title, container, content) = when (outcome) {
        AssessmentOutcome.Compatible -> StatusTuple(
            AppIcons.CheckCircle,
            tr("Compatible profile", "Совместимый профиль"),
            status.successContainer,
            status.onSuccessContainer,
        )
        AssessmentOutcome.NeedClarification -> StatusTuple(
            AppIcons.Warning,
            tr("Need clarification", "Нужно уточнение"),
            status.warningContainer,
            status.onWarningContainer,
        )
        AssessmentOutcome.PotentialConflict -> StatusTuple(
            AppIcons.Cancel,
            tr("Potential conflict", "Возможен конфликт"),
            status.conflictContainer,
            status.onConflictContainer,
        )
        AssessmentOutcome.Unavailable -> StatusTuple(
            AppIcons.Error,
            tr("Assessment unavailable", "Оценка недоступна"),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(title, detail).joinToString(". ")
        },
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(LocalAppSpacing.current.lg),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(shape = RoundedCornerShape(50), color = content.copy(alpha = 0.12f)) {
                AppIcon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs)) {
                Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class StatusTuple(
    val icon: DrawableResource,
    val title: String,
    val container: Color,
    val content: Color,
)

@Composable
fun MessageCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    tone: MessageTone = MessageTone.Information,
    icon: DrawableResource? = null,
) {
    val effectiveTone = if (isError) MessageTone.Error else tone
    val status = LocalStatusColors.current
    val (containerColor, contentColor, defaultIcon) = when (effectiveTone) {
        MessageTone.Information -> Triple(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.onSurface,
            AppIcons.Info,
        )
        MessageTone.Warning -> Triple(status.warningContainer, status.onWarningContainer, AppIcons.Warning)
        MessageTone.Success -> Triple(status.successContainer, status.onSuccessContainer, AppIcons.CheckCircle)
        MessageTone.Error -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            AppIcons.Error,
        )
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(LocalAppSpacing.current.md),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
            verticalAlignment = Alignment.Top,
        ) {
            AppIcon(icon ?: defaultIcon, contentDescription = null, Modifier.size(22.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ProvenanceBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier
                .sizeIn(minHeight = 48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(AppIcons.Source, contentDescription = null, Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LoadingOrErrorState(
    state: ScreenLoadState,
    modifier: Modifier = Modifier,
    emptyTitle: String,
    emptyBody: String,
    errorTitle: String,
    errorBody: String,
    onRetry: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    when (state) {
        ScreenLoadState.Content -> content()
        ScreenLoadState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text(tr("Loading…", "Загрузка…"))
            }
        }
        ScreenLoadState.Empty -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MessageCard(emptyTitle, emptyBody, Modifier.padding(LocalAppSpacing.current.md))
        }
        ScreenLoadState.Error -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(LocalAppSpacing.current.md),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            ) {
                MessageCard(errorTitle, errorBody, isError = true)
                SecondaryAction(tr("Retry", "Повторить"), onRetry)
            }
        }
    }
}

@Composable
fun AdvertisementSlot(
    loaded: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!loaded) return
    val advertisementLabel = tr("Advertisement", "Реклама")
    Surface(
        modifier = modifier.fillMaxWidth().height(64.dp).semantics {
            contentDescription = advertisementLabel
            role = Role.Image
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(advertisementLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BaseDiagram(
    code: String,
    alternativeText: String,
    modifier: Modifier = Modifier,
) {
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(MaterialTheme.shapes.medium)
            .semantics { contentDescription = "$code. $alternativeText" },
    ) {
        val centerX = size.width / 2f
        val top = size.height * 0.12f
        val bottom = size.height * 0.86f
        val half = size.width * 0.14f
        drawRoundRect(
            color = strokeColor,
            topLeft = Offset(centerX - half, top),
            size = androidx.compose.ui.geometry.Size(half * 2f, bottom - top),
            style = Stroke(width = 4f),
        )
        when {
            code.startsWith("E") -> {
                repeat(5) { index ->
                    val y = size.height * (0.48f + index * 0.065f)
                    drawLine(strokeColor, Offset(centerX - half, y), Offset(centerX + half, y), 4f)
                }
                drawLine(accent, Offset(centerX - half / 2f, bottom), Offset(centerX + half / 2f, bottom), 7f, StrokeCap.Round)
            }
            code == "GU10" -> {
                drawLine(strokeColor, Offset(centerX - half / 2f, bottom), Offset(centerX - half / 2f, size.height * .97f), 7f)
                drawLine(strokeColor, Offset(centerX + half / 2f, bottom), Offset(centerX + half / 2f, size.height * .97f), 7f)
                drawCircle(accent, 7f, Offset(centerX - half / 2f, size.height * .97f))
                drawCircle(accent, 7f, Offset(centerX + half / 2f, size.height * .97f))
            }
            else -> {
                drawLine(strokeColor, Offset(centerX - half / 2f, bottom), Offset(centerX - half / 2f, size.height * .97f), 5f)
                drawLine(strokeColor, Offset(centerX + half / 2f, bottom), Offset(centerX + half / 2f, size.height * .97f), 5f)
            }
        }
        drawLine(accent, Offset(centerX - half, top + 12f), Offset(centerX + half, top + 12f), 5f, StrokeCap.Round)
    }
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.height(LocalAppSpacing.current.xs))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
}
