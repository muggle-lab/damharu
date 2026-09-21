package com.mugglelab.damharu.data

import org.jetbrains.skia.*
import kotlin.test.*
import kotlin.math.abs

class PhotoExportTest {
    @Test fun nativeResizeKeepsAspectRatioAndColor() {
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

            PhotoFiles.resize(source, result, 16)
            val output = Image.makeFromEncoded(PhotoFiles.read(result))
            assertEquals(16, output.width); assertEquals(12, output.height)
            val bitmap = Bitmap.makeFromImage(output)
            val pixel = bitmap.getColor(8, 6)
            // No filter exists any more: the saved photo keeps the captured color (JPEG rounding only).
            listOf(Color.getR(input) to Color.getR(pixel), Color.getG(input) to Color.getG(pixel), Color.getB(input) to Color.getB(pixel))
                .forEach { (expected, actual) -> assertTrue(abs(expected - actual) <= 4, "expected $expected actual $actual") }
            bitmap.close(); output.close()

            // Photos smaller than the limit are never upscaled.
            PhotoFiles.resize(source, result, 2048)
            val same = Image.makeFromEncoded(PhotoFiles.read(result))
            assertEquals(32, same.width); assertEquals(24, same.height); same.close()
        } finally { surface.close(); PhotoFiles.delete(source); PhotoFiles.delete(result) }
    }
}
