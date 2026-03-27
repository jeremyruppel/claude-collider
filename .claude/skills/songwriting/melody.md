# Melody & Lead Lines

## Rule: Use CCMotif and CCPhrase

**NEVER write a melody as a flat Pseq longer than 8 elements.** Use CCMotif for motifs and CCPhrase for structured development. This is the difference between a melody that sounds composed and one that sounds random.

```supercollider
// BAD: flat array, no structure, sounds random or contrived
\degree, Pseq([0, 2, 4, Rest(), 2, 4, 5, Rest(), 3, 4, 2, 0, Rest(), Rest(), Rest(), Rest()], inf)

// GOOD: motif with structured development
var m = CCMotif([0, 2, 4, Rest()]);
\degree, CCPhrase(m, [\state, \state, \transpose, 2, \invert])
```

## CCMotif — Transformable Melodic Fragment

A CCMotif wraps a short degree array (2–6 notes + rests) and provides transformations. Each transformation returns a new CCMotif. Since it's a Pattern subclass, it works directly in Pseq and Pn.

```supercollider
var m = CCMotif([0, 2, 4, Rest()]);

m.transpose(2)    // [2, 4, 6, Rest()]
m.transpose(-1)   // [-1, 1, 3, Rest()]
m.invert           // [0, -2, -4, Rest()] — mirror around first note
m.retrograde       // [Rest(), 4, 2, 0]
m.extend([3, 2])   // [0, 2, 4, Rest(), 3, 2]
m.truncate(3)      // [0, 2, 4]

// Chainable
m.transpose(2).invert   // [2, 0, -2, Rest()]

// Works directly with SC patterns
\degree, Pseq([Pn(m, 2), m.transpose(2), m.invert], inf)
```

## CCPhrase — Motif Development Sequencer

CCPhrase takes a motif and a development plan, producing a structured phrase that loops.

**Operations:**
- `\state` — play motif as-is
- `\transpose, n` — play shifted by n degrees
- `\invert` — play mirrored around first note
- `\retrograde` — play reversed
- `\extend, [degrees]` — play with extra notes appended

```supercollider
var m = CCMotif([0, 2, 4, Rest()]);

// State it, repeat it, vary it, resolve it
\degree, CCPhrase(m, [
    \state,          // state the motif
    \state,          // repeat so it registers
    \transpose, 2,   // variation: shift up
    \invert          // variation: mirror
])
```

## CCMelody — Common Structures

Convenience factory methods for standard melodic forms.

### Call and response
```supercollider
var call = CCMotif([0, 2, 4, 5, Rest(), Rest()]);
var resp = CCMotif([4, 3, 2, 0, Rest(), Rest()]);
\degree, CCMelody.callAndResponse(call, resp)
```

### Auto-develop (state ×2, transpose, resolve)
```supercollider
\degree, CCMelody.develop(CCMotif([0, 2, 4, Rest()]))
```

## Contour Shapes

Every motif has a shape. Choose one intentionally:

- **Arch** (up then down): `CCMotif([0, 2, 4, 2, 0, Rest()])` — natural, singable
- **Descent** (gravity): `CCMotif([5, 4, 3, 2, 0, Rest()])` — tension release
- **Wave** (oscillation): `CCMotif([0, 2, 4, 2, 0, -1, 0, Rest()])` — hypnotic, good for arps
- **Plateau** (repeated note): `CCMotif([0, 0, 0, 2, 0, Rest()])` — rhythm is the hook

## Step vs Leap

- **Mostly stepwise**: Adjacent degrees (0→1, 3→2) sound smooth. This is the default.
- **Leaps for emphasis**: A jump of 3+ degrees grabs attention. Resolve by step in the opposite direction.
- **Large leaps (>5th)**: Save for climactic moments. An octave jump is powerful — use it once.

## Phrasing

### Phrase length
Motifs should be 2–6 notes. Phrases (via CCPhrase) should total 2 or 4 bars. Count your beats.

### Leave gaps
End motifs with `Rest()`. The silence is anticipation, not emptiness.

### Call and response
First phrase asks (ends on tension — degree 3, 4, or 5). Second phrase answers (resolves to 0 or 2):
```supercollider
var call = CCMotif([0, 2, 4, 5, Rest(), Rest()]);   // tension
var resp = CCMotif([4, 3, 2, 0, Rest(), Rest()]);   // resolve
\degree, CCMelody.callAndResponse(call, resp)
```

## Melody in Electronic Music

Electronic melodies are different from pop/rock. They need to:
- **Be sparse** — 3–5 notes per motif, lots of Rest()
- **Loop as a hook** — the phrase cycle IS the melody
- **Leave room** — effects (delay, reverb) fill space, not more notes
- **Stay in register** — octave 4–5 for leads, don't overlap bass or pads

## Full Example

```supercollider
var m = CCMotif([0, 2, 4, Rest()]);

Pdef(\lead, Pbind(
    \instrument, \cc_lead,
    \scale, Scale.dorian, \root, 2, \octave, 5,
    \degree, CCPhrase(m, [
        \state,           // state the motif
        \state,           // repeat
        \transpose, 2,    // shift up — new territory
        \extend, [3, 2, Rest()]  // add a descending tail — resolution
    ]),
    \dur, 0.25,
    \amp, 0.4,
    \sustain, 0.15
)).play(quant: 4);
```

## Rhythmic Displacement

Same motif, shifted by a 16th — instant variation:
```supercollider
var m = CCMotif([0, 2, 4, Rest()]);
// Displace by adding a rest at the start
var displaced = CCMotif([Rest()]).extend(m.degrees);
```
