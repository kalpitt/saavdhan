# How to use the `.claude/` helpers (plain-English guide)

This folder holds small files that make Claude Code work the way *this* project needs — without
you having to remember or re-type the rules every time. You don't have to maintain these; this
page just explains **what they are and when to use them.**

There are two kinds of helper here: **skills** (things *you* run by typing `/name`) and **rules**
(things that work *automatically* in the background). That's the whole system.

---

## 1. Skills — type `/name` to run them

A **skill** is a saved checklist. Instead of you remembering a multi-step routine, you type one
short command and Claude does the whole routine the same way every time. You run these in the
Claude Code chat box — just type the `/` and the name.

| Type this | What it does | When to use it |
|-----------|--------------|----------------|
| `/orient` | Reads the project's "where are we now" notes (STATE, the latest handoff, your profile) so Claude is caught up before touching anything. | **At the start of a session**, or any time you sit down and want Claude up to speed. Good first thing to type in a fresh chat. |
| `/handoff` | Writes the "what we did today" notes so the *next* chat (even in a different tool, on a different day) starts current. Updates STATE, ticks the progress list, saves a dated handoff. | **Before you stop**, or when a chat is getting long and you're worried it'll run out of room. Think: "save the game before quitting." |
| `/release` | Walks through the steps to cut a new app version (version bump → build → sign → publish). | **Only when you're shipping a new release.** This one won't run on its own — you have to ask for it on purpose. |

**Tip:** if you forget the names, just type `/` in the chat and a menu pops up. If a skill
doesn't appear right after it's first added, fully restart Claude Code once — new skills are only
noticed on a fresh start.

A normal day looks like: open Claude → `/orient` → do the work → `/handoff` → close.

---

## 2. Rules — automatic, you don't type anything

A **rule** is a guardrail that switches itself on *only when relevant*. You never call these.
Claude reads them automatically the moment it touches a matching file, and ignores them the rest
of the time (so they don't slow things down or clutter the chat).

| Rule file | What it protects | When it wakes up |
|-----------|------------------|------------------|
| `rules/domain-no-android.md` | Keeps the app's "brain" (the `domain/` folder) free of phone-specific code, so it stays easy to test. | Automatically, whenever Claude edits anything in `domain/`. |
| `rules/bilingual-strings.md` | Makes sure every piece of on-screen text exists in **both** English and Hindi — never one without the other. | Automatically, whenever Claude edits a `strings.xml` file. |

You don't have to do anything for these — they just quietly keep Claude inside the lines. (For
the Hindi/English one, there's also a stronger automatic check that runs on GitHub and fails the
build if a translation is ever missing, so nothing slips through.)

---

## 3. The bigger picture (one paragraph)

`AGENTS.md` at the repo root is still the **master rulebook** — it's written so *any* AI tool can
read it. The files in this `.claude/` folder are **shortcuts for Claude Code specifically**: the
skills save you typing, and the rules enforce the master rulebook automatically. If the two ever
seem to disagree, `AGENTS.md` wins — tell Claude and it'll fix the mismatch.

**What you (the human) still do yourself:** merge changes into `main`, tap through real phone
screens, and anything needing your Google/GitHub account. Claude will always call those out.

---

## 4. Growing this setup (future-proofing)

We deliberately use only **two** of Claude Code's seven steering tools (skills + rules). The other
five aren't a to-do list — they're tools to **reach for when a specific thing happens.** Building
them before you need them is its own mistake (most of that machinery is built for big teams, not a
solo project). Three principles keep this from rotting as the app grows:

1. **Load only when relevant.** Anything *always*-loaded (AGENTS.md, an un-scoped rule) costs you
   on every session. On-demand things (skills, path-scoped rules) cost almost nothing. Prefer them.
2. **Machinery beats prose for hard rules.** A written rule is a *hope* — the AI can slip in a long
   or pressured session. A check that runs automatically (our CI) *can't* slip. Keep the hardest
   guarantees (like "never touch the internet") as machinery, not prose.
3. **Grow by moving things OUT, not piling them ON.** When AGENTS.md gets big, move a procedure
   into a skill or a constraint into a rule — don't keep appending. It's an index, not a warehouse.

### "Reach for it when…" — the trigger table

| When you notice this… | Reach for… | Note |
|---|---|---|
| AGENTS.md creeps past **~200 lines** | move a procedure → a **skill**, or a constraint → a **path-scoped rule** | It's ~199 now. The next addition should push something out, not grow it. |
| You catch the AI *almost* adding a network call, **or a contributor / 2nd machine joins** | the **offline-INTERNET hook** we chose to skip | We skipped it because CI already guards this and you're solo. A near-miss or a second person changes that. |
| A routine **floods the chat** with logs or audit output | a **subagent** (does the work in its own space, hands back only a summary) | The day a task buries your real conversation, that's the signal. |
| You re-type the same **personal preference** in *every* project | **user-level** config in `~/.claude/` (outside this repo) | The home for "how *Kalpit* likes things" vs. "how *Saavdhan* works." |
| The app grows a **second module** | more **path-scoped rules** | They scale better than scattering mini-rulebooks into subfolders. |

### "Compaction" in one breath
When a chat runs long, Claude Code **summarizes the older part to make room** — that's compaction.
AGENTS.md gets re-read afterward (good — and why keeping it lean pays off). Skills can quietly drop
out of a *very* long session, so if a marathon chat feels lost, **just re-run `/orient`.** It's
cheap on purpose.

### Two things to never do
- **Never add an "output style."** It silently *replaces* Claude's built-in safety, testing, and
  scope defaults — the opposite of the "no vibe-coded fragility" bar we hold.
- **Never strip the `paths:` line** off the rules in `rules/`. Without it a rule becomes
  always-loaded dead weight instead of loading only when relevant.
