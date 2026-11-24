package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ASSAM_LEVELS
import com.example.model.CareerProgress
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.careerDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "assam_truck_career"
)

class CareerRepository(private val context: Context) {

  private object Keys {
    val UNLOCKED_LEVEL = intPreferencesKey("assam_unlocked_level")
    val MONEY = intPreferencesKey("assam_money")
    val TRIPS_COMPLETED = intPreferencesKey("assam_trips_completed")
    val TOTAL_KM = doublePreferencesKey("assam_total_km")
    val SOUND_ENABLED = booleanPreferencesKey("assam_sound_enabled")
    val SOUND_VOLUME = floatPreferencesKey("assam_sound_volume")
    val CAMERA_DISTANCE = floatPreferencesKey("assam_camera_distance")
    val GRAPHICS_QUALITY = stringPreferencesKey("assam_graphics_quality")
  }

  val careerProgress: Flow<CareerProgress> = context.careerDataStore.data.map { prefs ->
    val qualityName = prefs[Keys.GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name
    val quality = GraphicsQuality.entries.firstOrNull { it.name == qualityName }
      ?: GraphicsQuality.HIGH

    CareerProgress(
      unlockedLevel = (prefs[Keys.UNLOCKED_LEVEL] ?: 1).coerceIn(1, ASSAM_LEVELS.size),
      money = prefs[Keys.MONEY] ?: 12500,
      totalTripsCompleted = prefs[Keys.TRIPS_COMPLETED] ?: 0,
      totalKmDriven = prefs[Keys.TOTAL_KM] ?: 0.0,
      settings = GameSettings(
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        soundVolume = (prefs[Keys.SOUND_VOLUME] ?: 0.55f).coerceIn(0f, 1f),
        cameraDistance = (prefs[Keys.CAMERA_DISTANCE] ?: 13f).coerceIn(9f, 24f),
        graphicsQuality = quality,
      ),
    )
  }

  suspend fun recordLevelCompletion(
    completedLevelIndex: Int,
    rewardEarned: Int,
    kmDriven: Int,
  ) {
    context.careerDataStore.edit { prefs ->
      val currentUnlocked = (prefs[Keys.UNLOCKED_LEVEL] ?: 1).coerceIn(1, ASSAM_LEVELS.size)
      val nextUnlocked = (completedLevelIndex + 2).coerceIn(currentUnlocked, ASSAM_LEVELS.size)
      val currentMoney = prefs[Keys.MONEY] ?: 12500
      val currentTrips = prefs[Keys.TRIPS_COMPLETED] ?: 0
      val currentKm = prefs[Keys.TOTAL_KM] ?: 0.0

      prefs[Keys.UNLOCKED_LEVEL] = nextUnlocked
      prefs[Keys.MONEY] = currentMoney + rewardEarned
      prefs[Keys.TRIPS_COMPLETED] = currentTrips + 1
      prefs[Keys.TOTAL_KM] = currentKm + kmDriven.toDouble()
    }
  }

  suspend fun spendMoney(amount: Int): Boolean {
    var succeeded = false
    context.careerDataStore.edit { prefs ->
      val currentMoney = prefs[Keys.MONEY] ?: 12500
      if (currentMoney >= amount) {
        prefs[Keys.MONEY] = currentMoney - amount
        succeeded = true
      }
    }
    return succeeded
  }

  suspend fun saveSettings(settings: GameSettings) {
    context.careerDataStore.edit { prefs ->
      prefs[Keys.SOUND_ENABLED] = settings.soundEnabled
      prefs[Keys.SOUND_VOLUME] = settings.soundVolume.coerceIn(0f, 1f)
      prefs[Keys.CAMERA_DISTANCE] = settings.cameraDistance.coerceIn(9f, 24f)
      prefs[Keys.GRAPHICS_QUALITY] = settings.graphicsQuality.name
    }
  }
}
