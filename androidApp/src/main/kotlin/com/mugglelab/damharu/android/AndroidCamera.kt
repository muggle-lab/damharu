package com.mugglelab.damharu.android

import android.content.pm.ActivityInfo
import android.graphics.ColorMatrixColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.view.Surface
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import com.mugglelab.damharu.camera.CAMERA_PREVIEW_CORNER_DP
import com.mugglelab.damharu.camera.CameraActions
import com.mugglelab.damharu.camera.CameraScreen
import com.mugglelab.damharu.camera.CameraState
import com.mugglelab.damharu.data.Film
import java.io.File

/** CameraX owns native lifecycle, orientation and JPEG capture. UI and zoom state stay shared. */
@Composable
fun AndroidCamera(activity: ComponentActivity, onComplete: (File?, Film) -> Unit) {
    val state = remember { CameraState() }
    val preview = remember { PreviewView(activity).apply {
        // TextureView keeps the Compose viewfinder guides above the preview and lets a layer paint tint it.
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        // The native view rounds itself with the same radius the shared screen uses.
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, CAMERA_PREVIEW_CORNER_DP * view.resources.displayMetrics.density)
            }
        }
        clipToOutline = true
        scaleType = PreviewView.ScaleType.FIT_CENTER
    } }
    val latestCompletion by rememberUpdatedState(onComplete)
    val adapter = remember { AndroidCameraAdapter(activity, preview, state) { file, film -> latestCompletion(file, film) } }
    DisposableEffect(adapter) {
        val oldOrientation = activity.requestedOrientation
        val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val lightStatus = bars.isAppearanceLightStatusBars
        val lightNavigation = bars.isAppearanceLightNavigationBars
        bars.isAppearanceLightStatusBars = false
        bars.isAppearanceLightNavigationBars = false
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        adapter.start()
        onDispose {
            adapter.dispose(); activity.requestedOrientation = oldOrientation
            bars.isAppearanceLightStatusBars = lightStatus
            bars.isAppearanceLightNavigationBars = lightNavigation
        }
    }
    BackHandler { if (!state.busy) adapter.close() }
    CameraScreen(state, adapter) { modifier -> AndroidView(factory = { preview }, modifier = modifier) }
}

private class AndroidCameraAdapter(
    private val activity: ComponentActivity,
    private val preview: PreviewView,
    private val state: CameraState,
    private val complete: (File?, Film) -> Unit,
) : CameraActions {
    private val executor = ContextCompat.getMainExecutor(activity)
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val live = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
    private val capture = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    private var disposed = false
    private var completed = false
    private var pending: File? = null
    private var zoomGeneration = 0
    private val streamObserver = Observer<PreviewView.StreamState> { stream ->
        state.ready = stream == PreviewView.StreamState.STREAMING && state.error == null
    }
    private val cameraObserver = Observer<androidx.camera.core.CameraState> { status ->
        if (status.error != null) {
            state.ready = false
            state.busy = false
            state.error = "카메라를 사용할 수 없어요. 닫고 다시 시도해 주세요."
        }
    }
    fun start() {
        preview.previewStreamState.observe(activity, streamObserver)
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            if (disposed) return@addListener
            try {
                val owner = future.get(); provider = owner
                live.setSurfaceProvider(preview.surfaceProvider)
                capture.targetRotation = preview.display?.rotation ?: Surface.ROTATION_0
                val bound = owner.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, live, capture)
                camera = bound
                val range = requireNotNull(bound.cameraInfo.zoomState.value)
                state.configure(range.minZoomRatio, minOf(6f, range.maxZoomRatio).coerceAtLeast(range.minZoomRatio), 1f)
                bound.cameraInfo.cameraState.observe(activity, cameraObserver)
                zoom(state.zoom)
            } catch (_: Exception) {
                state.error = "카메라를 열 수 없어요. 닫고 다시 시도해 주세요."
            }
        }, executor)
    }
    override fun zoom(value: Float) {
        val bound = camera ?: return
        val generation = ++zoomGeneration
        val future = bound.cameraControl.setZoomRatio(value.coerceIn(state.minimum, state.maximum))
        future.addListener({
            if (!disposed && generation == zoomGeneration) {
                try { future.get() }
                catch (_: Exception) { state.zoom = bound.cameraInfo.zoomState.value?.zoomRatio ?: state.minimum }
            }
        }, executor)
    }
    override fun film(value: Film) {
        // The hardware layer applies the film's shared sRGB matrix to the live TextureView only;
        // the captured JPEG stays unfiltered and the shared store bakes the film in when it saves.
        val matrix = value.matrix
        if (matrix == null) preview.setLayerType(View.LAYER_TYPE_NONE, null)
        else preview.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
        preview.invalidate()
    }
    override fun capture() {
        if (disposed || state.busy || !state.ready) return
        state.busy = true
        val film = state.film
        val file = try { File.createTempFile("capture-", ".jpg", File(activity.cacheDir, "camera").apply { mkdirs() }) }
        catch (_: Exception) { state.busy = false; state.error = "촬영할 공간이 부족해요. 닫고 다시 시도해 주세요."; return }
        pending = file
        capture.targetRotation = preview.display?.rotation ?: Surface.ROTATION_0
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                pending = null
                if (disposed || completed) { file.delete(); return }
                completed = true; complete(file, film)
            }
            override fun onError(exception: ImageCaptureException) {
                file.delete(); pending = null
                if (!disposed) { state.busy = false; state.error = "촬영하지 못했어요. 닫고 다시 시도해 주세요." }
            }
        })
    }
    override fun close() {
        if (!state.busy && !completed) { completed = true; complete(null, Film.None) }
    }
    fun dispose() {
        disposed = true
        preview.previewStreamState.removeObserver(streamObserver)
        camera?.cameraInfo?.cameraState?.removeObserver(cameraObserver)
        provider?.unbind(live, capture)
        // An in-flight CameraX callback owns deletion of its unfinished output after unbinding.
        if (!state.busy) pending?.delete()
        preview.setLayerType(View.LAYER_TYPE_NONE, null)
    }
}
