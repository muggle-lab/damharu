package com.mugglelab.damharu.home

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DayCountdownTest {
    @Test fun localMidnightAndRollover() {
        val zone = TimeZone.of("Asia/Seoul")
        assertEquals("23:59:00", dayCountdown(Instant.parse("2026-09-20T15:01:00Z"), zone))
        assertEquals("00:00:01", dayCountdown(Instant.parse("2026-09-21T14:59:59.900Z"), zone))
        assertEquals("24:00:00", dayCountdown(Instant.parse("2026-09-21T15:00:00Z"), zone))
        assertEquals("01:51:20", dayCountdown(Instant.parse("2026-09-21T13:08:40Z"), zone))
    }
    @Test fun daylightSavingUsesActualNextMidnight() {
        val zone = TimeZone.of("America/New_York")
        assertEquals("23:00:00", dayCountdown(Instant.parse("2026-03-08T05:00:00Z"), zone))
        assertEquals("25:00:00", dayCountdown(Instant.parse("2026-11-01T04:00:00Z"), zone))
    }
}
