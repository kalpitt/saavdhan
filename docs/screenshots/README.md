# Screenshots — captured on the emulator

These were captured running the real debug app on an Android 15 (API 35) emulator, end to end.
They are the visual proof that the [testing checklist](../07-testing-strategy.md) passes, and they
feed the [landing page](../index.html).

| File | Screen | What it proves |
|---|---|---|
| `01-language-picker.png` | First-launch language picker | Bilingual onboarding; "Saavdhan" branding; English + हिन्दी |
| `02-home.png` | Home | Clean launch skips onboarding (language persisted); Scan button; "works fully offline" note; Settings gear |
| `03-results.png` | Scan results (English) | Scan works; risk-tinted cards, scariest first — **System Update → Very dangerous (CRITICAL)**, **Fast Cash Loan → Dangerous (HIGH)** |
| `04-detail-critical.png` | App detail (top) | Visual hierarchy: severity banner + **"What to do now"** hero card; plain-language "what this app could do"; "why this looks risky" signals |
| `05-detail-actions.png` | App detail (actions) | Honesty note; **numbered do-it-yourself steps** (Accessibility → Device-Admin → red Uninstall); App Info deep link last |
| `06-guided-cleanup.png` | Guided cleanup | Reactive checklist with a **"Step 1 of 2"** counter; isolate → uninstall; factory reset as the last resort |
| `07-settings.png` | Settings | Language radios; background-protection (battery) card; "fully offline" reassurance |
| `08-hindi-results.png` | Scan results (Hindi) | Full bilingual switch: **सावधान**, **बहुत खतरनाक**, **खतरनाक** in Devanagari |
| `09-watchdog-notification.png` | Notification shade | **Background watchdog works**: a newly-installed dangerous app raises *"A new app looks dangerous"* |

Also verified (not pictured): the app icon renders on the launcher, and the notification
permission prompt correctly appears **after** language selection, not before.

> Regenerate with: build the debug APK, boot an API-35 emulator, then
> `adb install -r`, `adb shell am start`, and `adb exec-out screencap -p > file.png`.
> See [Build & Run](../08-build-and-run.md).
