package com.example.vrvideoviewer

import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent

class VRGLSurfaceView (context: Context, attrs: AttributeSet) : GLSurfaceView (context, attrs) {
    private lateinit var renderer: MyGLRenderer

    private val gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            renderer.togglePause()
            return true
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            val screenWidth = width
            val skipTime: Int = 60

            if (event.x > screenWidth/2) {
                renderer.seekRelative(skipTime)
            } else {
                renderer.seekRelative(-skipTime/2)
            }

            return true
        }

    })

    fun initRenderer (videoUri: Uri){
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context, videoUri)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    }

    override fun onTouchEvent (event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        val x = event.y
        val y = event.x
        when(event.action) {
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - renderer.getPreviousX()
                val deltaY = y - renderer.getPreviousY()
                val rotationSpeed = 0.15f

                renderer.changeAngleX(-deltaX * rotationSpeed)
                renderer.changeAngleY(-deltaY * rotationSpeed)
            }
        }
        renderer.setPreviousX(x)
        renderer.setPreviousY(y)
        return true
    }

    fun pauseVideo () {
        renderer.pauseVideo()
    }

    fun shutdownPlayer() {
        renderer.releasePlayer()
    }

    fun getDuration () : Int {
        return renderer.getDuration()
    }

    fun getCurrentPosition() : Int {
        return renderer.getCurrentPosition()
    }
    fun seekTo(position: Int) {
        renderer.seekTo(position)
    }
}
