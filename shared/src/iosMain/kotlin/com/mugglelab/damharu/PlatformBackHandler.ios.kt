package com.mugglelab.damharu
import androidx.compose.runtime.Composable
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) { /* iOS uses the visible close action. */ }
