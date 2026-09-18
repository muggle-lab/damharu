package com.mugglelab.damharu.home

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Dates have no UI dependency, so month arithmetic can be tested independently. */
data class CalendarMonth(val year: Int, val month: Int) {
    init {
        require(year in 1..9999)
        require(month in 1..12)
    }

    val firstDay: LocalDate get() = LocalDate(year, month, 1)
    val canGoBack: Boolean get() = year > 1 || month > 1
    val canGoForward: Boolean get() = year < 9999 || month < 12
    val label: String get() = "${year}년 ${month}월"

    fun shiftedBy(months: Int): CalendarMonth = from(firstDay.plus(months, DateTimeUnit.MONTH))

    /** Sunday-first grid. Nulls are padding, never interactive dates. */
    fun cells(): List<LocalDate?> {
        val leading = firstDay.dayOfWeek.ordinal.let { (it + 1) % 7 }
        val dayCount = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
        val count = ((leading + dayCount + 6) / 7) * 7
        return List(count) { index ->
            val day = index - leading + 1
            if (day in 1..dayCount) LocalDate(year, month, day) else null
        }
    }

    companion object {
        fun from(date: LocalDate) = CalendarMonth(date.year, date.month.number)
    }
}

enum class SampleScene { Coffee, Walk, Book, Sunset }

/** Temporary in-memory fixture; never represents a persisted user photo. */
data class DiaryEntry(val date: LocalDate, val scene: SampleScene)

fun sampleEntries(today: LocalDate): Map<LocalDate, DiaryEntry> =
    listOf(1, 2, 4, 5, 8, 9, 11, 13, 16, 20, 25, 32).mapIndexed { index, daysAgo ->
        val date = today.minus(daysAgo, DateTimeUnit.DAY)
        date to DiaryEntry(date, SampleScene.entries[index % SampleScene.entries.size])
    }.toMap()

sealed interface HomeAction {
    data class AddRecord(val date: LocalDate) : HomeAction
    data class OpenRecord(val date: LocalDate) : HomeAction
}

fun actionForDate(date: LocalDate, today: LocalDate, entries: Map<LocalDate, DiaryEntry>): HomeAction? =
    when {
        date > today -> null
        date in entries -> HomeAction.OpenRecord(date)
        else -> HomeAction.AddRecord(date)
    }
