package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HelloDarkColorScheme = darkColorScheme(
  primary = HelloCoral,
  onPrimary = Color.White,
  primaryContainer = HelloCoralContainerDark,
  onPrimaryContainer = Color(0xFFFFD9DA),
  secondary = HelloVioletLight,
  onSecondary = HelloMidnight,
  secondaryContainer = HelloVioletContainerDark,
  onSecondaryContainer = Color(0xFFEDE9FE),
  tertiary = HelloRose,
  onTertiary = Color.White,
  tertiaryContainer = HelloRoseContainerDark,
  onTertiaryContainer = Color(0xFFFFD9E0),
  background = HelloMidnight,
  onBackground = DarkOnSurface,
  surface = HelloMidnightCard,
  onSurface = DarkOnSurface,
  surfaceVariant = HelloMidnightElevated,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  error = Color(0xFFFF6B6B),
  onError = HelloMidnight
)

private val HelloLightColorScheme = lightColorScheme(
  primary = HelloCoral,
  onPrimary = Color.White,
  primaryContainer = HelloCoralContainerLight,
  onPrimaryContainer = Color(0xFF6B0F15),
  secondary = HelloViolet,
  onSecondary = Color.White,
  secondaryContainer = HelloVioletContainerLight,
  onSecondaryContainer = Color(0xFF2E1065),
  tertiary = HelloRose,
  onTertiary = Color.White,
  tertiaryContainer = HelloRoseContainerLight,
  onTertiaryContainer = Color(0xFF6B0921),
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  error = Color(0xFFDC2626),
  onError = Color.White
)

val HelloShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HelloTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) HelloDarkColorScheme else HelloLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = HelloTypography,
    shapes = HelloShapes,
    content = content
  )
}
