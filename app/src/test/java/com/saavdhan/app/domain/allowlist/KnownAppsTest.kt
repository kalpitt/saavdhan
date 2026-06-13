package com.saavdhan.app.domain.allowlist

import com.saavdhan.app.domain.model.InstallSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for allowlist and impersonation detection. Pure logic, runs on your computer. */
class KnownAppsTest {

    @Test
    fun `a sideloaded app named System Update is impersonating`() {
        assertTrue(KnownApps.isImpersonating("System Update", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `name match is case and space insensitive`() {
        assertTrue(KnownApps.isImpersonating("  system update ", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `the real Google Play Services is NOT impersonating`() {
        assertFalse(
            KnownApps.isImpersonating("Google Play Services", "com.google.android.gms", isSystemApp = false)
        )
    }

    @Test
    fun `a fake Google Play Services IS impersonating`() {
        assertTrue(
            KnownApps.isImpersonating("Google Play Services", "com.evil.fake", isSystemApp = false)
        )
    }

    @Test
    fun `system apps are never treated as impersonators`() {
        assertFalse(KnownApps.isImpersonating("System Update", "com.android.whatever", isSystemApp = true))
    }

    @Test
    fun `an ordinary app name is not flagged`() {
        assertFalse(KnownApps.isImpersonating("My Recipes", "com.example.recipes", isSystemApp = false))
    }

    @Test
    fun `name match ignores punctuation, emoji and extra spaces`() {
        assertTrue(KnownApps.isImpersonating("System  Update!", "com.evil.fake", isSystemApp = false))
        assertTrue(KnownApps.isImpersonating("System-Update ⬇️", "com.evil.fake", isSystemApp = false))
        assertTrue(KnownApps.isImpersonating("SYSTEM_UPDATE", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `a fake Security Update is impersonating`() {
        assertTrue(KnownApps.isImpersonating("Security Update", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `a fake Android System WebView is impersonating but the real one is not`() {
        assertTrue(KnownApps.isImpersonating("Android System WebView", "com.evil.fake", isSystemApp = false))
        assertFalse(
            KnownApps.isImpersonating("Android System WebView", "com.google.android.webview", isSystemApp = false)
        )
    }

    @Test
    fun `a fake Play Protect is impersonating`() {
        assertTrue(KnownApps.isImpersonating("Play Protect", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `a non-Latin label simply does not match`() {
        assertFalse(KnownApps.isImpersonating("मेरा ऐप", "com.example.app", isSystemApp = false))
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
