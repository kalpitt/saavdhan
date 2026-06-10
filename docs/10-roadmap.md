# 10 — Roadmap

A living plan. Checked = done; unchecked = planned. Order within a phase is rough priority.

## Phase 1 — Scanner core

**Goal:** detect → explain (bilingual) → one-tap deep links.

- [x] Project scaffold, build toolchain, version catalog, Gradle wrapper
- [x] Pure, tested danger brain (`RiskEngine`) + 9 passing unit tests
- [x] Real scanner (`AppScanner`): accessibility, device admin, SMS, install source, hidden icon, impersonation
- [x] Bilingual UI (Hindi/English) with first-launch language picker + switch in Settings
- [x] Results screen (scariest first) + app detail (plain-language danger, red flags, honesty banner)
- [x] One-tap deep links: App Info, Accessibility list, Device-Admin/Security, Uninstall
- [x] Debug demo threats so the danger UI is visible on a clean emulator
- [x] **Floating overlay coach** (`SYSTEM_ALERT_WINDOW`) — step-by-step help over the Settings screen
- [x] **Background new-app alert** — WorkManager periodic + on-open scan, diffing an installed-app snapshot → notification (with POST_NOTIFICATIONS prompt). **Live-tested** with the `decoyapp` fixture. (First tried a `PACKAGE_ADDED` receiver; on-device testing proved Android blocks it in the background — see ADR-0007.)
- [x] `KnownApps` unit tests (impersonation detection) — 6 tests; 15 total, all passing
- [x] Bulletproof the offline rule — strip any library-merged `INTERNET` permission via `tools:node="remove"`
- [x] Run on emulator (API 35) + capture screenshots — full flow verified, both languages ([screenshots](screenshots/README.md))
- [x] Android Lint clean (0 errors) + fix first-launch UX (notification prompt deferred until after language choice)

## Phase 2 — Guided cleanup

**Goal:** a reactive checklist that responds to the phone's live state.

- [ ] Cleanup flow: isolate phone → revoke permissions → uninstall → re-check after each step
- [ ] **Safe Mode** escalation when a Device Admin blocks uninstall (the key resistant-malware path)
- [ ] Factory reset as a guided last resort
- [ ] Post-cleanup account-security guidance (change passwords from another device, watch UPI mandates)
- [ ] Re-scan after cleanup to confirm the threat is gone

## Phase 3 — Hardening & reach

**Goal:** make it trustworthy and shippable at scale.

- [ ] **CI**: GitHub Action running `testDebugUnitTest` + Android Lint on every push
- [ ] Static analysis: `detekt` + `ktlint`
- [ ] Compose UI tests + an instrumented scanner test
- [ ] Accessibility audit (TalkBack, contrast, touch targets, font scaling)
- [ ] Wider device/OEM testing (deep-link behaviour varies by manufacturer)
- [ ] Release signing setup (documented, key backed up)
- [ ] **Distribution & trust story** — ideally Google Play (most trusted) with the
      `QUERY_ALL_PACKAGES` declaration; avoid asking relatives to sideload *this* app too
- [ ] Play Store policy review: Permissions Declaration Form, Data Safety section (we collect nothing)

## Known limitations to revisit

- Accessibility/Device-Admin deep links open a *list*, not the exact row (Android limit) — the
  overlay coach is our mitigation.
- Detection is heuristic; brand-new malware avoiding all six signals could be missed.
- Impersonation list is small and updates only via app releases (we never fetch rules online).
