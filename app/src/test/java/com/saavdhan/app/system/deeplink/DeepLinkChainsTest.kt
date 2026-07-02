package com.saavdhan.app.system.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the deep-link decision brain. These run on your computer, no phone needed — which is
 * the whole point: we can't test on real Samsung/Xiaomi/Vivo hardware yet, so these chains are
 * pinned down here instead. Each test checks WHICH screens we try and in WHAT order per maker.
 */
class DeepLinkChainsTest {

    private val allFamilies = SkinFamily.values().toList()

    // ---- Brand normalization: parent brands ----

    @Test
    fun `samsung maps to One UI`() = assertEquals(SkinFamily.ONE_UI, DeepLinkChains.skinFamilyFor("samsung"))

    @Test
    fun `xiaomi maps to MIUI`() = assertEquals(SkinFamily.MIUI, DeepLinkChains.skinFamilyFor("xiaomi"))

    @Test
    fun `oppo maps to ColorOS`() = assertEquals(SkinFamily.COLOR_OS, DeepLinkChains.skinFamilyFor("oppo"))

    @Test
    fun `vivo maps to Funtouch`() = assertEquals(SkinFamily.FUNTOUCH, DeepLinkChains.skinFamilyFor("vivo"))

    @Test
    fun `huawei and honor map to EMUI`() {
        assertEquals(SkinFamily.EMUI, DeepLinkChains.skinFamilyFor("huawei"))
        assertEquals(SkinFamily.EMUI, DeepLinkChains.skinFamilyFor("honor"))
    }

    // ---- Brand normalization: sub-brand aliasing (huge in India) ----

    @Test
    fun `poco and redmi are Xiaomi sub-brands`() {
        assertEquals(SkinFamily.MIUI, DeepLinkChains.skinFamilyFor("poco"))
        assertEquals(SkinFamily.MIUI, DeepLinkChains.skinFamilyFor("redmi"))
    }

    @Test
    fun `realme and oneplus are Oppo-family sub-brands`() {
        assertEquals(SkinFamily.COLOR_OS, DeepLinkChains.skinFamilyFor("realme"))
        assertEquals(SkinFamily.COLOR_OS, DeepLinkChains.skinFamilyFor("oneplus"))
    }

    @Test
    fun `iqoo is a Vivo sub-brand`() = assertEquals(SkinFamily.FUNTOUCH, DeepLinkChains.skinFamilyFor("iqoo"))

    // ---- Brand normalization: unknown makers and messy input ----

    @Test
    fun `unknown makers fall back to generic`() {
        assertEquals(SkinFamily.GENERIC, DeepLinkChains.skinFamilyFor("google"))
        assertEquals(SkinFamily.GENERIC, DeepLinkChains.skinFamilyFor("motorola"))
        assertEquals(SkinFamily.GENERIC, DeepLinkChains.skinFamilyFor(""))
        assertEquals(SkinFamily.GENERIC, DeepLinkChains.skinFamilyFor(null))
    }

    @Test
    fun `matching ignores case and surrounding whitespace`() {
        // Build.MANUFACTURER casing varies by maker ("Xiaomi", "OPPO", "samsung").
        assertEquals(SkinFamily.ONE_UI, DeepLinkChains.skinFamilyFor("Samsung"))
        assertEquals(SkinFamily.MIUI, DeepLinkChains.skinFamilyFor(" XIAOMI "))
        assertEquals(SkinFamily.COLOR_OS, DeepLinkChains.skinFamilyFor("OnePlus"))
    }

    // ---- Device-admin chain ----

    @Test
    fun `device admin chain tries AOSP screen then Security then Settings, for every family`() {
        for (family in allFamilies) {
            val chain = DeepLinkChains.deviceAdminChain(family)
            assertEquals("$family chain length", 3, chain.size)
            assertEquals("$family first stop", "com.android.settings", chain[0].packageName)
            assertEquals(
                "$family first stop",
                "com.android.settings.Settings\$DeviceAdminSettingsActivity",
                chain[0].className
            )
            assertEquals("$family middle stop", DeepLinkChains.ACTION_SECURITY_SETTINGS, chain[1].action)
            assertEquals("$family last stop", DeepLinkChains.ACTION_SETTINGS, chain[2].action)
        }
    }

    // ---- Auto-start chain: per-family leading screens ----

    @Test
    fun `MIUI auto-start leads with the SecurityCenter autostart manager`() {
        val first = DeepLinkChains.autoStartChain(SkinFamily.MIUI).first()
        assertEquals("com.miui.securitycenter", first.packageName)
        assertEquals("com.miui.permcenter.autostart.AutoStartManagementActivity", first.className)
    }

    @Test
    fun `One UI auto-start leads with the global Device Care, not the China-only build`() {
        val packages = DeepLinkChains.autoStartChain(SkinFamily.ONE_UI).mapNotNull { it.packageName }
        assertEquals("com.samsung.android.sm", packages.first())
        // The China-only package must still be present (last resort) but never lead.
        assertTrue("sm_cn should remain as a fallback", "com.samsung.android.sm_cn" in packages)
        assertEquals("sm_cn must be the last OEM candidate", "com.samsung.android.sm_cn", packages.last())
    }

    @Test
    fun `ColorOS auto-start tries newer oplus package before legacy coloros and oneplus`() {
        val packages = DeepLinkChains.autoStartChain(SkinFamily.COLOR_OS).mapNotNull { it.packageName }
        assertEquals(
            listOf("com.oplus.safecenter", "com.coloros.safecenter", "com.oneplus.security"),
            packages
        )
    }

    @Test
    fun `Funtouch auto-start leads with vivo permission manager and keeps an iQOO variant`() {
        val packages = DeepLinkChains.autoStartChain(SkinFamily.FUNTOUCH).mapNotNull { it.packageName }
        assertEquals("com.vivo.permissionmanager", packages.first())
        assertTrue("com.iqoo.secure" in packages)
    }

    @Test
    fun `EMUI auto-start tries both known system-manager screens`() {
        val chain = DeepLinkChains.autoStartChain(SkinFamily.EMUI)
        assertEquals(2, chain.count { it.packageName == "com.huawei.systemmanager" })
    }

    @Test
    fun `generic auto-start chain is just the universal Settings action`() {
        val chain = DeepLinkChains.autoStartChain(SkinFamily.GENERIC)
        assertEquals(1, chain.size)
        assertEquals(DeepLinkChains.ACTION_SETTINGS, chain.single().action)
    }

    // ---- Cross-cutting invariants (every family, both targets) ----

    @Test
    fun `every chain ends in a generic action that exists on every phone`() {
        for (family in allFamilies) {
            val chains = listOf(
                "deviceAdmin" to DeepLinkChains.deviceAdminChain(family),
                "autoStart" to DeepLinkChains.autoStartChain(family)
            )
            for ((name, chain) in chains) {
                assertTrue("$family/$name must not be empty", chain.isNotEmpty())
                assertTrue("$family/$name must end in a generic action", chain.last().isGenericAction)
                assertEquals("$family/$name last resort", DeepLinkChains.ACTION_SETTINGS, chain.last().action)
            }
        }
    }

    @Test
    fun `every spec is either an action or a complete component, never both or neither`() {
        for (family in allFamilies) {
            val specs = DeepLinkChains.deviceAdminChain(family) + DeepLinkChains.autoStartChain(family)
            for (spec in specs) {
                val isAction = spec.action != null && spec.packageName == null && spec.className == null
                val isComponent = spec.action == null && spec.packageName != null && spec.className != null
                assertTrue("$family malformed spec: $spec", isAction != isComponent && (isAction || isComponent))
            }
        }
    }

    @Test
    fun `chains never try the same screen twice`() {
        for (family in allFamilies) {
            for (chain in listOf(DeepLinkChains.deviceAdminChain(family), DeepLinkChains.autoStartChain(family))) {
                assertEquals("$family has duplicate screens", chain.distinct().size, chain.size)
            }
        }
    }
}
