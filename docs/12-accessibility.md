# 12 — Accessibility audit

First automated pass done 2026-06-13 (emulator, API 35). The remaining items need a human with
a real phone — they are listed at the bottom as a checklist for Kalpit.

Why this matters more than usual: Saavdhan's primary users are elderly, non-technical people —
large font scaling, shaky taps, and (sometimes) screen readers are the *normal* case here, not
the edge case.

## What was checked automatically (and the results)

| Check | Result |
|---|---|
| Android lint accessibility rules (missing content descriptions, etc.) | ✅ 0 issues |
| Touch targets | ✅ All buttons 52–56dp tall and full-width; icon buttons are the 48dp Material default; result cards are full-width rows |
| Font scaling at 1.5× | ✅ Verified on emulator: all screens reflow, nothing clips or overlaps (the layouts scroll and avoid fixed heights on text) |
| Dark theme | ✅ Verified on emulator: risk-tinted banners, hero card, and warning icons stay legible |
| Decorative icons | ✅ All decorative icons (warning triangles, check circle) set `contentDescription = null` so TalkBack skips them and reads the adjacent text; functional icons (back, settings) have proper labels |
| Hindi (Devanagari) rendering | ✅ Verified on emulator across home, results, detail, cleanup |
| Colour-only meaning | ✅ Risk is never colour-alone: every level also has a text label (chip) and flagged screens add a warning icon |

## Contrast (WCAG 1.4.3 / 1.4.11) — measured

| Element | Ratio | Verdict |
|---|---|---|
| "Very dangerous" chip — white on `#C62828` | 5.6:1 | ✅ Pass |
| "Looks okay" chip — white on `#2E7D32` | 5.1:1 | ✅ Pass |
| "Worth checking" chip — ~~white~~ on `#F9A825` | ~~1.97:1 ❌~~ → **fixed 2026-06-13**: now dark text (`#212121`), ≈9:1 | ✅ Pass |
| "Dangerous" chip — white on `#EF6C00` | 3.1:1 | ⚠️ Passes only as *large text* (the chip is 18sp semibold, which qualifies). If we ever shrink the chip font, darken the orange instead. |

## Human checklist (needs a real phone + judgment)

- [ ] **TalkBack walkthrough** (Settings → Accessibility → TalkBack): scan → open a flagged app
      → start cleanup. Listen for: does the risk level get announced with the app name? Is the
      "What to do now" card read before the quieter actions? Can every button be reached by
      swiping in a sensible order?
- [ ] **Font scaling at maximum** (Settings → Display size and text → largest): repeat the same
      walk-through; emulator verified 1.5×, but OEM maximums go higher.
- [ ] **One-handed reachability**: can the primary button be tapped with a thumb on a big phone?
- [ ] **Greyscale test** (Developer options → Simulate colour space → Monochromacy): is a
      dangerous row still obviously different from a mild one? (Should be — icon + label + order.)
- [ ] **Real OEM skins** (Samsung OneUI, Xiaomi MIUI/HyperOS): check the deep-link buttons land
      on the right Settings screens and the overlay coach draws correctly.
