package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AssamDarkColorScheme = darkColorScheme(
  primary = AssamSaffron,
  onPrimary = AssamSaffronDarkText,
  primaryContainer = AssamSaffronDeep,
  onPrimaryContainer = AssamSaffronDarkText,
  secondary = AssamTeaEmerald,
  onSecondary = AssamIvory,
  tertiary = AssamHighwayBlue,
  onTertiary = AssamIvory,
  background = AssamNavyDeep,
  onBackground = AssamIvory,
  surface = AssamNavySurface,
  onSurface = AssamIvory,
  surfaceVariant = AssamNavyCard,
  onSurfaceVariant = AssamMutedText,
  error = AssamBrakeRed,
  onError = AssamIvory,
  outline = AssamHudBorder,
)

private val AssamShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
)

@Composable
fun AssamTruckTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = AssamDarkColorScheme,
    typography = AssamTypography,
    shapes = AssamShapes,
    content = content,
  )
}
