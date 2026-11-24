package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ASSAM_LEVELS
import com.example.model.DriveHudState
import com.example.model.HighwayLevel
import com.example.ui.theme.AssamHudBorder
import com.example.ui.theme.AssamIvory
import com.example.ui.theme.AssamMutedText
import com.example.ui.theme.AssamNavyCard
import com.example.ui.theme.AssamNavyDeep
import com.example.ui.theme.AssamNavySurface
import com.example.ui.theme.AssamSaffron
import com.example.ui.theme.AssamSaffronDarkText
import com.example.ui.theme.AssamTeaEmerald
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MenuScreen(
  state: DriveHudState,
  onSelectLevel: (Int) -> Unit,
  onStartDriving: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val inrFormatter = NumberFormat.getIntegerInstance(Locale("en", "IN"))

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            AssamNavyDeep.copy(alpha = 0.86f),
            AssamNavyDeep.copy(alpha = 0.94f),
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .padding(horizontal = 14.dp, vertical = 10.dp),
      shape = RoundedCornerShape(22.dp),
      color = AssamNavySurface.copy(alpha = 0.96f),
      border = BorderStroke(1.dp, AssamHudBorder),
      tonalElevation = 8.dp,
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Hero Banner Header
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_assam_highway_hero),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    AssamNavyDeep.copy(alpha = 0.25f),
                    AssamNavySurface.copy(alpha = 0.94f),
                  )
                )
              )
          )

          // Top Settings Icon
          Surface(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp),
            shape = CircleShape,
            color = AssamNavyDeep.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, AssamHudBorder),
          ) {
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(48.dp)
                .testTag("menu_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = AssamIvory,
              )
            }
          }

          // Top Wallet Badge
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp)
              .testTag("menu_wallet_pill"),
            shape = RoundedCornerShape(12.dp),
            color = AssamNavyDeep.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, AssamSaffron.copy(alpha = 0.6f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = AssamSaffron,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "₹${inrFormatter.format(state.money)}",
                style = MaterialTheme.typography.titleMedium,
                color = AssamIvory,
              )
            }
          }

          // Title & Subtitle
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(horizontal = 18.dp, vertical = 12.dp)
          ) {
            Text(
              text = stringResource(R.string.menu_title),
              style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
              ),
              color = AssamSaffron,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = stringResource(R.string.menu_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = AssamMutedText,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Career Stats Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CareerStatChip(
            icon = Icons.Rounded.Route,
            label = "Routes ${state.unlockedLevel}/${ASSAM_LEVELS.size}",
            modifier = Modifier.weight(1f),
          )
          CareerStatChip(
            icon = Icons.Rounded.LocalShipping,
            label = "${state.totalTripsCompleted} Trips",
            modifier = Modifier.weight(1f),
          )
        }

        // Scrollable Assam Highway Route List
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(ASSAM_LEVELS, key = { it.index }) { level ->
            val isLocked = level.index + 1 > state.unlockedLevel
            val isSelected = level.index == state.selectedLevelIndex
            LevelRouteCard(
              level = level,
              isLocked = isLocked,
              isSelected = isSelected,
              inrFormatter = inrFormatter,
              onClick = { onSelectLevel(level.index) },
            )
          }
        }

        // Bottom Primary CTA & Disclaimer
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(AssamNavySurface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Button(
            onClick = onStartDriving,
            enabled = state.selectedLevelIndex + 1 <= state.unlockedLevel,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("start_driving_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AssamSaffron,
              contentColor = AssamSaffronDarkText,
            ),
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.start_driving) +
                " • ${state.currentLevel.from} → ${state.currentLevel.to}",
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.menu_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = AssamMutedText.copy(alpha = 0.75f),
          )
        }
      }
    }
  }
}

@Composable
private fun CareerStatChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = AssamNavyCard.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = AssamSaffron,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = AssamIvory,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun LevelRouteCard(
  level: HighwayLevel,
  isLocked: Boolean,
  isSelected: Boolean,
  inrFormatter: NumberFormat,
  onClick: () -> Unit,
) {
  val borderColor = when {
    isSelected -> AssamSaffron
    isLocked -> Color.White.copy(alpha = 0.08f)
    else -> Color.White.copy(alpha = 0.18f)
  }
  val containerColor = when {
    isSelected -> AssamNavyCard
    isLocked -> AssamNavyCard.copy(alpha = 0.45f)
    else -> AssamNavyCard.copy(alpha = 0.85f)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(14.dp))
      .clickable(enabled = !isLocked, onClick = onClick)
      .testTag("level_card_${level.index + 1}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(13.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(
            when {
              isLocked -> Color.White.copy(alpha = 0.10f)
              isSelected -> AssamSaffron
              else -> AssamTeaEmerald
            }
          ),
        contentAlignment = Alignment.Center,
      ) {
        if (isLocked) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = AssamMutedText,
            modifier = Modifier.size(18.dp),
          )
        } else {
          Text(
            text = "${level.index + 1}",
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected) AssamSaffronDarkText else AssamIvory,
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${level.from} → ${level.to}",
          style = MaterialTheme.typography.titleMedium,
          color = if (isLocked) AssamMutedText else AssamIvory,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${level.km} km • Reward ₹${inrFormatter.format(level.reward)} • ${level.sceneryNote}",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
          color = AssamMutedText.copy(alpha = if (isLocked) 0.65f else 0.9f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
          isLocked -> Color(0xFF4F5B66)
          isSelected -> AssamSaffron
          else -> AssamTeaEmerald
        },
        modifier = Modifier.testTag("level_select_badge_${level.index + 1}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = AssamSaffronDarkText,
              modifier = Modifier.size(15.dp),
            )
          } else if (!isLocked) {
            Icon(
              imageVector = Icons.Rounded.DirectionsCar,
              contentDescription = null,
              tint = AssamIvory,
              modifier = Modifier.size(15.dp),
            )
          }
          Text(
            text = when {
              isLocked -> stringResource(R.string.level_locked)
              isSelected -> stringResource(R.string.level_selected)
              else -> stringResource(R.string.level_select)
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) AssamSaffronDarkText else AssamIvory,
          )
        }
      }
    }
  }
}
