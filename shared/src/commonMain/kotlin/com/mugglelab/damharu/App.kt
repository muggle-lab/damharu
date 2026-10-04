package com.mugglelab.damharu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.camera.CAMERA_PREVIEW_CORNER_DP
import com.mugglelab.damharu.camera.cameraPreviewRect
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.math.roundToInt

/** Home → camera → saved. A capture is stored immediately; there is no picker, editor or date choice. */
@Composable
fun App(today: LocalDate, photoSource: PhotoSource) {
    val store = remember { DiaryStore() }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<Record>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.month.number) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var route by rememberSaveable { mutableStateOf("home") }
    var splashVisible by rememberSaveable { mutableStateOf(route == "home") }
    var captureBounds by remember { mutableStateOf<Rect?>(null) }
    var memo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableIntStateOf(0) }
    // A fresh capture is shown at once and, when saved, shrinks into today's calendar cell.
    var flight by remember { mutableStateOf<CaptureFlight?>(null) }
    var todayBounds by remember { mutableStateOf<Rect?>(null) }
    fun load() { scope.launch {
        busy = true
        try { records = withContext(Dispatchers.Default) { store.load() }; loaded = true }
        catch (_: Exception) { error = "기록을 불러오지 못했어요. 기존 데이터는 유지됩니다. 다시 시도해 주세요." }
        finally { busy = false }
    } }
    LaunchedEffect(Unit) { load() }
    DisposableEffect(Unit) { onDispose { requestId++ } }
    fun save(capture: String, film: Film) { scope.launch {
        busy = true
        year = today.year; month = today.month.number; selected = today.toString(); memo = null; route = "home"
        flight = CaptureFlight(capture, film)
        try {
            records = withContext(Dispatchers.Default) { store.save(records, today, capture, film) }
            flight = flight?.copy(saved = true) // The temporary capture stays until the flight has landed.
        } catch (_: Exception) {
            flight = null; photoSource.closeCamera(); runCatching { PhotoFiles.delete(capture) }
            error = "사진을 저장하지 못했어요. 여유 공간을 확인하고 다시 촬영해 주세요."
        } finally { busy = false }
    } }
    fun capture() {
        if (capturing || busy || !loaded) return
        capturing = true
        val id = ++requestId
        photoSource.capture { path, film, failure ->
            if (id != requestId) { path?.let { runCatching { PhotoFiles.delete(it) } } }
            else {
                capturing = false
                if (failure != null) error = failure
                else if (path != null) save(path, Film.fromId(film)) // The shutter is the save button: no confirm step after capture.
            }
        }
    }
    /** Replacing today's photo asks first; the camera then saves straight over it. */
    fun requestCapture() { if (records.any { it.date == today.toString() }) confirmation = "replace" else capture() }
    fun closeScreen() {
        if (memo != null && memo != records.find { it.date == selected }?.memo) confirmation = "memo-discard"
        else { memo = null; route = "home" }
    }
    PlatformBackHandler(route != "home" && !busy) { closeScreen() }
    DiaryTheme {
        val colors = LocalDiaryColors.current
        val current = records.find { it.date == selected }
        if (route == "home") Box(Modifier.fillMaxSize()) {
            HomeScreen(today, CalendarMonth(year, month), records.associate { LocalDate.parse(it.date) to DiaryEntry(LocalDate.parse(it.date), imagePath = store.path(it.thumbnail)) },
                { year = it.year; month = it.month }, { action ->
                    if (loaded && !busy && flight == null && !splashVisible) when (action) {
                        is HomeAction.AddRecord -> if (canRecord(action.date, today)) requestCapture()
                        is HomeAction.OpenRecord -> { selected = action.date.toString(); memo = null; route = "detail" }
                    }
                }, Modifier.fillMaxSize().then(if (splashVisible) Modifier.clearAndSetSemantics {} else Modifier),
                arrivingDate = if (flight != null) today else null, onTodayBounds = { todayBounds = it },
                onCaptureBounds = { captureBounds = it }, demoControls = {
                    if (!loaded && !busy) DiaryButton("기록 다시 불러오기", { load() })
                })
            flight?.let { active -> CaptureFlightOverlay(active, todayBounds, onReady = { photoSource.closeCamera() }) { flight = null; runCatching { PhotoFiles.delete(active.path) } } }
            if (splashVisible) SplashOverlay(captureBounds, ready = loaded || error != null) { splashVisible = false }
        } else if (current == null) {
            Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DiaryText("기록이 없어요."); DiaryButton("홈으로", { route = "home" })
            }
        } else {
            val date = LocalDate.parse(current.date)
            val index = records.indexOf(current)
            fun navigate(delta: Int) { if (memo == null && index + delta in records.indices) selected = records[index + delta].date }
            Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clickable(role = Role.Button) { closeScreen() }.testTag("close-screen").semantics { contentDescription = "닫기" }, contentAlignment = Alignment.Center) {
                        BasicText("‹", style = DiaryTextStyle.copy(color = colors.ink, fontSize = 26.sp))
                    }
                    Spacer(Modifier.weight(1f))
                    if (memo == null) {
                        // Only today's photo can be retaken; past days keep the moment they were taken.
                        if (canRecord(date, today)) TextAction("사진 변경", colors.soft, "change-photo") { requestCapture() }
                        TextAction("삭제", colors.danger, "delete-record") { confirmation = "delete" }
                    }
                }
                if (memo == null) {
                    var drag by remember { mutableFloatStateOf(0f) }
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                        LocalPhoto(store.path(current.image), Modifier.aspectRatio(.75f).clip(RoundedCornerShape(3.dp)).pointerInput(selected) {
                            detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                                if (drag < -70f) navigate(1) else if (drag > 70f) navigate(-1)
                            }) { change, amount -> change.consume(); drag += amount }
                        })
                    }
                }
                Column(Modifier.fillMaxWidth().then(if (memo != null) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(18.dp))
                    // The film is part of how the day was taken, so it sits with the date like an edge print.
                    DiaryMono(listOf(date.monoLabel, Film.fromId(current.film).monoName).filter { it.isNotEmpty() }.joinToString(" · "), Modifier.testTag("record-meta"))
                    Spacer(Modifier.height(8.dp))
                    BasicText(date.koreanLabel, style = DiaryTextStyle.copy(color = colors.ink, fontSize = 22.sp, lineHeight = 30.sp))
                    if (memo == null) {
                        BasicText(current.memo.ifEmpty { "오늘을 한 줄로 남겨보세요" },
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClickLabel = "한 줄 수정") { memo = current.memo }
                                .padding(top = 10.dp, bottom = 14.dp).testTag("edit-memo"),
                            style = DiaryTextStyle.copy(color = if (current.memo.isEmpty()) colors.faint else colors.soft, fontSize = 14.sp, lineHeight = 24.sp))
                        Hairline(Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            NeighbourLink(records.getOrNull(index - 1)?.let { "‹ ${LocalDate.parse(it.date).shortMonoLabel}" } ?: "‹ 이전", index > 0, "previous-record") { navigate(-1) }
                            NeighbourLink(records.getOrNull(index + 1)?.let { "${LocalDate.parse(it.date).shortMonoLabel} ›" } ?: "다음 ›", index < records.lastIndex, "next-record") { navigate(1) }
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        MemoInput(memo.orEmpty(), { memo = it })
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiaryButton("취소", { memo = null }, Modifier.weight(1f))
                            DiaryButton("한 줄 저장", { scope.launch {
                                busy = true
                                try { records = withContext(Dispatchers.Default) { store.memo(records, selected, memo.orEmpty()) }; memo = null }
                                catch (_: Exception) { error = "메모를 저장하지 못했어요. 다시 시도해 주세요." }
                                finally { busy = false }
                            } }, Modifier.weight(1f).testTag("save-memo"), primary = true)
                        }
                    }
                }
            }
        }
        confirmation?.let { kind ->
            val message = when (kind) {
                "delete" -> if (selected == today.toString()) "이 날의 기록을 삭제할까요? 사진과 메모가 기기에서 삭제됩니다."
                    else "이 날의 기록을 삭제할까요? 사진과 메모가 기기에서 삭제되고, 지나간 날은 다시 채울 수 없어요."
                "replace" -> "오늘의 사진을 다시 찍을까요? 촬영하면 바로 교체되고 한 줄 메모는 유지됩니다."
                else -> "저장하지 않은 변경을 닫을까요?"
            }
            DiaryDialog(message, { confirmation = null }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiaryButton("취소", { confirmation = null }, Modifier.weight(1f), shape = DiaryDialogShape)
                    DiaryButton("확인", {
                        confirmation = null
                        when (kind) {
                            "replace" -> capture()
                            "memo-discard" -> { memo = null; route = "home" }
                            "delete" -> scope.launch {
                                busy = true
                                try { records = withContext(Dispatchers.Default) { store.delete(records, selected) }; route = "home" }
                                catch (_: Exception) { error = "삭제하지 못했어요. 다시 시도해 주세요." }
                                finally { busy = false }
                            }
                        }
                    }, Modifier.weight(1f).testTag("confirm-action"), primary = true, shape = DiaryDialogShape)
                }
            }
        }
        if (!splashVisible) error?.let { DiaryDialog(it, { error = null }) { DiaryButton("확인", { error = null }, Modifier.fillMaxWidth(), shape = DiaryDialogShape) } }
        // While a capture is saving, the photo itself is on screen; a wait dialog would only cover it.
        if (busy && flight == null && !splashVisible) DiaryDialog("잠시만 기다려 주세요…", {}) {}
    }
}

private data class CaptureFlight(val path: String, val film: Film, val saved: Boolean = false)

/** The fresh capture leaves the camera and shrinks straight into today's cell. The overlay is prepared
 *  *under* the still-open camera at the exact viewfinder bounds; only when its pixels are decoded is the
 *  camera closed (instantly), so there is no in-between screen. The cell is square and the photo 3:4, so
 *  width and height travel separately while the image stays cropped. */
@Composable
private fun CaptureFlightOverlay(flight: CaptureFlight, target: Rect?, onReady: () -> Unit, onLanded: () -> Unit) {
    val progress = remember(flight.path) { Animatable(0f) }
    val ready by rememberUpdatedState(onReady)
    val landed by rememberUpdatedState(onLanded)
    val latestTarget by rememberUpdatedState(target)
    val latestFlight by rememberUpdatedState(flight)
    var bitmap by remember(flight.path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(flight.path) {
        bitmap = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(flight.path) }.getOrNull() }
        withFrameNanos { } // Let the decoded photo reach the screen before the camera above it goes away.
        ready()
        // Without pixels or a laid-out cell there is nothing to fly; the calendar simply shows the new day.
        if (bitmap != null && latestTarget != null) progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        // Saving usually finishes mid-flight. If it has not, rest on the cell until its thumbnail exists.
        val late = !latestFlight.saved
        snapshotFlow { latestFlight.saved }.first { it }
        if (late) delay(220)
        landed()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val insets = WindowInsets.safeDrawing
        val start = with(density) { cameraPreviewRect(maxWidth.toPx(), maxHeight.toPx(), insets.getTop(this).toFloat(), insets.getBottom(this).toFloat(), this.density) }
        val end = target ?: start
        bitmap?.let {
            // The animated value is read only in the layout and draw phases, so a 60-120Hz flight never
            // recomposes: each frame is one measure/place of this image and a redraw.
            Image(it, "방금 찍은 사진", Modifier
                .layout { measurable, constraints ->
                    val t = progress.value
                    val placeable = measurable.measure(Constraints.fixed(
                        lerp(start.width, end.width, t).roundToInt().coerceAtLeast(1), lerp(start.height, end.height, t).roundToInt().coerceAtLeast(1)))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(lerp(start.left, end.left, t).roundToInt(), lerp(start.top, end.top, t).roundToInt())
                    }
                }
                .graphicsLayer { clip = true; shape = RoundedCornerShape(lerp(CAMERA_PREVIEW_CORNER_DP.toFloat(), 2f, progress.value).dp) }
                .filmTexture(flight.film.texture).testTag("capture-flight"),
                contentScale = ContentScale.Crop,
                // The capture is still unfiltered here; give it the film look so it matches the saved photo it turns into.
                colorFilter = flight.film.matrix?.let { values -> ColorFilter.colorMatrix(ColorMatrix(values)) })
        }
    }
}

private fun lerp(from: Float, to: Float, fraction: Float) = from + (to - from) * fraction

@Composable
private fun TextAction(text: String, color: Color, tag: String, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp).testTag(tag), contentAlignment = Alignment.Center) {
        BasicText(text, style = DiaryTextStyle.copy(color = color, fontSize = 13.sp))
    }
}

@Composable
private fun NeighbourLink(text: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    Box(Modifier.heightIn(min = 44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag), contentAlignment = Alignment.Center) {
        DiaryMono(text, color = if (enabled) colors.soft else Color(0xFF2A2824))
    }
}

@Composable
private fun DiaryDialog(message: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalDiaryColors.current
    val shape = DiaryDialogShape
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().background(colors.panel, shape).border(1.dp, colors.line, shape).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) { DiaryText(message); content() }
    }
}
