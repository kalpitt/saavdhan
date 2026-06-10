# 04 — OS Constraints (the honest capability map)

Android deliberately stops one app from controlling another. This is good security — and it
shapes everything Saavdhan can and can't do. We design *with* these walls and are honest about
them in the UI, rather than faking an "auto-fix." See [ADR-0002](decisions/0002-detective-not-enforcer.md).

## ✅ What we CAN do (detection — all public APIs, no root)

| Capability | API |
|---|---|
| See which apps hold **Accessibility** | `AccessibilityManager.getEnabledAccessibilityServiceList()` |
| See which apps are **Device Admins** | `DevicePolicyManager.getActiveAdmins()` |
| See each app's **requested + granted permissions** (e.g. SMS) | `PackageManager.getPackageInfo(..., GET_PERMISSIONS)` |
| Tell **sideloaded vs Play Store** | `getInstallSourceInfo()` (API 30+); older fallback `getInstallerPackageName()` |
| Detect a **hidden icon** | query `ACTION_MAIN`/`CATEGORY_LAUNCHER`; none ⇒ hidden |
| List **all installed apps** | `getInstalledPackages()` **+ `QUERY_ALL_PACKAGES`** (see below) |

## ✅ What we CAN do (remediation — *take the user to the exact screen*)

| Action | API | Precision |
|---|---|---|
| Open an app's **App Info** page (Force Stop / Permissions / Uninstall) | `ACTION_APPLICATION_DETAILS_SETTINGS` + `package:` URI | **Exact, per-app** |
| Show the **Uninstall** confirmation | `ACTION_DELETE` + `package:` URI | **Exact, per-app** |
| Open the **Accessibility** list | `ACTION_ACCESSIBILITY_SETTINGS` | List only (we coach which row) |
| Open **Security / Device-Admin** settings | `ACTION_SECURITY_SETTINGS` | List only (we coach) |

## ❌ What Android BLOCKS (and our honest alternative)

| The dream | Why it's blocked | What we do instead |
|---|---|---|
| Silently turn OFF another app's Accessibility | No API exists for third-party apps | One tap to the list + coach the user to flip it |
| Silently remove another app's Device Admin | No API exists | One tap to Security settings + coach |
| Force-stop or silently uninstall another app | Not allowed without system/root | Deep-link to App Info / Uninstall prompt; **user confirms** |
| Toggle **airplane mode** for the user | Blocked for non-system apps since API 17 | Tell them to swipe down + tap Airplane mode |
| Perform a **factory reset** | Requires Device Owner / system | Guide them through Settings (Phase 2, last resort) |

## The one nasty real-world chain (Phase 2 design)

A malicious **Device Admin blocks uninstall**, and if it *also* abuses Accessibility it can
auto-close the very settings screen the user opens to disable it. The reliable defeat is
**Safe Mode** — booting with all third-party apps (and their admin/accessibility powers)
disabled, so the user can deactivate the admin and uninstall cleanly. Saavdhan's Phase-2 guided
cleanup escalates to Safe Mode exactly when it detects this resistance.

## The QUERY_ALL_PACKAGES wall

To scan *all* installed apps on Android 11+ (API 30+), an app needs the `QUERY_ALL_PACKAGES`
permission. Google Play restricts it — but **security/antivirus apps are an explicitly permitted
use case**, and because Saavdhan is fully offline it trivially satisfies the "never sell/share the
installed-app inventory" rule.

- **If shipped on Play:** declare it via the Permissions Declaration Form, justified as a security
  scanner. ([ADR-0005](decisions/0005-query-all-packages.md))
- **If sideloaded:** unrestricted.
- **If denied/limited:** the app **degrades gracefully** — it still scans what it can see and
  shows an honest "this scan may be incomplete" banner.

Sources: [Play Console — broad app visibility policy](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en),
[Android — package visibility filtering](https://developer.android.com/training/package-visibility).
