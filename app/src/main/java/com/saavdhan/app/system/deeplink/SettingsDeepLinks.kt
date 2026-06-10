package com.saavdhan.app.system.deeplink

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/**
 * Builds the "jump straight to the right screen" intents.
 *
 * Reality check (this is the honest core of the app): Android does NOT allow one app to switch off
 * another app's Accessibility or Device-Admin, or to uninstall it silently. So every function here
 * either opens the EXACT page for that app (App Info, Uninstall) or the right SETTINGS LIST
 * (Accessibility, Device Admin) and we coach the user to make the final tap themselves.
 */
object SettingsDeepLinks {

    /** Opens THIS specific app's "App info" page (Force Stop / Permissions / Uninstall live here). Exact. */
    fun appInfo(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Shows the system uninstall confirmation for THIS app. Exact. (Blocked only if Device Admin is active.) */
    fun uninstall(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the Accessibility settings LIST. We cannot pre-select the row, so we coach the user. */
    fun accessibilitySettings(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens Security settings, where the Device-Admin list lives on most phones. Best available. */
    fun deviceAdminSettings(): Intent =
        Intent(Settings.ACTION_SECURITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the Airplane-mode / wireless settings. We cannot toggle it — the user taps it. */
    fun airplaneSettings(): Intent =
        Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the main system Settings. There is no reliable factory-reset deep link, so we guide. */
    fun mainSettings(): Intent =
        Intent(Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Launches [intent], or shows a gentle message if this particular phone has no screen for it
     * (some manufacturers move these around). Never crashes.
     */
    fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Could not open that screen on this phone. Please open Settings manually.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
