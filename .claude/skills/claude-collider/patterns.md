# Compositional Pattern Helpers

Classes for building melodic phrases and arrangements. All are Pattern subclasses (except CCMelody) and embed naturally in Pbind/Pseq.

## CCMotif - Melodic Motif

Wraps a degree array with chainable transformations. Each returns a new immutable CCMotif.

| Method | Args | Description |
|---|---|---|
| `*new` | `degrees` | Create from array of scale degrees |
| `transpose` | `n` | Shift all degrees by n |
| `invert` | | Mirror around first non-rest degree |
| `retrograde` | | Reverse the sequence |
| `extend` | `extraDegrees` | Append degrees |
| `truncate` | `n` | Keep first n degrees |

Rests are preserved through all transformations.

```supercollider
m = CCMotif([0, 2, 4, 3]);
m.transpose(2);           // [2, 4, 6, 5]
m.invert;                 // [0, -2, -4, -3]
m.retrograde;             // [3, 4, 2, 0]
m.transpose(2).invert;    // chaining works

// Use in Pbind like any pattern
Pdef(\mel, Pbind(\instrument, \cc_lead, \degree, Pn(m), \dur, 0.5)).play
```

## CCPhrase - Motif Development Plan

Sequences a motif through a series of transformations. Each step applies an operation to the **original motif** (not cumulative).

| Operation | Arg? | Description |
|---|---|---|
| `\state` | no | Play motif unchanged |
| `\transpose` | yes | Transpose by n degrees |
| `\invert` | no | Invert the motif |
| `\retrograde` | no | Reverse the motif |
| `\extend` | yes | Append degrees (array) |
| `\truncate` | yes | Keep first n degrees |

```supercollider
m = CCMotif([0, 2, 4, 3]);

// Plan: play, play, transpose up 2, back to original
p = CCPhrase(m, [
  \state, \state,
  \transpose, 2,
  \state
]);

Pdef(\mel, Pbind(\instrument, \cc_lead, \degree, p, \dur, 0.5)).play
```

## CCMelody - Melodic Structure Factory

Utility class with factory methods that return Patterns.

| Method | Args | Description |
|---|---|---|
| `*callAndResponse` | `call, response, repeats=inf` | Alternates two motifs |
| `*develop` | `motif, repeats=inf` | Standard development: state x2, transpose +2, resolve |

```supercollider
m = CCMotif([0, 2, 4, 3]);

// Call and response
CCMelody.callAndResponse(m, m.transpose(2))

// Standard development (uses CCPhrase internally)
CCMelody.develop(m)
```

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

Sections are `[name, bars, elements, action]` arrays. `action` is an optional function.

```supercollider
// Define patterns first, then arrange them
CCArrangement([
  [\intro,  4, [\drums]],
  [\verse,  8, [\drums, \bass, \pad]],
  [\chorus, 8, [\drums, \bass, \lead, \pad]],
  [\outro,  4, [\pad]]
]).play
```

Elements can be Pdef or Ndef names. The arrangement auto-starts/stops them at section boundaries using `TempoClock.schedAbs` for drift-free timing.

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
