package com.mugglelab.damharu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mugglelab.damharu.home.*

/** Shared appearance and home presentation preferences; each host owns persistence. */
class AppearanceState(initialLightMode: Boolean) {
    var lightMode by mutableStateOf(initialLightMode)
        private set
    var onChange: ((Boolean) -> Unit)? = null
    fun updateLightMode(enabled: Boolean) {
        lightMode = enabled
        onChange?.invoke(enabled)
    }
}

@Composable
fun SettingsScreen(appearance: AppearanceState, modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    Column(modifier.fillMaxSize().background(colors.background).safeDrawingPadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).testTag("settings-screen")) {
        Spacer(Modifier.height(28.dp))
        BasicText("담 하 루", style = DiaryTextStyle.copy(color = colors.muted, fontSize = 10.sp, letterSpacing = 5.sp))
        Spacer(Modifier.height(16.dp))
        BasicText("설정", style = DiaryTextStyle.copy(color = colors.ink, fontSize = 28.sp, lineHeight = 38.sp))
        Spacer(Modifier.height(8.dp))
        DiaryText("기록을 바라보는 나만의 방식.", secondary = true)
        Spacer(Modifier.height(40.dp))
        DiaryMono("APPEARANCE")
        Spacer(Modifier.height(14.dp))
        Hairline(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().heightIn(min = 88.dp)
            .toggleable(value = appearance.lightMode, role = Role.Switch, onValueChange = appearance::updateLightMode)
            .semantics { stateDescription = if (appearance.lightMode) "켜짐" else "꺼짐" }
            .testTag("light-mode-toggle").padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                DiaryText("라이트 모드")
                Spacer(Modifier.height(4.dp))
                BasicText(if (appearance.lightMode) "따뜻한 종이빛으로 보기" else "고요한 어둠 속에서 보기",
                    style = DiaryTextStyle.copy(color = colors.muted, fontSize = 12.sp))
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(50.dp, 30.dp).background(if (appearance.lightMode) colors.accent else colors.panel, RoundedCornerShape(50))
                .border(1.dp, if (appearance.lightMode) colors.accent else colors.faint, RoundedCornerShape(50)).padding(4.dp)) {
                Box(Modifier.size(22.dp).align(if (appearance.lightMode) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(if (appearance.lightMode) colors.background else colors.soft, CircleShape))
            }
        }
        Hairline(Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
