package com.saavdhan.app.system.watchdog

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the background [NewAppScanWorker]. Call [onAppOpen] when the app starts. */
object Watchdog {
    private const val PERIODIC_WORK = "saavdhan_periodic_new_app_scan"
    private const val ONESHOT_WORK = "saavdhan_oneshot_new_app_scan"

    /**
     * Ensures the periodic background scan is scheduled, and runs one scan right now (so anything
     * installed since the app was last open is caught promptly).
     */
    fun onAppOpen(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Periodic background scan (15 min is the OS minimum for periodic work).
        val periodic = PeriodicWorkRequestBuilder<NewAppScanWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP, // don't reset the schedule on every open
            periodic
        )

        // Immediate one-off scan to catch installs that happened while we were closed.
        val oneShot = OneTimeWorkRequestBuilder<NewAppScanWorker>().build()
        workManager.enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, oneShot)
    }
}
