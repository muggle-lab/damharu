package com.mugglelab.damharu.android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.mugglelab.damharu.data.*
import java.io.File
import kotlin.math.abs
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Records are created only through the in-app camera and only for today, so this uses the emulator camera. */
@RunWith(AndroidJUnit4::class)
class PhotoFlowTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun hierarchy() = java.io.ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }.toString()
    private fun tap(text: String) {
        val target = device.wait(Until.findObject(By.text(text)), 8000) ?: error("Missing $text: ${hierarchy()}")
        target.click(); device.waitForIdle()
    }
    private fun launch() {
        device.pressHome(); device.waitForIdle()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("담 하 루")), 15000))
    }
    /** An interrupted earlier run may have left a record for today; the app then asks before replacing it. */
    private fun openCamera() {
        device.wait(Until.findObject(By.desc("오늘 한 장 촬영")), 8000)!!.click()
        device.wait(Until.findObject(By.text("확인")), 1500)?.click()
    }
    @Test fun captureSavesImmediatelyThenMemoAndDelete() {
        device.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
        launch()
        openCamera()
        val close = device.wait(Until.findObject(By.desc("촬영 닫기")), 15000)
        assertNotNull(close); close.click()
        assertTrue("closing the camera returns home unchanged", device.wait(Until.hasObject(By.desc("오늘 한 장 촬영")), 8000))

        openCamera()
        assertTrue(device.wait(Until.hasObject(By.desc("사진 촬영").enabled(true)), 15000))
        device.pressHome()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(device.wait(Until.hasObject(By.desc("사진 촬영").enabled(true)), 15000))
        val zoom = device.findObject(By.desc("광각 및 줌 배율"))
        assertNotNull(zoom)
        val bounds = zoom.visibleBounds
        device.swipe(bounds.right - 20, bounds.centerY(), bounds.left + 20, bounds.centerY(), 20)
        device.findObject(By.descStartsWith("필름 선택")).click()
        device.wait(Until.findObject(By.desc("테스트 필름")), 5000)!!.click()
        if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.clearCache()
        assertTrue(hierarchy(), device.wait(Until.hasObject(By.desc("필름 선택, 테스트 필름")), 5000))
        device.takeScreenshot(File(context.getExternalFilesDir(null), "custom-camera.png"))
        device.findObject(By.desc("사진 촬영")).click()
        // No picker, editor or confirm step: the shutter saves and returns home.
        assertTrue(hierarchy(), device.wait(Until.hasObject(By.descContains("오늘, 기록 보기")), 20000))

        launch()
        device.wait(Until.findObject(By.descContains("오늘, 기록 보기")), 10000)!!.click()
        assertTrue("today's photo can be retaken", device.wait(Until.hasObject(By.text("사진 변경")), 8000))
        assertTrue("the record remembers its film", device.hasObject(By.textContains("TEST FILM")))
        tap("오늘을 한 줄로 남겨보세요")
        device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5000).text = "quiet afternoon"
        tap("한 줄 저장")
        assertTrue(device.wait(Until.hasObject(By.text("quiet afternoon")), 5000))
        device.takeScreenshot(File(context.getExternalFilesDir(null), "photo-detail.png"))
        tap("삭제"); tap("확인")
        assertTrue(device.wait(Until.hasObject(By.descContains("오늘, 기록 남기기")), 10000))
    }
    @Test fun nativeRenderResizesAndAppliesFilm() {
        PhotoFiles.initialize(context)
        val source = File(context.cacheDir, "resize-source.jpg")
        val output = File(context.cacheDir, "resize-result.jpg")
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(100, 150, 200)) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }; bitmap.recycle()
        val input = BitmapFactory.decodeFile(source.path).let { val pixel = it.getPixel(10, 10); it.recycle(); pixel }
        try {
            PhotoFiles.render(source.path, output.path, null, null, 16)
            val actual = BitmapFactory.decodeFile(output.path)
            assertEquals(16, actual.width); assertEquals(12, actual.height)
            val pixel = actual.getPixel(8, 6); actual.recycle()
            // Without a film the saved photo keeps the captured color (JPEG rounding only).
            listOf(Color.red(input) to Color.red(pixel), Color.green(input) to Color.green(pixel), Color.blue(input) to Color.blue(pixel))
                .forEach { (expected, result) -> assertTrue("expected $expected actual $result", abs(expected - result) <= 4) }
            PhotoFiles.render(source.path, output.path, null, null, 2048)
            val same = BitmapFactory.decodeFile(output.path)
            assertEquals(32, same.width); assertEquals(24, same.height); same.recycle()
            // A film is the same 4x5 matrix the preview uses, so the saved pixels must follow it.
            val matrix = Film.Test.matrix!!
            PhotoFiles.render(source.path, output.path, matrix, null, 2048)
            val filmed = BitmapFactory.decodeFile(output.path).let { val p = it.getPixel(10, 10); it.recycle(); p }
            val rgb = intArrayOf(Color.red(input), Color.green(input), Color.blue(input))
            val out = intArrayOf(Color.red(filmed), Color.green(filmed), Color.blue(filmed))
            for (row in 0..2) {
                val expected = (matrix[row * 5] * rgb[0] + matrix[row * 5 + 1] * rgb[1] + matrix[row * 5 + 2] * rgb[2] + matrix[row * 5 + 4]).coerceIn(0f, 255f)
                assertTrue("channel $row expected $expected actual ${out[row]}", abs(expected - out[row]) <= 4)
            }
            // Vintage burns in a vignette (corners darker than the centre) and grain, keeping the centre brightness on average.
            PhotoFiles.render(source.path, output.path, null, Film.Vintage.texture, 2048)
            val vintage = BitmapFactory.decodeFile(output.path)
            fun green(x0: Int, y0: Int, side: Int) = (0 until side * side).map { Color.green(vintage.getPixel(x0 + it % side, y0 + it / side)) }
            val centre = green(12, 8, 8); val corner = green(0, 0, 4); vintage.recycle()
            assertTrue("centre ${centre.average()}", abs(centre.average() - Color.green(input)) < 14)
            assertTrue("corner ${corner.average()} centre ${centre.average()}", centre.average() - corner.average() > 20)
            assertTrue("grain should vary neighbouring pixels", centre.max() - centre.min() > 6)
        } finally { source.delete(); output.delete() }
    }
}
