package com.saavdhan.app.ui.scan

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    footer: String,
    timestampMillis: Long = System.currentTimeMillis()
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

    val df = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
    df.timeZone = TimeZone.getDefault()
    val dateStr = df.format(Date(timestampMillis))

    val contentString = appLines.joinToString("") + timestampMillis.toString()
    val hash = Integer.toHexString(contentString.hashCode()).takeLast(4).uppercase(Locale.US).padStart(4, '0')
    val refId = "SVDN-$dateStr-$hash"

    sb.append("\n\n").append(footer)
    sb.append("\n").append("Ref: ").append(refId)
    return sb.toString()
}
