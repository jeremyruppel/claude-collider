/*
# Chopped Break

Haunting harmonic minor melody over chopped breakbeats at 142 BPM in Em.

## Elements

- **Break** (cc_breakbeat) - 4-bar break sliced into 16, rearranged with reversed slices, half-time repeats, and stutter patterns across sections
- **Bass** (cc_bass) - 2-bar CCMotif riff on root/4th/3rd in Em harmonic minor
- **Melody** (cc_pluck) - Descending pluck motif with CCPhrase development (state, transpose +3, invert), dark and sparse

## Effects

- **Delay** on melody (mix 0.15, dotted eighth 0.375s, decay 2)
- **Reverb** chained from delay (mix 0.55, room 0.9, damp 0.7)

## Arrangement (36 bars, ~1:01)

1. **Intro** (4 bars) - Melody alone with reverb wash
2. **Build** (4 bars) - Straight break enters
3. **Chop 1** (8 bars) - Full stack, rearranged slices with reversed hits
4. **Halftime** (4 bars) - Bass and break only, repeated slices at half rate
5. **Chop 2** (8 bars) - Full stack, fully reversed slice order
6. **Stutter** (4 bars) - Melody and break, stutter pattern at double rate
7. **Outro** (4 bars) - Melody alone

## Notes

- The break uses `drum_break_142bpm_4_bar` from the repo's `samples/` directory
- Delay chains into reverb for extra depth on the pluck melody
- No chord progression; all parts are riff-based over Em harmonic minor
- CCBreakbeat showcase: straight, chopped, halftime, reversed, and stutter patterns
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(142);

// 2. Break - 4-bar breakbeat sliced into 16
~cc.samples.load(\drum_break_142bpm_4_bar);
b = CCBreakbeat(\break, ~cc.samples.at(\drum_break_142bpm_4_bar), 16).bars(4);
b.pattern((0..15), amp: 0.5);

// 3. Bass - 2-bar root/4th/3rd riff in Em harmonic minor
m = CCMotif([0, 0, Rest(), 0, 3, 3, Rest(), 2]);
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.harmonicMinor,
    \root, 4,
    \octave, 3,
    \degree, Pn(m),
    \dur, 0.5,
    \amp, 0.5,
    \legato, 0.6
));

// 4. Melody - haunting pluck in Em harmonic minor
~m2 = CCMotif([4, 3, Rest(), 2, 0, Rest(), -1, 0]);
Pdef(\melody, Pbind(
    \instrument, \cc_pluck,
    \scale, Scale.harmonicMinor,
    \root, 4,
    \octave, 5,
    \degree, CCPhrase(~m2, [
        \state,
        \state,
        \transpose, 3,
        \invert
    ]),
    \dur, 0.5,
    \decay, 3,
    \color, 0.3,
    \amp, 0.35
));

// 5. Effects - delay into reverb on melody
~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.15, \time, 0.375, \decay, 2);
~cc.fx.route(\melody, \fx_delay);
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.55, \room, 0.9, \damp, 0.7);
~cc.fx.connect(\fx_delay, \fx_reverb);

// 6. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 4, [\melody], {
        b.pattern((0..15));
    }],
    [\build, 4, [\melody, \break], {
        b.pattern((0..15));
    }],
    [\chop1, 8, [\melody, \bass, \break], {
        b.pattern([0, 0, 3, 2, -7, 6, 5, 4, 0, 1, -2, 3, 8, 9, -14, 15]);
    }],
    [\halftime, 4, [\bass, \break], {
        b.pattern([0, 0, 0, 0, 4, 4, 4, 4, 8, 8, 8, 8, 12, 12, 12, 12], 0.5);
    }],
    [\chop2, 8, [\melody, \bass, \break], {
        b.pattern([15, 14, -13, 12, 3, 2, -1, 0, 7, 6, -5, 4, 11, 10, -9, 8]);
    }],
    [\stutter, 4, [\melody, \break], {
        b.pattern([0, 0, 0, 1, 4, 4, 4, 5, 0, 0, -7, -7, 14, 14, 15, 15], 2);
    }],
    [\outro, 4, [\melody], {
        b.pattern((0..15));
    }]
]);
