# 06 — Coding Standards & Anti-"Vibe-Code" Guardrails

"Vibe-coded" apps look fine in a demo and then break in confusing ways: no tests, secrets and
text hardcoded everywhere, no structure, crashes on edge cases, and nobody remembers *why*
anything was done. This doc is how we don't become that. Each guardrail below is a rule we
actually follow in this repo.

## The guardrails

| # | Vibe-code failure | Our guardrail | Where to see it |
|---|---|---|---|
| 1 | "It works, ship it" — no tests | The danger **brain is pure and unit-tested**; rules can't change without a test | [Testing](07-testing-strategy.md), `RiskEngineTest` |
| 2 | Logic tangled into the UI | **Layered architecture**; domain layer has zero Android imports | [Architecture](02-architecture.md) |
| 3 | Text hardcoded in code | **All user-facing text in `strings.xml` / `values-hi`** — also makes it bilingual | `res/values*/strings.xml` |
| 4 | Mystery decisions | **Every decision is an ADR**, linked from the code seam | [decisions/](decisions/README.md) |
| 5 | Crashes on the unexpected | Deep links wrapped in try/catch; nullable Android data handled, never `!!` on external data | `SettingsDeepLinks.launch()`, `AppScanner` |
| 6 | Dependency bloat / supply-chain risk | **Minimal, well-known AndroidX/Compose deps only**, pinned in one [version catalog](../gradle/libs.versions.toml) | `gradle/libs.versions.toml` |
| 7 | Silent privacy leaks | **No `INTERNET` permission** — leaks are impossible by construction | [Security](05-security-and-privacy.md) |
| 8 | "Magic" the user can't trust | **Explainable** rules; every verdict lists its reasons | [Detection](03-detection-rules.md) |
| 9 | UI freezes / jank | Scanning runs **off the main thread** (`Dispatchers.Default`) | `ScanViewModel` |
| 10 | Inaccessible to real users | Large tap targets, high-contrast risk colours, content descriptions on icons | `ui/components`, `ui/theme` |

## Kotlin / Compose conventions

- **Immutability first.** Prefer `val`, `data class`, and pure functions. The whole `domain`
  layer is pure functions over immutable data.
- **No `!!` on external data.** Anything from `PackageManager` can be null; handle it
  (`?:`, `mapNotNull`, `try/catch`). `!!` is only acceptable on values we just created.
- **Unidirectional data flow.** Composables receive state and emit events via lambdas; they don't
  reach into ViewModels to mutate things. State down, events up.
- **One responsibility per file/class.** `AppScanner` reads; `RiskEngine` judges; screens display.
- **Comment the "why," not the "what."** Especially at architectural seams, point to the ADR.
- **Naming:** `PascalCase` types, `camelCase` functions/vals, `UPPER_SNAKE` constants, Compose
  functions are `PascalCase` nouns (e.g. `ScanScreen`).

## Definition of Done (for any change)

A change isn't done until:
1. It **builds**: `./gradlew assembleDebug`.
2. Tests **pass**: `./gradlew testDebugUnitTest` (and you added/updated a test if you changed a rule).
3. No new user-facing string is hardcoded (it's in `strings.xml` **and** `values-hi`).
4. If it's an architectural choice, there's an **ADR** for it.
5. It still honours the [five rules](README.md#the-five-rules-that-never-change) — above all, no `INTERNET`.

## Tooling we should add as the project grows (tracked in the [Roadmap](10-roadmap.md))

- **Static analysis:** Android Lint (built in) on every build; consider `detekt` + `ktlint` for
  style. *(Not yet wired up — first easy win.)*
- **CI:** a GitHub Action that runs `testDebugUnitTest` + `lint` on every push, so a broken change
  can't be merged silently.
- **Instrumented/UI tests** for the screens (Compose test rule) once the UI stabilises.
