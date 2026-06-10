package com.saavdhan.app.system.watchdog

import com.saavdhan.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Characterization tests for the watchdog diff-and-alert policy. Pure logic, no WorkManager. */
class WatchdogPolicyTest {

    @Test
    fun `first run establishes baseline without alerting`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = null,
            currentPackages = setOf("com.evil.app", "com.normal.app"),
            getRiskLevel = { pkg ->
                when (pkg) {
                    "com.evil.app" -> RiskLevel.CRITICAL
                    "com.normal.app" -> RiskLevel.LOW
                    else -> null
                }
            },
        )
        assertTrue("First run should not alert on baseline apps", result.isEmpty())
    }

    @Test
    fun `new CRITICAL package triggers alert`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = setOf("com.safe.app"),
            currentPackages = setOf("com.safe.app", "com.evil.critical"),
            getRiskLevel = { pkg ->
                when (pkg) {
                    "com.safe.app" -> RiskLevel.LOW
                    "com.evil.critical" -> RiskLevel.CRITICAL
                    else -> null
                }
            },
        )
        assertEquals(listOf("com.evil.critical"), result)
    }

    @Test
    fun `new HIGH package triggers alert`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = setOf("com.safe.app"),
            currentPackages = setOf("com.safe.app", "com.evil.high"),
            getRiskLevel = { pkg ->
                when (pkg) {
                    "com.safe.app" -> RiskLevel.LOW
                    "com.evil.high" -> RiskLevel.HIGH
                    else -> null
                }
            },
        )
        assertEquals(listOf("com.evil.high"), result)
    }

    @Test
    fun `new SUSPICIOUS package does not alert`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = setOf("com.safe.app"),
            currentPackages = setOf("com.safe.app", "com.suspicious.app"),
            getRiskLevel = { pkg ->
                when (pkg) {
                    "com.safe.app" -> RiskLevel.LOW
                    "com.suspicious.app" -> RiskLevel.SUSPICIOUS
                    else -> null
                }
            },
        )
        assertTrue("SUSPICIOUS apps should not alert", result.isEmpty())
    }

    @Test
    fun `multiple new HIGH packages all alert`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = setOf("com.safe.app"),
            currentPackages = setOf("com.safe.app", "com.evil1", "com.evil2", "com.suspicious.app"),
            getRiskLevel = { pkg ->
                when (pkg) {
                    "com.evil1" -> RiskLevel.HIGH
                    "com.evil2" -> RiskLevel.CRITICAL
                    "com.suspicious.app" -> RiskLevel.SUSPICIOUS
                    else -> RiskLevel.LOW
                }
            },
        )
        assertEquals(setOf("com.evil1", "com.evil2"), result.toSet())
    }

    @Test
    fun `unassessable package (null risk) does not crash or alert`() {
        val result = WatchdogPolicy.newlyInstalledThreats(
            knownPackages = setOf("com.safe.app"),
            currentPackages = setOf("com.safe.app", "com.unassessable"),
            getRiskLevel = { null }, // Can't assess any package
        )
        assertTrue("Unassessable packages should be skipped", result.isEmpty())
    }
}
