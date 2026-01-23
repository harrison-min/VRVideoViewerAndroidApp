package com.example.vrvideoviewer

import android.opengl.GLSurfaceView
import android.content.Context
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface

class MyGLRenderer (private val context: Context, private val videoUri: Uri): GLSurfaceView.Renderer {
    private var surfaceTexture: SurfaceTexture? = null
    private var mediaPlayer: MediaPlayer? = null
    private var textureID: Int = 0

    private lateinit var sphereData: SphereData
    private lateinit var vertexBuffer: java.nio.FloatBuffer
    private lateinit var textureBuffer: java.nio.FloatBuffer
    private lateinit var indexBuffer: java.nio.ShortBuffer
    private val numberOfHorizontalSlices : Int = 40
    private val numberOfVerticalSlices: Int = 40

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            // Multiply the lens by the 3D point to get the screen position
            gl_Position = uMVPMatrix * vPosition;
            // Pass the texture coordinate over to the Fragment shader
            vTexCoord = aTexCoord;
        }
    """.trimIndent()
    private val fragmentShaderCode = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 vTexCoord;
        uniform samplerExternalOES sTexture;
        void main() {
            // Look at the video texture at position vTexCoord and paint the pixel
            gl_FragColor = texture2D(sTexture, vTexCoord);
        }
    """.trimIndent()
    private var shaderProgram : Int = 0


    override fun onSurfaceCreated (unused: GL10, config: EGLConfig) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null


        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        bindTextures()
        loadSphereCalculations()

        surfaceTexture = SurfaceTexture(textureID)
        val surface = Surface(surfaceTexture)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, videoUri)
            setSurface(surface)
            isLooping = true
            prepareAsync()
            setOnPreparedListener { start() }
        }

        surface.release()
        linkShaders()
    }

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var angleX: Float = 0f
    private var angleY: Float = 0f
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        surfaceTexture?.updateTexImage()

        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        android.opengl.Matrix.rotateM(modelMatrix, 0, angleX, 1f, 0f, 0f)
        android.opengl.Matrix.rotateM(modelMatrix, 0, angleY, 0f, 1f, 0f)
        android.opengl.Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 0.1f, 0f, 0f, 0f, 0f, 1.0f, 0f )

        android.opengl.Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)


        GLES20.glUseProgram(shaderProgram)

        shaderGLLink()

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            sphereData.horizontalSlices * sphereData.verticalSlices * 6 ,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer

        )
    }

    private val projectionMatrix = FloatArray(16)
    private val FOV : Float = 75f
    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

        val viewDistance : Float = 100f

        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        android.opengl.Matrix.perspectiveM(projectionMatrix, 0, FOV, ratio, 0.1f, viewDistance)
    }


    private fun bindTextures() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureID = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun loadSphereCalculations() {
        //Load sphere tranformation calculations to renderer
        sphereData = SphereData (radius = 50f, horizontalSlices = numberOfHorizontalSlices, verticalSlices = numberOfVerticalSlices)

        vertexBuffer = sphereData.getVertexBuffer()
        textureBuffer = sphereData.getTextureBuffer()
        indexBuffer = sphereData.getTriangleIndexBuffer()


    }

    private fun linkShaders () {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)

        GLES20.glLinkProgram(shaderProgram)
    }
    private fun loadShader(type: Int, shaderCode: String): Int {
        // Create a shader "bucket" (Vertex or Fragment)
        val shader = GLES20.glCreateShader(type)
        // Put the string code into the bucket
        GLES20.glShaderSource(shader, shaderCode)
        // Tell the GPU to compile it
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun shaderGLLink() {
        val matrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0)

        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        val textureHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord")
        GLES20.glEnableVertexAttribArray(textureHandle)
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
    }

    fun getPreviousX(): Float {
        return previousX
    }
    fun getPreviousY(): Float {
        return previousY
    }
    fun setPreviousX (newValue: Float) {
        previousX = newValue
    }
    fun setPreviousY (newValue: Float) {
        previousY = newValue
    }
    fun changeAngleX (newAngle: Float) {
        angleX = (newAngle + angleX).coerceIn(-90f + FOV/2, 90f - FOV/2)
    }
    fun changeAngleY (newAngle: Float) {
        angleY += newAngle
    }

    fun seekRelative(seconds: Int) {
        mediaPlayer?.let {
            val currentPos = it.currentPosition
            val jumpTime = seconds * 1000
            val newPos = (currentPos + jumpTime).coerceIn(0, it.duration)

            it.seekTo(newPos)
        }
    }

    fun togglePause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.start()
            }
        }
    }

    fun pauseVideo () {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
               player.pause()
            }
        }
    }
    fun releasePlayer () {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        mediaPlayer = null
        surfaceTexture?.release()
        surfaceTexture = null
    }

    fun getDuration () : Int{
        val duration = mediaPlayer?.duration
        if (duration == null) {
            return 0
        }
        return duration
    }

    fun getCurrentPosition() : Int {
        val position = mediaPlayer?.currentPosition
        if (position == null) {
            return 0
        }
        return position
    }

    fun seekTo(newPosition : Int) {
        mediaPlayer?.seekTo(newPosition)
    }

}