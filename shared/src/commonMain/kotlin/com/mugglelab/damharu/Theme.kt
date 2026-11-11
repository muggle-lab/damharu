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

// All screens use the selected appearance; overlays on photos keep their own contrast.
val DiaryPalette = DiaryColors(
    background = Color.Black, panel = Color(0xFF12110F), ink = Color(0xFFEDE8DD), soft = Color(0xFFC9C3B6),
    muted = Color(0xFF8F897C), faint = Color(0xFF4A463E), accent = Color(0xFFD98551), onAccent = Color(0xFF0B0A09),
    line = Color(0xFFEDE8DD).copy(alpha = .14f), stamp = Color(0xFFF08A3C), danger = Color(0xFFB5553F),
)
val DiaryLightPalette = DiaryColors(
    background = Color(0xFFF5F1E8), panel = Color(0xFFECE6DA), ink = Color(0xFF29251F), soft = Color(0xFF514A40),
    muted = Color(0xFF736A5C), faint = Color(0xFF948979), accent = Color(0xFFA95127), onAccent = Color(0xFFF5F1E8),
    line = Color(0xFF29251F).copy(alpha = .14f), stamp = Color(0xFFA95127), danger = Color(0xFF9C382A),
)
val DiaryDialogShape = RoundedCornerShape(4.dp)

val LocalDiaryColors = staticCompositionLocalOf { DiaryPalette }

/** Serif for headlines and body, monospace for metadata. Platform families until bundled fonts are decided. */
val DiaryTextStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 23.sp)
val DiaryMonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.sp)

@Composable
fun DiaryTheme(lightMode: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDiaryColors provides if (lightMode) DiaryLightPalette else DiaryPalette) { content() }
}
