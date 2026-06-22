---
paths:
  - "app/src/main/java/com/saavdhan/app/domain/**"
---

# Domain layer: no Android imports

- The `domain/` package is **pure Kotlin** and must stay free of Android imports — no `android.*`
  and no `androidx.*`. This is what keeps the risk/cleanup "brain" unit-testable without an
  emulator.
- Anything Android-specific (PackageManager, Context, Compose, intents) belongs in `data/`,
  `system/`, or `ui/` — never in `domain/`.
- Rationale: ADR-0004 (layered architecture), in `docs/decisions/`.
