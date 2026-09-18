package com.mugglelab.damharu.data

import kotlinx.serialization.Serializable

@Serializable
enum class PhotoFilter(val label: String) {
    Original("원본"), Warm("온기"), Clear("맑음"), Film("필름"), Mono("흑백");

    /** RGBA matrix with offsets in 0..255 units, shared by preview and native export. */
    fun matrix(intensity: Float): FloatArray {
        val identity = floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
        val target = when (this) {
            Original -> identity
            Warm -> floatArrayOf(1.06f,0f,0f,0f,5f, 0f,1.01f,0f,0f,2f, 0f,0f,.91f,0f,0f, 0f,0f,0f,1f,0f)
            Clear -> floatArrayOf(1.08f,0f,0f,0f,-4f, 0f,1.08f,0f,0f,-3f, 0f,0f,1.1f,0f,-2f, 0f,0f,0f,1f,0f)
            Film -> floatArrayOf(.92f,.03f,.02f,0f,10f, .02f,.9f,.02f,0f,9f, .02f,.04f,.82f,0f,12f, 0f,0f,0f,1f,0f)
            Mono -> floatArrayOf(.2126f,.7152f,.0722f,0f,0f, .2126f,.7152f,.0722f,0f,0f, .2126f,.7152f,.0722f,0f,0f, 0f,0f,0f,1f,0f)
        }
        val amount = intensity.coerceIn(0f, 1f)
        return FloatArray(20) { identity[it] + (target[it] - identity[it]) * amount }
    }
}
