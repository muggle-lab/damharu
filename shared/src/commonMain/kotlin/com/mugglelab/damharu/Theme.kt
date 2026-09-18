package com.mugglelab.damharu

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

data class DiaryColors(
    val background: Color, val ink: Color, val muted: Color,
    val accent: Color, val onAccent: Color, val line: Color,
)

private val LightColors = DiaryColors(
    Color(0xFFF8F7F2), Color(0xFF30372E), Color(0xFF74786D),
    Color(0xFF54644A), Color.White, Color(0xFFE4E5DC),
)
private val DarkColors = DiaryColors(
    Color(0xFF242520), Color(0xFFEFEEE6), Color(0xFFB7B9AE),
    Color(0xFFB9C9AA), Color(0xFF24311D), Color(0xFF45483E),
)
val LocalDiaryColors = staticCompositionLocalOf { LightColors }

val DiaryTextStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 22.sp)

@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDiaryColors provides if (isSystemInDarkTheme()) DarkColors else LightColors) {
        content()
    }
}
