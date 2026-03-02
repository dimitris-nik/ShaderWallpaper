package com.example.shaderwallpaper

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    // List of all loaded wallpapers found in assets/shaders/*.json
    private val _availableWallpapers = MutableStateFlow<List<ShaderConfig>>(emptyList())
    val availableWallpapers: StateFlow<List<ShaderConfig>> = _availableWallpapers.asStateFlow()

    // The currently active wallpaper configuration
    private val _shaderConfig = MutableStateFlow(ShaderConfig.DEFAULT_CONFIG)

    val shaderConfig: StateFlow<ShaderConfig> = _shaderConfig.asStateFlow()

    init {
        // Load all wallpapers on startup
        viewModelScope.launch(Dispatchers.IO) {
            loadAvailableWallpapers()

            val store = WallpaperSettingsStore(getApplication())
            val savedId = store.getWallpaperId()

            val targetId = if (savedId != "default") savedId else _availableWallpapers.value.firstOrNull()?.shaderId

            if (targetId != null) {
                selectWallpaperInternal(targetId)
            }
        }
    }

    private fun loadAvailableWallpapers() {
        val context = getApplication<Application>()
        val configs = ShaderRepository.loadAllConfigs(context).toMutableList()
        val store = WallpaperSettingsStore(context)
        val customCode = store.getCustomShaderCode()?.trim()
        if (!customCode.isNullOrEmpty()) {
            configs.add(ShaderConfig.buildCustomConfig(customCode))
        }
        _availableWallpapers.value = configs
    }

    fun selectWallpaper(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            selectWallpaperInternal(id)
        }
    }

    private fun selectWallpaperInternal(id: String) {
        val configManager = WallpaperConfigurationManager(getApplication())
        _shaderConfig.value = configManager.getMergedConfig(id)
    }

    fun importCustomShader(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        val context = getApplication<Application>()
        val store = WallpaperSettingsStore(context)
        store.saveCustomShaderCode(trimmed)
        store.setWallpaperId(Constants.CUSTOM_WALLPAPER_ID)

        val customConfig = ShaderConfig.buildCustomConfig(trimmed)
        _shaderConfig.value = customConfig
        _availableWallpapers.update { current ->
            current.filterNot { it.shaderId == Constants.CUSTOM_WALLPAPER_ID } + customConfig
        }
    }

    // --- Generic Update Actions ---


    /**
     * Updates a float parameter (like "speed" or "density") in the state.
     * This triggers the UI to redraw and the live preview to update.
     */
    fun updateFloatParam(key: String, value: Float) {
        _shaderConfig.update { current ->
            // Create a copy of the map with the new value
            val newMap = current.floatParams.toMutableMap()
            val existingParam = newMap[key]
            if (existingParam != null) {
                newMap[key] = existingParam.copy(value = value)
            }
            current.copy(floatParams = newMap)
        }
    }

    /**
     * Updates a color parameter (like "background_color") in the state.
     */
    @Suppress("unused")
    fun updateColorParam(key: String, color: Color) {
        _shaderConfig.update { current ->
            val newMap = current.colorParams.toMutableMap()
            newMap[key] = color
            current.copy(colorParams = newMap)
        }
    }

    /**
     * Persists the current configuration to device storage.
     * This allows the LiveWallpaperService to pick up these changes later.
     */
    fun saveSettingsToDevice(context: android.content.Context) {
        val store = WallpaperSettingsStore(context)
        val config = _shaderConfig.value

        // 1. Save the ID so the wallpaper service knows which JSON to load
        store.setWallpaperId(config.shaderId)

        // 2. Save all float overrides
        config.floatParams.forEach { (key, param) ->
            store.saveFloatOverride(config.shaderId, key, param.value)
        }

        // 3. Save all color overrides
        config.colorParams.forEach { (key, value) ->
            store.saveColorOverride(config.shaderId, key, value)
        }
    }

    // Default config values to avoid duplication
    companion object {
    }
}
