---
paths:
  - "app/src/main/res/values*/strings.xml"
---

# Bilingual strings: English and Hindi must mirror

- Every user-facing string lives in **both** `app/src/main/res/values/strings.xml` (English) and
  `app/src/main/res/values-hi/strings.xml` (Hindi). Add the two together; never hard-code
  user-facing text in code.
- The two files must contain the **same `name=` keys**. A mismatch is caught mechanically by
  `scripts/drift_check.sh` (Gate 2), which runs in CI and locally — so a missing translation
  fails the build, not just review.
