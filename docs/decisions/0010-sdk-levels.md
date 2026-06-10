# ADR-0010 — minSdk 24, target/compile SDK 35

**Status:** Accepted · **Date:** 2026-06-09

## Context
The people most at risk often use older, budget Android phones, so we want broad device coverage.
But targeting a recent SDK is required by Google Play and gives us modern, safer APIs. Some APIs we
rely on (e.g. `getInstallSourceInfo`) only exist on API 30+.

## Decision
- **minSdk 24** (Android 7.0) — runs on the large base of older phones.
- **targetSdk / compileSdk 35** (Android 15) — modern behaviour and Play compliance.
- Guard newer APIs with version checks and provide fallbacks (e.g. `getInstallSourceInfo` on 30+,
  deprecated `getInstallerPackageName` below 30).

## Consequences
- ✅ Wide reach without giving up modern APIs.
- ✅ Forces us to write version-aware code (good discipline), already done in `AppScanner`.
- ⚠️ Slightly more conditional code paths and testing across versions.
- ⚠️ On very old devices, some signals (precise install source) are weaker; the engine still works
  with the signals it has.
