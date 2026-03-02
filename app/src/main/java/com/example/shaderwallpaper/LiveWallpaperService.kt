package com.example.shaderwallpaper

import android.content.SharedPreferences
import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WallpaperEngine()
    }

    inner class WallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var renderer: WallpaperRenderer? = null
        private lateinit var store: WallpaperSettingsStore
        private lateinit var prefs: SharedPreferences

        private val engineScope = CoroutineScope(Dispatchers.Main + Job())

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            glSurfaceView = WallpaperGLSurfaceView()
            glSurfaceView?.setEGLContextClientVersion(2)
            glSurfaceView?.setPreserveEGLContextOnPause(true)

            store = WallpaperSettingsStore(applicationContext)

            prefs = applicationContext.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)

            val fallbackConfig = ShaderConfig.DEFAULT_CONFIG
            renderer = WallpaperRenderer(fallbackConfig)

            glSurfaceView?.setRenderer(renderer)
            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            // Load actual config in background
            reloadConfig()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                reloadConfig()
                glSurfaceView?.onResume()
            } else {
                glSurfaceView?.onPause()
            }
        }

        // Called automatically when the user modifies settings in the MainActivity
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (isVisible) {
                reloadConfig()
            }
        }

        private fun reloadConfig() {
            engineScope.launch {
                val newConfig = withContext(Dispatchers.IO) {
                    buildConfigFromPrefs()
                }
                glSurfaceView?.queueEvent {
                    renderer?.config = newConfig
                }
            }
        }

        /**
         * Loads the active wallpaper ID from storage, parses the corresponding JSON/GLSL from assets,
         * and applies any user overrides.
         */
        private fun buildConfigFromPrefs(): ShaderConfig {
            val configManager = WallpaperConfigurationManager(applicationContext)
            return configManager.getActiveWallpaperConfig()
        }

        override fun onDestroy() {
            super.onDestroy()
            engineScope.cancel() // Clean up coroutines
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            glSurfaceView?.destroy()
            glSurfaceView = null
        }

        inner class WallpaperGLSurfaceView : GLSurfaceView(this@LiveWallpaperService) {
            override fun getHolder(): SurfaceHolder {
                return this@WallpaperEngine.surfaceHolder
            }

            fun destroy() {
                super.onDetachedFromWindow()
            }
        }
    }
}
