package com.saavdhan.app.domain.risk

import com.saavdhan.app.domain.model.InstallSource
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.domain.model.RiskSignal
import com.saavdhan.app.domain.model.ScannedApp
import org.junit.Assert.assertEquals
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
        hiddenIcon: Boolean = false,
        impersonates: Boolean = false,
    ) = ScannedApp(
        packageName = packageName,
        label = label,
        installSource = installSource,
        isSystemApp = isSystemApp,
        hasAccessibilityEnabled = accessibility,
        isDeviceAdmin = deviceAdmin,
        requestsSms = requestsSms,
        smsGranted = smsGranted,
        hasHiddenIcon = hiddenIcon,
        impersonatesSystemApp = impersonates,
        firstInstallTimeMillis = 0L,
    )

    @Test
    fun `the spyware trinity is CRITICAL`() {
        val result = RiskEngine.assess(
            app(
                installSource = InstallSource.SIDELOADED,
                accessibility = true,
                deviceAdmin = true,
                smsGranted = true,
            ),
        )
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(RiskSignal.ACCESSIBILITY in result.signals)
        assertTrue(RiskSignal.DEVICE_ADMIN in result.signals)
        assertTrue(RiskSignal.SMS_ACCESS in result.signals)
    }

    @Test
    fun `sideloaded plus accessibility is HIGH`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, accessibility = true),
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `hidden icon on a sideloaded app is HIGH`() {
        val result = RiskEngine.assess(
            app(installSource = InstallSource.SIDELOADED, hiddenIcon = true),
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `impersonating a system app is HIGH`() {
        val result = RiskEngine.assess(
            app(label = "System Update", impersonates = true, installSource = InstallSource.SIDELOADED),
        )
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test
    fun `a Play-Store app holding accessibility is only SUSPICIOUS`() {
        // e.g. a password manager or screen reader the user installed on purpose.
        val result = RiskEngine.assess(app(accessibility = true))
        assertEquals(RiskLevel.SUSPICIOUS, result.level)
    }

    @Test
    fun `SMS access alone is too common to flag`() {
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
                smsGranted = true,
            ),
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `an explicitly trusted package is not alarming`() {
        val result = RiskEngine.assess(
            app(packageName = "com.google.android.marvin.talkback", accessibility = true),
        )
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowlisted)
    }

    @Test
    fun `a clean app is LOW`() {
        assertEquals(RiskLevel.LOW, RiskEngine.assess(app()).level)
    }
}
