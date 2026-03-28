// Night Drive - EDM jam at 128 BPM in E Dorian
// Em7 | Em7 | Dm7 | Dm7

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(128);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, 1,
    \amp, 0.65
);

// 3. Hi-hats - offbeat 16ths with dynamics and open hat accents
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0, 0.3, 0, 0.2, 0, 0.35, 0, 0.15], inf),
    \decay, Pseq([0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.08, 0.02], inf)
);

// 4. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 2,
    \amp, 0.4
);

// 5. Rim - syncopated pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([0.75, 0.25, 0.5, 0.5, 0.75, 0.25, 0.5, 0.5], inf),
    \amp, Pseq([0.2, 0.1, 0.15, 0.12, 0.18, 0.08, 0.15, 0.1], inf)
);

// 6. Shaker - subtle 16th texture
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.05, 0.12, 0.05, 0.08, 0.06], inf)
);

// 7. Tom - fills every couple bars
Pdef(\tom, Pbind(
    \instrument, \cc_tom, \freq, 48,
    \dur, Pseq([Rest(3), 0.25, 0.25, 0.5, Rest(4), Rest(3), 0.5, 0.25, 0.25, Rest(4)], inf),
    \amp, Pseq([0, 0.25, 0.2, 0.3, 0, 0, 0.3, 0.2, 0.25, 0], inf),
    \note, Pseq([0, 7, 5, 0, 0, 0, 0, 5, 7, 0], inf)
);

// 8. Cowbell - sparse accents
Pdef(\cowbell, Pbind(
    \instrument, \cc_cowbell, \freq, 48,
    \dur, Pseq([Rest(1.5), 0.5, Rest(2), Rest(1.5), 0.25, 0.25, Rest(2)], inf),
    \amp, Pseq([0, 0.12, 0, 0, 0.1, 0.08, 0], inf)
);

// 9. Bass - E Dorian with filter movement, sidechained
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.dorian, \root, 4,
    \octave, 3,
    \degree, Pseq([0, 0, Rest(), 0, 4, Rest(), 0, 3], inf),
    \dur, 0.25,
    \amp, Pseq([0.55, 0.3, 0, 0.45, 0.5, 0, 0.55, 0.35], inf),
    \cutoff, Pseq([400, 600, 400, 800, 1200, 400, 500, 900], inf),
    \resonance, 0.3
);

// 10. Pad - Em7 to Dm7 with long attack
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \scale, Scale.dorian, \root, 4,
    \octave, 4,
    \degree, Pseq([
        [0, 2, 4, 6],
        [0, 2, 4, 6],
        [-1, 1, 3, 5],
        [-1, 1, 3, 5]
    ], inf),
    \dur, 4,
    \amp, 0.15,
    \attack, 1.5,
    \release, 3
);

// 11. Lead - airy keys with motif development
Pdef(\lead, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 4,
    \octave, 5,
    \degree, Pseq([
        0, Rest(), 4, Rest(),  Rest(), 2, Rest(), Rest(),
        6, Rest(), Rest(), 5,  Rest(), Rest(), Rest(), Rest(),
        3, Rest(), Rest(), 4,  Rest(), Rest(), 2, Rest(),
        Rest(), 0, Rest(), Rest(), Rest(), Rest(), Rest(), Rest()
    ], inf),
    \dur, 0.25,
    \amp, Pseq([
        0.22, 0, 0.18, 0,  0, 0.15, 0, 0,
        0.2, 0, 0, 0.16,  0, 0, 0, 0,
        0.18, 0, 0, 0.2,  0, 0, 0.15, 0,
        0, 0.22, 0, 0, 0, 0, 0, 0
    ], inf),
    \release, 3,
    \decay, 2
);

// 12. Lead low - octave down at 50% volume
Pdef(\lead_lo, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 4,
    \octave, 4,
    \degree, Pseq([
        0, Rest(), 4, Rest(),  Rest(), 2, Rest(), Rest(),
        6, Rest(), Rest(), 5,  Rest(), Rest(), Rest(), Rest(),
        3, Rest(), Rest(), 4,  Rest(), Rest(), 2, Rest(),
        Rest(), 0, Rest(), Rest(), Rest(), Rest(), Rest(), Rest()
    ], inf),
    \dur, 0.25,
    \amp, Pseq([
        0.11, 0, 0.09, 0,  0, 0.075, 0, 0,
        0.1, 0, 0, 0.08,  0, 0, 0, 0,
        0.09, 0, 0, 0.1,  0, 0, 0.075, 0,
        0, 0.11, 0, 0, 0, 0, 0, 0
    ], inf),
    \release, 3,
    \decay, 2
);

// 13. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.35, \room, 0.7, \damp, 0.5);
~cc.fx.route(\pad, \fx_reverb);
~cc.fx.route(\clap, \fx_reverb);
~cc.fx.route(\lead, \fx_reverb);
~cc.fx.route(\lead_lo, \fx_reverb);

~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.25, \time, 0.375, \feedback, 0.4);
~cc.fx.route(\rim, \fx_delay);
~cc.fx.route(\lead, \fx_delay);
~cc.fx.route(\lead_lo, \fx_delay);

~cc.fx.load(\distortion);
~cc.fx.set(\fx_distortion, \mix, 0.3, \drive, 0.4);
~cc.fx.route(\kick, \fx_distortion);
~cc.fx.route(\hat, \fx_distortion);
~cc.fx.route(\clap, \fx_distortion);

// 14. Sidechain - kick ducks bass, pad, and leads
~cc.fx.sidechain(\sc_kick, 0.1, 6, 0.005, 0.15);
~cc.fx.route(\bass, \sc_kick);
~cc.fx.route(\pad, \sc_kick);
~cc.fx.route(\lead, \sc_kick);
~cc.fx.route(\lead_lo, \sc_kick);
~cc.fx.routeTrigger(\kick, \sc_kick, true);
