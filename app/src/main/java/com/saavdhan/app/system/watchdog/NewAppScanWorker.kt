package com.saavdhan.app.system.watchdog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saavdhan.app.data.scanner.AppScanner
import com.saavdhan.app.domain.model.RiskLevel

/**
 * The real background watchdog. Runs periodically (and once when the app opens). It diffs the
 * current installed-app list against the saved [InstalledAppsSnapshot]; for each *newly* installed
 * package it runs the same risk check as a full scan and notifies if the app is dangerous.
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

        if (known == null) {
            // First ever run: establish the baseline. We don't alarm about apps that were already
            // installed — the on-demand scan covers those. Only future installs are "new".
            InstalledAppsSnapshot.save(applicationContext, current)
            return Result.success()
        }

        val newlyInstalled = current - known
        for (packageName in newlyInstalled) {
            val assessed = scanner.assessSingle(packageName) ?: continue
            val level = assessed.assessment.level
            if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL) {
                ThreatNotifier.notifyThreat(applicationContext, assessed)
            }
        }

        InstalledAppsSnapshot.save(applicationContext, current)
        return Result.success()
    }
}
