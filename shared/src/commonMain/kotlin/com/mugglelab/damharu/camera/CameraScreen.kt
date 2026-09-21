package com.mugglelab.damharu.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.DiaryPalette
import kotlin.math.roundToInt

/** Main-thread state shared by both native camera adapters. Native zoom limits are display units. */
class CameraState {
    var minimum by mutableStateOf(1f)
    var maximum by mutableStateOf(1f)
    var zoom by mutableStateOf(1f)
    var ready by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    fun configure(minimum: Float, maximum: Float, initial: Float) {
        require(minimum.isFinite() && maximum.isFinite() && minimum > 0 && maximum >= minimum)
        this.minimum = minimum; this.maximum = maximum
        zoom = clampZoom(initial, minimum, maximum)
    }
}

fun clampZoom(value: Float, minimum: Float, maximum: Float): Float =
    if (value.isFinite()) value.coerceIn(minimum, maximum) else minimum

interface CameraActions {
    fun zoom(value: Float)
    fun capture()
    fun close()
}

// The camera can be hosted outside DiaryTheme (its own iOS view controller), so it reads the palette directly.
private val Background = DiaryPalette.background
private val Ink = DiaryPalette.ink
private val Muted = DiaryPalette.muted
private val Pointer = DiaryPalette.accent

@Composable
private fun Label(text: String, modifier: Modifier = Modifier, color: Color = Ink, size: Int = 14, mono: Boolean = true) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size.sp, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Serif))
}

@Composable
fun CameraScreen(state: CameraState, actions: CameraActions, preview: @Composable (Modifier) -> Unit) {
    val enabled = state.ready && !state.busy && state.error == null
    Column(Modifier.fillMaxSize().background(Background).safeDrawingPadding()) {
        // A dismissal action is necessary for cancellation, including devices without a back gesture.
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            Box(Modifier.size(44.dp).clickable(enabled = !state.busy, role = Role.Button, onClickLabel = "촬영 닫기") { actions.close() }
                .testTag("camera-close").semantics { contentDescription = "촬영 닫기" }, contentAlignment = Alignment.Center) {
                Label("×", size = 26)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val width = minOf(maxWidth, maxHeight * .75f)
            Box(Modifier.width(width).aspectRatio(.75f).clipToBounds()) {
                preview(Modifier.fillMaxSize())
                if (!state.ready || state.error != null) {
                    Box(Modifier.fillMaxSize().background(Background.copy(alpha = .85f)).padding(24.dp), contentAlignment = Alignment.Center) {
                        Label(state.error ?: "카메라를 준비하고 있어요…", Modifier.semantics { liveRegion = LiveRegionMode.Polite }, mono = false)
                    }
                }
                if (state.ready && state.error == null) ViewfinderGuides(Modifier.fillMaxSize())
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ZoomRuler(state, enabled) { value -> state.zoom = value; actions.zoom(value) }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(88.dp)) {
                Canvas(Modifier.align(Alignment.Center).size(80.dp).clickable(enabled, role = Role.Button) { actions.capture() }
                    .testTag("camera-shutter").semantics { contentDescription = if (state.busy) "촬영 중" else "사진 촬영" }) {
                    // Metal-look shutter: thin outer ring, black gap, off-centre highlight.
                    val radius = size.minDimension / 2
                    val alpha = if (enabled) 1f else .35f
                    drawCircle(Color(0xFF8A857A).copy(alpha = alpha), radius, style = Stroke(1.dp.toPx()))
                    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFD4CFC4), Color(0xFF807B70)),
                        center = Offset(size.width * .35f, size.height * .28f), radius = radius * 1.5f), radius - 5.dp.toPx(), alpha = alpha)
                }
            }
        }
    }
}

/** Corner brackets and a centre focus mark, drawn above the native preview like an optical viewfinder. */
@Composable
private fun ViewfinderGuides(modifier: Modifier) {
    Canvas(modifier) {
        val inset = 14.dp.toPx(); val arm = 22.dp.toPx(); val width = 1.dp.toPx()
        listOf(Offset(inset, inset) to Offset(1f, 1f), Offset(size.width - inset, inset) to Offset(-1f, 1f),
            Offset(inset, size.height - inset) to Offset(1f, -1f), Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f),
        ).forEach { (corner, direction) ->
            drawLine(Pointer, corner, corner + Offset(arm * direction.x, 0f), width)
            drawLine(Pointer, corner, corner + Offset(0f, arm * direction.y), width)
        }
        val centre = Offset(size.width / 2, size.height / 2); val half = 13.dp.toPx(); val tall = 9.dp.toPx()
        drawLine(Ink.copy(alpha = .75f), centre + Offset(-half, -tall), centre + Offset(-half, tall), width)
        drawLine(Ink.copy(alpha = .75f), centre + Offset(half, -tall), centre + Offset(half, tall), width)
    }
}

private fun zoomLabel(value: Float): String {
    val tenth = (value * 10).roundToInt()
    return if (tenth % 10 == 0) "${tenth / 10}×" else "${tenth / 10}.${tenth % 10}×"
}

@Composable
private fun ZoomRuler(state: CameraState, enabled: Boolean, onZoom: (Float) -> Unit) {
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentZoom = state.zoom
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Label(zoomLabel(state.zoom), size = 20)
        // Millimetres are omitted: a fixed 24mm conversion is incorrect on many real cameras.
        Box(Modifier.width(220.dp).height(58.dp).clipToBounds().testTag("camera-zoom")
            .semantics {
                contentDescription = "광각 및 줌 배율"
                stateDescription = zoomLabel(currentZoom)
                progressBarRangeInfo = ProgressBarRangeInfo(currentZoom, state.minimum..state.maximum)
                if (!enabled) disabled()
                setProgress { if (enabled) { currentOnZoom(clampZoom(it, state.minimum, state.maximum)); true } else false }
            }.pointerInput(enabled, state.minimum, state.maximum) {
                if (enabled) detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    currentOnZoom(clampZoom(state.zoom - drag / (180.dp.toPx()), state.minimum, state.maximum))
                }
            }) {
            Canvas(Modifier.fillMaxSize()) {
                val spacing = 18.dp.toPx()
                for (tick in (state.minimum * 10).roundToInt()..(state.maximum * 10).roundToInt()) {
                    val x = size.width / 2 + (tick - state.zoom * 10) * spacing
                    if (x in 0f..size.width) drawLine(Muted.copy(alpha = (.2f + .8f * (1f - kotlin.math.abs(x - size.width / 2) / (size.width / 2))).coerceIn(0f, 1f)), Offset(x, 13.dp.toPx()), Offset(x, (if (tick % 5 == 0) 28 else 21).dp.toPx()), 1.dp.toPx())
                }
                val x = size.width / 2
                drawLine(Pointer, Offset(x, 6.dp.toPx()), Offset(x, 30.dp.toPx()), 2.dp.toPx())
                drawPath(Path().apply { moveTo(x - 5.dp.toPx(), 4.dp.toPx()); lineTo(x + 5.dp.toPx(), 4.dp.toPx()); lineTo(x, 11.dp.toPx()); close() }, Pointer)
            }
            for (tick in (state.minimum * 2).roundToInt()..(state.maximum * 2).roundToInt()) {
                val x = 110f + (tick / 2f - state.zoom) * 180f
                if (x in 18f..202f) Box(Modifier.offset(x = (x - 18).dp, y = 34.dp).width(36.dp), contentAlignment = Alignment.Center) {
                    Label(zoomLabel(tick / 2f), color = Muted, size = 11)
                }
            }
        }
    }
}
