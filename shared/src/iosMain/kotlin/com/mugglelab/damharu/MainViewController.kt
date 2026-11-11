package com.mugglelab.damharu

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.useContents
import platform.UIKit.UIViewController
import com.mugglelab.damharu.data.PhotoSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.native.ref.WeakReference

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
fun MainViewController(photoSource: PhotoSource, dateOverride: String, appearance: AppearanceState, startup: StartupTransition, onTabsVisible: (Boolean) -> Unit): UIViewController {
    var controllerReference: WeakReference<UIViewController>? = null
    val controller = ComposeUIViewController {
        val today = if (dateOverride.isNotBlank()) LocalDate.parse(dateOverride)
            else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        App(today, photoSource, appearance, onTabsVisible, startup = startup, captureViewport = {
            controllerReference?.get()?.view?.window?.let { window ->
                val size = window.bounds.useContents { size.width.toFloat() to size.height.toFloat() }
                val insets = window.safeAreaInsets.useContents { top.toFloat() to bottom.toFloat() }
                CaptureViewport(size.first, size.second, insets.first, insets.second)
            }
        })
    }
    controllerReference = WeakReference(controller)
    return controller
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun CameraViewController(
    state: com.mugglelab.damharu.camera.CameraState,
    actions: com.mugglelab.damharu.camera.CameraActions,
    preview: platform.UIKit.UIView,
    appearance: AppearanceState,
) = ComposeUIViewController {
    DiaryTheme(appearance.lightMode) {
        com.mugglelab.damharu.camera.CameraScreen(state, actions) { modifier ->
            androidx.compose.ui.viewinterop.UIKitView(factory = { preview }, modifier = modifier)
        }
    }
}

fun SettingsViewController(appearance: AppearanceState) = ComposeUIViewController {
    DiaryTheme(appearance.lightMode) { SettingsScreen(appearance) }
}

/** Transparent, full-window composition above the native TabView. Never changes home safe areas. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun SplashViewController(startup: StartupTransition, holdMillis: Long, onFinished: () -> Unit): UIViewController =
    ComposeUIViewController(configure = { opaque = false }) {
        if (startup.visible) SplashOverlay(startup.target, startup.ready, holdMillis) {
            startup.visible = false
            onFinished()
        }
    }

fun FilmShelfViewController(appearance: AppearanceState) = ComposeUIViewController {
    DiaryTheme(appearance.lightMode) { FilmShelfScreen() }
}
