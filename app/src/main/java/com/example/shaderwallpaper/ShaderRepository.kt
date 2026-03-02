package com.example.shaderwallpaper

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object ShaderRepository {

    fun loadAllConfigs(context: Context): List<ShaderConfig> {
        val assetManager = context.assets
        val loadedConfigs = mutableListOf<ShaderConfig>()

        try {
            val files = assetManager.list("shaders") ?: emptyArray()
            files.filter { it.endsWith(".json") }.forEach { filename ->
                val id = filename.removeSuffix(".json")
                loadConfigById(context, id)?.let { loadedConfigs.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return loadedConfigs
    }

    fun loadConfigById(context: Context, id: String): ShaderConfig? {
        try {
            val assetManager = context.assets
            val jsonPath = "shaders/${id}.json"

            val jsonStream = assetManager.open(jsonPath)
            val jsonString = BufferedReader(InputStreamReader(jsonStream)).use { it.readText() }

            // Extract shader path from JSON to load the shader code
            val json = JSONObject(jsonString)
            var shaderPath = json.getString("shader_path")
            if (shaderPath.startsWith("assets/")) {
                shaderPath = shaderPath.substring("assets/".length)
            }
            val shaderStream = assetManager.open(shaderPath)
            val shaderCode = BufferedReader(InputStreamReader(shaderStream)).use { it.readText() }

            return ShaderJsonParser.parseConfig(id, json, shaderCode)
        } catch (e: Exception) {

            e.printStackTrace()
            return null
        }
    }
}




