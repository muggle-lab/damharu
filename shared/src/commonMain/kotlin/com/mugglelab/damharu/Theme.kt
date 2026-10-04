package com.mugglelab.damharu

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DiaryColors(
    val background: Color, val panel: Color, val ink: Color, val soft: Color, val muted: Color, val faint: Color,
    val accent: Color, val onAccent: Color, val line: Color, val stamp: Color, val danger: Color,
)

// One fixed dark palette (docs/PRODUCT.md). The app ignores the system appearance on purpose.
val DiaryPalette = DiaryColors(
    background = Color.Black, panel = Color(0xFF12110F), ink = Color(0xFFEDE8DD), soft = Color(0xFFC9C3B6),
    muted = Color(0xFF8F897C), faint = Color(0xFF4A463E), accent = Color(0xFFD98551), onAccent = Color(0xFF0B0A09),
    line = Color(0xFFEDE8DD).copy(alpha = .14f), stamp = Color(0xFFF08A3C), danger = Color(0xFFB5553F),
)
val DiaryDialogShape = RoundedCornerShape(4.dp)

val LocalDiaryColors = staticCompositionLocalOf { DiaryPalette }

/** Serif for headlines and body, monospace for metadata. Platform families until bundled fonts are decided. */
val DiaryTextStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 23.sp)
val DiaryMonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.sp)

@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDiaryColors provides DiaryPalette) { content() }
}
