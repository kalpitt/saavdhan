# ADR-0006 — SharedPreferences for the language choice

**Status:** Accepted · **Date:** 2026-06-09

## Context
The app is bilingual (Hindi/English) and must apply the chosen language **before the first screen
is drawn**, inside `Activity.attachBaseContext()`. That callback runs very early and needs a
*synchronous* read of the saved choice. The plan originally mentioned Jetpack DataStore, but
DataStore is asynchronous (Flow/coroutines) and awkward to read synchronously at that point.

## Decision
Store the language choice (`"en"`/`"hi"`) in **SharedPreferences**, read synchronously by
`LocaleManager` in `attachBaseContext()`. We apply it via `createConfigurationContext()` and call
`recreate()` when the user switches languages.

## Consequences
- ✅ Correct, instant locale application before any UI is built; no flash of the wrong language.
- ✅ Simple, dependency-free, well within beginner reach.
- ⚠️ SharedPreferences is older than DataStore, but for a single tiny value it's the right tool.
- ⚠️ We don't use the Android 13 per-app language API (`AppCompatDelegate.setApplicationLocales`)
  to avoid pulling in AppCompat just for this. Revisit if we adopt AppCompat for other reasons.
