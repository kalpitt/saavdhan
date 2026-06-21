# ADR-0014 — "Send to family" uses the OS share sheet, not a network

**Status:** Accepted · **Date:** 2026-06-21

## Context
The real unit of protection against family-targeting scams is a *relationship*, not a device: the
person who needs Saavdhan (an elderly target) is usually not the person who installs and understands
it (a worried adult child, often in another city). Until now the product had no way for that remote
protector to see what a scan found — the watchdog notification and the results screen only ever
reach the person holding the (possibly compromised) phone, who mid-scam is the least able to act.

The obvious "solution" — accounts, a caregiver dashboard, push to a second device, cloud sync — is
**categorically forbidden**: Saavdhan never touches the internet ([ADR-0001](0001-fully-offline-no-internet.md)).

## Decision
Add a **"Send result to family"** button on the scan-results screen (shown in both the all-clear and
threats-found states). It builds a plain-language summary of the scan from data already in memory and
hands it to the phone's **own share sheet** via a standard `Intent.ACTION_SEND` (`text/plain`). The
family's existing trusted channel — WhatsApp, SMS, anything — carries the message. Saavdhan itself
sends nothing over any network.

- The receipt is written as a human message ("Saavdhan checked this phone… These apps need attention:
  • System Update — Very dangerous… Please check on whoever uses this phone."), bilingual, assembled
  from the existing risk labels and app names. For a clean scan it's the "all clear" reassurance an
  adult child can ask their parent to send on demand.
- The assembly is a **pure function** (`buildFamilyReport`) with no Android dependency, unit-tested on
  the JVM (clean vs flagged states, never mixing the safe line with the threat advice).
- **No recipient is stored.** The user picks who to send to in the OS share sheet each time. This keeps
  the privacy surface at zero (no new permission, no contacts access, no persisted personal data) and
  respects the minimal-permissions promise ([ADR-0003 layered](0004-layered-architecture.md), privacy
  non-negotiable).

## Consequences
- ✅ The remote protector can finally see and act on what the phone found — the product's deepest
  structural gap — with zero infrastructure and zero internet.
- ✅ Reuses the channel families already trust (WhatsApp), rather than asking them to trust a new one.
- ✅ Fully deterministic and offline; nothing leaves the device except what the user explicitly shares.
- ⚠️ We can't guarantee what the user does with the text after sharing (that's true of any share). The
  receipt contains only app names + risk levels already visible on-screen — no sensitive personal data.
- ⚠️ No stored caregiver means one extra tap (pick the contact) each time. Deliberate: storing a
  contact would add a privacy surface we chose not to open. Revisit only if families ask for it.

See `ui/scan/FamilyReport.kt`, `FamilyReportTest.kt`, and the share button in `ScanScreen.kt`.
