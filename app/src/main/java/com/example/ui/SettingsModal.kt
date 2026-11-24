package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.theme.AssamHudBorder
import com.example.ui.theme.AssamIvory
import com.example.ui.theme.AssamMutedText
import com.example.ui.theme.AssamNavyCard
import com.example.ui.theme.AssamNavyDeep
import com.example.ui.theme.AssamNavySurface
import com.example.ui.theme.AssamSaffron
import com.example.ui.theme.AssamSaffronDarkText
import com.example.ui.theme.AssamTeaEmerald
import kotlin.math.roundToInt

@Composable
fun SettingsModal(
  initialSettings: GameSettings,
  onSaveAndClose: (GameSettings) -> Unit,
  modifier: Modifier = Modifier,
) {
  var soundEnabled by remember(initialSettings) { mutableStateOf(initialSettings.soundEnabled) }
  var volume by remember(initialSettings) { mutableFloatStateOf(initialSettings.soundVolume) }
  var cameraDist by remember(initialSettings) { mutableFloatStateOf(initialSettings.cameraDistance) }
  var quality by remember(initialSettings) { mutableStateOf(initialSettings.graphicsQuality) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(AssamNavyDeep.copy(alpha = 0.82f))
      .padding(18.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .widthIn(max = 460.dp)
        .fillMaxWidth()
        .testTag("settings_modal"),
      shape = RoundedCornerShape(20.dp),
      color = AssamNavySurface,
      border = BorderStroke(1.dp, AssamHudBorder),
      tonalElevation = 12.dp,
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      ) {
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.headlineSmall,
          color = AssamSaffron,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Sound Effects Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.setting_sound),
            style = MaterialTheme.typography.bodyLarge,
            color = AssamIvory,
          )
          Switch(
            checked = soundEnabled,
            onCheckedChange = { soundEnabled = it },
            modifier = Modifier.testTag("settings_sound_switch"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = AssamSaffronDarkText,
              checkedTrackColor = AssamSaffron,
            ),
          )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

        // Sound Volume Slider
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.setting_volume),
              style = MaterialTheme.typography.bodyLarge,
              color = AssamIvory,
            )
            Text(
              text = "${(volume * 100f).roundToInt()}%",
              style = MaterialTheme.typography.labelLarge,
              color = AssamSaffron,
            )
          }
          Slider(
            value = volume,
            onValueChange = { volume = it },
            valueRange = 0f..1f,
            enabled = soundEnabled,
            modifier = Modifier.testTag("settings_volume_slider"),
            colors = SliderDefaults.colors(
              thumbColor = AssamSaffron,
              activeTrackColor = AssamSaffron,
            ),
          )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

        // Camera Distance Slider
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.setting_camera),
              style = MaterialTheme.typography.bodyLarge,
              color = AssamIvory,
            )
            Text(
              text = "${cameraDist.roundToInt()} m",
              style = MaterialTheme.typography.labelLarge,
              color = AssamSaffron,
            )
          }
          Slider(
            value = cameraDist,
            onValueChange = { cameraDist = it },
            valueRange = 9f..24f,
            modifier = Modifier.testTag("settings_camera_slider"),
            colors = SliderDefaults.colors(
              thumbColor = AssamSaffron,
              activeTrackColor = AssamSaffron,
            ),
          )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

        // Graphics Quality Selector
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
          Text(
            text = stringResource(R.string.setting_quality),
            style = MaterialTheme.typography.bodyLarge,
            color = AssamIvory,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GraphicsQuality.entries.forEach { option ->
              val selected = quality == option
              FilterChip(
                selected = selected,
                onClick = { quality = option },
                label = {
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                  )
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("settings_quality_${option.name.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AssamTeaEmerald,
                  selectedLabelColor = AssamIvory,
                  containerColor = AssamNavyCard,
                  labelColor = AssamMutedText,
                ),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = {
            onSaveAndClose(
              GameSettings(
                soundEnabled = soundEnabled,
                soundVolume = volume,
                cameraDistance = cameraDist,
                graphicsQuality = quality,
              )
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("settings_save_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = AssamSaffron,
            contentColor = AssamSaffronDarkText,
          ),
        ) {
          Text(
            text = stringResource(R.string.save_and_close),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
