package com.example.shaderwallpaper

import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun GalleryShaderPreview(
    config: ShaderConfig,
    isScrolling: Boolean,
    modifier: Modifier = Modifier,
    onPreviewClicked: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Detect single taps for navigation vs long presses for preview
    val gestureDetector = remember {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onPreviewClicked()
                return true
            }
        })
    }

    val glSurfaceView = remember {
        ShaderGLSurfaceView(context).apply {
            setConfig(config)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

            onTouchListenerCallback = { event ->
                if (gestureDetector.onTouchEvent(event)) {
                    performClick()
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    false // consumed
                } else {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        }
                    }
                    true
                }
            }
        }
    }

    LaunchedEffect(isScrolling) {
        if (!isScrolling) {
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    // Lifecycle management is now easier if we use the helper or just keep existing observer
    DisposableEffect(lifecycleOwner) {
        glSurfaceView.registerLifecycle(lifecycleOwner)
        onDispose { /* Handled by observer or garbage collection */ }
    }

    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView },
        update = { view ->
            view.setConfig(config)
            if (view.renderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                 view.requestRender()
            }
        }
    )
}



