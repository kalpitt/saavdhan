# 01 — Vision & Scope

## The problem

In India, scam APKs spread over WhatsApp disguised as wedding invitations, courier/KYC/electricity-bill
notices, and similar lures. Tapping them sideloads Android banking spyware from the **SpyNote /
SpyMax** family. Once installed, the malware:

- abuses **Accessibility Services** to read the screen, log keystrokes, and auto-tap "Allow" on
  other permission prompts;
- intercepts **SMS** to steal bank OTPs / 2FA codes;
- registers as a **Device Admin** and runs "diehard" background services to **resist uninstall**;
- **hides its icon** from the home screen and Recents;
- **impersonates** trusted apps ("System Update", "Google Play Services", a bank, a courier).

(Every one of those behaviours is documented threat intelligence — see
[Detection Rules → sources](03-detection-rules.md#sources).)

For a non-technical victim, cleaning an infected phone today means 30–40 minutes of panicked
digging through Settings under pressure, often with a scammer on the phone.

## Who it's for

The person *holding the infected phone* — typically an older or non-technical family member, in a
panic. **Not** a security analyst. Every design choice optimises for: calm, simple language
(Hindi or English), large tap targets, one clear next step.

## What Saavdhan does

A **detective + guide**: it inspects what's installed, flags apps showing the scam fingerprint,
explains the danger in plain language, and takes the user *one tap* to the exact system screen
where they can fix it. It is honest that Android only lets the **user** make the final change.

## What Saavdhan is NOT

- Not an antivirus with cloud scanning (it's **fully offline** — [ADR-0001](decisions/0001-fully-offline-no-internet.md)).
- Not an enforcer — it cannot and will not silently disable or delete other apps
  ([ADR-0002](decisions/0002-detective-not-enforcer.md)).
- Not a tracker — it never collects, sends, or sells any data.

## Scope by phase

### Phase 1 — Scanner core *(current)*
Detect → explain (bilingual) → one-tap deep links. **Built and tested.** Remaining Phase-1
additions: the floating overlay coach and background new-app alerts.

### Phase 2 — Guided cleanup *(next)*
A reactive checklist that responds to the phone's live state: isolate the phone → revoke
permissions → uninstall → escalate to **Safe Mode** when the malware resists → factory reset as a
last resort. Plus post-cleanup account-security guidance (change passwords from another device,
watch for unauthorized UPI mandates).

### Phase 3 — Hardening & reach *(later)*
Wider device/OEM testing, accessibility audit, Play Store compliance submission, and a trusted
distribution story. See the [Roadmap](10-roadmap.md).

## Success criteria

A scared, non-technical person can open Saavdhan, understand what's wrong in under a minute, and
reach the right fix screen in one tap — in their own language, without ever going online.
