# Security Policy

Saavdhan is a security tool, so we hold it to a high standard.

## Design that limits risk

- The app **has no `INTERNET` permission**, so it cannot exfiltrate data or be remotely controlled.
- It collects no personal data and writes nothing off-device.
- It never silently changes another app; it only guides the user.

## Reporting a vulnerability

If you believe you've found a security issue (in the app, the build, or the detection logic):

- **Please do not open a public issue.** Instead, use GitHub's **private vulnerability reporting**:
  go to the repository's **Security** tab → **Report a vulnerability**.
- Include steps to reproduce, the affected version/commit, and the impact as you understand it.

We'll acknowledge your report, investigate, and credit you (if you wish) once a fix ships. Because
detection rules ship inside the app, fixes are delivered as new app releases.

## Scope notes

- **False positives / missed detections** are expected limitations of heuristic detection, not
  security vulnerabilities — but reports that help us tune the rules are very welcome as normal
  issues or PRs (with a test).
- Saavdhan only operates on the local device for the local user. It has no capability to act on
  another person or device.
