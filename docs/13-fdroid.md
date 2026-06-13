# 13 — F-Droid submission

F-Droid is a community app store that **builds every app from source** on its own servers and
signs it with its own key. That makes it the most on-brand store for Saavdhan: it independently
proves the "fully offline, no tracking" promise — nobody has to take our word for it.

This doc has everything needed to submit. The in-repo half (listing metadata) is **done**; the
other half (a build recipe submitted to F-Droid's data repo) is a human step, pre-written below.

## 1. Why Saavdhan qualifies cleanly

F-Droid only accepts free/open-source apps with no proprietary dependencies or anti-features.
Saavdhan passes on every count:

| Requirement | Status |
|---|---|
| FOSS license | ✅ MIT |
| Source public & buildable | ✅ `github.com/kalpitt/saavdhan`, plain Gradle, builds with `assembleRelease` |
| No proprietary dependencies | ✅ only AndroidX / Compose / JUnit (all Apache-2.0). **No** Google Play Services, Firebase, or analytics SDKs |
| No tracking / ads | ✅ none |
| Non-free network services | ✅ none — the app holds **no INTERNET permission** |
| Anti-features | ✅ none. `QUERY_ALL_PACKAGES` is used (it's a security scanner), but that is not an anti-feature; the description explains it |

Release builds succeed **without** `keystore.properties` (signing is conditional in
`app/build.gradle.kts`), so F-Droid's build server produces an unsigned APK and signs it with the
F-Droid key — exactly what's required.

## 2. What's already in the repo (this PR)

F-Droid auto-reads localized listing text and screenshots from `fastlane/metadata/android/` in the
app's own repo. Provided for **en-US** and **hi-IN**:

```
fastlane/metadata/android/<locale>/
├── title.txt                      # "Saavdhan" / "सावधान"
├── short_description.txt           # ≤80-char summary
├── full_description.txt            # the store description
├── changelogs/4.txt                # notes for versionCode 4 (v0.4.0)
└── images/phoneScreenshots/*.png   # from the emulator (EN: home/results/detail/cleanup; HI: results/language)
```

The app icon is taken automatically from the APK's launcher icon — no separate upload needed.

## 3. The build recipe (human step — submit to fdroiddata)

F-Droid's build metadata lives in **gitlab.com/fdroid/fdroiddata**, not in this repo. Add a file
`metadata/com.saavdhan.app.yml` there with:

```yaml
Categories:
  - Security
License: MIT
AuthorName: Kalpit Tiwari
AuthorEmail: tiwari.kalpit@gmail.com
WebSite: https://kalpitt.github.io/saavdhan/
SourceCode: https://github.com/kalpitt/saavdhan
IssueTracker: https://github.com/kalpitt/saavdhan/issues
Changelog: https://github.com/kalpitt/saavdhan/releases

RepoType: git
Repo: https://github.com/kalpitt/saavdhan.git

Builds:
  - versionName: 0.4.0
    versionCode: 4
    commit: v0.4.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.4.0
CurrentVersionCode: 4
```

`UpdateCheckMode: Tags` + `AutoUpdateMode: Version` means F-Droid automatically picks up each new
`vX.Y.Z` git tag — so future releases need no further action here.

### Steps
1. Fork **gitlab.com/fdroid/fdroiddata**.
2. Add `metadata/com.saavdhan.app.yml` (above).
3. *(Optional, needs `fdroidserver` + Docker)* dry-run locally: `fdroid build com.saavdhan.app:4`.
4. Open a **merge request**. (Alternatively, open a *Request For Packaging* (RFP) issue and let an
   F-Droid maintainer write the recipe — slower, but zero local tooling.)
5. F-Droid builds from the `v0.4.0` tag on the public mirror, signs it, and publishes to the catalogue.

## 4. The one caveat to know

F-Droid signs with **its own key**, so the F-Droid build and the GitHub-Releases / Play build have
**different signatures**. Android won't update across signatures — a phone has to pick one source
and stick with it (switching means uninstall → reinstall). Decide a primary channel per audience:
Play/direct-APK for family installs, F-Droid for privacy-conscious users who want a source-verified
build. Note it on the landing page if both are offered.

*(Optional later: enable reproducible builds so F-Droid can verify our APK byte-for-byte and show
the "built and signed by the developer" badge.)*
