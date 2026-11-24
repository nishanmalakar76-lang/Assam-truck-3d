package com.example.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.CameraMode
import com.example.sim.TruckSimulationEngine
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.sin

class AssamRoadRenderer(
  private val engine: TruckSimulationEngine,
) : GLSurfaceView.Renderer {

  private lateinit var shader: ShaderProgram
  private lateinit var boxMesh: GlMesh
  private lateinit var planeMesh: GlMesh
  private lateinit var cylinderMesh: GlMesh
  private lateinit var sphereMesh: GlMesh
  private lateinit var roofMesh: GlMesh

  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)

  private var lastFrameNs: Long = 0L

  private data class TreeSpec(val x: Float, val baseZ: Float, val scale: Float)
  private data class BushSpec(val x: Float, val baseZ: Float, val scale: Float)
  private data class HouseSpec(val x: Float, val baseZ: Float, val yaw: Float)

  private val trees = buildList {
    for (i in 0 until 18) {
      val z = -220f + i * 13.5f
      val jitterL = ((i * 37) % 10) * 0.55f
      val jitterR = ((i * 53) % 10) * 0.55f
      add(TreeSpec(-10.5f - jitterL, z, 0.85f + ((i * 19) % 5) * 0.11f))
      add(TreeSpec(10.5f + jitterR, z + 6.2f, 0.85f + ((i * 29) % 5) * 0.11f))
    }
  }

  private val teaBushes = buildList {
    for (i in 0 until 40) {
      val z = -220f + i * 6.0f
      val offsetL = ((i * 17) % 10) * 0.22f
      val offsetR = ((i * 23) % 10) * 0.22f
      add(BushSpec(-7.7f - offsetL, z, 0.9f + (i % 3) * 0.15f))
      add(BushSpec(7.7f + offsetR, z + 2.6f, 0.9f + ((i + 1) % 3) * 0.15f))
    }
  }

  private val houses = listOf(
    HouseSpec(-15.5f, -35f, 8f),
    HouseSpec(16.0f, -95f, -10f),
    HouseSpec(-16.5f, -155f, 5f),
    HouseSpec(15.2f, -205f, -6f),
  )

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.529f, 0.808f, 0.922f, 1.0f) // #87CEEB sunny Assam sky
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)

    shader = ShaderProgram()
    boxMesh = MeshPrimitives.createBox()
    planeMesh = MeshPrimitives.createPlane()
    cylinderMesh = MeshPrimitives.createCylinder(14)
    sphereMesh = MeshPrimitives.createSphere(7, 10)
    roofMesh = MeshPrimitives.createPyramidRoof(4)

    lastFrameNs = System.nanoTime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
    val fov = if (aspect < 1f) 68f else 58f
    Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, 0.3f, 360f)
  }

  override fun onDrawFrame(gl: GL10?) {
    val now = System.nanoTime()
    val dt = ((now - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
    lastFrameNs = now

    engine.step(dt)

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    shader.use()
    GLES20.glUniform3f(shader.uFogColorHandle, 0.529f, 0.808f, 0.922f)

    val truckX = engine.truckX
    val camX = engine.cameraX
    val speed = engine.speedKmh
    val scrollZ = engine.worldScrollZ
    val steer = engine.steerInput
    val wheelSpinDeg = engine.wheelSpinRadians * (180f / PI.toFloat())
    val isBraking = engine.brakePressed.get()

    when (engine.cameraMode) {
      CameraMode.CHASE -> {
        Matrix.setLookAtM(
          viewMatrix, 0,
          camX, 5.8f, 12.2f,
          truckX * 0.82f, 1.6f, -14f,
          0f, 1f, 0f,
        )
      }
      CameraMode.CLOSE -> {
        Matrix.setLookAtM(
          viewMatrix, 0,
          camX * 0.9f, 4.1f, 8.6f,
          truckX, 1.5f, -15f,
          0f, 1f, 0f,
        )
      }
      CameraMode.HIGH -> {
        Matrix.setLookAtM(
          viewMatrix, 0,
          camX * 0.6f, 10.5f, 15.5f,
          truckX * 0.5f, 1.0f, -16f,
          0f, 1f, 0f,
        )
      }
    }

    Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    // 1. Lush Assam tea-garden ground + distant hills
    drawMesh(
      planeMesh,
      0f, -0.14f, -80f,
      520f, 1f, 520f,
      0.224f, 0.482f, 0.208f, // #397B35
    )
    // Gentle distant Brahmaputra valley hills on the horizon
    drawMesh(sphereMesh, -75f, -2f, -210f, 130f, 34f, 60f, 0.20f, 0.44f, 0.30f)
    drawMesh(sphereMesh, 82f, -3f, -215f, 145f, 38f, 65f, 0.18f, 0.42f, 0.29f)

    // 2. Two-lane asphalt highway (#33383D)
    drawMesh(
      planeMesh,
      0f, 0f, -95f,
      12f, 1f, 360f,
      0.200f, 0.220f, 0.239f,
    )
    // Continuous white edge lines
    drawMesh(boxMesh, -5.85f, 0.025f, -95f, 0.18f, 0.04f, 360f, 0.94f, 0.95f, 0.96f, unlit = true)
    drawMesh(boxMesh, 5.85f, 0.025f, -95f, 0.18f, 0.04f, 360f, 0.94f, 0.95f, 0.96f, unlit = true)

    // 3. Scrolling center dashed lane dividers (every 12m over 240m loop)
    for (i in 0 until 20) {
      val z = wrapZ(-210f + i * 12f, scrollZ)
      drawMesh(boxMesh, 0f, 0.03f, z, 0.20f, 0.035f, 4.8f, 0.98f, 0.98f, 0.96f, unlit = true)
    }

    // 4. Scrolling milestone KM posts
    for (i in 0 until 4) {
      val z = wrapZ(-200f + i * 60f, scrollZ)
      drawMesh(boxMesh, 6.7f, 0.5f, z, 0.32f, 1.0f, 0.22f, 0.95f, 0.95f, 0.95f)
      drawMesh(boxMesh, 6.7f, 0.85f, z, 0.34f, 0.32f, 0.24f, 0.95f, 0.72f, 0.12f)
    }

    // 5. Scrolling Assam tea bushes along both sides of the highway
    for (bush in teaBushes) {
      val z = wrapZ(bush.baseZ, scrollZ)
      drawMesh(
        sphereMesh,
        bush.x, 0.30f * bush.scale, z,
        1.25f * bush.scale, 0.68f * bush.scale, 1.55f * bush.scale,
        0.184f, 0.545f, 0.235f, // #2F8B3C
      )
    }

    // 6. Scrolling roadside trees
    for (tree in trees) {
      val z = wrapZ(tree.baseZ, scrollZ)
      val s = tree.scale
      drawMesh(
        cylinderMesh,
        tree.x, 1.0f * s, z,
        0.44f * s, 2.0f * s, 0.44f * s,
        0.463f, 0.314f, 0.176f, // #76502D
      )
      drawMesh(
        sphereMesh,
        tree.x, 2.55f * s, z,
        2.35f * s, 2.65f * s, 2.35f * s,
        0.137f, 0.420f, 0.176f, // #236B2D
      )
    }

    // 7. Scrolling Assamese village houses
    for (house in houses) {
      val z = wrapZ(house.baseZ, scrollZ)
      drawHouse(house.x, z, house.yaw)
    }

    // 8. Detailed 3D Assam Cargo Truck
    drawTruck(truckX, steer, speed, wheelSpinDeg, isBraking)
  }

  private fun wrapZ(baseZ: Float, scrollZ: Float): Float {
    var z = (baseZ + scrollZ + 210f) % 240f
    if (z < 0f) z += 240f
    return z - 210f // Range: [-210f, +30f]
  }

  private fun drawHouse(x: Float, z: Float, yawDeg: Float) {
    // Mud-ochre walls (#D8B77B)
    drawMesh(boxMesh, x, 0.95f, z, 3.2f, 1.9f, 2.6f, 0.847f, 0.718f, 0.482f, yawDeg = yawDeg)
    // Sloped terracotta roof (#8D3424)
    drawMesh(roofMesh, x, 2.55f, z, 4.4f, 1.55f, 3.8f, 0.553f, 0.204f, 0.141f, yawDeg = yawDeg)
  }

  private fun drawTruck(
    x: Float,
    steer: Float,
    speed: Float,
    wheelSpinDeg: Float,
    isBraking: Boolean,
  ) {
    val speedRatio = (speed / 95f).coerceIn(0f, 1f)
    val bodyRollDeg = -steer * speedRatio * 3.8f
    val bodyYawDeg = -steer * 6.5f
    val bounceY = if (speed > 1f) sin(wheelSpinDeg * 0.12f) * 0.025f * speedRatio else 0f

    // Ground contact shadow
    drawMesh(planeMesh, x, 0.04f, 0.4f, 4.1f, 1f, 8.2f, 0.09f, 0.10f, 0.11f, yawDeg = bodyYawDeg)

    // Helper transform around truck center (x, bounceY, 0.5f)
    fun drawTruckPart(
      mesh: GlMesh,
      lx: Float, ly: Float, lz: Float,
      sx: Float, sy: Float, sz: Float,
      r: Float, g: Float, b: Float,
      unlit: Boolean = false,
      localRotX: Float = 0f,
      localRotY: Float = 0f,
      localRotZ: Float = 0f,
    ) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, x, bounceY, 0.5f)
      Matrix.rotateM(modelMatrix, 0, bodyYawDeg, 0f, 1f, 0f)
      Matrix.rotateM(modelMatrix, 0, bodyRollDeg, 0f, 0f, 1f)
      Matrix.translateM(modelMatrix, 0, lx, ly, lz)
      if (localRotY != 0f) Matrix.rotateM(modelMatrix, 0, localRotY, 0f, 1f, 0f)
      if (localRotX != 0f) Matrix.rotateM(modelMatrix, 0, localRotX, 1f, 0f, 0f)
      if (localRotZ != 0f) Matrix.rotateM(modelMatrix, 0, localRotZ, 0f, 0f, 1f)
      Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
      Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
      GLES20.glUniformMatrix4fv(shader.uMvpMatrixHandle, 1, false, mvpMatrix, 0)
      GLES20.glUniformMatrix4fv(shader.uModelMatrixHandle, 1, false, modelMatrix, 0)
      GLES20.glUniform4f(shader.uColorHandle, r, g, b, 1f)
      GLES20.glUniform1f(shader.uUnlitHandle, if (unlit) 1f else 0f)
      mesh.draw(shader)
    }

    // Chassis frame (#20252A)
    drawTruckPart(boxMesh, 0f, 0.72f, 0f, 2.7f, 0.28f, 7.2f, 0.125f, 0.145f, 0.165f)

    // Royal blue cargo box (#174FBD) at rear (+Z)
    drawTruckPart(boxMesh, 0f, 2.25f, 1.35f, 3.35f, 2.35f, 4.9f, 0.090f, 0.310f, 0.741f)
    // Decorative Assam saffron trim band along cargo box
    drawTruckPart(boxMesh, 0f, 1.38f, 1.35f, 3.40f, 0.24f, 4.94f, 0.98f, 0.76f, 0.18f)
    // Rear cargo door seam & bumper
    drawTruckPart(boxMesh, 0f, 2.25f, 3.82f, 0.08f, 2.15f, 0.04f, 0.05f, 0.18f, 0.45f)
    drawTruckPart(boxMesh, 0f, 0.68f, 3.85f, 3.35f, 0.26f, 0.22f, 0.15f, 0.17f, 0.19f)

    // Rear tail / brake lights (glow bright red when braking)
    val tailR = if (isBraking) 1.0f else 0.72f
    val tailG = if (isBraking) 0.16f else 0.08f
    drawTruckPart(boxMesh, -1.25f, 0.96f, 3.84f, 0.46f, 0.22f, 0.08f, tailR, tailG, 0.08f, unlit = isBraking)
    drawTruckPart(boxMesh, 1.25f, 0.96f, 3.84f, 0.46f, 0.22f, 0.08f, tailR, tailG, 0.08f, unlit = isBraking)

    // Saffron-gold cab (#E2A52B) at front (-Z)
    drawTruckPart(boxMesh, 0f, 1.95f, -2.25f, 3.05f, 2.15f, 2.15f, 0.886f, 0.647f, 0.169f)
    // Cab roof visor / carrier bar
    drawTruckPart(boxMesh, 0f, 3.12f, -2.15f, 2.85f, 0.16f, 1.6f, 0.98f, 0.80f, 0.24f)
    // Tinted sky-blue windshield (#9DD8ED) facing forward (-Z)
    drawTruckPart(boxMesh, 0f, 2.38f, -3.33f, 2.65f, 0.86f, 0.06f, 0.616f, 0.847f, 0.929f, unlit = true)
    // Cab side windows
    drawTruckPart(boxMesh, -1.53f, 2.38f, -2.35f, 0.05f, 0.78f, 1.15f, 0.35f, 0.56f, 0.68f, unlit = true)
    drawTruckPart(boxMesh, 1.53f, 2.38f, -2.35f, 0.05f, 0.78f, 1.15f, 0.35f, 0.56f, 0.68f, unlit = true)
    // Front bumper & glowing headlights
    drawTruckPart(boxMesh, 0f, 0.68f, -3.40f, 3.25f, 0.28f, 0.28f, 0.125f, 0.145f, 0.165f)
    drawTruckPart(boxMesh, -1.15f, 1.08f, -3.34f, 0.42f, 0.22f, 0.06f, 1.0f, 0.96f, 0.76f, unlit = true)
    drawTruckPart(boxMesh, 1.15f, 1.08f, -3.34f, 0.42f, 0.22f, 0.06f, 1.0f, 0.96f, 0.76f, unlit = true)

    // 4 spinning wheels (front wheels pivot slightly with steering)
    val wheelXs = floatArrayOf(-1.68f, 1.68f)
    val wheelZs = floatArrayOf(-2.25f, 2.55f)
    for (wx in wheelXs) {
      for (wz in wheelZs) {
        val frontPivot = if (wz < 0f) -steer * 18f else 0f
        // Rubber tire
        drawTruckPart(
          cylinderMesh,
          wx, 0.58f, wz,
          1.15f, 0.42f, 1.15f,
          0.12f, 0.14f, 0.16f,
          localRotY = frontPivot,
          localRotX = wheelSpinDeg,
          localRotZ = 90f,
        )
        // Silver hubcap
        drawTruckPart(
          cylinderMesh,
          wx + (if (wx < 0f) -0.04f else 0.04f), 0.58f, wz,
          0.60f, 0.40f, 0.60f,
          0.74f, 0.78f, 0.82f,
          localRotY = frontPivot,
          localRotX = wheelSpinDeg,
          localRotZ = 90f,
        )
      }
    }
  }

  private fun drawMesh(
    mesh: GlMesh,
    x: Float, y: Float, z: Float,
    sx: Float, sy: Float, sz: Float,
    r: Float, g: Float, b: Float,
    unlit: Boolean = false,
    yawDeg: Float = 0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (yawDeg != 0f) {
      Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
    }
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
    GLES20.glUniformMatrix4fv(shader.uMvpMatrixHandle, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(shader.uModelMatrixHandle, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(shader.uColorHandle, r, g, b, 1f)
    GLES20.glUniform1f(shader.uUnlitHandle, if (unlit) 1f else 0f)
    mesh.draw(shader)
  }
}
