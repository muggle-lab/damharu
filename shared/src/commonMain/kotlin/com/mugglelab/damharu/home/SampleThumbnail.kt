package com.mugglelab.damharu.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/** Code-drawn sample artwork. Replace with local thumbnails during the photo task. */
@Composable
fun SampleThumbnail(scene: SampleScene, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (scene) {
            SampleScene.Coffee -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFFB4A18A), Color(0xFFE1D5BE))))
                drawCircle(Color(0xFF74634D).copy(alpha = .25f), w * .34f, Offset(w * .55f, h * .59f))
                drawCircle(Color(0xFFF0E6D5), w * .32f, Offset(w * .46f, h * .54f))
                drawCircle(Color(0xFF8F603D), w * .22f, Offset(w * .46f, h * .54f))
                drawCircle(Color(0xFFECE0CB), w * .09f, Offset(w * .8f, h * .54f), style = Stroke(w * .05f))
            }
            SampleScene.Walk -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF91B2C2), Color(0xFFE4E3D1))))
                drawOval(Color(0xFFF6F1E7).copy(alpha = .8f), Offset(w * .15f, h * .3f), Size(w * .55f, h * .12f))
                drawOval(Color(0xFF73896B), Offset(-w * .6f, h * .58f), Size(w * 2f, h))
                drawOval(Color(0xFF485F48), Offset(w * .3f, h * .73f), Size(w * 1.8f, h))
            }
            SampleScene.Book -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFF788870), Color(0xFFBFC5AA))))
                rotate(-12f) {
                    drawRect(Color(0xFFEEE4CD), Offset(w * .17f, h * .29f), Size(w * .68f, h * .5f))
                    drawLine(Color(0xFFB2A991), Offset(w * .51f, h * .29f), Offset(w * .51f, h * .79f), w * .025f)
                }
            }
            SampleScene.Sunset -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFAC98A9), Color(0xFFE6B697), Color(0xFFDCB789))))
                drawCircle(Color(0xFFFFDFB0), w * .12f, Offset(w * .65f, h * .48f))
                drawRect(Color(0xFF68756E), Offset(0f, h * .67f), Size(w, h * .33f))
                drawRect(Color(0xFF425957), Offset(0f, h * .8f), Size(w, h * .2f))
            }
        }
    }
}
