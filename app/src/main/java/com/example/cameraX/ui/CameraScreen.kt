package com.example.cameraX.ui

import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraScreen(
    cameraSelector: CameraSelector,
    onSwitch: () -> Unit,
    onRequestBind: (PreviewView) -> Unit,
    onCapture: () -> Unit,
    onToggleFlash: () -> Unit
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(cameraSelector, previewView) {
        previewView?.let { onRequestBind(it) }
    }

    Box(Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            }
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onToggleFlash) {
                Text("Flash")
            }
            Button(onClick = onSwitch) {
                Text("Switch")
            }
            Button(onClick = onCapture) {
                Text("Capture")
            }
        }
    }
}
