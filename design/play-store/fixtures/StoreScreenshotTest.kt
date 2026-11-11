package com.mugglelab.damharu.android

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    @Test fun captureStoreScreens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val diary = File(context.filesDir, "diary").apply { mkdirs() }
        context.assets.list("store-diary")!!.forEach { name ->
            context.assets.open("store-diary/$name").use { source -> File(diary, name).outputStream().use { source.copyTo(it) } }
        }
        device.executeShellCommand("pm grant ${context.packageName} android.permission.CAMERA")
        device.pressHome()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("담 하 루")), 20000))
        Thread.sleep(2000)
        fun screenshot(name: String) {
            device.waitForIdle()
            // Semantics can update before the Compose frame reaches the display compositor.
            Thread.sleep(1200)
            assertTrue(device.takeScreenshot(File(context.getExternalFilesDir(null), "$name.png")))
        }
        screenshot("01-home")
        device.wait(Until.findObject(By.desc("오늘 한 장 촬영")), 10000)!!.click()
        assertTrue(device.wait(Until.hasObject(By.desc("사진 촬영").enabled(true)), 10000))
        screenshot("02-camera")
        device.findObject(By.descStartsWith("필름 선택")).click()
        device.wait(Until.findObject(By.desc("빈티지")), 5000)!!.click()
        InstrumentationRegistry.getInstrumentation().uiAutomation.clearCache()
        assertTrue(device.wait(Until.hasObject(By.desc("필름 선택, 빈티지")), 5000))
        screenshot("03-film")
        device.findObject(By.desc("촬영 닫기")).click()
        val day = device.wait(Until.findObject(By.desc("10월 1일, 기록 보기")), 10000)
        assertNotNull(day); day.click()
        assertTrue(device.wait(Until.hasObject(By.text("창가에서 천천히 마신 커피.")), 10000))
        screenshot("04-memo")
    }
}
