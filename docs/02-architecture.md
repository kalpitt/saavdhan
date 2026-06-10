# 02 — Architecture

We follow Google's official **[Guide to app architecture](https://developer.android.com/topic/architecture)**:
separate layers, a single source of truth, and **Unidirectional Data Flow** (state flows *down*
to the UI, events flow *up* from it). This is what keeps the app testable and stops it becoming
a tangle.

## The layers

```
┌──────────────────────────────────────────────────────────────┐
│  UI layer  (ui/)                                              │
│  Jetpack Compose screens + ScanViewModel (holds screen state) │
│  Knows about Android & Compose. Shows state, sends events up.  │
└───────────────▲───────────────────────────┬──────────────────┘
                │ state                       │ events (scan, open link)
┌───────────────┴───────────────────────────▼──────────────────┐
│  Data layer  (data/scanner/)                                  │
│  AppScanner — reads real facts from Android's package system.  │
│  The ONLY place that talks to PackageManager etc.              │
└───────────────────────────────┬──────────────────────────────┘
                                 │ ScannedApp (plain facts)
┌────────────────────────────────▼─────────────────────────────┐
│  Domain layer  (domain/)  ← PURE KOTLIN, NO ANDROID           │
│  RiskEngine + models + allowlist. The danger-judging "brain".  │
│  Can run, and is fully tested, on a normal computer.           │
└──────────────────────────────────────────────────────────────┘
```

**Why the domain layer is pure (no Android imports):** it lets us test every danger rule in
milliseconds without an emulator. This is the most important architectural decision in the app —
see [ADR-0004](decisions/0004-layered-architecture.md). It's also *why* we caught the rules being
correct before we ever ran the UI.

## Module map (package `com.saavdhan.app`)

| Package | Responsibility | Touches Android? |
|---|---|---|
| `domain/model/` | `ScannedApp`, `RiskLevel`, `RiskSignal`, `RiskAssessment` | ❌ pure |
| `domain/risk/` | `RiskEngine` — combines signals into a verdict | ❌ pure |
| `domain/allowlist/` | `KnownApps` — trusted packages + impersonation list | ❌ pure |
| `data/scanner/` | `AppScanner` — reads packages, permissions, install source | ✅ yes |
| `system/deeplink/` | `SettingsDeepLinks` — intents to the exact fix screens | ✅ yes |
| `i18n/` | `LocaleManager` — language choice + applying it | ✅ yes |
| `ui/scan/` | `ScanScreen`, `ScanViewModel` — home + results | ✅ yes |
| `ui/detail/` | `AppDetailScreen` — explanation + actions | ✅ yes |
| `ui/onboarding/` | `LanguageScreen` | ✅ yes |
| `ui/settings/` | `SettingsScreen` | ✅ yes |
| `ui/components/` | reusable buttons, chips, cards | ✅ yes |
| `ui/theme/` | colours, typography | ✅ yes |
| `ui/UiMappings.kt` | maps domain enums → localized text + colour | ✅ yes |

## Data flow of one scan

1. User taps **Scan**. `ScanScreen` calls `ScanViewModel.scan()`.
2. `ScanViewModel` runs `AppScanner.scan()` on a **background thread** so the UI never freezes.
3. `AppScanner` reads the real facts for each app into a plain `ScannedApp`, then asks
   `RiskEngine.assess()` for a verdict.
4. The list of verdicts (scariest first) becomes the ViewModel's state.
5. Compose observes the state and redraws the results. State down, events up.

## Conscious "not yet" choices

- **No dependency-injection framework (Hilt).** The official guide recommends Hilt for larger
  apps, but at this size manual construction is simpler and has fewer moving parts. Revisit if the
  graph grows. ([ADR-0011](decisions/0011-no-di-framework-yet.md))
- **Single Activity + Navigation Compose.** Standard modern structure; each screen is a composable.

## Related decisions
- [ADR-0003 — Kotlin + Jetpack Compose + Material 3](decisions/0003-compose-material3.md)
- [ADR-0004 — Pure, layered, testable architecture](decisions/0004-layered-architecture.md)
- [ADR-0006 — SharedPreferences for locale](decisions/0006-sharedprefs-for-locale.md)
