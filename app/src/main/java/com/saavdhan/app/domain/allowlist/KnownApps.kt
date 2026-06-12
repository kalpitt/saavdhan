package com.saavdhan.app.domain.allowlist

/**
 * Reference lists used to (a) recognise apps we trust and (b) spot apps faking a trusted name.
 * Kept as plain data so both the scanner and our tests can use it.
 */
object KnownApps {

    /**
     * Names that scam apps love to wear as a disguise, mapped to the REAL package that is allowed
     * to use that name. A `null` value means NO normal user-installed app should ever carry this
     * name, so seeing it is a strong red flag.
     *
     * Compare using lower-cased, trimmed labels (see [isImpersonating]).
     */
    val IMPERSONATED_LABELS: Map<String, String?> = mapOf(
        "system update" to null,
        "android update" to null,
        "update" to null,
        "software update" to null,
        "system" to null,
        "android system" to null,
        "system service" to null,
        "google play services" to "com.google.android.gms",
        "play services" to "com.google.android.gms",
        "google services" to "com.google.android.gms",
        "google play store" to "com.android.vending",
        "play store" to "com.android.vending",
        "google" to "com.google.android.googlequicksearchbox",
        "settings" to "com.android.settings",
        "chrome" to "com.android.chrome"
    )

    /**
     * Packages we explicitly trust even though they legitimately hold powerful permissions
     * (e.g. screen readers that genuinely need Accessibility). Prevents needless alarm.
     */
    val TRUSTED_PACKAGES: Set<String> = setOf(
        "com.google.android.marvin.talkback", // TalkBack screen reader
        "com.android.vending", // Google Play Store
        "com.google.android.gms", // Google Play Services
        "com.huawei.health", // Huawei Health
        "com.microsoft.windowsintune.companyportal" // Company Portal
    )

    /**
     * Package name prefixes we trust, for store-installed apps that start with these.
     * Prevents flagging icon-less Google/Samsung helper modules.
     */
    val TRUSTED_PREFIXES: Set<String> = setOf(
        "com.google.android.",
        "com.samsung.android.",
        "com.samsung.accessory."
    )

    /**
     * Returns true if [packageName] is trusted either by exact match or by prefix (e.g. Play-installed
     * Google/Samsung helpers). Only applies to store-installed apps (not sideloaded).
     */
    fun isTrustedPackage(packageName: String, installSource: com.saavdhan.app.domain.model.InstallSource): Boolean {
        if (installSource == com.saavdhan.app.domain.model.InstallSource.SIDELOADED) return false
        return packageName in TRUSTED_PACKAGES || TRUSTED_PREFIXES.any { packageName.startsWith(it) }
    }

    /**
     * Returns true if [label] matches a commonly-impersonated name but [packageName] is NOT the
     * real package allowed to use it. System apps are never treated as impersonators.
     */
    fun isImpersonating(label: String, packageName: String, isSystemApp: Boolean): Boolean {
        if (isSystemApp) return false
        val key = label.trim().lowercase()
        if (!IMPERSONATED_LABELS.containsKey(key)) return false
        val realOwner = IMPERSONATED_LABELS[key]
        // null owner -> no app should use this name; otherwise it's only OK if the package matches.
        return realOwner != packageName
    }
}
