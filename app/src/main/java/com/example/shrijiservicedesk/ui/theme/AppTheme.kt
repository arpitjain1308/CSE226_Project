package com.example.shrijiservicedesk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF202033)
val Muted = Color(0xFF77768A)
val Canvas = Color(0xFFF5F5FA)
val Violet = Color(0xFF6750D8)
val Coral = Color(0xFFFF806B)
val Mint = Color(0xFF48B88A)
val Sky = Color(0xFF5C9DEB)
val Lavender = Color(0xFFEAE5FF)
val PaleMint = Color(0xFFE0F5EB)
val PaleCoral = Color(0xFFFFE9E3)
val PaleBlue = Color(0xFFE4EEFF)

private val AppColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Coral,
    onSecondary = Color.White,
    tertiary = Mint,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECEBF3),
    onSurfaceVariant = Muted,
    outline = Color(0xFFE1DFE9)
)

@Composable
fun ShriJiServiceDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content
    )
}