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
}
