package com.example.shaderwallpaper

import android.content.Context

class WallpaperConfigurationManager(private val context: Context) {

    private val store = WallpaperSettingsStore(context)

    fun getActiveWallpaperConfig(): ShaderConfig {
        val activeId = store.getWallpaperId()
        return getMergedConfig(activeId)
    }

    fun getMergedConfig(shaderId: String): ShaderConfig {
        if (shaderId == Constants.CUSTOM_WALLPAPER_ID) {
            return getCustomConfig() ?: getFallbackConfig()
        }
        // Use repository to load the base config
        // Start with finding it in the repository
        val baseConfig = ShaderRepository.loadConfigById(context, shaderId) ?: getFallbackConfig()

        // Apply User Overrides (Floats)
        val floatParams = baseConfig.floatParams.toMutableMap()
        floatParams.keys.forEach { key ->
            store.getFloatOverride(shaderId, key)?.let { savedValue ->
                val existing = floatParams[key]!!
                floatParams[key] = existing.copy(value = savedValue)
            }
        }

        // Apply User Overrides (Colors)
        val colorParams = baseConfig.colorParams.toMutableMap()
        colorParams.keys.forEach { key ->
            store.getColorOverride(shaderId, key)?.let { colorParams[key] = it }
        }

        return baseConfig.copy(floatParams = floatParams, colorParams = colorParams)
    }

    private fun getCustomConfig(): ShaderConfig? {
        val code = store.getCustomShaderCode()?.trim()
        if (code.isNullOrEmpty()) return null
        return ShaderConfig.buildCustomConfig(code)
    }

    private fun getFallbackConfig(): ShaderConfig {
        // Return Default Config with saved speed if applicable
        val fallbackId = Constants.DEFAULT_WALLPAPER_ID
        val savedSpeed = store.getFloatOverride(fallbackId, "speed")

        var config = ShaderConfig.DEFAULT_CONFIG

        if (savedSpeed != null) {
            val floats = config.floatParams.toMutableMap()
            val speedParam = floats["speed"]
            if (speedParam != null) {
                floats["speed"] = speedParam.copy(value = savedSpeed)
                config = config.copy(floatParams = floats)
            }
        }
        return config
    }
}
