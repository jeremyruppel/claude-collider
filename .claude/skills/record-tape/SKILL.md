---
name: record-tape
description: Record the current live session as a tape (a single .scd file) in the tapes/ directory.
---

# Record Tape

This skill captures the current live coding session as a **tape** — a single replayable file that preserves the musical idea.

## What is a tape?

A tape is one file, `tapes/<name>.scd`:

- **Header comment** — A `/* ... */` block of Markdown docs. Describes the musical content so future sessions can understand, modify, or remix the tape without parsing SC code.
- **Code** — Executable SuperCollider that defines every element, effect, and sidechain. The source of truth. Nothing in it starts playback.
- **Arrangement** (optional) — `~arrangement = CCArrangement([...]);` at the end. Added by `/arrange-tape`.

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

### Step 2: Write the tape

Write `tapes/<name>.scd` following this structure:

```supercollider
/*
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
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(<bpm>);

// 2. <Element name> - <musical role>
Pdef(\<name>, Pbind(
    ...
));

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

**Rules for the header comment:**

- No YAML frontmatter — the `#` heading is the name, and the summary sentence always states the tempo and key (use `atonal` if no key center)
- Never write `*/` inside the comment — it ends the block early
- Omit the Chord Progression section if atonal
- Omit the Effects section if there are none (note this in Notes instead)
- Omit the Sidechain section if there is none
- Element descriptions should focus on musical role, not implementation details
- Repo sample names (from `samples/`) can be mentioned in element descriptions. Do NOT include system-specific sample names.
- The synth in parentheses is the default voice used in the code, not a requirement

**Rules for the code:**

- Always start with `~cc.clear` and `~cc.tempo`
- Number each block with a comment: `// 1.`, `// 2.`, etc.
- **Never call `.play`** — define patterns only. Playback (jam or arrangement) is chosen by `/play-tape`. If a pattern needs a quant other than 4, set it after the definition: `Pdef(\fill).quant = 32;`
- **Repo samples OK** — samples from the `samples/` directory (committed to the repo) can be loaded and used. System-specific sample paths are still not allowed.
- **NO MIDI setup** — playback-time decision
- **NO hardware output routing** (`routeToOutput`) — system-specific
- **NO server boot/reboot commands** — handled by playback
- **NO absolute file paths**
- Drums always need `\freq, 48`
- Use `\instrument, \cc_<synthname>` — never dynamically construct the prefix
- Prefer `\degree` + `\scale` + `\root` + `\octave` for tonal parts (more readable than raw midinote)
- Group: drums first, then bass, then melodic/harmonic, then effects, then sidechain

## What NOT to record

These are **playback-time decisions**, not part of the tape:

- **System-specific samples** — samples outside the repo's `samples/` directory
- **MIDI routing** — which devices, channels, CC mappings
- **Audio device / hardware output routing** — system-specific
- **Server boot configuration** — device, num outputs, etc.
- **Arrangement / ordering** — how to introduce elements over time (use `/arrange-tape` for that)

## Fixing existing tapes

If asked to fix or update an existing tape, apply these same rules. Common fixes:
- Remove system-specific sample references (replace with built-in synths or repo samples)
- Remove system-specific sample names from the header's element list (repo `samples/` names are OK)
- Add missing tempo or key to the summary sentence (use `atonal` if appropriate)
- Remove any `.play` calls and device/routing setup
