package com.saavdhan.app.domain.risk

import com.saavdhan.app.domain.model.InstallSource
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.domain.model.ScannedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the danger-judging brain. These run on your computer in seconds, no phone needed.
 * Each test builds a pretend app and checks the engine reaches the verdict we expect.
 */
class RiskEngineTest {

    /** Builds a harmless, Play-installed app; tests override only the fields they care about. */
    private fun app(
        packageName: String = "com.example.app",
        label: String = "Example",
        installSource: InstallSource = InstallSource.PLAY_STORE,
        isSystemApp: Boolean = false,
        accessibility: Boolean = false,
        deviceAdmin: Boolean = false,
        requestsSms: Boolean = false,
        smsGranted: Boolean = false,
        notificationListener: Boolean = false,
        hiddenIcon: Boolean = false,
        impersonates: Boolean = false,
        originatingPackage: String? = null,
        requestsInstallPackages: Boolean = false,
        declaresAccessibilityService: Boolean = false
    ) = ScannedApp(
        packageName = packageName,
        label = label,
        installSource = installSource,
        isSystemApp = isSystemApp,
        hasAccessibilityEnabled = accessibility,
        isDeviceAdmin = deviceAdmin,
        requestsSms = requestsSms,
        smsGranted = smsGranted,
        hasNotificationListener = notificationListener,
        hasHiddenIcon = hiddenIcon,
        impersonatesSystemApp = impersonates,
        firstInstallTimeMillis = 0L,
        originatingPackage = originatingPackage,
        isFromMessenger = originatingPackage in com.saavdhan.app.domain.allowlist.KnownApps.MESSENGERS,
        requestsInstallPackages = requestsInstallPackages,
        declaresAccessibilityService = declaresAccessibilityService
    )

    @Test
    fun `the spyware trinity is CRITICAL`() {
        val result = RiskEngine.assess(
            app(
                installSource = InstallSource.SIDELOADED,
                accessibility = true,
                deviceAdmin = true,
                smsGranted = true
            )
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.ACCESSIBILITY in result.signals)
        assertTrue(RiskSignal.DEVICE_ADMIN in result.signals)
        assertTrue(RiskSignal.SMS_ACCESS in result.signals)
    }

    @Test
    fun `sideloaded plus accessibility is HIGH`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `sideloaded via messenger plus accessibility is CRITICAL`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true, originatingPackage = "com.whatsapp")
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.SIDELOADED_VIA_MESSENGER in result.signals)
    }

    @Test
    fun `sideloaded via WhatsApp Business plus accessibility is CRITICAL`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true, originatingPackage = "com.whatsapp.w4b")
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.SIDELOADED_VIA_MESSENGER in result.signals)
    }

    @Test
    fun `sideloaded where installer is messenger but originatingPackage is null is correctly caught via AppScanner`() {
        // Here we simulate the fact that AppScanner detected the installer as messenger, so isFromMessenger is true,
        // even though originatingPackage is null.
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true, originatingPackage = null).copy(isFromMessenger = true)
        )
        // This test proves the fix! The score MUST have SIDELOADED_VIA_MESSENGER.
        assertTrue("Fix: RiskEngine uses isFromMessenger flag passed by AppScanner.", RiskSignal.SIDELOADED_VIA_MESSENGER in result.signals)
    }

    @Test
    fun `hidden icon on a sideloaded app alone is HIGH`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, hiddenIcon = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `sideloaded hidden icon plus SMS request is HIGH`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, hiddenIcon = true, requestsSms = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `store-installed app with no icon is LOW (not flagged)`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.PLAY_STORE, hiddenIcon = true)
        )
        assertEquals(RiskLevel.LOW, result.level)
    }

    @Test
    fun `impersonating a system app (no powers) is HIGH`() {
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `impersonation plus accessibility is CRITICAL`() {
        // A fake "System Update" that can read the screen and tap = an active banking trojan.
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, accessibility = true, installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `impersonation plus SMS access is CRITICAL`() {
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, requestsSms = true, smsGranted = true, installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `impersonation plus device admin is CRITICAL`() {
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, deviceAdmin = true, installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `impersonation plus notification listener is CRITICAL`() {
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, notificationListener = true, installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `a Play-Store app holding accessibility is only SUSPICIOUS`() {
        // e.g. a password manager or screen reader the user installed on purpose.
        val result = RiskEngine.assess(app(accessibility = true))
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
    }

    @Test
    fun `SMS access alone is LOW (a messaging app should not be flagged without other signals)`() {
        // SMS_ACCESS weight is 10 — below the SUSPICIOUS threshold — so a store-installed
        // messaging app that reads SMS is LOW, not SUSPICIOUS. A sideloaded SMS app adds
        // SIDELOADED (20) for a combined 30 = SUSPICIOUS, which is the correct distinction.
        val result = RiskEngine.assess(app(requestsSms = true, smsGranted = true))
        assertEquals(RiskLevel.LOW, result.level)
    }

    @Test
    fun `a system app is trusted even with strong powers`() {
        val result = RiskEngine.assess(
            app(
                isSystemApp = true,
                accessibility = true,
                deviceAdmin = true,
                smsGranted = true
            )
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `an explicitly trusted package is not alarming`() {
        val result = RiskEngine.assess(
            app(packageName = "com.google.android.marvin.talkback", accessibility = true)
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `sideloaded plus SMS is SUSPICIOUS`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, smsGranted = true)
        )
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
    }

    @Test
    fun `sideloaded plus notification listener is SUSPICIOUS`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, notificationListener = true)
        )
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
    }

    @Test
    fun `accessibility plus SMS is HIGH`() {
        val result = RiskEngine.assess(
            app(accessibility = true, smsGranted = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `accessibility plus notification listener is HIGH`() {
        val result = RiskEngine.assess(
            app(accessibility = true, notificationListener = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `device admin plus SMS is HIGH`() {
        val result = RiskEngine.assess(
            app(deviceAdmin = true, smsGranted = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `device admin plus notification listener is HIGH`() {
        val result = RiskEngine.assess(
            app(deviceAdmin = true, notificationListener = true)
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `accessibility plus device admin plus notification listener (no SMS) is CRITICAL`() {
        val result = RiskEngine.assess(
            app(accessibility = true, deviceAdmin = true, notificationListener = true)
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.NOTIFICATION_LISTENER in result.signals)
    }

    @Test
    fun `notification listener alone is SUSPICIOUS`() {
        val result = RiskEngine.assess(app(notificationListener = true))
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
    }

    @Test
    fun `a clean app is LOW`() {
        assertEquals(RiskLevel.LOW, RiskEngine.assess(app()).level)
    }

    @Test
    fun `a Play-installed Google module (no icon) is LOW and allowlisted`() {
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.safetycore",
                installSource = InstallSource.PLAY_STORE,
                hiddenIcon = true
            )
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `sideloaded app faking a trusted package name with Accessibility is flagged`() {
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.gms",
                installSource = InstallSource.SIDELOADED,
                accessibility = true
            )
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertFalse(result.allowlisted)
    }

    @Test
    fun `sideloaded app faking a trusted package name but harmless is LOW and allowlisted`() {
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.gms",
                installSource = InstallSource.SIDELOADED,
                accessibility = false
            )
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `sideloaded app with a trusted prefix but Accessibility is flagged HIGH (not allowlisted)`() {
        // A sideloaded fake claiming a com.google.android.* prefix: isTrustedPackage already
        // rejects sideloaded, so it's flagged on the normal sideloaded+accessibility path.
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.evil",
                installSource = InstallSource.SIDELOADED,
                accessibility = true
            )
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertFalse(result.allowlisted)
    }

    @Test
    fun `OTHER_STORE app with a trusted prefix and dangerous powers is flagged CRITICAL (not allowlisted)`() {
        // The real prefix-spoofing gap: a non-Play install (other store / unknown) claiming a
        // trusted prefix while holding Accessibility + Device Admin must NOT be allowlisted.
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.evil",
                installSource = InstallSource.OTHER_STORE,
                accessibility = true,
                deviceAdmin = true
            )
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertFalse(result.allowlisted)
    }

    @Test
    fun `UNKNOWN-source app with a trusted prefix and Device Admin plus SMS is flagged HIGH (not allowlisted)`() {
        val result = RiskEngine.assess(
            app(
                packageName = "com.samsung.android.fake",
                installSource = InstallSource.UNKNOWN,
                deviceAdmin = true,
                smsGranted = true
            )
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertFalse(result.allowlisted)
    }

    @Test
    fun `every signal has a weight, and the decisive clues outrank the circumstantial ones`() {
        // The map that drives the score must cover every signal — a missing one would crash the
        // UI ranking (weightOf uses getValue). This guards against adding a signal without a weight.
        RiskSignal.entries.forEach { signal ->
            assertTrue("missing weight for $signal", RiskEngine.weightOf(signal) > 0)
        }
        // Impersonation (the loudest lie) must outrank a single power, which must outrank mere
        // timing — so the detail screen always leads with the most damning evidence.
        assertTrue(RiskEngine.weightOf(RiskSignal.IMPERSONATION) > RiskEngine.weightOf(RiskSignal.ACCESSIBILITY))
        assertTrue(RiskEngine.weightOf(RiskSignal.ACCESSIBILITY) > RiskEngine.weightOf(RiskSignal.NEW_INSTALL))
    }

    @Test
    fun `the score equals the sum of its signal weights`() {
        // Locks the scoring refactor: the points the user could tally from the reasons shown must
        // add up to the score the engine assigned.
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true, deviceAdmin = true, smsGranted = true)
        )
        assertEquals(result.signals.sumOf { RiskEngine.weightOf(it) }, result.score)
    }

    // --- The 2026-campaign signals: LURE_LABEL, INSTALL_PACKAGES_REQUESTED, ACCESSIBILITY_DECLARED

    @Test
    fun `a fresh wedding-invite APK from WhatsApp is CRITICAL before ANY permission is granted`() {
        // The flagship scenario: caught at install time by name (lure), dropper permission, and
        // a declared-but-off Accessibility service — the watchdog can warn BEFORE the victim
        // taps "Allow". 40 (messenger) + 30 (lure) + 25 (installer) + 15 (declared) = 110.
        val result = RiskEngine.assess(
            app(
                label = "Wedding Invitation",
                installSource = InstallSource.SIDELOADED,
                originatingPackage = "com.whatsapp",
                requestsInstallPackages = true,
                declaresAccessibilityService = true
            )
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.LURE_LABEL in result.signals)
        assertTrue(RiskSignal.INSTALL_PACKAGES_REQUESTED in result.signals)
        assertTrue(RiskSignal.ACCESSIBILITY_DECLARED in result.signals)
    }

    @Test
    fun `a lure name on a Play-Store app does not fire (sideload gate)`() {
        // A legit "Electricity Bill Check" utility from Play must stay calm.
        val result = RiskEngine.assess(app(label = "Electricity Bill Check"))
        assertEquals(RiskLevel.LOW, result.level)
        assertFalse(RiskSignal.LURE_LABEL in result.signals)
    }

    @Test
    fun `sideloaded lure name alone is HIGH`() {
        // 30 (lure) + 20 (sideloaded) = 50. A sideloaded APK named "E-Challan" IS the scam.
        val result = RiskEngine.assess(
            app(label = "RTO E-Challan Update", installSource = InstallSource.SIDELOADED)
        )
        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(RiskSignal.LURE_LABEL in result.signals)
    }

    @Test
    fun `sideloaded app store asking to install apps is SUSPICIOUS not HIGH (F-Droid case)`() {
        // 25 (installer) + 20 (sideloaded) = 45: "worth a glance", honest for a legit
        // sideloaded store like F-Droid — the copy says it may be fine if installed on purpose.
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, requestsInstallPackages = true)
        )
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
        assertTrue(RiskSignal.INSTALL_PACKAGES_REQUESTED in result.signals)
    }

    @Test
    fun `install-packages permission on a Play-Store app does not fire (sideload gate)`() {
        val result = RiskEngine.assess(app(requestsInstallPackages = true))
        assertEquals(RiskLevel.LOW, result.level)
        assertFalse(RiskSignal.INSTALL_PACKAGES_REQUESTED in result.signals)
    }

    @Test
    fun `messenger-delivered dropper is HIGH (the SecuriDropper delivery chain)`() {
        // 40 (messenger) + 25 (installer) = 65.
        val result = RiskEngine.assess(
            app(
                installSource = InstallSource.SIDELOADED,
                originatingPackage = "org.telegram.messenger",
                requestsInstallPackages = true
            )
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `sideloaded declared-but-off accessibility is SUSPICIOUS (Bitwarden or Tasker case)`() {
        // 20 (sideloaded) + 15 (declared) = 35: flagged for a glance, not an alarm.
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, declaresAccessibilityService = true)
        )
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
        assertTrue(RiskSignal.ACCESSIBILITY_DECLARED in result.signals)
    }

    @Test
    fun `once accessibility is enabled the declared signal yields to ACCESSIBILITY (no double count)`() {
        val result = RiskEngine.assess(
            app(
                installSource = InstallSource.SIDELOADED,
                accessibility = true,
                declaresAccessibilityService = true
            )
        )
        assertTrue(RiskSignal.ACCESSIBILITY in result.signals)
        assertFalse(RiskSignal.ACCESSIBILITY_DECLARED in result.signals)
        // Identical verdict to today's sideloaded+accessibility: the new signal never worsens it.
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `declared accessibility on a Play-Store app does not fire (sideload gate)`() {
        val result = RiskEngine.assess(app(declaresAccessibilityService = true))
        assertEquals(RiskLevel.LOW, result.level)
        assertFalse(RiskSignal.ACCESSIBILITY_DECLARED in result.signals)
    }

    @Test
    fun `the lure name outranks the softer clues in the explanation order`() {
        // ADR-0012: the UI ranks reasons by weight. The disguise (30) must lead the installer
        // power (25), which must lead the declared-but-off service (15).
        assertTrue(RiskEngine.weightOf(RiskSignal.LURE_LABEL) > RiskEngine.weightOf(RiskSignal.INSTALL_PACKAGES_REQUESTED))
        assertTrue(RiskEngine.weightOf(RiskSignal.INSTALL_PACKAGES_REQUESTED) > RiskEngine.weightOf(RiskSignal.ACCESSIBILITY_DECLARED))
        assertTrue(RiskEngine.weightOf(RiskSignal.ACCESSIBILITY) > RiskEngine.weightOf(RiskSignal.ACCESSIBILITY_DECLARED))
    }

    @Test
    fun `Play-installed app with a trusted prefix and Accessibility is still allowed`() {
        // Play-verified installs of trusted-prefix packages stay trusted even with powers.
        val result = RiskEngine.assess(
            app(
                packageName = "com.google.android.something",
                installSource = InstallSource.PLAY_STORE,
                accessibility = true
            )
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }
}
