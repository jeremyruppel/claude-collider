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

Read `tapes/<name>.scd` — the header comment and the code — to understand:
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

The arrangement lives at the end of the tape's `.scd` as the last numbered block. Assign it to `~arrangement` — **don't call `.play`**. If the tape already has an `~arrangement` block, replace it.

```supercollider
// N. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 8, [\kick, \hat]],
    [\build, 8, [\kick, \hat, \bass, \rim]],
    [\drop, 16, [\kick, \hat, \bass, \rim, \lead, \pad]],
    [\break, 8, [\hat, \pad]],
    [\drop2, 16, [\kick, \hat, \bass, \rim, \lead, \pad]],
    [\outro, 8, [\kick, \hat]],
]);
```

Helper code the arrangement needs (e.g. functions that hot-swap Pdef sources in action functions) goes just before `~arrangement`, in the same block.

To try it live before saving, execute the block via `cc_execute` and then `~arrangement.play`.

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

### Step 4: Update the tape's header comment

Add or update the `## Arrangement` section in the tape's `/* ... */` header (before `## Notes`) to document what was composed:

```markdown
## Arrangement (<total bars> bars, ~<duration>)

1. **<Section>** (<bars> bars) - <description of active elements>
2. **<Section>** (<bars> bars) - <description of active elements>
...
```

Include approximate total duration calculated from tempo and total bars:
- duration_seconds = total_bars * beats_per_bar * (60 / tempo)

## Interaction with tape playback

Executing a tape never starts playback — it defines elements, effects, and `~arrangement`. `/play-tape` then either calls `~arrangement.play` or plays every element at once (jam). Stop with `~arrangement.stop`.

## MIDI clock sync

If the user wants to sync external gear, enable MIDI clock before playing the arrangement. CCArrangement automatically sends MIDI clock start/stop when `CCMIDIClock` is enabled:

```supercollider
~cc.midi.connect("My Device", \out);
CCMIDIClock.enable(~cc.midi.output);
```

Once enabled, any `CCArrangement.play` sends MIDI Start + 24 ppqn clock ticks, and stop/finish sends MIDI Stop. No changes to the arrangement code are needed.

## Tips for good arrangements

- **Genre conventions matter** — tech house builds slowly over 8-16 bar sections; DnB drops hit harder with shorter builds
- **Tension and release** — alternate between adding and removing elements
- **Frequency balance** — don't introduce all low-end at once; don't strip all high-end at once
- **Effects as arrangement tools** — bypass/enable effects between sections for contrast
- **The break is the arrangement** — the most important moment is what you take away, not what you add
- **Odd numbers feel natural** — 12-bar drops and 6-bar breaks can feel more interesting than perfect powers of 2
