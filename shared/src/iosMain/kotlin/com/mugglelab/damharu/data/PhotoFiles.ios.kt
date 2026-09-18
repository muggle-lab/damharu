@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.mugglelab.damharu.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy
import org.jetbrains.skia.*

actual object PhotoFiles {
    actual fun root(): String {
        val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val directory = "$documents/diary"
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
        return directory
    }
    actual fun read(path: String): ByteArray {
        val data = NSData.dataWithContentsOfFile(path) ?: error("Cannot read image")
        return ByteArray(data.length.toInt()).also { bytes -> if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }
    actual fun write(path: String, bytes: ByteArray) {
        require(bytes.isNotEmpty())
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToFile(path, atomically = true)) { "Cannot save file" }
    }
    actual fun delete(path: String) { NSFileManager.defaultManager.removeItemAtPath(path, null) }
    actual fun exists(path: String) = NSFileManager.defaultManager.fileExistsAtPath(path)
    actual fun names(path: String): List<String> = NSFileManager.defaultManager.contentsOfDirectoryAtPath(path, null)?.filterIsInstance<String>().orEmpty()
    actual fun bitmap(path: String): ImageBitmap = Image.makeFromEncoded(read(path)).toComposeImageBitmap()
    actual fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int) {
        Image.makeFromEncoded(read(source)).using { image ->
            val ratio = minOf(1f, maxSide.toFloat() / maxOf(image.width, image.height))
            Surface.makeRasterN32Premul(maxOf(1, (image.width * ratio).toInt()), maxOf(1, (image.height * ratio).toInt())).using { surface ->
                // Skia offsets use normalized components; Compose/Android matrices use 0..255.
                val normalized = matrix.copyOf().also { for (i in listOf(4,9,14,19)) it[i] /= 255f }
                ColorFilter.makeMatrix(ColorMatrix(normalized)).using { filter ->
                    Paint().using { paint ->
                        paint.colorFilter = filter
                        surface.canvas.drawImageRect(image, Rect.makeWH(surface.width.toFloat(), surface.height.toFloat()), paint)
                    }
                }
                surface.makeImageSnapshot().using { output ->
                    val data = output.encodeToData(EncodedImageFormat.JPEG, 94) ?: error("Cannot encode image")
                    data.using { write(destination, it.bytes) }
                }
            }
        }
    }
}

private inline fun <T : org.jetbrains.skia.impl.Managed, R> T.using(block: (T) -> R): R = try { block(this) } finally { close() }
