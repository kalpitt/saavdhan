# Architecture Decision Records (ADRs)

An ADR is a short note capturing **one** important decision and **why** we made it. They follow
[Michael Nygard's format](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
(Context · Decision · Consequences), the most widely used ADR style.

**Rules:**
- One decision per record. Keep it short.
- ADRs are **append-only**: we never rewrite history. If we change our mind, we add a new ADR that
  *supersedes* the old one and update the old one's status.
- The code points back here: at each architectural seam, a comment names the relevant ADR.

## Index

| # | Decision | Status |
|---|----------|--------|
| [0001](0001-fully-offline-no-internet.md) | Fully offline — never declare INTERNET | Accepted |
| [0002](0002-detective-not-enforcer.md) | Detective + guide, not enforcer | Accepted |
| [0003](0003-compose-material3.md) | Kotlin + Jetpack Compose + Material 3 | Accepted |
| [0004](0004-layered-architecture.md) | Pure, layered, testable architecture | Accepted |
| [0005](0005-query-all-packages.md) | QUERY_ALL_PACKAGES with graceful degradation | Accepted |
| [0006](0006-sharedprefs-for-locale.md) | SharedPreferences for the language choice | Accepted |
| [0007](0007-scan-model.md) | On-demand scan + background watchdog; no own Accessibility service | Accepted |
| [0008](0008-overlay-coach.md) | Floating overlay coach via SYSTEM_ALERT_WINDOW | Accepted |
| [0009](0009-deterministic-rule-engine.md) | Deterministic, explainable rule engine (no ML/cloud) | Accepted |
| [0010](0010-sdk-levels.md) | minSdk 24, target/compile SDK 35 | Accepted |
| [0011](0011-no-di-framework-yet.md) | No dependency-injection framework yet | Accepted |

## Statuses
**Proposed** → under discussion · **Accepted** → in effect · **Superseded** → replaced by a newer ADR.
