package com.mugglelab.damharu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mugglelab.damharu.data.*
import com.mugglelab.damharu.home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/** A small noise tile built once and repeated. Fixed seed keeps the grain stable between frames. */
private val grainTile: ImageBitmap by lazy {
    val side = 96
    ImageBitmap(side, side).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val random = Random(7)
        for (y in 0 until side) for (x in 0 until side) {
            val value = random.nextFloat()
            paint.color = (if (value > .5f) Color.White else Color.Black).copy(alpha = abs(value - .5f) * .3f)
            canvas.drawRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f), paint)
        }
    }
}

/** Film grain and vignette are display-only; the saved JPEG is never touched. */
fun Modifier.filmTexture(): Modifier = drawWithCache {
    val grain = ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    val vignette = Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .42f)),
        center = Offset(size.width / 2, size.height / 2), radius = maxOf(size.width, size.height) * .72f)
    onDrawWithContent { drawContent(); drawRect(vignette); drawRect(grain) }
}

@Composable
fun LocalPhoto(path: String, modifier: Modifier = Modifier, film: Boolean = false) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        val result = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(path) } }
        bitmap = result.getOrNull(); failed = result.isFailure
    }
    Box(modifier.background(LocalDiaryColors.current.panel).then(if (film) Modifier.filmTexture() else Modifier)) {
        bitmap?.let { Image(it, "기록 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (failed) DiaryText("사진을 읽을 수 없어요", Modifier.padding(12.dp), secondary = true)
    }
}

@Composable
fun MemoInput(value: String, onChange: (String) -> Unit) {
    val colors = LocalDiaryColors.current
    BasicTextField(value, { onChange(it.replace("\n", " ").take(120)) },
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .drawBehind { drawLine(colors.faint, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(vertical = 12.dp).testTag("memo-input"),
        singleLine = true, textStyle = DiaryTextStyle.copy(color = colors.ink), cursorBrush = SolidColor(colors.accent))
    DiaryMono("${value.length}/120", Modifier.padding(top = 8.dp))
}
