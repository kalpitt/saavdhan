# 07 — Testing Strategy

Our testing follows the standard pyramid: lots of fast logic tests at the bottom, fewer
slow device tests at the top. The most valuable tests live where the most important logic does —
the **danger brain**.

## What we test today

### Unit tests — domain core ✅ *done*
**42 tests across 3 files**, all passing. Because the `domain` layer is pure Kotlin with **no Android dependencies**, these run on your computer in ~30 milliseconds, no emulator needed.

- **[RiskEngineTest.kt](../app/src/test/java/com/saavdhan/app/domain/risk/RiskEngineTest.kt)** — 22 tests locking in detection rules:
  - the spyware trinity (Accessibility + Device Admin + SMS) ⇒ `CRITICAL`
  - sideloaded + accessibility ⇒ `HIGH`; trusted-package spoofing with powers ⇒ `HIGH`; impersonation ⇒ `HIGH`
  - SMS_REQUESTED signal (sideloaded + asks for SMS) ⇒ `HIGH` when paired with accessibility/admin
  - a Play-Store app with accessibility ⇒ only `SUSPICIOUS` (don't scare legit users)
  - SMS access alone ⇒ `LOW` (too common to flag)
  - system apps and trusted packages ⇒ `LOW` + allowlisted (with power re-check for spoofing)
  - a clean app ⇒ `LOW`
- **[CleanupEngineTest.kt](../app/src/test/java/com/saavdhan/app/domain/cleanup/CleanupEngineTest.kt)** — 8 tests for the reactive cleanup flow:
  - step ordering (isolate → disable access → remove admin → uninstall → secure accounts)
  - state transitions as the user completes steps
  - Safe Mode escalation when Device Admin resists uninstall
- **[KnownAppsTest.kt](../app/src/test/java/com/saavdhan/app/domain/allowlist/KnownAppsTest.kt)** — 12 tests for allowlist & impersonation:
  - impersonation detection (label matching, case/space insensitivity, system-app bypass)
  - trusted-package lookup (exact matches, prefixes like `com.google.android.*`)
  - sideload-blocking for prefix-trusted packages

**Rule:** you may not change a detection rule or allowlist behaviour without adding/updating a test.
This is guardrail #1 in [Coding Standards](06-coding-standards.md).

**Rule:** you may not change a detection rule without adding/updating a test that proves the new
behaviour. This is guardrail #1 in [Coding Standards](06-coding-standards.md).

### Manual demo mode — exercising the UI ✅ *done*
Debug builds inject two fake threats (`AppScanner.demoApps()`), so the danger screens are visible
on a clean emulator that has no real malware. This is for *seeing* the UI, not automated testing.

### Live watchdog test — the `decoyapp` fixture ✅ *done*
`decoyapp/` is a **harmless** test app (no code; labelled "System Update"; no launcher icon). It
exists only as a real detection target. Installing it makes the background `NewAppScanWorker`
flag it (impersonation + hidden-icon + sideloaded ⇒ HIGH) and post the *"A new app looks
dangerous"* notification — verified on the emulator (`docs/screenshots/08-watchdog-notification.png`).
This is how we caught that a manifest `PACKAGE_ADDED` receiver is blocked in the background and
switched to WorkManager ([ADR-0007](decisions/0007-scan-model.md)).

## Bugs caught by live testing (why "it compiles" isn't enough)

Live, on-device testing during Phase 1 caught three real issues a green build had hidden — exactly
the kind of latent failure that sinks "vibe-coded" apps:

1. **Watchdog silently dead in the background.** The first watchdog used a manifest `PACKAGE_ADDED`
   receiver. It compiled and looked right, but Android logged `Background execution not allowed`
   and never delivered the broadcast. Re-built on WorkManager ([ADR-0007](decisions/0007-scan-model.md)).
2. **Results list crash on duplicate package.** When an app's package appeared twice (the installed
   decoy colliding with the debug demo fixture), `LazyColumn` threw `Key … was already used`. Fixed
   by de-duplicating scan results by package name in `AppScanner.scan()`.
3. **Notification prompt before onboarding.** `POST_NOTIFICATIONS` was requested on first launch,
   over the language picker. Deferred until after a language is chosen.

## How to run the tests

```bash
# from the project root
./gradlew testDebugUnitTest
# human-readable report:
open app/build/reports/tests/testDebugUnitTest/index.html
```

## What we'll add (in order of value) — tracked in the [Roadmap](10-roadmap.md)

1. **More RiskEngine cases** as we add signals (e.g. "OTHER_STORE + accessibility").
2. **`KnownApps` tests** — impersonation detection (e.g. label "System Update" + non-system ⇒ true;
   the real `com.google.android.gms` named "Google Play Services" ⇒ false).
3. **CI** — run `testDebugUnitTest` + `lint` automatically on every push (guardrail against
   "works on my machine").
4. **Compose UI tests** — once screens stabilise: language picker flips locale; tapping a flagged
   app opens its detail; the honest banner is always shown.
5. **Instrumented scanner test** — verify `AppScanner` reads a known installed package's facts on a
   real emulator (PackageManager behaviour can't be unit-tested off-device).

## Manual end-to-end checklist (until UI tests exist)

- [ ] First launch shows the language picker; choosing Hindi switches the whole UI.
- [ ] Scan completes without freezing; results list the two demo threats, scariest first.
- [ ] Tapping a threat shows plain-language danger, the red-flag list, and the honesty banner.
- [ ] Each action button opens the correct system screen (App Info, Accessibility, Uninstall).
- [ ] Settings switches language back; choice survives an app restart.
- [ ] The merged manifest contains **no** `INTERNET` permission (see [Security](05-security-and-privacy.md)).
