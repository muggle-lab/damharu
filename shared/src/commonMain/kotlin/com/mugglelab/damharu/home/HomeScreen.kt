package com.mugglelab.damharu.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.DiaryTextStyle
import com.mugglelab.damharu.LocalDiaryColors
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate

@Composable
fun DiaryText(text: String, modifier: Modifier = Modifier, secondary: Boolean = false) {
    val colors = LocalDiaryColors.current
    BasicText(text, modifier, style = DiaryTextStyle.copy(color = if (secondary) colors.muted else colors.ink))
}

@Composable
fun DiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    Box(
        modifier.clip(RoundedCornerShape(17.dp)).background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = DiaryTextStyle.copy(color = colors.onAccent, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
    }
}

@Composable
fun HomeScreen(
    today: LocalDate,
    month: CalendarMonth,
    entries: Map<LocalDate, DiaryEntry>,
    onMonthChange: (CalendarMonth) -> Unit,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    demoControls: @Composable () -> Unit = {},
) {
    val colors = LocalDiaryColors.current
    val cells = remember(month) { month.cells() }
    Column(modifier.background(colors.background).safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(28.dp))
            BasicText("담하루", style = DiaryTextStyle.copy(color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(27.dp))
            BasicText("나의 하루들", style = DiaryTextStyle.copy(color = colors.muted, fontSize = 12.sp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(month.label, Modifier.weight(1f).testTag("month-title"), style = DiaryTextStyle.copy(color = colors.ink, fontSize = 25.sp, lineHeight = 33.sp, fontWeight = FontWeight.Medium))
                MonthArrow(previous = true, enabled = month.canGoBack) { onMonthChange(month.shiftedBy(-1)) }
                MonthArrow(previous = false, enabled = month.canGoForward) { onMonthChange(month.shiftedBy(1)) }
            }
            // Reserve the same height so switching months never shifts the calendar.
            Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
                if (month != CalendarMonth.from(today)) {
                    Box(Modifier.fillMaxHeight().clickable(role = Role.Button) { onMonthChange(CalendarMonth.from(today)) }, contentAlignment = Alignment.Center) {
                        BasicText("이번 달로", style = DiaryTextStyle.copy(color = colors.accent, fontSize = 13.sp))
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sidePadding = if (maxWidth < 360.dp) 6.dp else 20.dp
            Column(Modifier.padding(horizontal = sidePadding)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                        BasicText(it, Modifier.weight(1f), style = DiaryTextStyle.copy(color = colors.muted, fontSize = 12.sp, textAlign = TextAlign.Center))
                    }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).height(60.dp))
                            else DayCell(date, today, entries[date], Modifier.weight(1f)) {
                                actionForDate(date, today, entries)?.let(onAction)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(38.dp))
            BasicText(
                if (today in entries) "오늘을 남겼어요." else "오늘, 기억하고 싶은 장면 하나.",
                style = DiaryTextStyle.copy(color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(18.dp))
            DiaryButton(if (today in entries) "오늘의 사진 보기" else "오늘 한 장 남기기", {
                actionForDate(today, today, entries)?.let(onAction)
            }, Modifier.fillMaxWidth().testTag("today-action"))
            Spacer(Modifier.height(20.dp))
            demoControls()
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, entry: DiaryEntry?, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(7.dp)
    val description = "${date.month.number}월 ${date.day}일" +
        (if (date == today) ", 오늘" else "") +
        (if (entry != null) ", 기록 보기" else if (date > today) ", 미래 날짜" else ", 기록 남기기")
    Box(
        modifier.height(60.dp).clip(shape).testTag("day-$date")
            .then(if (date == today) Modifier.border(1.dp, colors.accent, shape) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(enabled = date <= today, role = Role.Button, onClick = onClick),
    ) {
        if (entry != null) {
            if (entry.imagePath != null) com.mugglelab.damharu.LocalPhoto(entry.imagePath, Modifier.fillMaxSize())
            else SampleThumbnail(entry.scene, Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().height(29.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent))))
        }
        BasicText(
            date.day.toString(), Modifier.fillMaxWidth().padding(top = 5.dp),
            style = DiaryTextStyle.copy(
                color = when {
                    entry != null -> Color.White
                    date == today -> colors.accent
                    date > today -> colors.muted.copy(alpha = .55f)
                    else -> colors.muted
                },
                fontSize = 12.sp, textAlign = TextAlign.Center,
                fontWeight = if (date == today) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun MonthArrow(previous: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = LocalDiaryColors.current.ink.copy(alpha = if (enabled) 1f else .3f)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).testTag(if (previous) "previous-month" else "next-month")
            .semantics { contentDescription = if (previous) "이전 달" else "다음 달" }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val a = if (previous) .65f else .35f
            val b = 1 - a
            drawLine(color, Offset(size.width * a, size.height * .2f), Offset(size.width * b, size.height * .5f), 1.5.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(size.width * b, size.height * .5f), Offset(size.width * a, size.height * .8f), 1.5.dp.toPx(), StrokeCap.Round)
        }
    }
}
