package com.saavdhan.app.domain.cleanup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Timing rules for the Uninstall step's "did it work?" feedback. */
class UninstallWatchTest {

    @Test
    fun `gone is REMOVED at any moment, even instantly`() {
        assertEquals(UninstallPhase.REMOVED, UninstallWatch.phase(0, stillInstalled = false))
        assertEquals(UninstallPhase.REMOVED, UninstallWatch.phase(60_000, stillInstalled = false))
    }

    @Test
    fun `still installed right after return is CHECKING, never a failure`() {
        assertEquals(UninstallPhase.CHECKING, UninstallWatch.phase(0, stillInstalled = true))
        assertEquals(UninstallPhase.CHECKING, UninstallWatch.phase(1_999, stillInstalled = true))
    }

    @Test
    fun `still installed after 2s is NOT_YET, so a cancel isn't stuck waiting`() {
        assertEquals(UninstallPhase.NOT_YET, UninstallWatch.phase(2_000, stillInstalled = true))
        assertEquals(UninstallPhase.NOT_YET, UninstallWatch.phase(11_999, stillInstalled = true))
    }

    @Test
    fun `still installed after 12s is NOT_REMOVED`() {
        assertEquals(UninstallPhase.NOT_REMOVED, UninstallWatch.phase(12_000, stillInstalled = true))
    }

    @Test
    fun `attempt is watched only while fresh`() {
        assertFalse(UninstallWatch.isAttemptFresh(null, 1_000))
        assertTrue(UninstallWatch.isAttemptFresh(1_000, 1_000))
        assertTrue(UninstallWatch.isAttemptFresh(0, UninstallWatch.ATTEMPT_EXPIRY_MS - 1))
        assertFalse(UninstallWatch.isAttemptFresh(0, UninstallWatch.ATTEMPT_EXPIRY_MS))
        // A clock that went backwards (e.g. after reboot) is not a fresh attempt.
        assertFalse(UninstallWatch.isAttemptFresh(5_000, 1_000))
    }
}
