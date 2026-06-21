package com.saavdhan.app.ui.scan

/**
 * Assembles the plain-language scan summary a user can send to a worried family member through the
 * phone's own share sheet (WhatsApp/SMS) — the offline way to let the person who set Saavdhan up
 * (often a remote adult child) see what a scan found. Pure string assembly, no Android, so the
 * content is unit-tested on the JVM; the caller supplies the already-localised pieces and the
 * per-app lines (formatted "• Name — Very dangerous"). See ADR-0014.
 */
internal fun buildFamilyReport(
    intro: String,
    appLines: List<String>,
    safeLine: String,
    foundHeader: String,
    advice: String,
    footer: String
): String {
    val sb = StringBuilder()
    sb.append(intro).append("\n\n")
    if (appLines.isEmpty()) {
        sb.append(safeLine)
    } else {
        sb.append(foundHeader).append("\n")
        appLines.forEach { sb.append(it).append("\n") }
        sb.append("\n").append(advice)
    }
    sb.append("\n\n").append(footer)
    return sb.toString()
}
