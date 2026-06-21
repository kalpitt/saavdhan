package com.saavdhan.app.domain.risk

import com.saavdhan.app.domain.allowlist.KnownApps
import com.saavdhan.app.domain.model.InstallSource
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
        val allowlisted = isAllowlisted(app, signals)

        // Apps we trust are reported calmly regardless of the powers they hold.
        if (allowlisted) {
            return RiskAssessment(RiskLevel.LOW, 0, signals, allowlisted = true)
        }

        val score = calculateScore(signals)
        val level = levelForScore(score)
        return RiskAssessment(level, score, signals, allowlisted = false)
    }

    /**
     * Each signal's contribution to the risk score — the single source of truth for "how damning
     * is this clue". [calculateScore] sums it, and the UI reads it (via [weightOf]) to show the
     * most decisive evidence first. Keeping it in one map means the engine's priority and the
     * reasons the user reads can never drift apart. Every [RiskSignal] must appear here.
     */
    private val WEIGHTS: Map<RiskSignal, Int> = mapOf(
        RiskSignal.IMPERSONATION to 50,
        RiskSignal.ACCESSIBILITY to 40,
        RiskSignal.DEVICE_ADMIN to 40,
        RiskSignal.HIDDEN_ICON to 40,
        RiskSignal.NOTIFICATION_LISTENER to 20,
        RiskSignal.SMS_ACCESS to 20,
        RiskSignal.SIDELOADED to 20,
        RiskSignal.SMS_REQUESTED to 10,
        RiskSignal.NEW_INSTALL to 10
    )

    /**
     * How many points [signal] adds to an app's risk score. The UI uses this to rank the reasons
     * it shows — scariest first — so the explanation always agrees with the verdict.
     */
    fun weightOf(signal: RiskSignal): Int = WEIGHTS.getValue(signal)

    private fun calculateScore(signals: List<RiskSignal>): Int =
        signals.sumOf { WEIGHTS.getValue(it) }

    private fun levelForScore(score: Int): RiskLevel {
        return when {
            score >= 80 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 20 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.LOW
        }
    }

    /** Determine if an app should be trusted and hidden from results. */
    private fun isAllowlisted(app: ScannedApp, signals: List<RiskSignal>): Boolean {
        if (app.isSystemApp) return true

        // Absolute Override: If the signature matches a trusted key, trust it unconditionally.
        if (app.signatureHashes.any { it in KnownApps.TRUSTED_SIGNATURES }) {
            return true
        }

        // Exact-match trusted packages must also pass a power check for sideloaded impostors
        if (app.packageName in KnownApps.TRUSTED_PACKAGES) {
            if (app.installSource == InstallSource.SIDELOADED &&
                (app.hasAccessibilityEnabled || app.isDeviceAdmin || app.smsGranted)
            ) {
                return false
            }
            return true
        }

        // Prefix-trusted packages (e.g. com.google.android.*). isTrustedPackage already excludes
        // sideloaded apps, so the real spoofing risk here is a NON-Play install (other store /
        // unknown source) claiming a trusted prefix while holding dangerous powers. Only a
        // Play-verified install of such a package is trusted unconditionally.
        if (KnownApps.isTrustedPackage(app.packageName, app.installSource)) {
            if (app.installSource != InstallSource.PLAY_STORE &&
                (app.hasAccessibilityEnabled || app.isDeviceAdmin || app.smsGranted)
            ) {
                return false
            }
            return true
        }

        return false
    }

    /** Turn the raw facts about an app into the list of named red flags. */
    private fun collectSignals(app: ScannedApp): List<RiskSignal> = buildList {
        if (app.hasAccessibilityEnabled) add(RiskSignal.ACCESSIBILITY)
        if (app.isDeviceAdmin) add(RiskSignal.DEVICE_ADMIN)
        // Granted SMS access is the dangerous state (that's where OTPs can be read).
        if (app.smsGranted) add(RiskSignal.SMS_ACCESS)
        // Sideloaded app that requests SMS is a warning sign (even if not granted yet).
        if (app.installSource == InstallSource.SIDELOADED &&
            app.requestsSms && !app.smsGranted
        ) {
            add(RiskSignal.SMS_REQUESTED)
        }
        if (app.hasNotificationListener) add(RiskSignal.NOTIFICATION_LISTENER)
        if (app.installSource == InstallSource.SIDELOADED) add(RiskSignal.SIDELOADED)
        // Hidden icon is only a flag for sideloaded apps (store apps may legitimately lack icons).
        if (app.installSource == InstallSource.SIDELOADED && app.hasHiddenIcon) {
            add(RiskSignal.HIDDEN_ICON)
        }
        if (app.impersonatesSystemApp) add(RiskSignal.IMPERSONATION)

        // Installed within the last 24 hours
        if (app.firstInstallTimeMillis > System.currentTimeMillis() - 86400000L) {
            add(RiskSignal.NEW_INSTALL)
        }
    }
}
