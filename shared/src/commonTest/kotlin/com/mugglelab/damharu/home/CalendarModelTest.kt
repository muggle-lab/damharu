package com.mugglelab.damharu.home

import kotlinx.datetime.number
import kotlinx.datetime.LocalDate
import kotlin.test.*

class CalendarModelTest {
    @Test fun monthLengthsAndLeapCentury() {
        listOf(2025 to 28, 2024 to 29, 1900 to 28, 2000 to 29).forEach { (year, count) ->
            assertEquals(count, CalendarMonth(year, 2).cells().filterNotNull().size)
        }
        assertEquals(30, CalendarMonth(2026, 4).cells().filterNotNull().size)
        assertEquals(31, CalendarMonth(2026, 1).cells().filterNotNull().size)
    }

    @Test fun sundayFirstAndSixWeekMonth() {
        assertEquals(LocalDate(2026, 2, 1), CalendarMonth(2026, 2).cells().first())
        val september = CalendarMonth(2026, 9).cells()
        assertEquals(listOf(null, null, LocalDate(2026, 9, 1)), september.take(3))
        assertEquals(42, CalendarMonth(2026, 8).cells().size)
        assertEquals(LocalDate(2026, 8, 1), CalendarMonth(2026, 8).cells()[6])
    }

    @Test fun yearBoundaryNavigation() {
        assertEquals(CalendarMonth(2027, 1), CalendarMonth(2026, 12).shiftedBy(1))
        assertEquals(CalendarMonth(2025, 12), CalendarMonth(2026, 1).shiftedBy(-1))
        assertFalse(CalendarMonth(1, 1).canGoBack)
        assertFalse(CalendarMonth(9999, 12).canGoForward)
    }

    @Test fun allMonthsProduceOrderedDatesWithOnlyEdgePadding() {
        for (year in 1999..2030) for (month in 1..12) {
            val cells = CalendarMonth(year, month).cells()
            val dates = cells.filterNotNull()
            assertEquals(0, cells.size % 7)
            assertEquals((1..dates.size).toList(), dates.map { it.day })
            assertTrue(dates.all { it.year == year && it.month.number == month })
            dates.forEach { assertEquals((it.dayOfWeek.ordinal + 1) % 7, cells.indexOf(it) % 7) }
        }
    }

    @Test fun onlyTodayCanBeRecorded() {
        val today = LocalDate(2026, 9, 17)
        val entries = sampleEntries(today)
        assertEquals(HomeAction.AddRecord(today), actionForDate(today, today, entries))
        val recorded = LocalDate(2026, 9, 16)
        assertEquals(HomeAction.OpenRecord(recorded), actionForDate(recorded, today, entries))
        val missed = LocalDate(2026, 9, 14)
        assertFalse(missed in entries)
        assertNull(actionForDate(missed, today, entries))
        assertNull(actionForDate(LocalDate(2026, 9, 18), today, entries))
        assertTrue(entries.keys.all { it < today })
    }

    @Test fun todayWithRecordOpensItInsteadOfCamera() {
        val today = LocalDate(2026, 9, 17)
        val entries = mapOf(today to DiaryEntry(today))
        assertEquals(HomeAction.OpenRecord(today), actionForDate(today, today, entries))
        assertTrue(canRecord(today, today))
        assertFalse(canRecord(LocalDate(2026, 9, 16), today))
        assertFalse(canRecord(LocalDate(2026, 9, 18), today))
    }

    @Test fun dateLabels() {
        val sunday = LocalDate(2026, 9, 20)
        assertEquals("9월 20일, 일요일", sunday.koreanLabel)
        assertEquals("2026.09.20 SUN", sunday.monoLabel)
        assertEquals("09.20", sunday.shortMonoLabel)
        assertEquals("1월 5일, 월요일", LocalDate(2026, 1, 5).koreanLabel)
    }

    @Test fun monoLabelPadsMonth() {
        assertEquals("2026.09", CalendarMonth(2026, 9).monoLabel)
        assertEquals("2026.12", CalendarMonth(2026, 12).monoLabel)
    }
}
