package com.mugglelab.damharu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mugglelab.damharu.data.PhotoFiles
import com.mugglelab.damharu.home.DiaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LocalPhoto(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path) {
        val result = withContext(Dispatchers.Default) { runCatching { PhotoFiles.bitmap(path) } }
        bitmap = result.getOrNull(); failed = result.isFailure
    }
    Box(modifier.background(LocalDiaryColors.current.line)) {
        bitmap?.let { Image(it, "선택한 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        if (failed) DiaryText("사진을 읽을 수 없어요", Modifier.padding(12.dp), secondary = true)
    }
}
