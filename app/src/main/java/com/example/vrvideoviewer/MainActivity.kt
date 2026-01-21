package com.example.vrvideoviewer

//defaults
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat



//appactivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

//video selection
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest

//graphics
import android.content.Context
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface
import android.provider.MediaStore


class MainActivity : AppCompatActivity() {
    private val videoSelector = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            println("videoSelector: Video selected: $uri")
            // This is where you will eventually start the VR Activity
            val intent = android.content.Intent (
                this,
                VRVideoPlayer::class.java
            )
            intent.data = uri
            startActivity(intent)
        } else {
            println("videoSelector: No video selected")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_menu)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val openVideoButton = findViewById<Button>(R.id.openVideoButton)


        openVideoButton.setOnClickListener {
            videoSelector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

        }

    }
}

class VRVideoPlayer : AppCompatActivity() {
    private lateinit var glView : GLSurfaceView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUri : Uri? = intent.data
        if (videoUri == null) {
            finish()
            return
        }

        glView = VRGLSurfaceView(this, videoUri)
        setContentView(glView)

    }
}

class VRGLSurfaceView (private val context: Context, private val videoUri: Uri) : GLSurfaceView (context) {
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context, videoUri)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    }

}

class MyGLRenderer (private val context: Context, private val videoUri: Uri): GLSurfaceView.Renderer {
    private var surfaceTexture: SurfaceTexture? = null
    private var mediaPlayer: MediaPlayer? = null
    private var textureID: Int = 0

    private lateinit var sphereData: SphereData
    private lateinit var vertexBuffer: java.nio.FloatBuffer
    private lateinit var textureBuffer: java.nio.FloatBuffer
    private lateinit var indexBuffer: java.nio.ShortBuffer


    override fun onSurfaceCreated (unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureID = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        sphereData = SphereData (radius = 50f, horizontalSlices = 40, verticalSlices = 40)

        vertexBuffer = sphereData.getVertexBuffer()
        textureBuffer = sphereData.getTextureBuffer()
        indexBuffer = sphereData.getTriangleIndexBuffer()

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

    }

    override fun onDrawFrame(unused: GL10) {

    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {

    }


}

class SphereData (val radius: Float = 50f, val horizontalSlices: Int = 40, val verticalSlices: Int = 40) {
    private val vertexCount = (horizontalSlices + 1) * (verticalSlices + 1)
    private val vertices = FloatArray(vertexCount * 3)
    private val textureCoordinates = FloatArray (vertexCount * 2)
    private val triangleIndices = ShortArray (horizontalSlices * verticalSlices * 6)


    init {
        var verticesIndex = 0
        var textureIndex = 0

        for (h in 0..horizontalSlices) {
            val phi = (Math.PI * h / horizontalSlices).toFloat()

            for (v in 0..verticalSlices) {
                val theta = (2.0 * Math.PI * v/verticalSlices).toFloat()

                val x = (radius * Math.sin(phi.toDouble()) * Math.cos(theta.toDouble())).toFloat()
                val y = (radius * Math.cos(phi.toDouble())).toFloat()
                val z = (radius * Math.sin(phi.toDouble()) * Math.sin(theta.toDouble())).toFloat()

                vertices [verticesIndex ++] = x
                vertices [verticesIndex ++] = y
                vertices [verticesIndex ++] = z

                textureCoordinates [textureIndex ++] = v.toFloat() / verticalSlices
                textureCoordinates [textureIndex ++] = h.toFloat() / horizontalSlices

            }
        }

        var tOffset = 0

        for (h in 0..(horizontalSlices - 1)) {
            for (v in 0 .. (verticalSlices - 1)) {

                val topLeft = (h * (verticalSlices +1) + v).toShort()
                val topRight = (topLeft + 1).toShort()
                val bottomLeft = ((h + 1) * (verticalSlices + 1) + v).toShort()
                val bottomRight = (bottomLeft + 1).toShort()

                //triangle 1:: topLeft, bottomLeft, topRight
                triangleIndices [tOffset ++] = topLeft
                triangleIndices [tOffset ++] = bottomLeft
                triangleIndices [tOffset ++] = topRight


                //triangle 2:: bottomLeft, bottomRight, topRight
                triangleIndices [tOffset ++] = bottomLeft
                triangleIndices [tOffset ++] = bottomRight
                triangleIndices [tOffset ++] = topRight

            }
        }
    }


    fun getVertexBuffer(): java.nio.FloatBuffer =
        java.nio.ByteBuffer.allocateDirect (vertices.size * java.lang.Float.BYTES).run {
           order(java.nio.ByteOrder.nativeOrder())
           asFloatBuffer().put(vertices).apply {position(0)}
        }

    fun getTextureBuffer(): java.nio.FloatBuffer =
        java.nio.ByteBuffer.allocateDirect(textureCoordinates.size * java.lang.Float.BYTES).run {
            order(java.nio.ByteOrder.nativeOrder())
            asFloatBuffer().put(textureCoordinates).apply{position(0)}
        }

    fun getTriangleIndexBuffer(): java.nio.ShortBuffer =
        java.nio.ByteBuffer.allocateDirect(triangleIndices.size * java.lang.Short.BYTES).run{
            order(java.nio.ByteOrder.nativeOrder())
            asShortBuffer().put(triangleIndices).apply{position(0)}
        }
}