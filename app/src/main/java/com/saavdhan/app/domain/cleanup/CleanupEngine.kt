package com.saavdhan.app.domain.cleanup

/**
 * Turns the phone's live [CleanupState] into an ordered, reactive checklist. Pure logic, fully
 * tested (CleanupEngineTest) — no Android, no emulator needed.
 *
 * The order is deliberate: **isolate** the phone first (stop data theft), then strip the powers
 * that block removal (**accessibility**, then **device admin**), then **uninstall**, then once the
 * app is gone, **secure accounts**. Steps that don't apply to a given app are simply omitted.
 */
object CleanupEngine {

    fun plan(state: CleanupState): CleanupPlan {
        // (stepId -> isDone) in canonical order; only the steps that apply to this app.
        val ordered = buildList {
            add(CleanupStepId.ISOLATE to state.isIsolated)
            if (state.hadAccessibility) {
                add(CleanupStepId.DISABLE_ACCESSIBILITY to !state.hasAccessibility)
            }
            if (state.wasDeviceAdmin) {
                add(CleanupStepId.REMOVE_ADMIN to !state.isDeviceAdmin)
            }
            add(CleanupStepId.UNINSTALL to !state.isInstalled)
            // Account-security advice only appears once the threat is actually gone, and is never
            // auto-marked "done" — it's the user's final action.
            if (!state.isInstalled) add(CleanupStepId.SECURE_ACCOUNTS to false)
        }

        // The first not-yet-done step is CURRENT; everything after it is PENDING.
        var currentTaken = false
        val steps = ordered.map { (id, done) ->
            when {
                done -> CleanupStep(id, StepStatus.DONE)
                !currentTaken -> {
                    currentTaken = true
                    CleanupStep(id, StepStatus.CURRENT)
                }
                else -> CleanupStep(id, StepStatus.PENDING)
            }
        }

        // A Device-Admin app that's still installed is the classic "resists uninstall" case —
        // Safe Mode is the reliable way out, so we surface it prominently.
        val showSafeMode = state.isInstalled && state.isDeviceAdmin

        return CleanupPlan(
            steps = steps,
            showSafeModeEscalation = showSafeMode,
            threatRemoved = !state.isInstalled
        )
    }
}
