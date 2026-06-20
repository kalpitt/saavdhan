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
        impersonates: Boolean = false
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
        firstInstallTimeMillis = 0L
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
    fun `SMS access alone is SUSPICIOUS`() {
        val result = RiskEngine.assess(app(requestsSms = true, smsGranted = true))
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
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
        RiskSignal.values().forEach { signal ->
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
