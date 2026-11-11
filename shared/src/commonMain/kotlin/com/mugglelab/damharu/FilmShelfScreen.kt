package com.mugglelab.damharu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mugglelab.damharu.data.Film
import com.mugglelab.damharu.home.bottomSheetProperties
import damharu.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

enum class DiaryTab(val id: String, val label: String) {
    Home("home", "기록"), Films("films", "필름"), Settings("settings", "설정")
}

@Composable
fun FilmShelfScreen(modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    var selected by remember { mutableStateOf<Film?>(null) }
    Column(modifier.fillMaxSize().background(colors.background).safeDrawingPadding()
        .testTag("film-shelf-screen").verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp)) {
        BasicText("담 하 루 / FILM COLLECTION", style = DiaryMonoStyle.copy(color = colors.muted))
        Spacer(Modifier.height(18.dp))
        BasicText("필름 진열장", style = DiaryTextStyle.copy(color = colors.ink, fontSize = 28.sp, lineHeight = 38.sp))
        Spacer(Modifier.height(8.dp))
        BasicText("오늘을 담을 색과 질감을 만나보세요.", style = DiaryTextStyle.copy(color = colors.soft, fontSize = 13.sp))
        Spacer(Modifier.height(30.dp))
        Film.available.filter { it != Film.None }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                pair.forEach { film ->
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .background(colors.panel).border(1.dp, colors.line, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = "${film.label} 예시 보기") { selected = film }
                        .testTag("film-card-${film.id}").padding(14.dp)) {
                        BasicText(if (film == Film.Monochrome) "MONO" else film.monoName, style = DiaryMonoStyle.copy(color = colors.muted))
                        Image(painterResource(film.cartridge),
                            null, Modifier.fillMaxWidth().height(152.dp).padding(vertical = 10.dp), contentScale = ContentScale.Fit)
                        BasicText(film.label, style = DiaryTextStyle.copy(color = colors.ink, fontSize = 20.sp))
                        Spacer(Modifier.height(8.dp))
                        BasicText(film.shelfCaption, style = DiaryTextStyle.copy(color = colors.soft, fontSize = 12.sp, lineHeight = 20.sp))
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.height(22.dp))
                        BasicText("예시 보기 ↗", style = DiaryTextStyle.copy(color = colors.accent, fontSize = 12.sp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    selected?.let { film -> FilmPreviewSheet(film) { selected = null } }
}

@Composable
private fun FilmPreviewSheet(film: Film, onDismiss: () -> Unit) {
    val colors = LocalDiaryColors.current
    var original by remember(film) { mutableStateOf(false) }
    val preview = if (original) Film.None else film
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(1f, tween(200))
            onDismiss()
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(0f, tween(280)) }
    Dialog(onDismissRequest = { close() }, properties = bottomSheetProperties()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - progress.value }
                .background(Color.Black.copy(alpha = .55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    role = Role.Button) { close() }.semantics { contentDescription = "예시 닫기" }.testTag("film-preview-scrim"))
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = maxHeight * .9f)
                .graphicsLayer { translationY = size.height * progress.value + dragOffset }
                .background(colors.panel, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .pointerInput(Unit) { detectTapGestures {} }.testTag("film-preview-sheet")
                .semantics { paneTitle = "${film.label} 예시" }.navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 22.dp)) {
                Box(Modifier.fillMaxWidth().height(36.dp).testTag("film-preview-handle")
                    .pointerInput(Unit) {
                        fun settle() {
                            if (dragOffset > 64.dp.toPx()) close()
                            else scope.launch { animate(dragOffset, 0f, animationSpec = tween(160)) { value, _ -> dragOffset = value } }
                        }
                        detectVerticalDragGestures(
                            onDragEnd = { settle() },
                            onDragCancel = { settle() },
                        ) { change, amount ->
                            if (!closing && !progress.isRunning) {
                                change.consume()
                                dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(32.dp, 4.dp).background(colors.faint, RoundedCornerShape(50)))
                }
                BasicText(film.label, style = DiaryTextStyle.copy(color = colors.ink, fontSize = 24.sp))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp))) {
                    Image(painterResource(Res.drawable.film_sample_coffee), if (original) "커피 사진 원본" else "${film.label} 적용 커피 사진",
                        Modifier.fillMaxSize().testTag("film-preview-image"), contentScale = ContentScale.Crop,
                        colorFilter = preview.matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) })
                    Box(Modifier.fillMaxSize().filmTexture(preview.texture))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true, false).forEach { isOriginal ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (original == isOriginal) colors.accent else colors.background)
                            .selectable(original == isOriginal, role = Role.RadioButton) { original = isOriginal }
                            .testTag(if (isOriginal) "film-preview-original" else "film-preview-effect").padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            BasicText(if (isOriginal) "원본" else film.label, style = DiaryTextStyle.copy(color = if (original == isOriginal) colors.onAccent else colors.soft))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                BasicText("SAMPLE · 창가의 오후", style = DiaryMonoStyle.copy(color = colors.muted))
                Spacer(Modifier.height(10.dp))
                BasicText(film.previewDescription, style = DiaryTextStyle.copy(color = colors.soft, fontSize = 14.sp))
            }
        }
    }
}
