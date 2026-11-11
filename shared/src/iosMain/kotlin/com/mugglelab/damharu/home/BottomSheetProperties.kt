package com.mugglelab.damharu.home

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun bottomSheetProperties() = DialogProperties(
    usePlatformDefaultWidth = false,
    usePlatformInsets = false,
    scrimColor = Color.Transparent,
    animateTransition = false,
)
