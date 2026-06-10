package com.saavdhan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- Brand + risk colours (shared by both light and dark) ---
val Teal = Color(0xFF00695C)
val TealDark = Color(0xFF4DB6AC)

val RiskCritical = Color(0xFFC62828) // deep red
val RiskHigh = Color(0xFFEF6C00)     // strong orange
val RiskSuspicious = Color(0xFFF9A825) // amber
val RiskLow = Color(0xFF2E7D32)      // calm green

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Teal,
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color.Black,
    secondary = TealDark,
)

// Slightly larger, friendlier defaults for readability under stress.
private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun SaavdhanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
