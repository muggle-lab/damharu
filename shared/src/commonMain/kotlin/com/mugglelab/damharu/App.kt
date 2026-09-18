package com.mugglelab.damharu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.home.*
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate

@Composable
fun App(today: LocalDate, showDemoControls: Boolean = false, initialScenario: String = "empty") {
    DiaryTheme {
        var year by rememberSaveable { mutableStateOf(today.year) }
        var month by rememberSaveable { mutableStateOf(today.month.number) }
        var scenario by rememberSaveable { mutableStateOf(initialScenario) }
        var pendingAction by remember { mutableStateOf<HomeAction?>(null) }
        val entries = remember(today, scenario) {
            when (scenario) {
                "sample" -> sampleEntries(today)
                "saved" -> sampleEntries(today) + (today to DiaryEntry(today, SampleScene.Coffee))
                else -> emptyMap()
            }
        }
        HomeScreen(
            today, CalendarMonth(year, month), entries,
            onMonthChange = { year = it.year; month = it.month },
            // Later tasks replace this boundary with the picker and detail routes.
            onAction = { pendingAction = it },
            modifier = Modifier.fillMaxSize(),
            demoControls = {
                if (showDemoControls) {
                    DiaryText("개발 미리보기 · 실제 기록이 아니에요", secondary = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("empty" to "첫 사용", "sample" to "샘플 기록", "saved" to "오늘 완료").forEach { (value, label) ->
                            Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button) { scenario = value }, contentAlignment = Alignment.Center) {
                                DiaryText(if (scenario == value) "• $label" else label, secondary = true)
                            }
                        }
                    }
                }
            },
        )
        pendingAction?.let { action ->
            Dialog(onDismissRequest = { pendingAction = null }) {
                Column(
                    Modifier.fillMaxWidth().background(LocalDiaryColors.current.background, RoundedCornerShape(20.dp)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DiaryText(if (action is HomeAction.AddRecord) "사진 기록을 준비하고 있어요" else "기록 보기를 준비하고 있어요")
                    DiaryText(
                        if (action is HomeAction.AddRecord) "사진 선택·촬영은 다음 작업에서 연결됩니다. 아직 저장된 내용은 없어요."
                        else "샘플 기록입니다. 상세 화면은 후속 작업에서 연결됩니다.", secondary = true,
                    )
                    DiaryButton("확인", { pendingAction = null }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
