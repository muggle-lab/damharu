package com.mugglelab.damharu.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.DiaryMonoStyle
import com.mugglelab.damharu.DiaryTextStyle
import com.mugglelab.damharu.LocalDiaryColors
import com.mugglelab.damharu.LocalPhoto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@Composable
fun DiaryText(text: String, modifier: Modifier = Modifier, secondary: Boolean = false) {
    val colors = LocalDiaryColors.current
    BasicText(text, modifier, style = DiaryTextStyle.copy(color = if (secondary) colors.muted else colors.ink))
}

/** Wide-tracked monospace label for dates and other metadata, like print on a film edge. */
@Composable
fun DiaryMono(text: String, modifier: Modifier = Modifier, color: Color = LocalDiaryColors.current.muted) {
    BasicText(text, modifier, style = DiaryMonoStyle.copy(color = color))
}

/** Pill button. Outline by default; [primary] fills it for the one main action of a screen. */
@Composable
fun DiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier.heightIn(min = 44.dp).clip(shape)
            .then(if (primary) Modifier.background(colors.ink) else Modifier.border(1.dp, colors.faint, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = DiaryTextStyle.copy(color = if (primary) colors.onAccent else colors.soft, fontSize = 13.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center))
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(LocalDiaryColors.current.line))
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
    Box(modifier.background(colors.background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(28.dp))
                BasicText("담 하 루", style = DiaryTextStyle.copy(color = colors.muted, fontSize = 10.sp, letterSpacing = 5.sp))
                Spacer(Modifier.height(16.dp))
                BasicText("${month.month}월, 담아둔 하루", style = DiaryTextStyle.copy(color = colors.ink, fontSize = 28.sp, lineHeight = 38.sp))
                Spacer(Modifier.height(8.dp))
                BasicText("지나간 날들이 여기 모여 있어요.", style = DiaryTextStyle.copy(color = colors.muted, fontSize = 12.sp))
            }
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 22.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(previous = true, enabled = month.canGoBack) { onMonthChange(month.shiftedBy(-1)) }
                // VoiceOver/TalkBack read the full month name; the visible label stays film-edge style.
                DiaryMono(month.monoLabel, Modifier.testTag("month-title").semantics { contentDescription = month.label }, color = colors.ink)
                MonthArrow(previous = false, enabled = month.canGoForward) { onMonthChange(month.shiftedBy(1)) }
                Hairline(Modifier.weight(1f))
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sidePadding = if (maxWidth < 360.dp) 6.dp else 12.dp
                Column(Modifier.padding(horizontal = sidePadding)) {
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            week.forEach { date ->
                                if (date == null) Spacer(Modifier.weight(1f).heightIn(min = 48.dp).aspectRatio(1f))
                                else {
                                    val action = actionForDate(date, today, entries)
                                    DayCell(date, today, entries[date], action != null, Modifier.weight(1f)) { action?.let(onAction) }
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
            Row(Modifier.align(Alignment.End).padding(end = 12.dp).heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClickLabel = "이번 달로") { onMonthChange(CalendarMonth.from(today)) }
                .padding(horizontal = 10.dp).testTag("today-legend"),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(5.dp).background(colors.accent, CircleShape))
                DiaryMono("TODAY")
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) { demoControls() }
            // The section under the calendar is intentionally empty (content undecided); this also clears the capture button.
            Spacer(Modifier.height(140.dp))
        }
        CaptureButton(Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 26.dp)) { onAction(HomeAction.AddRecord(today)) }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, entry: DiaryEntry?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val shape = RoundedCornerShape(2.dp)
    val missed = entry == null && date < today
    val description = "${date.month.number}월 ${date.day}일" +
        (if (date == today) ", 오늘" else "") +
        (if (entry != null) ", 기록 보기" else if (date > today) ", 미래 날짜" else if (missed) ", 지나간 날" else ", 기록 남기기")
    Box(
        modifier.heightIn(min = 48.dp).aspectRatio(1f).clip(shape).testTag("day-$date")
            .then(if (missed) Modifier.background(Color(0xFF0A0908)) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        if (entry != null) {
            if (entry.imagePath != null) LocalPhoto(entry.imagePath, Modifier.fillMaxSize())
            else SampleThumbnail(entry.scene, Modifier.fillMaxSize())
            // Darken only the bottom of the thumbnail presentation so the day number stays legible.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .55f)))))
        }
        if (date == today) {
            if (entry == null) Box(Modifier.matchParentSize().drawBehind {
                drawRoundRect(colors.faint, cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
            })
            Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(4.dp).background(colors.accent, CircleShape))
        }
        BasicText(
            date.day.toString(), Modifier.align(Alignment.BottomStart).padding(start = 5.dp, bottom = 3.dp),
            style = DiaryMonoStyle.copy(
                color = when {
                    entry != null -> colors.ink.copy(alpha = .92f)
                    date == today -> colors.ink
                    date > today -> Color(0xFF2A2824)
                    else -> Color(0xFF35322C)
                },
                fontSize = 9.sp, letterSpacing = 0.sp,
            ),
        )
    }
}

/** Metal shutter-like button: the single way to add today's photo. */
@Composable
private fun CaptureButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(60.dp).clip(CircleShape).testTag("capture-today")
            .semantics { contentDescription = "오늘 한 장 촬영" }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(Color(0xFF6A655B), radius)
            drawCircle(Color.Black, radius - 1.dp.toPx())
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFCFCABF), Color(0xFF7D786D)),
                center = Offset(size.width * .35f, size.height * .28f), radius = radius * 1.5f), radius - 3.dp.toPx())
            val ink = Color(0xFF1B1A17); val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
            val w = 22.dp.toPx(); val h = 15.dp.toPx()
            val left = (size.width - w) / 2; val top = (size.height - h) / 2 + 1.5.dp.toPx()
            drawRoundRect(ink, Offset(left, top), Size(w, h), CornerRadius(2.dp.toPx()), style = stroke)
            drawLine(ink, Offset(left + w * .32f, top), Offset(left + w * .4f, top - 3.dp.toPx()), stroke.width, StrokeCap.Round)
            drawLine(ink, Offset(left + w * .4f, top - 3.dp.toPx()), Offset(left + w * .6f, top - 3.dp.toPx()), stroke.width, StrokeCap.Round)
            drawLine(ink, Offset(left + w * .6f, top - 3.dp.toPx()), Offset(left + w * .68f, top), stroke.width, StrokeCap.Round)
            drawCircle(ink, 4.dp.toPx(), Offset(size.width / 2, top + h / 2), style = stroke)
        }
    }
}

@Composable
private fun MonthArrow(previous: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = LocalDiaryColors.current.muted.copy(alpha = if (enabled) 1f else .3f)
    Box(
        Modifier.size(44.dp).clip(CircleShape).testTag(if (previous) "previous-month" else "next-month")
            .semantics { contentDescription = if (previous) "이전 달" else "다음 달" }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val a = if (previous) .65f else .35f
            val b = 1 - a
            drawLine(color, Offset(size.width * a, size.height * .2f), Offset(size.width * b, size.height * .5f), 1.2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(size.width * b, size.height * .5f), Offset(size.width * a, size.height * .8f), 1.2.dp.toPx(), StrokeCap.Round)
        }
    }
}
