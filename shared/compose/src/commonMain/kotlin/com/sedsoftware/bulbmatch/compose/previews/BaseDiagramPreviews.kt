package com.sedsoftware.bulbmatch.compose.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sedsoftware.bulbmatch.compose.components.BaseDiagram
import com.sedsoftware.bulbmatch.compose.model.AppLanguage
import com.sedsoftware.bulbmatch.compose.model.AppThemeMode
import com.sedsoftware.bulbmatch.compose.theme.BulbMatchTheme

@Preview(name = "COMPONENT BaseDiagram E27 light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram E27 dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramE27Preview() = BaseDiagramPreview(code = "E27")

@Preview(name = "COMPONENT BaseDiagram E14 light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram E14 dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramE14Preview() = BaseDiagramPreview(code = "E14")

@Preview(name = "COMPONENT BaseDiagram B22d light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram B22d dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramB22dPreview() = BaseDiagramPreview(code = "B22d")

@Preview(name = "COMPONENT BaseDiagram GU10 light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram GU10 dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramGu10Preview() = BaseDiagramPreview(code = "GU10")

@Preview(name = "COMPONENT BaseDiagram G9 light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram G9 dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramG9Preview() = BaseDiagramPreview(code = "G9")

@Preview(name = "COMPONENT BaseDiagram R7s light", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "COMPONENT BaseDiagram R7s dark", widthDp = 240, heightDp = 240, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun BaseDiagramR7sPreview() = BaseDiagramPreview(code = "R7s")

@Composable
private fun BaseDiagramPreview(code: String) {
    BulbMatchTheme(
        themeMode = AppThemeMode.System,
        language = AppLanguage.English,
    ) {
        Surface(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BaseDiagram(
                    code = code,
                    alternativeText = code,
                )
            }
        }
    }
}

private const val UI_MODE_NIGHT_NO = 0x10
private const val UI_MODE_NIGHT_YES = 0x20
