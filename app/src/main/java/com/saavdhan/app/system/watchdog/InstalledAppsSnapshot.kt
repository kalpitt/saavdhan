package com.saavdhan.app.system.watchdog

import android.content.Context

/**
 * Remembers which packages we'd already seen, so the watchdog can spot *newly* installed apps by
 * diffing the current list against this snapshot. Stored locally in SharedPreferences.
 */
object InstalledAppsSnapshot {
    private const val PREFS = "saavdhan_watchdog"
    private const val KEY = "known_packages"

    /** The set of known packages, or null if we've never taken a snapshot (first run). */
    fun get(context: Context): Set<String>? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, null)
            ?.toSet() // defensive copy; the returned set must not be mutated

    fun save(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, packages)
            .apply()
    }
}
