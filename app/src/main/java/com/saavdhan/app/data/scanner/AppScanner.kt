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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/** Result of [resilientPackageFetch]: the items we got, and whether we had to give up on some. */
internal data class FetchOutcome<T>(val items: List<T>, val partial: Boolean)

/**
 * Resilient package listing — the fix for a silent total scan failure on cheap phones with many apps.
 *
 * The fast path is one bulk call ([bulk]) that loads every app with heavy data (permissions,
 * signatures, activities). On a budget phone with hundreds of apps that single Binder reply can
 * exceed the 1 MB limit and fail in TWO ways: it throws (AOSP), OR some OEM ROMs (MIUI/ColorOS)
 * SILENTLY truncate the list and return fewer apps with no error — the worst case for a security
 * scanner, because it looks like success.
 *
 * So we take a cheap [names] count first (flags=0 → tiny per-app payload, fits the limit), then try
 * [bulk]. We trust the bulk result only if it didn't throw AND returned at least [trustRatio] of the
 * names. Otherwise we fall back to fetching each app individually via [perName] (one bounded Binder
 * call each), skipping any that fail and reporting `partial = true`.
 *
 * Pure and Android-free (generic over T) so every branch is unit-tested on the JVM without a device.
 */
internal fun <T> resilientPackageFetch(
    names: () -> List<String>,
    bulk: () -> List<T>,
    perName: (String) -> T?,
    trustRatio: Double = 0.95
): FetchOutcome<T> {
    val keys = try {
        names()
    } catch (e: Exception) {
        // Even the cheap listing failed; we can't see the apps. Report nothing, but flag it.
        return FetchOutcome(emptyList(), partial = true)
    }
    val bulkItems = try {
        bulk()
    } catch (e: Exception) {
        null
    }
    if (bulkItems != null && bulkItems.size >= keys.size * trustRatio) {
        return FetchOutcome(bulkItems, partial = false)
    }
    val recovered = keys.mapNotNull { perName(it) }
    return FetchOutcome(recovered, partial = recovered.size < keys.size)
}

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
    suspend fun scan(includeDemoFixtures: Boolean = true): ScanResult = withContext(Dispatchers.IO) {
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

        // Resilient fetch: one fast bulk call, but degrade to a per-package fetch (and mark the scan
        // partial) if that call fails or is silently truncated on a budget phone with many apps.
        val packages = resilientPackageFetch(
            names = { lightweightPackageNames() },
            bulk = { bulkInstalledPackages() },
            perName = { name ->
                try {
                    packageInfo(name)
                } catch (e: Exception) {
                    null
                }
            }
        )
        val assessed = packages.items
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

        // Partial if visibility is restricted (Play policy / OEM) OR the fetch had to skip apps.
        val isPartial = !canQueryAllPackages() || packages.partial
        ScanResult(apps = sorted, partial = isPartial)
    }

    /**
     * Assess ONE app by package name. Used by the background watchdog when a new app is installed.
     * Returns null if the package can't be read or is our own app.
     */
    suspend fun assessSingle(packageName: String): AssessedApp? = withContext(Dispatchers.IO) {
        if (packageName == context.packageName) return@withContext null
        val info = packageInfo(packageName) ?: return@withContext null
        assessInfo(info, enabledAccessibilityPackages(), activeDeviceAdminPackages(), enabledNotificationListenerPackages(), context.packageName)
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
        var src = installSource(pkg)
        val originatingPkg = getOriginatingPackage(pkg)

        val rawInstaller = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg)
            }
        } catch (e: Exception) {
            null
        }

        val isMessenger = originatingPkg in KnownApps.MESSENGERS || rawInstaller in KnownApps.MESSENGERS

        if (isMessenger && src != InstallSource.PLAY_STORE) {
            src = InstallSource.SIDELOADED
        }

        val hashes = getSignatureHashes(info)
        android.util.Log.d("SaavdhanScanner", "App: $pkg, Signature Hash: $hashes")

        var impersonates = KnownApps.isImpersonating(label, pkg, isSystem, src)

        // Task Affinity hijacking check (only for sideloaded)
        if (src == InstallSource.SIDELOADED) {
            info.activities?.forEach { activity ->
                val affinity = activity.taskAffinity
                if (affinity != null && affinity != pkg) {
                    if (affinity.startsWith("com.android.settings") || affinity.contains("bank")) {
                        impersonates = true
                    }
                }
            }
        }

        val scanned = ScannedApp(
            packageName = pkg,
            label = label,
            installSource = src,
            isSystemApp = isSystem,
            hasAccessibilityEnabled = pkg in accessibilityPackages,
            isDeviceAdmin = pkg in adminPackages,
            requestsSms = requestsSms(info),
            smsGranted = smsGranted(info),
            hasNotificationListener = pkg in notificationListenerPackages,
            hasHiddenIcon = !isSystem && pm.getLaunchIntentForPackage(pkg) == null,
            impersonatesSystemApp = impersonates,
            firstInstallTimeMillis = info.firstInstallTime,
            signatureHashes = hashes,
            originatingPackage = originatingPkg,
            isFromMessenger = isMessenger
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

    /** The fast path: one bulk call loading every app with the heavy data the brain needs. */
    @Suppress("DEPRECATION")
    private fun bulkInstalledPackages(): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong() or PackageManager.GET_SIGNING_CERTIFICATES.toLong() or PackageManager.GET_ACTIVITIES.toLong())
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES)
        } else {
            pm.getInstalledPackages(flags)
        }
    }

    /**
     * The cheap pass: just package names, no heavy data, so the reply stays well under the Binder
     * limit even on a phone with hundreds of apps. Gives us an authoritative count (to spot a
     * silently-truncated bulk call) and the names to fetch one-by-one on the fallback path.
     */
    @Suppress("DEPRECATION")
    private fun lightweightPackageNames(): List<String> {
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            pm.getInstalledPackages(0)
        }
        return list.map { it.packageName }
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo? = try {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong() or PackageManager.GET_SIGNING_CERTIFICATES.toLong() or PackageManager.GET_ACTIVITIES.toLong()))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES)
        } else {
            pm.getPackageInfo(packageName, flags)
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

    @Suppress("DEPRECATION")
    private fun getOriginatingPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                info.originatingPackageName ?: info.initiatingPackageName
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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
                firstInstallTimeMillis = now,
                originatingPackage = "com.whatsapp",
                isFromMessenger = true
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
