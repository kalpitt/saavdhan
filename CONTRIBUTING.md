# Contributing to Saavdhan

Thank you for helping make people safer from scams. 🙏 Contributions of all kinds are welcome —
code, translations, detection ideas, documentation, and real-world testing on different phones.

## The five principles (non-negotiable)

Every change must respect these. They're what make Saavdhan trustworthy:

1. **Fully offline** — never add the `INTERNET` permission or any networking. (We even strip it
   from merged dependencies; see [ADR-0001](docs/decisions/0001-fully-offline-no-internet.md).)
2. **Detective + guide, not enforcer** — never try to silently change/remove another app; deep-link
   the user to the right screen and coach them.
3. **Explainable, not magic** — detection stays deterministic and reason-giving. No cloud, no ML.
4. **Calm under panic** — simple language, big targets, bilingual (Hindi + English).
5. **Honest about limits** — if Android blocks something, say so in the UI.

## Before you start

- Read the **[docs](docs/README.md)** — especially [Architecture](docs/02-architecture.md),
  [Detection Rules](docs/03-detection-rules.md), and [Coding Standards](docs/06-coding-standards.md).
- For anything non-trivial, open an issue first so we can agree on the approach.

## Setting up

See **[docs/08-build-and-run.md](docs/08-build-and-run.md)** (beginner-friendly). In short:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest assembleDebug :app:lintDebug
```

## Definition of Done (every PR)

1. **Builds**: `./gradlew assembleDebug`.
2. **Tests pass**: `./gradlew testDebugUnitTest`. **If you change a detection rule, you must
   add/update a test** that proves the new behaviour (`RiskEngineTest` / `KnownAppsTest`).
3. **Lint clean**: `./gradlew :app:lintDebug` (0 errors).
4. **No hardcoded user-facing strings** — add them to `res/values/strings.xml` **and**
   `res/values-hi/strings.xml`.
5. **Architectural choices get an [ADR](docs/decisions/README.md).**
6. The offline rule still holds (no `INTERNET` in the merged manifest).

## Especially wanted

- 🌐 **More languages** (add a `values-xx/strings.xml`).
- 🔎 **New detection signals / impersonation names** (with tests).
- 📱 **Testing on real phones** — deep-link behaviour varies by manufacturer; tell us what you see.
- ♿ **Accessibility** improvements.

## Commits & PRs

- Small, focused commits with clear messages.
- Reference the issue you're addressing.
- Describe what you tested (and on which Android version / device).

## Code style

Kotlin official style; immutability first; comment the *why* (point at the relevant ADR). Details in
[Coding Standards](docs/06-coding-standards.md).
