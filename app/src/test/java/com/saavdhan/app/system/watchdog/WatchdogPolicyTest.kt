package com.saavdhan.app.system.watchdog

import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.system.watchdog.WatchdogPolicy.Alert
import com.saavdhan.app.system.watchdog.WatchdogPolicy.AlertKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the watchdog v2 diff-and-alert policy. Pure logic, no WorkManager. */
class WatchdogPolicyTest {

    @Test
    fun `first run establishes baseline without alerting, even on dangerous apps`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = null,
            current = mapOf("com.evil" to RiskLevel.CRITICAL, "com.ok" to RiskLevel.LOW)
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `newly installed CRITICAL app alerts as NEW_INSTALL`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.ok" to RiskLevel.LOW),
            current = mapOf("com.ok" to RiskLevel.LOW, "com.evil" to RiskLevel.CRITICAL)
        )
        assertEquals(listOf(Alert("com.evil", AlertKind.NEW_INSTALL)), alerts)
    }

    @Test
    fun `newly installed SUSPICIOUS app does not alert`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.ok" to RiskLevel.LOW),
            current = mapOf("com.ok" to RiskLevel.LOW, "com.meh" to RiskLevel.SUSPICIOUS)
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `existing app escalating from LOW to HIGH alerts as ESCALATION`() {
        // The real SpyNote sequence: installed quietly, armed later.
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.sleeper" to RiskLevel.LOW),
            current = mapOf("com.sleeper" to RiskLevel.HIGH)
        )
        assertEquals(listOf(Alert("com.sleeper", AlertKind.ESCALATION)), alerts)
    }

    @Test
    fun `existing app escalating from SUSPICIOUS to CRITICAL alerts as ESCALATION`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.sleeper" to RiskLevel.SUSPICIOUS),
            current = mapOf("com.sleeper" to RiskLevel.CRITICAL)
        )
        assertEquals(listOf(Alert("com.sleeper", AlertKind.ESCALATION)), alerts)
    }

    @Test
    fun `app already dangerous last run does not re-alert (no spam)`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.evil" to RiskLevel.HIGH),
            current = mapOf("com.evil" to RiskLevel.CRITICAL) // worse, but already alerted
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `migrated v1 entry (unknown old level) is baselined silently even if dangerous now`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.evil" to null), // v1 snapshot had only the name
            current = mapOf("com.evil" to RiskLevel.CRITICAL)
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `uninstalled packages simply disappear without alerting`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.gone" to RiskLevel.HIGH),
            current = emptyMap()
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `mixed run reports both kinds at once`() {
        val alerts = WatchdogPolicy.assessChanges(
            known = mapOf("com.sleeper" to RiskLevel.LOW, "com.ok" to RiskLevel.LOW),
            current = mapOf(
                "com.sleeper" to RiskLevel.CRITICAL,
                "com.ok" to RiskLevel.LOW,
                "com.fresh" to RiskLevel.HIGH
            )
        ).toSet()
        assertEquals(
            setOf(
                Alert("com.sleeper", AlertKind.ESCALATION),
                Alert("com.fresh", AlertKind.NEW_INSTALL)
            ),
            alerts
        )
    }

    // --- snapshot codec ------------------------------------------------------------------------

    @Test
    fun `encode then decode round-trips levels`() {
        val levels = mapOf("com.a" to RiskLevel.LOW, "com.b" to RiskLevel.CRITICAL)
        assertEquals(levels, WatchdogPolicy.decode(WatchdogPolicy.encode(levels)))
    }

    @Test
    fun `decode handles v1 bare package names as unknown level`() {
        val decoded = WatchdogPolicy.decode(setOf("com.old.app", "com.b:HIGH"))!!
        assertNull(decoded["com.old.app"])
        assertEquals(RiskLevel.HIGH, decoded["com.b"])
    }

    @Test
    fun `decode of null (never snapshotted) is null`() {
        assertNull(WatchdogPolicy.decode(null))
    }

    @Test
    fun `decode tolerates a corrupted level name`() {
        val decoded = WatchdogPolicy.decode(setOf("com.x:NOT_A_LEVEL"))!!
        assertTrue("com.x" in decoded)
        assertNull(decoded["com.x"])
    }
}
