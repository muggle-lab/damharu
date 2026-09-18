@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.mugglelab.damharu.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy
import org.jetbrains.skia.*

actual object PhotoFiles {
    actual fun read(path: String): ByteArray {
        val data = NSData.dataWithContentsOfFile(path) ?: error("Cannot read image")
        return ByteArray(data.length.toInt()).also { bytes -> if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }
    actual fun delete(path: String) { NSFileManager.defaultManager.removeItemAtPath(path, null) }
    actual fun bitmap(path: String): ImageBitmap = Image.makeFromEncoded(read(path)).toComposeImageBitmap()
}
