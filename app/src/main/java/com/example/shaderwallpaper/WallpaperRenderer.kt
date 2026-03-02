package com.example.shaderwallpaper

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WallpaperRenderer(
    @Volatile var config: ShaderConfig
) : GLSurfaceView.Renderer {

    private var program = 0

    // Standard uniforms that ALMOST ALL shaders will need
    private var timeLocation = 0
    private var resolutionLocation = 0
    private var offsetLocation = 0
    private var dateLocation = 0

    // Track the currently compiled shader ID to detect changes
    private var currentShaderId: String? = null

    // Dynamic uniforms: Maps "variable_name" -> GL integer location
    private val uniformLocations = mutableMapOf<String, Int>()

    private val startTime = SystemClock.elapsedRealtime()

    private var surfaceWidth = 0f
    private var surfaceHeight = 0f

    private val vertices = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f,  1.0f,
        1.0f,  1.0f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4) // 4 bytes per float
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertices)

    // A simple pass-through vertex shader (doesn't change)
    private val vertexShaderCode = """
        attribute vec2 position;
        void main() {
            gl_Position = vec4(position, 0.0, 1.0);
        }
    """.trimIndent()

    private val timeUniformAliases = listOf("time", "u_time", "iTime", "uTime", "iGlobalTime")
    private val timeUniformLocations = mutableListOf<Int>()

    init {
        vertexBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, eglConfig: EGLConfig?) {
        // Initial compilation
        recompileShader(config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width.toFloat()
        surfaceHeight = height.toFloat()
    }

    private fun recompileShader(config: ShaderConfig) {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, config.fragmentShaderCode)

        if (program != 0) GLES20.glDeleteProgram(program)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Detach and delete shaders after linking
        GLES20.glDetachShader(program, vertexShader)
        GLES20.glDetachShader(program, fragmentShader)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Error linking program: $error")
        }

        timeLocation = GLES20.glGetUniformLocation(program, "time")
        resolutionLocation = GLES20.glGetUniformLocation(program, "resolution")
        offsetLocation = GLES20.glGetUniformLocation(program, "offset")
        dateLocation = GLES20.glGetUniformLocation(program, "date")

        timeUniformLocations.clear()
        timeUniformAliases.forEach { name ->
            val loc = GLES20.glGetUniformLocation(program, name)
            if (loc != -1) timeUniformLocations.add(loc)
        }

        uniformLocations.clear()

        config.floatParams.keys.forEach { key ->
            val loc = GLES20.glGetUniformLocation(program, key)
            if (loc != -1) uniformLocations[key] = loc
        }

        config.colorParams.keys.forEach { key ->
            val loc = GLES20.glGetUniformLocation(program, key)
            if (loc != -1) uniformLocations[key] = loc
        }

        currentShaderId = config.shaderId
    }

    override fun onDrawFrame(gl: GL10?) {
        val currentConfig = config

        if (currentConfig.shaderId != currentShaderId) {
            recompileShader(currentConfig)
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val time = (SystemClock.elapsedRealtime() - startTime) / 1000f
        if (timeLocation != -1) GLES20.glUniform1f(timeLocation, time)
        timeUniformLocations.forEach { loc ->
            if (loc != timeLocation) {
                GLES20.glUniform1f(loc, time)
            }
        }

        if (resolutionLocation != -1) {
            GLES20.glUniform2f(resolutionLocation, surfaceWidth, surfaceHeight)
        }

        currentConfig.floatParams.forEach { (key, param) ->
            uniformLocations[key]?.let { loc ->
                GLES20.glUniform1f(loc, param.value)
            }
        }

        currentConfig.colorParams.forEach { (key, color) ->
            uniformLocations[key]?.let { loc ->
                GLES20.glUniform3f(loc, color.red, color.green, color.blue)
            }
        }

        val positionHandle = GLES20.glGetAttribLocation(program, "position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // Check compile status
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $error")
        }

        return shader
    }
}