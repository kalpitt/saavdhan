# ADR-0002 — Detective + guide, not enforcer

**Status:** Accepted · **Date:** 2026-06-09

## Context
Users naturally want a "fix it for me" button. But Android deliberately blocks one app from
turning off another app's Accessibility or Device Admin, force-stopping it, silently uninstalling
it, or toggling airplane mode — without system or root privileges. Pretending to "auto-fix" would
either fail silently or mislead a vulnerable user.

## Decision
Saavdhan **detects and guides**; it never claims to flip another app's switches. For every
problem it: explains the danger in plain language, then **deep-links the user to the exact system
screen** and coaches the final tap. The UI is explicit that only the user can make the change.

## Consequences
- ✅ Honest UX that won't break or mislead across OEMs and Android versions.
- ✅ App Info and Uninstall deep links *are* exact and per-app, so it still feels fast (one tap).
- ⚠️ Accessibility/Device-Admin deep links can only open a *list*, not the exact row — mitigated by
  the [overlay coach](0008-overlay-coach.md).
- ⚠️ Some users may still wish for full automation; we set expectations clearly instead.

See the full capability map in [OS Constraints](../04-os-constraints.md).
