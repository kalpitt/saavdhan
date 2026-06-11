package com.saavdhan.app.system.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Small façade the UI uses to control the [OverlayCoachService] without touching service plumbing.
 */
object OverlayCoach {

    /** True if the user has granted the "draw over other apps" permission. */
    fun isAvailable(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Send the user to the system screen where they can grant the overlay permission. */
    fun requestPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Some devices route this differently; fail quietly.
        }
    }

    /** Show the coaching banner with [message]. No-ops if the permission isn't granted. */
    fun show(context: Context, message: String) {
        if (!isAvailable(context)) return
        val intent = Intent(context, OverlayCoachService::class.java)
            .putExtra(OverlayCoachService.EXTRA_MESSAGE, message)
        context.startService(intent)
    }

    /** Remove the banner if it's showing. */
    fun hide(context: Context) {
        context.stopService(Intent(context, OverlayCoachService::class.java))
    }
}
