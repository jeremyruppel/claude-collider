# Scales & Modes

## Common Scales

| Scale | Character | Genres |
|-------|-----------|--------|
| Natural minor | Dark, standard | Most electronic, techno, DnB |
| Dorian | Groovy, hopeful minor | House, deep house, neo-soul |
| Phrygian | Dark, Spanish, tense | Techno, industrial, psytrance |
| Mixolydian | Bright, bluesy | Funk, disco, classic house |
| Harmonic minor | Dramatic, Eastern | Trance, psytrance, darkwave |
| Pentatonic minor | Safe, hooky | Everything — never sounds wrong |
| Blues | Gritty, expressive | Hip-hop, R&B, lo-fi |

## Using Scales in Pbind

Set scale and root once, then write melodies as degree numbers:

```supercollider
// Dorian in D — the classic deep house sound
Pdef(\lead, Pbind(
    \instrument, \cc_lead,
    \scale, Scale.dorian,
    \root, 2,  // D
    \degree, Pseq([0, 2, 4, 3, 2, 1, 0, Rest()], inf),
    \octave, 4,
    \dur, Pseq([0.5, 0.5, 0.25, 0.25, 0.5, 0.5, 0.5, 1], inf)
));
```

Switching scales mid-pattern for color:
```supercollider
\scale, Pseq([Scale.minor, Scale.minor, Scale.minor, Scale.harmonicMinor], inf)
```

## Chord Tones vs Tension Notes

Scale degrees and their role:
- **0, 2, 4** (root, 3rd, 5th) — Chord tones. Safe landing points. Resolve to these.
- **1, 3** (2nd, 4th) — Mild tension. Good passing tones and suspensions.
- **5, 6** (6th, 7th) — Color tones. Add flavor, especially 7ths in jazz/R&B contexts.

**Rule of thumb:** Start and end phrases on chord tones (0, 2, 4). Use tension notes in the middle of phrases for movement.

## Chromatic Approach Notes

Approach a chord tone from a half step below for a jazz/R&B feel:

```supercollider
// Approaching root (0) from below via chromatic note
\degree, Pseq([4, 3, 2, -0.5, 0], 1)  // the -0.5 is a half step below root
```

In practice, use `\note` or `\midinote` for chromatic passages instead of `\degree`:
```supercollider
// Chromatic walk up to the 5th (degree 4 = MIDI offset 7 in minor)
\note, Pseq([5, 6, 7], 1)  // half steps relative to root
```

## Mode Selection Guide

When unsure which mode to use:
1. **Default safe choice:** Natural minor or pentatonic minor
2. **Want it groovier?** Dorian (raise the 6th)
3. **Want more tension?** Phrygian (flat 2nd) or harmonic minor
4. **Want brightness?** Mixolydian
5. **Want no wrong notes?** Pentatonic minor — 5 notes, all consonant
