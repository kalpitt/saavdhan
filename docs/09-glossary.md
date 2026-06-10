# 09 — Glossary

Plain-language definitions for the terms used across this project and the codebase. Skim it once;
come back when a word trips you up.

## Android & app terms

- **APK** — the file format of an Android app (like a `.exe` on Windows). "Sideloading" means
  installing an APK directly instead of from a store.
- **Sideloaded** — an app installed from a file/link rather than an app store. Scam apps spread
  this way.
- **Package name** — an app's unique id, e.g. `com.whatsapp`. No two apps share one.
- **Permission** — a capability an app must be granted, e.g. reading SMS or using the camera.
- **Accessibility Service** — a powerful Android feature meant to help users with disabilities
  (e.g. screen readers). It can read the screen and tap on the user's behalf — which is why
  malware abuses it.
- **Device Admin** — an elevated role (meant for company/security tools) that, among other things,
  can block an app from being uninstalled. Malware uses it to dig in.
- **OTP** — One-Time Password, the code your bank texts you. Stealing these is the scam's goal.
- **Launcher** — the home screen. An app with no launcher icon is "hidden."
- **Safe Mode** — booting Android with all *third-party* apps temporarily disabled. The reliable
  way to remove malware that resists uninstall.
- **Deep link** — opening a specific screen directly. We deep-link to the exact Settings page for
  a risky app so the user gets there in one tap.

## Build & tooling terms

- **SDK (Software Development Kit)** — the toolkit for building apps on a platform. The **Android
  SDK** includes build tools, the platform libraries, and the emulator.
- **JDK (Java Development Kit) / Java** — the engine that compiles Kotlin/Java code into something
  Android can run.
- **Kotlin** — the modern programming language we write the app in (Google's recommended language
  for Android).
- **Gradle** — the "build robot" that turns source code into an installable app. The **wrapper**
  (`./gradlew`) is a copy of Gradle that lives inside the project so it always behaves the same.
- **Manifest (`AndroidManifest.xml`)** — the app's "identity card": its name, screens, and the
  permissions it requests.
- **Emulator** — a virtual Android phone running in a window on your computer, for testing.
- **AVD (Android Virtual Device)** — one configured emulator (e.g. "a Pixel 6 on Android 15").
- **adb (Android Debug Bridge)** — the command-line tool that talks to a phone/emulator (install
  apps, take screenshots, read logs).
- **Gradle sync** — when Android Studio reads the build files and downloads anything missing.
- **Version catalog (`libs.versions.toml`)** — one tidy file listing every external library and
  its version, so versions live in one place.

## Architecture & code terms

- **Jetpack Compose** — the modern way to build Android screens by writing Kotlin functions
  (instead of older XML layouts).
- **Material 3** — Google's design system (colours, components, spacing) that Compose uses.
- **Composable** — a Kotlin function that describes a piece of UI (e.g. `ScanScreen`).
- **ViewModel** — a class that holds a screen's data and survives screen rotations; it does the
  thinking so the UI just displays.
- **State / event (Unidirectional Data Flow)** — "state flows down" (the ViewModel gives the UI
  what to show) and "events flow up" (the UI tells the ViewModel what the user did). Keeps things
  predictable.
- **Domain layer** — the pure-logic core (our danger "brain") with no Android code, so it's easy
  to test.
- **Heuristic** — a rule-of-thumb that's usually right but not guaranteed. Our detection is
  heuristic: strong clues, not absolute proof.
- **ADR (Architecture Decision Record)** — a short note recording one important decision and *why*
  we made it. See [decisions/](decisions/README.md).
- **Allowlist / denylist** — a list of things we explicitly trust (allow) or distrust (deny). We
  allowlist known-good apps and denylist impersonated names.
- **Dependency** — an external library our app uses. Fewer, well-known dependencies = safer.
