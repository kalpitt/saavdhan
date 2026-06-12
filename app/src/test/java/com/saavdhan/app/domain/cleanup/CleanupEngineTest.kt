package com.saavdhan.app.domain.cleanup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the reactive guided-cleanup engine. Pure logic — runs on your computer in milliseconds.
 * Each test sets up a phone state and checks which step is CURRENT and what's surfaced.
 */
class CleanupEngineTest {

    /** A fully-armed threat at the very start of cleanup; tests override what they care about. */
    private fun state(
        isInstalled: Boolean = true,
        hadAccessibility: Boolean = true,
        hasAccessibility: Boolean = true,
        wasDeviceAdmin: Boolean = true,
        isDeviceAdmin: Boolean = true,
        isIsolated: Boolean = false
    ) = CleanupState(
        isInstalled = isInstalled,
        hadAccessibility = hadAccessibility,
        hasAccessibility = hasAccessibility,
        wasDeviceAdmin = wasDeviceAdmin,
        isDeviceAdmin = isDeviceAdmin,
        isIsolated = isIsolated
    )

    private fun CleanupPlan.statusOf(id: CleanupStepId) = steps.firstOrNull { it.id == id }?.status
    private fun CleanupPlan.current() = steps.firstOrNull { it.status == StepStatus.CURRENT }?.id

    @Test
    fun `at the start, isolate the phone is the current step`() {
        val plan = CleanupEngine.plan(state())
        assertEquals(CleanupStepId.ISOLATE, plan.current())
        assertFalse(plan.threatRemoved)
    }

    @Test
    fun `once isolated, disabling accessibility is current`() {
        val plan = CleanupEngine.plan(state(isIsolated = true))
        assertEquals(StepStatus.DONE, plan.statusOf(CleanupStepId.ISOLATE))
        assertEquals(CleanupStepId.DISABLE_ACCESSIBILITY, plan.current())
    }

    @Test
    fun `after accessibility is off, removing admin is current`() {
        val plan = CleanupEngine.plan(state(isIsolated = true, hasAccessibility = false))
        assertEquals(StepStatus.DONE, plan.statusOf(CleanupStepId.DISABLE_ACCESSIBILITY))
        assertEquals(CleanupStepId.REMOVE_ADMIN, plan.current())
    }

    @Test
    fun `after admin removed, uninstall is current and Safe Mode is no longer pushed`() {
        val plan = CleanupEngine.plan(
            state(isIsolated = true, hasAccessibility = false, isDeviceAdmin = false)
        )
        assertEquals(StepStatus.DONE, plan.statusOf(CleanupStepId.REMOVE_ADMIN))
        assertEquals(CleanupStepId.UNINSTALL, plan.current())
        assertFalse(plan.showSafeModeEscalation)
    }

    @Test
    fun `a device-admin app that is still installed pushes Safe Mode`() {
        val plan = CleanupEngine.plan(state(isIsolated = true, hasAccessibility = false))
        assertTrue(plan.showSafeModeEscalation)
    }

    @Test
    fun `once uninstalled, secure-accounts is current and the threat is removed`() {
        val plan = CleanupEngine.plan(
            state(isInstalled = false, hasAccessibility = false, isDeviceAdmin = false, isIsolated = true)
        )
        assertEquals(StepStatus.DONE, plan.statusOf(CleanupStepId.UNINSTALL))
        assertEquals(CleanupStepId.SECURE_ACCOUNTS, plan.current())
        assertTrue(plan.threatRemoved)
        assertFalse(plan.showSafeModeEscalation)
    }

    @Test
    fun `a simple app with no special powers skips the accessibility and admin steps`() {
        val plan = CleanupEngine.plan(
            state(hadAccessibility = false, hasAccessibility = false, wasDeviceAdmin = false, isDeviceAdmin = false, isIsolated = true)
        )
        val ids = plan.steps.map { it.id }
        assertFalse(ids.contains(CleanupStepId.DISABLE_ACCESSIBILITY))
        assertFalse(ids.contains(CleanupStepId.REMOVE_ADMIN))
        assertEquals(CleanupStepId.UNINSTALL, plan.current())
    }

    @Test
    fun `an admin app that never had accessibility goes straight to removing admin`() {
        val plan = CleanupEngine.plan(
            state(hadAccessibility = false, hasAccessibility = false, isIsolated = true)
        )
        assertFalse(plan.steps.map { it.id }.contains(CleanupStepId.DISABLE_ACCESSIBILITY))
        assertEquals(CleanupStepId.REMOVE_ADMIN, plan.current())
    }
}
