package com.mugglelab.damharu.data

import org.jetbrains.skia.*
import kotlin.test.*
import kotlin.math.abs

class PhotoExportTest {
    @Test fun nativeRenderResizesAndAppliesFilm() {
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

            PhotoFiles.render(source, result, null, null, 16)
            val output = Image.makeFromEncoded(PhotoFiles.read(result))
            assertEquals(16, output.width); assertEquals(12, output.height)
            val bitmap = Bitmap.makeFromImage(output)
            val pixel = bitmap.getColor(8, 6)
            // Without a film the saved photo keeps the captured color (JPEG rounding only).
            listOf(Color.getR(input) to Color.getR(pixel), Color.getG(input) to Color.getG(pixel), Color.getB(input) to Color.getB(pixel))
                .forEach { (expected, actual) -> assertTrue(abs(expected - actual) <= 4, "expected $expected actual $actual") }
            bitmap.close(); output.close()

            // Photos smaller than the limit are never upscaled.
            PhotoFiles.render(source, result, null, null, 2048)
            val same = Image.makeFromEncoded(PhotoFiles.read(result))
            assertEquals(32, same.width); assertEquals(24, same.height); same.close()

            // A film is the same 4x5 matrix the preview uses, so the saved pixels must follow it.
            val matrix = Film.Test.matrix!!
            PhotoFiles.render(source, result, matrix, null, 2048)
            val filmed = Image.makeFromEncoded(PhotoFiles.read(result)); val filmedBitmap = Bitmap.makeFromImage(filmed)
            val out = filmedBitmap.getColor(10, 10)
            val rgb = intArrayOf(Color.getR(input), Color.getG(input), Color.getB(input))
            val actual = intArrayOf(Color.getR(out), Color.getG(out), Color.getB(out))
            for (row in 0..2) {
                val expected = (matrix[row * 5] * rgb[0] + matrix[row * 5 + 1] * rgb[1] + matrix[row * 5 + 2] * rgb[2] + matrix[row * 5 + 4]).coerceIn(0f, 255f)
                assertTrue(abs(expected - actual[row]) <= 4, "channel $row expected $expected actual ${actual[row]}")
            }
            filmedBitmap.close(); filmed.close()

            // Vintage burns in a vignette (corners darker than the centre) and grain (pixels no longer all equal),
            // while the centre keeps the captured brightness on average.
            PhotoFiles.render(source, result, null, Film.Vintage.texture, 2048)
            val vintage = Image.makeFromEncoded(PhotoFiles.read(result)); val vintageBitmap = Bitmap.makeFromImage(vintage)
            fun green(x0: Int, y0: Int, side: Int) = (0 until side * side).map { Color.getG(vintageBitmap.getColor(x0 + it % side, y0 + it / side)) }
            val centre = green(12, 8, 8); val corner = green(0, 0, 4)
            assertTrue(abs(centre.average() - Color.getG(input)) < 14, "centre ${centre.average()} vs ${Color.getG(input)}")
            assertTrue(centre.average() - corner.average() > 20, "corner ${corner.average()} should be darker than centre ${centre.average()}")
            assertTrue(centre.max() - centre.min() > 6, "grain should vary neighbouring pixels")
            vintageBitmap.close(); vintage.close()
        } finally { surface.close(); PhotoFiles.delete(source); PhotoFiles.delete(result) }
    }
}
