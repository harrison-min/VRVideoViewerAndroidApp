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
import android.opengl.GLES20
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

class VRGLSurfaceView (context: Context, private val videoUri: Uri) : GLSurfaceView (context) {
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(videoUri)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    }

}

class MyGLRenderer (private val videoUri: Uri): GLSurfaceView.Renderer {
    private var surfaceTexture: SurfaceTexture? = null
    private var textureId: Int = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun onSurfaceCreated (unused: GL10, config: EGLConfig) {

    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }


}