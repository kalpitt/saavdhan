# ADR-0012 — The detail-screen explanation is ranked by the engine's own weights

**Status:** Accepted · **Date:** 2026-06-21

## Context
The risk engine scores each signal very differently — impersonation is worth 50 points,
Accessibility / Device Admin / a hidden icon 40 each, reading SMS or notifications 20, and a recent
install or an as-yet-ungranted SMS request only 10 ([ADR-0009](0009-deterministic-rule-engine.md)).
But the app detail screen used to render `assessment.signals` in the order they happened to be
*collected*, giving every reason an identical visual weight. The worst consequence: impersonation —
the single most damning clue — was added **last** in `collectSignals`, so on a CRITICAL
impersonation app it appeared at the *bottom* of "Why this looks risky", beneath weaker clues like
"installed from outside the Play Store".

The engine's priority therefore existed only inside a Kotlin data class. A panicking, non-technical
user reading the list could not tell the decisive evidence from the supporting context. An
explanation that doesn't reflect the engine's own judgement is not really explainable.

## Decision
1. The per-signal scores become a **single source of truth** — a `WEIGHTS` map in `RiskEngine` —
   that both `calculateScore` (which sums it) and the UI (via the new public `RiskEngine.weightOf`)
   read. The score and the explanation can no longer drift apart.
2. The detail screen ranks the reasons **most-damning-first** using those weights, and, when an app
   shows both decisive powers and soft circumstantial clues, splits them into two tiers: the
   dangerous capabilities (weight ≥ 20) shown prominently under "Why this looks risky", and the
   soft clues (a recent install, an ungranted SMS request) demoted, quieter, under "Also noticed".
   The tier split is purely weight-derived; it appears only when both tiers are non-empty, so a
   single-clue app still reads as one clean list.

## Consequences
- ✅ The smoking gun always leads. Impersonation, the loudest lie, is now first, not last.
- ✅ The engine's priority finally reaches the user — the same numbers that decide the verdict decide
   what they read first. No second, drift-prone ranking to maintain.
- ✅ Removed the duplicated magic numbers in `calculateScore`; a new signal without a weight now
   fails fast (`getValue`), guarded by a unit test that every signal has a weight.
- ⚠️ The presentation threshold (≥ 20 = decisive) lives in the UI layer, tied by a comment to the
   engine's weight tiers. If the weights are re-tuned, the comment must be revisited.

See [Detection Rules](../03-detection-rules.md) and `AppDetailScreen.kt`.
