---
name: play-tape
description: Play a tape file by loading its patterns, effects, and arrangement from the tapes/ directory.
---

# Play Tape

This skill plays a tape — loading its patterns, effects, and routing in the correct order.

## When to use

When the user says `play @tapes/<name>.scd` or asks you to play a tape.

## Process

### Step 1: Read the tape files

1. Read the tape's `.scd` file to understand all the Pdef/Ndef definitions, effects, and sidechains.
2. Check for an arrangement file at `tapes/<name>-arrangement.scd`.

### Step 2: Ask the user how they want to play

Always ask:

- **Jam** — Play all parts at once (add `.play` to each Pdef/Ndef)
- **Arrangement** — Play using the CCArrangement (only available if an arrangement file exists)

If no arrangement file exists, offer to create one with `/arrange-tape` as an alternative.

### Step 3: Clear and set tempo

```supercollider
~cc.clear;
~cc.tempo(<bpm>);
```

### Step 4: Load patterns WITHOUT playing them

**CRITICAL: Do NOT call `.play` on any Pdef or Ndef yet.** Load all pattern definitions first.

Execute all `Pdef(\name, Pbind(...))` and `Ndef(\name, {...})` definitions exactly as written in the tape — they define the patterns but do not start them.

### Step 5: Load effects, routing, and sidechains

Execute all `~cc.fx.load(...)`, `~cc.fx.set(...)`, `~cc.fx.route(...)`, `~cc.fx.connect(...)`, `~cc.fx.sidechain(...)`, and `~cc.fx.routeTrigger(...)` calls from the tape.

### Step 6: Play

**If the user chose Jam:**
Play all Pdefs and Ndefs at once by calling `.play` on each one.

**If the user chose Arrangement:**
Execute the `CCArrangement([...]).play` code from the arrangement file.

## Key rules

- **Always ask jam vs arrangement before playing.**
- **Never add `.play` to patterns when playing an arrangement** — the arrangement handles start/stop timing.
- Execute the tape code in chunks: tempo, then patterns, then effects, then play.
- Keep the SC code faithful to the tape file — don't rewrite or modify patterns.
