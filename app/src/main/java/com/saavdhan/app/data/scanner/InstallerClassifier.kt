package com.saavdhan.app.data.scanner

import com.saavdhan.app.domain.allowlist.KnownApps
import com.saavdhan.app.domain.model.InstallSource

/**
 * Pure logic for classifying app install source from the installer package name.
 * Extracted here so the mapping is testable without PackageManager.
 */
object InstallerClassifier {

    /**
     * Map an installer package name to an InstallSource.
     *
     * @param installer the package that installed the app (from pm.getInstallSourceInfo() or null)
     * @return the classified InstallSource
     */
    fun classify(installer: String?): InstallSource {
        if (installer == null) return InstallSource.SIDELOADED
        if (installer == "com.android.vending") return InstallSource.PLAY_STORE
        if (installer in KnownApps.MESSENGERS) return InstallSource.SIDELOADED
        
        val sideloaders = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.packageinstaller", // Xiaomi file manager / sideload installer
            "com.samsung.android.packageinstaller", // Samsung package manager
            "com.coloros.filemanager", // Oppo/Realme file manager
            "com.transsion.packageinstaller" // Tecno/Infinix file manager
        )
        if (installer in sideloaders) return InstallSource.SIDELOADED

        return InstallSource.OTHER_STORE
    }
}
