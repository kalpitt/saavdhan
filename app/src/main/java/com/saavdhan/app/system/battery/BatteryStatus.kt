package com.saavdhan.app.system.battery

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

object BatteryStatus {
    /**
     * Returns true if this app is exempted from battery optimizations. If false, the OS may
     * suspend the background watchdog job.
     */
    fun isExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the system "apps not optimised" list. No special permission needed. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
