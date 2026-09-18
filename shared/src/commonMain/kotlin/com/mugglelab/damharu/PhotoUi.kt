package com.mugglelab.damharu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun LocalPhoto(path: String, modifier: Modifier = Modifier, filter: PhotoFilter = PhotoFilter.Original, intensity: Float = 1f, fit: Boolean = false, sharedBitmap: ImageBitmap? = null) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path, sharedBitmap) {
        if (sharedBitmap != null) { bitmap = sharedBitmap; return@LaunchedEffect }
        val result = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(path) } }
        bitmap = result.getOrNull(); failed = result.isFailure
    }
    Box(modifier.background(LocalDiaryColors.current.line)) {
        bitmap?.let { Image(it, "기록 사진", Modifier.fillMaxSize(), contentScale = if (fit) ContentScale.Fit else ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix(filter.matrix(intensity)))) }
        if (failed) DiaryText("사진을 읽을 수 없어요", Modifier.padding(12.dp), secondary = true)
    }
}

@Composable
fun FilterEditor(path: String, date: String, filter: PhotoFilter, intensity: Float, onFilter: (PhotoFilter) -> Unit, onIntensity: (Float) -> Unit) {
    var compare by remember { mutableStateOf(false) }
    var preview by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) { preview = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(path) }.getOrNull() } }
    DiaryText(date, secondary = true)
    DiaryText("오늘을 어떤 빛으로 남길까요?")
    if (preview != null) LocalPhoto(path, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)), if (compare) PhotoFilter.Original else filter, intensity, fit = true, sharedBitmap = preview)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PhotoFilter.entries.forEach { item ->
            Column(Modifier.width(64.dp).clickable { onFilter(item) }.testTag("filter-${item.name}")) {
                if (preview != null) LocalPhoto(path, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), item, sharedBitmap = preview)
                DiaryText((if (item == filter) "• " else "") + item.label)
            }
        }
    }
    if (filter != PhotoFilter.Original) {
        DiaryText("강도 ${(intensity * 100).roundToInt()}%", secondary = true)
        BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp).testTag("filter-intensity")
            .semantics {
                contentDescription = "필터 강도"
                progressBarRangeInfo = ProgressBarRangeInfo(intensity, 0f..1f)
                setProgress { onIntensity(it.coerceIn(0f, 1f)); true }
            }) {
            val width = constraints.maxWidth.toFloat()
            Box(Modifier.fillMaxSize().pointerInput(width) {
                detectTapGestures { onIntensity((it.x / width).coerceIn(0f, 1f)) }
            }.pointerInput(width) {
                detectHorizontalDragGestures { change, _ -> change.consume(); onIntensity((change.position.x / width).coerceIn(0f, 1f)) }
            }.padding(vertical = 18.dp).background(LocalDiaryColors.current.line, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(intensity).fillMaxHeight().background(LocalDiaryColors.current.accent, RoundedCornerShape(4.dp)))
            }
        }
        DiaryButton(if (compare) "필터로 돌아가기" else "원본과 비교", { compare = !compare }, Modifier.fillMaxWidth())
    }
}

@Composable
fun MemoInput(value: String, onChange: (String) -> Unit) {
    BasicTextField(value, { onChange(it.replace("\n", " ").take(120)) },
        Modifier.fillMaxWidth().heightIn(min = 54.dp).background(LocalDiaryColors.current.line, RoundedCornerShape(12.dp)).padding(14.dp)
            .testTag("memo-input"), singleLine = true, textStyle = DiaryTextStyle.copy(color = LocalDiaryColors.current.ink))
    DiaryText("${value.length}/120 · 쓰고 싶은 날에만", secondary = true)
}
