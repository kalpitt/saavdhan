package com.saavdhan.app.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.saavdhan.app.R
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.ui.theme.RiskCritical
import com.saavdhan.app.ui.theme.RiskHigh
import com.saavdhan.app.ui.theme.RiskLow
import com.saavdhan.app.ui.theme.RiskSuspicious

/** Maps the pure domain enums to user-facing text and colours. Lives in the UI layer on purpose. */

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.CRITICAL -> R.string.risk_critical
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.SUSPICIOUS -> R.string.risk_suspicious
    RiskLevel.LOW -> R.string.risk_low
}

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.CRITICAL -> RiskCritical
    RiskLevel.HIGH -> RiskHigh
    RiskLevel.SUSPICIOUS -> RiskSuspicious
    RiskLevel.LOW -> RiskLow
}

/** Text colour that stays readable on top of [color] — amber needs dark text (WCAG contrast). */
fun RiskLevel.onColor(): Color = when (this) {
    RiskLevel.SUSPICIOUS -> Color(0xFF212121)
    else -> Color.White
}

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
    RiskSignal.HIDDEN_ICON -> R.string.signal_hidden_icon
    RiskSignal.IMPERSONATION -> R.string.signal_impersonation
}
