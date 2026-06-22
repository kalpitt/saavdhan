@AGENTS.md

# CLAUDE.md

The line above (`@AGENTS.md`) **imports** our cross-tool instructions so Claude Code loads them
automatically at the start of every session. [`AGENTS.md`](AGENTS.md) is the single source of truth
that every AI tool reads. (Claude Code reads `CLAUDE.md`, not `AGENTS.md` — without this import it
would only see this pointer, not the actual rules.)

After the import loads, follow AGENTS.md's "30-second orientation" — or just run `/orient`, the
Claude Code skill that performs it (reads `context/STATE.md`, the newest handoff, and
`context/PROFILE.md`). See AGENTS.md §8 for the full `.claude/` steering layer.

Keep the real guidance in AGENTS.md, not here, so the two can never drift apart.
