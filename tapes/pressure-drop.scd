// Pressure Drop - tech house jam at 126 BPM in F Dorian
// Fm7 | Fm7 | Bbm7 | Bbm7

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(126);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, 1,
    \amp, 0.6
)).play(quant: 4);

// 3. Hi-hats - intricate 16ths with ghost notes and open/closed variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.35, 0.1, 0.22, 0.08, 0.3, 0.12, 0.25, 0.08,
                0.33, 0.1, 0.2, 0.08, 0.3, 0.1, 0.38, 0.12], inf),
    \decay, Pseq([0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.06, 0.02,
                  0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.1, 0.02], inf)
)).play(quant: 4);

// 4. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 2,
    \amp, 0.4
)).play(quant: 4);

// 5. Shaker - subtle 16th texture
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.05, 0.12, 0.05, 0.08, 0.06,
                0.1, 0.06, 0.12, 0.05, 0.1, 0.06, 0.08, 0.05], inf)
)).play(quant: 4);

// 6. Rim - syncopated pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([0.75, 0.5, 0.75, 0.5, 0.5, 1], inf),
    \amp, Pseq([0.2, 0.12, 0.18, 0.15, 0.12, 0.2], inf)
)).play(quant: 4);

// 7. Bass - driving acid sequencer with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.dorian, \root, 5,
    \octave, 3,
    \degree, Pseq([0, 0, Rest(), 0, 3, 0, Rest(), 0,
                   0, 0, 4, 0, Rest(), 0, -1, 0], inf),
    \dur, 0.25,
    \amp, Pseq([0.5, 0.25, 0, 0.4, 0.45, 0.3, 0, 0.35,
                0.5, 0.25, 0.4, 0.3, 0, 0.35, 0.45, 0.25], inf),
    \cutoff, Pseq([400, 500, 400, 800, 1400, 600, 400, 500,
                   400, 600, 1800, 700, 400, 500, 1200, 500], inf),
    \resonance, 0.35,
    \sustain, 0.1
)).play(quant: 4);

// 8. Pad - Fm7 to Bbm7 with open voicings
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [41, 60, 65, 72],  // Fm7: F2 C4 F4 C5
        [41, 60, 65, 72],
        [46, 58, 65, 70],  // Bbm7: Bb2 Bb3 F4 Bb4
        [46, 58, 65, 70]
    ], inf),
    \dur, 4,
    \amp, 0.18,
    \attack, 1.5,
    \sustain, 2,
    \release, 2
)).play(quant: 4);

// 9. Stabs - choppy keys
Pdef(\stab, Pbind(
    \instrument, \cc_keys,
    \midinote, Pseq([
        [65, 68, 72], Rest(), Rest(), [65, 68, 72], Rest(), [63, 68, 72], Rest(), Rest(),
        [65, 70, 73], Rest(), Rest(), [65, 70, 73], Rest(), Rest(), [63, 68, 72], Rest()
    ], inf),
    \dur, 0.25,
    \amp, 0.2,
    \sustain, 0.08
)).play(quant: 4);

// 10. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.45, \room, 0.7, \damp, 0.6);
~cc.fx.route(\pad, \fx_reverb);

~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.3, \time, 0.375, \feedback, 0.35);
~cc.fx.route(\stab, \fx_delay);

// 11. Sidechain - kick ducks the bass
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);
