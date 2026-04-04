# Breakbeats

## CCBreakbeat Basics

CCBreakbeat slices a buffer into equal segments and sequences them via Pbind patterns. Load a breakbeat sample, divide it, rearrange the slices.

```supercollider
~cc.samples.load(\amen);
b = CCBreakbeat(~cc.samples.at(\amen), 8).bars(1);

// Original order — straight playback
Pdef(\break, b.pattern([0, 1, 2, 3, 4, 5, 6, 7]));
```

### Slice count choices
- **8 slices** over 1 bar = 8th note resolution (most common)
- **16 slices** over 1 bar = 16th note resolution (fine chops)
- **16 slices** over 2 bars = 8th note resolution on a 2-bar loop
- **4 slices** over 1 bar = quarter note resolution (coarse, big swaps)

## Rearrangement Techniques

### Swap halves
Flip the first and second half of the beat:
```supercollider
Pdef(\break, b.pattern([4, 5, 6, 7, 0, 1, 2, 3]));
```

### Stutter
Repeat a slice to create tension or rolls:
```supercollider
// Stutter slice 0 four times, then normal
Pdef(\break, b.pattern([0, 0, 0, 0, 4, 5, 6, 7]));

// Snare roll: repeat the snare slice
Pdef(\break, b.pattern([0, 1, 2, 3, 4, 4, 4, 4]));
```

### Drop and replace
Replace weak slices with repeated strong ones:
```supercollider
// Replace slices 1 and 5 with slice 0 (kick repeat)
Pdef(\break, b.pattern([0, 0, 2, 3, 4, 0, 6, 7]));
```

### Reverse individual slices
Negative indices play that slice backwards:
```supercollider
// Reverse slices 2 and 6 for a turntable effect
Pdef(\break, b.pattern([0, 1, -2, 3, 4, 5, -6, 7]));

// Reverse the whole second half
Pdef(\break, b.pattern([0, 1, 2, 3, -7, -6, -5, -4]));
```

### Rearrange to new groove
Build completely new patterns from the same source material:
```supercollider
// Jungle-style chop
Pdef(\break, b.pattern([0, 0, 3, 2, 7, 6, 5, 4]));

// Halftime feel — stretch by repeating each slice
b2 = CCBreakbeat(~cc.samples.at(\amen), 8).bars(2);
Pdef(\break, b2.pattern([0, 0, 1, 1, 2, 2, 3, 3]));
```

## Per-Slice Rate Control

### Speed up / slow down individual slices
```supercollider
// Double-time the last two slices
Pdef(\break, b.pattern([0, 1, 2, 3, 4, 5, 6, 7], rate: [1, 1, 1, 1, 1, 1, 2, 2]));
```

### Pitch shifting via rate
Rate changes pitch: 2 = octave up, 0.5 = octave down:
```supercollider
// Pitched down intro, normal drop
Pdef(\break_intro, b.pattern([0, 1, 2, 3, 4, 5, 6, 7], rate: 0.5));
Pdef(\break_drop, b.pattern([0, 1, 2, 3, 4, 5, 6, 7], rate: 1));
```

### Random rate variation with Pattern
```supercollider
Pdef(\break, b.pattern(
    [0, 1, 2, 3, 4, 5, 6, 7],
    rate: Prand([1, 1, 1, 1.5, 0.75], inf)
));
```

## Genre Applications

### Jungle / DnB (160-175 BPM)
Heavy rearrangement, fast tempo, snare on beat 3. The amen break is the canonical source:
```supercollider
~cc.tempo(170);
b = CCBreakbeat(~cc.samples.at(\amen), 16).bars(1);
Pdef(\break, b.pattern([0, 0, 4, 3, 8, 9, -10, 7, 0, 1, 12, 3, 8, 8, 14, 15]));
```

### Hip-hop (85-95 BPM)
Slower, keep the original groove mostly intact, subtle swaps:
```supercollider
~cc.tempo(90);
b = CCBreakbeat(~cc.samples.at(\think), 8).bars(2);
Pdef(\break, b.pattern([0, 1, 2, 3, 4, 5, 6, 7]));
```

### Breakcore (160-300 BPM)
Extreme slicing, stutters, reversals, rapid rate changes:
```supercollider
~cc.tempo(200);
b = CCBreakbeat(~cc.samples.at(\amen), 16).bars(1);
Pdef(\break, b.pattern(
    [0, 0, 0, 3, -4, -4, 6, 7, 0, 0, 12, -3, 8, 8, 8, 8],
    rate: [1, 2, 2, 1, 1, 1, 1.5, 0.5, 1, 2, 1, -1, 2, 2, 4, 4]
));
```

## Layering Breakbeats with Live Drums

Breakbeats work well layered under or over programmed drums:
```supercollider
// Programmed kick reinforces the break
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 1, 0.75, 0.25, 1], inf),
    \amp, 0.5
));

// Break provides texture and ghost notes
Pdef(\break, b.pattern([0, 1, 2, 3, 4, 5, 6, 7], amp: 0.25));
```

## Processing Breaks

Route through effects for character:
```supercollider
// Bitcrushed break
~cc.fx.load(\bitcrush);
~cc.fx.route(\break, \fx_bitcrush);
~cc.fx.set(\fx_bitcrush, \bits, 8, \rate, 8000);

// Filtered break with resonance
~cc.fx.load(\lpf);
~cc.fx.route(\break, \fx_lpf);
~cc.fx.set(\fx_lpf, \freq, 2000, \res, 0.6);

// Distorted break
~cc.fx.load(\distortion);
~cc.fx.route(\break, \fx_distortion);
~cc.fx.set(\fx_distortion, \drive, 0.7);
```
