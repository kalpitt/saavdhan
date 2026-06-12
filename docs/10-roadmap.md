# 10 — Roadmap

A living plan. Checked = done; unchecked = planned. Order within a phase is rough priority.
**Last synced:** 2026-06-11 after Phase 3 shipped. See [`context/STATE.md`](../context/STATE.md) for "now".

## Phase 1 — Scanner core ✅ Complete

**Goal:** detect → explain (bilingual) → one-tap deep links.

- [x] Project scaffold, build toolchain, version catalog, Gradle wrapper
- [x] Pure, tested danger brain (`RiskEngine`); see [`RiskEngineTest.kt`](../app/src/test/java/com/saavdhan/app/domain/risk/RiskEngineTest.kt) for rules
- [x] Real scanner (`AppScanner`): accessibility, device admin, SMS, install source, hidden icon, impersonation
- [x] Bilingual UI (Hindi/English) with first-launch language picker + switch in Settings
- [x] Results screen (scariest first) + app detail (plain-language danger, red flags, honesty banner)
- [x] One-tap deep links: App Info, Accessibility list, Device-Admin/Security, Uninstall
- [x] Debug demo threats on emulator only; real-device scan shows only real apps
- [x] **Floating overlay coach** (`SYSTEM_ALERT_WINDOW`) — step-by-step help over the Settings screen
- [x] **Background new-app alert** — WorkManager periodic + on-open scan; tested with `decoyapp` fixture

## Phase 2 — Guided cleanup ✅ Complete

**Goal:** a reactive checklist that responds to the phone's live state.

- [x] Cleanup flow: isolate phone → revoke permissions → uninstall → progress auto-updates
- [x] **Safe Mode** escalation when a Device Admin blocks uninstall (the key resistant-malware path)
- [x] Factory reset as a guided last resort
- [x] Post-cleanup account-security guidance (secure your accounts step)
- [x] Live-tested on emulator; real-device testing in progress (OEM-specific deep-link variants)

## Phase 3 — Detection & UX hardening ✅ Complete

**Goal:** first-class signals, allowlist, watchdog, real-device polish.

- [x] **CI**: GitHub Action running tests + lint on every push
- [x] Detection-engine upgrades: `SEND_SMS`, `NOTIFICATION_LISTENER`, OEM installer classifiers, `SMS_REQUESTED` signal
- [x] Allowlist hardening: trusted prefixes (`com.google.android.*`, `com.samsung.android.*`), power-gating sideloaded fakes
- [x] Watchdog reliability: battery-optimization detection, last-run heartbeat in Settings
- [x] Real-device UX fixes: layout fixes for long app names, false-positive elimination (icon-less store helpers, etc.)
- [x] Public mirror published (`kalpitt/saavdhan`)

## Phase 4 — Trust & Reach 🚀 In progress

**Goal:** signed APK → GitHub Releases → landing page; failure-path hardening; watchdog v2.

### Milestone 0 — Safety net & docs ✅ (merged in PR #5)
- [x] CI hardening: offline-guarantee check fails if manifests missing (not silent-pass on missing path)
- [x] Context sync: STATE, roadmap, testing-strategy to reality; no machine-local paths in published files
- [x] Drift detection: `scripts/drift_check.sh` in CI — machine-local paths, string-key mirroring
- [x] Seam extraction: `WatchdogPolicy` + `InstallerClassifier` as tested pure functions

### Milestone 1 — Failure-path hardening ✅ (merged in PR #5)
- [x] Scan error handling: `ScanState.Error` + try/catch + calm bilingual retry
- [x] Partial visibility: `partial=true` when `QUERY_ALL_PACKAGES` is restricted (API 30+)
- [x] Cleanup survives process death: `SavedStateHandle` for progress
- [x] Prefix-trust power-gating: non-Play installs with dangerous powers override the allowlist
- [x] Overlay timeout + dismissal: banner auto-hides after ~2 min and on app resume
- [x] Settings resume: battery card updates when user returns from exemption screen
- [x] Slow-scan reassurance: "lots of apps to check" message after ~8 seconds

### Milestone 2 — Release & watchdog v2 (in progress)
- [x] Watchdog v2: alerts when an already-installed app *gains* HIGH/CRITICAL powers (the real
      SpyNote sequence); level-aware snapshot with silent migration from v1 (no alert spam on upgrade)
- [x] Release signing wiring: Gradle reads optional `keystore.properties` (see the `.example`
      template); unsigned builds keep working without it
- [x] Landing page built: `docs/index.html` (bilingual, promises, install guide, download button)
- [x] Keystore generated + backed up (human, 2026-06-12)
- [x] GitHub Pages enabled — landing page live at kalpitt.github.io/saavdhan
- [x] **v0.2.0 released**: signed APK on GitHub Releases, signature verified, download
      button live (2026-06-12)
- [x] ktlint in CI: enforced via `ktlintCheck` (.editorconfig keeps the repo's inline-comment style)

### Milestone 3 — Quality & polish (in progress)
- [x] Watchdog v2 live-tested on emulator (decoyapp → notification verified; fixture regression
      found & fixed in the process)
- [x] Home screen offline-promise note; honest detail-page "scan again" fallback; style cleanups
- [ ] Real-device testing across phone makers (OEM deep-link variants) — needs human + phones
- [ ] Accessibility audit (TalkBack, contrast, touch targets, font scaling) — needs human judgment
- [ ] Dependency bumps (Compose BOM / AGP / Kotlin) — deferred; do with an emulator pass

### Milestone 3 — Polish & reach
- [ ] Real-device testing across phone makers (OEM deep-link variants, edge cases)
- [ ] Accessibility audit: TalkBack, contrast, touch targets, font scaling (for real users, not just dev testing)
- [ ] Play Store evaluation: decide whether Play is a future target (v0.3+) or GitHub Releases only
- [ ] Dependency updates: routine version bumps (Compose BOM, AGP, Kotlin)

## Known limitations to revisit

- Accessibility/Device-Admin deep links open a *list*, not the exact row (Android limit) — the
  overlay coach is our mitigation.
- Detection is heuristic; brand-new malware avoiding all six signals could be missed.
- Impersonation list is small and updates only via app releases (we never fetch rules online).
