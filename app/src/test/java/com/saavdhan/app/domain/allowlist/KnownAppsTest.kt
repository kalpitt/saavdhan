package com.saavdhan.app.domain.allowlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for impersonation detection. Pure logic, runs on your computer. */
class KnownAppsTest {

    @Test
    fun `a sideloaded app named System Update is impersonating`() {
        // No real user-installed app should ever be called "System Update".
        assertTrue(KnownApps.isImpersonating("System Update", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `name match is case and space insensitive`() {
        assertTrue(KnownApps.isImpersonating("  system update ", "com.evil.fake", isSystemApp = false))
    }

    @Test
    fun `the real Google Play Services is NOT impersonating`() {
        assertFalse(
            KnownApps.isImpersonating("Google Play Services", "com.google.android.gms", isSystemApp = false),
        )
    }

    @Test
    fun `a fake Google Play Services IS impersonating`() {
        assertTrue(
            KnownApps.isImpersonating("Google Play Services", "com.evil.fake", isSystemApp = false),
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
}
