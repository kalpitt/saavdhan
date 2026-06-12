# 08 — Build & Run

A beginner-friendly record of how the project is set up and how to build/run it. If a word here
is unfamiliar, check the [Glossary](09-glossary.md).

## What's installed on this machine (and what each thing is)

| Tool | Plain-English role | Where |
|---|---|---|
| **JDK 21 (Java)** | The engine that compiles the code | Homebrew, plus one bundled inside Android Studio |
| **Android Studio** | The workshop you write/run the app in | `/Applications/Android Studio.app` |
| **Android SDK** | The Android toolkit (build tools, platform, emulator) | `~/Library/Android/sdk` |
| **Gradle (wrapper)** | The "build robot" that assembles the app | lives *inside* the project (`./gradlew`) — you never install it separately |

We build using **Android Studio's bundled Java** so the command line and the IDE behave
identically:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

## Project layout (the important files)

```
Saavdhan/
├─ settings.gradle.kts        # names the project, lists modules, sets download sources
├─ build.gradle.kts           # top-level: declares plugins
├─ gradle/libs.versions.toml  # the version catalog: every library + version in one place
├─ local.properties           # points at YOUR Android SDK (machine-specific, git-ignored)
├─ gradlew                     # the build robot launcher
├─ app/
│  ├─ build.gradle.kts         # the app module's config (SDK levels, dependencies, Compose on)
│  └─ src/
│     ├─ main/java/com/saavdhan/app/   # the Kotlin code (see Architecture doc)
│     ├─ main/res/values/strings.xml   # English text
│     ├─ main/res/values-hi/strings.xml# Hindi text
│     └─ test/java/...                  # the unit tests (the brain)
└─ docs/                       # you are here
```

## Common commands (run from the project root)

```bash
# Build the debug app (produces app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# Run the unit tests (the brain) — fast, no phone needed
./gradlew testDebugUnitTest

# Build + test together
./gradlew testDebugUnitTest assembleDebug

# If Gradle ever gets confused, stop its background process and retry
./gradlew --stop
```

> **Beginner tip — the #1 gotcha:** the build is sensitive to exact text in the `.gradle.kts` and
> `.toml` files. A wrong version number or a missing comma there stops the *whole* build with a
> long red error. When that happens, read the **first** error line (not the wall of text below
> it), and check the [version catalog](../gradle/libs.versions.toml). It's almost never your app
> code — it's the config.

## Building a signed release APK

Release builds require the keystore file at the repo root (see `keystore.properties.example`).
Without it, `assembleRelease` still runs but produces an unsigned APK.

```bash
# Produces app/build/outputs/apk/release/saavdhan.apk
# The filename is always saavdhan.apk (Gradle enforces this) so the landing-page
# direct-download link at releases/latest/download/saavdhan.apk stays stable.
./gradlew assembleRelease

# Verify the signature before uploading to GitHub Releases
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose \
  app/build/outputs/apk/release/saavdhan.apk
```

## Running it on the emulator (virtual phone)

The emulator is a real Android phone in a window on your Mac — no physical device needed.

**Easiest way:** open the project in Android Studio, pick a virtual device from the toolbar
dropdown, and press the green ▶ Run button.

**Command-line way** (what we use here):
```bash
# create a virtual device once
echo "no" | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd \
  -n saavdhan_pixel -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_6

# boot it
"$ANDROID_HOME/emulator/emulator" -avd saavdhan_pixel &

# install the app onto it
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

## Opening the project in Android Studio

`File ▸ Open` → choose the project folder. The first time, Android Studio runs a **Gradle sync**
(it reads the build files and downloads anything missing) — this is normal and may take a minute.
Because we generated the project from the command line, everything it needs is already there.
