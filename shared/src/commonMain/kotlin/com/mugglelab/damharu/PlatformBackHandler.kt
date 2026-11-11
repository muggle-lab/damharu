package com.mugglelab.damharu
import androidx.compose.runtime.Composable
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
