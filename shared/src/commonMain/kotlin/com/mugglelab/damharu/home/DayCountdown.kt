package com.mugglelab.damharu.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.DiaryMonoStyle
import com.mugglelab.damharu.LocalDiaryColors
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant

/** Recalculate from wall time rather than decrementing: suspension and clock changes cannot accumulate drift. */
internal fun dayCountdown(now: Instant, zone: TimeZone): String {
    val tomorrow = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
    val remaining = tomorrow.atStartOfDayIn(zone) - now
    val seconds = ((remaining.inWholeMilliseconds + 999) / 1000).coerceAtLeast(0)
    return listOf(seconds / 3600, seconds / 60 % 60, seconds % 60)
        .joinToString(":") { it.toString().padStart(2, '0') }
}

@Composable
internal fun DayCountdown() {
    val text by produceState(dayCountdown(Clock.System.now(), TimeZone.currentSystemDefault())) {
        while (true) {
            val now = Clock.System.now()
            value = dayCountdown(now, TimeZone.currentSystemDefault())
            delay(1000L - now.toEpochMilliseconds().mod(1000))
        }
    }
    BasicText(text, Modifier.testTag("today-countdown")
        .semantics { contentDescription = "오늘 남은 시간 $text" },
        style = DiaryMonoStyle.copy(color = LocalDiaryColors.current.muted, fontSize = 12.sp, letterSpacing = 1.sp))
}
