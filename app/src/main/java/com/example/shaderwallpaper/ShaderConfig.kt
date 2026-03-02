package com.example.shaderwallpaper

import androidx.compose.ui.graphics.Color

data class ShaderFloatParam(
    val value: Float,
    val defaultValue: Float,
    val min: Float,
    val max: Float,
    val description: String
)

data class ShaderConfig(
    val shaderId: String,
    val name: String,
    val description: String = "",
    val credit: String = "",
    val tags: List<String> = emptyList(),
    val fragmentShaderCode: String,
    val floatParams: Map<String, ShaderFloatParam> = emptyMap(),
    val colorParams: Map<String, Color> = emptyMap()
) {
    companion object {
        val DEFAULT_SHADER = """
            #ifdef GL_ES
            precision highp float;
            #endif
            
            void main() {
                gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0); // Simple magenta fallback
            }
        """.trimIndent()

        val DEFAULT_CONFIG = ShaderConfig(
            shaderId = Constants.DEFAULT_WALLPAPER_ID,
            name = Constants.FALLBACK_SHADER_NAME,
            description = "Default fallback shader",
            credit = "Unknown",
            tags = listOf("simple"),
            fragmentShaderCode = DEFAULT_SHADER,
            floatParams = mapOf(
                "speed" to ShaderFloatParam(1.0f, 1.0f, 0.1f, 5.0f, "Speed")
            ),
            colorParams = emptyMap()
        )

        private val speedUniformRegex = Regex("""uniform\s+float\s+speed\b""")

        fun buildCustomConfig(code: String): ShaderConfig {
            val floatParams = if (speedUniformRegex.containsMatchIn(code)) {
                mapOf(
                    "speed" to ShaderFloatParam(1.0f, 1.0f, 0.1f, 5.0f, "Speed")
                )
            } else {
                emptyMap()
            }

            return ShaderConfig(
                shaderId = Constants.CUSTOM_WALLPAPER_ID,
                name = "Imported Shader",
                description = "Custom imported shader",
                fragmentShaderCode = code,
                floatParams = floatParams,
                colorParams = emptyMap()
            )
        }
    }
}
