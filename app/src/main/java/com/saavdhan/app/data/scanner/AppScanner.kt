package com.saavdhan.app.data.scanner

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.saavdhan.app.BuildConfig
import com.saavdhan.app.domain.allowlist.KnownApps
import com.saavdhan.app.domain.model.InstallSource
import com.saavdhan.app.domain.model.RiskAssessment
import com.saavdhan.app.domain.model.ScannedApp
import com.saavdhan.app.domain.risk.RiskEngine

/** One app plus the verdict the brain reached about it. */
data class AssessedApp(
    val app: ScannedApp,
    val assessment: RiskAssessment
)

/** The outcome of a whole scan: every app assessed (scariest first), and whether the scan was limited. */
data class ScanResult(
    val apps: List<AssessedApp>,
    val partial: Boolean
)

/**
 * Reads the real facts about installed apps using Android's public APIs (no root needed) and runs
 * each through [RiskEngine]. This is the only place that talks to Android's package system.
 */
class AppScanner(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    /**
     * @param includeDemoFixtures inject the fake demo threats on emulator debug builds (UI
     * exercise). The watchdog passes false: its baseline must contain only REAL packages, or the
     * decoyapp test fixture (same package name as a demo entry) could never alert as "new".
     */
    fun scan(includeDemoFixtures: Boolean = true): ScanResult {
        val accessibilityPackages = try {
            enabledAccessibilityPackages()
        } catch (e: Exception) {
            emptySet()
        }
        val adminPackages = try {
            activeDeviceAdminPackages()
        } catch (e: Exception) {
            emptySet()
        }
        val notificationListenerPackages = try {
            enabledNotificationListenerPackages()
        } catch (e: Exception) {
            emptySet()
        }
        val myPackage = context.packageName

        val assessed = installedPackages()
            .mapNotNull { info ->
                try {
                    assessInfo(info, accessibilityPackages, adminPackages, notificationListenerPackages, myPackage)
                } catch (e: Exception) {
                    // Skip packages that fail to assess; don't crash the whole scan
                    null
                }
            }
            .toMutableList()

        // On an emulator there is no real malware, so add demo threats to exercise the UI.
        if (includeDemoFixtures && BuildConfig.DEBUG && isEmulator()) assessed += demoApps()

        // Scariest first, and de-duplicated by package: a package name must be unique in the list
        // (the UI uses it as a stable key, and a duplicate would otherwise crash the results list).
        val sorted = assessed
            .sortedByDescending { it.assessment.level.ordinal }
            .distinctBy { it.app.packageName }

        // Mark as partial if visibility is restricted (Play policy, OEM restrictions, etc.)
        val isPartial = !canQueryAllPackages()
        return ScanResult(apps = sorted, partial = isPartial)
    }

    /**
     * Assess ONE app by package name. Used by the background watchdog when a new app is installed.
     * Returns null if the package can't be read or is our own app.
     */
    fun assessSingle(packageName: String): AssessedApp? {
        if (packageName == context.packageName) return null
        val info = packageInfo(packageName) ?: return null
        return assessInfo(info, enabledAccessibilityPackages(), activeDeviceAdminPackages(), enabledNotificationListenerPackages(), context.packageName)
    }

    /** Build the facts for one package and run the brain over them. Shared by full and single scans. */
    private fun assessInfo(
        info: PackageInfo,
        accessibilityPackages: Set<String>,
        adminPackages: Set<String>,
        notificationListenerPackages: Set<String>,
        myPackage: String
    ): AssessedApp? {
        val appInfo = info.applicationInfo ?: return null
        val pkg = info.packageName
        if (pkg == myPackage) return null // never flag ourselves

        val label = pm.getApplicationLabel(appInfo).toString()
        val isSystem = isSystemApp(appInfo)

        val hashes = getSignatureHashes(info)
        android.util.Log.d("SaavdhanScanner", "App: $pkg, Signature Hash: $hashes")

        val scanned = ScannedApp(
            packageName = pkg,
            label = label,
            installSource = installSource(pkg),
            isSystemApp = isSystem,
            hasAccessibilityEnabled = pkg in accessibilityPackages,
            isDeviceAdmin = pkg in adminPackages,
            requestsSms = requestsSms(info),
            smsGranted = smsGranted(info),
            hasNotificationListener = pkg in notificationListenerPackages,
            hasHiddenIcon = !isSystem && pm.getLaunchIntentForPackage(pkg) == null,
            impersonatesSystemApp = KnownApps.isImpersonating(label, pkg, isSystem),
            firstInstallTimeMillis = info.firstInstallTime,
            signatureHashes = hashes
        )
        return AssessedApp(scanned, RiskEngine.assess(scanned))
    }

    // --- Reading the phone -------------------------------------------------------------------

    /** Check if we can see all installed packages (QUERY_ALL_PACKAGES is granted and not restricted by OEM/Play). */
    private fun canQueryAllPackages(): Boolean {
        // Package-visibility filtering arrived in Android 11 (API 30). Before that, apps could
        // always see every installed package, and QUERY_ALL_PACKAGES isn't even a known permission
        // (checkSelfPermission would wrongly report DENIED on API < 30). So below R, never partial.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.QUERY_ALL_PACKAGES)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong() or PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong() or PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    @Suppress("DEPRECATION")
    private fun getSignatureHashes(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            if (signingInfo == null) return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            info.signatures
        }

        if (signatures == null) return emptySet()

        val md = java.security.MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            val hash = md.digest(sig.toByteArray())
            hash.joinToString("") { "%02X".format(it) }
        }.toSet()
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

    private fun enabledNotificationListenerPackages(): Set<String> {
        // Colon-separated flattened ComponentNames, e.g. "com.foo/com.foo.Listener:com.bar/..."
        val flat = android.provider.Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return emptySet()
        return flat.split(':').mapNotNull {
            android.content.ComponentName.unflattenFromString(it)?.packageName
        }.toSet()
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
        return InstallerClassifier.classify(installer)
    }

    private val smsPermissions = setOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS // trojans send fraud SMS/UPI requests from the victim's number
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

    // --- Emulator detection ------------------------------------------------------------------

    private fun isEmulator(): Boolean {
        return Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.PRODUCT.contains("sdk_gphone")
    }

    // --- Demo data (debug builds on emulator only) -------------------------------------------

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
                hasNotificationListener = true,
                hasHiddenIcon = true,
                impersonatesSystemApp = true,
                firstInstallTimeMillis = now
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
                firstInstallTimeMillis = now
            )
        )
        return demo.map { AssessedApp(it, RiskEngine.assess(it)) }
    }
}
