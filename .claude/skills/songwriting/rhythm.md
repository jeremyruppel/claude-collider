# Drums & Rhythm

## Velocity and Dynamics

The #1 fix for mechanical-sounding drums. Never use flat `\amp`.

### Mechanical vs humanized
```supercollider
// Bad: flat velocity — sounds like a metronome
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, 0.3
));

// Good: accent pattern with ghost notes
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.4, 0.15, 0.25, 0.15, 0.35, 0.15, 0.25, 0.1], inf)
));
```

### Accent principle
- **Downbeats** get strong hits
- **Offbeats** get ghost notes (very low amp, 0.05–0.15)
- **Upbeats** are medium — they drive the groove forward

## Swing and Shuffle

Express swing by alternating long-short durations:

```supercollider
// Straight 16ths
\dur, 0.25

// Swung 16ths (approx 60% swing)
\dur, Pseq([0.3, 0.2], inf)

// Heavy shuffle (triplet feel)
\dur, Pseq([1/3, 1/6], inf)
```

Swing applied to hi-hats:
```supercollider
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([0.3, 0.2], inf),
    \amp, Pseq([0.35, 0.15], inf)  // accent the long note
));
```

## Ghost Notes

Very low amplitude hits that fill gaps and make drums feel "played":
```supercollider
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([
        Rest(), Rest(), Rest(), Rest(),  // beat 1
        0.5,   0.08,  0.06,  0.1,       // beat 2: hit + ghosts
        Rest(), Rest(), 0.06,  Rest(),   // beat 3: ghost only
        0.5,   0.08,  0.1,   0.06       // beat 4: hit + ghosts
    ], inf)
));
```

## Genre Drum Patterns

### House (4/4, 120–128 BPM)
Kick on every beat. Offbeat hats. Clap on 2 & 4:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, 1, \amp, 0.6
));
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.5,
    \amp, Pseq([0, 0.3, 0, 0.25], inf)  // offbeat only
));
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 2,
    \amp, 0.4
));
```

### Techno (4/4, 128–140 BPM)
Harder kick, industrial hats, syncopated variations:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 1, 0.75, 0.25, 1, 1, 0.5, 0.5], inf),
    \amp, 0.7
));
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.4, 0.1, 0.25, 0.1], inf),
    \decay, Pseq([0.03, 0.02, 0.06, 0.02], inf)  // open/closed feel
));
```

### Hip-hop / Boom-bap (85–95 BPM)
Kick-snare with lazy swing, sparse hats:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1.5, 0.5, 1, 1], inf),
    \amp, 0.6
));
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, Pseq([Rest(), Rest(), 2, Rest(), Rest(), 2], inf),
    \amp, 0.5
));
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([0.3, 0.2], inf),  // swung
    \amp, Pseq([0.2, 0.1], inf)
));
```

### DnB (170–175 BPM)
Breakbeat kick-snare. Snare on beat 3 (not 2 & 4). Fast hats:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 0.75, 0.25, 0.5, 0.5, 1], inf),
    \amp, 0.6
));
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, Pseq([Rest(), 2, Rest(), 2], inf),  // beat 3 of each bar
    \amp, 0.5
));
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.25, 0.08, 0.15, 0.08], inf)
));
```

### R&B (90–110 BPM)
Shuffled hats, ghost snares, rim clicks:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1.5, 1, 0.5, 1], inf),
    \amp, 0.5
));
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, Pseq([Rest(), 2, Rest(), 2], inf),
    \amp, 0.4
));
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([0.75, 0.5, 0.75, Rest(), 0.5, 0.5], inf),
    \amp, 0.2
));
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([1/3, 1/6], inf),  // triplet shuffle
    \amp, Pseq([0.2, 0.08], inf)
));
```

### Garage / 2-step (130–135 BPM)
Skippy kick, snare on beat 2 only, shuffled hats:
```supercollider
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([0.75, 0.5, 0.75, 0.5, 0.5, 1], inf),
    \amp, 0.5
));
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, Pseq([Rest(), 2, Rest(), Rest()], inf),
    \amp, 0.45
));
```

## Hi-hat Beyond Straight 16ths

### Open/closed alternation
Vary `\decay` per hit:
```supercollider
\decay, Pseq([0.02, 0.02, 0.08, 0.02], inf)  // short-short-open-short
```

### Accent placement
- **House:** accent offbeats (push the groove forward)
- **Techno:** accent on-beats (driving, mechanical)
- **Hip-hop/R&B:** accent irregularly (human feel)

### Open hat on the "and" of 4
Classic fill-in tension builder:
```supercollider
\amp, Pseq([0.3, 0.1, 0.2, 0.1, 0.3, 0.1, 0.2, 0.1,
            0.3, 0.1, 0.2, 0.1, 0.3, 0.1, 0.4, 0.1], inf),
\decay, Pseq([0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
              0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.1, 0.02], inf)
```
