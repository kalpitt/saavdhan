# ADR-0003 — Kotlin + Jetpack Compose + Material 3

**Status:** Accepted · **Date:** 2026-06-09

## Context
We need a native Android UI that's quick to build, easy to make bilingual and accessible, and
aligned with what Google recommends today. The two choices were the older XML Views system or the
modern Jetpack Compose toolkit.

## Decision
Build the UI with **Kotlin + Jetpack Compose + Material 3**, single Activity, Navigation Compose.

## Consequences
- ✅ Google's current recommended UI toolkit; large-button, high-contrast, accessible screens are
  fast to compose.
- ✅ Less boilerplate than XML; UI is plain Kotlin functions, easier for one developer to maintain.
- ✅ Works cleanly with the recommended state model (ViewModel + Unidirectional Data Flow).
- ⚠️ Compose has a learning curve and pulls in the Compose libraries (kept in check via the
  [version catalog](../../gradle/libs.versions.toml)).
- ⚠️ Some Material 3 APIs are still `@ExperimentalMaterial3Api` (e.g. `TopAppBar`); acceptable and
  widely used.

Reference: [Guide to app architecture](https://developer.android.com/topic/architecture).
