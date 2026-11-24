package com.example.model

data class HighwayLevel(
  val index: Int,
  val from: String,
  val to: String,
  val km: Int,
  val reward: Int,
  val sceneryNote: String,
)

val ASSAM_LEVELS = listOf(
  HighwayLevel(0, "Rajgarh", "Dibrugarh", 25, 1200, "Upper Assam Tea Belt"),
  HighwayLevel(1, "Dibrugarh", "Tinsukia", 35, 1500, "NH-15 Tea Estates"),
  HighwayLevel(2, "Tinsukia", "Makum", 12, 800, "Timber & Tea Corridor"),
  HighwayLevel(3, "Makum", "Margherita", 45, 2000, "Patkai Foothills Highway"),
  HighwayLevel(4, "Margherita", "Digboi", 20, 1100, "Historic Oil Town Road"),
  HighwayLevel(5, "Dibrugarh", "Sivasagar", 75, 2500, "Ahom Heritage Stretch"),
  HighwayLevel(6, "Sivasagar", "Jorhat", 55, 2200, "Brahmaputra Valley Run"),
  HighwayLevel(7, "Jorhat", "Golaghat", 50, 2200, "Kaziranga Approach Road"),
  HighwayLevel(8, "Golaghat", "Nagaon", 90, 3000, "Central Assam Express"),
  HighwayLevel(9, "Nagaon", "Guwahati", 125, 4000, "Gateway to the Northeast"),
)

enum class CameraMode(val label: String) {
  CHASE("Chase Cam"),
  CLOSE("Close Cam"),
  HIGH("Overhead Cam"),
}

enum class GraphicsQuality(val label: String, val scale: Float) {
  HIGH("High", 1.0f),
  MEDIUM("Medium", 0.7f),
  LOW("Low", 0.45f),
}

data class GameSettings(
  val soundEnabled: Boolean = true,
  val soundVolume: Float = 0.55f,
  val cameraDistance: Float = 13f,
  val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
)

data class CareerProgress(
  val unlockedLevel: Int = 1,
  val money: Int = 12500,
  val totalTripsCompleted: Int = 0,
  val totalKmDriven: Double = 0.0,
  val settings: GameSettings = GameSettings(),
)

data class DriveHudState(
  val isInMenu: Boolean = true,
  val isPaused: Boolean = false,
  val isSettingsOpen: Boolean = false,
  val isLandscapeLocked: Boolean = false,
  val isLevelComplete: Boolean = false,
  val isOutOfFuel: Boolean = false,
  val selectedLevelIndex: Int = 0,
  val unlockedLevel: Int = 1,
  val money: Int = 12500,
  val speedKmh: Float = 0f,
  val fuelPercent: Float = 100f,
  val travelledKm: Float = 0f,
  val cameraMode: CameraMode = CameraMode.CHASE,
  val settings: GameSettings = GameSettings(),
  val lastRewardEarned: Int = 0,
  val newlyUnlockedLevel: Int? = null,
  val totalTripsCompleted: Int = 0,
  val totalKmDriven: Double = 0.0,
) {
  val currentLevel: HighwayLevel
    get() = ASSAM_LEVELS[selectedLevelIndex.coerceIn(0, ASSAM_LEVELS.lastIndex)]
}
