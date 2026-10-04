package com.mugglelab.damharu.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.DiaryPalette
import com.mugglelab.damharu.filmTexture
import com.mugglelab.damharu.data.Film
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** Main-thread state shared by both native camera adapters. Native zoom limits are display units. */
class CameraState {
    var minimum by mutableStateOf(1f)
    var maximum by mutableStateOf(1f)
    var zoom by mutableStateOf(1f)
    var ready by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    /** The film loaded for the next shot. The native adapter tints its live preview to match. */
    var film by mutableStateOf(Film.None)
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
    fun film(value: Film)
    fun capture()
    fun close()
}

/** Vertical layout of the camera screen in dp. [cameraPreviewRect] mirrors it so other screens can line up
 *  with the viewfinder; change these together with the layout below. */
const val CAMERA_TOP_BAR_DP = 44
const val CAMERA_CONTROLS_DP = 224
const val CAMERA_PREVIEW_SIDE_DP = 12

/** Window-relative bounds of the 3:4 viewfinder, in pixels, for a window of [width] x [height]. */
fun cameraPreviewRect(width: Float, height: Float, safeTop: Float, safeBottom: Float, density: Float): Rect {
    val top = safeTop + CAMERA_TOP_BAR_DP * density
    val bottom = height - safeBottom - CAMERA_CONTROLS_DP * density
    val previewWidth = minOf(width - 2 * CAMERA_PREVIEW_SIDE_DP * density, (bottom - top) * .75f).coerceAtLeast(0f)
    val previewHeight = previewWidth / .75f
    val left = (width - previewWidth) / 2; val y = top + ((bottom - top) - previewHeight) / 2
    return Rect(left, y, left + previewWidth, y + previewHeight)
}

/** Corner radius of the live preview in dp. Compose cannot round a native interop view on every platform,
 *  so the iOS and Android adapters round their own preview view with this same value. */
const val CAMERA_PREVIEW_CORNER_DP = 20

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
    var filmsOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Background).safeDrawingPadding()) {
        // A dismissal action is necessary for cancellation, including devices without a back gesture.
        Box(Modifier.fillMaxWidth().height(CAMERA_TOP_BAR_DP.dp)) {
            Box(Modifier.size(44.dp).clickable(enabled = !state.busy, role = Role.Button, onClickLabel = "촬영 닫기") { actions.close() }
                .testTag("camera-close").semantics { contentDescription = "촬영 닫기" }, contentAlignment = Alignment.Center) {
                Label("×", size = 26)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val width = minOf(maxWidth - (2 * CAMERA_PREVIEW_SIDE_DP).dp, maxHeight * .75f)
            Box(Modifier.width(width).aspectRatio(.75f).clip(RoundedCornerShape(CAMERA_PREVIEW_CORNER_DP.dp))) {
                preview(Modifier.fillMaxSize())
                if (!state.ready || state.error != null) {
                    Box(Modifier.fillMaxSize().background(Background.copy(alpha = .85f)).padding(24.dp), contentAlignment = Alignment.Center) {
                        Label(state.error ?: "카메라를 준비하고 있어요…", Modifier.semantics { liveRegion = LiveRegionMode.Polite }, mono = false)
                    }
                }
                if (state.ready && state.error == null) {
                    // Grain and vignette are drawn by Compose above the native preview with the same numbers the export burns in.
                    Box(Modifier.fillMaxSize().filmTexture(state.film.texture))
                    ViewfinderGuides(Modifier.fillMaxSize())
                    if (state.film != Film.None) Label(state.film.monoName, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 44.dp)
                        .background(Color.Black.copy(alpha = .45f), RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp), size = 10)
                }
            }
        }
        Column(Modifier.fillMaxWidth().height(CAMERA_CONTROLS_DP.dp).padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Same slot for both, so opening the film list never moves the shutter.
            Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                if (filmsOpen) FilmList(state.film, enabled) { film -> state.film = film; actions.film(film) }
                else ZoomRuler(state, enabled) { value -> state.zoom = value; actions.zoom(value) }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(88.dp)) {
                Canvas(Modifier.align(Alignment.Center).size(80.dp).alpha(if (enabled) 1f else .35f).clickable(enabled, role = Role.Button) { actions.capture() }
                    .testTag("camera-shutter").semantics { contentDescription = if (state.busy) "촬영 중" else "사진 촬영" }) {
                    drawLensShutter()
                }
                FilmButton(state.film, filmsOpen, enabled, Modifier.align(Alignment.CenterEnd).padding(end = 26.dp)) { filmsOpen = !filmsOpen }
            }
        }
    }
}

/** Right of the shutter, like the film door of a compact camera. Accent color means a film is loaded or the list is open. */
@Composable
private fun FilmButton(film: Film, open: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val active = open || film != Film.None
    val color = (if (active) Pointer else Ink).copy(alpha = if (enabled) 1f else .35f)
    Column(modifier.width(64.dp).clickable(enabled, role = Role.Button, onClick = onClick).testTag("camera-films")
        .semantics(mergeDescendants = true) { contentDescription = "필름 선택, ${film.label}" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = Stroke(1.dp.toPx())
            drawCircle(if (active) color else DiaryPalette.faint, size.minDimension / 2 - stroke.width, style = stroke)
            // A 35mm cartridge: body, spool cap and the film leader coming out of it.
            val w = 11.dp.toPx(); val h = 16.dp.toPx(); val left = size.width / 2 - 9.dp.toPx(); val top = (size.height - h) / 2 + 1.dp.toPx()
            drawRoundRect(color, Offset(left, top), Size(w, h), CornerRadius(1.5.dp.toPx()), style = stroke)
            drawLine(color, Offset(left + w * .3f, top - 2.5.dp.toPx()), Offset(left + w * .7f, top - 2.5.dp.toPx()), 2.dp.toPx())
            drawLine(color, Offset(left + w, top + 3.dp.toPx()), Offset(left + w + 8.dp.toPx(), top + 3.dp.toPx()), stroke.width)
            drawLine(color, Offset(left + w, top + h - 3.dp.toPx()), Offset(left + w + 8.dp.toPx(), top + h - 3.dp.toPx()), stroke.width)
            drawLine(color, Offset(left + w + 8.dp.toPx(), top + 3.dp.toPx()), Offset(left + w + 8.dp.toPx(), top + h - 3.dp.toPx()), stroke.width)
        }
        Spacer(Modifier.height(5.dp))
        Label(if (film == Film.None) "필름" else film.label, color = if (film == Film.None) Muted else Ink, size = 11, mono = false)
    }
}

/** One tile per film. The swatch is a fixed sky/skin/leaf sample drawn through the film's own matrix,
 *  so a tile shows its look without needing a live camera frame. */
@Composable
private fun FilmList(selected: Film, enabled: Boolean, onSelect: (Film) -> Unit) {
    Row(Modifier.testTag("camera-film-list"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Film.entries.forEach { film ->
            val on = film == selected
            Column(Modifier.width(72.dp).clickable(enabled, role = Role.Button) { onSelect(film) }.testTag("camera-film-${film.name}")
                .semantics(mergeDescendants = true) { contentDescription = film.label; this.selected = on }, horizontalAlignment = Alignment.CenterHorizontally) {
                val shape = RoundedCornerShape(10.dp)
                Box(Modifier.size(48.dp).then(if (on) Modifier.border(1.dp, Pointer, shape).padding(3.dp) else Modifier.padding(3.dp)).clip(shape).filmTexture(film.texture)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val tint = film.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
                        val band = size.height / 3
                        listOf(Color(0xFF4F86C6), Color(0xFFD9A77F), Color(0xFF4E7A45)).forEachIndexed { index, sample ->
                            drawRect(sample, Offset(0f, band * index), Size(size.width, band + 1f), colorFilter = tint)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Label(film.label, color = if (on) Ink else Muted, size = 11, mono = false)
                Box(Modifier.padding(top = 3.dp).size(14.dp, 1.dp).background(if (on) Pointer else Color.Transparent))
            }
        }
    }
}

/** Corner brackets and a centre focus mark, drawn above the native preview like an optical viewfinder. */
@Composable
private fun ViewfinderGuides(modifier: Modifier) {
    Canvas(modifier) {
        val inset = 20.dp.toPx(); val arm = 20.dp.toPx(); val width = 1.dp.toPx()
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

/** The ruler is laid out by zoom *ratio* (log2), not by zoom value: the same drag distance always multiplies
 *  the zoom by the same factor, so 0.5x→1x feels exactly like 3x→6x. A linear ruler rushes through the
 *  wide end and crawls at the tele end. */
fun zoomPosition(zoom: Float): Float = ln(zoom) / ln(2f)
fun zoomAtPosition(position: Float): Float = 2f.pow(position)

data class ZoomTick(val zoom: Float, val major: Boolean)

/** Tenths up to 2x, quarters up to 3x and halves above, so ticks never crowd on the log scale. Majors carry labels. */
fun zoomTicks(minimum: Float, maximum: Float): List<ZoomTick> {
    val majors = setOf(50, 100, 150, 200, 300, 400, 500, 600, 800, 1000)
    val hundredths = buildList {
        var value = 10
        while (value <= 1000) { add(value); value += if (value < 200) 10 else if (value < 300) 25 else 50 }
    }
    val low = (minimum * 100).roundToInt(); val high = (maximum * 100).roundToInt()
    return hundredths.filter { it in low..high }.map { ZoomTick(it / 100f, it in majors) }
}

private const val RULER_DP_PER_DOUBLING = 96f

@Composable
private fun ZoomRuler(state: CameraState, enabled: Boolean, onZoom: (Float) -> Unit) {
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentZoom = state.zoom
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val pxPerDoubling = with(LocalDensity.current) { RULER_DP_PER_DOUBLING.dp.toPx() }
    // The ruler position (log2 of zoom) is the single animated value: drags snap it, releases let it decay.
    val position = remember { Animatable(zoomPosition(state.zoom)) }
    val ticks = remember(state.minimum, state.maximum) { zoomTicks(state.minimum, state.maximum) }
    var lastMajor by remember { mutableStateOf<Float?>(null) }
    fun push() {
        val zoom = clampZoom(zoomAtPosition(position.value), state.minimum, state.maximum)
        // A light tick when the pointer passes a labelled mark, like a detent on a lens ring.
        val passed = ticks.lastOrNull { it.major && it.zoom <= zoom + .001f }?.zoom
        if (lastMajor != null && passed != lastMajor) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastMajor = passed
        currentOnZoom(zoom)
    }
    // The native adapter reports its real zoom range after the first frame.
    LaunchedEffect(state.minimum, state.maximum) {
        position.updateBounds(zoomPosition(state.minimum), zoomPosition(state.maximum))
        position.snapTo(zoomPosition(state.zoom))
    }
    // Zoom changed from outside the ruler (accessibility action, a rejected native zoom): follow it.
    LaunchedEffect(state.zoom) {
        if (!position.isRunning && abs(zoomAtPosition(position.value) - state.zoom) > .005f) position.snapTo(zoomPosition(state.zoom))
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    val labels = remember(ticks) { ticks.filter { it.major }.associate { it.zoom to textMeasurer.measure(zoomLabel(it.zoom), labelStyle) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Label(zoomLabel(state.zoom), size = 20)
        // Millimetres are omitted: a fixed 24mm conversion is incorrect on many real cameras.
        Box(Modifier.width(260.dp).height(58.dp).clipToBounds().testTag("camera-zoom")
            .semantics {
                contentDescription = "광각 및 줌 배율"
                stateDescription = zoomLabel(currentZoom)
                progressBarRangeInfo = ProgressBarRangeInfo(currentZoom, state.minimum..state.maximum)
                if (!enabled) disabled()
                setProgress { if (enabled) { currentOnZoom(clampZoom(it, state.minimum, state.maximum)); true } else false }
            }.draggable(
                rememberDraggableState { delta -> scope.launch { position.snapTo(position.value - delta / pxPerDoubling); push() } },
                Orientation.Horizontal, enabled = enabled,
                onDragStarted = { position.stop() },
                // Releasing with speed lets the ruler coast to a stop instead of freezing under the finger.
                onDragStopped = { velocity -> position.animateDecay(-velocity / pxPerDoubling, exponentialDecay(frictionMultiplier = 3f)) { push() } },
            )) {
            // Reading the animated position only inside draw keeps a 60-120Hz fling out of recomposition.
            Canvas(Modifier.fillMaxSize()) {
                val centre = size.width / 2
                val here = position.value
                ticks.forEach { tick ->
                    val x = centre + (zoomPosition(tick.zoom) - here) * pxPerDoubling
                    if (x < -20.dp.toPx() || x > size.width + 20.dp.toPx()) return@forEach
                    val alpha = (.15f + .85f * (1f - abs(x - centre) / centre)).coerceIn(0f, 1f)
                    drawLine(Muted.copy(alpha = alpha), Offset(x, 13.dp.toPx()), Offset(x, (if (tick.major) 28 else 20).dp.toPx()), 1.dp.toPx())
                    labels[tick.zoom]?.let { drawText(it, topLeft = Offset(x - it.size.width / 2f, 35.dp.toPx()), alpha = alpha) }
                }
                drawLine(Pointer, Offset(centre, 6.dp.toPx()), Offset(centre, 30.dp.toPx()), 2.dp.toPx())
                drawPath(Path().apply { moveTo(centre - 5.dp.toPx(), 4.dp.toPx()); lineTo(centre + 5.dp.toPx(), 4.dp.toPx()); lineTo(centre, 11.dp.toPx()); close() }, Pointer)
            }
        }
    }
}
