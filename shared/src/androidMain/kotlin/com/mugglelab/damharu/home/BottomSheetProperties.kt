package com.mugglelab.damharu.home

import androidx.compose.ui.window.DialogProperties

internal actual fun bottomSheetProperties() = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
