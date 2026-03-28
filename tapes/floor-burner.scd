// Floor Burner - dancy techno loop at 132 BPM in G Dorian
// Gm - Bb - C - F (i - III - IV - bVII, 4 bars)

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(132);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, 1,
    \amp, 0.6
);

// 3. Hi-hat - 16ths with ghost notes and open hat variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.05, 0.28, 0.08, 0.22, 0.05, 0.3, 0.08, 0.18], inf),
    \decay, Pseq([0.02, 0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.08], inf)
);

// 4. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, Pseq([Rest(1), 1, Rest(1), 1], inf),
    \amp, 0.4
);

// 5. Open hat - offbeat accents
Pdef(\openhat, Pbind(
    \instrument, \cc_openhat, \freq, 48,
    \dur, Pseq([Rest(0.5), 0.5], inf),
    \amp, Pseq([0, 0.18, 0, 0.15], inf)
);

// 6. Rim - sparse ghost hits
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([
        0, 0, 0.15, 0, 0, 0, 0, 0.12,
        0, 0, 0, 0.15, 0, 0, 0.1, 0
    ], inf)
);

// 7. Shaker - steady 16ths glue
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.06], inf)
);

// 8. Bass - syncopated root notes following chord progression
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.dorian, \root, 7, \octave, 3,
    \degree, Pseq([
        0, Rest(), 0, Rest(), 0, Rest(), Rest(), 0,
        2, Rest(), 2, Rest(), Rest(), 2, Rest(), 2,
        3, Rest(), Rest(), 3, Rest(), 3, 3, Rest(),
        6, Rest(), 6, Rest(), Rest(), Rest(), 6, Rest()
    ], inf),
    \dur, 0.25,
    \amp, Pseq([
        0.5, 0, 0.3, 0, 0.4, 0, 0, 0.35,
        0.5, 0, 0.3, 0, 0, 0.4, 0, 0.3,
        0.5, 0, 0, 0.4, 0, 0.35, 0.3, 0,
        0.5, 0, 0.35, 0, 0, 0, 0.4, 0
    ], inf),
    \sustain, 0.2
);

// 9. Lead - stepwise motif with retrograde variation
Pdef(\lead, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 7, \octave, 5,
    \degree, CCPhrase(CCMotif([0, Rest(), 0, 2, Rest(), Rest(), Rest(), Rest(), 3, Rest(), 2, 0, Rest(), Rest(), Rest(), Rest()]), [\state, \state, \state, \retrograde]),
    \dur, 0.25,
    \amp, 0.25,
    \sustain, 0.1
);

// 10. Effects - chorus and ping-pong delay on lead
~cc.fx.load(\chorus);
~cc.fx.set(\fx_chorus, \mix, 0.4, \rate, 0.3, \depth, 0.005);
~cc.fx.route(\lead, \fx_chorus);

~cc.fx.load(\pingpong);
~cc.fx.set(\fx_pingpong, \mix, 0.3, \time, 1.0, \feedback, 0.4);
~cc.fx.route(\lead, \fx_pingpong);

// 11. Sidechain - kick ducks the lead
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\lead, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true)