package com.saavdhan.app.system.watchdog

import com.saavdhan.app.domain.model.RiskLevel

/**
 * Pure logic for watchdog alerting: given the old and new package sets and a risk lookup function,
 * determine which packages should trigger a notification.
 *
 * Extracted here so the decision is testable without a CoroutineWorker or Android context.
 */
object WatchdogPolicy {

    /**
     * Determine which newly-installed packages should trigger a HIGH/CRITICAL alert.
     *
     * @param knownPackages the package set from the last scan (or null if first run)
     * @param currentPackages all packages currently installed
     * @param getRiskLevel a function that returns the risk level for a package (or null if can't assess)
     * @return list of packageNames that should trigger a notification
     */
    fun newlyInstalledThreats(
        knownPackages: Set<String>?,
        currentPackages: Set<String>,
        getRiskLevel: (packageName: String) -> RiskLevel?,
    ): List<String> {
        if (knownPackages == null) {
            // First run: establish baseline. Don't alert on apps that were already here.
            return emptyList()
        }

        val newlyInstalled = currentPackages - knownPackages
        return newlyInstalled.mapNotNull { packageName ->
            val level = getRiskLevel(packageName) ?: return@mapNotNull null
            if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL) {
                packageName
            } else {
                null
            }
        }
    }
}
