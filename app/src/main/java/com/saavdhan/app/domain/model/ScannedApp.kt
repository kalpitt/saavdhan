package com.saavdhan.app.domain.model

/**
 * A plain, Android-free description of ONE installed app and the facts we managed to read
 * about it. The scanner (which DOES touch Android) fills this in; the [com.saavdhan.app.domain.risk.RiskEngine]
 * (which is pure logic) reads it.
 *
 * Keeping this class free of Android types is deliberate: it lets us test our danger rules
 * on a normal computer, with made-up apps, without an emulator.
 */
data class ScannedApp(
    /** The app's unique id, e.g. "com.whatsapp". Two apps can never share this. */
    val packageName: String,

    /** The human-visible name, e.g. "WhatsApp" or "System Update". */
    val label: String,

    /** Where this app came from. The single most useful clue for telling scam from normal. */
    val installSource: InstallSource,

    /** True if it's part of the phone's built-in software (came with the phone, not installed by the user). */
    val isSystemApp: Boolean,

    /** True if this app currently holds an Accessibility Service that is switched ON. */
    val hasAccessibilityEnabled: Boolean,

    /** True if this app is registered as a Device Admin (the power that resists uninstall). */
    val isDeviceAdmin: Boolean,

    /** True if this app asked for the ability to read or receive text messages (where OTPs arrive). */
    val requestsSms: Boolean,

    /** True if the SMS permission above is actually granted (not just requested). */
    val smsGranted: Boolean,

    /** True if this app is registered as a Notification Listener (can read all notifications, including bank OTPs). */
    val hasNotificationListener: Boolean = false,

    /** True if the app has NO icon in the launcher — i.e. it's hiding from the user. */
    val hasHiddenIcon: Boolean,

    /**
     * True if the app's name pretends to be a trusted system component (e.g. "Google Play Services",
     * "System Update") while NOT actually being that real, signed system app.
     */
    val impersonatesSystemApp: Boolean,

    /** When the app was first installed, in epoch milliseconds. Used only for "this appeared recently" hints. */
    val firstInstallTimeMillis: Long,

    /** The SHA-256 hashes of the app's signing certificates. Used to verify trusted developers. */
    val signatureHashes: Set<String> = emptySet()
)

/** How an app arrived on the phone. */
enum class InstallSource {
    /** Installed from the official Google Play Store. Strong signal of legitimacy. */
    PLAY_STORE,

    /** Installed from another recognised app store (e.g. Galaxy Store, Amazon). */
    OTHER_STORE,

    /** "Sideloaded" — installed from an APK file outside any store (WhatsApp, a browser, a file
     *  manager). This is exactly how the scam apps spread, so it raises suspicion. */
    SIDELOADED,

    /** Built into the phone's system. */
    SYSTEM,

    /** We could not determine the source. */
    UNKNOWN
}
