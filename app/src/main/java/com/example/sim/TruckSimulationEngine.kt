package com.example.sim

import com.example.model.CameraMode
import com.example.model.GraphicsQuality
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

data class TrafficCarSnapshot(
  val x: Float,
  val z: Float,
  val r: Float,
  val g: Float,
  val b: Float,
)

data class TelemetrySnapshot(
  val speedKmh: Float,
  val fuelPercent: Float,
  val travelledKm: Float,
  val truckX: Float,
  val steerInput: Float,
  val worldScrollZ: Float,
  val wheelSpinRadians: Float,
  val isBraking: Boolean,
)

/**
 * Thread-safe physics, traffic, and railway simulation engine shared between Compose input
 * controls, ViewModel telemetry, and the 60fps OpenGL ES 2.0 render thread.
 */
class TruckSimulationEngine {

  val gasPressed = AtomicBoolean(false)
  val brakePressed = AtomicBoolean(false)
  val leftPressed = AtomicBoolean(false)
  val rightPressed = AtomicBoolean(false)

  @Volatile var isRunning: Boolean = false
  @Volatile var targetDistanceKm: Float = 25f
  @Volatile var cameraMode: CameraMode = CameraMode.CHASE
  @Volatile var cameraDistance: Float = 13f
  @Volatile var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH

  @Volatile var speedKmh: Float = 0f
    private set
  @Volatile var fuelPercent: Float = 100f
    private set
  @Volatile var travelledKm: Float = 0f
    private set
  @Volatile var truckX: Float = 0f
    private set
  @Volatile var steerInput: Float = 0f
    private set
  @Volatile var worldScrollZ: Float = 0f
    private set
  @Volatile var wheelSpinRadians: Float = 0f
    private set
  @Volatile var cameraX: Float = 0f
    private set
  @Volatile var trainZ: Float = 45f
    private set

  private class MutableTrafficCar(
    val x: Float,
    var z: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val dirSign: Float,
  )

  private val traffic = listOf(
    MutableTrafficCar(-2.8f, -35f, 0.784f, 0.196f, 0.196f, 1f),  // Red 0xC83232
    MutableTrafficCar(2.8f, -95f, 0.945f, 0.945f, 0.945f, -1f),  // White 0xF1F1F1
    MutableTrafficCar(-2.8f, -155f, 0.153f, 0.459f, 0.788f, 1f), // Blue 0x2775C9
    MutableTrafficCar(2.8f, -15f, 0.839f, 0.647f, 0.169f, -1f),  // Gold 0xD6A52B
  )

  private val trafficRenderBuffer = Array(traffic.size) {
    TrafficCarSnapshot(traffic[it].x, traffic[it].z, traffic[it].r, traffic[it].g, traffic[it].b)
  }

  private val stateLock = Any()
  private var lastHudEmitNs: Long = 0L

  var onTelemetryTick: ((TelemetrySnapshot) -> Unit)? = null
  var onLevelCompleted: (() -> Unit)? = null
  var onOutOfFuel: (() -> Unit)? = null

  private var completionFired = false
  private var outOfFuelFired = false

  fun copyTrafficSnapshot(out: Array<TrafficCarSnapshot>) {
    synchronized(stateLock) {
      for (i in traffic.indices) {
        val c = traffic[i]
        out[i] = TrafficCarSnapshot(c.x, c.z, c.r, c.g, c.b)
      }
    }
  }

  fun trafficCount(): Int = traffic.size

  fun resetForLevel(distanceKm: Int) {
    synchronized(stateLock) {
      targetDistanceKm = distanceKm.toFloat()
      speedKmh = 0f
      fuelPercent = 100f
      travelledKm = 0f
      truckX = 0f
      cameraX = 0f
      steerInput = 0f
      worldScrollZ = 0f
      wheelSpinRadians = 0f
      traffic[0].z = -35f
      traffic[1].z = -95f
      traffic[2].z = -155f
      traffic[3].z = -15f
      trainZ = 65f
      completionFired = false
      outOfFuelFired = false
      clearInputs()
    }
    emitTelemetry(force = true)
  }

  fun refuelReserve() {
    synchronized(stateLock) {
      fuelPercent = 60f
      outOfFuelFired = false
      clearInputs()
    }
    emitTelemetry(force = true)
  }

  fun clearInputs() {
    gasPressed.set(false)
    brakePressed.set(false)
    leftPressed.set(false)
    rightPressed.set(false)
  }

  fun step(dtSeconds: Float) {
    val dt = min(0.05f, max(0.001f, dtSeconds))
    var fireComplete = false
    var fireOutOfFuel = false

    synchronized(stateLock) {
      if (!isRunning) {
        cameraX += (truckX * 0.45f - cameraX) * min(1f, dt * 3f)
        return
      }

      val gas = gasPressed.get()
      val brake = brakePressed.get()
      val left = leftPressed.get()
      val right = rightPressed.get()

      speedKmh = if (gas && fuelPercent > 0f) {
        min(95f, speedKmh + 34f * dt)
      } else {
        max(0f, speedKmh - 9f * dt)
      }

      if (brake) {
        speedKmh = max(0f, speedKmh - 55f * dt)
      }

      val targetSteer = (if (right) 1f else 0f) - (if (left) 1f else 0f)
      steerInput += (targetSteer - steerInput) * min(1f, dt * 10f)

      truckX += steerInput * speedKmh * 0.018f * dt * 12f
      truckX = truckX.coerceIn(-4.3f, 4.3f)

      val worldDelta = speedKmh * 0.55f * dt
      worldScrollZ = (worldScrollZ + worldDelta) % 240f
      wheelSpinRadians = (wheelSpinRadians + worldDelta * 1.4f) % (2f * PI.toFloat())

      // Update traffic cars relative to truck's moving highway frame
      for (i in traffic.indices) {
        val car = traffic[i]
        car.z += worldDelta + car.dirSign * (3f + speedKmh * 0.025f) * dt
        if (car.z > 45f) {
          car.z = -170f - i * 18f
        } else if (car.z < -190f) {
          car.z = 45f + i * 15f
        }
      }

      // Update parallel railway train moving along -Z
      trainZ += worldDelta - 7f * dt
      if (trainZ < -230f) {
        trainZ = 230f
      } else if (trainZ > 235f) {
        trainZ = -225f
      }

      travelledKm += (speedKmh / 3600f) * dt * 18f
      fuelPercent = max(0f, fuelPercent - speedKmh * 0.0028f * dt)
      cameraX += (truckX * 0.45f - cameraX) * min(1f, dt * 3.5f)

      if (travelledKm >= targetDistanceKm && !completionFired) {
        travelledKm = targetDistanceKm
        completionFired = true
        isRunning = false
        speedKmh = 0f
        clearInputs()
        fireComplete = true
      } else if (fuelPercent <= 0f && speedKmh <= 0.5f && !outOfFuelFired && !completionFired) {
        outOfFuelFired = true
        isRunning = false
        speedKmh = 0f
        clearInputs()
        fireOutOfFuel = true
      }
    }

    if (fireComplete) {
      emitTelemetry(force = true)
      onLevelCompleted?.invoke()
    } else if (fireOutOfFuel) {
      emitTelemetry(force = true)
      onOutOfFuel?.invoke()
    } else {
      emitTelemetry(force = false)
    }
  }

  private fun emitTelemetry(force: Boolean) {
    val now = System.nanoTime()
    if (!force && now - lastHudEmitNs < 50_000_000L) return
    lastHudEmitNs = now
    val snap = TelemetrySnapshot(
      speedKmh = speedKmh,
      fuelPercent = fuelPercent,
      travelledKm = travelledKm,
      truckX = truckX,
      steerInput = steerInput,
      worldScrollZ = worldScrollZ,
      wheelSpinRadians = wheelSpinRadians,
      isBraking = brakePressed.get(),
    )
    onTelemetryTick?.invoke(snap)
  }
}
