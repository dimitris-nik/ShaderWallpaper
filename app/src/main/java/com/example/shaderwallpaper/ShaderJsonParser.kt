package com.example.shaderwallpaper

import androidx.compose.ui.graphics.Color
import org.json.JSONObject

object ShaderJsonParser {

    fun parseConfig(id: String, json: JSONObject, shaderCode: String): ShaderConfig {

        val name = json.optString("name", id.replace("_", " ").run {

            replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
        })

        val description = json.optString("description", "")
        val credit = json.optString("credit", "")
        val tags = mutableListOf<String>()
        val tagsArray = json.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }

        val variables = json.getJSONObject("variables")
        val floatParams = mutableMapOf<String, ShaderFloatParam>()
        val colorParams = mutableMapOf<String, Color>()

        val keys = variables.keys()
        while(keys.hasNext()) {
            val key = keys.next()
            val varObj = variables.getJSONObject(key)
            val type = varObj.getString("type")

            if (type == "float") {
                val defaultVal = varObj.getDouble("default").toFloat()
                val minVal = varObj.optDouble("min", 0.0).toFloat()
                val maxVal = varObj.optDouble("max", 10.0).toFloat()
                val desc = varObj.optString("description", "")

                floatParams[key] = ShaderFloatParam(
                    value = defaultVal,
                    defaultValue = defaultVal,
                    min = minVal,
                    max = maxVal,
                    description = desc
                )
            } else if (type == "color") {
                val defaultHex = varObj.getString("default")
                colorParams[key] = Color(android.graphics.Color.parseColor(defaultHex))
            }
        }

        return ShaderConfig(
            shaderId = id,
            name = name,
            description = description,
            credit = credit,
            tags = tags,
            fragmentShaderCode = shaderCode,
            floatParams = floatParams,
            colorParams = colorParams
        )
    }
}


