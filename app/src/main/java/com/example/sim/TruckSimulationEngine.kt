package com.example.sim

import com.example.model.CameraMode
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

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
 * Thread-safe physics engine shared between Compose input controls, ViewModel telemetry,
 * and the 60fps OpenGL ES 2.0 render thread.
 */
class TruckSimulationEngine {

  val gasPressed = AtomicBoolean(false)
  val brakePressed = AtomicBoolean(false)
  val leftPressed = AtomicBoolean(false)
  val rightPressed = AtomicBoolean(false)

  @Volatile var isRunning: Boolean = false
  @Volatile var targetDistanceKm: Float = 25f
  @Volatile var cameraMode: CameraMode = CameraMode.CHASE

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

  private val stateLock = Any()
  private var lastHudEmitNs: Long = 0L

  var onTelemetryTick: ((TelemetrySnapshot) -> Unit)? = null
  var onLevelCompleted: (() -> Unit)? = null
  var onOutOfFuel: (() -> Unit)? = null

  private var completionFired = false
  private var outOfFuelFired = false

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
        // Gentle idle camera centering in menu/pause
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

      // Steer left moves truck toward -X (screen left), steer right toward +X (screen right)
      val targetSteer = (if (right) 1f else 0f) - (if (left) 1f else 0f)
      steerInput += (targetSteer - steerInput) * min(1f, dt * 10f)

      truckX += steerInput * speedKmh * 0.018f * dt * 12f
      truckX = truckX.coerceIn(-4.3f, 4.3f)

      // World scrolls backward (+Z toward camera) as truck drives forward (-Z)
      val worldDelta = speedKmh * 0.55f * dt
      worldScrollZ = (worldScrollZ + worldDelta) % 240f
      wheelSpinRadians = (wheelSpinRadians + worldDelta * 1.4f) % (2f * PI.toFloat())

      // Arcade distance scale matching the prototype (25 km ~ 45s at top speed)
      travelledKm += (speedKmh / 3600f) * dt * 18f

      // Fuel consumption tuned so 100% tank comfortably finishes longest 125km route
      fuelPercent = max(0f, fuelPercent - speedKmh * 0.0028f * dt)

      // Smooth chase camera lateral tracking
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
    if (!force && now - lastHudEmitNs < 50_000_000L) return // ~20 Hz HUD refresh
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
