package com.saavdhan.app.domain.risk

import com.saavdhan.app.domain.allowlist.KnownApps
import com.saavdhan.app.domain.model.RiskAssessment
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.domain.model.ScannedApp

/**
 * The danger-judging brain. Pure logic, no Android: give it a [ScannedApp], get back a
 * [RiskAssessment] (how worried to be, and exactly why). Because it's pure, every rule below is
 * covered by fast tests in RiskEngineTest — no phone needed.
 *
 * Guiding ideas:
 *  - The scam apps share a fingerprint: Accessibility + Device Admin + SMS, all at once.
 *  - WHERE an app came from is the great disambiguator. A Play-Store app holding Accessibility is
 *    probably a password manager or screen reader; a *sideloaded* one holding it is alarming.
 *  - We never want to terrify someone about their own legitimate tools, so trusted/system apps are
 *    capped at LOW and we keep the softer single-signal cases at SUSPICIOUS, not HIGH.
 */
object RiskEngine {

    fun assess(app: ScannedApp): RiskAssessment {
        val signals = collectSignals(app)
        val allowlisted = app.isSystemApp || app.packageName in KnownApps.TRUSTED_PACKAGES

        // Apps we trust are reported calmly regardless of the powers they hold.
        if (allowlisted) {
            return RiskAssessment(RiskLevel.LOW, signals, allowlisted = true)
        }

        val level = levelFor(signals)
        return RiskAssessment(level, signals, allowlisted = false)
    }

    /** Turn the raw facts about an app into the list of named red flags. */
    private fun collectSignals(app: ScannedApp): List<RiskSignal> = buildList {
        if (app.hasAccessibilityEnabled) add(RiskSignal.ACCESSIBILITY)
        if (app.isDeviceAdmin) add(RiskSignal.DEVICE_ADMIN)
        // Granted SMS access is the dangerous state (that's where OTPs can be read).
        if (app.smsGranted) add(RiskSignal.SMS_ACCESS)
        if (app.hasNotificationListener) add(RiskSignal.NOTIFICATION_LISTENER)
        if (app.installSource == com.saavdhan.app.domain.model.InstallSource.SIDELOADED) add(RiskSignal.SIDELOADED)
        if (app.hasHiddenIcon) add(RiskSignal.HIDDEN_ICON)
        if (app.impersonatesSystemApp) add(RiskSignal.IMPERSONATION)
    }

    /** Combine the red flags into one overall level. Order matters: we check scariest first. */
    private fun levelFor(signals: List<RiskSignal>): RiskLevel {
        val accessibility = RiskSignal.ACCESSIBILITY in signals
        val deviceAdmin = RiskSignal.DEVICE_ADMIN in signals
        val sms = RiskSignal.SMS_ACCESS in signals
        val notif = RiskSignal.NOTIFICATION_LISTENER in signals
        val sideloaded = RiskSignal.SIDELOADED in signals
        val hiddenIcon = RiskSignal.HIDDEN_ICON in signals
        val impersonation = RiskSignal.IMPERSONATION in signals

        // 1. The spyware trinity — the classic banking-trojan fingerprint.
        // Also: accessibility + deviceAdmin + notification (trojans read notifications instead of SMS sometimes).
        if (accessibility && deviceAdmin && (sms || notif)) return RiskLevel.CRITICAL

        // 2. Strong two-signal combinations, and the standalone "this is hiding / faking" flags.
        val highCombo =
            (sideloaded && accessibility) ||
                (sideloaded && deviceAdmin) ||
                (sideloaded && sms) ||
                (sideloaded && notif) ||
                (accessibility && deviceAdmin) ||
                (accessibility && sms) ||
                (accessibility && notif) ||
                (deviceAdmin && sms) ||
                (deviceAdmin && notif) ||
                impersonation ||
                (hiddenIcon && (accessibility || deviceAdmin || sms || sideloaded || notif))
        if (highCombo) return RiskLevel.HIGH

        // 3. A single mild clue worth a glance.
        val suspicious = sideloaded || accessibility || deviceAdmin || hiddenIcon || notif
        if (suspicious) return RiskLevel.SUSPICIOUS

        // 4. Nothing notable (SMS access alone is too common to flag on its own).
        return RiskLevel.LOW
    }
}
