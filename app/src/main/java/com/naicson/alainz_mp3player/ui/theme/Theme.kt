package com.naicson.alainz_mp3player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

// The design exposes the accent color as a configurable prop (default #1E90FF,
// alternatives #0066CC / #00A8E8) — kept as a CompositionLocal so a future
// settings screen can swap it without threading it through every composable.
val LocalAccentColor = staticCompositionLocalOf { AccentBlue }

private val AppDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    background = ScreenBackground,
    onBackground = TextPrimary,
    surface = ScreenBackground,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    error = Danger,
)

@Composable
fun AlainzMp3PlayerTheme(
    accentColor: Color = AccentBlue,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = AppDarkColorScheme.copy(primary = accentColor),
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
