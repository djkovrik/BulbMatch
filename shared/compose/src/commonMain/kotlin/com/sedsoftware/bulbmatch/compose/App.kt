package com.sedsoftware.bulbmatch.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.app.RootComponent
import com.sedsoftware.bulbmatch.compose.components.MessageCard
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode
import com.sedsoftware.bulbmatch.compose.theme.BulbMatchTheme

/**
 * Compatibility entry point for a host that has not supplied the Decompose graph yet.
 * It does not start demo state or expose a recommendation.
 */
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    BulbMatchTheme(
        themeMode = AppThemeMode.System,
        language = AppLanguage.English,
        onThemeChanged = onThemeChanged,
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MessageCard(
                title = "BulbMatch",
                body = "The application component graph is not connected. No assessment is available.",
                isError = true,
            )
        }
    }
}

/** Production entry point backed by the real Decompose component graph. */
@Composable
fun App(
    root: RootComponent,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    slots: BulbMatchSlots = BulbMatchSlots(),
    formatEpochMillis: (Long) -> String = { it.toString() },
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenSourceSummary: () -> Unit = {},
    onEmailSupport: () -> Unit = {},
) {
    BulbMatchRoot(
        root = root,
        slots = slots,
        formatEpochMillis = formatEpochMillis,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenSourceSummary = onOpenSourceSummary,
        onEmailSupport = onEmailSupport,
        onThemeChanged = onThemeChanged,
    )
}
