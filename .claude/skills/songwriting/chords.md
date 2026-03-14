# Chord Voicing & Progressions

## Voicing Rules

### No 3rds below C3 (MIDI 48)
Low intervals are muddy. Below C3, use only roots, 5ths, and octaves. The 3rd can live in the mid register.

### Pad voicing — open and spread
Spread notes across octaves. Don't cluster everything in one octave:
```supercollider
// Bad: block chord, all in one octave
\midinote, [60, 64, 67, 71]  // C E G B — dense, muddy

// Good: open voicing, spread across registers
\midinote, [48, 67, 71, 76]  // C3, G4, B4, E5 — open, clear
```

### Keys/stabs — close voicing OK
Short sounds in the mid register can cluster. Stabs and keys benefit from tight voicing:
```supercollider
\midinote, [64, 67, 72]  // E4, G4, C5 — tight, punchy
\dur, 0.25, \sustain, 0.1
```

### Drop-2 voicing
Take the 2nd-from-top note of a close voicing and drop it an octave. Creates space:
```supercollider
// Close: C E G B = [60, 64, 67, 71]
// Drop-2: move G down an octave
\midinote, [55, 60, 64, 71]  // G3, C4, E4, B4
```

## Voice Leading

### Common tones stay, other voices move by step
When changing chords, keep shared notes in place. Move everything else by the smallest interval:

```supercollider
// Smooth voice leading: Dm7 -> G7 -> Cmaj7
Pdef(\pads, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [50, 60, 65, 72],  // Dm7:  D3 C4 F4 C5
        [50, 59, 65, 71],  // G7:   D3 B3 F4 B4 (D stays, others step)
        [48, 60, 64, 71],  // Cmaj7: C3 C4 E4 B4 (B stays, others step)
    ], inf),
    \dur, 4
));
```

### Contrary motion
When bass moves up, upper voices should move down (and vice versa). Creates width and independence.

## Extensions by Genre

| Genre | Typical harmony |
|-------|----------------|
| House/techno | Triads and 7ths — keep simple |
| R&B/neo-soul | 9ths, 11ths, 13ths, altered dominants |
| Ambient | Sus chords, add9, quartal voicings (stacked 4ths) |
| DnB | Minor triads, sometimes 7ths |
| Lo-fi | Maj7, min7, add color notes from key |

Quartal voicing example (ambient):
```supercollider
// Stacked 4ths — no clear major/minor, ethereal
\midinote, [48, 53, 58, 63]  // C F Bb Eb — all 4ths apart
```

## Progressions by Genre

**Never use Am-F-C-G (I-V-vi-IV) axis progression or any rotation.**

### House
Minimal harmony — two-chord loops with 7ths:
```supercollider
\degree, Pseq([[0, 2, 4, 6], [3, 5, 0, 2]], inf)  // i7 -> iv7 loop
\scale, Scale.dorian, \octave, 4, \dur, 4
```

### R&B / Neo-soul
Rich chromatic movement — ii9-V13-Imaj9:
```supercollider
\midinote, Pseq([
    [50, 57, 62, 65, 69],  // Dm9
    [43, 59, 62, 65, 69],  // G13
    [48, 55, 64, 67, 71],  // Cmaj9
], inf), \dur, Pseq([4, 4, 8], inf)
```

### Techno
Single chord or modal — minimal harmonic motion:
```supercollider
// One chord, let rhythm and timbre do the work
\degree, [0, 2, 4], \scale, Scale.minor, \dur, 8
```

### DnB
Minor key movement with tension:
```supercollider
\degree, Pseq([[0, 2, 4], [4, 6, 1], [3, 5, 0], [5, 0, 2]], inf)
\scale, Scale.minor, \dur, 4  // i -> v -> iv -> vi
```

### Ambient / Downtempo
Suspended and add9 chords, slow movement:
```supercollider
\midinote, Pseq([
    [48, 55, 60, 67],  // Csus2 spread
    [53, 58, 65, 72],  // Fsus2 spread
    [50, 57, 62, 69],  // Dsus2 spread
], inf), \dur, 8
```
