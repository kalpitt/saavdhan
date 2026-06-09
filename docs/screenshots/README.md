# Screenshots — Phase 1 verified on the emulator

These were captured running the real debug app on an Android 15 (API 35) emulator, end to end.
They are the visual proof that the [testing checklist](../07-testing-strategy.md) passes.

| File | Screen | What it proves |
|---|---|---|
| `01-language-picker.png` | First-launch language picker | Bilingual onboarding; "Saavdhan" branding; English + हिन्दी |
| `02-home.png` | Home | Clean launch skips onboarding (language persisted); Scan button; Settings gear |
| `03-results.png` | Scan results (English) | Scan works; **System Update → Very dangerous (CRITICAL)**, **Fast Cash Loan → Dangerous (HIGH)**, scariest first |
| `04-detail-critical.png` | App detail (top) | Plain-language "what this app could do"; all six red flags listed; honesty banner |
| `05-detail-actions.png` | App detail (actions) | Overlay-coach opt-in; App Info / Accessibility / Device-Admin deep links with hints; red Uninstall |
| `06-settings.png` | Settings | Language radios; "fully offline" reassurance |
| `07-hindi-results.png` | Scan results (Hindi) | Full bilingual switch: **सावधान**, **बहुत खतरनाक**, **खतरनाक** in Devanagari |
| `08-watchdog-notification.png` | Notification shade | **Background watchdog works**: installing the decoy "System Update" raised *"A new app looks dangerous"* |

Also verified (not pictured): the app icon renders on the launcher, and the notification
permission prompt correctly appears **after** language selection, not before.

> Regenerate with: build the debug APK, boot an API-35 emulator, then
> `adb install -r`, `adb shell am start`, and `adb exec-out screencap -p > file.png`.
> See [Build & Run](../08-build-and-run.md).
