package com.mugglelab.damharu.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual object PhotoFiles {
    private lateinit var directory: File
    fun initialize(context: Context) { directory = File(context.filesDir, "diary").apply { mkdirs() } }
    actual fun root(): String = directory.absolutePath
    actual fun read(path: String) = File(path).readBytes()
    actual fun write(path: String, bytes: ByteArray) {
        val target = File(path)
        val temporary = File.createTempFile("write-", ".tmp", target.parentFile)
        try {
            temporary.outputStream().use { it.write(bytes); it.fd.sync() }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }
    actual fun delete(path: String) { File(path).delete() }
    actual fun exists(path: String) = File(path).exists()
    actual fun names(path: String) = File(path).list()?.toList().orEmpty()
    actual fun bitmap(path: String): ImageBitmap = (BitmapFactory.decodeFile(path) ?: error("Cannot read image")).asImageBitmap()
    actual fun render(source: String, destination: String, matrix: FloatArray, maxSide: Int) {
        val original = BitmapFactory.decodeFile(source) ?: error("Cannot read image")
        try {
            val scale = minOf(1f, maxSide.toFloat() / maxOf(original.width, original.height))
            val output = Bitmap.createBitmap(maxOf(1, (original.width * scale).toInt()), maxOf(1, (original.height * scale).toInt()), Bitmap.Config.ARGB_8888)
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
                Canvas(output).drawBitmap(original, null, android.graphics.Rect(0, 0, output.width, output.height), paint)
                val bytes = ByteArrayOutputStream().use { check(output.compress(Bitmap.CompressFormat.JPEG, 94, it)); it.toByteArray() }
                write(destination, bytes)
            } finally { output.recycle() }
        } finally { original.recycle() }
    }
}
