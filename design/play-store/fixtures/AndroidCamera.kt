package com.mugglelab.damharu.android

import android.graphics.BitmapFactory
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import com.mugglelab.damharu.camera.*
import com.mugglelab.damharu.data.Film
import java.io.File

/** Screenshot-only replacement for camera sensor input. Shared controls and film overlay are unchanged. */
@Composable
fun AndroidCamera(activity: ComponentActivity, onComplete: (File?, Film) -> Unit) {
    val state = remember { CameraState().apply { configure(1f, 6f, 1f); ready = true } }
    val photo = remember { activity.assets.open("store-preview.jpg").use { BitmapFactory.decodeStream(it) } }
    val actions = remember { object : CameraActions {
        override fun zoom(value: Float) {}
        override fun film(value: Film) {}
        override fun capture() {}
        override fun close() { onComplete(null, Film.None) }
    } }
    CameraScreen(state, actions) { modifier ->
        AndroidView(factory = { ImageView(it).apply {
            setImageBitmap(photo); scaleType = ImageView.ScaleType.FIT_CENTER
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, CAMERA_PREVIEW_CORNER_DP * view.resources.displayMetrics.density)
                }
            }
            clipToOutline = true
        } }, modifier = modifier, update = { view ->
            view.colorFilter = state.film.matrix?.let { ColorMatrixColorFilter(it) }
        })
    }
}
