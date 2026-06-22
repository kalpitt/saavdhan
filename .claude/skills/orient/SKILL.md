---
description: Get oriented on the Saavdhan repo — read the living project state before doing any work. Use at the start of a session, when resuming, or when the user asks "where are we" / "what's next".
---

# Orient (start-of-session)

Mirrors AGENTS.md §0, which stays authoritative (other AI tools follow that prose). Do this in order:

1. Read `context/STATE.md` — where the project is right now and the next concrete steps.
2. **Drift check:** compare STATE.md's claims against `git log --oneline -10` on main. If they
   disagree (e.g. a "pending" branch is already merged), fix STATE.md before starting work.
3. Read the newest file in `context/handoffs/` — the story of the last session.
4. Read `context/PROFILE.md` — who the human is and how to work with them.

Then confirm the plan in one line and start.
