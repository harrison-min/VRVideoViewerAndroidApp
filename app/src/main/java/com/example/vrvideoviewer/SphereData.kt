package com.example.vrvideoviewer

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