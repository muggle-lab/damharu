package com.mugglelab.damharu.data

import org.jetbrains.skia.*
import kotlin.test.*
import kotlin.math.abs

class PhotoExportTest {
    @Test fun nativeExportMatchesSharedMatrix() {
        val source = "${PhotoFiles.root()}/test-source.jpg"
        val result = "${PhotoFiles.root()}/test-result.jpg"
        val surface = Surface.makeRasterN32Premul(32, 24)
        try {
            surface.canvas.clear(Color.makeRGB(100, 150, 200))
            val image = surface.makeImageSnapshot()
            val data = image.encodeToData(EncodedImageFormat.JPEG, 100)!!
            PhotoFiles.write(source, data.bytes); data.close(); image.close()
            val decoded = Image.makeFromEncoded(PhotoFiles.read(source))
            val inputBitmap = Bitmap.makeFromImage(decoded)
            val input = inputBitmap.getColor(10, 10); inputBitmap.close(); decoded.close()
            PhotoFilter.entries.forEach { filter ->
                val matrix = filter.matrix(.7f)
                PhotoFiles.render(source, result, matrix, 16)
                val output = Image.makeFromEncoded(PhotoFiles.read(result))
                assertEquals(16, output.width); assertEquals(12, output.height)
                val bitmap = Bitmap.makeFromImage(output)
                val pixel = bitmap.getColor(8, 6)
                val rgb = intArrayOf(Color.getR(input), Color.getG(input), Color.getB(input))
                val actual = intArrayOf(Color.getR(pixel), Color.getG(pixel), Color.getB(pixel))
                for (row in 0..2) {
                    val expected = (matrix[row * 5] * rgb[0] + matrix[row * 5 + 1] * rgb[1] + matrix[row * 5 + 2] * rgb[2] + matrix[row * 5 + 4]).coerceIn(0f,255f)
                    assertTrue(abs(expected - actual[row]) <= 4, "$filter channel $row expected $expected actual ${actual[row]}")
                }
                bitmap.close(); output.close()
            }
        } finally { surface.close(); PhotoFiles.delete(source); PhotoFiles.delete(result) }
    }
}
