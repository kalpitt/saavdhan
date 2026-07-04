package com.saavdhan.app.domain.allowlist

import com.saavdhan.app.domain.model.InstallSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for allowlist and impersonation detection. Pure logic, runs on your computer. */
class KnownAppsTest {

    // --- Lure labels (the 2026 "document as an app" disguise) ---------------------------------

    @Test
    fun `Wedding Invitation is a lure label`() {
        assertTrue(KnownApps.isLureLabel("Wedding Invitation"))
    }

    @Test
    fun `punctuation and emoji do not dodge the lure match`() {
        // Same normalize() as impersonation: "Wedding‑Invitation 💌" -> "wedding invitation".
        assertTrue(KnownApps.isLureLabel("Wedding‑Invitation 💌"))
    }

    @Test
    fun `containment catches longer campaign names`() {
        assertTrue(KnownApps.isLureLabel("RTO E-Challan Update 2026")) // via "challan"
        assertTrue(KnownApps.isLureLabel("Mahavitaran Bill Update")) // via "bill update"
        assertTrue(KnownApps.isLureLabel("India Post Tracking")) // via "india post"
        assertTrue(KnownApps.isLureLabel("SBI KYC Update")) // via "kyc update"
    }

    @Test
    fun `Wedding Card Maker is deliberately NOT a lure label`() {
        // Precision over recall: legit wedding-card-maker apps exist, so "wedding card" is
        // excluded from the list. Only invitation-style phrasing matches.
        assertFalse(KnownApps.isLureLabel("Wedding Card Maker"))
    }

    @Test
    fun `ordinary and single-theme-word names are not lures`() {
        assertFalse(KnownApps.isLureLabel("My Recipes"))
        assertFalse(KnownApps.isLureLabel("Courier Font Editor")) // bare "courier" is excluded
        assertFalse(KnownApps.isLureLabel("Wedding Planner"))
        assertFalse(KnownApps.isLureLabel("Bill Splitter")) // bare "bill" is excluded
    }

    @Test
    fun `a Hindi-script label does not match the English lure keys`() {
        // normalize() reduces non-Latin text to ""; documented existing behaviour.
        assertFalse(KnownApps.isLureLabel("शादी का निमंत्रण"))
    }

    // --- Impersonation ------------------------------------------------------------------------

    @Test
    fun `a sideloaded app named System Update is impersonating`() {
        assertTrue(KnownApps.isImpersonating("System Update", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `name match is case and space insensitive`() {
        assertTrue(KnownApps.isImpersonating("  system update ", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `the real Google Play Services is NOT impersonating`() {
        assertFalse(
            KnownApps.isImpersonating("Google Play Services", "com.google.android.gms", isSystemApp = false, installSource = InstallSource.SIDELOADED)
        )
    }

    @Test
    fun `a fake Google Play Services IS impersonating`() {
        assertTrue(
            KnownApps.isImpersonating("Google Play Services", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED)
        )
    }

    @Test
    fun `system apps are never treated as impersonators`() {
        assertFalse(KnownApps.isImpersonating("System Update", "com.android.whatever", isSystemApp = true, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `an ordinary app name is not flagged`() {
        assertFalse(KnownApps.isImpersonating("My Recipes", "com.example.recipes", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `name match ignores punctuation, emoji and extra spaces`() {
        assertTrue(KnownApps.isImpersonating("System  Update!", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
        assertTrue(KnownApps.isImpersonating("System-Update ⬇️", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
        assertTrue(KnownApps.isImpersonating("SYSTEM_UPDATE", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `a fake Security Update is impersonating`() {
        assertTrue(KnownApps.isImpersonating("Security Update", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `a fake Android System WebView is impersonating but the real one is not`() {
        assertTrue(KnownApps.isImpersonating("Android System WebView", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
        assertFalse(
            KnownApps.isImpersonating("Android System WebView", "com.google.android.webview", isSystemApp = false, installSource = InstallSource.SIDELOADED)
        )
    }

    @Test
    fun `a fake Play Protect is impersonating`() {
        assertTrue(KnownApps.isImpersonating("Play Protect", "com.evil.fake", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `a non-Latin label simply does not match`() {
        assertFalse(KnownApps.isImpersonating("मेरा ऐप", "com.example.app", isSystemApp = false, installSource = InstallSource.SIDELOADED))
    }

    @Test
    fun `a Play-Store Google module is trusted by prefix`() {
        assertTrue(KnownApps.isTrustedPackage("com.google.android.safetycore", InstallSource.PLAY_STORE))
    }

    @Test
    fun `a Play-Store Samsung module is trusted by prefix`() {
        assertTrue(KnownApps.isTrustedPackage("com.samsung.android.messaging", InstallSource.PLAY_STORE))
    }

    @Test
    fun `a sideloaded app is never trusted by prefix`() {
        assertFalse(KnownApps.isTrustedPackage("com.google.android.safetycore", InstallSource.SIDELOADED))
    }

    @Test
    fun `Huawei Health in the trusted list is recognized`() {
        assertTrue(KnownApps.isTrustedPackage("com.huawei.health", InstallSource.PLAY_STORE))
    }

    @Test
    fun `Company Portal in the trusted list is recognized`() {
        assertTrue(
            KnownApps.isTrustedPackage("com.microsoft.windowsintune.companyportal", InstallSource.PLAY_STORE)
        )
    }

    @Test
    fun `an unknown Store app is not trusted`() {
        assertFalse(KnownApps.isTrustedPackage("com.random.unknown", InstallSource.PLAY_STORE))
    }
}
