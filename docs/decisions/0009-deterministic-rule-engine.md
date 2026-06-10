# ADR-0009 — Deterministic, explainable rule engine (no ML / no cloud)

**Status:** Accepted · **Date:** 2026-06-09

## Context
We could detect malware with an on-device machine-learning model or a cloud-scanning service. But
our users are non-technical and scared, and the app is fully offline ([ADR-0001](0001-fully-offline-no-internet.md)).
A verdict the user can't understand ("the AI says it's bad") doesn't build trust, and an ML model
is hard to explain, test, and ship offline.

## Decision
Use a **deterministic, rule-based engine** (`RiskEngine`) over a small set of explainable signals.
Every verdict carries the exact list of reasons behind it, which the UI shows in plain language.

## Consequences
- ✅ Fully explainable: "This app is dangerous because it can read your screen, read your OTPs, and
  blocks removal." Builds trust and educates.
- ✅ Easy to unit-test exhaustively and to reason about; no training data, no model files, no cloud.
- ✅ Runs instantly, offline, on old budget phones.
- ⚠️ Won't catch threats that avoid all known signals as well as a behavioural ML model might. We
  target the *behavioural fingerprint* these RATs need to steal money, which is hard to avoid.
- ⚠️ Rules are hand-maintained and update only via app releases.

See [Detection Rules](../03-detection-rules.md).
