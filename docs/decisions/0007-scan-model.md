# ADR-0007 — On-demand scan + background watchdog; no own Accessibility service

**Status:** Accepted · **Date:** 2026-06-09

## Context
How aggressively should the app watch for threats? Options ranged from purely on-demand scanning,
to a background watcher, to the app running its *own* Accessibility service for deep real-time
monitoring. There's an irony in a scam-detector asking the user to grant Accessibility — the very
permission we warn about.

## Decision
Two layers, no more:
1. **On-demand scan** — the user taps "Scan." Primary, predictable, no battery cost.
2. **Background new-app alert** — a **WorkManager** periodic job (plus a one-off scan each time the
   app opens) diffs the installed-app list against a saved snapshot; each *newly* installed package
   is risk-checked and, if dangerous, a notification is posted.

We deliberately do **not** create our own Accessibility service.

## Revision (corrected during implementation)
The first implementation used a **manifest `BroadcastReceiver` for `PACKAGE_ADDED`**. Live testing
on an API-35 emulator proved this does **not** work: the OS reports
`skipped by policy at enqueue: Background execution not allowed` — since Android 8, a background
manifest receiver does not receive `PACKAGE_ADDED` (it only fires while the app is foreground).
The receiver was removed and replaced with the WorkManager approach above. *(Lesson: verify
background behaviour on-device; the docs' "exemption" list does not cover this case the way we
assumed.)*

## Consequences
- ✅ Reliably catches newly-installed dangerous apps in the background (within the ~15-min periodic
   window) **and** promptly whenever the user opens the app.
- ✅ No persistent "protection running" foreground-service notification — keeps the UX calm.
- ✅ Avoids the trust paradox and complexity of holding Accessibility ourselves.
- ⚠️ Not truly instant in the background (WorkManager's minimum periodic interval is 15 min) — an
   acceptable trade vs. a persistent foreground service.
- ⚠️ `POST_NOTIFICATIONS` runtime permission needed on Android 13+.

*Status note:* **Built & tested live.** `NewAppScanWorker` + `Watchdog` (scheduler) +
`InstalledAppsSnapshot` + `ThreatNotifier`; scheduled from `MainActivity.onAppOpen`. Verified on an
emulator: installing a decoy "System Update" app then reopening Saavdhan raises the danger
notification.
