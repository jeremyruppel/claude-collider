// Downtempo Drift - brooding downtempo techno at 108 BPM in E Phrygian
// Single chord (Em) stabs over a dark acid bass loop

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(108);

// 2. Kick - four on the floor with ghost 16th at phrase end
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 1, 1, 0.75, 0.25], inf),
    \amp, Pseq([0.7, 0.65, 0.7, 0.6, 0.5], inf)
);

// 3. Hats - mechanical 16ths with ghost dynamics and open/closed variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.3, 0.07, 0.18, 0.06, 0.25, 0.06, 0.15, 0.07], inf),
    \decay, Pseq([0.04, 0.02, 0.02, 0.02, 0.03, 0.02, 0.08, 0.02], inf)
);

// 4. Open hat - sparse offbeat accents
Pdef(\ohat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.5,
    \amp, Pseq([Rest(), 0.18, Rest(), 0.14, Rest(), 0.16, Rest(), Rest()], inf),
    \decay, 0.12
);

// 5. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 2,
    \amp, 0.35
);

// 6. Bass - 2-bar acid loop in E Phrygian with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.phrygian, \root, 4,
    \degree, Pseq([
        0, 0, Rest(), 0, 1, 0, Rest(), -1,
        0, Rest(), 0, 3, 1, 0, -1, 0
    ], inf),
    \octave, 3,
    \dur, 0.25,
    \cutoff, Pseq([
        500, 800, 0, 600, 1400, 700, 0, 300,
        600, 0, 900, 1800, 1200, 600, 400, 500
    ], inf),
    \res, 0.4,
    \amp, Pseq([
        0.5, 0.35, 0, 0.4, 0.5, 0.35, 0, 0.3,
        0.45, 0, 0.4, 0.55, 0.45, 0.35, 0.3, 0.4
    ], inf)
);

// 7. Stabs - sparse Em chord hits across 2 bars
Pdef(\stabs, Pbind(
    \instrument, \cc_keys,
    \midinote, Pseq([
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), [64, 67, 71], Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        [64, 67, 71], Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest()
    ], inf),
    \dur, 0.25,
    \sustain, 0.12,
    \amp, Pseq([
        0, 0, 0, 0,
        0, 0, 0.3, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0.25, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0
    ], inf)
);

// 8. Pad - slow detuned saw drone
Ndef(\pad, {
    var freq = 164.81;
    var sig = LFSaw.ar([freq, freq * 1.003, freq * 2.001, freq * 0.998], 0, 0.08);
    sig = LPF.ar(sig.sum, SinOsc.kr(0.06).range(350, 1600));
    sig = Pan2.ar(sig, SinOsc.kr(0.03).range(-0.4, 0.4));
    sig * 0.2 * EnvGate.new
});

// 9. Texture - dusty comb-filtered particles
Ndef(\texture, {
    var sig = Dust2.ar([3, 4]) * 0.15;
    sig = CombL.ar(sig, 0.2, SinOsc.kr(0.04).range(0.05, 0.18), 3);
    sig = LPF.ar(sig, 2000);
    sig * 0.15 * EnvGate.new
});

// 10. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.35, \room, 0.85, \damp, 0.6);
~cc.fx.route(\pad, \fx_reverb);
~cc.fx.route(\texture, \fx_reverb);
~cc.fx.route(\hat, \fx_reverb);

// 11. Sidechain - bass ducks under kick
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true)