package com.saavdhan.app.system.deeplink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import com.saavdhan.app.R

/**
 * Builds the "jump straight to the right screen" intents.
 *
 * Reality check (this is the honest core of the app): Android does NOT allow one app to switch off
 * another app's Accessibility or Device-Admin, or to uninstall it silently. So every function here
 * either opens the EXACT page for that app (App Info, Uninstall) or the right SETTINGS LIST
 * (Accessibility, Device Admin) and we coach the user to make the final tap themselves.
 *
 * Maker-specific screens come as ORDERED CHAINS from [DeepLinkChains] (pure, unit-tested):
 * most specific first, degrading one honest step at a time down to plain Settings.
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

    /**
     * Ordered candidates for the Device-Admin list: the exact AOSP screen, then the generic
     * Security list, then plain Settings. Launch with [launch] plus the device-admin degrade hint.
     */
    fun deviceAdminSettings(): List<Intent> =
        DeepLinkChains.deviceAdminChain(currentFamily()).map { it.toIntent() }

    /**
     * Ordered candidates for this maker's proprietary auto-start / background-app manager,
     * or null when this maker has no such screen (callers hide their warning card then).
     */
    fun oemAutoStartSettings(): List<Intent>? {
        val chain = DeepLinkChains.autoStartChain(currentFamily())
        if (chain.all { it.isGenericAction }) return null // generic Android: nothing proprietary to warn about
        return chain.map { it.toIntent() }
    }

    /** Opens the Airplane-mode / wireless settings. We cannot toggle it — the user taps it. */
    fun airplaneSettings(): Intent =
        Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the main system Settings. There is no reliable factory-reset deep link, so we guide. */
    fun mainSettings(): Intent =
        Intent(Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Tries each candidate in order and stops at the first screen that opens. Never crashes.
     * Honesty rule: if only the LAST candidate opened (the generic last resort, not the exact
     * screen we aimed for), a toast tells the user where to look — [degradeHintRes] for the
     * chain-specific wording. If nothing opens at all, a gentle "find it yourself" message shows.
     */
    fun launch(context: Context, intents: List<Intent>, @StringRes degradeHintRes: Int = R.string.deeplink_degraded_generic) {
        intents.forEachIndexed { index, candidate ->
            try {
                context.startActivity(candidate)
                if (index == intents.lastIndex && intents.size > 1) {
                    Toast.makeText(context, context.getString(degradeHintRes), Toast.LENGTH_LONG).show()
                }
                return
            } catch (e: Exception) {
                // ActivityNotFoundException or SecurityException — try the next candidate
            }
        }
        Toast.makeText(
            context,
            context.getString(R.string.error_screen_not_found),
            Toast.LENGTH_LONG
        ).show()
    }

    /** Convenience for single-intent call sites (App Info, Uninstall) and ad-hoc fallback pairs. */
    fun launch(context: Context, intent: Intent, vararg fallbacks: Intent) =
        launch(context, listOf(intent, *fallbacks))

    private fun currentFamily(): SkinFamily = DeepLinkChains.skinFamilyFor(Build.MANUFACTURER)

    /** The Android-facing half of [ScreenSpec]: strings become a launchable Intent. */
    private fun ScreenSpec.toIntent(): Intent =
        (action?.let { Intent(it) } ?: Intent().setComponent(ComponentName(packageName!!, className!!)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
