---
name: record-tape
description: Record the current live session as a tape (md + scd file pair) in the tapes/ directory.
---

# Record Tape

This skill captures the current live coding session as a **tape** — a replayable pair of files that preserve the musical idea.

## What is a tape?

A tape is two files in `tapes/`:

- **`<name>.scd`** — Executable SuperCollider code. The source of truth.
- **`<name>.md`** — Documentation for Claude. Describes the musical content so future sessions can understand, modify, or remix the tape without parsing SC code.

## Recording process

### Step 1: Gather session state

Use the MCP tools to capture what's currently playing:

1. `cc_status` (action: `status`) — Get tempo, playing Pdefs/Ndefs, active synths
2. `cc_status` (action: `routing`) — Get effects routing, sidechain configuration
3. `cc_execute` to inspect individual Pdefs/Ndefs if needed (e.g., `Pdef(\kick).source.postcs`)

Ask the user for:
- **Name** for the tape (used as filename, kebab-case)
- **Genre/vibe** description (one sentence)
- **Key** (e.g., `Dm`, `Ebm`, or `atonal`)
- **Chord progression** if tonal (chords + cycle length)
- Any notes about the session they want preserved

### Step 2: Write the `.scd` file

Write `tapes/<name>.scd` following this structure:

```supercollider
// <Title> - <one-line description> at <BPM> BPM in <key>
// <Chord progression if applicable>

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(<bpm>);

// 2. <Element name> - <musical role>
Pdef(\<name>, Pbind(
    ...
)).play(quant: 4);

// ... more elements ...

// N. Effects
~cc.fx.load(\<effect>);
~cc.fx.set(\<slot>, <params>);
~cc.fx.route(\<source>, \<slot>);

// N+1. Sidechain
~cc.fx.sidechain(\<name>, <threshold>, <ratio>, <attack>, <release>);
~cc.fx.route(\<source>, \<name>);
~cc.fx.routeTrigger(\<trigger>, \<name>, true);
```

**Rules for `.scd` files:**

- Always start with `~cc.clear` and `~cc.tempo`
- Number each block with a comment: `// 1.`, `// 2.`, etc.
- Use `play(quant: 4)` on Pdefs for aligned playback
- **Repo samples OK** — samples from the `samples/` directory (committed to the repo) can be loaded and used. System-specific sample paths are still not allowed.
- **NO MIDI setup** — playback-time decision
- **NO hardware output routing** (`routeToOutput`) — system-specific
- **NO server boot/reboot commands** — handled by playback
- **NO absolute file paths**
- Drums always need `\freq, 48`
- Use `\instrument, \cc_<synthname>` — never dynamically construct the prefix
- Prefer `\degree` + `\scale` + `\root` + `\octave` for tonal parts (more readable than raw midinote)
- Group: drums first, then bass, then melodic/harmonic, then effects, then sidechain

### Step 3: Write the `.md` file

Write `tapes/<name>.md` following this structure:

```markdown
---
name: <Human Readable Name>
tempo: <BPM>
key: <key or "atonal">
scd: <name>.scd
---

> **IMPORTANT:** This tape has a companion script at `<name>.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# <Title>

<One sentence: genre, tempo, key, vibe.>

## Chord Progression

<Chords> (<cycle length>)

## Elements

- **<Name>** (<synth>) - <musical role and character>
- ...

## Effects

- **<Effect>** on <targets> (<key params>)
- ...

## Sidechain

- **<Name>** — <params> -> <targets>, trigger: <source>

## Notes

<Anything unusual or worth knowing>

## Playback

Execute each block from `<name>.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
```

**Rules for `.md` files:**

- `key` is always present in frontmatter — use `atonal` if no key center
- `tempo` is always present
- Omit the Chord Progression section if atonal
- Omit the Effects section if there are none (note this in Notes instead)
- Omit the Sidechain section if there is none
- Element descriptions should focus on musical role, not implementation details
- Repo sample names (from `samples/`) can be mentioned in element descriptions. Do NOT include system-specific sample names.
- The synth in parentheses is the default voice used in the .scd, not a requirement

## What NOT to record

These are **playback-time decisions**, not part of the tape:

- **System-specific samples** — samples outside the repo's `samples/` directory
- **MIDI routing** — which devices, channels, CC mappings
- **Audio device / hardware output routing** — system-specific
- **Server boot configuration** — device, num outputs, etc.
- **Arrangement / ordering** — how to introduce elements over time (this is a separate concern)

## Fixing existing tapes

If asked to fix or update an existing tape, apply these same rules. Common fixes:
- Remove system-specific sample references (replace with built-in synths or repo samples)
- Remove system-specific sample names from the `.md` element list (repo `samples/` names are OK)
- Add missing `key` to frontmatter (use `atonal` if appropriate)
- Remove any device/routing setup
