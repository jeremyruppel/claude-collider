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

1. **Drums need `\freq, 48`** — without it, drums sound wrong
2. **Use Pdef for rhythms** — patterns that repeat
3. **Use Ndef for continuous** — pads, drones, textures
4. **Symbols not strings** — `\kick` not `"kick"`
5. **Semicolons between statements** — no trailing semicolon
6. **NEVER Synth() inside Ndef** — causes infinite spawning

## Reference Docs

- [ClaudeCollider README](../../../ClaudeCollider/README.md) — Full ~cc API reference with usage examples
- [synths.md](synths.md) — All prebuilt synths with type, description, and key params
