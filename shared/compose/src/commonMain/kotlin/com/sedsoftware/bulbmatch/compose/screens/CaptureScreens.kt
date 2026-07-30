package com.sedsoftware.bulbmatch.compose.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.AppScreenScaffold
import com.sedsoftware.bulbmatch.compose.components.AppIcon
import com.sedsoftware.bulbmatch.compose.components.AppIcons
import com.sedsoftware.bulbmatch.compose.components.AppNavigationIcon
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.components.PrimaryAction
import com.sedsoftware.bulbmatch.compose.components.ReadableContent
import com.sedsoftware.bulbmatch.compose.components.SecondaryAction
import com.sedsoftware.bulbmatch.compose.components.TertiaryAction
import com.sedsoftware.bulbmatch.compose.localization.tr
import com.sedsoftware.bulbmatch.compose.model.CameraState
import com.sedsoftware.bulbmatch.compose.model.RootDestination
import com.sedsoftware.bulbmatch.compose.model.ScreenLoadState
import com.sedsoftware.bulbmatch.compose.theme.LocalAppSpacing
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun MatchHomeScreen(
    modifier: Modifier = Modifier,
    catalogAvailable: Boolean = true,
    cameraGuidance: String? = null,
    unfinishedDraftMessage: Boolean = false,
    onCamera: () -> Unit,
    onChoosePhoto: () -> Unit,
    onManual: () -> Unit,
    onSettings: () -> Unit,
    onRootDestination: (RootDestination) -> Unit,
) {
    AppScreenScaffold(
        title = "BulbMatch",
        modifier = modifier,
        onSettings = onSettings,
        rootDestination = RootDestination.Match,
        onRootDestination = onRootDestination,
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(LocalAppSpacing.current.md),
            verticalArrangement = Arrangement.Center,
        ) {
            ReadableContent {
                Text(
                    tr(
                        "Match a replacement from what is printed on your old bulb.",
                        "Подберите замену по маркировке на старой лампе.",
                    ),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    tr(
                        "For 220–240 V / 50 Hz regions. Always check the fixture label.",
                        "Для регионов 220–240 В / 50 Гц. Всегда проверяйте маркировку светильника.",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!catalogAvailable) {
                    MessageCard(
                        tr("Catalog unavailable", "Каталог недоступен"),
                        tr(
                            "Photo assessment is disabled because the bundled catalog could not be verified. Manual entry and the source information remain available.",
                            "Оценка по фото отключена: встроенный каталог не прошёл проверку. Ручной ввод и сведения об источниках остаются доступны.",
                        ),
                        isError = true,
                    )
                }
                if (cameraGuidance != null) {
                    MessageCard(tr("Camera", "Камера"), cameraGuidance)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                ) {
                    PrimaryAction(
                        tr("Camera", "Камера"),
                        onCamera,
                        enabled = catalogAvailable,
                        leadingIcon = { AppIcon(AppIcons.PhotoCamera, contentDescription = null) },
                    )
                    SecondaryAction(
                        tr("Choose photo", "Выбрать фото"),
                        onChoosePhoto,
                        enabled = catalogAvailable,
                        leadingIcon = { AppIcon(AppIcons.PhotoLibrary, contentDescription = null) },
                    )
                    TertiaryAction(
                        tr("Enter manually", "Ввести вручную"),
                        onManual,
                        leadingIcon = { AppIcon(AppIcons.Edit, contentDescription = null) },
                    )
                }
                if (unfinishedDraftMessage) {
                    MessageCard(
                        tr("Draft not restored", "Черновик не восстановлен"),
                        tr(
                            "An unfinished scan was not saved. Start again when you are ready.",
                            "Незавершённое сканирование не было сохранено. Начните заново, когда будете готовы.",
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun CameraCaptureScreen(
    state: CameraState,
    modifier: Modifier = Modifier,
    torchSupported: Boolean = true,
    torchEnabled: Boolean = false,
    onClose: () -> Unit,
    onShutter: () -> Unit,
    onToggleTorch: () -> Unit,
    onTryAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onChoosePhoto: () -> Unit,
    onManual: () -> Unit,
    cameraPreview: (@Composable () -> Unit)? = null,
) {
    AppScreenScaffold(
        title = tr("Capture marking", "Снимок маркировки"),
        modifier = modifier,
        onBack = onClose,
        navigationIcon = AppNavigationIcon.Close,
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
        ) {
            when (state) {
                CameraState.Content -> CameraContent(
                    modifier = Modifier.weight(1f),
                    torchSupported = torchSupported,
                    torchEnabled = torchEnabled,
                    onShutter = onShutter,
                    onToggleTorch = onToggleTorch,
                    onChoosePhoto = onChoosePhoto,
                    onManual = onManual,
                    cameraPreview = cameraPreview,
                )
                CameraState.Opening -> CenteredCameraMessage {
                    CircularProgressIndicator()
                    Text(tr("Opening camera", "Открываем камеру"), style = MaterialTheme.typography.titleMedium)
                    Text(tr("This should only take a moment.", "Это займёт немного времени."))
                }
                CameraState.DeniedCanAsk -> CameraRecovery(
                    tr("Camera access is needed only when you take a photo.", "Доступ к камере нужен только для съёмки."),
                    tr("Try again", "Повторить"),
                    onTryAgain,
                    onChoosePhoto,
                    onManual,
                    AppIcons.RestartAlt,
                )
                CameraState.DeniedOpenSettings -> CameraRecovery(
                    tr(
                        "Camera access is off. You can enable it in system Settings or use another method.",
                        "Доступ к камере отключён. Включите его в системных настройках или выберите другой способ.",
                    ),
                    tr("Open Settings", "Открыть настройки"),
                    onOpenSettings,
                    onChoosePhoto,
                    onManual,
                    AppIcons.Settings,
                )
                CameraState.Unavailable -> CameraRecovery(
                    tr("No camera is available on this device.", "На этом устройстве камера недоступна."),
                    null,
                    {},
                    onChoosePhoto,
                    onManual,
                    null,
                )
                CameraState.Error -> CameraRecovery(
                    tr(
                        "The camera could not be opened. Try again or use a photo or manual entry.",
                        "Не удалось открыть камеру. Повторите попытку, выберите фото или введите данные вручную.",
                    ),
                    tr("Retry", "Повторить"),
                    onTryAgain,
                    onChoosePhoto,
                    onManual,
                    AppIcons.RestartAlt,
                )
            }
        }
    }
}

@Composable
private fun CameraContent(
    torchSupported: Boolean,
    torchEnabled: Boolean,
    onShutter: () -> Unit,
    onToggleTorch: () -> Unit,
    onChoosePhoto: () -> Unit,
    onManual: () -> Unit,
    cameraPreview: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val previewDescription = tr(
        "Live camera preview for the printed bulb marking",
        "Предпросмотр камеры для маркировки лампы",
    )
    val shutterDescription = tr("Take photo", "Сделать снимок")
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().semantics { contentDescription = previewDescription },
            contentAlignment = Alignment.Center,
        ) {
            if (cameraPreview != null) {
                cameraPreview()
            } else {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF181818)) {}
            }
            CameraFrameOverlay(
                Modifier.fillMaxWidth(.78f).aspectRatio(1.5f),
                color = Color.White.copy(alpha = .9f),
            )
            Text(
                tr("Place the printed marking inside the frame", "Поместите маркировку в рамку"),
                modifier = Modifier.align(Alignment.TopCenter).padding(LocalAppSpacing.current.md),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(LocalAppSpacing.current.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
            ) {
                if (torchSupported) {
                    TextButton(onClick = onToggleTorch, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                        Text(if (torchEnabled) tr("Torch on", "Фонарик включён") else tr("Torch off", "Фонарик выключен"))
                    }
                }
                Button(
                    onClick = onShutter,
                    modifier = Modifier.size(72.dp).semantics { contentDescription = shutterDescription },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    AppIcon(AppIcons.PhotoCamera, contentDescription = null, Modifier.size(32.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.sm),
                ) {
                    TextButton(onClick = onChoosePhoto, modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)) {
                        Text(tr("Choose photo", "Выбрать фото"), textAlign = TextAlign.Center)
                    }
                    TextButton(onClick = onManual, modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)) {
                        Text(tr("Enter manually", "Ввести вручную"), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraFrameOverlay(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val segment = size.minDimension * .18f
        val stroke = 5f
        listOf(
            Offset(0f, 0f) to Offset(segment, 0f),
            Offset(0f, 0f) to Offset(0f, segment),
            Offset(size.width, 0f) to Offset(size.width - segment, 0f),
            Offset(size.width, 0f) to Offset(size.width, segment),
            Offset(0f, size.height) to Offset(segment, size.height),
            Offset(0f, size.height) to Offset(0f, size.height - segment),
            Offset(size.width, size.height) to Offset(size.width - segment, size.height),
            Offset(size.width, size.height) to Offset(size.width, size.height - segment),
        ).forEach { (start, end) -> drawLine(color, start, end, stroke) }
    }
}

@Composable
private fun CenteredCameraMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 520.dp).padding(LocalAppSpacing.current.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
            content = { content() },
        )
    }
}

@Composable
private fun CameraRecovery(
    message: String,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    onChoosePhoto: () -> Unit,
    onManual: () -> Unit,
    primaryIcon: DrawableResource?,
) {
    CenteredCameraMessage {
        MessageCard(
            tr("Camera unavailable", "Камера недоступна"),
            message,
            isError = true,
            icon = AppIcons.CameraOff,
        )
        if (primaryLabel != null) {
            PrimaryAction(
                primaryLabel,
                onPrimary,
                leadingIcon = primaryIcon?.let { icon -> { AppIcon(icon, contentDescription = null) } },
            )
        }
        SecondaryAction(
            tr("Choose photo", "Выбрать фото"),
            onChoosePhoto,
            leadingIcon = { AppIcon(AppIcons.PhotoLibrary, contentDescription = null) },
        )
        TertiaryAction(
            tr("Enter manually", "Ввести вручную"),
            onManual,
            leadingIcon = { AppIcon(AppIcons.Edit, contentDescription = null) },
        )
    }
}

@Composable
private fun SelectedPhotoPreview(
    contentDescription: String,
    previewContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .heightIn(min = 180.dp, max = 360.dp)
            .semantics { this.contentDescription = contentDescription },
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (previewContent == null) {
                Text(tr("Selected marking photo", "Выбранное фото маркировки"))
            } else {
                previewContent()
            }
        }
    }
}

@Composable
private fun RecognitionStateActions(
    state: ScreenLoadState,
    errorMessage: String?,
    readingDescription: String,
    onUsePhoto: () -> Unit,
    onCancelRecognition: () -> Unit,
) {
    when (state) {
        ScreenLoadState.Loading -> {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = readingDescription
                },
            )
            MessageCard(
                tr("Reading text on this device", "Распознаём текст на устройстве"),
                tr("The photo is not being uploaded.", "Фото не отправляется в сеть."),
            )
            SecondaryAction(
                tr("Cancel", "Отмена"),
                onCancelRecognition,
                leadingIcon = { AppIcon(AppIcons.Close, contentDescription = null) },
            )
        }
        ScreenLoadState.Error -> {
            MessageCard(
                tr("Could not read this photo", "Не удалось прочитать фото"),
                errorMessage ?: tr(
                    "No readable text was found.",
                    "Читаемый текст не найден.",
                ),
                isError = true,
            )
            PrimaryAction(
                tr("Try again", "Повторить"),
                onUsePhoto,
                leadingIcon = { AppIcon(AppIcons.RestartAlt, contentDescription = null) },
            )
        }
        else -> PrimaryAction(
            tr("Use photo", "Использовать фото"),
            onUsePhoto,
            leadingIcon = { AppIcon(AppIcons.CheckCircle, contentDescription = null) },
        )
    }
}

@Composable
fun ImageReviewScreen(
    state: ScreenLoadState,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    previewContent: (@Composable () -> Unit)? = null,
    onBack: () -> Unit,
    onUsePhoto: () -> Unit,
    onRetake: () -> Unit,
    onChooseAnother: () -> Unit,
    onManual: () -> Unit,
    onCancelRecognition: () -> Unit,
) {
    val selectedPhotoDescription = tr(
        "Selected photo of the printed bulb marking",
        "Выбранное фото маркировки лампы",
    )
    val readingDescription = tr(
        "Reading text on this device",
        "Распознаём текст на устройстве",
    )
    AppScreenScaffold(
        title = tr("Review photo", "Проверьте фото"),
        modifier = modifier,
        onBack = onBack,
    ) { insets ->
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
                SelectedPhotoPreview(selectedPhotoDescription, previewContent)
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
                    verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.md),
                ) {
                    Text(
                        tr("Can you read the marking?", "Маркировка читается?"),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        tr(
                            "Use a sharp photo with the printed text visible. Recognition happens on this device and works offline.",
                            "Используйте чёткое фото с видимым текстом. Распознавание выполняется на устройстве и работает офлайн.",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    RecognitionStateActions(
                        state = state,
                        errorMessage = errorMessage,
                        readingDescription = readingDescription,
                        onUsePhoto = onUsePhoto,
                        onCancelRecognition = onCancelRecognition,
                    )
                    SecondaryAction(
                        tr("Retake", "Переснять"),
                        onRetake,
                        leadingIcon = { AppIcon(AppIcons.RestartAlt, contentDescription = null) },
                    )
                    SecondaryAction(
                        tr("Choose another", "Выбрать другое"),
                        onChooseAnother,
                        leadingIcon = { AppIcon(AppIcons.PhotoLibrary, contentDescription = null) },
                    )
                    TertiaryAction(
                        tr("Enter manually", "Ввести вручную"),
                        onManual,
                        leadingIcon = { AppIcon(AppIcons.Edit, contentDescription = null) },
                    )
                }
            }
        }
    }
}
