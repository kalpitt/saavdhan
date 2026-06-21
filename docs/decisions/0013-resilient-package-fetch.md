# ADR-0013 — Resilient package fetch (survive the Binder limit on cheap phones)

**Status:** Accepted · **Date:** 2026-06-21

## Context
The scanner read every installed app in **one** `PackageManager.getInstalledPackages()` call loaded
with heavy data (`GET_PERMISSIONS | GET_SIGNING_CERTIFICATES | GET_ACTIVITIES`). That single Binder
reply is capped at ~1 MB. On a budget phone with hundreds of apps — exactly Saavdhan's target
hardware (TECNO, itel, Infinix, OEM-bloated Xiaomi/Realme) — that reply can exceed the cap and fail
in two ways:

1. **It throws** (`TransactionTooLargeException`, AOSP). The old code let this propagate: the
   on-demand scan fell into `ScanState.Error`, and the background watchdog (`NewAppScanWorker`)
   returned `Result.retry()` **forever** with exponential backoff — silently starving threat alerts.
2. **It silently truncates** (some MIUI/ColorOS ROMs return a shorter list with no error). This is the
   worst case for a security scanner: the scan "succeeds" while invisibly missing apps — a scam app
   could sit in the truncated tail and never be examined.

Either way, the user on the cheapest, most-targeted hardware could be left unprotected with nothing
they'd notice.

## Decision
Replace the single bulk call with a **resilient, two-tier fetch** (`resilientPackageFetch` in
`AppScanner.kt`):

1. Take a **cheap names-only count** first (`getInstalledPackages(0)` — tiny per-app payload, fits the
   limit even on bloated phones). This is the authoritative app count and the fallback key list.
2. Try the **fast bulk call**. Trust its result only if it did **not** throw **and** returned at least
   95% of the names count (so a silent truncation is caught, while a 1–2 app install/uninstall race is
   tolerated).
3. Otherwise **fall back** to fetching each app individually (one bounded Binder call each, on
   `Dispatchers.IO`), skipping any that fail, and mark the scan **`partial = true`** so the UI's
   existing "scan may be incomplete" note appears — honest, not a false all-clear.

The decision logic is a **generic, Android-free function** (`<T>`), so every branch is unit-tested on
the plain JVM without Robolectric (`PackageInfo` can't be created off-device, so tests use `String`).
The background watchdog also gets a **retry cap** (`runAttemptCount >= 2 → Result.success()`) so a
persistent failure can never become an unbounded retry-storm.

## Consequences
- ✅ No more silent total scan failure on the exact cheap-phone demographic we serve. The fast path is
  unchanged on healthy phones (verified: emulator scan identical, `partial=false`).
- ✅ Silent OEM truncation is now detected and recovered, not trusted.
- ✅ The watchdog degrades gracefully instead of retry-storming.
- ⚠️ On the fallback path the scan is slower (N individual Binder calls) — acceptable, since it only
  happens on phones where the fast call already failed, and the scan screen already shows progress.
- ⚠️ The 95% trust ratio is a heuristic; documented inline so it can be tuned if a device legitimately
  returns a smaller heavy-list than its name-list for some other reason.

See `AppScanner.kt` (`resilientPackageFetch`, `bulkInstalledPackages`, `lightweightPackageNames`) and
`ResilientPackageFetchTest.kt`.
