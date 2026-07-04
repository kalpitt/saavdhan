# 10 — Roadmap

A living plan. Checked = done; unchecked = planned. Order within a phase is rough priority.
**Last synced:** 2026-07-04, after the v0.6.0 release. See
[`context/STATE.md`](../context/STATE.md) for "now".

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

### Milestone 3 — Quality & polish ✅ (merged in PR #9)
- [x] Watchdog v2 live-tested on emulator (decoyapp → notification verified; fixture regression
      found & fixed in the process)
- [x] Home screen offline-promise note; honest detail-page "scan again" fallback; style cleanups
- [x] ktlint enforced in CI

### Milestone 4 — First feedback & reach ✅ (shippable work complete; two items are human-only)
- [x] First family feedback round (2026-06-13): "app looks too simple; once a threat is found,
      all the steps look the same" → visual-hierarchy redesign: severity banner, "What to do
      now" hero card, numbered do-it-yourself steps, cleanup step counters ("Step 2 of 5"),
      risk-tinted result cards. Emulator-verified: EN + HI, light + dark, 1.5× font scale.
- [x] Accessibility — automated first-pass (lint, contrast measurements, touch targets, font
      scaling, decorative-icon labels): see [`12-accessibility.md`](12-accessibility.md);
      fixed the amber-chip contrast failure (white→dark text)
- [ ] Accessibility — human pass: TalkBack walkthrough, OEM skins, max font scale
      (checklist at the bottom of [`12-accessibility.md`](12-accessibility.md))
- [ ] Real-device testing across phone makers (OEM deep-link variants) — needs human + phones
- [x] Play Store decision & prep: going to Play (removes the sideloading irony, adds
      auto-updates). Privacy policy ([`privacy.html`](privacy.html)), data-safety answers,
      bilingual listing copy, policy declarations all ready: [`11-play-store-prep.md`](11-play-store-prep.md)
- [ ] Play Store execution (human): $25 dev account → closed test (12 testers × 14 days —
      use the family testers) → production
- [x] v0.3.0 release: visual-hierarchy redesign shipped; landing-page button flipped to direct
      `releases/latest/download/saavdhan.apk` URL (stable forever)
- [x] Public site redesign: landing + privacy pages restyled to the "AI Glass Bento" design from
      the [kalpit.me](https://kalpit.me) portfolio (glass cards, green mesh, self-hosted Inter,
      screenshot showcase) — brand teal kept, bilingual + zero-build preserved
- [x] F-Droid prep: fastlane listing metadata (EN + HI) + screenshots in-repo, FOSS/anti-features
      check, and the fdroiddata build recipe — all ready in [`13-fdroid.md`](13-fdroid.md). Submitting
      the recipe to fdroiddata is the remaining human step.
- [x] Dependency bumps: AGP 8.7.3→8.9.1, Kotlin 2.0.21→2.1.0, Compose BOM 2024.12.01→2025.01.00,
      core-ktx/lifecycle/activity/navigation/work to current stable (Gradle wrapper 8.11.1 kept).
      Build + 78 unit tests + lint green; emulator smoke-tested (scan renders, no crashes)
- [x] **v0.4.0 released** (2026-06-13): detection escalation (impersonation + power → CRITICAL),
      normalized impersonation matching, redesigned public site, dependency bumps.

## Phase 5 — Deep architecture, delivery-chain detection & product polish ✅ Complete — shipped in v0.5.0

**Goal:** move the risk engine from a boolean cascade to an explainable point system, close real
detection gaps (messenger-delivered malware, signature spoofing), and turn seven nights of an
unattended multi-agent "world-class loop" (`claude/world-class-loop`, now fully merged) into
shipped, tested product polish. Everything below shipped in the **v0.5.0** signed release — see
the [changelog](../CHANGELOG.md) `[0.5.0]` section for full detail, and
[`context/ITERATION_LOG.md`](../context/ITERATION_LOG.md) for the iteration-by-iteration diary.

- [x] Point-based `RiskEngine` (`WEIGHTS` map, CRITICAL ≥ 80 / HIGH ≥ 50 / SUSPICIOUS ≥ 20)
      replacing the old boolean cascade — see [`03-detection-rules.md`](03-detection-rules.md)
- [x] Signature verification: `TRUSTED_SIGNATURES` absolute-trust override via SHA-256 signing-key hash
- [x] Fuzzy (Levenshtein) impersonation matching + `taskAffinity` hijacking check for sideloaded apps
- [x] `AppScanner` Binder/PackageManager IPCs moved off the main thread
- [x] Resilient package fetch: survives the ~1 MB Binder reply limit on cheap phones with many
      apps instead of silently failing or trusting a truncated list ([ADR-0013](decisions/0013-resilient-package-fetch.md))
- [x] `SIDELOADED_VIA_MESSENGER` signal: traces `originatingPackageName` (API 30+) to WhatsApp/
      Telegram/other messengers — the real-world scam delivery chain — plus a fix for an OEM
      installer-classifier bypass that let messenger-delivered apps dodge the signal
- [x] Danger reasons ranked by engine weight, most-decisive-first ([ADR-0012](decisions/0012-explanation-ranked-by-engine-weight.md))
- [x] Screen reader announces the verdict before the app name, not the package id
- [x] Mid-scam watchdog notification rewritten to interrupt the scam script, not describe the artifact
- [x] "Send result to family" offline share-sheet receipt, now timestamped ([ADR-0014](decisions/0014-offline-share-to-family.md))
- [x] Phone-health severity bar, per-app launcher icons on result cards, full dark-theme polish pass
- [x] OEM-aware Settings deep links (auto-start screens for Xiaomi/Samsung/Oppo/Vivo/Huawei) +
      Settings warning card against OEM battery kills
- [x] **OEM deep-link fallback chains** (2026-07-02): Device-Admin and auto-start screens now try
      an ordered, unit-tested per-maker chain (with OnePlus/Realme/Poco/Redmi/iQOO sub-brand
      aliasing) instead of one hardcoded component, with an honest "here's where to look instead"
      toast when only the generic fallback opens
- [x] Claude Code steering layer (`.claude/rules`, `.claude/skills`, `spyware-researcher` subagent)
- [x] **v0.5.0 released** (2026-07-02): versionCode 5, signed APK on GitHub Releases, public
      mirror synced.

## Phase 6 — Detection signals v2: 2026 sideload-lure campaigns ✅ Complete — shipped in v0.6.0

**Goal:** close three specific, high-precision offline gaps observed in 2026 India scam-app
campaigns spread over WhatsApp/Telegram (wedding-invite, e-challan, bill-update, KYC-update
sideload lures) — see [`03-detection-rules.md`](03-detection-rules.md) for the full spec.

- [x] `LURE_LABEL` (30 pts): sideloaded app named like a scam bait file, matched against a
      curated, precision-first phrase list (`KnownApps.LURE_LABELS`)
- [x] `INSTALL_PACKAGES_REQUESTED` (25 pts): requests permission to install other apps — the
      two-stage dropper tell
- [x] `ACCESSIBILITY_DECLARED` (15 pts): manifest declares an accessibility service not yet
      enabled — early warning before the victim turns it on; yields to `ACCESSIBILITY` once on
- [x] Scanner: `GET_SERVICES` added to the resilient fetch; new `ScannedApp` fields; new emulator
      demo fixture ("Wedding Invitation" fresh from WhatsApp, nothing granted → CRITICAL)
- [x] 16 new tests (131 total), mirrored EN/HI strings, detection-rules doc rewritten for 13
      signals
- [x] Live-tested on the `saavdhan_pixel` emulator in English and Hindi: CRITICAL verdict, all
      three new reasons ranked correctly by weight, zero crashes
- [x] **v0.6.0 released** (2026-07-04): versionCode 6, signed APK on GitHub Releases, public
      mirror synced.

## Known limitations to revisit

- Accessibility/Device-Admin deep links open a *list*, not the exact row (Android limit) — the
  overlay coach is our mitigation.
- Detection is heuristic; brand-new malware avoiding all thirteen signals could be missed.
  Signature verification and fuzzy impersonation matching close two real gaps, but neither is
  proof.
- Impersonation list is small and updates only via app releases (we never fetch rules online).
- OEM auto-start/device-admin screens are best-effort fallback chains built from undocumented
  component names — only real hardware across makers can confirm they still resolve correctly.
