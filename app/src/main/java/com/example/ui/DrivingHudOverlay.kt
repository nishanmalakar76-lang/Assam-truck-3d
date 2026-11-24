package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ASSAM_LEVELS
import com.example.model.DriveHudState
import com.example.sim.TruckSimulationEngine
import com.example.ui.theme.AssamBrakeRed
import com.example.ui.theme.AssamFuelAmber
import com.example.ui.theme.AssamHudBorder
import com.example.ui.theme.AssamHudDark
import com.example.ui.theme.AssamIvory
import com.example.ui.theme.AssamMutedText
import com.example.ui.theme.AssamNavyDeep
import com.example.ui.theme.AssamNavySurface
import com.example.ui.theme.AssamSaffron
import com.example.ui.theme.AssamSaffronDarkText
import com.example.ui.theme.AssamSaffronDeep
import com.example.ui.theme.AssamTeaEmerald
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DrivingHudOverlay(
  state: DriveHudState,
  engine: TruckSimulationEngine,
  onPauseClick: () -> Unit,
  onResumeClick: () -> Unit,
  onRestartRoute: () -> Unit,
  onReturnToMenu: () -> Unit,
  onStartNextMission: () -> Unit,
  onCycleCamera: () -> Unit,
  onRefuel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val inrFormatter = remember { NumberFormat.getIntegerInstance(Locale("en", "IN")) }
  val level = state.currentLevel
  val fuelRounded = state.fuelPercent.roundToInt()
  val speedRounded = state.speedKmh.roundToInt()
  val progressFraction = (state.travelledKm / level.km.toFloat()).coerceIn(0f, 1f)

  // Safety cleanup if driving overlay leaves composition
  DisposableEffect(Unit) {
    onDispose { engine.clearInputs() }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    // Top Status Bar & Mission Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HudPill(
          icon = Icons.Rounded.AccountBalanceWallet,
          iconTint = AssamSaffron,
          text = "₹${inrFormatter.format(state.money)}",
          modifier = Modifier.testTag("hud_money_pill"),
        )
        HudPill(
          icon = Icons.Rounded.LocalGasStation,
          iconTint = if (fuelRounded < 20) AssamBrakeRed else AssamFuelAmber,
          text = "$fuelRounded%",
          modifier = Modifier.testTag("hud_fuel_pill"),
        )
        HudPill(
          icon = Icons.Rounded.Flag,
          iconTint = AssamTeaEmerald,
          text = "Lv ${level.index + 1}",
          modifier = Modifier.testTag("hud_level_pill"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        // Mission Progress Card
        Surface(
          modifier = Modifier
            .widthIn(max = 240.dp)
            .testTag("hud_mission_card"),
          shape = RoundedCornerShape(12.dp),
          color = AssamHudDark,
          border = BorderStroke(1.dp, AssamHudBorder),
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
              text = "MISSION: Reach ${level.to}",
              style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
              color = AssamSaffron,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Distance: ${String.format(Locale.US, "%.1f", state.travelledKm.coerceAtMost(level.km.toFloat()))} / ${level.km} km",
              style = MaterialTheme.typography.labelSmall,
              color = AssamIvory,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { progressFraction },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = AssamSaffron,
              trackColor = Color.White.copy(alpha = 0.2f),
            )
          }
        }

        // Camera & Pause Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = CircleShape,
            color = AssamHudDark,
            border = BorderStroke(1.dp, AssamHudBorder),
          ) {
            IconButton(
              onClick = onCycleCamera,
              modifier = Modifier
                .size(48.dp)
                .testTag("hud_camera_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = stringResource(R.string.cd_camera),
                tint = AssamIvory,
              )
            }
          }
          Surface(
            shape = CircleShape,
            color = AssamHudDark,
            border = BorderStroke(1.dp, AssamHudBorder),
          ) {
            IconButton(
              onClick = onPauseClick,
              modifier = Modifier
                .size(48.dp)
                .testTag("hud_pause_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = stringResource(R.string.cd_pause),
                tint = AssamIvory,
              )
            }
          }
        }
      }
    }

    // Bottom Driving Cockpit (Speedometer, Hint, Left/Right Steer, Brake/Gas Pedals)
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .widthIn(max = 860.dp)
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Speedometer Badge
      SpeedometerDial(
        speedKmh = speedRounded,
        modifier = Modifier.testTag("hud_speedometer"),
      )

      AnimatedVisibility(
        visible = state.travelledKm < 0.8f && state.speedKmh < 5f,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Surface(
          shape = RoundedCornerShape(9.dp),
          color = AssamHudDark,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        ) {
          Text(
            text = stringResource(R.string.control_hint),
            style = MaterialTheme.typography.labelSmall,
            color = AssamIvory,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        // Left Steering Cluster
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          HoldControlButton(
            width = 72.dp,
            height = 72.dp,
            activeColor = AssamSaffronDeep,
            contentDescription = stringResource(R.string.cd_steer_left),
            onHoldChange = { held -> engine.leftPressed.set(held) },
            modifier = Modifier.testTag("steer_left_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = null,
              tint = AssamIvory,
              modifier = Modifier.size(38.dp),
            )
          }

          HoldControlButton(
            width = 72.dp,
            height = 72.dp,
            activeColor = AssamSaffronDeep,
            contentDescription = stringResource(R.string.cd_steer_right),
            onHoldChange = { held -> engine.rightPressed.set(held) },
            modifier = Modifier.testTag("steer_right_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              contentDescription = null,
              tint = AssamIvory,
              modifier = Modifier.size(38.dp),
            )
          }
        }

        // Right Pedal Cluster (Brake & Gas)
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Bottom,
        ) {
          HoldControlButton(
            width = 68.dp,
            height = 78.dp,
            activeColor = AssamBrakeRed,
            contentDescription = stringResource(R.string.cd_brake),
            onHoldChange = { held -> engine.brakePressed.set(held) },
            modifier = Modifier.testTag("brake_pedal_button"),
          ) {
            Text(
              text = stringResource(R.string.btn_brake),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = AssamIvory,
            )
          }

          HoldControlButton(
            width = 72.dp,
            height = 92.dp,
            activeColor = AssamTeaEmerald,
            contentDescription = stringResource(R.string.cd_gas),
            onHoldChange = { held -> engine.gasPressed.set(held) },
            modifier = Modifier.testTag("gas_pedal_button"),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = AssamSaffron,
                modifier = Modifier.size(28.dp),
              )
              Text(
                text = stringResource(R.string.btn_gas),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = AssamIvory,
              )
            }
          }
        }
      }
    }

    // Pause Overlay Modal
    if (state.isPaused && !state.isLevelComplete) {
      OverlayModalCard(title = stringResource(R.string.paused_title)) {
        Text(
          text = "Route: ${level.from} → ${level.to} (${level.km} km)",
          style = MaterialTheme.typography.bodyMedium,
          color = AssamMutedText,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onResumeClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("resume_driving_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = AssamSaffron,
            contentColor = AssamSaffronDarkText,
          ),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.resume_driving), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onRestartRoute,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("restart_route_button"),
          border = BorderStroke(1.dp, AssamHudBorder),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, tint = AssamIvory)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            stringResource(R.string.restart_route),
            style = MaterialTheme.typography.labelLarge,
            color = AssamIvory,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onReturnToMenu,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("pause_to_menu_button"),
          border = BorderStroke(1.dp, AssamHudBorder),
        ) {
          Text(
            stringResource(R.string.back_to_routes),
            style = MaterialTheme.typography.labelLarge,
            color = AssamMutedText,
          )
        }
      }
    }

    // Out of Fuel Modal
    if (state.isOutOfFuel && !state.isLevelComplete) {
      OverlayModalCard(title = stringResource(R.string.out_of_fuel_title)) {
        Text(
          text = stringResource(R.string.out_of_fuel_desc),
          style = MaterialTheme.typography.bodyMedium,
          color = AssamMutedText,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onRefuel,
          enabled = state.money >= 250,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("refuel_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = AssamSaffron,
            contentColor = AssamSaffronDarkText,
          ),
        ) {
          Text(
            stringResource(R.string.refuel_now, 250),
            style = MaterialTheme.typography.labelLarge,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onRestartRoute,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          border = BorderStroke(1.dp, AssamHudBorder),
        ) {
          Text(
            stringResource(R.string.restart_route),
            style = MaterialTheme.typography.labelLarge,
            color = AssamIvory,
          )
        }
      }
    }

    // Mission Complete Modal
    if (state.isLevelComplete) {
      val hasNext = state.selectedLevelIndex < ASSAM_LEVELS.lastIndex
      OverlayModalCard(
        title = if (hasNext) {
          stringResource(R.string.mission_complete_title, level.to.uppercase(Locale.US))
        } else {
          stringResource(R.string.mission_complete_all)
        },
        accentIcon = Icons.Rounded.EmojiEvents,
      ) {
        Text(
          text = "${level.from} → ${level.to} (${level.km} km)",
          style = MaterialTheme.typography.titleMedium,
          color = AssamIvory,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AssamTeaEmerald.copy(alpha = 0.25f),
          border = BorderStroke(1.dp, AssamTeaEmerald),
        ) {
          Text(
            text = "+ ₹${inrFormatter.format(state.lastRewardEarned)} Earned",
            style = MaterialTheme.typography.titleMedium,
            color = AssamSaffron,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        if (state.newlyUnlockedLevel != null) {
          Spacer(modifier = Modifier.height(8.dp))
          val unlockedRoute = ASSAM_LEVELS[state.newlyUnlockedLevel - 1]
          Text(
            text = "Unlocked Level ${state.newlyUnlockedLevel}: ${unlockedRoute.from} → ${unlockedRoute.to}",
            style = MaterialTheme.typography.bodyMedium,
            color = AssamMutedText,
            textAlign = TextAlign.Center,
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (hasNext) {
          Button(
            onClick = onStartNextMission,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("start_next_mission_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = AssamSaffron,
              contentColor = AssamSaffronDarkText,
            ),
          ) {
            Text(
              stringResource(R.string.start_next_mission),
              style = MaterialTheme.typography.labelLarge,
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
          onClick = onReturnToMenu,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("complete_to_menu_button"),
          border = BorderStroke(1.dp, AssamHudBorder),
        ) {
          Text(
            stringResource(R.string.back_to_routes),
            style = MaterialTheme.typography.labelLarge,
            color = AssamIvory,
          )
        }
      }
    }
  }
}

@Composable
private fun HudPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  text: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = AssamHudDark,
    border = BorderStroke(1.dp, AssamHudBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
        color = AssamIvory,
      )
    }
  }
}

@Composable
private fun SpeedometerDial(
  speedKmh: Int,
  modifier: Modifier = Modifier,
) {
  val sweep = (speedKmh / 95f).coerceIn(0f, 1f) * 240f
  Surface(
    modifier = modifier.size(78.dp),
    shape = CircleShape,
    color = AssamHudDark,
    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.55f)),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        drawArc(
          color = Color.White.copy(alpha = 0.16f),
          startAngle = 150f,
          sweepAngle = 240f,
          useCenter = false,
          style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
          color = if (speedKmh > 78) AssamFuelAmber else AssamSaffron,
          startAngle = 150f,
          sweepAngle = sweep,
          useCenter = false,
          style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "$speedKmh",
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
          ),
          color = AssamIvory,
        )
        Text(
          text = "KM/H",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = AssamMutedText,
        )
      }
    }
  }
}

@Composable
private fun HoldControlButton(
  width: Dp,
  height: Dp,
  activeColor: Color,
  contentDescription: String,
  onHoldChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  var isPressed by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose {
      if (isPressed) {
        onHoldChange(false)
      }
    }
  }

  Surface(
    modifier = modifier
      .size(width = width, height = height)
      .semantics {
        this.contentDescription = contentDescription
        this.role = Role.Button
      }
      .pointerInput(Unit) {
        awaitEachGesture {
          awaitFirstDown(requireUnconsumed = false)
          isPressed = true
          onHoldChange(true)
          try {
            waitForUpOrCancellation()
          } finally {
            isPressed = false
            onHoldChange(false)
          }
        }
      },
    shape = RoundedCornerShape(18.dp),
    color = if (isPressed) activeColor else AssamHudDark,
    border = BorderStroke(
      width = 2.dp,
      color = if (isPressed) AssamIvory else Color.White.copy(alpha = 0.65f),
    ),
    shadowElevation = 6.dp,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxSize(),
    ) {
      content()
    }
  }
}

@Composable
private fun OverlayModalCard(
  title: String,
  accentIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(AssamNavyDeep.copy(alpha = 0.72f))
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = AssamNavySurface,
      border = BorderStroke(1.dp, AssamHudBorder),
      tonalElevation = 10.dp,
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (accentIcon != null) {
          Icon(
            imageVector = accentIcon,
            contentDescription = null,
            tint = AssamSaffron,
            modifier = Modifier.size(44.dp),
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = AssamSaffron,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
      }
    }
  }
}
