# 03 — Detection Rules

This is the logic that decides whether an app is dangerous. It lives in
`domain/risk/RiskEngine.kt` and is **pure, deterministic, and fully tested**
(`app/src/test/.../RiskEngineTest.kt`). No machine learning, no cloud — every verdict can be
explained to the user in plain words. See [ADR-0009](decisions/0009-deterministic-rule-engine.md).

## The six signals

Each signal is one fact we can read about an app using public Android APIs (no root). Each maps
directly to a documented SpyNote/SpyMax behaviour (sources at the bottom).

| Signal | What it means | Why it's a red flag | How we read it |
|---|---|---|---|
| `ACCESSIBILITY` | App holds an enabled Accessibility Service | Malware uses it to read the screen, log keys, and auto-tap "Allow" | `AccessibilityManager.getEnabledAccessibilityServiceList()` |
| `DEVICE_ADMIN` | App is an active Device Admin | Used to **block uninstall** and persist | `DevicePolicyManager.getActiveAdmins()` |
| `SMS_ACCESS` | App can read/receive SMS (and it's granted) | Steals bank **OTP / 2FA** codes | `PackageInfo.requestedPermissions` + granted flags |
| `SIDELOADED` | Installed from outside an app store | Exactly how scam APKs spread (WhatsApp/phishing) | `getInstallSourceInfo()` (API 30+) |
| `HIDDEN_ICON` | User-installed app with no launcher icon | Malware hides from the home screen & Recents | no `ACTION_MAIN`/`LAUNCHER` activity |
| `IMPERSONATION` | Name fakes a trusted app, but isn't it | Disguises as "System Update", "Play Services", a bank | `KnownApps.isImpersonating()` |

## How signals combine into a verdict

The engine checks **scariest first** (`RiskEngine.levelFor()`):

| Verdict | Condition | Meaning to user |
|---|---|---|
| 🔴 **CRITICAL** | The "spyware trinity": Accessibility **AND** Device Admin **AND** (SMS **or** notification access); **or** **impersonation + any spy/control power** (a fake "System Update" that can read the screen, your OTPs, or resist removal) | Almost certainly malicious. Act now. |
| 🟠 **HIGH** | Strong pairs: sideloaded+accessibility, sideloaded+admin, accessibility+admin, accessibility+SMS, admin+SMS (and the notification-access equivalents); **or** impersonation on its own; **or** hidden-icon + any other flag | Dangerous unless you clearly trust it |
| 🟡 **SUSPICIOUS** | A single mild clue: sideloaded, accessibility, admin, or hidden icon, on its own | Worth a glance |
| 🟢 **LOW** | Nothing notable (SMS access *alone* is too common to flag) | Looks okay |

## Avoiding false alarms (this matters a lot)

A non-technical user must **not** be terrified about their own legitimate apps. Two guards:

1. **Install source is the great disambiguator.** A *Play-Store* app holding Accessibility is
   probably a password manager or a screen reader, so it's only `SUSPICIOUS`. A *sideloaded* app
   holding the same permission is `HIGH`. Same permission, very different meaning.
2. **Allowlist.** System apps (`FLAG_SYSTEM`) and a short list of known-good packages
   (`KnownApps.TRUSTED_PACKAGES`, e.g. TalkBack the screen reader) are capped at `LOW` and clearly
   labelled "built-in or well-known app". The detail screen still shows their signals for
   transparency.

The copy for `SUSPICIOUS` deliberately says *"this may be perfectly fine if it's something you
installed on purpose — like a password manager or a screen reader."*

## Limits & honest caveats

- These are **heuristics**, not proof. We say "looks risky," never "this is a virus."
- A brand-new, never-seen malware that avoids all six signals could be missed. The signals target
  the *behavioural* fingerprint these RATs need to steal money, which is hard for them to avoid.
- The impersonation list is small and hand-maintained; it ships in-app and grows with app updates
  (we never fetch rules online — [ADR-0001](decisions/0001-fully-offline-no-internet.md)).

## Changing the rules

1. Edit `RiskEngine` (logic) or `KnownApps` (lists).
2. **Add or update a test** in `RiskEngineTest` — never change a rule without a test proving the
   new behaviour. See [Testing Strategy](07-testing-strategy.md).
3. Run `./gradlew testDebugUnitTest`.

## Sources

These confirm the signals reflect real SpyNote/SpyMax/Android-banking-trojan behaviour:

- [The Hacker News — SpyNote records audio & calls, abuses Accessibility](https://thehackernews.com/2023/10/spynote-beware-of-this-android-trojan.html)
- [McAfee — SpyNote attacks utility users (courier/utility lures)](https://www.mcafee.com/blogs/other-blogs/mcafee-labs/android-spynote-attacks-electric-and-water-public-utility-users-in-japan/)
- [Malwarebytes (2025) — new Android malware hides in fake ID/news apps](https://www.malwarebytes.com/blog/news/2025/11/sneaky-new-android-malware-takes-over-your-phone-hiding-in-fake-news-and-id-apps)
- [Infosecurity Magazine — SpyNote targets financial institutions](https://www.infosecurity-magazine.com/news/spynote-spyware-financial/)
- [Cybersecurity News — banking trojan abuses Accessibility for control](https://cybersecuritynews.com/android-banking-trojan-overlayphantom/)
