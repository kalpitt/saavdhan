# 05 — Security & Privacy

Saavdhan is a *security* app, so it must hold itself to a higher standard than the apps it warns
about. The core promise: **it works entirely on your phone and never talks to the internet.**

## The offline guarantee (how it's enforced, not just promised)

The app **does not declare the `INTERNET` permission** in `AndroidManifest.xml`. On Android, an
app with no `INTERNET` permission is *physically prevented by the OS* from opening any network
connection — it's not a policy we follow, it's a wall the system enforces. This is the strongest
possible version of "we don't send your data anywhere." ([ADR-0001](decisions/0001-fully-offline-no-internet.md))

**How to verify it yourself** (also part of our test checklist):
```bash
# After building, search the final merged manifest — INTERNET must NOT appear:
grep -i "android.permission.INTERNET" app/build/intermediates/merged_manifests/debug/AndroidManifest.xml
# Expect: no output.
```

## What data the app touches (and never keeps)

| Data | Why | Where it goes |
|---|---|---|
| List of installed apps + their permissions | To detect danger | Held in memory during a scan only |
| The user's language choice | To show Hindi/English | One local key in SharedPreferences |
| Scan results | To show on screen | In memory; gone when the app closes |

It collects **no** personal data, **no** identifiers, and writes **no** logs off-device. There is
nowhere for data to go — there's no network.

## Permissions we request, and why

| Permission | Why | Notes |
|---|---|---|
| `QUERY_ALL_PACKAGES` | See all installed apps to scan them | Permitted security use case; [ADR-0005](decisions/0005-query-all-packages.md) |
| `SYSTEM_ALERT_WINDOW` *(Phase 1, overlay coach)* | Float step-by-step help over Settings | Requested only when first needed |
| `POST_NOTIFICATIONS` *(Phase 1, watchdog)* | Alert on a dangerous new install | Android 13+ runtime prompt |

Permissions we will **never** request: `INTERNET`, location, contacts, microphone, camera, SMS
contents. We *read which apps hold* SMS access; we never read message contents ourselves.

## Our own threat model

- **We are a juicy target.** A fake "Saavdhan" could itself be malware. Mitigations: ship through
  a trusted channel, sign consistently, and keep the real app's behaviour boringly verifiable
  (offline, minimal permissions). See [Roadmap → distribution](10-roadmap.md).
- **We must not become an attack tool.** Saavdhan only ever guides the *local* user on the
  *local* phone. It has no capability to act on another device or person.
- **We must not cause harm through false confidence.** "No dangerous apps found" never means
  "you are 100% safe." Copy is always "we did not find warning signs," and we encourage re-scanning.

## Release-build notes

- Code shrinking (`minify`) is currently **off** for clarity; revisit before a Play release and
  keep `proguard-rules.pro` updated if anything is stripped.
- The release build must be signed with a key kept secret and backed up; losing it means you can't
  update the app. (We'll document the signing setup when we get to release — Phase 3.)
