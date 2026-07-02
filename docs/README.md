# Saavdhan — Project Documentation

This folder is the **single source of truth** for *why* Saavdhan is built the way it is. If you
ever wonder "why did we do it this way?", the answer should live here. New to the project? Read
in this order.

> **Saavdhan** (सावधान, "be alert") is a purely defensive, fully-offline Android app that helps a
> non-technical person find and safely remove scam/spyware apps from their phone.

This is an open-source project (MIT). New contributors: start with the
[root README](../README.md) and [CONTRIBUTING](../CONTRIBUTING.md), then come back here for depth.

## Map of the docs

| # | Doc | What it covers |
|---|-----|----------------|
| 01 | [Vision & Scope](01-vision-and-scope.md) | The problem, who it's for, what's in/out of each phase |
| 02 | [Architecture](02-architecture.md) | How the code is organised: layers, modules, data flow |
| 03 | [Detection Rules](03-detection-rules.md) | The danger signals, the scoring engine, and the real-world threats behind them |
| 04 | [OS Constraints](04-os-constraints.md) | What Android lets us do and what it blocks — the honest capability map |
| 05 | [Security & Privacy](05-security-and-privacy.md) | The offline rule, threat model, what data we touch |
| 06 | [Coding Standards](06-coding-standards.md) | Our rules for the code — and how they prevent "vibe-coded" fragility |
| 07 | [Testing Strategy](07-testing-strategy.md) | What we test, why, and how to run it |
| 08 | [Build & Run](08-build-and-run.md) | Set up the tools, build the app, run it on the emulator |
| 09 | [Glossary](09-glossary.md) | Plain-language dictionary of every dev/Android term we use |
| 10 | [Roadmap](10-roadmap.md) | Phase-by-phase plan and status — what's shipped, what's next |
| — | [Decision Records](decisions/README.md) | One short file per important decision, and *why* we made it |

## The five rules that never change

1. **Fully offline.** The app never declares the `INTERNET` permission, so it *cannot* make a
   network call. ([ADR-0001](decisions/0001-fully-offline-no-internet.md))
2. **Detective + guide, not enforcer.** We detect and guide; we never pretend to auto-fix what
   Android only lets the user fix. ([ADR-0002](decisions/0002-detective-not-enforcer.md))
3. **Explainable, not magic.** Every verdict the app reaches can be explained to the user in
   plain words. No black-box ML. ([ADR-0009](decisions/0009-deterministic-rule-engine.md))
4. **Calm under panic.** The person using this may be scared and non-technical. Big buttons,
   simple words, one clear action at a time, Hindi or English.
5. **Honest about limits.** When Android blocks something, we say so in the UI rather than fake it.

## How we keep this honest

- Every architectural "seam" in the code has a short comment pointing back to the ADR that
  explains it. The ADR is the long-form *why*; the code comment is the breadcrumb.
- Decisions are append-only. We don't delete an ADR when we change our mind — we add a new one
  that supersedes it, so the history of *why* stays intact.
