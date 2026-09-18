package com.mugglelab.damharu.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.mugglelab.damharu.App
import com.mugglelab.damharu.data.PhotoFiles
import com.mugglelab.damharu.data.PhotoSource
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainActivity : ComponentActivity(), PhotoSource {
    private var completion: ((String?, String?) -> Unit)? = null
    private var cameraFile: File? = null
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> receive(uri) }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile
        if (success && file != null) receive(Uri.fromFile(file)) else { file?.delete(); cameraFile = null; finish(null, null) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PhotoFiles.initialize(applicationContext)
        // Uncommitted imports have no persistent references and are safe to clean at startup.
        File(cacheDir, "imports").apply { mkdirs() }.listFiles()?.forEach { it.delete() }
        File(cacheDir, "camera").apply { mkdirs() }.listFiles()?.forEach { it.delete() }
        setContent { App(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date, this) }
    }
    override fun request(camera: Boolean, completion: (String?, String?) -> Unit) {
        if (this.completion != null) { completion(null, "사진을 불러오는 중이에요."); return }
        this.completion = completion
        try {
            if (camera) {
                val file = File.createTempFile("capture-", ".jpg", File(cacheDir, "camera")); cameraFile = file
                takePhoto.launch(FileProvider.getUriForFile(this, "$packageName.photos", file))
            } else pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            cameraFile?.delete(); cameraFile = null
            finish(null, if (camera) "카메라를 열 수 없어요. 카메라 앱과 접근 설정을 확인하거나 보관함에서 선택해 주세요." else "사진 선택기를 열 수 없어요. 다시 시도해 주세요.")
        }
    }
    private fun finish(path: String?, error: String?) {
        val callback = completion; completion = null
        if (callback == null) path?.let { File(it).delete() } else callback(path, error)
    }
    private fun receive(uri: Uri?) {
        if (uri == null) { finish(null, null); return }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { import(uri) } }
            cameraFile?.delete(); cameraFile = null
            finish(result.getOrNull(), if (result.isFailure) "사진을 읽지 못했어요. 기기에 내려받은 다른 사진으로 다시 시도해 주세요." else null)
        }
    }
    private fun import(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
        val input = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Invalid image")
        try {
            val exif = contentResolver.openInputStream(uri).use { ExifInterface(requireNotNull(it)) }
            val matrix = Matrix().apply {
                postRotate(exif.rotationDegrees.toFloat())
                if (exif.isFlipped) postScale(-1f, 1f)
                val ratio = minOf(1f, 2048f / maxOf(input.width, input.height)); postScale(ratio, ratio)
            }
            val normalized = Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
            try {
                val opaque = Bitmap.createBitmap(normalized.width, normalized.height, Bitmap.Config.ARGB_8888)
                try {
                    Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(normalized, 0f, 0f, null) }
                    val output = File.createTempFile("photo-", ".jpg", File(cacheDir, "imports"))
                    try { output.outputStream().use { check(opaque.compress(Bitmap.CompressFormat.JPEG, 96, it)) } }
                    catch (error: Exception) { output.delete(); throw error }
                    return output.absolutePath
                } finally { opaque.recycle() }
            } finally { if (normalized !== input) normalized.recycle() }
        } finally { input.recycle() }
    }
}
