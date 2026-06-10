# ADR-0001 — Fully offline: never declare the INTERNET permission

**Status:** Accepted · **Date:** 2026-06-09

## Context
Saavdhan is a security app for non-technical users, often handling an actively-infected phone.
Trust is everything, and the simplest, most verifiable privacy promise is "it can't send your
data anywhere." Android enforces network access through the `INTERNET` permission: an app that
never declares it is *physically prevented by the OS* from opening any connection.

## Decision
Do **not** declare `android.permission.INTERNET` (or any networking permission) anywhere. All
detection logic and rule data ship inside the app. Rules update only via new app versions.

## Consequences
- ✅ "We never connect to the internet" becomes a provable, OS-enforced guarantee, not a policy.
- ✅ Trivially satisfies Play's "don't sell installed-app data" rule — there's nowhere to send it.
- ✅ Eliminates a whole class of vulnerabilities (exfiltration, MITM, malicious server).
- ⚠️ No live threat-intelligence updates; the impersonation list and rules are only as fresh as
  the installed version. Accepted trade-off.
- ⚠️ No crash/analytics reporting. We rely on local testing and user reports instead.

## Verification
`grep -i "android.permission.INTERNET" app/build/intermediates/merged_manifests/debug/AndroidManifest.xml`
must return nothing. Part of the release checklist in [Security & Privacy](../05-security-and-privacy.md).
