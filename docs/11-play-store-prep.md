# 11 — Play Store preparation

Everything needed to publish Saavdhan on Google Play, prepared in advance. The decision to
publish is Kalpit's; this doc removes all the prep work from that decision.

**Why Play matters for this app:** the target users (non-technical family members) can't or
shouldn't sideload — and today, installing an *anti-scam* app requires enabling the very
"install unknown apps" habit scammers exploit. Play removes that irony and adds automatic
updates, which matters because detection rules only ship via app updates.

---

## 1. One-time prerequisites (human steps)

- [ ] Create a Google Play developer account (one-time **US$25**) at play.google.com/console.
      Use a personal account; the legal name appears on the listing.
- [ ] Identity verification (Google requires ID + can take a few days).
- [ ] **Closed testing requirement (new personal accounts):** before production access, the app
      must run a closed test with **at least 12 testers opted in for 14 continuous days**.
      Family/friends testing the APK can be those testers — invite them by email in Play Console.
- [ ] Privacy policy URL (required, ready): **https://kalpitt.github.io/saavdhan/privacy.html**

## 2. Policy declarations the app will need

- **QUERY_ALL_PACKAGES** is a sensitive permission. Play requires a *Permissions Declaration
  Form* at submission. Saavdhan's justification (truthful and category-appropriate):
  > Saavdhan is a device-security app. Its core, user-facing purpose is scanning installed
  > apps for the behavioural warning signs of banking-trojan spyware (Accessibility abuse,
  > Device-Admin abuse, SMS access, hidden icons, impersonation). This scan is impossible
  > without visibility of installed packages. Analysis is fully on-device; the app holds no
  > INTERNET permission, so the package list cannot leave the device.
  Device-security apps are an explicitly permitted use case for this permission.
- **SYSTEM_ALERT_WINDOW** (overlay coach) — no declaration form, but reviewers may ask; the
  in-app flow only requests it optionally, with a plain-language explanation. Fine as is.
- **Anti-stalkerware / security-app policies** — Saavdhan complies: it's defensive only, makes
  no removal claims it can't keep, and surveils nothing (no data leaves the device).

## 3. Data safety form — exact answers

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | N/A (no data collected or transmitted; app has no INTERNET permission) |
| Do you provide a way for users to request that their data is deleted? | N/A (no data collected; local data is deleted on uninstall) |

Result: the listing shows the "No data collected" badge — a major trust signal for this app.
If the form asks about specific types (location, personal info, app activity, etc.): **none
are collected**. The on-device scan snapshot and language choice never leave the phone and are
not "collection" under Play's definition (data must leave the device to count).

## 4. Content rating questionnaire (IARC)

- Category: Utility / Productivity / Other.
- No violence, sexuality, profanity, gambling, drugs, or user-generated content.
- No data sharing; no in-app purchases; no ads.
- Expected rating: **Everyone / 3+**.

## 5. Store listing copy

### App details
- **App name (30 chars max):** `Saavdhan — Anti-Scam Scanner` (28 chars)
  - Hindi listing: `सावधान — स्कैम से सुरक्षा`
- **Category:** Tools
- **Tags:** security, antivirus alternative, spyware
- **Contact email:** tiwari.kalpit@gmail.com
- **Website:** https://kalpitt.github.io/saavdhan/

### Short description (80 chars max)

- **EN** (74): `Finds scam & spyware apps and guides you to remove them — fully offline.`
- **HI**: `स्कैम और जासूसी ऐप्स ढूँढता है और हटाने में आपकी मदद करता है — पूरी तरह ऑफ़लाइन।`

### Full description — English

```
Scam APKs spread on WhatsApp disguised as wedding invitations, courier updates, KYC notices,
or electricity bills. One install plants banking spyware that reads OTPs and drains accounts —
and resists being removed.

Saavdhan (सावधान, "be alert") finds these apps, explains the danger in plain Hindi or English,
and walks you through removing them — one calm step at a time.

HOW IT WORKS
• Scan — checks every installed app for the warning signs of scam spyware: Accessibility abuse,
  Device-Admin abuse, SMS access, sideloading, hidden icons, fake system names.
• Explain — every verdict lists its reasons in plain words. No black-box AI.
• Guide — a step-by-step cleanup checklist (isolate the phone → strip the app's powers →
  uninstall → secure your accounts) that ticks itself off as you complete each step.
• Watch — a background watchdog warns you when a new or changed app looks dangerous.

PROMISES THAT NEVER CHANGE
• Fully offline. The app does not hold the INTERNET permission, so the operating system makes
  any network connection impossible. Nothing about you or your phone is ever collected or sent.
• No ads, no account, no tracking. Free and open source (MIT license).
• Honest. Android only lets YOU turn off another app's powers — Saavdhan takes you straight to
  the right screen and coaches the final tap. It never fakes an "auto-fix".

HONEST DISCLAIMER
Saavdhan is a defensive aid, not a guarantee. It uses behavioural warning signs (not a malware
database), so it can raise false alarms and can miss brand-new threats. If money has already
been stolen, contact your bank and cyber-crime authorities (in India: dial 1930 or visit
cybercrime.gov.in).

Designed for the family members everyone worries about — big buttons, simple language, full
support for Hindi and English.
```

### Full description — Hindi

```
WhatsApp पर शादी के कार्ड, कूरियर अपडेट, KYC नोटिस या बिजली बिल के बहाने स्कैम APK फैलाए जाते
हैं। एक बार install होने पर ये ऐसा बैंकिंग स्पाईवेयर डाल देते हैं जो आपके OTP पढ़ लेता है और
बैंक खाता खाली कर देता है — और इसे हटाना भी मुश्किल होता है।

सावधान ऐसे ऐप्स को ढूँढता है, खतरे को आसान हिन्दी या English में समझाता है, और बिना घबराए
एक-एक कदम पर उन्हें हटाने में आपकी मदद करता है।

यह कैसे काम करता है
• स्कैन — हर installed ऐप में स्कैम स्पाईवेयर के चेतावनी संकेत जाँचता है: Accessibility का
  दुरुपयोग, Device-Admin का दुरुपयोग, SMS access, बाहर से इंस्टॉल, छुपे आइकॉन, नकली सिस्टम नाम।
• समझाना — हर फ़ैसले के कारण आसान भाषा में बताए जाते हैं। कोई छुपा हुआ AI नहीं।
• गाइड — कदम-दर-कदम सफ़ाई चेकलिस्ट (फ़ोन को अलग करें → ऐप की शक्तियाँ हटाएँ → uninstall करें →
  खाते सुरक्षित करें) जो हर कदम पूरा होने पर खुद टिक हो जाती है।
• निगरानी — बैकग्राउंड वॉचडॉग नए या बदले हुए खतरनाक ऐप पर तुरंत आगाह करता है।

हमारे वादे जो कभी नहीं बदलेंगे
• पूरी तरह ऑफ़लाइन। ऐप के पास INTERNET परमिशन ही नहीं है, इसलिए सिस्टम इसके लिए इंटरनेट से जुड़ना
  नामुमकिन कर देता है। आपकी कोई भी जानकारी कभी इकट्ठा या बाहर नहीं भेजी जाती।
• कोई विज्ञापन नहीं, कोई अकाउंट नहीं, कोई ट्रैकिंग नहीं। मुफ़्त और ओपन सोर्स (MIT लाइसेंस)।
• ईमानदार। Android में सिर्फ़ आप ही किसी दूसरे ऐप की परमिशन बंद कर सकते हैं — सावधान आपको सीधे
  सही स्क्रीन पर ले जाता है और आखिरी टैप में मदद करता है। यह कभी झूठा "ऑटो-फिक्स" नहीं करता।

एक ज़रूरी सच्ची बात
सावधान एक मदद है, गारंटी नहीं। यह ऐप्स के व्यवहार को देखकर काम करता है (किसी मैलवेयर डेटाबेस से
नहीं), इसलिए कभी सुरक्षित ऐप पर भी अलार्म दे सकता है या बिल्कुल नए खतरे चूक सकता है। अगर पैसे
पहले ही चोरी हो चुके हैं, तो तुरंत अपने बैंक और साइबर क्राइम हेल्पलाइन से संपर्क करें (भारत में:
1930 डायल करें या cybercrime.gov.in पर जाएँ)।

यह ऐप उन्हीं घरवालों के लिए बनाया गया है जिनकी चिंता हम सबको रहती है — बड़े बटन, आसान भाषा,
और पूरी हिन्दी + English सपोर्ट।
```

## 6. Graphic assets still needed (human/design step)

- [ ] App icon 512×512 PNG (export from the launcher icon source).
- [ ] Feature graphic 1024×500 (simple: teal background, shield, app name EN+HI).
- [ ] At least 2 phone screenshots per language (take from the emulator: home, results,
      detail with hero card, cleanup checklist — `docs/screenshots/` already has some).

## 7. Release artifact

Play requires an **AAB** (Android App Bundle), not an APK: `./gradlew bundleRelease` with the
keystore present produces `app/build/outputs/bundle/release/`. Same signing key as the APK
releases. GitHub Releases (APK) and Play (AAB) can ship the same version side by side.
**Note:** enrolling in Play App Signing is required for new apps — Google holds the production
signing key and our keystore becomes the *upload* key. GitHub APK releases keep using our key
directly; the two install bases won't cross-update (different signers). Decide once at first
upload; document the outcome here.
