package com.saavdhan.app.data.scanner

import com.saavdhan.app.domain.model.InstallSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** Characterization tests for the installer package → InstallSource mapping. Pure logic, no PackageManager. */
class InstallerClassifierTest {

    @Test
    fun `Google Play Store is PLAY_STORE`() {
        assertEquals(InstallSource.PLAY_STORE, InstallerClassifier.classify("com.android.vending"))
    }

    @Test
    fun `null installer (direct APK installation) is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify(null))
    }

    @Test
    fun `Android package installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.android.packageinstaller"))
    }

    @Test
    fun `Google package installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.google.android.packageinstaller"))
    }

    @Test
    fun `Xiaomi (MIUI) package installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.miui.packageinstaller"))
    }

    @Test
    fun `Samsung package installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.samsung.android.packageinstaller"))
    }

    @Test
    fun `Oppo file manager installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.coloros.filemanager"))
    }

    @Test
    fun `Tecno file manager installer is SIDELOADED`() {
        assertEquals(InstallSource.SIDELOADED, InstallerClassifier.classify("com.transsion.packageinstaller"))
    }

    @Test
    fun `unknown installer is OTHER_STORE`() {
        assertEquals(InstallSource.OTHER_STORE, InstallerClassifier.classify("com.amazon.venezia"))
    }

    @Test
    fun `WhatsApp as installer is OTHER_STORE (not recognized sideload tool)`() {
        assertEquals(InstallSource.OTHER_STORE, InstallerClassifier.classify("com.whatsapp"))
    }
}
