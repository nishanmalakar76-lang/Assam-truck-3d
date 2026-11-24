package com.example.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight triangle-list mesh backed by native FloatBuffers for OpenGL ES 2.0.
 */
class GlMesh(verticesAndNormals: FloatArray) {

  private val vertexBuffer: FloatBuffer =
    ByteBuffer.allocateDirect(verticesAndNormals.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply {
        put(verticesAndNormals)
        position(0)
      }

  private val vertexCount: Int = verticesAndNormals.size / 6

  fun draw(shader: ShaderProgram) {
    vertexBuffer.position(0)
    GLES20.glEnableVertexAttribArray(shader.aPositionHandle)
    GLES20.glVertexAttribPointer(
      shader.aPositionHandle,
      3,
      GLES20.GL_FLOAT,
      false,
      24,
      vertexBuffer,
    )

    vertexBuffer.position(3)
    GLES20.glEnableVertexAttribArray(shader.aNormalHandle)
    GLES20.glVertexAttribPointer(
      shader.aNormalHandle,
      3,
      GLES20.GL_FLOAT,
      false,
      24,
      vertexBuffer,
    )

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
  }
}

object MeshPrimitives {

  private fun MutableList<Float>.pushTri(
    ax: Float, ay: Float, az: Float,
    bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float,
    nx: Float, ny: Float, nz: Float,
  ) {
    add(ax); add(ay); add(az); add(nx); add(ny); add(nz)
    add(bx); add(by); add(bz); add(nx); add(ny); add(nz)
    add(cx); add(cy); add(cz); add(nx); add(ny); add(nz)
  }

  private fun MutableList<Float>.pushSmoothVertex(
    x: Float, y: Float, z: Float,
    nx: Float, ny: Float, nz: Float,
  ) {
    add(x); add(y); add(z); add(nx); add(ny); add(nz)
  }

  fun createBox(): GlMesh {
    val d = ArrayList<Float>(36 * 6)
    val h = 0.5f
    // Front (+Z)
    d.pushTri(-h, -h, h, h, -h, h, h, h, h, 0f, 0f, 1f)
    d.pushTri(-h, -h, h, h, h, h, -h, h, h, 0f, 0f, 1f)
    // Back (-Z)
    d.pushTri(h, -h, -h, -h, -h, -h, -h, h, -h, 0f, 0f, -1f)
    d.pushTri(h, -h, -h, -h, h, -h, h, h, -h, 0f, 0f, -1f)
    // Right (+X)
    d.pushTri(h, -h, h, h, -h, -h, h, h, -h, 1f, 0f, 0f)
    d.pushTri(h, -h, h, h, h, -h, h, h, h, 1f, 0f, 0f)
    // Left (-X)
    d.pushTri(-h, -h, -h, -h, -h, h, -h, h, h, -1f, 0f, 0f)
    d.pushTri(-h, -h, -h, -h, h, h, -h, h, -h, -1f, 0f, 0f)
    // Top (+Y)
    d.pushTri(-h, h, h, h, h, h, h, h, -h, 0f, 1f, 0f)
    d.pushTri(-h, h, h, h, h, -h, -h, h, -h, 0f, 1f, 0f)
    // Bottom (-Y)
    d.pushTri(-h, -h, -h, h, -h, -h, h, -h, h, 0f, -1f, 0f)
    d.pushTri(-h, -h, -h, h, -h, h, -h, -h, h, 0f, -1f, 0f)
    return GlMesh(d.toFloatArray())
  }

  fun createPlane(): GlMesh {
    val d = ArrayList<Float>(6 * 6)
    val h = 0.5f
    d.pushTri(-h, 0f, h, h, 0f, h, h, 0f, -h, 0f, 1f, 0f)
    d.pushTri(-h, 0f, h, h, 0f, -h, -h, 0f, -h, 0f, 1f, 0f)
    return GlMesh(d.toFloatArray())
  }

  fun createCylinder(segments: Int = 14): GlMesh {
    val d = ArrayList<Float>(segments * 12 * 6)
    val r = 0.5f
    val hh = 0.5f
    for (i in 0 until segments) {
      val a0 = (i.toDouble() / segments * 2.0 * PI).toFloat()
      val a1 = ((i + 1).toDouble() / segments * 2.0 * PI).toFloat()
      val x0 = cos(a0) * r
      val z0 = sin(a0) * r
      val x1 = cos(a1) * r
      val z1 = sin(a1) * r
      val nx0 = cos(a0)
      val nz0 = sin(a0)
      val nx1 = cos(a1)
      val nz1 = sin(a1)

      // Side quad (two tris, CCW outward)
      d.pushSmoothVertex(x0, -hh, z0, nx0, 0f, nz0)
      d.pushSmoothVertex(x1, hh, z1, nx1, 0f, nz1)
      d.pushSmoothVertex(x1, -hh, z1, nx1, 0f, nz1)

      d.pushSmoothVertex(x0, -hh, z0, nx0, 0f, nz0)
      d.pushSmoothVertex(x0, hh, z0, nx0, 0f, nz0)
      d.pushSmoothVertex(x1, hh, z1, nx1, 0f, nz1)

      // Top cap
      d.pushTri(0f, hh, 0f, x1, hh, z1, x0, hh, z0, 0f, 1f, 0f)
      // Bottom cap
      d.pushTri(0f, -hh, 0f, x0, -hh, z0, x1, -hh, z1, 0f, -1f, 0f)
    }
    return GlMesh(d.toFloatArray())
  }

  fun createSphere(rings: Int = 7, sectors: Int = 10): GlMesh {
    val d = ArrayList<Float>(rings * sectors * 6 * 6)
    val r = 0.5f
    for (ring in 0 until rings) {
      val phi0 = (ring.toDouble() / rings * PI).toFloat()
      val phi1 = ((ring + 1).toDouble() / rings * PI).toFloat()
      for (sec in 0 until sectors) {
        val th0 = (sec.toDouble() / sectors * 2.0 * PI).toFloat()
        val th1 = ((sec + 1).toDouble() / sectors * 2.0 * PI).toFloat()

        fun point(phi: Float, th: Float): FloatArray {
          val x = sin(phi) * cos(th)
          val y = cos(phi)
          val z = sin(phi) * sin(th)
          return floatArrayOf(x * r, y * r, z * r, x, y, z)
        }

        val p00 = point(phi0, th0)
        val p10 = point(phi1, th0)
        val p11 = point(phi1, th1)
        val p01 = point(phi0, th1)

        // CCW outward winding
        d.pushSmoothVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5])
        d.pushSmoothVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5])
        d.pushSmoothVertex(p10[0], p10[1], p10[2], p10[3], p10[4], p10[5])

        d.pushSmoothVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5])
        d.pushSmoothVertex(p01[0], p01[1], p01[2], p01[3], p01[4], p01[5])
        d.pushSmoothVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5])
      }
    }
    return GlMesh(d.toFloatArray())
  }

  fun createPyramidRoof(segments: Int = 4): GlMesh {
    val d = ArrayList<Float>(segments * 6 * 6)
    val r = 0.5f
    val hh = 0.5f
    val angleOffset = (PI / 4.0).toFloat()
    for (i in 0 until segments) {
      val a0 = angleOffset + (i.toDouble() / segments * 2.0 * PI).toFloat()
      val a1 = angleOffset + ((i + 1).toDouble() / segments * 2.0 * PI).toFloat()
      val x0 = cos(a0) * r
      val z0 = sin(a0) * r
      val x1 = cos(a1) * r
      val z1 = sin(a1) * r

      // Compute outward face normal for (x1, -hh, z1) -> (x0, -hh, z0) -> (0, hh, 0)
      val ux = x0 - x1
      val uy = 0f
      val uz = z0 - z1
      val vx = -x1
      val vy = 2f * hh
      val vz = -z1
      var nx = uy * vz - uz * vy
      var ny = uz * vx - ux * vz
      var nz = ux * vy - uy * vx
      val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
      nx /= len; ny /= len; nz /= len
      if (ny < 0f) {
        nx = -nx; ny = -ny; nz = -nz
      }

      d.pushTri(x1, -hh, z1, x0, -hh, z0, 0f, hh, 0f, nx, ny, nz)
      d.pushTri(0f, -hh, 0f, x0, -hh, z0, x1, -hh, z1, 0f, -1f, 0f)
    }
    return GlMesh(d.toFloatArray())
  }
}
