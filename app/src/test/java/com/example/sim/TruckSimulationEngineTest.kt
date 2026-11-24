package com.example.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TruckSimulationEngineTest {

  @Test
  fun gasAcceleratesUpToTopSpeedAndUpdatesDistance() {
    val engine = TruckSimulationEngine()
    engine.resetForLevel(distanceKm = 25)
    engine.isRunning = true
    engine.gasPressed.set(true)

    repeat(120) {
      engine.step(0.03f)
    }

    assertTrue("Expected speed near top speed", engine.speedKmh > 80f)
    assertTrue("Expected top speed cap <= 95", engine.speedKmh <= 95f)
    assertTrue("Expected positive distance travelled", engine.travelledKm > 0f)
    assertTrue("Expected fuel to decrease slightly", engine.fuelPercent < 100f)
  }

  @Test
  fun steeringStaysWithinHighwayBounds() {
    val engine = TruckSimulationEngine()
    engine.resetForLevel(distanceKm = 25)
    engine.isRunning = true
    engine.gasPressed.set(true)
    engine.rightPressed.set(true)

    repeat(150) {
      engine.step(0.03f)
    }
    assertEquals(4.3f, engine.truckX, 0.001f)

    engine.rightPressed.set(false)
    engine.leftPressed.set(true)
    repeat(300) {
      engine.step(0.03f)
    }
    assertEquals(-4.3f, engine.truckX, 0.001f)
  }

  @Test
  fun reachingTargetDistanceCompletesLevelAndStopsTruck() {
    val engine = TruckSimulationEngine()
    var completed = false
    engine.onLevelCompleted = { completed = true }
    engine.resetForLevel(distanceKm = 1)
    engine.isRunning = true
    engine.gasPressed.set(true)

    repeat(300) {
      if (!completed) engine.step(0.04f)
    }

    assertTrue("Expected level completion callback", completed)
    assertFalse("Expected simulation paused on completion", engine.isRunning)
    assertEquals(1f, engine.travelledKm, 0.001f)
  }
}
