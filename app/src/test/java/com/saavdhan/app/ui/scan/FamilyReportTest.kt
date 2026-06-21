package com.saavdhan.app.ui.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the family-share report text. Pure assembly, runs on the JVM in milliseconds. We check
 * the two states a family member must be able to read at a glance: "all clear" vs "these apps need
 * attention", and that we never mix the two (no advice line on a clean phone, no safe line when
 * threats exist).
 */
class FamilyReportTest {

    private fun report(appLines: List<String>) = buildFamilyReport(
        intro = "Saavdhan checked this phone.",
        appLines = appLines,
        safeLine = "All clear.",
        foundHeader = "These apps need attention:",
        advice = "Please check on whoever uses this phone.",
        footer = "Sent from Saavdhan."
    )

    @Test
    fun `clean scan reads as all-clear and omits the threat sections`() {
        val out = report(emptyList())
        assertTrue(out.contains("Saavdhan checked this phone."))
        assertTrue(out.contains("All clear."))
        assertTrue(out.contains("Sent from Saavdhan."))
        assertFalse(out.contains("These apps need attention:"))
        assertFalse(out.contains("Please check on whoever uses this phone.")) // no advice when clean
    }

    @Test
    fun `flagged scan lists every app and the advice, and omits the safe line`() {
        val out = report(listOf("• System Update — Very dangerous", "• Fast Cash Loan — Very dangerous"))
        assertTrue(out.contains("These apps need attention:"))
        assertTrue(out.contains("• System Update — Very dangerous"))
        assertTrue(out.contains("• Fast Cash Loan — Very dangerous"))
        assertTrue(out.contains("Please check on whoever uses this phone."))
        assertTrue(out.contains("Sent from Saavdhan."))
        assertFalse(out.contains("All clear.")) // no safe line when threats exist
    }

    @Test
    fun `intro always leads and footer always trails`() {
        val out = report(listOf("• X — Dangerous"))
        assertTrue(out.startsWith("Saavdhan checked this phone."))
        assertTrue(out.trimEnd().endsWith("Sent from Saavdhan."))
    }
}
