package com.mugglelab.damharu.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Shared home and camera optical lens: normalized geometry keeps the metal rings and glass aligned at any density. */
internal fun DrawScope.drawLensShutter() {
    val r = size.minDimension / 2
    val c = center
    drawCircle(Color(0xFF141512), r)
    drawCircle(Brush.linearGradient(listOf(Color(0xFF68665E), Color(0xFF171817), Color(0xFF4A4A43))), r * .94f)
    drawCircle(Brush.sweepGradient(listOf(
        Color(0xFF75766F), Color(0xFFD2D1C6), Color(0xFF5E605C),
        Color(0xFFA5A69C), Color(0xFF40423E), Color(0xFFB6B7AC), Color(0xFF75766F),
    ), c), r * .85f)
    drawCircle(Color(0xFF14191C), r * .75f)
    val glass = r * .69f
    drawCircle(Brush.radialGradient(listOf(Color(0xFF080D17), Color(0xFF182036), Color(0xFF414865)), c, glass), glass)
    // Both reflections are painted within the glass disc, so no glow spills over the metal rim.
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB5DEFF).copy(alpha = .85f),
        Color(0xFF397DB1).copy(alpha = .5f), Color.Transparent),
        c + Offset(-glass * .34f, -glass * .42f), glass * .75f), glass)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF83C8F6).copy(alpha = .8f),
        Color(0xFF337BBC).copy(alpha = .45f), Color.Transparent),
        c + Offset(glass * .4f, glass * .5f), glass * .65f), glass)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF080C15), Color(0xFF0C1120).copy(alpha = .8f),
        Color.Transparent), c, glass * .85f), glass)
    drawCircle(Color(0xFF9CAED0).copy(alpha = .3f), glass, style = Stroke(r * .018f))
    drawCircle(Color(0xFF5A84AE).copy(alpha = .3f), glass * .77f, style = Stroke(r * .015f))
}
