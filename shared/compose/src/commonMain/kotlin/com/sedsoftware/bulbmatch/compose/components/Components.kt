package com.sedsoftware.bulbmatch.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                stickyAd?.let { ad ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (rootDestination == null) {
                                    Modifier.windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        ad()
                    }
                }
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
    val style = when (outcome) {
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
            contentDescription = listOfNotNull(style.title, detail).joinToString(". ")
        },
        shape = MaterialTheme.shapes.large,
        color = style.container,
        contentColor = style.content,
    ) {
        Row(
            modifier = Modifier.padding(LocalAppSpacing.current.lg),
            horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(shape = RoundedCornerShape(50), color = style.content.copy(alpha = 0.12f)) {
                AppIcon(
                    style.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.xs)) {
                Text(
                    style.title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .then(modifier)
            .clip(MaterialTheme.shapes.medium)
            .semantics { contentDescription = "$code. $alternativeText" },
    ) {
        when (code.uppercase()) {
            "E27" -> drawScrewBase(size.width * .15f, strokeColor, accent)
            "E14" -> drawScrewBase(size.width * .105f, strokeColor, accent)
            "B22D" -> drawBayonetBase(strokeColor, accent)
            "GU10" -> drawGu10Base(strokeColor, accent)
            "G9" -> drawG9Base(strokeColor, accent)
            "R7S" -> drawR7sBase(strokeColor, accent)
            else -> drawGenericPinBase(strokeColor, accent)
        }
    }
}

private fun DrawScope.drawScrewBase(
    bodyHalfWidth: Float,
    strokeColor: Color,
    contactColor: Color,
) {
    val centerX = size.width / 2f
    val top = size.height * .12f
    val bottom = size.height * .86f
    val stroke = size.minDimension * .022f
    drawBaseBody(centerX, bodyHalfWidth, top, bottom, strokeColor, stroke)
    repeat(5) { index ->
        val y = size.height * (.48f + index * .065f)
        drawLine(
            strokeColor,
            Offset(centerX - bodyHalfWidth, y),
            Offset(centerX + bodyHalfWidth, y),
            stroke,
        )
    }
    drawLine(
        contactColor,
        Offset(centerX - bodyHalfWidth * .42f, bottom),
        Offset(centerX + bodyHalfWidth * .42f, bottom),
        stroke * 1.8f,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawBayonetBase(
    strokeColor: Color,
    contactColor: Color,
) {
    val centerX = size.width / 2f
    val half = size.width * .13f
    val top = size.height * .12f
    val bottom = size.height * .82f
    val stroke = size.minDimension * .022f
    drawBaseBody(centerX, half, top, bottom, strokeColor, stroke)

    val pinY = size.height * .5f
    val pinLength = half * .42f
    drawLine(
        strokeColor,
        Offset(centerX - half - pinLength, pinY),
        Offset(centerX - half, pinY),
        stroke * 1.6f,
        StrokeCap.Round,
    )
    drawLine(
        strokeColor,
        Offset(centerX + half, pinY),
        Offset(centerX + half + pinLength, pinY),
        stroke * 1.6f,
        StrokeCap.Round,
    )
    val contactOffset = half * .42f
    drawCircle(contactColor, stroke * 1.25f, Offset(centerX - contactOffset, bottom))
    drawCircle(contactColor, stroke * 1.25f, Offset(centerX + contactOffset, bottom))
}

private fun DrawScope.drawGu10Base(
    strokeColor: Color,
    contactColor: Color,
) {
    val centerX = size.width / 2f
    val half = size.width * .14f
    val top = size.height * .12f
    val bottom = size.height * .78f
    val pinBottom = size.height * .95f
    val stroke = size.minDimension * .022f
    drawBaseBody(centerX, half, top, bottom, strokeColor, stroke)
    listOf(centerX - half * .48f, centerX + half * .48f).forEach { x ->
        drawLine(strokeColor, Offset(x, bottom), Offset(x, pinBottom), stroke * 1.6f)
        drawCircle(contactColor, stroke * 1.7f, Offset(x, pinBottom))
    }
}

private fun DrawScope.drawG9Base(
    strokeColor: Color,
    contactColor: Color,
) {
    val centerX = size.width / 2f
    val half = size.width * .105f
    val top = size.height * .12f
    val bottom = size.height * .68f
    val stroke = size.minDimension * .022f
    drawBaseBody(centerX, half, top, bottom, strokeColor, stroke)

    val loopBottom = size.height * .93f
    val loopHalf = half * .2f
    listOf(centerX - half * .5f, centerX + half * .5f).forEach { loopCenter ->
        val loop = Path().apply {
            moveTo(loopCenter - loopHalf, bottom)
            lineTo(loopCenter - loopHalf, loopBottom - loopHalf)
            quadraticBezierTo(loopCenter, loopBottom + loopHalf, loopCenter + loopHalf, loopBottom - loopHalf)
            lineTo(loopCenter + loopHalf, bottom)
        }
        drawPath(loop, contactColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawR7sBase(
    strokeColor: Color,
    contactColor: Color,
) {
    val stroke = size.minDimension * .022f
    val left = size.width * .18f
    val right = size.width * .82f
    val top = size.height * .36f
    val bottom = size.height * .64f
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        style = Stroke(width = stroke),
    )
    val centerY = size.height / 2f
    drawLine(strokeColor, Offset(left + stroke * 2f, top), Offset(left + stroke * 2f, bottom), stroke)
    drawLine(strokeColor, Offset(right - stroke * 2f, top), Offset(right - stroke * 2f, bottom), stroke)
    drawLine(
        contactColor,
        Offset(size.width * .09f, centerY),
        Offset(left, centerY),
        stroke * 1.8f,
        StrokeCap.Round,
    )
    drawLine(
        contactColor,
        Offset(right, centerY),
        Offset(size.width * .91f, centerY),
        stroke * 1.8f,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawGenericPinBase(
    strokeColor: Color,
    contactColor: Color,
) {
    val centerX = size.width / 2f
    val half = size.width * .12f
    val top = size.height * .12f
    val bottom = size.height * .78f
    val pinBottom = size.height * .96f
    val stroke = size.minDimension * .022f
    drawBaseBody(centerX, half, top, bottom, strokeColor, stroke)
    listOf(centerX - half * .45f, centerX + half * .45f).forEach { x ->
        drawLine(contactColor, Offset(x, bottom), Offset(x, pinBottom), stroke * 1.4f, StrokeCap.Round)
    }
}

private fun DrawScope.drawBaseBody(
    centerX: Float,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    color: Color,
    stroke: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - halfWidth, top),
        size = Size(halfWidth * 2f, bottom - top),
        style = Stroke(width = stroke),
    )
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.height(LocalAppSpacing.current.xs))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
}
