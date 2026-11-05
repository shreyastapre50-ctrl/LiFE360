
package com.life360.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF176B45),
    onPrimary = Color.White,

    secondary = Color(0xFF3478F6),
    onSecondary = Color.White,

    tertiary = Color(0xFFE58A24),
    onTertiary = Color.White,

    background = Color(0xFFF7F9F7),
    onBackground = Color(0xFF17231C),

    surface = Color.White,
    onSurface = Color(0xFF17231C),

    surfaceVariant = Color(0xFFE8EEE9),
    onSurfaceVariant = Color(0xFF35443A),

    outline = Color(0xFF7A8A7F),

    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BD5AA),
    onPrimary = Color(0xFF003920),

    secondary = Color(0xFFADC7FF),
    onSecondary = Color(0xFF102F60),

    tertiary = Color(0xFFFFC27A),
    onTertiary = Color(0xFF482900),

    background = Color(0xFF121A15),
    onBackground = Color(0xFFE2EDE4),

    surface = Color(0xFF19231D),
    onSurface = Color(0xFFE2EDE4),

    surfaceVariant = Color(0xFF35443A),
    onSurfaceVariant = Color(0xFFD0DDD2),

    outline = Color(0xFF8A9A8E),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun LiFE360CopyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme && dynamicColor) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
