package com.pupil.app.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF1D9E75)
private val DarkGray = Color(0xFF121212)
private val LightGray = Color(0xFFF5F5F5)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    background = LightGray,
    surface = Color.White,
    onSurface = Color.Black,
    secondary = Teal
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    background = DarkGray,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    secondary = Teal
)

@Composable
fun PupilTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
