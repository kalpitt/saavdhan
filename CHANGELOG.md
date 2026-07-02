# Changelog

All notable changes to Saavdhan are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project aims to follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.5.0] — 2026-07-02
Deep architecture hardening, messenger delivery-chain detection, family share receipts, and OEM
deep-link fallback chains — the largest jump in detection depth since Phase 1.

### Added
- **Deep architecture hardening.** The risk engine moved from a boolean cascade to a continuous,
  point-based score (CRITICAL ≥ 80, HIGH ≥ 50, SUSPICIOUS ≥ 20 — see
  [docs/03-detection-rules.md](docs/03-detection-rules.md)). App signing certificates are now
  extracted and checked against a hardcoded set of trusted keys as an absolute-trust override.
  Impersonation matching uses Levenshtein fuzzy matching (catches near-miss disguise names) plus
  a `taskAffinity` hijacking check for sideloaded apps. All Binder/PackageManager IPCs in
  `AppScanner` now run off the main thread.
- **Scam delivery-chain detection for messengers.** A new `SIDELOADED_VIA_MESSENGER` signal fires
  when an app's install traces (via `originatingPackageName`, API 30+) back to WhatsApp, WhatsApp
  Business, Telegram, or another recognized messenger — the exact real-world path scam APKs
  travel (fake wedding invite / courier / KYC lure sent straight over chat).
- Danger reasons on the app-detail screen are now ranked by the engine's own point weights
  (most decisive evidence first), with softer circumstantial clues split into a quieter "Also
  noticed" group ([ADR-0012](docs/decisions/0012-explanation-ranked-by-engine-weight.md)).
- Screen readers now announce the danger verdict before the app name (not the raw package id) on
  result cards and the app-detail banner.
- The scan is now resilient to the ~1 MB Binder reply-size limit on cheap phones with many
  installed apps: it falls back to a slower per-app read instead of silently failing or trusting
  a truncated list, and shows an honest "may be incomplete" note. The background watchdog also
  got a retry cap ([ADR-0013](docs/decisions/0013-resilient-package-fetch.md)).
- The mid-scam watchdog notification now speaks to someone being actively walked through an
  install by a scammer ("Is someone on the phone telling you to do this? …hang up, don't share
  any OTP") instead of describing the artifact ("A new app looks dangerous").
- **"Send result to family."** An offline share-sheet receipt on the results screen builds a
  plain-language summary and opens the phone's own share sheet (WhatsApp/SMS/etc.), so the
  remote family member who set up Saavdhan can finally see what a scan found. Zero network, zero
  stored contacts ([ADR-0014](docs/decisions/0014-offline-share-to-family.md)). The receipt is
  now timestamped and phrased like a verifiable UPI-style confirmation.
- A glanceable phone-health bar (green → red severity meter + verdict word) atop flagged results.
- Each flagged app's own launcher icon (with a monogram-circle fallback) on result cards, for
  Play-Protect-style at-a-glance recognition.
- A complete dark-theme visual-polish pass across risk-tinted cards, the health bar, and monogram
  avatars.
- OEM-aware Settings deep links (Xiaomi/Samsung/Oppo/Vivo/Huawei) and a warning card in Settings
  steering users away from OEM battery-optimization kills that silence the watchdog.
- **OEM deep-link fallback chains.** The Device-Admin and auto-start "fix" buttons now try an
  ordered, per-maker chain (One UI / MIUI / ColorOS-family / Funtouch / EMUI, with
  OnePlus/Realme/Poco/Redmi/iQOO sub-brand aliasing) that degrades one honest step at a time
  instead of betting on a single hardcoded screen, with a bilingual "here's where to look
  instead" toast when only the generic last resort opens. Fixed the previous Samsung auto-start
  screen leading with a China-only package that could never work on an Indian phone.
- Claude Code steering layer (`.claude/rules`, `.claude/skills`) and a `spyware-researcher`
  subagent for growing detection coverage — contributor tooling, no user-facing change.

### Fixed
- A false positive where a legitimate sideloaded app naming its own tasks after its package
  (containing "bank") could earn an undeserved impersonation penalty; the check is now a precise
  fingerprint of Settings-UI hijacking instead.
- `SMS_ACCESS` alone was weighted equal to the SUSPICIOUS threshold, flagging any store-installed
  messaging app that merely holds SMS permission; demoted so SMS access alone stays LOW, and only
  escalates in combination with other signals.
- A bypass where apps sideloaded via WhatsApp Business, Telegram X, or Messenger could dodge the
  new messenger-delivery signal if an OEM package installer misreported the source as "other
  store." The install source is now forced to sideloaded whenever the originating package is a
  known messenger.

## [0.4.0] — 2026-06-13
Sharper detection, dependency refresh, and a redesigned public site.

### Added
- Detection: impersonation combined with a real spy/control power (Accessibility, Device Admin,
  SMS, or notification access) now escalates straight to CRITICAL instead of HIGH — that
  combination is an active banking trojan, not just a suspicious name. Impersonation on its own
  stays HIGH.
- Impersonation name-matching hardened: labels are normalized (case, punctuation, emoji,
  whitespace) so "System  Update!", "System-Update ⬇️", and "SYSTEM_UPDATE" all resolve to the
  same disguise instead of slipping past an exact match. Expanded the impersonation list with
  more system/update/security/Google-component disguises.
- Public site redesigned to the "AI Glass Bento" look (glass cards, green mesh, self-hosted
  Inter, screenshot showcase) — brand teal kept, bilingual + zero-build preserved.

### Changed
- Dependency bumps: AGP 8.7.3→8.9.1, Kotlin 2.0.21→2.1.0, Compose BOM 2024.12.01→2025.01.00,
  core-ktx/lifecycle/activity/navigation/work to current stable (Gradle wrapper 8.11.1 kept).

## [0.3.0] — 2026-06-13
First feedback-driven redesign, plus accessibility and Play Store groundwork.

### Added
- Visual-hierarchy redesign from the first family feedback round ("app looks too simple; once a
  threat is found, all the steps look the same"): severity banner, "What to do now" hero card,
  numbered do-it-yourself steps, cleanup step counters ("Step 2 of 5"), risk-tinted result cards.
- Automated accessibility first-pass: lint, contrast measurements, touch targets, font scaling,
  decorative-icon labels — fixed the amber-chip contrast failure (white → dark text).
- Home screen now states the offline promise up front ("Works fully offline — nothing ever
  leaves your phone", EN+HI) — trust-building for first-time users.
- Landing page bilingual toggle (EN/हिन्दी) and a stable, direct-download APK filename.
- Play Store prep: privacy policy, bilingual listing copy, data-safety answers, policy
  declarations (execution — creating the developer account and submitting — remains a human
  step).
- ktlint code-style enforcement in CI (`./gradlew ktlintCheck`; auto-fix with `ktlintFormat`).

### Fixed
- **Watchdog v2 fixture regression (caught in emulator testing):** its baseline included the
  emulator demo fixtures, which share a package name with the `decoyapp` test app — so the
  documented decoyapp flow could never alert. The watchdog now scans real packages only.
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

[Unreleased]: https://github.com/kalpitt/saavdhan/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/kalpitt/saavdhan/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/kalpitt/saavdhan/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/kalpitt/saavdhan/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/kalpitt/saavdhan/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/kalpitt/saavdhan/releases/tag/v0.1.0
