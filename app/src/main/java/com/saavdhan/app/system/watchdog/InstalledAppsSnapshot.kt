package com.saavdhan.app.system.watchdog

import android.content.Context

/**
 * Remembers which packages we'd already seen, so the watchdog can spot *newly* installed apps by
 * diffing the current list against this snapshot. Stored locally in SharedPreferences.
 */
object InstalledAppsSnapshot {
    private const val PREFS = "saavdhan_watchdog"
    private const val KEY_PACKAGES = "known_packages"
    private const val KEY_LAST_RUN = "last_run_millis"

    /** The set of known packages, or null if we've never taken a snapshot (first run). */
    fun get(context: Context): Set<String>? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_PACKAGES, null)
            ?.toSet() // defensive copy; the returned set must not be mutated

    fun save(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PACKAGES, packages)
            .apply()
    }

    /** When the watchdog last completed a scan, in epoch milliseconds. Returns 0 if never run. */
    fun getLastRunMillis(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_RUN, 0L)

    fun setLastRunMillis(context: Context, millis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_RUN, millis)
            .apply()
    }
}
