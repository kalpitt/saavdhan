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

// Light Mode Semantics (Deep colors)
val RiskCriticalLight = Color(0xFFC62828) // deep red
val RiskHighLight = Color(0xFFEF6C00) // strong orange
val RiskSuspiciousLight = Color(0xFFF9A825) // amber
val RiskLowLight = Color(0xFF2E7D32) // calm green

// Dark Mode Semantics (High-luminance pastels for contrast)
val RiskCriticalDark = Color(0xFFFF8A80) // coral red
val RiskHighDark = Color(0xFFFFB74D) // pastel orange
val RiskSuspiciousDark = Color(0xFFFFE082) // soft amber
val RiskLowDark = Color(0xFF81C784) // pastel green

// Backward compatibility (deprecated, prefer using RiskLevel.color() inside @Composable)
val RiskCritical = RiskCriticalLight
val RiskHigh = RiskHighLight
val RiskSuspicious = RiskSuspiciousLight
val RiskLow = RiskLowLight

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Teal
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Color.Black,
    secondary = TealDark
)

// Slightly larger, friendlier defaults for readability under stress.
private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun SaavdhanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
