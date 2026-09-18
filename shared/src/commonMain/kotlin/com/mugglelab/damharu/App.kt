package com.mugglelab.damharu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.datetime.*

@Composable
fun App(today: LocalDate, photoSource: PhotoSource) {
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.month.number) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    var route by rememberSaveable { mutableStateOf("home") }
    var source by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    var requestId by remember { mutableIntStateOf(0) }
    fun discardSource() { source?.let { runCatching { PhotoFiles.delete(it) } }; source = null }
    DisposableEffect(Unit) { onDispose { requestId++; discardSource() } }
    fun pick(camera: Boolean) {
        if (picking) return
        picking = true
        val id = ++requestId
        photoSource.request(camera) { path, failure ->
            if (id != requestId) path?.let { runCatching { PhotoFiles.delete(it) } }
            else {
                picking = false
                if (failure != null) error = failure
                else if (path != null) { discardSource(); source = path; route = "editor" }
            }
        }
    }
    DiaryTheme {
        if (route == "home") {
            HomeScreen(today, CalendarMonth(year, month), emptyMap(), { year = it.year; month = it.month }, { action ->
                if (action is HomeAction.AddRecord) { selected = action.date.toString(); route = "source" }
            }, Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize().background(LocalDiaryColors.current.background).safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DiaryButton("닫기", {
                    if (route == "editor" && source != null) discard = true
                    else { requestId++; picking = false; route = "home" }
                }, Modifier.testTag("close-screen"))
                DiaryText(selected, secondary = true)
                if (route == "source") {
                    DiaryText(if (picking) "사진을 준비하고 있어요…" else "한 장으로 남기는 하루")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DiaryButton("이전 날짜", { selected = LocalDate.parse(selected).minus(1, DateTimeUnit.DAY).toString() })
                        if (LocalDate.parse(selected) < today) DiaryButton("다음 날짜", { selected = LocalDate.parse(selected).plus(1, DateTimeUnit.DAY).toString() })
                    }
                    DiaryButton("사진 보관함에서 선택", { pick(false) }, Modifier.fillMaxWidth().testTag("pick-photo"))
                    DiaryButton("지금 촬영하기", { pick(true) }, Modifier.fillMaxWidth().testTag("take-photo"))
                } else {
                    source?.let { path -> LocalPhoto(path, Modifier.fillMaxWidth().aspectRatio(1f)) }
                    DiaryText("사진을 선택했어요. 아직 저장되지 않았습니다.", secondary = true)
                    DiaryButton("다른 사진 선택", { discardSource(); route = "source" })
                }
            }
        }
        if (discard) DiaryDialog("선택한 사진을 닫을까요? 아직 저장되지 않았습니다.", { discard = false }) {
            DiaryButton("취소", { discard = false })
            DiaryButton("확인", { discard = false; discardSource(); route = "home" })
        }
        error?.let { DiaryDialog(it, { error = null }) { DiaryButton("확인", { error = null }) } }
    }
}

@Composable
private fun DiaryDialog(message: String, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().background(LocalDiaryColors.current.background, RoundedCornerShape(20.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) { DiaryText(message); content() }
    }
}
