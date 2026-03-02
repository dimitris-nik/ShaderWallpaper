package com.example.shaderwallpaper

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ShaderLivePreview(
    modifier: Modifier = Modifier,
    config: ShaderConfig,
    isPaused: Boolean = false
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val glSurfaceView = remember {
        ShaderGLSurfaceView(context).apply {
            setConfig(config)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            registerLifecycle(lifecycleOwner)
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            glSurfaceView.onPause()
        } else {
            glSurfaceView.onResume()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView },
        update = { view ->
            view.setConfig(config)
        }
    )
}

