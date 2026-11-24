package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.TruckAudioEngine
import com.example.data.CareerRepository
import com.example.model.ASSAM_LEVELS
import com.example.model.CameraMode
import com.example.model.DriveHudState
import com.example.model.GameSettings
import com.example.sim.TruckSimulationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TruckSimViewModel(
  private val repository: CareerRepository,
) : ViewModel() {

  val engine = TruckSimulationEngine()
  private val audioEngine = TruckAudioEngine()

  private val _uiState = MutableStateFlow(DriveHudState())
  val uiState: StateFlow<DriveHudState> = _uiState.asStateFlow()

  init {
    engine.onTelemetryTick = { snap ->
      val currentSettings = _uiState.value.settings
      audioEngine.updateState(
        speedKmh = snap.speedKmh,
        engineActive = engine.isRunning,
        soundEnabled = currentSettings.soundEnabled,
        volume = currentSettings.soundVolume,
      )
      _uiState.update { state ->
        state.copy(
          speedKmh = snap.speedKmh,
          fuelPercent = snap.fuelPercent,
          travelledKm = snap.travelledKm,
        )
      }
    }
    engine.onLevelCompleted = {
      handleLevelCompleted()
    }
    engine.onOutOfFuel = {
      syncAudioState()
      _uiState.update { it.copy(isOutOfFuel = true) }
    }

    viewModelScope.launch {
      repository.careerProgress.collect { progress ->
        engine.cameraDistance = progress.settings.cameraDistance
        engine.graphicsQuality = progress.settings.graphicsQuality
        _uiState.update { state ->
          val clampedSelected = state.selectedLevelIndex.coerceIn(0, progress.unlockedLevel - 1)
          state.copy(
            unlockedLevel = progress.unlockedLevel,
            money = progress.money,
            totalTripsCompleted = progress.totalTripsCompleted,
            totalKmDriven = progress.totalKmDriven,
            settings = progress.settings,
            selectedLevelIndex = if (state.isInMenu) clampedSelected else state.selectedLevelIndex,
          )
        }
        syncAudioState()
      }
    }
  }

  fun selectLevel(index: Int) {
    val state = _uiState.value
    if (index in ASSAM_LEVELS.indices && index + 1 <= state.unlockedLevel) {
      _uiState.update { it.copy(selectedLevelIndex = index) }
    }
  }

  fun startSelectedLevel() {
    val state = _uiState.value
    if (state.selectedLevelIndex + 1 > state.unlockedLevel) return
    val level = ASSAM_LEVELS[state.selectedLevelIndex]
    engine.resetForLevel(level.km)
    engine.isRunning = true
    if (state.settings.soundEnabled) {
      audioEngine.startIfNeeded()
    }
    _uiState.update {
      it.copy(
        isInMenu = false,
        isPaused = false,
        isSettingsOpen = false,
        isLevelComplete = false,
        isOutOfFuel = false,
        speedKmh = 0f,
        fuelPercent = 100f,
        travelledKm = 0f,
        lastRewardEarned = 0,
        newlyUnlockedLevel = null,
      )
    }
    syncAudioState()
  }

  fun pauseDriving() {
    if (_uiState.value.isInMenu || _uiState.value.isLevelComplete) return
    engine.isRunning = false
    engine.clearInputs()
    _uiState.update { it.copy(isPaused = true) }
    syncAudioState()
  }

  fun resumeDriving() {
    if (_uiState.value.isInMenu || _uiState.value.isLevelComplete || _uiState.value.isOutOfFuel) return
    engine.isRunning = true
    _uiState.update { it.copy(isPaused = false, isSettingsOpen = false) }
    syncAudioState()
  }

  fun returnToMenu() {
    engine.isRunning = false
    engine.clearInputs()
    _uiState.update {
      it.copy(
        isInMenu = true,
        isPaused = false,
        isSettingsOpen = false,
        isLevelComplete = false,
        isOutOfFuel = false,
      )
    }
    syncAudioState()
  }

  fun openSettings() {
    val wasRunning = engine.isRunning
    if (wasRunning) {
      engine.isRunning = false
      engine.clearInputs()
    }
    _uiState.update { it.copy(isSettingsOpen = true) }
    syncAudioState()
  }

  fun saveAndCloseSettings(newSettings: GameSettings) {
    engine.cameraDistance = newSettings.cameraDistance
    engine.graphicsQuality = newSettings.graphicsQuality
    val shouldResume = !_uiState.value.isInMenu &&
      !_uiState.value.isPaused &&
      !_uiState.value.isLevelComplete &&
      !_uiState.value.isOutOfFuel
    if (shouldResume) {
      engine.isRunning = true
    }
    if (newSettings.soundEnabled) {
      audioEngine.startIfNeeded()
    }
    _uiState.update { it.copy(settings = newSettings, isSettingsOpen = false) }
    syncAudioState()
    viewModelScope.launch {
      repository.saveSettings(newSettings)
    }
  }

  fun playHorn() {
    audioEngine.playHorn()
  }

  fun setLandscapeLocked(locked: Boolean) {
    _uiState.update { it.copy(isLandscapeLocked = locked) }
  }

  fun setAppForeground(foreground: Boolean) {
    audioEngine.setForeground(foreground)
  }

  fun cycleCameraMode() {
    val modes = CameraMode.entries
    val next = modes[(_uiState.value.cameraMode.ordinal + 1) % modes.size]
    engine.cameraMode = next
    _uiState.update { it.copy(cameraMode = next) }
  }

  fun startNextMission() {
    val state = _uiState.value
    val nextIndex = (state.selectedLevelIndex + 1).coerceAtMost(ASSAM_LEVELS.lastIndex)
    val nextLevel = ASSAM_LEVELS[nextIndex]
    engine.resetForLevel(nextLevel.km)
    engine.isRunning = true
    if (state.settings.soundEnabled) {
      audioEngine.startIfNeeded()
    }
    _uiState.update {
      it.copy(
        selectedLevelIndex = nextIndex,
        isInMenu = false,
        isPaused = false,
        isSettingsOpen = false,
        isLevelComplete = false,
        isOutOfFuel = false,
        speedKmh = 0f,
        fuelPercent = 100f,
        travelledKm = 0f,
      )
    }
    syncAudioState()
  }

  fun refuelOnRoadside(cost: Int = 250) {
    viewModelScope.launch {
      val paid = repository.spendMoney(cost)
      if (paid) {
        engine.refuelReserve()
        engine.isRunning = true
        _uiState.update { it.copy(isOutOfFuel = false) }
        syncAudioState()
      }
    }
  }

  private fun syncAudioState() {
    val s = _uiState.value.settings
    audioEngine.updateState(
      speedKmh = engine.speedKmh,
      engineActive = engine.isRunning,
      soundEnabled = s.soundEnabled,
      volume = s.soundVolume,
    )
  }

  private fun handleLevelCompleted() {
    val state = _uiState.value
    val level = state.currentLevel
    val previousUnlocked = state.unlockedLevel
    val candidateNextUnlocked = (level.index + 2).coerceAtMost(ASSAM_LEVELS.size)
    val newlyUnlocked = if (candidateNextUnlocked > previousUnlocked) candidateNextUnlocked else null

    syncAudioState()
    _uiState.update {
      it.copy(
        isLevelComplete = true,
        lastRewardEarned = level.reward,
        newlyUnlockedLevel = newlyUnlocked,
      )
    }

    viewModelScope.launch {
      repository.recordLevelCompletion(
        completedLevelIndex = level.index,
        rewardEarned = level.reward,
        kmDriven = level.km,
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.release()
  }

  class Factory(private val repository: CareerRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return TruckSimViewModel(repository) as T
    }
  }
}
