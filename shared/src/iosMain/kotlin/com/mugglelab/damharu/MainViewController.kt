package com.mugglelab.damharu

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Swift hosts this controller; screen content and state stay in commonMain. */
fun MainViewController(showDemoControls: Boolean, scenario: String, dateOverride: String) = ComposeUIViewController {
    val today = if (dateOverride.isNotBlank()) LocalDate.parse(dateOverride)
        else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    App(today, showDemoControls, scenario)
}
