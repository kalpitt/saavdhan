package com.saavdhan.app.domain.cleanup

/**
 * What the Uninstall step shows after the user comes back from Android's uninstall dialog.
 *
 * Android often finishes removing the app a second or two AFTER our screen is back, so one check
 * on return isn't enough. We keep checking for a short while and show honest, calm progress.
 */
enum class UninstallPhase {
    /** No attempt in flight: the normal "Uninstall this app" card. */
    IDLE,

    /** Just came back: spinner, buttons briefly disabled. */
    CHECKING,

    /** Still installed but still checking; true whether the user cancelled or removal is slow. */
    NOT_YET,

    /** Gave up waiting: point the user at the App info route instead. */
    NOT_REMOVED,

    /** Confirmed gone. */
    REMOVED
}

/** Pure timing rules for [UninstallPhase], so they're unit-tested without a phone. */
object UninstallWatch {
    /** Until this long after returning, we only show "checking". */
    const val CHECKING_MS = 2_000L

    /** After this long, stop checking and say it wasn't removed. */
    const val GIVE_UP_MS = 12_000L

    /** How often to re-check while watching. */
    const val POLL_MS = 500L

    /** How long the "removed" confirmation stays before the checklist moves on. */
    const val REMOVED_HOLD_MS = 1_500L

    /** A pending attempt older than this is forgotten: coming back later is not a failed attempt. */
    const val ATTEMPT_EXPIRY_MS = 5 * 60_000L

    /** The phase [elapsedSinceReturnMs] after the user came back, given whether the app is still there. */
    fun phase(elapsedSinceReturnMs: Long, stillInstalled: Boolean): UninstallPhase = when {
        !stillInstalled -> UninstallPhase.REMOVED
        elapsedSinceReturnMs < CHECKING_MS -> UninstallPhase.CHECKING
        elapsedSinceReturnMs < GIVE_UP_MS -> UninstallPhase.NOT_YET
        else -> UninstallPhase.NOT_REMOVED
    }

    /** True if a tap on Uninstall (or App info) at [attemptAtMs] should be watched at [nowMs]. */
    fun isAttemptFresh(attemptAtMs: Long?, nowMs: Long): Boolean =
        attemptAtMs != null && nowMs - attemptAtMs in 0 until ATTEMPT_EXPIRY_MS
}
