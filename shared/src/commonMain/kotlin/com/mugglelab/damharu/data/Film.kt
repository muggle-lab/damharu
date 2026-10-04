package com.mugglelab.damharu.data

import kotlin.random.Random

/**
 * Grain and vignette that a film burns into the photo. The numbers describe one look for every renderer
 * (Compose preview, Skia export, Android Bitmap export), so the viewfinder shows what gets saved.
 *
 * Vignette: a radial gradient from clear at the centre to black at [vignetteAlpha], reaching full strength
 * at [vignetteRadius] x the longer side. Grain: a repeating [GRAIN_SIDE] tile of [grainCells]; a cell above
 * 0.5 is white, below is black, with alpha |cell - 0.5| x [grainAlpha], drawn with plain source-over.
 */
data class FilmTexture(val vignetteAlpha: Float = .42f, val vignetteRadius: Float = .72f, val grainAlpha: Float = .3f) {
    companion object {
        const val GRAIN_SIDE = 96
        /** On screen one grain cell is one device pixel of a roughly 1100px-wide photo; exports scale the tile to keep that size. */
        const val GRAIN_REFERENCE_SIDE = 1100f
        /** Fixed seed: the same grain everywhere and on every run. Row-major, values in 0..1. */
        val grainCells: FloatArray by lazy { Random(7).let { random -> FloatArray(GRAIN_SIDE * GRAIN_SIDE) { random.nextFloat() } } }
        fun grainScale(longSide: Int): Int = maxOf(1, kotlin.math.round(longSide / GRAIN_REFERENCE_SIDE).toInt())
    }
}

/**
 * A film is chosen before the shot, like loading a roll: it has a fixed look (no strength slider) and is
 * baked into the single saved photo. Without a film the photo is saved exactly as captured. The lineup is
 * undecided; [Test] is a placeholder stock.
 *
 * Records store [id] as a plain string instead of the enum, so retiring a film later never breaks
 * decoding of older catalogs.
 *
 * [matrix] is an RGBA 4x5 color matrix with offsets in 0..255 units and [texture] is grain/vignette; both are
 * shared by the live preview and the native export so that what the viewfinder shows is what gets saved.
 */
enum class Film(val id: String, val label: String, val monoName: String, val matrix: FloatArray?, val texture: FilmTexture?) {
    None("", "필름 없음", "", null, null),
    Vintage("vintage", "빈티지", "VINTAGE", null, FilmTexture()),
    Test("test", "테스트 필름", "TEST FILM", floatArrayOf(
        .92f, .03f, .02f, 0f, 10f,
        .02f, .90f, .02f, 0f, 9f,
        .02f, .04f, .82f, 0f, 12f,
        0f, 0f, 0f, 1f, 0f,
    ), null);

    companion object {
        /** Unknown or retired ids fall back to no film instead of failing. */
        fun fromId(id: String?): Film = entries.firstOrNull { it.id == id } ?: None
    }
}
