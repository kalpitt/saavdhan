---
description: Run the end-of-session ritual on the Saavdhan repo — update the living memory so the next chat (in any tool) starts current. Use before stopping, before context runs low, or when the user says "wrap up" / "write a handoff".
---

# Handoff (end-of-session)

Mirrors the protocol in `context/README.md`, which stays authoritative. Do this BEFORE you run low
on context or stop — even mid-task; a 3-minute handoff beats a lost hour.

1. Overwrite `context/STATE.md` to reflect reality now: current focus, what you just finished, the
   next 1–3 concrete steps, exact resume commands, blockers.
2. Tick `context/PROGRESS.md` — check off what's done, add anything newly discovered.
3. Write a new handoff `context/handoffs/YYYY-MM-DD-short-topic.md` (template in
   `context/handoffs/README.md`). Capture decisions and gotchas, not just a task list.
4. Commit these `context/` files together with your code change — uncommitted state is invisible to
   the next chat. (Direct push to `main` is blocked; the human merges via PR.)
