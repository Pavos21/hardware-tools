package cz.hardwaretools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = lightBaseColor,
    onPrimary = Color.Black,
    secondary = lightSecondaryColor,
    onSecondary = Color.Black,
    surface = lightCardColor,
    background = lightBaseColor,
    onSurface = Color.Black, // For text on surface
    onBackground = Color.Black // For text on background
)

private val DarkColors = darkColorScheme(
    primary = darkBaseColor,
    onPrimary = Color.White,
    secondary = darkSecondaryColor,
    onSecondary = Color.White,
    surface = darkCardColor,
    background = darkBaseColor,
    onSurface = Color.White, // For text on surface
    onBackground = Color.White // For text on background
)

@Composable
fun HardwareToolsTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}