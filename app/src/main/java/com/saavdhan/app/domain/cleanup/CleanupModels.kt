package com.saavdhan.app.domain.cleanup

/**
 * The guided-cleanup model. Pure, Android-free, and driven by the phone's *live* state so the
 * checklist reacts to what the user has actually done. See [CleanupEngine].
 */

/** The ordered steps of a cleanup. Not all apply to every app (see [CleanupEngine.plan]). */
enum class CleanupStepId {
    /** Turn on airplane mode so the spyware can't send your data while you work. */
    ISOLATE,

    /** Turn off the app's Accessibility (it can watch your screen). */
    DISABLE_ACCESSIBILITY,

    /** Remove the app's Device-Admin power (this is what blocks uninstall). */
    REMOVE_ADMIN,

    /** Uninstall the app. */
    UNINSTALL,

    /** After the app is gone: secure your accounts. */
    SECURE_ACCOUNTS,
}

enum class StepStatus {
    /** Already handled (the live state confirms it). */
    DONE,

    /** The one step to do right now. */
    CURRENT,

    /** Comes later. */
    PENDING,
}

data class CleanupStep(val id: CleanupStepId, val status: StepStatus)

/**
 * Live facts about the phone and the target app. Re-read every time the cleanup screen resumes,
 * so progress is detected automatically. `had*`/`was*` = what the original scan saw (so a step
 * still appears, and can be ticked off, after the user turns the power off).
 */
data class CleanupState(
    val isInstalled: Boolean,
    val hadAccessibility: Boolean,
    val hasAccessibility: Boolean,
    val wasDeviceAdmin: Boolean,
    val isDeviceAdmin: Boolean,
    val isIsolated: Boolean,
)

data class CleanupPlan(
    /** The ordered, status-tagged steps to show. */
    val steps: List<CleanupStep>,
    /** Surface the Safe Mode escalation prominently (the app resists normal removal). */
    val showSafeModeEscalation: Boolean,
    /** True once the malicious app is no longer installed. */
    val threatRemoved: Boolean,
)
