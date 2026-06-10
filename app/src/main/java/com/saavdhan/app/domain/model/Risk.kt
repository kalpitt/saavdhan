package com.saavdhan.app.domain.model

/**
 * How worried should the user be about a given app? Ordered from calmest to most alarming.
 * `ordinal` (the position in this list) doubles as a sort key so the UI can show the scariest first.
 */
enum class RiskLevel {
    /** Nothing concerning found. */
    LOW,

    /** One mild clue. Probably fine, but worth a glance. */
    SUSPICIOUS,

    /** A combination that scam apps commonly show. Treat seriously. */
    HIGH,

    /** The classic spyware fingerprint. Almost certainly malicious. Act now. */
    CRITICAL,
}

/**
 * One specific reason an app looked risky. Each value is just an identifier — the actual words
 * shown to the user (in Hindi or English) live in the string resources, looked up in the UI layer.
 * This keeps the danger rules testable without dragging in Android.
 */
enum class RiskSignal {
    /** Holds an Accessibility Service — can read the screen and tap on the user's behalf. */
    ACCESSIBILITY,

    /** Is a Device Admin — gains powers that block uninstall. */
    DEVICE_ADMIN,

    /** Can read/receive SMS — where bank OTPs arrive. */
    SMS_ACCESS,

    /** Holds Notification Listener Service — can read every notification, including bank OTPs. */
    NOTIFICATION_LISTENER,

    /** Was sideloaded (installed from an APK, not a store). */
    SIDELOADED,

    /** Hides its icon from the launcher. */
    HIDDEN_ICON,

    /** Pretends to be a trusted system app. */
    IMPERSONATION,
}

/**
 * The verdict for one app: its [level], the [signals] that explain why, and whether we chose to
 * trust it via the allowlist (so the UI can say "we recognise this app").
 */
data class RiskAssessment(
    val level: RiskLevel,
    val signals: List<RiskSignal>,
    val allowlisted: Boolean,
)
