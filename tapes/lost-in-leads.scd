// Lost in Leads - D Dorian groove with syncopated bass and arp at 124 BPM
// Dm9 -> Cmaj7 -> Fmaj9 -> Dm11 (4-bar cycle)

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(124);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \freq, 48,
    \amp, 0.8,
    \dur, 1,
    \drive, 1.3
);

// 3. Hihat - 16th note pattern with accents
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \freq, 48,
    \amp, Pseq([0.3, 0.15, 0.25, 0.15], inf),
    \dur, 0.25,
    \decay, 0.08
);

// 4. Snare - backbeat
Pdef(\snare, Pbind(
    \instrument, \cc_snare,
    \freq, 48,
    \amp, 0.6,
    \dur, Pseq([Rest(1), 1], inf),
    \color, 0.5
);

// 5. Open hat - offbeat accents
Pdef(\ohat, Pbind(
    \instrument, \cc_openhat,
    \freq, 48,
    \amp, Pseq([0, 0, 0.25, 0, 0, 0, 0.2, 0], inf),
    \dur, 0.25,
    \decay, 0.15
);

// 6. Rim - syncopated ghost hits
Pdef(\rim, Pbind(
    \instrument, \cc_rim,
    \freq, 48,
    \amp, Pseq([0, 0, 0, 0.35, 0, 0.25, 0, 0], inf),
    \dur, 0.25,
    \decay, 0.04
);

// 7. Bass - syncopated D Dorian with filter movement
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.dorian,
    \root, 2,
    \octave, 3,
    \degree, Pseq([0, Rest(), 4, 0, Rest(), 3, 0, Rest(), 0, 0, Rest(), 4, Rest(), 2, 0, Rest()], inf),
    \amp, Pseq([0.7, 0, 0.4, 0.5, 0, 0.35, 0.6, 0, 0.5, 0.3, 0, 0.45, 0, 0.35, 0.6, 0], inf),
    \dur, 0.25,
    \cutoff, Pseq([400, 400, 800, 600, 400, 500, 400, 400, 500, 400, 400, 1200, 400, 600, 400, 400], inf),
    \sustain, 0.15
);

// 8. Pad - 4-bar D Dorian progression with voice leading
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [50, 57, 62, 69],  // Dm9
        [48, 55, 64, 67],  // Cmaj7
        [53, 60, 65, 72],  // Fmaj9
        [50, 57, 65, 71]   // Dm11
    ], inf),
    \amp, 0.3,
    \dur, 4,
    \sustain, 3.8
);

// 9. Arp - 16th note D Dorian arp across octaves
Pdef(\arp, Pbind(
    \instrument, \cc_pluck,
    \scale, Scale.dorian,
    \root, 2,
    \octave, Pseq([4, 4, 5, 5], inf),
    \degree, Pseq([0, 2, 4, 6, 4, 2, 0, 4, 2, 4, 6, 8, 6, 4, 2, 6], inf),
    \amp, Pseq([0.5, 0.3, 0.4, 0.3, 0.35, 0.25, 0.45, 0.3, 0.5, 0.3, 0.4, 0.3, 0.35, 0.25, 0.45, 0.3], inf),
    \dur, 0.25,
    \sustain, 0.15
);
