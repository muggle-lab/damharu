package com.mugglelab.damharu.data

import androidx.compose.ui.graphics.ImageBitmap

/** Picker result is an app-owned temporary JPEG; null path without error means cancellation. */
interface PhotoSource {
    fun request(camera: Boolean, completion: (String?, String?) -> Unit)
}
expect object PhotoFiles {
    fun read(path: String): ByteArray
    fun delete(path: String)
    fun bitmap(path: String): ImageBitmap
}
