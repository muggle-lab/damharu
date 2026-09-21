package com.mugglelab.damharu

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*

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
    var memo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableIntStateOf(0) }
    fun load() { scope.launch {
        busy = true
        try { records = withContext(Dispatchers.Default) { store.load() }; loaded = true }
        catch (_: Exception) { error = "기록을 불러오지 못했어요. 기존 데이터는 유지됩니다. 다시 시도해 주세요." }
        finally { busy = false }
    } }
    LaunchedEffect(Unit) { load() }
    DisposableEffect(Unit) { onDispose { requestId++ } }
    fun save(capture: String) { scope.launch {
        busy = true
        try {
            records = withContext(Dispatchers.Default) { store.save(records, today, capture) }
            year = today.year; month = today.month.number; selected = today.toString(); memo = null; route = "home"
        } catch (_: Exception) { error = "사진을 저장하지 못했어요. 여유 공간을 확인하고 다시 촬영해 주세요." }
        finally { runCatching { PhotoFiles.delete(capture) }; busy = false }
    } }
    fun capture() {
        if (capturing || busy || !loaded) return
        capturing = true
        val id = ++requestId
        photoSource.capture { path, failure ->
            if (id != requestId) { path?.let { runCatching { PhotoFiles.delete(it) } } }
            else {
                capturing = false
                if (failure != null) error = failure
                else if (path != null) save(path) // The shutter is the save button: no confirm step after capture.
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
        if (route == "home") {
            HomeScreen(today, CalendarMonth(year, month), records.associate { LocalDate.parse(it.date) to DiaryEntry(LocalDate.parse(it.date), imagePath = store.path(it.thumbnail)) },
                { year = it.year; month = it.month }, { action ->
                    if (loaded && !busy) when (action) {
                        is HomeAction.AddRecord -> if (canRecord(action.date, today)) requestCapture()
                        is HomeAction.OpenRecord -> { selected = action.date.toString(); memo = null; route = "detail" }
                    }
                }, Modifier.fillMaxSize(), demoControls = {
                    if (!loaded && !busy) DiaryButton("기록 다시 불러오기", { load() })
                })
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
                        }, film = true)
                    }
                }
                Column(Modifier.fillMaxWidth().then(if (memo != null) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(18.dp))
                    DiaryMono(date.monoLabel)
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
                    DiaryButton("취소", { confirmation = null }, Modifier.weight(1f))
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
                    }, Modifier.weight(1f).testTag("confirm-action"), primary = true)
                }
            }
        }
        error?.let { DiaryDialog(it, { error = null }) { DiaryButton("확인", { error = null }, Modifier.fillMaxWidth()) } }
        if (busy) DiaryDialog("잠시만 기다려 주세요…", {}) {}
    }
}

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
    val shape = RoundedCornerShape(4.dp)
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().background(colors.panel, shape).border(1.dp, colors.line, shape).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) { DiaryText(message); content() }
    }
}
