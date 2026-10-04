package com.mugglelab.damharu

import androidx.compose.ui.window.ComposeUIViewController
import com.mugglelab.damharu.data.PhotoSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun MainViewController(photoSource: PhotoSource, dateOverride: String) = ComposeUIViewController {
    val today = if (dateOverride.isNotBlank()) LocalDate.parse(dateOverride)
        else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    App(today, photoSource)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun CameraViewController(
    state: com.mugglelab.damharu.camera.CameraState,
    actions: com.mugglelab.damharu.camera.CameraActions,
    preview: platform.UIKit.UIView,
) = ComposeUIViewController {
    com.mugglelab.damharu.camera.CameraScreen(state, actions) { modifier ->
        androidx.compose.ui.viewinterop.UIKitView(factory = { preview }, modifier = modifier)
    }
}
