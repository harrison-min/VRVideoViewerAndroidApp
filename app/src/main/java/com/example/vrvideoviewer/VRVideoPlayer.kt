package com.example.vrvideoviewer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class VRVideoPlayer : AppCompatActivity() {
    private lateinit var glView : VRGLSurfaceView
    private lateinit var seekBar: SeekBar
    private var isUserScrolling = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.vr_video_player)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        seekBar = findViewById<SeekBar>(R.id.videoSeekBar)
        glView = findViewById<VRGLSurfaceView>(R.id.glView)

        val videoUri : Uri? = intent.data
        if (videoUri == null) {
            finish()
            return
        }

        glView.initRenderer(videoUri)

        setupSeekBar()
        startProgressUpdater()


    }

    private fun setupSeekBar () {
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(sb:SeekBar?, progress:Int, fromUser: Boolean) {
                if (fromUser == true) {
                    val duration = glView.getDuration()
                    val newPos = (duration * (progress/100f)).toInt()
                    glView.seekTo(newPos)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserScrolling = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserScrolling = false
            }
        })
    }
    private fun startProgressUpdater() {
       handler.post (object: Runnable {
           override fun run() {
               if (isUserScrolling == false) {
                   val currentPos = glView.getCurrentPosition()
                   val totalDuration = glView.getDuration()
                   if (totalDuration > 0) {
                       val progress = (currentPos.toFloat() / totalDuration * 100).toInt()
                       seekBar.progress = progress
                   }
               }

               val scrollDelay : Long = 500

               handler.postDelayed (this,scrollDelay)
           }
       })
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
        glView.pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onDestroy () {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        glView.shutdownPlayer()

    }
}