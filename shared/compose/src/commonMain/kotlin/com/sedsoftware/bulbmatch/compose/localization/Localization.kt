package com.sedsoftware.bulbmatch.compose.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.sedsoftware.bulbmatch.compose.model.AppLanguage

val LocalAppLanguage = compositionLocalOf { AppLanguage.English }

@Composable
@ReadOnlyComposable
fun tr(english: String, russian: String): String =
    if (LocalAppLanguage.current == AppLanguage.Russian) russian else english

fun AppLanguage.displayName(): String = when (this) {
    AppLanguage.English -> "English"
    AppLanguage.Russian -> "Русский"
}
