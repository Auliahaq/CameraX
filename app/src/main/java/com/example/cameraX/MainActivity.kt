package com.example.cameraX

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraX.ui.CameraPermissionWrapper
import com.example.cameraX.ui.CameraScreen
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var cameraSelector by mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)

    private var camera: Camera? = null
    private var isTorchOn by mutableStateOf(false)

    private val uiExecutor: Executor by lazy { ContextCompat.getMainExecutor(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                CameraPermissionWrapper {
                    CameraScreen(
                        cameraSelector = cameraSelector,
                        onSwitch = { switchCamera() },
                        onRequestBind = { pv -> bindCamera(pv, cameraSelector) },
                        onCapture = { capturePhoto() },
                        onToggleFlash = { toggleTorch() }
                    )
                }
            }
        }
    }

    private fun switchCamera() {
        cameraSelector =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun bindCamera(previewView: PreviewView, selector: CameraSelector) {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()

            runCatching {
                camera = provider.bindToLifecycle(
                    this as LifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
            }.onFailure {
                camera = provider.bindToLifecycle(
                    this as LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                Toast.makeText(
                    this,
                    "Kamera depan tidak tersedia, pakai kamera belakang",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }, uiExecutor)
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: run {
            Toast.makeText(this, "Kamera belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Demo")
            }
        }

        val output = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        capture.takePicture(
            output,
            uiExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@MainActivity,
                        "Foto Tersimpan!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun toggleTorch() {
        val cam = camera ?: run {
            Toast.makeText(this, "Kamera belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        if (!cam.cameraInfo.hasFlashUnit()) {
            Toast.makeText(this, "Perangkat tidak punya flash", Toast.LENGTH_SHORT).show()
            return
        }

        isTorchOn = !isTorchOn
        cam.cameraControl.enableTorch(isTorchOn)
    }
}