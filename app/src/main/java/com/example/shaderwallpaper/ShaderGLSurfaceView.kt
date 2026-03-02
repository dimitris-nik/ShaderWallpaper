package com.example.shaderwallpaper

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class ShaderGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var renderer: WallpaperRenderer? = null
    var onTouchListenerCallback: ((MotionEvent) -> Boolean)? = null

    init {
        setEGLContextClientVersion(2)
        setPreserveEGLContextOnPause(true)
    }

    fun setConfig(config: ShaderConfig) {
        if (renderer == null) {
            renderer = WallpaperRenderer(config)
            setRenderer(renderer)
            // default render mode
            renderMode = RENDERMODE_CONTINUOUSLY
        } else {
            val currentRenderer = renderer!!
            if (currentRenderer.config != config) {
                currentRenderer.config = config
                if (renderMode == RENDERMODE_WHEN_DIRTY) {
                    requestRender()
                }
            }
        }
    }

    fun getRenderer(): WallpaperRenderer? {
        return renderer
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return onTouchListenerCallback?.invoke(event) ?: super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun registerLifecycle(lifecycleOwner: LifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
}
