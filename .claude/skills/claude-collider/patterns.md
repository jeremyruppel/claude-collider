# Arrangement & Slicing Helpers

**Melody helpers (CCMotif, CCPhrase, CCMelody) live in [songwriting/melody.md](../songwriting/melody.md)**, next to the theory for using them. Read that file before writing any melody, hook, or lead.

## CCArrangement - Song Arrangement

Declarative section sequencer. Diffs elements between sections — starts new ones, stops removed ones.

| Method | Args | Description |
|---|---|---|
| `*new` | `sections, beatsPerBar=4` | Create from section array |
| `play` | | Start from beginning (stops any current arrangement) |
| `stop` | | Stop all elements |
| `goto` | `name` | Jump to named section |
| `status` | | Current section name and bar position |
| `sectionBar` | | Current bar within section |

Sections are `[name, bars, elements, action]` arrays. `action` is an optional function that runs on every beat of the section as `{ |beat, arrangement| }`, with `beat` counting from 0. Use `if(beat == 0)` for things that should happen once when the section starts. Put a `CCFade` in the action slot (`CCFade.in`, `CCFade.out`, `CCFade(1, 0.5)`) to fade the master volume across the section.

```supercollider
// Define patterns first, then arrange them
CCArrangement([
  [\intro,  4, [\drums]],
  [\verse,  8, [\drums, \bass, \pad]],
  [\chorus, 8, [\drums, \bass, \lead, \pad]],
  [\outro,  4, [\pad]]
]).play
```

Elements can be Pdef or Ndef names. The arrangement auto-starts/stops them at section boundaries, from a single Routine on `TempoClock.default` (drift-free logical time).

`CCArrangement.current` holds the playing arrangement (or nil). Auto-starts/stops CCMIDIClock if enabled.

## CCBreakbeat - Slice Sequencer

See [synths.md](synths.md) for `cc_breakbeat` synth. CCBreakbeat manages slice indexing and creates the Pdef.

| Method | Args | Description |
|---|---|---|
| `*new` | `name, buffer, numSlices=8` | Create slicer bound to a Pdef name |
| `bars` | `numBars, beatsPerBar=4` | Set timing from bar count (chainable) |
| `dur_` | `beats` | Set explicit slice duration |
| `pattern` | `order, rate=1, amp=0.5` | Assign slice pattern to the Pdef |

Negative indices in `order` reverse that slice. Does NOT auto-play — call `Pdef(\name).play` after.

```supercollider
~cc.samples.load(\amen);
b = CCBreakbeat(\break, ~cc.samples.at(\amen), 8).bars(1);
b.pattern([0, 0, 3, 2, 7, 6, 5, 4]);
Pdef(\break).play;

// Hot-swap while playing
b.pattern([0, 1, -2, 3, 4, 5, -6, 7]);
```
