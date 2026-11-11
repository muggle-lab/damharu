package com.mugglelab.damharu.camera

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant

fun cameraTime(now: Instant, zone: TimeZone): String {
    val local = now.toLocalDateTime(zone)
    return listOf(local.hour, local.minute, local.second).joinToString(":") { it.toString().padStart(2, '0') }
}
fun currentCameraTime(): String = cameraTime(Clock.System.now(), TimeZone.currentSystemDefault())

@Composable
fun CameraTimestamp(frozen: String?, modifier: Modifier = Modifier) {
    val live by produceState(currentCameraTime(), frozen) {
        while (frozen == null) {
            val now = Clock.System.now()
            value = cameraTime(now, TimeZone.currentSystemDefault())
            delay(1000L - now.toEpochMilliseconds().mod(1000))
        }
    }
    BasicText(frozen ?: live, modifier.testTag("camera-timestamp"), style = TextStyle(
        color = Color(0xFFF08A3C), fontFamily = FontFamily.Monospace, fontSize = 24.sp,
        letterSpacing = 1.sp, shadow = Shadow(Color.Black, Offset(0f, 1f), 3f)))
}
