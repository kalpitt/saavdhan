package com.saavdhan.app.system.watchdog

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saavdhan.app.data.scanner.AppScanner

/**
 * The background watchdog (v2). Runs periodically (and once when the app opens). It runs the same
 * full assessment as an on-demand scan, then diffs package *risk levels* against the saved
 * [InstalledAppsSnapshot]. The pure, tested [WatchdogPolicy] decides what to alert on:
 * dangerous NEW installs, and existing apps that ESCALATED into HIGH/CRITICAL (e.g. the user was
 * talked into enabling Accessibility days after install — the real SpyNote attack sequence).
 *
 * Why a worker and not a manifest `PACKAGE_ADDED` receiver: since Android 8, the system blocks a
 * background manifest receiver from getting `PACKAGE_ADDED` ("Background execution not allowed"),
 * so that approach only fires while the app is open. WorkManager runs reliably in the background.
 * See ADR-0007.
 */
class NewAppScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scanner = AppScanner(applicationContext)

        // Full assessment (same brain as the on-demand scan). Needed because an escalation —
        // an existing app gaining Accessibility/Device-Admin — is invisible to a name-only diff.
        val assessedByPackage = try {
            scanner.scan(includeDemoFixtures = false).apps.associateBy { it.app.packageName }
        } catch (e: Exception) {
            // Background scan failed unexpectedly. Retry a couple of times, then give up gracefully
            // and wait for the next periodic run — never an unbounded retry-storm (which, with
            // WorkManager's exponential backoff, could silently starve the watchdog for hours).
            return if (runAttemptCount >= 2) Result.success() else Result.retry()
        }
        val currentLevels = assessedByPackage.mapValues { it.value.assessment.level }
        val known = InstalledAppsSnapshot.getLevels(applicationContext)

        val alerts = WatchdogPolicy.assessChanges(known, currentLevels)
        for (alert in alerts) {
            val assessed = assessedByPackage[alert.packageName] ?: continue
            ThreatNotifier.notifyThreat(
                applicationContext,
                assessed,
                escalation = alert.kind == WatchdogPolicy.AlertKind.ESCALATION
            )
        }

        InstalledAppsSnapshot.saveLevels(applicationContext, currentLevels)
        InstalledAppsSnapshot.setLastRunMillis(applicationContext, System.currentTimeMillis())
        return Result.success()
    }
}
