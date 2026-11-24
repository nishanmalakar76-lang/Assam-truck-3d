package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CareerRepository
import com.example.gl.AssamGlSurfaceView
import com.example.ui.DrivingHudOverlay
import com.example.ui.MenuScreen
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

        // System Back navigation when driving or inside pause/completion modals
        BackHandler(enabled = !uiState.isInMenu) {
          when {
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
                else -> false
              }
            }
        ) {
          // 3D OpenGL ES Assam Highway Scene
          AssamGlSurfaceView(
            engine = viewModel.engine,
            modifier = Modifier.fillMaxSize(),
          )

          if (uiState.isInMenu) {
            MenuScreen(
              state = uiState,
              onSelectLevel = viewModel::selectLevel,
              onStartDriving = viewModel::startSelectedLevel,
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
              onRefuel = { viewModel.refuelOnRoadside() },
            )
          }
        }
      }
    }
  }
}
