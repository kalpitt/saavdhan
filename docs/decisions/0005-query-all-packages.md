# ADR-0005 — QUERY_ALL_PACKAGES with graceful degradation

**Status:** Accepted · **Date:** 2026-06-09

## Context
To scan every installed app on Android 11+ (API 30+), an app must hold `QUERY_ALL_PACKAGES`.
Google Play restricts this permission, but **security/antivirus apps are an explicitly permitted
use case**. Distribution is currently undecided (Play and/or sideload).

## Decision
Declare `QUERY_ALL_PACKAGES`, justified as a security scanner. Design so the app **degrades
gracefully** if the permission is ever unavailable: it scans what it *can* see (accessibility
holders, active admins, resolvable packages) and shows an honest "scan may be incomplete" banner.

## Consequences
- ✅ Full-device scanning where allowed; still useful where limited.
- ✅ Being fully offline ([ADR-0001](0001-fully-offline-no-internet.md)) satisfies Play's
  "never sell/share installed-app data" condition automatically.
- ⚠️ A Play release requires the **Permissions Declaration Form** and may draw review scrutiny;
  failing to justify it can get an app removed.
- ⚠️ Sideloaded builds are unrestricted, but sideloading *this* app has its own trust cost
  (see [Roadmap → distribution](../10-roadmap.md)).

Sources: [Play Console — broad app visibility](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en),
[Package visibility filtering](https://developer.android.com/training/package-visibility).
