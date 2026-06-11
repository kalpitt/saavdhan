package com.saavdhan.app.system.watchdog

import android.content.Context
import com.saavdhan.app.domain.model.RiskLevel

/**
 * Remembers which packages we'd already seen AND the risk level each had, so the watchdog can
 * spot newly-installed dangerous apps and apps that escalated (gained dangerous powers) since
 * the last check. Stored locally in SharedPreferences.
 *
 * Format v2: entries are "package:LEVEL". v1 entries (bare package names) decode with a null
 * level, which [WatchdogPolicy] treats as "baseline silently" — so upgrading never spams alerts.
 */
object InstalledAppsSnapshot {
    private const val PREFS = "saavdhan_watchdog"
    private const val KEY_PACKAGES = "known_packages"
    private const val KEY_LAST_RUN = "last_run_millis"

    /** Known package → last-seen risk level (null level = migrated v1 entry). Null map = first run. */
    fun getLevels(context: Context): Map<String, RiskLevel?>? =
        WatchdogPolicy.decode(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY_PACKAGES, null)
        )

    fun saveLevels(context: Context, levels: Map<String, RiskLevel>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PACKAGES, WatchdogPolicy.encode(levels))
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
