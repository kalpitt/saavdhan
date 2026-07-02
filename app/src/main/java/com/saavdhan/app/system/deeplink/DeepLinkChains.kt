package com.saavdhan.app.system.deeplink

/**
 * The pure "decision" half of deep-linking: WHICH settings screens to try, in WHAT order,
 * for WHICH phone maker. No Android types here — only strings and data classes — so every
 * chain is unit-testable on the JVM. [SettingsDeepLinks] is the "doing" half that turns
 * these specs into real Intents and launches them.
 *
 * Ordering rule: most specific screen first, degrading one honest step at a time, and every
 * chain ends in a universal Settings action that exists on every Android phone.
 */

/**
 * One launchable settings screen, described as plain strings.
 * Exactly one of [action] or ([packageName] + [className]) is set.
 */
data class ScreenSpec(
    val action: String? = null,
    val packageName: String? = null,
    val className: String? = null
) {
    /** True when this spec is a standard Android action (guaranteed to exist) rather than an OEM component (may not). */
    val isGenericAction: Boolean get() = action != null
}

/** Skin family = which company's modified Android this phone runs. Sub-brands share their parent's skin. */
enum class SkinFamily { ONE_UI, MIUI, COLOR_OS, FUNTOUCH, EMUI, GENERIC }

object DeepLinkChains {

    // Standard action strings (literal values of android.provider.Settings constants —
    // spelled out because this file must stay free of Android imports).
    const val ACTION_SECURITY_SETTINGS = "android.settings.SECURITY_SETTINGS"
    const val ACTION_SETTINGS = "android.settings.SETTINGS"

    /**
     * Maps Build.MANUFACTURER to a skin family. Sub-brands matter in India:
     * Poco/Redmi are Xiaomi (MIUI), Realme/OnePlus are Oppo-owned (ColorOS family), iQOO is Vivo (Funtouch).
     */
    fun skinFamilyFor(manufacturer: String?): SkinFamily =
        when (manufacturer?.trim()?.lowercase()) {
            "samsung" -> SkinFamily.ONE_UI
            "xiaomi", "poco", "redmi" -> SkinFamily.MIUI
            "oppo", "realme", "oneplus" -> SkinFamily.COLOR_OS
            "vivo", "iqoo" -> SkinFamily.FUNTOUCH
            "huawei", "honor" -> SkinFamily.EMUI
            else -> SkinFamily.GENERIC
        }

    /**
     * Screens where the user can deactivate a malicious Device Admin.
     * The AOSP device-admin activity survives on most skins (incl. One UI and MIUI); if a maker
     * removed it we land on the generic Security list instead of jumping all the way to Settings home.
     */
    fun deviceAdminChain(family: SkinFamily): List<ScreenSpec> {
        val aospDeviceAdminList = ScreenSpec(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings\$DeviceAdminSettingsActivity"
        )
        return listOf(
            aospDeviceAdminList,
            ScreenSpec(action = ACTION_SECURITY_SETTINGS),
            ScreenSpec(action = ACTION_SETTINGS)
        )
    }

    /**
     * Proprietary "auto-start / background app" manager screens, per skin, newest package first.
     * These components are undocumented and move between OS versions — that's exactly why each
     * family lists several known variants before falling back to plain Settings.
     */
    fun autoStartChain(family: SkinFamily): List<ScreenSpec> {
        val oemScreens = when (family) {
            SkinFamily.ONE_UI -> listOf(
                // Global Device Care first; sm_cn is the China-only build and must not lead.
                ScreenSpec(packageName = "com.samsung.android.sm", className = "com.samsung.android.sm.ui.battery.BatteryActivity"),
                ScreenSpec(packageName = "com.samsung.android.lool", className = "com.samsung.android.sm.ui.battery.BatteryActivity"),
                ScreenSpec(packageName = "com.samsung.android.sm_cn", className = "com.samsung.android.sm.ui.ram.AutoRunActivity")
            )
            SkinFamily.MIUI -> listOf(
                ScreenSpec(packageName = "com.miui.securitycenter", className = "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                ScreenSpec(packageName = "com.miui.securitycenter", className = "com.miui.securityscan.MainActivity")
            )
            SkinFamily.COLOR_OS -> listOf(
                // Newer Oppo/Realme/OnePlus ship under com.oplus; older ColorOS under com.coloros;
                // legacy OnePlus OxygenOS had its own security app.
                ScreenSpec(packageName = "com.oplus.safecenter", className = "com.oplus.safecenter.startupapp.StartupAppListActivity"),
                ScreenSpec(packageName = "com.coloros.safecenter", className = "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                ScreenSpec(packageName = "com.oneplus.security", className = "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")
            )
            SkinFamily.FUNTOUCH -> listOf(
                ScreenSpec(packageName = "com.vivo.permissionmanager", className = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                ScreenSpec(packageName = "com.iqoo.secure", className = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
            )
            SkinFamily.EMUI -> listOf(
                ScreenSpec(packageName = "com.huawei.systemmanager", className = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                ScreenSpec(packageName = "com.huawei.systemmanager", className = "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
            )
            SkinFamily.GENERIC -> emptyList()
        }
        return oemScreens + ScreenSpec(action = ACTION_SETTINGS)
    }
}
