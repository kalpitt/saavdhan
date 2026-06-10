# ADR-0008 — Floating overlay coach via SYSTEM_ALERT_WINDOW

**Status:** Accepted · **Date:** 2026-06-09

## Context
Because Android only lets the *user* flip another app's Accessibility/Device-Admin switches
([ADR-0002](0002-detective-not-enforcer.md)), we deep-link to the right Settings *list* but can't
highlight the exact row. A panicking, non-technical user can easily get lost on an unfamiliar
system screen and tap the wrong thing.

## Decision
Provide a **floating overlay coach**: a small bubble, drawn with the `SYSTEM_ALERT_WINDOW`
("draw over other apps") permission, that shows the next step on top of the real Settings screen
(e.g. "Find **<app name>** in this list and turn it OFF"), with Next/Dismiss.

## Consequences
- ✅ Dramatically easier to follow under stress — the help stays visible *on* the screen that needs
  action, where in-app instructions can't reach.
- ⚠️ Requires the overlay permission, which we request only when first needed and explain plainly.
- ⚠️ Overlay APIs need care (lifecycle, not obscuring system prompts). Keep the bubble small and
  non-blocking.
- Alternative considered: in-app screenshots + numbered steps only. Simpler, but far weaker for the
  exact moment of confusion. Rejected as the default; may remain a fallback when overlay is denied.

*Status note:* **Built & verified.** `OverlayCoachService` (draws the banner) + `OverlayCoach`
(permission + control), wired into the accessibility/device-admin actions on the detail screen,
with an opt-in "Enable the on-screen helper" button shown only when the permission isn't granted.
Live-tested on an emulator: tapping "Turn off its Accessibility" opens the system Accessibility
list AND creates the overlay window (`dumpsys window` confirms a `TYPE_APPLICATION_OVERLAY`,
1080×198, surfaced, visible window owned by us). The banner doesn't composite into a *headless*
software-GPU screencap (`isReadyForDisplay=false`) — an emulator-capture limitation, not a code
bug; it renders on a real device / windowed emulator.
