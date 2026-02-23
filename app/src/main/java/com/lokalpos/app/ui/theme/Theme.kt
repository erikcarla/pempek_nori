package com.lokalpos.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PrimaryOrange = Color(0xFFFC6203)
private val PrimaryOrangeDark = Color(0xFFD45200)
private val PrimaryOrangeLight = Color(0xFFFFE0CC)
private val PrimaryOrangeOnContainer = Color(0xFF7D3000)
private val Teal600 = Color(0xFF009688)

val SuccessGreen = Color(0xFF34A853)
val ErrorRed = Color(0xFFEA4335)
val WarningOrange = Color(0xFFFB8C00)
val PrimaryBlue = Color(0xFF1A73E8)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = PrimaryOrangeLight,
    onPrimaryContainer = PrimaryOrangeOnContainer,
    secondary = Teal600,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    error = ErrorRed,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB088),
    onPrimary = Color(0xFF5C2600),
    primaryContainer = Color(0xFF7D3000),
    onPrimaryContainer = PrimaryOrangeLight,
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    tertiary = Color(0xFF81C784),
    error = Color(0xFFCF6679),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun LokalPOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
