package com.mugglelab.damharu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*

@Composable
fun App(today: LocalDate, photoSource: PhotoSource) {
    val store = remember { DiaryStore() }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<Record>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.month.number) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var route by rememberSaveable { mutableStateOf("home") }
    var source by remember { mutableStateOf<String?>(null) }
    var sourceReturn by rememberSaveable { mutableStateOf("home") }
    var sourceReturnDate by rememberSaveable { mutableStateOf(today.toString()) }
    var filter by remember { mutableStateOf(PhotoFilter.Original) }
    var intensity by remember { mutableStateOf(1f) }
    var memo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var requestId by remember { mutableIntStateOf(0) }
    fun discardSource() { source?.let { runCatching { PhotoFiles.delete(it) } }; source = null }
    fun load() { scope.launch {
        busy = true
        try { records = withContext(Dispatchers.Default) { store.load() }; loaded = true }
        catch (_: Exception) { error = "기록을 불러오지 못했어요. 기존 데이터는 유지됩니다. 다시 시도해 주세요." }
        finally { busy = false }
    } }
    LaunchedEffect(Unit) { load() }
    DisposableEffect(Unit) { onDispose { requestId++; discardSource() } }
    fun save() { scope.launch {
        val input = source ?: return@launch
        busy = true
        try {
            records = withContext(Dispatchers.Default) { store.save(records, LocalDate.parse(selected), input, filter, intensity) }
            val date = LocalDate.parse(selected); year = date.year; month = date.month.number
            discardSource(); route = "home"
        } catch (_: Exception) { error = "사진을 저장하지 못했어요. 여유 공간을 확인하고 다시 시도해 주세요." }
        finally { busy = false }
    } }
    fun pick(camera: Boolean) {
        if (picking) return
        picking = true
        val id = ++requestId
        photoSource.request(camera) { path, failure ->
            if (id != requestId) { path?.let { runCatching { PhotoFiles.delete(it) } } }
            else {
                picking = false
                if (failure != null) error = failure
                else if (path != null) { discardSource(); source = path; filter = PhotoFilter.Original; intensity = 1f; route = "editor" }
            }
        }
    }
    fun closeScreen() {
        if (picking) { requestId++; picking = false }
        if (route == "editor") confirmation = "discard"
        else if (memo != null && memo != records.find { it.date == selected }?.memo) confirmation = "memo-discard"
        else {
            memo = null
            if (route == "source" && sourceReturn == "detail") selected = sourceReturnDate
            route = if (route == "source") sourceReturn else "home"
        }
    }
    PlatformBackHandler(route != "home" && !busy) { closeScreen() }
    DiaryTheme {
        val colors = LocalDiaryColors.current
        val current = records.find { it.date == selected }
        if (route == "home") {
            HomeScreen(today, CalendarMonth(year, month), records.associate { LocalDate.parse(it.date) to DiaryEntry(LocalDate.parse(it.date), imagePath = store.path(it.thumbnail)) },
                { year = it.year; month = it.month }, { action ->
                    if (loaded && !busy) when (action) {
                        is HomeAction.AddRecord -> { selected = action.date.toString(); sourceReturn = "home"; route = "source" }
                        is HomeAction.OpenRecord -> { selected = action.date.toString(); memo = null; route = "detail" }
                    }
                }, Modifier.fillMaxSize(), demoControls = {
                    if (!loaded && !busy) DiaryButton("기록 다시 불러오기", { load() })
                })
        } else if (route == "editor") {
            Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DiaryButton("닫기", { closeScreen() }, Modifier.testTag("close-screen"))
                val input = source
                if (input == null) {
                    DiaryText("사진을 다시 선택해 주세요.")
                    DiaryButton("사진 선택", { route = "source" })
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilterEditor(input, selected, filter, intensity, { filter = it }, { intensity = it })
                    }
                    DiaryButton(if (filter == PhotoFilter.Original) "이대로 남기기" else "이 사진으로 남기기",
                        { if (current != null) confirmation = "replace" else save() }, Modifier.fillMaxWidth().testTag("save-photo"))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DiaryButton("닫기", { closeScreen() }, Modifier.testTag("close-screen"))
                when (route) {
                    "source" -> {
                        DiaryText(if (picking) "사진을 준비하고 있어요…" else "한 장으로 남기는 하루")
                        DiaryText(selected, secondary = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DiaryButton("이전 날짜", { selected = LocalDate.parse(selected).minus(1, DateTimeUnit.DAY).toString() })
                            if (LocalDate.parse(selected) < today) DiaryButton("다음 날짜", { selected = LocalDate.parse(selected).plus(1, DateTimeUnit.DAY).toString() })
                        }
                        DiaryText("날짜를 고르고, 기억하고 싶은 사진을 골라 주세요.", secondary = true)
                        DiaryButton("사진 보관함에서 선택", { pick(false) }, Modifier.fillMaxWidth().testTag("pick-photo"))
                        DiaryButton("지금 촬영하기", { pick(true) }, Modifier.fillMaxWidth().testTag("take-photo"))
                        if (current != null) DiaryText("이 날짜에 기록이 있어요. 저장할 때 교체 여부를 확인합니다.", secondary = true)
                    }
                    "detail" -> if (current != null) {
                        DiaryText(selected)
                        var drag by remember { mutableFloatStateOf(0f) }
                        fun navigate(delta: Int) {
                            val index = records.indexOfFirst { it.date == selected } + delta
                            if (memo == null && index in records.indices) selected = records[index].date
                        }
                        if (memo == null) LocalPhoto(store.path(current.image), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).pointerInput(selected, memo) {
                            detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                                if (drag < -70f) navigate(1) else if (drag > 70f) navigate(-1)
                            }) { change, amount -> change.consume(); drag += amount }
                        }, fit = true)
                        if (memo == null) {
                            if (current.memo.isNotEmpty()) DiaryText(current.memo)
                            DiaryButton(if (current.memo.isEmpty()) "한 줄 남기기" else "한 줄 수정", { memo = current.memo }, Modifier.testTag("edit-memo"))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (records.indexOf(current) > 0) DiaryButton("이전 기록", { navigate(-1) })
                                if (records.indexOf(current) < records.lastIndex) DiaryButton("다음 기록", { navigate(1) })
                            }
                            DiaryButton("사진 변경", { sourceReturn = "detail"; sourceReturnDate = selected; route = "source" }, Modifier.testTag("change-photo"))
                            DiaryButton("기록 삭제", { confirmation = "delete" }, Modifier.testTag("delete-record"))
                        } else {
                            MemoInput(memo.orEmpty(), { memo = it })
                            DiaryButton("한 줄 저장", { scope.launch {
                                busy = true
                                try { records = withContext(Dispatchers.Default) { store.memo(records, selected, memo.orEmpty()) }; memo = null }
                                catch (_: Exception) { error = "메모를 저장하지 못했어요. 다시 시도해 주세요." }
                                finally { busy = false }
                            } }, Modifier.testTag("save-memo"))
                            DiaryButton("수정 취소", { memo = null })
                        }
                    } else { DiaryText("기록이 없어요."); DiaryButton("홈으로", { route = "home" }) }
                }
            }
        }
        confirmation?.let { kind ->
            DiaryDialog(when(kind) { "delete" -> "이 날의 기록을 삭제할까요? 사진과 메모가 기기에서 삭제됩니다."; "replace" -> "이 날짜의 사진을 바꿀까요? 한 줄 메모는 유지됩니다."; else -> "저장하지 않은 변경을 닫을까요?" }, { confirmation = null }) {
                DiaryButton("취소", { confirmation = null })
                DiaryButton("확인", {
                    confirmation = null
                    when(kind) {
                        "replace" -> save()
                        "discard" -> { discardSource(); if (sourceReturn == "detail") selected = sourceReturnDate; route = sourceReturn }
                        "memo-discard" -> { memo = null; route = "home" }
                        "delete" -> scope.launch {
                            busy = true
                            try { records = withContext(Dispatchers.Default) { store.delete(records, selected) }; route = "home" }
                            catch (_: Exception) { error = "삭제하지 못했어요. 다시 시도해 주세요." }
                            finally { busy = false }
                        }
                    }
                }, Modifier.testTag("confirm-action"))
            }
        }
        error?.let { DiaryDialog(it, { error = null }) { DiaryButton("확인", { error = null }) } }
        if (busy) DiaryDialog("잠시만 기다려 주세요…", {}) {}
    }
}


@Composable
private fun DiaryDialog(message: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().background(LocalDiaryColors.current.background, RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) { DiaryText(message); content() }
    }
}
