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
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
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

/** The in-app CameraX screen is the only photo input; there is no gallery or external camera path. */
class MainActivity : ComponentActivity(), PhotoSource {
    private var showingCamera by mutableStateOf(false)
    private var completion: ((String?, String?) -> Unit)? = null
    private var cameraFile: File? = null
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (completion != null) {
            if (granted) showingCamera = true
            // There is no other way to add a photo, so a denied permission must point at Settings.
            else finish(null, "카메라 접근이 꺼져 있어요. 설정 > 앱 > 담하루에서 카메라를 허용하면 오늘을 남길 수 있어요.")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app uses one fixed dark palette, so system bar icons stay light regardless of the system theme.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        PhotoFiles.initialize(applicationContext)
        // Unsaved captures have no persistent references and are safe to clean at startup.
        File(cacheDir, "captures").apply { mkdirs() }.listFiles()?.forEach { it.delete() }
        File(cacheDir, "camera").apply { mkdirs() }.listFiles()?.forEach { it.delete() }
        setContent {
            Box {
                Box(if (showingCamera) Modifier.clearAndSetSemantics { } else Modifier) {
                    App(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date, this@MainActivity)
                }
                if (showingCamera) AndroidCamera(this@MainActivity) { file ->
                    showingCamera = false
                    if (file == null) finish(null, null)
                    else { cameraFile = file; receive(Uri.fromFile(file)) }
                }
            }
        }
    }
    override fun capture(completion: (String?, String?) -> Unit) {
        if (this.completion != null) { completion(null, "사진을 준비하는 중이에요."); return }
        this.completion = completion
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) showingCamera = true
            else cameraPermission.launch(Manifest.permission.CAMERA)
        } catch (_: Exception) {
            cameraFile?.delete(); cameraFile = null
            finish(null, "카메라를 열 수 없어요. 접근 설정을 확인하고 다시 시도해 주세요.")
        }
    }
    private fun finish(path: String?, error: String?) {
        val callback = completion; completion = null
        if (callback == null) path?.let { File(it).delete() } else callback(path, error)
    }
    private fun receive(uri: Uri?) {
        if (uri == null) { finish(null, null); return }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { normalize(uri) } }
            cameraFile?.delete(); cameraFile = null
            finish(result.getOrNull(), if (result.isFailure) "촬영한 사진을 준비하지 못했어요. 다시 시도해 주세요." else null)
        }
    }
    /** CameraX writes rotation as EXIF. Bake it in, cap the size, and re-encode without any metadata. */
    private fun normalize(uri: Uri): String {
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
                    val output = File.createTempFile("photo-", ".jpg", File(cacheDir, "captures"))
                    try { output.outputStream().use { check(opaque.compress(Bitmap.CompressFormat.JPEG, 96, it)) } }
                    catch (error: Exception) { output.delete(); throw error }
                    return output.absolutePath
                } finally { opaque.recycle() }
            } finally { if (normalized !== input) normalized.recycle() }
        } finally { input.recycle() }
    }
}
