---
name: claude-collider
description: Use when live coding music with SuperCollider via ClaudeCollider MCP server.
---

# SuperCollider Live Coding with ClaudeCollider

This skill enables live music synthesis using SuperCollider via the ClaudeCollider MCP server.

## Related Skills

- **Always load `/songwriting`** when composing or improving musical parts (bass lines, chords, melodies, drums). It contains music theory principles and genre conventions that produce better-sounding patterns.
- Use `/play-tape` to play a tape file.
- Use `/record-tape` to save a session as a replayable tape.
- Use `/arrange-tape` to compose an arrangement for a tape.

## Quick Reference

1. **Pitch drums with `\tune`** — drums ignore `\freq`, `\degree` and `\midinote`, so they sound right with no pitch keys at all
2. **Use Pdef for rhythms** — patterns that repeat
3. **Use Ndef for continuous** — pads, drones, textures
4. **Symbols not strings** — `\kick` not `"kick"`
5. **Semicolons between statements** — no trailing semicolon
6. **NEVER Synth() inside Ndef** — causes infinite spawning

## Composition Rules

1. **Melodies use CCMotif + CCPhrase** — never a flat pitch Pseq longer than 8 steps. Motifs are 2–6 notes; phrases are 1–4 bars. See [songwriting/melody.md](../songwriting/melody.md).
2. **Songwriting files by part** — bass → `bass.md`, chords/stabs/pads → `chords.md`, melody/hook/lead → `melody.md`, drums → `rhythm.md`, breaks → `breakbeats.md`.
3. **Song structure uses CCArrangement** — see [patterns.md](patterns.md).

## Reference Docs

- [ClaudeCollider README](../../../ClaudeCollider/README.md) — Full ~cc API reference with usage examples
- [synths.md](synths.md) — All prebuilt synths with type, description, and key params
- [songwriting/melody.md](../songwriting/melody.md) — CCMotif, CCPhrase, CCMelody
- [patterns.md](patterns.md) — CCArrangement, CCBreakbeat
