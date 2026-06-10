# Changelog

All notable changes to Saavdhan are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project aims to follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[Unreleased]: https://github.com/kalpitt/Saavdhan---Anti-Scam-App/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/kalpitt/Saavdhan---Anti-Scam-App/releases/tag/v0.1.0
