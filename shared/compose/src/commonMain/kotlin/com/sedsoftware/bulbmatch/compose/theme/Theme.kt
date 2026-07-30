package com.sedsoftware.bulbmatch.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sedsoftware.bulbmatch.compose.localization.LocalAppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode

private val LightColors = lightColorScheme(
    primary = WarmYellowLight,
    onPrimary = OnWarmYellowLight,
    primaryContainer = WarmYellowContainerLight,
    onPrimaryContainer = OnWarmYellowContainerLight,
    secondary = Color(0xFF665D45),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEE1C2),
    onSecondaryContainer = Color(0xFF211B0B),
    error = ConflictLight,
    onError = Color.White,
    errorContainer = ConflictContainerLight,
    onErrorContainer = OnConflictContainerLight,
    background = NeutralBackgroundLight,
    onBackground = NeutralOnSurfaceLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralOnSurfaceLight,
    surfaceVariant = NeutralVariantLight,
    onSurfaceVariant = NeutralOnVariantLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = WarmYellowDark,
    onPrimary = OnWarmYellowDark,
    primaryContainer = WarmYellowContainerDark,
    onPrimaryContainer = OnWarmYellowContainerDark,
    secondary = Color(0xFFD1C5A7),
    onSecondary = Color(0xFF362F1B),
    secondaryContainer = Color(0xFF4E4630),
    onSecondaryContainer = Color(0xFFEEE1C2),
    error = ConflictDark,
    onError = Color(0xFF5F1607),
    errorContainer = ConflictContainerDark,
    onErrorContainer = OnConflictContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnSurfaceDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralVariantDark,
    onSurfaceVariant = NeutralOnVariantDark,
    outline = OutlineDark,
)

private val DefaultTypography = Typography()

private val BulbMatchTypography = Typography(
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = DefaultTypography.titleSmall.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = DefaultTypography.labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val BulbMatchShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Immutable
data class AppSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

@Immutable
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val conflict: Color,
    val conflictContainer: Color,
    val onConflictContainer: Color,
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        SuccessLight, SuccessContainerLight, OnSuccessContainerLight,
        WarningLight, WarningContainerLight, OnWarningContainerLight,
        ConflictLight, ConflictContainerLight, OnConflictContainerLight,
    )
}

@Composable
fun BulbMatchTheme(
    themeMode: AppThemeMode = AppThemeMode.System,
    language: AppLanguage = AppLanguage.English,
    systemIsRussian: Boolean = false,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.System -> systemDark
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val effectiveLanguage = when (language) {
        AppLanguage.System -> if (systemIsRussian) AppLanguage.Russian else AppLanguage.English
        else -> language
    }
    val status = if (isDark) {
        StatusColors(
            SuccessDark, SuccessContainerDark, OnSuccessContainerDark,
            WarningDark, WarningContainerDark, OnWarningContainerDark,
            ConflictDark, ConflictContainerDark, OnConflictContainerDark,
        )
    } else {
        StatusColors(
            SuccessLight, SuccessContainerLight, OnSuccessContainerLight,
            WarningLight, WarningContainerLight, OnWarningContainerLight,
            ConflictLight, ConflictContainerLight, OnConflictContainerLight,
        )
    }

    CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalStatusColors provides status,
        LocalAppLanguage provides effectiveLanguage,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColors else LightColors,
            typography = BulbMatchTypography,
            shapes = BulbMatchShapes,
        ) {
            onThemeChanged(isDark)
            Surface(content = content)
        }
    }
}
