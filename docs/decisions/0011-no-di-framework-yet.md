# ADR-0011 — No dependency-injection framework yet

**Status:** Accepted · **Date:** 2026-06-09

## Context
Google's architecture guide recommends a dependency-injection (DI) library — usually **Hilt** — to
wire components together in larger apps. DI improves testability and decoupling, but adds an
annotation processor, build complexity, and concepts that are a lot to absorb for a first-time
developer on a small app.

## Decision
For now, construct dependencies manually (e.g. `AppScanner(context)` created where needed). Revisit
adopting **Hilt** when the dependency graph grows (more repositories, shared services across many
screens) or when instrumented tests need easy substitution.

## Consequences
- ✅ Fewer moving parts; the build stays simple and fast; easier to learn.
- ✅ The pure domain layer is already trivially testable without DI.
- ⚠️ If the app grows, manual wiring gets repetitive — that's the trigger to revisit this ADR.
- ⚠️ Swapping fakes in instrumented tests is slightly more manual until DI is added.
