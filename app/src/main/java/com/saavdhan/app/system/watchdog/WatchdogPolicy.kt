package com.saavdhan.app.system.watchdog

import com.saavdhan.app.domain.model.RiskLevel

/**
 * Pure logic for watchdog alerting. Watchdog v2 watches for TWO things:
 *
 *  1. **New installs** that are immediately dangerous (HIGH/CRITICAL).
 *  2. **Escalations** — an app that was already installed and looked harmless, but has since
 *     gained dangerous powers (e.g. the user was talked into enabling Accessibility days after
 *     install — the real SpyNote sequence).
 *
 * Alerts fire only on an *upward crossing into* HIGH/CRITICAL. An app that was already dangerous
 * last run does not re-alert (no notification spam). Extracted here, free of Android imports,
 * so every rule is unit-tested (WatchdogPolicyTest).
 */
object WatchdogPolicy {

    enum class AlertKind {
        /** A dangerous app was newly installed since the last check. */
        NEW_INSTALL,

        /** An already-installed app crossed up into HIGH/CRITICAL since the last check. */
        ESCALATION
    }

    data class Alert(val packageName: String, val kind: AlertKind)

    private val DANGEROUS = setOf(RiskLevel.HIGH, RiskLevel.CRITICAL)

    /**
     * Compare the last snapshot against the current assessment and decide what to alert on.
     *
     * @param known levels from the last run. `null` map = first ever run (baseline silently).
     *              A `null` *value* = package known but level unknown (migrated v1 snapshot) —
     *              baselined silently too, so an app upgrade never causes alert spam.
     * @param current package → risk level for everything installed right now.
     */
    fun assessChanges(
        known: Map<String, RiskLevel?>?,
        current: Map<String, RiskLevel>
    ): List<Alert> {
        if (known == null) return emptyList()
        return current.mapNotNull { (pkg, level) ->
            if (level !in DANGEROUS) return@mapNotNull null
            if (pkg !in known) return@mapNotNull Alert(pkg, AlertKind.NEW_INSTALL)
            val previous = known[pkg] ?: return@mapNotNull null // unknown old level: baseline, don't spam
            if (previous !in DANGEROUS) Alert(pkg, AlertKind.ESCALATION) else null
        }
    }

    // --- Snapshot (de)serialization — pure so it's testable -----------------------------------
    // v2 entries are "package:LEVEL"; v1 entries were bare package names (level unknown → null).
    // Package names never contain ':' so lastIndexOf is unambiguous.

    fun encode(levels: Map<String, RiskLevel>): Set<String> =
        levels.mapTo(mutableSetOf()) { "${it.key}:${it.value.name}" }

    fun decode(entries: Set<String>?): Map<String, RiskLevel?>? =
        entries?.associate { entry ->
            val i = entry.lastIndexOf(':')
            if (i < 0) {
                entry to null
            } else {
                entry.substring(0, i) to
                    runCatching { RiskLevel.valueOf(entry.substring(i + 1)) }.getOrNull()
            }
        }
}
