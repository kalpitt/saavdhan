package com.saavdhan.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.saavdhan.app.R
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.ui.theme.RiskCriticalDark
import com.saavdhan.app.ui.theme.RiskCriticalLight
import com.saavdhan.app.ui.theme.RiskHighDark
import com.saavdhan.app.ui.theme.RiskHighLight
import com.saavdhan.app.ui.theme.RiskLowDark
import com.saavdhan.app.ui.theme.RiskLowLight
import com.saavdhan.app.ui.theme.RiskSuspiciousDark
import com.saavdhan.app.ui.theme.RiskSuspiciousLight

/** Maps the pure domain enums to user-facing text and colours. Lives in the UI layer on purpose. */

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.SUSPICIOUS -> R.string.risk_suspicious
    RiskLevel.LOW -> R.string.risk_low
}

fun RiskLevel.colorForTheme(isDark: Boolean): Color = when (this) {
    RiskLevel.CRITICAL -> if (isDark) RiskCriticalDark else RiskCriticalLight
    RiskLevel.HIGH -> if (isDark) RiskHighDark else RiskHighLight
    RiskLevel.SUSPICIOUS -> if (isDark) RiskSuspiciousDark else RiskSuspiciousLight
    RiskLevel.LOW -> if (isDark) RiskLowDark else RiskLowLight
}

fun RiskLevel.onColorForTheme(isDark: Boolean): Color {
    if (isDark) {
        // Dark mode pastels are very bright, so they need dark text for contrast
        return Color(0xFF212121)
    }
    return when (this) {
        RiskLevel.SUSPICIOUS -> Color(0xFF212121)
        else -> Color.White
    }
}

@Composable
fun RiskLevel.color(): Color = colorForTheme(isSystemInDarkTheme())

/** Text colour that stays readable on top of [color] — amber needs dark text (WCAG contrast). */
@Composable
fun RiskLevel.onColor(): Color = onColorForTheme(isSystemInDarkTheme())

/** The "what this app could do" explanation paragraph, tuned to the overall level. */
@StringRes
fun RiskLevel.explanationRes(): Int = when (this) {
    RiskLevel.CRITICAL -> R.string.detail_what_critical
    RiskLevel.HIGH -> R.string.detail_what_high
    RiskLevel.SUSPICIOUS -> R.string.detail_what_suspicious
    RiskLevel.LOW -> R.string.detail_what_low
}

@StringRes
fun RiskSignal.labelRes(): Int = when (this) {
    RiskSignal.ACCESSIBILITY -> R.string.signal_accessibility
    RiskSignal.DEVICE_ADMIN -> R.string.signal_device_admin
    RiskSignal.SMS_ACCESS -> R.string.signal_sms
    RiskSignal.SMS_REQUESTED -> R.string.signal_sms_requested
    RiskSignal.NOTIFICATION_LISTENER -> R.string.signal_notification_listener
    RiskSignal.SIDELOADED -> R.string.signal_sideloaded
    RiskSignal.SIDELOADED_VIA_MESSENGER -> R.string.signal_sideloaded_via_messenger
    RiskSignal.HIDDEN_ICON -> R.string.signal_hidden_icon
    RiskSignal.IMPERSONATION -> R.string.signal_impersonation
    RiskSignal.NEW_INSTALL -> R.string.signal_new_install
    RiskSignal.LURE_LABEL -> R.string.signal_lure_label
    RiskSignal.INSTALL_PACKAGES_REQUESTED -> R.string.signal_install_packages
    RiskSignal.ACCESSIBILITY_DECLARED -> R.string.signal_accessibility_declared
}
