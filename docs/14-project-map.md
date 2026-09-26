# Project map (where things live)

Reference map of the source tree. The rules that govern it live in [`AGENTS.md`](../AGENTS.md) §4.

```
app/src/main/java/com/saavdhan/app/
├─ domain/          PURE Kotlin, NO Android imports → unit-testable "brain"
│  ├─ model/        ScannedApp, Risk (levels + signals)
│  ├─ risk/         RiskEngine — deterministic, explainable scoring
│  ├─ allowlist/    KnownApps — trusted packages + impersonation denylist
│  └─ cleanup/      CleanupEngine + CleanupModels — Phase 2 reactive checklist
├─ data/scanner/    AppScanner — reads live device state via PackageManager etc.
├─ system/          Android-facing services
│  ├─ deeplink/     SettingsDeepLinks — intent builders for every fix screen
│  ├─ overlay/      OverlayCoachService — floating step-by-step coach
│  └─ watchdog/     WorkManager job that alerts on newly-installed dangerous apps
├─ i18n/            LocaleManager — per-app Hindi/English
├─ ui/              Jetpack Compose screens (onboarding, scan, detail, settings, cleanup)
└─ MainActivity.kt  single Activity + Navigation Compose (routes)

app/src/test/...    unit tests (RiskEngineTest, CleanupEngineTest, KnownAppsTest, …)
app/src/main/res/   values/strings.xml (English) + values-hi/strings.xml (Hindi) — MIRRORED
docs/               stable reference: vision, architecture, ADRs, testing, glossary, roadmap
context/            living state: STATE / PROGRESS / PROFILE / handoffs  ← read these each session
```
