# AGENTS.md — Operating manual for AI agents working on Saavdhan

> **Read this file first, every session, no matter which AI tool you are.**
> `AGENTS.md` is the cross-tool standard (read natively by Claude Code, Codex, Cursor,
> Aider, Copilot, Gemini CLI, Windsurf, and more). It is the single source of truth for
> *how to work in this repo*. It exists so that work is never trapped inside one chat.

If you are a human: you normally don't need to read this — it's written for the AI. But it's
plain English, so skim it if you're curious what your assistant is being told.

---

## 0. The 30-second orientation (do this, in order)

1. Read **this file** (rules, commands, map — below).
2. Read **[`context/STATE.md`](context/STATE.md)** — where the project is *right now* and the next steps.
3. Read the newest file in **[`context/handoffs/`](context/handoffs/)** — the story of the last session.
4. Read **[`context/PROFILE.md`](context/PROFILE.md)** — who the human is and how to work with them.
5. Now you are caught up. Start working.

Before you stop or run low on context, follow the **end-of-session ritual** in
[`context/README.md`](context/README.md). That ritual is what keeps this system alive.

---

## 1. What this project is

**Saavdhan** (सावधान, "be alert") is a native Android (Kotlin) **defensive, fully-offline
anti-scam app** for non-technical Indian family members. It detects scam/spyware apps
(SpyNote/SpyMax banking trojans spread over WhatsApp as fake wedding invites / courier / KYC /
electricity-bill lures), explains the danger in plain Hindi or English, and guides the user to
the exact fix screen in one tap.

It is a **detective + guide, never an enforcer**. Android deliberately stops one app from
turning off another app's Accessibility / Device-Admin / airplane state, so we deep-link the
user to the right screen and are honest about the wall — we never fake an auto-fix.

Full vision & scope: [`docs/01-vision-and-scope.md`](docs/01-vision-and-scope.md).
Architecture: [`docs/02-architecture.md`](docs/02-architecture.md).

---

## 2. 🔒 THE HARD RULE — never break this

**The app must NEVER touch the internet.** It must never declare the `INTERNET` permission and
must never connect to any external/third-party service. This is the whole security promise of
the product. It is enforced three ways, all of which must stay true:

1. `INTERNET` is **not** declared in `app/src/main/AndroidManifest.xml`.
2. The manifest actively strips any dependency-merged INTERNET:
   `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />`.
3. CI fails the build if the **merged** manifest contains INTERNET
   (see [`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

If a task ever seems to need a network call, an API, telemetry, crash reporting, or a remote
config — **stop and flag it to the human.** Detection rules ship in-app and update only via app
updates. Rationale: [`docs/decisions/0001-fully-offline-no-internet.md`](docs/decisions/0001-fully-offline-no-internet.md).

---

## 3. How to build, test, and run

This is a standard Gradle Android project. Tests run on your computer (no phone needed); the
app runs on an emulator or device.

**Environment (this machine — macOS):**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # bundled JDK 21
export ANDROID_HOME="$HOME/Library/Android/sdk"
```
*In general:* any JDK 17+ works; Android Studio bundles one. The Gradle wrapper
(`./gradlew`) pins the correct Gradle version, so never run a globally-installed `gradle`.

**The commands you'll actually use:**
```bash
./gradlew testDebugUnitTest          # run the unit tests (the risk/cleanup brain) — fast, do this often
./gradlew assembleDebug              # build the debug APK
./gradlew :app:lintDebug             # Android lint (catches manifest/string/a11y issues)
./gradlew testDebugUnitTest assembleDebug :app:lintDebug   # the full pre-commit check
```

**Emulator:** AVD name is `saavdhan_pixel`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

**Definition of done for any change:** the full pre-commit check above passes (tests + build +
lint), and — for UI/behaviour changes — it has been live-tested on the emulator. See
[`docs/08-build-and-run.md`](docs/08-build-and-run.md) and
[`docs/07-testing-strategy.md`](docs/07-testing-strategy.md).

---

## 4. Project map (where things live)

```
app/src/main/java/com/saavdhan/app/
├─ domain/          PURE Kotlin, NO Android imports → unit-testable "brain"
│  ├─ model/        ScannedApp, Risk (levels + signals)
│  ├─ risk/         RiskEngine — deterministic, explainable scoring
│  ├─ allowlist/    KnownApps — trusted packages + impersonation denylist
│  └─ cleanup/      CleanupEngine + CleanupModels — Phase 2 reactive checklist
├─ data/scanner/    AppScanner — reads live device state via PackageManager etc.
├─ system/          Android-facing services
│  ├─ deeplink/     SettingsDeepLinks — intent builders for every fix screen
│  ├─ overlay/      OverlayCoachService — floating step-by-step coach
│  └─ watchdog/     WorkManager job that alerts on newly-installed dangerous apps
├─ i18n/            LocaleManager — per-app Hindi/English
├─ ui/              Jetpack Compose screens (onboarding, scan, detail, settings, cleanup)
└─ MainActivity.kt  single Activity + Navigation Compose (routes)

app/src/test/...    unit tests (RiskEngineTest, CleanupEngineTest)
app/src/main/res/   values/strings.xml (English) + values-hi/strings.xml (Hindi) — MIRRORED
docs/               stable reference: vision, architecture, ADRs, testing, glossary, roadmap
context/            living state: STATE / PROGRESS / PROFILE / handoffs  ← read these each session
```

**Architecture rules (don't violate):**
- `domain/` must stay free of Android imports so it stays unit-testable. Android-specific code
  lives in `data/`, `system/`, `ui/`. ([`docs/decisions/0004-layered-architecture.md`](docs/decisions/0004-layered-architecture.md))
- **Every user-facing string lives in both** `values/strings.xml` and `values-hi/strings.xml`.
  Never hard-code user-facing text. Add the English and Hindi at the same time.
- The risk engine is **deterministic and explainable** — every verdict carries its reasons so
  the UI can explain in plain language. ([`docs/decisions/0009-deterministic-rule-engine.md`](docs/decisions/0009-deterministic-rule-engine.md))
- We are honest about OS walls (detective, not enforcer). Never claim to auto-fix what Android
  reserves for the user. ([`docs/decisions/0002-detective-not-enforcer.md`](docs/decisions/0002-detective-not-enforcer.md))

All locked decisions are in [`docs/decisions/`](docs/decisions/) (ADRs). If you make a new
significant decision, add an ADR there.

---

## 5. Working with the human

The human (Kalpit) is **bright and fast-learning but a first-time developer with zero prior
software experience.** Full profile and working agreement: [`context/PROFILE.md`](context/PROFILE.md).
The short version:

- Explain a developer-native term in plain language **the first time it appears**.
- **Flag mistake-prone steps *before* they happen** (e.g. a config typo that breaks the build).
- Don't dumb down the concepts, but don't assume tooling knowledge.
- Be **token-responsible** — high signal, no filler.
- Some steps **only the human can do** (merge a PR to `main`, flip the repo to Public, tap
  through OS screens on a real phone). Call those out clearly; don't pretend to do them.

---

## 6. Git & commits

- Work on a feature branch; **direct pushes to `main` are blocked** — the human merges via PR.
- Commit after a meaningful, building milestone. The full pre-commit check (§3) must pass first.
- Co-author trailer on commits: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
- Updating `context/` files **is part of finishing a task**, not an afterthought — commit them
  together with the code so the next chat (in any tool) starts current.
