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
    /** Re-encoding through Skia also drops any metadata the source JPEG carried. Without matrix and texture it only resizes. */
    actual fun render(source: String, destination: String, matrix: FloatArray?, texture: FilmTexture?, maxSide: Int) {
        Image.makeFromEncoded(read(source)).using { image ->
            val ratio = minOf(1f, maxSide.toFloat() / maxOf(image.width, image.height))
            Surface.makeRasterN32Premul(maxOf(1, (image.width * ratio).toInt()), maxOf(1, (image.height * ratio).toInt())).using { surface ->
                val target = Rect.makeWH(surface.width.toFloat(), surface.height.toFloat())
                if (matrix == null) surface.canvas.drawImageRect(image, target)
                else {
                    // Skia offsets use normalized components; Compose/Android matrices use 0..255.
                    val normalized = matrix.copyOf().also { for (i in listOf(4, 9, 14, 19)) it[i] /= 255f }
                    ColorFilter.makeMatrix(ColorMatrix(normalized)).using { filter ->
                        Paint().using { paint -> paint.colorFilter = filter; surface.canvas.drawImageRect(image, target, paint) }
                    }
                }
                if (texture != null) drawTexture(surface.canvas, surface.width.toFloat(), surface.height.toFloat(), texture)
                surface.makeImageSnapshot().using { output ->
                    val data = output.encodeToData(EncodedImageFormat.JPEG, 94) ?: error("Cannot encode image")
                    data.using { write(destination, it.bytes) }
                }
            }
        }
    }
}

/** Same vignette and grain as the Compose preview (see [FilmTexture]), burned into the export. */
private fun drawTexture(canvas: Canvas, width: Float, height: Float, texture: FilmTexture) {
    val full = Rect.makeWH(width, height)
    Shader.makeRadialGradient(width / 2, height / 2, maxOf(width, height) * texture.vignetteRadius,
        intArrayOf(Color.makeARGB(0, 0, 0, 0), Color.makeARGB((texture.vignetteAlpha * 255).toInt(), 0, 0, 0))).using { gradient ->
        Paint().using { paint -> paint.shader = gradient; canvas.drawRect(full, paint) }
    }
    val side = FilmTexture.GRAIN_SIDE
    Surface.makeRasterN32Premul(side, side).using { tile ->
        Paint().using { cell ->
            FilmTexture.grainCells.forEachIndexed { index, value ->
                val level = if (value > .5f) 255 else 0
                cell.color = Color.makeARGB((kotlin.math.abs(value - .5f) * texture.grainAlpha * 255).toInt(), level, level, level)
                tile.canvas.drawRect(Rect.makeXYWH((index % side).toFloat(), (index / side).toFloat(), 1f, 1f), cell)
            }
        }
        tile.makeImageSnapshot().using { image ->
            // Nearest sampling keeps grain cells crisp when the tile is scaled up for large exports.
            val scale = FilmTexture.grainScale(maxOf(width, height).toInt()).toFloat()
            image.makeShader(FilterTileMode.REPEAT, FilterTileMode.REPEAT, SamplingMode.DEFAULT, Matrix33.makeScale(scale)).using { grain ->
                Paint().using { paint -> paint.shader = grain; canvas.drawRect(full, paint) }
            }
        }
    }
}

private inline fun <T : org.jetbrains.skia.impl.Managed, R> T.using(block: (T) -> R): R = try { block(this) } finally { close() }
