# Changelog

All notable changes to Saavdhan are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project aims to follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Home screen now states the offline promise up front ("Works fully offline — nothing ever
  leaves your phone", EN+HI) — trust-building for first-time users.
- ktlint code-style enforcement in CI (`./gradlew ktlintCheck`; auto-fix with `ktlintFormat`).

### Fixed
- **Watchdog test-fixture regression (caught in emulator testing):** the v2 watchdog's baseline
  included the emulator demo fixtures, which share a package name with the `decoyapp` test app —
  so the documented decoyapp flow could never alert. The watchdog now scans real packages only.
  Verified live on the emulator: clean baseline → install decoyapp → threat notification posts.
- Opening a flagged app's detail page after the scan results are gone (e.g. after Android killed
  the app) now shows an honest "scan again" note instead of a misleading "no dangerous apps" line.
- Cleanup screen's app name now survives process death too.

## [0.2.0] — 2026-06-11
Failure-path hardening + watchdog v2. First release distributed as a signed APK on GitHub Releases.

### Added
- **Watchdog v2.** The background watchdog now also warns when an *already-installed* app gains
  dangerous powers (Accessibility / Device Admin / OTP access) — the real-world scam sequence where
  an app installs quietly and is "armed" later. Alerts fire once per upward crossing; no repeat spam.
- **Slow-scan reassurance.** If a scan takes more than ~8 seconds, a calm "this can take a minute"
  note appears so the wait never feels broken.
- **Scan error screen.** If a scan fails mid-way, the app shows a calm bilingual "couldn't finish
  scanning — try again" screen instead of crashing.
- **Honest partial-scan banner.** If the phone restricts which apps Saavdhan can see, the results
  now say the scan may be incomplete instead of declaring a false "all clear".
- Landing page (`docs/index.html`) for GitHub Pages, and release-signing wiring
  (`keystore.properties`, gitignored) in preparation for the first signed APK release.

### Fixed
- Closed an allowlist gap: a non-Play app faking a trusted package prefix (e.g.
  `com.google.android.*`) while holding dangerous powers is no longer trusted.
- Cleanup progress now survives Android killing the app mid-cleanup (common on low-RAM phones).
- The battery-optimization card in Settings updates immediately on return from system Settings.
- The floating coach banner now auto-hides after ~2 minutes and whenever you return to the app.

## [0.1.0] — 2026-06-10
First public early release. **Fully offline** — the app does not hold the `INTERNET` permission, so
it cannot make a network call. Detection runs on-device with a deterministic, unit-tested rule
engine.

### Added
- **Scanner core (Phase 1).** Reads public, no-root signals for each installed app — Accessibility,
  Device Admin, SMS access, install source (sideloaded vs Play), hidden launcher icon, and system-app
  impersonation — and scores a risk level (Very dangerous / Dangerous / Worth checking / Looks okay)
  with the reasons spelled out in plain language.
- **Guided cleanup (Phase 2).** A reactive, step-by-step checklist — isolate the phone → turn off
  Accessibility → remove Device-Admin power → uninstall → secure your accounts — that ticks each step
  off automatically by re-reading the phone's live state, with one-tap deep links to the exact system
  screens and an optional on-screen coach. Surfaces Safe Mode and factory-reset escalations when an
  app resists removal.
- **Bilingual UI** (हिन्दी / English), chosen on first launch and switchable in Settings.
- **Background watchdog** (WorkManager) that warns when a newly installed app looks dangerous.
- **Calm, panic-friendly design**: large buttons, simple language, one clear step at a time.

### Known limitations
- Tested on the Android emulator; the system screens that "fix" buttons open vary by phone maker, so
  real-device testing across makers is the current focus.
- Detection is heuristic (behavioural signals, not a malware database): it can raise false alarms and
  can miss brand-new threats. It guides — it never silently changes or removes anything.

[Unreleased]: https://github.com/kalpitt/saavdhan/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/kalpitt/saavdhan/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/kalpitt/saavdhan/releases/tag/v0.1.0
