package com.example.shaderwallpaper

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

class WallpaperSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun setWallpaperId(id: String) {
        prefs.edit { putString(Constants.KEY_ACTIVE_WALLPAPER_ID, id) }
    }

    fun getWallpaperId(): String {
        return prefs.getString(Constants.KEY_ACTIVE_WALLPAPER_ID, Constants.DEFAULT_WALLPAPER_ID)
            ?: Constants.DEFAULT_WALLPAPER_ID
    }

    fun saveCustomShaderCode(code: String) {
        prefs.edit { putString(Constants.KEY_CUSTOM_SHADER_CODE, code) }
    }

    fun getCustomShaderCode(): String? {
        return prefs.getString(Constants.KEY_CUSTOM_SHADER_CODE, null)
    }

    fun saveFloatOverride(shaderId: String, key: String, value: Float) {
        val compositeKey = "${shaderId}_$key"
        prefs.edit { putFloat(compositeKey, value) }
    }

    fun getFloatOverride(shaderId: String, key: String): Float? {
        val compositeKey = "${shaderId}_$key"
        if (!prefs.contains(compositeKey)) return null
        return prefs.getFloat(compositeKey, 0f)
    }

    fun saveColorOverride(shaderId: String, key: String, color: Color) {
        val compositeKey = "${shaderId}_$key"
        prefs.edit { putLong(compositeKey, color.value.toLong()) }
    }

    fun getColorOverride(shaderId: String, key: String): Color? {
        val compositeKey = "${shaderId}_$key"
        if (!prefs.contains(compositeKey)) return null
        return Color(prefs.getLong(compositeKey, 0).toULong())
    }
}
