---
name: arrange-tape
description: Compose an arrangement for a tape — plan which elements enter and exit over time, then generate a playable CCArrangement.
---

# Arrange Tape

This skill composes an **arrangement** for a tape — the performance plan for how elements are introduced, removed, and layered over time.

## What is an arrangement?

A tape's `.scd` defines all the musical elements (Pdefs, Ndefs, effects, sidechains). An arrangement defines *when* each element plays and stops, turning a static loop into a track with structure.

Arrangements use `CCArrangement` — a declarative class where you list sections as `[name, bars, elements]` arrays. The class handles all Pdef/Ndef start/stop diffing and uses `TempoClock.schedAbs` for drift-free timing.

## Process

### Step 1: Understand the tape

Read the tape's `.md` and `.scd` files to understand:
- What elements exist (all Pdef/Ndef names)
- The tempo (needed to calculate total duration)
- The vibe/genre (informs pacing and conventions)
- Effects and sidechains (may need to be activated/deactivated per section)

### Step 2: Plan the arrangement with the user

Propose a section-by-section plan. Use standard arrangement conventions for the genre. Common structures:

**Dance/electronic (~2-4 min):**
- Intro → Build → Drop → Break → Drop 2 → Outro

**Ambient/textural:**
- Emerge → Develop → Peak → Dissolve

**Hip-hop/R&B:**
- Intro → Verse → Hook → Verse 2 → Hook → Outro

For each section, specify:
- **Name** and **length** in bars
- Which elements are **active** during that section
- Any **parameter changes** (effect tweaks, filter sweeps, etc.)

Present the plan as a table or outline and get user approval before writing code.

### Step 3: Write the arrangement

**IMPORTANT:** The arrangement does NOT go into the tape's `.scd` file. Tape `.scd` files define elements only — no arrangement logic. Instead, do one of:

**Option A: Live performance** — Execute via `cc_execute` during a session.

**Option B: Separate file** — Write to `tapes/<name>-arrangement.scd` if the user wants to save it.

The arrangement code follows this pattern:

```supercollider
// First, execute the tape's .scd to define all elements
// (but remove .play from each Pdef/Ndef — arrangement controls when they start)

// Then execute the arrangement:
CCArrangement([
    [\intro, 8, [\kick, \hat]],
    [\build, 8, [\kick, \hat, \bass, \rim]],
    [\drop, 16, [\kick, \hat, \bass, \rim, \lead, \pad]],
    [\break, 8, [\hat, \pad]],
    [\drop2, 16, [\kick, \hat, \bass, \rim, \lead, \pad]],
    [\outro, 8, [\kick, \hat]],
]).play;
```

Each section is `[name, bars, elements]`:
- **name** — a symbol identifying the section (posted to console automatically)
- **bars** — how many bars this section lasts
- **elements** — which Pdefs/Ndefs should be playing during this section

The class automatically diffs element lists between sections — elements not in the next section are stopped, new elements are started. You never need to manually call `.play` or `.stop` on individual Pdefs.

**Optional action function:** Add a 4th element for custom per-section logic (effect changes, parameter tweaks):

```supercollider
[\drop, 16, [\kick, \hat, \bass, \lead], { ~cc.fx.route(\lead, \delay) }]
```

**Rules:**

- List **all** active elements per section — the class computes what to start/stop
- Build tension by layering elements gradually — don't start everything at once
- Breaks should strip down to 2-3 elements, then rebuild
- Outros should remove elements gradually, with the most fundamental element (usually kick) last
- The final section's elements are automatically stopped when the arrangement ends

### Step 4: Update the tape's `.md`

Add or update the `## Arrangement` section in the tape's `.md` to document what was composed:

```markdown
## Arrangement (~<duration>)

1. **<Section>** (<bars> bars) - <description of active elements>
2. **<Section>** (<bars> bars) - <description of active elements>
...
```

Include approximate total duration calculated from tempo and total bars:
- duration_seconds = total_bars * beats_per_bar * (60 / tempo)

## Interaction with tape playback

When playing a tape that has an arrangement:
1. Execute the tape's `.scd` blocks to define all elements, but **don't play them** — either remove `.play` calls or stop everything after defining
2. Execute the arrangement to perform the piece

When playing a tape without an arrangement:
- Execute blocks in order as normal (elements start immediately)

## Tips for good arrangements

- **Genre conventions matter** — tech house builds slowly over 8-16 bar sections; DnB drops hit harder with shorter builds
- **Tension and release** — alternate between adding and removing elements
- **Frequency balance** — don't introduce all low-end at once; don't strip all high-end at once
- **Effects as arrangement tools** — bypass/enable effects between sections for contrast
- **The break is the arrangement** — the most important moment is what you take away, not what you add
- **Odd numbers feel natural** — 12-bar drops and 6-bar breaks can feel more interesting than perfect powers of 2
