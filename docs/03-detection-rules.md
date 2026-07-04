# 03 — Detection Rules

This is the logic that decides whether an app is dangerous. It lives in
`domain/risk/RiskEngine.kt` and is **pure, deterministic, and fully tested**
(`app/src/test/.../RiskEngineTest.kt`). No machine learning, no cloud — every verdict can be
explained to the user in plain words. See [ADR-0009](decisions/0009-deterministic-rule-engine.md).

## The thirteen signals

Each signal is one fact we can read about an app using public Android APIs (no root). Each maps
directly to a documented SpyNote/SpyMax behaviour (sources at the bottom). Every signal carries a
**point weight** — the single source of truth for "how damning is this clue," defined once in
`RiskEngine.WEIGHTS` and reused by the UI to rank the reasons it shows (see
[ADR-0012](decisions/0012-explanation-ranked-by-engine-weight.md)).

| Signal | Weight | What it means | Why it's a red flag | How we read it |
|---|---|---|---|---|
| `IMPERSONATION` | 50 | Name fakes a trusted app, but isn't it | Disguises as "System Update", "Play Services", a bank | `KnownApps.isImpersonating()` — fuzzy (Levenshtein) match after normalizing case/punctuation/emoji, so "System-Update ⬇️" still matches |
| `SIDELOADED_VIA_MESSENGER` | 40 | Sideloaded, and traced back to WhatsApp/Telegram/another messenger | The real-world scam delivery chain: a fake wedding invite/courier/KYC APK sent straight over chat | `originatingPackageName` (API 30+) resolved against `KnownApps.MESSENGERS` |
| `ACCESSIBILITY` | 40 | App holds an enabled Accessibility Service | Malware uses it to read the screen, log keys, and auto-tap "Allow" | `AccessibilityManager.getEnabledAccessibilityServiceList()` |
| `DEVICE_ADMIN` | 40 | App is an active Device Admin | Used to **block uninstall** and persist | `DevicePolicyManager.getActiveAdmins()` |
| `HIDDEN_ICON` | 40 | Sideloaded app with no launcher icon | Malware hides from the home screen & Recents | no `ACTION_MAIN`/`LAUNCHER` activity (only checked for sideloaded apps — store apps may legitimately lack one) |
| `LURE_LABEL` | 30 | Sideloaded app *named* like a scam bait file | A real wedding invitation, bill, challan, or parcel notice is a **document, never an app** — the 2026 India campaigns ("Wedding Invitation", "Mahavitaran Bill Update", "E-Challan") wear the document's name as the disguise | `KnownApps.isLureLabel()` — normalized-label containment of a curated phrase list, sideloaded apps only |
| `INSTALL_PACKAGES_REQUESTED` | 25 | Sideloaded app asks for the power to install *other* apps | The two-stage **dropper** tell: a harmless-looking first app sneaks in the real spyware (SpyMax wedding dropper; SecuriDropper-class loaders that dodge Android 13+ Restricted Settings) | `REQUEST_INSTALL_PACKAGES` in `requestedPermissions`, sideloaded apps only |
| `NOTIFICATION_LISTENER` | 20 | App can read all posted notifications | Can read OTP codes delivered as notifications, bypassing SMS permission entirely | `NotificationListenerService` binding check |
| `SIDELOADED` | 20 | Installed from outside an app store | Exactly how scam APKs spread (WhatsApp/phishing) | `getInstallSourceInfo()` (API 30+) |
| `SMS_ACCESS` | 10 | App can read/receive SMS (and it's granted) | Steals bank **OTP / 2FA** codes | `PackageInfo.requestedPermissions` + granted flags |
| `SMS_REQUESTED` | 10 | Sideloaded app *asks* for SMS access, not yet granted | An early-stage warning before the permission is even accepted | requested-but-not-granted permission flag |
| `ACCESSIBILITY_DECLARED` | 15 | Sideloaded app's manifest **declares** an Accessibility Service that isn't switched on yet | The RAT was *built* to take over the screen — this catches it **before** the victim is talked into tapping "Allow" (once enabled, `ACCESSIBILITY` takes over; never both at once, mirroring the `SMS_REQUESTED` pattern) | a `BIND_ACCESSIBILITY_SERVICE` service in `PackageInfo.services`, sideloaded apps only |
| `NEW_INSTALL` | 10 | Installed within the last 24 hours | Circumstantial — freshly installed apps deserve a closer look | `firstInstallTimeMillis` |

## How signals combine into a verdict

The engine is **point-based**, not a boolean cascade: every collected signal's weight is summed
into a score, and the score decides the tier (`RiskEngine.levelForScore()`):

| Verdict | Score | Meaning to user |
|---|---|---|
| 🔴 **CRITICAL** | ≥ 80 | Almost certainly malicious. Act now. (E.g. impersonation + Accessibility = 90; a **fresh wedding-invite APK from WhatsApp with nothing granted yet** = messenger 40 + lure name 30 + installer power 25 + declared-but-off Accessibility 15 = 110 — caught at install time.) |
| 🟠 **HIGH** | ≥ 50 | Dangerous unless you clearly trust it. (E.g. impersonation alone = 50; Accessibility + sideloaded = 60; a sideloaded APK named "E-Challan" = 50; messenger-delivered dropper = 65.) |
| 🟡 **SUSPICIOUS** | ≥ 20 | Worth a glance. (E.g. sideloaded alone = 20; a sideloaded store like F-Droid that can install apps = 45; a sideloaded tool with a declared-but-off Accessibility service, like Bitwarden's F-Droid build = 35.) |
| 🟢 **LOW** | < 20 | Nothing notable (SMS access *alone* is too common to flag — it's only 10 points). |

Because the score and the on-screen explanation share the same `WEIGHTS` map, they can never
disagree — the detail screen lists reasons most-decisive-first, with soft circumstantial clues
(like `NEW_INSTALL`) split into a quieter "Also noticed" group underneath.

## Avoiding false alarms (this matters a lot)

A non-technical user must **not** be terrified about their own legitimate apps. Several guards:

1. **Install source is a great disambiguator.** A *Play-Store* app holding Accessibility is
   probably a password manager or a screen reader, so it only scores 40 (SUSPICIOUS territory,
   not CRITICAL). A *sideloaded* app holding the same permission scores higher once combined with
   other signals. Same permission, very different meaning.
2. **Signature verification is an absolute override.** If an app's signing certificate hash
   matches `KnownApps.TRUSTED_SIGNATURES` (a hardcoded set of trusted keys), it is trusted
   unconditionally — no score is even computed.
3. **Allowlist, power-gated.** System apps (`FLAG_SYSTEM`) and known-good packages
   (`KnownApps.TRUSTED_PACKAGES`, e.g. TalkBack the screen reader) — plus trusted package
   *prefixes* like `com.google.android.*` — are trusted, but that trust is revoked if a
   non-Play-Store install of that exact name/prefix holds a dangerous power (Accessibility,
   Device Admin, or granted SMS). This closes the "fake a trusted prefix" spoofing gap.
4. **Fuzzy impersonation matching is name-only, not power-based.** Matching a disguise name never
   raises risk by itself unless it's a *confident* fuzzy match (small edit distance) — this keeps
   the false-positive rate low while still catching "System  Update!" vs "SYSTEM_UPDATE" variants.
5. **The three 2026-campaign signals are all sideload-gated.** `LURE_LABEL`,
   `INSTALL_PACKAGES_REQUESTED`, and `ACCESSIBILITY_DECLARED` never fire for Play/store/system
   installs — so the entire legitimate Indian banking/UPI/govt ecosystem (which arrives via
   Play) is immune by construction. The lure list is precision-curated ("wedding card" is
   deliberately excluded because legit card-maker apps exist; bare "courier"/"kyc"/"bill" are
   too broad). An officially sideloaded WhatsApp (whatsapp.com APK) requests the installer
   permission but is cleared by the `TRUSTED_SIGNATURES` override before scoring. And
   `ACCESSIBILITY_DECLARED` is suppressed the moment the service is actually enabled, so a
   sideloaded password manager is never double-counted — its verdict once enabled is identical
   to what it was before this signal existed.

The copy for `SUSPICIOUS` deliberately says *"this may be perfectly fine if it's something you
installed on purpose — like a password manager or a screen reader."*

## Limits & honest caveats

- These are **heuristics**, not proof. We say "looks risky," never "this is a virus."
- A brand-new, never-seen malware that avoids all ten signals could be missed. The signals target
  the *behavioural* fingerprint these RATs need to steal money, which is hard for them to avoid.
- The impersonation list is small and hand-maintained; it ships in-app and grows with app updates
  (we never fetch rules online — [ADR-0001](decisions/0001-fully-offline-no-internet.md)).
- `TRUSTED_SIGNATURES` is hardcoded and only as current as the last app release — a legitimate
  app's key rotation won't be recognized until Saavdhan itself updates.

## Changing the rules

1. Edit `RiskEngine` (logic, weights, thresholds) or `KnownApps` (lists, messengers, trusted
   signatures).
2. **Add or update a test** in `RiskEngineTest` — never change a rule without a test proving the
   new behaviour. See [Testing Strategy](07-testing-strategy.md).
3. Run `./gradlew testDebugUnitTest`.

## Sources

These confirm the signals reflect real SpyNote/SpyMax/Android-banking-trojan behaviour:

- [K7 Labs — SpyMax fake wedding-invitation app targeting Indian users (dropper installs hidden payload, declares Accessibility, asks to be Home app)](https://labs.k7computing.com/index.php/spymax-a-fake-wedding-invitation-app-targeting-indian-mobile-users/)
- [CERT-In / Cyber Swachhta Kendra — SpyMax Android malware alert](https://www.csk.gov.in/alerts/SpyMax_android_malware.html)
- [Broadcom — Wedding Invite scam deploys SpyMax RAT on Indian Android devices](https://www.broadcom.com/support/security-center/protection-bulletin/wedding-invite-scam-deploys-spymax-rat-on-indian-android-devices)
- [The Logical Indian — Bengaluru man loses ₹5 lakh to a WhatsApp wedding-invite APK (April 2026)](https://thelogicalindian.com/bengaluru-businessman-loses-%E2%82%B95-lakh-after-downloading-fake-whatsapp-wedding-invite-apk-file/)
- [the420.in — Karnataka APK-fraud complaints up ~190% in four months of 2026 (KYC / bill / courier lures, "Mahavitaran Bill Update.apk")](https://the420.in/karnataka-apk-fraud-surge-senior-citizens-cybercrime/)
- [Cyble — fake RTO e-challan APKs phishing Indian Android users](https://cyble.com/blog/regional-transport-office-phishing-scam-targets-android-users-in-india/)
- [McAfee Labs — Android phishing Malware-as-a-Service on the rise in India](https://www.mcafee.com/blogs/other-blogs/mcafee-labs/android-phishing-scam-using-malware-as-a-service-on-the-rise-in-india/)
- [ThreatFabric — SecuriDropper: droppers bypassing Android 13 Restricted Settings (why "can install other apps" is the delivery-chain tell)](https://www.threatfabric.com/blogs/droppers-bypassing-android-13-restrictions)
- [The Hacker News — SpyNote records audio & calls, abuses Accessibility](https://thehackernews.com/2023/10/spynote-beware-of-this-android-trojan.html)
- [McAfee — SpyNote attacks utility users (courier/utility lures)](https://www.mcafee.com/blogs/other-blogs/mcafee-labs/android-spynote-attacks-electric-and-water-public-utility-users-in-japan/)
- [Malwarebytes (2025) — new Android malware hides in fake ID/news apps](https://www.malwarebytes.com/blog/news/2025/11/sneaky-new-android-malware-takes-over-your-phone-hiding-in-fake-news-and-id-apps)
- [Infosecurity Magazine — SpyNote targets financial institutions](https://www.infosecurity-magazine.com/news/spynote-spyware-financial/)
- [Cybersecurity News — banking trojan abuses Accessibility for control](https://cybersecuritynews.com/android-banking-trojan-overlayphantom/)
