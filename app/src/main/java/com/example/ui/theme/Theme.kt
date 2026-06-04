package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = VibrantIndigo,
    onPrimary = PremiumWhite,
    secondary = SoftPurple,
    onSecondary = PremiumWhite,
    tertiary = CyberTeal,
    onTertiary = DeepMidnight,
    background = DeepMidnight,
    onBackground = PremiumWhite,
    surface = CardDarkBackground,
    onSurface = PremiumWhite,
    outline = BorderAccentLight
  )

private val LightColorScheme =
  darkColorScheme( // We enforce a uniform beautiful premium dark theme for Luna Workspace
    primary = VibrantIndigo,
    onPrimary = PremiumWhite,
    secondary = SoftPurple,
    onSecondary = PremiumWhite,
    tertiary = CyberTeal,
    onTertiary = DeepMidnight,
    background = DeepMidnight,
    onBackground = PremiumWhite,
    surface = CardDarkBackground,
    onSurface = PremiumWhite,
    outline = BorderAccentLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme for premium design feel
  dynamicColor: Boolean = false, // Disable dynamic colors to keep consistent cyber look
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
