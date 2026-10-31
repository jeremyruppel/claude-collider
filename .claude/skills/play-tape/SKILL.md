---
name: play-tape
description: Play a tape file by loading its patterns, effects, and arrangement from the tapes/ directory.
---

# Play Tape

This skill plays a tape — loading its patterns, effects, and routing in the correct order, then playing it as a jam or an arrangement.

## When to use

When the user says `play @tapes/<name>.scd` or asks you to play a tape.

## What's in a tape

A tape is a single file, `tapes/<name>.scd`:

1. **Header comment** (`/* ... */`) — Markdown docs: title, tempo, key, elements, effects, arrangement overview, notes.
2. **Setup** — `~cc.clear` and `~cc.tempo(...)`.
3. **Definitions** — Pdefs, Ndefs, motifs, samples. Defined only; nothing calls `.play`.
4. **Effects, routing, sidechains.**
5. **Arrangement** — `~arrangement = CCArrangement([...]);` Assigned, not played.

## Process

### Step 1: Read the tape

Read `tapes/<name>.scd`. The header comment tells you the musical intent; the code is the source of truth.

### Step 2: Ask the user how they want to play

Always ask:

- **Arrangement** — Play the tape's `~arrangement` (only if the file defines one)
- **Jam** — Play all parts at once

If the tape has no `~arrangement`, offer to create one with `/arrange-tape` as an alternative.

### Step 3: Load the tape

Execute the tape's code via `cc_execute` **verbatim**, in chunks (setup, definitions, effects, arrangement). Don't send the header comment — it's documentation only. Executing the file never starts playback.

### Step 4: Play

**Arrangement:**

```supercollider
~arrangement.play;
```

**Jam:** call `.play` on each Pdef and Ndef the tape defines. Pdefs pick up their own `quant` if the tape set one (e.g. `Pdef(\fill).quant = 32`); otherwise use `quant: 4`:

```supercollider
Pdef(\kick).play(quant: 4);
Pdef(\fill).play;  // tape set its quant to 32
Ndef(\drone).play;
```

### Stopping

- Arrangement: `~arrangement.stop`
- Jam: `~cc.stop`

### Optional: MIDI clock sync

If the user wants to sync external gear, enable MIDI clock before playing:

```supercollider
~cc.midi.connect("My Device", \out);
CCMIDIClock.enable(~cc.midi.output);
```

CCArrangement automatically sends MIDI clock start/stop/ticks when enabled. No extra steps needed during playback.

## Key rules

- **Always ask jam vs arrangement before playing.**
- **Never `.play` individual patterns when playing an arrangement** — the arrangement handles start/stop timing.
- Keep the SC code faithful to the tape file — don't rewrite or modify patterns.
