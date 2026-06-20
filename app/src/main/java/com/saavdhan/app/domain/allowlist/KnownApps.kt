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
        // "Update / system" disguises — no normal user-installed app is ever named these.
        "system update" to null,
        "system update available" to null,
        "android update" to null,
        "update" to null,
        "update available" to null,
        "update now" to null,
        "update service" to null,
        "software update" to null,
        "system" to null,
        "android system" to null,
        "system service" to null,
        "system services" to null,
        "android services" to null,
        "system notification" to null,
        "system upgrade" to null,
        "android upgrade" to null,
        "phone update" to null,
        "device update" to null,
        "firmware update" to null,
        // "Security" disguises — a favourite of fake "your phone is infected" droppers.
        "security update" to null,
        "system security" to null,
        "android security update" to null,
        "play protect" to "com.google.android.gms",
        "google play protect" to "com.google.android.gms",
        // Real Google/Android components — only their genuine package may carry the name.
        "google play services" to "com.google.android.gms",
        "play services" to "com.google.android.gms",
        "google services" to "com.google.android.gms",
        "google play store" to "com.android.vending",
        "play store" to "com.android.vending",
        "google" to "com.google.android.googlequicksearchbox",
        "carrier services" to "com.google.android.ims",
        "android system webview" to "com.google.android.webview",
        "system webview" to "com.google.android.webview",
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
     * SHA-256 hashes of the signing certificates of trusted developers (Google, Samsung, Meta, Top Banks).
     * If an app's signature matches one of these, it is unconditionally trusted.
     */
    val TRUSTED_SIGNATURES: Set<String> = setOf(
        "5F2391277B1DBD489000467E4C2FA6AF802430080457DCE2F618992E9DFB5402", // Google Play Services
        "FB920D381BEE1B2093F27DC8F13D994DA629DC91887D0529B35C9A2DC4F4A6C2", // WhatsApp
        "911D604446084CA7F4760B775BFC160FA8702441240A7258645D7A72C4312D27", // Facebook / Meta
        "C204492D7445D35231B2CF32B4F0693BB14CD648BF973AC912A2C13D230D4EFC", // ICICI Bank
        "E64EF4A810B911497B4DA58FBBC665CF161EF683015C5F4CF04713A3392E34D1", // Axis Bank
        "9555E7656EFBE1D85BA264B30FDE10068F678F96B45D3EDB70F4283E4678D8DC" // SBI (YONO/Freedom)
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
     *
     * Matching is done on a [normalize]d label so that punctuation, emoji, and runs of whitespace
     * a scammer adds to dodge an exact match ("System  Update!", "System‑Update ⬇️") still resolve
     * to the same key.
     */
    fun isImpersonating(
        label: String,
        packageName: String,
        isSystemApp: Boolean,
        installSource: com.saavdhan.app.domain.model.InstallSource
    ): Boolean {
        if (isSystemApp) return false
        val key = normalize(label)
        if (IMPERSONATED_LABELS.containsKey(key)) {
            val realOwner = IMPERSONATED_LABELS[key]
            return realOwner != packageName
        }

        // Run heavy fuzzy matching only for sideloaded apps to save CPU on background scans
        if (installSource == com.saavdhan.app.domain.model.InstallSource.SIDELOADED && key.length > 4) {
            for ((impersonated, realOwner) in IMPERSONATED_LABELS) {
                val maxDistance = if (impersonated.length > 8) 2 else 1
                if (levenshtein(key, impersonated) <= maxDistance) {
                    return realOwner != packageName
                }
            }
        }
        return false
    }

    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var v0 = IntArray(s2.length + 1) { it }
        var v1 = IntArray(s2.length + 1)

        for (i in s1.indices) {
            v1[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                v1[j + 1] = minOf(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost)
            }
            val temp = v0
            v0 = v1
            v1 = temp
        }
        return v0[s2.length]
    }

    /**
     * Lower-case, then collapse every run of non-alphanumeric characters (spaces, punctuation,
     * emoji, separators) into a single space and trim. "  System‑Update! ⬇️ " -> "system update".
     * Non-Latin labels reduce to "" and simply won't match any (English) impersonation key.
     */
    private fun normalize(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
