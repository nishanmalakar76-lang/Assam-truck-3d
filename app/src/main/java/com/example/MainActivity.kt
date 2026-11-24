package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CareerRepository
import com.example.gl.AssamGlSurfaceView
import com.example.ui.DrivingHudOverlay
import com.example.ui.MenuScreen
import com.example.ui.SettingsModal
import com.example.ui.theme.AssamTruckTheme
import com.example.viewmodel.TruckSimViewModel

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = CareerRepository(applicationContext)

    setContent {
      AssamTruckTheme {
        val viewModel: TruckSimViewModel = viewModel(
          factory = TruckSimViewModel.Factory(repository)
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            when (event) {
              Lifecycle.Event.ON_RESUME -> viewModel.setAppForeground(true)
              Lifecycle.Event.ON_PAUSE -> viewModel.setAppForeground(false)
              else -> Unit
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        BackHandler(enabled = !uiState.isInMenu || uiState.isSettingsOpen) {
          when {
            uiState.isSettingsOpen -> viewModel.saveAndCloseSettings(uiState.settings)
            uiState.isLevelComplete || uiState.isOutOfFuel -> viewModel.returnToMenu()
            uiState.isPaused -> viewModel.resumeDriving()
            else -> viewModel.pauseDriving()
          }
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
              val isDown = when (keyEvent.type) {
                KeyEventType.KeyDown -> true
                KeyEventType.KeyUp -> false
                else -> return@onPreviewKeyEvent false
              }
              when (keyEvent.key) {
                Key.DirectionUp, Key.W -> {
                  viewModel.engine.gasPressed.set(isDown)
                  true
                }
                Key.DirectionDown, Key.S -> {
                  viewModel.engine.brakePressed.set(isDown)
                  true
                }
                Key.DirectionLeft, Key.A -> {
                  viewModel.engine.leftPressed.set(isDown)
                  true
                }
                Key.DirectionRight, Key.D -> {
                  viewModel.engine.rightPressed.set(isDown)
                  true
                }
                Key.H -> {
                  if (isDown) viewModel.playHorn()
                  true
                }
                else -> false
              }
            }
        ) {
          AssamGlSurfaceView(
            engine = viewModel.engine,
            modifier = Modifier.fillMaxSize(),
          )

          if (uiState.isInMenu) {
            MenuScreen(
              state = uiState,
              onSelectLevel = viewModel::selectLevel,
              onStartDriving = viewModel::startSelectedLevel,
              onOpenSettings = viewModel::openSettings,
            )
          } else {
            DrivingHudOverlay(
              state = uiState,
              engine = viewModel.engine,
              onPauseClick = viewModel::pauseDriving,
              onResumeClick = viewModel::resumeDriving,
              onRestartRoute = viewModel::startSelectedLevel,
              onReturnToMenu = viewModel::returnToMenu,
              onStartNextMission = viewModel::startNextMission,
              onCycleCamera = viewModel::cycleCameraMode,
              onOpenSettings = viewModel::openSettings,
              onToggleRotate = {
                val lockLandscape = !uiState.isLandscapeLocked
                requestedOrientation = if (lockLandscape) {
                  ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                  ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                viewModel.setLandscapeLocked(lockLandscape)
              },
              onHornClick = viewModel::playHorn,
              onRefuel = { viewModel.refuelOnRoadside() },
            )
          }

          if (uiState.isSettingsOpen) {
            SettingsModal(
              initialSettings = uiState.settings,
              onSaveAndClose = viewModel::saveAndCloseSettings,
            )
          }
        }
      }
    }
  }
}
