package com.mugglelab.damharu.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mugglelab.damharu.DiaryTab
import com.mugglelab.damharu.DiaryTextStyle
import com.mugglelab.damharu.LocalDiaryColors
import kotlin.math.cos
import kotlin.math.sin

/** Material navigation with the diary's warm palette, without Android dynamic color. */
@Composable
fun DiaryNavigationBar(current: DiaryTab, select: (DiaryTab) -> Unit) {
    val colors = LocalDiaryColors.current
    NavigationBar(containerColor = colors.panel, tonalElevation = 0.dp) {
        DiaryTab.entries.forEach { tab ->
            val selected = current == tab
            val tint = if (selected) colors.accent else colors.muted
            NavigationBarItem(selected = selected, onClick = { select(tab) },
                modifier = Modifier.testTag("tab-${tab.id}"),
                colors = NavigationBarItemDefaults.colors(indicatorColor = colors.accent.copy(alpha = .14f)),
                label = { BasicText(tab.label, style = DiaryTextStyle.copy(color = tint)) },
                icon = {
                    Canvas(Modifier.size(24.dp)) {
                        val stroke = Stroke(1.7.dp.toPx())
                        if (tab == DiaryTab.Settings) {
                            drawCircle(tint, size.width * .29f, style = stroke)
                            drawCircle(tint, size.width * .1f, style = stroke)
                            repeat(8) { index ->
                                val angle = index * kotlin.math.PI / 4
                                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                                drawLine(tint, center + direction * size.width * .29f,
                                    center + direction * size.width * .43f, strokeWidth = stroke.width)
                            }
                        } else if (tab == DiaryTab.Films) {
                            drawRect(tint, Offset(size.width * .12f, size.height * .12f), androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .76f), style = stroke)
                            repeat(3) { i ->
                                val y = size.height * (.25f + i * .25f)
                                drawCircle(tint, 1.dp.toPx(), Offset(size.width * .24f, y))
                                drawCircle(tint, 1.dp.toPx(), Offset(size.width * .76f, y))
                            }
                        } else {
                            val path = Path().apply {
                                // A closed journal with a spine and a small bookmark.
                                moveTo(size.width * .22f, size.height * .12f)
                                lineTo(size.width * .82f, size.height * .12f)
                                lineTo(size.width * .82f, size.height * .88f)
                                lineTo(size.width * .22f, size.height * .88f)
                                quadraticTo(size.width * .12f, size.height * .88f, size.width * .12f, size.height * .78f)
                                lineTo(size.width * .12f, size.height * .22f)
                                quadraticTo(size.width * .12f, size.height * .12f, size.width * .22f, size.height * .12f)
                                close()
                                moveTo(size.width * .28f, size.height * .12f)
                                lineTo(size.width * .28f, size.height * .88f)
                                moveTo(size.width * .51f, size.height * .12f)
                                lineTo(size.width * .51f, size.height * .44f)
                                lineTo(size.width * .61f, size.height * .37f)
                                lineTo(size.width * .71f, size.height * .44f)
                                lineTo(size.width * .71f, size.height * .12f)
                            }
                            drawPath(path, tint, style = stroke)
                        }
                    }
                })
        }
    }
}
