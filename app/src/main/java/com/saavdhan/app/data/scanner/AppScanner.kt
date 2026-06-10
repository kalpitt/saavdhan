package com.saavdhan.app.data.scanner

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.saavdhan.app.BuildConfig
import com.saavdhan.app.domain.allowlist.KnownApps
import com.saavdhan.app.domain.model.InstallSource
import com.saavdhan.app.domain.model.RiskAssessment
import com.saavdhan.app.domain.model.ScannedApp
import com.saavdhan.app.domain.risk.RiskEngine

/** One app plus the verdict the brain reached about it. */
data class AssessedApp(
    val app: ScannedApp,
    val assessment: RiskAssessment,
)

/** The outcome of a whole scan: every app assessed (scariest first), and whether the scan was limited. */
data class ScanResult(
    val apps: List<AssessedApp>,
    val partial: Boolean,
)

/**
 * Reads the real facts about installed apps using Android's public APIs (no root needed) and runs
 * each through [RiskEngine]. This is the only place that talks to Android's package system.
 */
class AppScanner(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun scan(): ScanResult {
        val accessibilityPackages = enabledAccessibilityPackages()
        val adminPackages = activeDeviceAdminPackages()
        val myPackage = context.packageName

        val assessed = installedPackages()
            .mapNotNull { info -> assessInfo(info, accessibilityPackages, adminPackages, myPackage) }
            .toMutableList()

        // On an emulator there is no real malware, so add demo threats to exercise the UI.
        if (BuildConfig.DEBUG) assessed += demoApps()

        // Scariest first, and de-duplicated by package: a package name must be unique in the list
        // (the UI uses it as a stable key, and a duplicate would otherwise crash the results list).
        val sorted = assessed
            .sortedByDescending { it.assessment.level.ordinal }
            .distinctBy { it.app.packageName }
        return ScanResult(apps = sorted, partial = false)
    }

    /**
     * Assess ONE app by package name. Used by the background watchdog when a new app is installed.
     * Returns null if the package can't be read or is our own app.
     */
    fun assessSingle(packageName: String): AssessedApp? {
        if (packageName == context.packageName) return null
        val info = packageInfo(packageName) ?: return null
        return assessInfo(info, enabledAccessibilityPackages(), activeDeviceAdminPackages(), context.packageName)
    }

    /** Build the facts for one package and run the brain over them. Shared by full and single scans. */
    private fun assessInfo(
        info: PackageInfo,
        accessibilityPackages: Set<String>,
        adminPackages: Set<String>,
        myPackage: String,
    ): AssessedApp? {
        val appInfo = info.applicationInfo ?: return null
        val pkg = info.packageName
        if (pkg == myPackage) return null // never flag ourselves

        val label = pm.getApplicationLabel(appInfo).toString()
        val isSystem = isSystemApp(appInfo)

        val scanned = ScannedApp(
            packageName = pkg,
            label = label,
            installSource = installSource(pkg),
            isSystemApp = isSystem,
            hasAccessibilityEnabled = pkg in accessibilityPackages,
            isDeviceAdmin = pkg in adminPackages,
            requestsSms = requestsSms(info),
            smsGranted = smsGranted(info),
            hasHiddenIcon = !isSystem && pm.getLaunchIntentForPackage(pkg) == null,
            impersonatesSystemApp = KnownApps.isImpersonating(label, pkg, isSystem),
            firstInstallTimeMillis = info.firstInstallTime,
        )
        return AssessedApp(scanned, RiskEngine.assess(scanned))
    }

    // --- Reading the phone -------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    /** Lightweight list of all installed package names (no permission data). Used by the watchdog. */
    @Suppress("DEPRECATION")
    fun installedPackageNames(): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getInstalledPackages(0)
        }.mapTo(mutableSetOf()) { it.packageName }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun enabledAccessibilityPackages(): Set<String> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
            .toSet()
    }

    private fun activeDeviceAdminPackages(): Set<String> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
    }

    @Suppress("DEPRECATION")
    private fun installSource(packageName: String): InstallSource {
        val installer: String? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
        return when (installer) {
            "com.android.vending" -> InstallSource.PLAY_STORE
            null -> InstallSource.SIDELOADED
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            -> InstallSource.SIDELOADED
            else -> InstallSource.OTHER_STORE
        }
    }

    private val smsPermissions = setOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
    )

    private fun requestsSms(info: PackageInfo): Boolean =
        info.requestedPermissions?.any { it in smsPermissions } == true

    private fun smsGranted(info: PackageInfo): Boolean {
        val requested = info.requestedPermissions ?: return false
        val flags = info.requestedPermissionsFlags ?: return false
        for (i in requested.indices) {
            val isSms = requested[i] in smsPermissions
            val granted = (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (isSms && granted) return true
        }
        return false
    }

    private fun isSystemApp(info: ApplicationInfo): Boolean {
        val mask = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return (info.flags and mask) != 0
    }

    // --- Demo data (debug builds only) -------------------------------------------------------

    private fun demoApps(): List<AssessedApp> {
        val now = System.currentTimeMillis()
        val demo = listOf(
            ScannedApp(
                packageName = "com.demo.systemupdate",
                label = "System Update",
                installSource = InstallSource.SIDELOADED,
                isSystemApp = false,
                hasAccessibilityEnabled = true,
                isDeviceAdmin = true,
                requestsSms = true,
                smsGranted = true,
                hasHiddenIcon = true,
                impersonatesSystemApp = true,
                firstInstallTimeMillis = now,
            ),
            ScannedApp(
                packageName = "com.demo.fastcash",
                label = "Fast Cash Loan",
                installSource = InstallSource.SIDELOADED,
                isSystemApp = false,
                hasAccessibilityEnabled = true,
                isDeviceAdmin = false,
                requestsSms = true,
                smsGranted = true,
                hasHiddenIcon = false,
                impersonatesSystemApp = false,
                firstInstallTimeMillis = now,
            ),
        )
        return demo.map { AssessedApp(it, RiskEngine.assess(it)) }
    }
}
