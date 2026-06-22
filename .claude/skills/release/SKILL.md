---
description: Walk the Saavdhan release runbook — tag, build, verify, and publish a new version. Human-triggered only.
disable-model-invocation: true
---

# Release runbook

Trigger manually with `/release`. Several steps are **human-only** — call those out, don't fake them.

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Open a PR with the bump; **the human merges it to `main`** (direct push is blocked).
3. Tag the release: `git tag vX.Y.Z` (match `versionName`).
4. Build the signed release: `./gradlew assembleRelease` (the keystore must be present).
5. Verify the signature: `apksigner verify app/build/outputs/apk/release/saavdhan.apk`.
6. Publish the public mirror: `context/scripts/publish_mirror.sh` (pushes the tag).
7. Create the GitHub release:
   `gh release create vX.Y.Z app/build/outputs/apk/release/saavdhan.apk --repo kalpitt/saavdhan`.

Notes:
- Gradle pins the APK name to `saavdhan.apk` so the `/download/` URL stays stable forever.
- Play Store steps (paid dev account, data-safety answers) live in `docs/11-play-store-prep.md`.
- This mirrors the runbook in `context/STATE.md`; keep them consistent if the process changes.
