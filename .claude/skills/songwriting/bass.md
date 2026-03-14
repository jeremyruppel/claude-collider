# Bass Lines

## Beyond Root Notes

A bass line that only plays the root of each chord is a placeholder, not a part. Techniques to add movement:

### Root-5th alternation
The simplest upgrade. Alternates between root and 5th:
```supercollider
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.minor, \root, 0,
    \degree, Pseq([0, 4, 0, 4], inf),  // root, 5th, root, 5th
    \octave, 3,
    \dur, 0.5
));
```

### Octave jumps for energy
Drop to the low octave, jump up for emphasis:
```supercollider
\degree, Pseq([0, 0, 0, 0], inf),
\octave, Pseq([2, 3, 2, 2], inf)  // low-high-low-low
```

### Approach notes
Chromatic half-step into the next chord's root. Creates forward motion:
```supercollider
// Approaching root from half step below at end of bar
\note, Pseq([0, 0, 0, -1, | 0, 0, 0, 4], inf)  // -1 = half step below root
\dur, 0.5
```

### Passing tones
Connect chord tones with scale steps:
```supercollider
\degree, Pseq([0, 1, 2, 4, 3, 2, 1, 0], inf)  // walk up to 5th and back
\dur, 0.25
```

## Rhythmic Patterns by Genre

### Four-on-floor (house)
Not just steady quarters — add ghost notes and amp variation:
```supercollider
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \degree, Pseq([0, 0, Rest(), 0, 0, Rest(), 0, 0], inf),
    \octave, 3, \scale, Scale.dorian, \root, 2,
    \amp, Pseq([0.6, 0.3, 0, 0.5, 0.3, 0, 0.6, 0.2], inf),
    \dur, 0.25
));
```

### Syncopated (R&B, hip-hop)
Off-beat emphasis, rests on the downbeat:
```supercollider
\degree, Pseq([Rest(), 0, Rest(), 4, 0, Rest(), 2, Rest()], inf),
\dur, 0.25,
\amp, Pseq([0, 0.5, 0, 0.6, 0.4, 0, 0.5, 0], inf)
```

### Broken (DnB, garage)
Unpredictable hits with mixed durations:
```supercollider
\degree, Pseq([0, Rest(), 0, 4, Rest(), 2, Rest(), 0], inf),
\dur, Pseq([0.25, 0.5, 0.25, 0.25, 0.5, 0.25, 0.75, 0.25], inf)
```

### Driving (techno)
Steady 16ths with filter movement as the expression:
```supercollider
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \degree, Pseq([0, 0, 0, 0, 3, 3, 0, 0], inf),
    \octave, 3, \scale, Scale.phrygian,
    \dur, 0.25,
    \cutoff, Pseq([400, 600, 800, 1200, 2000, 1200, 800, 600], inf)
));
```

## Interaction with Kick

- **Kick is steady →** bass syncopates. The kick anchors, bass dances around it.
- **Kick is syncopated →** bass locks to the root on downbeats. One part should be the anchor.
- **Sidechain** creates pumping space — use it when both parts are active.

## Register

Stay in octave 2–3. Bass above C4 (MIDI 60) is a mid-range part, not a bass. If you want bass energy, keep it low and use filter movement for expression instead of climbing into higher registers.

## Filter as Expression

Vary `\cutoff` per note for movement instead of only changing pitch:
```supercollider
\cutoff, Pseq([300, 500, 800, 1500, 800, 500, 300, 300], inf),
\resonance, Pseq([0.3, 0.3, 0.2, 0.1, 0.2, 0.3, 0.3, 0.4], inf)
```
This keeps the bass line rooted but gives it timbral motion — darker on weak beats, brighter on accents.
