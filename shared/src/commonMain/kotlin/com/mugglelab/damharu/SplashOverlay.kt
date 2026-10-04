package com.mugglelab.damharu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import damharu.shared.generated.resources.Res
import damharu.shared.generated.resources.splash_photo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Composed over the already laid-out home. The real lens bounds include platform safe areas.
 * Only layout/layer phases read progress; the image is not decoded or recomposed every frame. */
@Composable
internal fun SplashOverlay(target: Rect?, ready: Boolean, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val finished by rememberUpdatedState(onFinished)
    val latestTarget by rememberUpdatedState(target)
    val latestReady by rememberUpdatedState(ready)
    val painter = painterResource(Res.drawable.splash_photo)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(650)
        // A failed/slow disk read or missing layout must never trap the user on the splash.
        withTimeoutOrNull(2500) {
            snapshotFlow { latestReady && latestTarget != null }.first { it }
        }
        progress.animateTo(1f, tween(780, easing = CubicBezierEasing(.55f, 0f, .2f, 1f)))
        finished()
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("launch-splash").pointerInput(Unit) {
        // Cover the entire home hit area even after the photograph has shrunk.
        awaitPointerEventScope {
            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }) {
        val density = LocalDensity.current
        val start = with(density) { Rect(0f, 0f, maxWidth.toPx(), maxHeight.toPx()) }
        val lens = target
        val end = lens?.let {
            val radius = it.width * .16f
            Rect(it.center.x - radius, it.center.y - radius, it.center.x + radius, it.center.y + radius)
        } ?: start
        Image(painter, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.layout { measurable, constraints ->
                val t = progress.value
                fun mix(a: Float, b: Float) = a + (b - a) * t
                val photo = measurable.measure(Constraints.fixed(
                    mix(start.width, end.width).roundToInt().coerceAtLeast(1),
                    mix(start.height, end.height).roundToInt().coerceAtLeast(1)))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    photo.place(mix(start.left, end.left).roundToInt(), mix(start.top, end.top).roundToInt())
                }
            }.graphicsLayer {
                val t = progress.value
                clip = true
                shape = RoundedCornerShape(percent = (50 * t).roundToInt())
                // Disappear inside the optical glass, revealing the real, still-clickable lens.
                alpha = if (lens == null) 1f - t else (1f - (t - .88f) / .12f).coerceIn(0f, 1f)
            })
    }
}
