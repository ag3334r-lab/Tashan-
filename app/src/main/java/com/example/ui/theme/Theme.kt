package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TashanDarkColorScheme = darkColorScheme(
  primary = TashanTeal,
  onPrimary = BgBase,
  secondary = TashanCyan,
  onSecondary = BgBase,
  tertiary = TashanTealDark,
  background = BgBase,
  onBackground = TextPrimary,
  surface = BgElevated,
  onSurface = TextPrimary,
  surfaceVariant = BgElevated2,
  onSurfaceVariant = TextSecondary,
  outline = BorderSubtle
)

private val TashanLightColorScheme = lightColorScheme(
  primary = TashanTeal,
  onPrimary = TextPrimary,
  secondary = TashanCyan,
  onSecondary = TextPrimary,
  tertiary = TashanTealDark,
  background = BgBase, // Keep dark canvas for immersive video experience
  onBackground = TextPrimary,
  surface = BgElevated,
  onSurface = TextPrimary
)

@Composable
fun TashanTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) TashanDarkColorScheme else TashanLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  TashanTheme(darkTheme = darkTheme, content = content)
}
