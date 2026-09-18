package com.mugglelab.damharu.android

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.mugglelab.damharu.data.*
import java.io.File
import kotlin.math.abs
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoFlowTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun tap(text: String) {
        if (!device.wait(Until.hasObject(By.text(text)), 8000)) {
            UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().text(text))
        }
        val target = device.findObject(By.text(text)) ?: run {
            val hierarchy = java.io.ByteArrayOutputStream(); device.dumpWindowHierarchy(hierarchy)
            error("Missing $text: $hierarchy")
        }
        target.click(); device.waitForIdle()
    }
    private fun launch() {
        device.pressHome(); device.waitForIdle()
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("담하루")), 15000))
    }
    @Test fun photoPickerFilterPersistMemoDelete() {
        val photo = Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(100,150,200)) }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,"damharu-test.jpg"); put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        })!!
        context.contentResolver.openOutputStream(uri).use { photo.compress(Bitmap.CompressFormat.JPEG,100,it!!) }; photo.recycle()
        try {
            launch()
            if (device.hasObject(By.text("오늘의 사진 보기"))) { tap("오늘의 사진 보기"); tap("사진 변경") }
            else tap("오늘 한 장 남기기")
            tap("사진 보관함에서 선택")
            device.waitForIdle()
            val image = device.wait(Until.findObject(By.descContains("Photo taken")),10000)
                ?: device.findObject(By.res("com.google.android.providers.media.module", "icon_thumbnail"))
                ?: device.findObject(By.res("com.android.providers.media.module", "icon_thumbnail"))
            if (image == null) {
                val hierarchy = java.io.ByteArrayOutputStream(); device.dumpWindowHierarchy(hierarchy)
                error(hierarchy.toString())
            }
            image.click()
            device.wait(Until.findObject(By.text("Done")), 3000)?.click()
            if (!device.wait(Until.hasObject(By.text("온기")),10000)) {
                val hierarchy = java.io.ByteArrayOutputStream(); device.dumpWindowHierarchy(hierarchy)
                error(hierarchy.toString())
            }
            tap("온기"); tap("이 사진으로 남기기")
            device.wait(Until.findObject(By.text("확인")), 1500)?.click()
            assertTrue(device.wait(Until.hasObject(By.text("오늘의 사진 보기")),10000))
            launch(); tap("오늘의 사진 보기"); tap("한 줄 남기기")
            device.findObject(By.clazz("android.widget.EditText")).text = "quiet afternoon"
            tap("한 줄 저장")
            assertTrue(device.wait(Until.hasObject(By.text("quiet afternoon")),5000))
            tap("기록 삭제"); tap("확인")
            assertTrue(device.wait(Until.hasObject(By.text("오늘 한 장 남기기")),10000))
        } finally { context.contentResolver.delete(uri,null,null) }
    }
    @Test fun nativeExportMatchesSharedMatrix() {
        PhotoFiles.initialize(context)
        val source = File(context.cacheDir,"filter-source.jpg")
        val output = File(context.cacheDir,"filter-result.jpg")
        val bitmap = Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(100,150,200)) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,100,it) }; bitmap.recycle()
        val input = BitmapFactory.decodeFile(source.path).let { val pixel=it.getPixel(10,10); it.recycle(); pixel }
        try { PhotoFilter.entries.forEach { filter ->
            val matrix = filter.matrix(.7f)
            PhotoFiles.render(source.path,output.path,matrix,16)
            val actual = BitmapFactory.decodeFile(output.path)
            assertEquals(16,actual.width); assertEquals(12,actual.height)
            val pixel = actual.getPixel(8,6); actual.recycle()
            val rgb = intArrayOf(Color.red(input),Color.green(input),Color.blue(input))
            val result = intArrayOf(Color.red(pixel),Color.green(pixel),Color.blue(pixel))
            for(row in 0..2) {
                val expected = (matrix[row*5]*rgb[0]+matrix[row*5+1]*rgb[1]+matrix[row*5+2]*rgb[2]+matrix[row*5+4]).coerceIn(0f,255f)
                assertTrue("$filter channel $row",abs(expected-result[row])<=4)
            }
        } } finally { source.delete(); output.delete() }
    }
}
