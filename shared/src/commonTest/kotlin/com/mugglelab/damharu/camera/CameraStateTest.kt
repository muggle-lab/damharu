package com.mugglelab.damharu.camera

import kotlin.test.*

class CameraStateTest {
    @Test fun zoomRespectsDeviceLimitsAndRejectsNonFiniteInput() {
        assertEquals(1f, clampZoom(.5f, 1f, 4f))
        assertEquals(4f, clampZoom(6f, 1f, 4f))
        assertEquals(1.7f, clampZoom(1.7f, .5f, 6f))
        assertEquals(.5f, clampZoom(Float.NaN, .5f, 6f))
        assertEquals(1f, clampZoom(Float.POSITIVE_INFINITY, 1f, 4f))
    }
    @Test fun cameraWithoutZoomHasValidSingleValueRange() {
        val state = CameraState()
        state.configure(1f, 1f, 2f)
        assertEquals(1f, state.zoom)
        assertFailsWith<IllegalArgumentException> { state.configure(2f, 1f, 1f) }
    }
    @Test fun rulerIsLaidOutByZoomRatio() {
        // One doubling is always the same distance, wherever it happens on the ruler.
        val wide = zoomPosition(1f) - zoomPosition(.5f)
        val tele = zoomPosition(6f) - zoomPosition(3f)
        assertEquals(1f, wide, 1e-4f); assertEquals(wide, tele, 1e-4f)
        assertEquals(0f, zoomPosition(1f), 1e-6f)
        listOf(.5f, .8f, 1f, 1.7f, 3.1f, 6f).forEach { assertEquals(it, zoomAtPosition(zoomPosition(it)), 1e-4f) }
        // The same drag moves the zoom by the same factor from any starting point.
        assertEquals(zoomAtPosition(zoomPosition(.5f) + .4f) / .5f, zoomAtPosition(zoomPosition(4f) + .4f) / 4f, 1e-4f)
    }
    @Test fun ticksStayInsideTheDeviceRangeAndThinOutAtTheTeleEnd() {
        val ticks = zoomTicks(.5f, 6f)
        assertEquals(.5f, ticks.first().zoom); assertEquals(6f, ticks.last().zoom)
        assertEquals(listOf(.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f), ticks.filter { it.major }.map { it.zoom })
        assertEquals(ticks.map { it.zoom }.sorted(), ticks.map { it.zoom })
        assertTrue(ticks.any { it.zoom == 1.1f }); assertTrue(ticks.any { it.zoom == 2.25f })
        assertFalse(ticks.any { it.zoom == 3.1f }, "tenths above 2x would crowd on a log scale")
        // Neighbouring ticks never get closer than about 5dp at 96dp per doubling.
        ticks.zipWithNext().forEach { (a, b) -> assertTrue((zoomPosition(b.zoom) - zoomPosition(a.zoom)) * 96f > 5f, "$a $b") }
        assertEquals(listOf(1f), zoomTicks(1f, 1f).map { it.zoom })
        assertEquals(1f, zoomTicks(1f, 4f).first().zoom)
    }
    @Test fun previewRectMirrorsTheCameraLayout() {
        // Tall phone: the side margins limit the width.
        val tall = cameraPreviewRect(400f, 900f, safeTop = 50f, safeBottom = 30f, density = 1f)
        assertEquals(12f, tall.left); assertEquals(376f, tall.width); assertEquals(.75f, tall.width / tall.height, 1e-4f)
        // Centred between the top bar (50 + 44) and the controls (900 - 30 - 224).
        assertEquals((94f + 646f) / 2, (tall.top + tall.bottom) / 2, 1e-3f)
        // Short window: the available height limits it instead, and it stays centred horizontally.
        val short = cameraPreviewRect(400f, 600f, 0f, 0f, 1f)
        assertEquals(332f, short.height, 1e-3f); assertEquals(249f, short.width, 1e-3f); assertEquals(200f, (short.left + short.right) / 2, 1e-3f)
        // Density scales the fixed parts, not the window.
        assertEquals(24f, cameraPreviewRect(800f, 1800f, 0f, 0f, 2f).left)
    }
}
