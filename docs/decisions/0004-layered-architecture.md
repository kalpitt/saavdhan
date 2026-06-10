# ADR-0004 — Pure, layered, testable architecture

**Status:** Accepted · **Date:** 2026-06-09

## Context
The danger-detection logic is the most important and highest-risk part of the app. If it's tangled
into Android screens and the package system, it can only be tested slowly on a device, and bugs in
it directly cause missed malware or false alarms.

## Decision
Split into three layers following Google's architecture guide:
- **domain/** — pure Kotlin, **no Android imports** (`RiskEngine`, models, `KnownApps`).
- **data/** — reads real facts from Android (`AppScanner`).
- **ui/** — Compose screens + `ScanViewModel`, using Unidirectional Data Flow.

The domain layer never depends on the others; dependencies point inward.

## Consequences
- ✅ The whole danger brain is unit-tested in ~30 ms with no emulator (`RiskEngineTest`, 9 tests).
- ✅ Clear responsibilities: scanner reads, engine judges, screens display.
- ✅ We validated the rules *before* writing any UI.
- ⚠️ Slightly more files/indirection than dumping logic into the Activity — a deliberate, worthwhile
  trade for testability and clarity.

See [Architecture](../02-architecture.md) and [Testing](../07-testing-strategy.md).
