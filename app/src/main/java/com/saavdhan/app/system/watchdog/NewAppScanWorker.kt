package com.saavdhan.app.system.watchdog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saavdhan.app.data.scanner.AppScanner
import com.saavdhan.app.data.scanner.AssessedApp

/**
 * The real background watchdog. Runs periodically (and once when the app opens). It diffs the
 * current installed-app list against the saved [InstalledAppsSnapshot]; for each *newly* installed
 * package it runs the same risk check as a full scan and notifies if the app is dangerous. The
 * "which new packages should alert" decision lives in the pure, tested [WatchdogPolicy].
 *
 * Why a worker and not a manifest `PACKAGE_ADDED` receiver: since Android 8, the system blocks a
 * background manifest receiver from getting `PACKAGE_ADDED` ("Background execution not allowed"),
 * so that approach only fires while the app is open. WorkManager runs reliably in the background.
 * See ADR-0007.
 */
class NewAppScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scanner = AppScanner(applicationContext)
        val current = scanner.installedPackageNames()
        val known = InstalledAppsSnapshot.get(applicationContext)

        // Assess only the newly-installed packages once, then let WatchdogPolicy decide which
        // ones warrant an alert. On the first run (known == null) the policy returns nothing.
        val newlyInstalled = if (known == null) emptySet() else current - known
        val assessedByPackage: Map<String, AssessedApp> = newlyInstalled
            .mapNotNull { pkg -> scanner.assessSingle(pkg)?.let { pkg to it } }
            .toMap()

        val toAlert = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = known,
            currentPackages = current,
            getRiskLevel = { assessedByPackage[it]?.assessment?.level },
        )
        toAlert.forEach { pkg ->
            assessedByPackage[pkg]?.let { ThreatNotifier.notifyThreat(applicationContext, it) }
        }

        InstalledAppsSnapshot.save(applicationContext, current)
        InstalledAppsSnapshot.setLastRunMillis(applicationContext, System.currentTimeMillis())
        return Result.success()
    }
}
