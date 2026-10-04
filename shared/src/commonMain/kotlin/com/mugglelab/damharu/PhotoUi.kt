package com.mugglelab.damharu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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

/** Building the tile means drawing 9,216 cells, so it is made once per grain strength and kept.
 *  It must never be rebuilt from a draw cache: drawWithCache is invalidated on every size change, and the
 *  capture flight changes size every frame. Only touched from the UI thread (Compose draw). */
private val grainBrushes = mutableMapOf<Float, ShaderBrush>()

private fun grainBrush(texture: FilmTexture): ShaderBrush = grainBrushes.getOrPut(texture.grainAlpha) {
    val side = FilmTexture.GRAIN_SIDE
    val tile = ImageBitmap(side, side).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint()
        FilmTexture.grainCells.forEachIndexed { index, value ->
            paint.color = (if (value > .5f) Color.White else Color.Black).copy(alpha = abs(value - .5f) * texture.grainAlpha)
            canvas.drawRect(Rect((index % side).toFloat(), (index / side).toFloat(), index % side + 1f, index / side + 1f), paint)
        }
    }
    // One grain cell per device pixel; the brush does not depend on the size it is drawn into.
    ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

/** Live preview of a film's grain and vignette: the viewfinder, the capture flight and the film tiles use it.
 *  Saved photos get the same numbers burned in by the native export, so nothing here touches a stored file. */
fun Modifier.filmTexture(texture: FilmTexture?): Modifier = if (texture == null) this else drawWithCache {
    // Only the vignette depends on size, and a gradient brush is cheap to rebuild while the photo shrinks.
    val grain = grainBrush(texture)
    val vignette = Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = texture.vignetteAlpha)),
        center = Offset(size.width / 2, size.height / 2), radius = maxOf(size.width, size.height) * texture.vignetteRadius)
    onDrawWithContent { drawContent(); drawRect(vignette); drawRect(grain) }
}

/** [matrix] tints the bitmap at draw time; it is used while a fresh capture is shown before its film is baked in. */
@Composable
fun LocalPhoto(path: String, modifier: Modifier = Modifier, matrix: FloatArray? = null) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        val result = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(path) } }
        bitmap = result.getOrNull(); failed = result.isFailure
    }
    // Photos are shown exactly as saved: any film look is already in the pixels.
    Box(modifier.background(LocalDiaryColors.current.panel)) {
        // Decoding is asynchronous; fading in avoids a hard pop when the pixels arrive.
        val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(160))
        bitmap?.let { Image(it, "기록 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = alpha,
            colorFilter = matrix?.let { values -> ColorFilter.colorMatrix(ColorMatrix(values)) }) }
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
