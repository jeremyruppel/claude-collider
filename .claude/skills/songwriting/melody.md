# Melody & Lead Lines

## Contour Shapes

Every good melody has a recognizable shape. Choose one intentionally:

### Arch (up then down)
Natural, singable. The most common melodic shape:
```supercollider
\degree, Pseq([0, 2, 4, 5, 4, 2, 0, Rest()], inf)
```

### Descent
Tension release, gravity pulling downward:
```supercollider
\degree, Pseq([5, 4, 3, 2, 1, 0, Rest(), Rest()], inf)
```

### Wave
Gentle oscillation, hypnotic. Good for pads and ambient leads:
```supercollider
\degree, Pseq([0, 2, 4, 2, 0, -1, 0, 2], inf)
```

### Plateau
Repeated note with rhythm as the hook. Pitch barely moves:
```supercollider
\degree, Pseq([0, 0, 0, 2, 0, 0, Rest(), 0], inf),
\dur, Pseq([0.25, 0.25, 0.5, 0.25, 0.25, 0.5, 0.5, 0.5], inf)
```

## Step vs Leap

### Mostly stepwise motion
Adjacent scale degrees (0→1, 3→2) sound smooth and singable. This should be the default.

### Leaps for emphasis
A leap (jump of 3+ degrees) grabs attention. Use sparingly. After a leap, resolve by step in the opposite direction:
```supercollider
// Leap up to 5, then step back down
\degree, Pseq([0, 4, 3, 2, 1, 0, Rest(), Rest()], inf)
//             ^ leap up   ^ stepwise descent
```

### Large leaps (>5th) should be rare
An octave jump or 6th is powerful — save it for climactic moments.

## Motif Development

The secret to melodies that feel composed, not random:

### 1. State a 2–4 note motif
```supercollider
\degree, Pseq([0, 2, 4, Rest()], 1)  // the motif: three rising notes
```

### 2. Repeat it
```supercollider
\degree, Pseq([0, 2, 4, Rest()], 2)  // say it again so it registers
```

### 3. Vary one thing
Change rhythm, starting pitch, octave, or add a note:
```supercollider
// Variation 1: same shape, different starting note
\degree, Pseq([2, 4, 5, Rest()], 1)

// Variation 2: same notes, different rhythm
\degree, Pseq([0, 2, 4, Rest()], 1),
\dur, Pseq([0.5, 0.25, 0.25, 1], 1)  // was [0.25, 0.25, 0.25, 0.25]

// Variation 3: add a tail
\degree, Pseq([0, 2, 4, 3, 2, Rest()], 1)
```

Full example combining motif + variations:
```supercollider
Pdef(\melody, Pbind(
    \instrument, \cc_lead,
    \scale, Scale.dorian, \root, 2, \octave, 5,
    \degree, Pseq([
        0, 2, 4, Rest(),           // motif
        0, 2, 4, Rest(),           // repeat
        2, 4, 5, Rest(),           // vary: shift up
        0, 2, 4, 3, 2, Rest()     // vary: add tail
    ], inf),
    \dur, Pseq([
        0.25, 0.25, 0.25, 0.75,
        0.25, 0.25, 0.25, 0.75,
        0.25, 0.25, 0.25, 0.75,
        0.25, 0.25, 0.25, 0.25, 0.25, 0.5
    ], inf),
    \amp, 0.4
));
```

## Phrasing

### 2-bar or 4-bar phrases
Don't write continuous streams of notes. Melodies breathe in phrases, just like speech:

```supercollider
// 2-bar phrase with breathing room
\degree, Pseq([0, 2, 4, 3, 2, 0, Rest(), Rest()], inf),  // 1 bar melody, 1 bar rest
\dur, 0.25
```

### Leave gaps
`Rest()` between phrases is not empty — it's anticipation. The listener waits for the next phrase.

### Call and response
First phrase asks (ends on a tension note), second phrase answers (resolves to root):
```supercollider
\degree, Pseq([
    0, 2, 4, 5,  Rest(), Rest(),  // call: ends on 5 (tension)
    4, 3, 2, 0,  Rest(), Rest()   // response: resolves to root
], inf)
```

## Rhythmic Displacement

Same notes, shifted by an 8th or 16th — instant variation with zero melodic effort:
```supercollider
// Original
\degree, Pseq([0, 2, 4, Rest()], 1), \dur, 0.25

// Displaced by one 16th: add a rest at the start
\degree, Pseq([Rest(), 0, 2, 4], 1), \dur, 0.25
```
