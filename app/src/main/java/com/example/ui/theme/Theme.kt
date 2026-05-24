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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF55CDFC), // Supportive Soft Blue
    secondary = Color(0xFFF7A8B8), // Supportive Rose Pink
    tertiary = Color(0xFFFFFFFF), // Pearl White
    background = Color(0xFF12141A), // Pure modern dark slate
    surface = Color(0xFF1C1F2E), // Card container dark
    onPrimary = Color(0xFF00354B),
    onSecondary = Color(0xFF5B1124),
    onBackground = Color(0xFFE2E2EC),
    onSurface = Color(0xFFE2E2EC),
    surfaceVariant = Color(0xFF282C3D),
    onSurfaceVariant = Color(0xFFC7C6D5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF006689), // Deep supportive ocean blue
    secondary = Color(0xFF944053), // Deep coral pink
    tertiary = Color(0xFF6C5D00),
    background = Color(0xFFFAF9FC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E1F24),
    onSurface = Color(0xFF1E1F24),
    surfaceVariant = Color(0xFFF0F1FA),
    onSurfaceVariant = Color(0xFF434550)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
