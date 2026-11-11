package com.mugglelab.damharu.camera

import kotlin.test.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class CameraTimestampTest {
    @Test fun formatsLocalTimeWithSecondsAndMidnight() {
        val zone = TimeZone.of("Asia/Seoul")
        assertEquals("15:34:09", cameraTime(Instant.parse("2026-10-26T06:34:09Z"), zone))
        assertEquals("00:00:00", cameraTime(Instant.parse("2026-10-26T15:00:00Z"), zone))
    }
}
