/*
# Chopped Break

Haunting harmonic minor melody over chopped breakbeats at 142 BPM in Em.

## Elements

- **Break** (cc_breakbeat) - 4-bar break sliced into 16, rearranged with reversed slices, half-time repeats, and stutter patterns across sections
- **Bass** (cc_bass) - 2-bar riff of long, anchoring notes: held E, pickup, B, then C–B–D# (leading tone pulling back to E)
- **Melody** (cc_pluck) - 8-bar call and answer from one descending motif. Call: state, state, transpose +3, invert. Answer: retrograde, transpose +5 (peak), transpose +2, truncated resolution to the root
- **Grains** (Ndef, GrainBuf) - Drifting granular cloud from a mono copy of the break, pitched down (0.25–0.75x), band-limited 180 Hz–2.5 kHz

## Effects

- **Delay** on melody (mix 0.4, quarter note 0.4225s, decay 3)
- **Reverb** chained from delay, also fed directly by grains (mix 0.55, room 0.9, damp 0.7)

## Arrangement (48 bars, ~1:21)

1. **Intro** (8 bars) - Melody and grains
2. **Build** (8 bars) - Straight break enters
3. **Chop 1** (8 bars) - Melody, bass, break; rearranged slices with reversed hits (grains drop out)
4. **Halftime** (8 bars) - Bass, break, grains; repeated slices at half rate
5. **Chop 2** (8 bars) - Melody, bass, break; fully reversed slice order
6. **Stutter** (4 bars) - Melody and break, stutter pattern at double rate
7. **Outro** (4 bars) - Melody and grains, ending on the answer's resolution

## Notes

- The break uses `drum_break_142bpm_4_bar` from the repo's `samples/` directory
- GrainBuf needs a mono buffer, so grains read channel 0 of the break into their own buffer `g`
- Melody sections are 8 bars so each one plays the full call and answer
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

// 3. Bass - 2-bar riff in Em harmonic minor: E, E, B | C, B, D#
Pdef(\bass).quant = 8;
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.harmonicMinor,
    \root, 4,
    \octave, 3,
    \degree, Pseq([CCMotif([0, 0, 4]), CCMotif([5, 4, -1])], inf),
    \dur, Pseq([1.5, 0.5, 2, 1.5, 0.5, 2], inf),
    \amp, Pseq([0.55, 0.4, 0.5, 0.55, 0.4, 0.5], inf),
    \legato, 0.9
));

// 4. Melody - 8-bar call and answer in Em harmonic minor
~m2 = CCMotif([4, 3, Rest(), 2, 0, Rest(), -1, 0]);
Pdef(\melody).quant = 16;
Pdef(\melody, Pbind(
    \instrument, \cc_pluck,
    \scale, Scale.harmonicMinor,
    \root, 4,
    \octave, 5,
    \degree, Pseq([
        CCPhrase(~m2, [
            \state,
            \state,
            \transpose, 3,
            \invert
        ], 1),
        Pseq([
            ~m2.retrograde,
            ~m2.transpose(5),
            ~m2.transpose(2),
            ~m2.truncate(5).extend([Rest(), Rest(), Rest()])
        ])
    ], inf),
    \dur, 0.5,
    \decay, 3,
    \color, 0.3,
    \amp, 0.35
));

// 5. Grains - granular cloud from a mono copy of the break
g = Buffer.readChannel(s, b.buffer.path, channels: [0]);
Ndef(\grains, {
    var trig = Dust.kr(14);
    var pos = LFNoise1.kr(0.05).range(0.05, 0.95);
    var sig = GrainBuf.ar(2, trig, TRand.kr(0.08, 0.3, trig), g,
        TChoose.kr(trig, [0.25, 0.5, 0.5, 0.75]) * BufRateScale.kr(g),
        pos, 2, TRand.kr(-0.8, 0.8, trig));
    sig = HPF.ar(LPF.ar(sig, 2500), 180);
    sig * 0.35
});

// 6. Effects - delay into reverb on melody, grains straight to reverb
~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.4, \time, 0.4225, \decay, 3);
~cc.fx.route(\melody, \fx_delay);
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.55, \room, 0.9, \damp, 0.7);
~cc.fx.connect(\fx_delay, \fx_reverb);
~cc.fx.route(\grains, \fx_reverb);

// 7. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 8, [\melody, \grains], { |beat|
        if(beat == 0) { b.pattern((0..15)) };
    }],
    [\build, 8, [\melody, \break, \grains], { |beat|
        if(beat == 0) { b.pattern((0..15)) };
    }],
    [\chop1, 8, [\melody, \bass, \break], { |beat|
        if(beat == 0) { b.pattern([0, 0, 3, 2, -7, 6, 5, 4, 0, 1, -2, 3, 8, 9, -14, 15]) };
    }],
    [\halftime, 8, [\bass, \break, \grains], { |beat|
        if(beat == 0) { b.pattern([0, 0, 0, 0, 4, 4, 4, 4, 8, 8, 8, 8, 12, 12, 12, 12], 0.5) };
    }],
    [\chop2, 8, [\melody, \bass, \break], { |beat|
        if(beat == 0) { b.pattern([15, 14, -13, 12, 3, 2, -1, 0, 7, 6, -5, 4, 11, 10, -9, 8]) };
    }],
    [\stutter, 4, [\melody, \break], { |beat|
        if(beat == 0) { b.pattern([0, 0, 0, 1, 4, 4, 4, 5, 0, 0, -7, -7, 14, 14, 15, 15], 2) };
    }],
    [\outro, 4, [\melody, \grains], { |beat|
        if(beat == 0) { b.pattern((0..15)) };
    }]
]);
