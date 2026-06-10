package com.saavdhan.app.data.scanner

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
    fun classify(installer: String?): InstallSource = when (installer) {
        "com.android.vending" -> InstallSource.PLAY_STORE
        null -> InstallSource.SIDELOADED
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",  // Xiaomi file manager / sideload installer
        "com.samsung.android.packageinstaller",  // Samsung package manager
        "com.coloros.filemanager",  // Oppo/Realme file manager
        "com.transsion.packageinstaller",  // Tecno/Infinix file manager
        -> InstallSource.SIDELOADED
        else -> InstallSource.OTHER_STORE
    }
}
