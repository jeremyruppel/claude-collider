// Pressure Drop - tech house jam at 126 BPM in F Dorian
// Fm7 | Fm7 | Bbm7 | Bbm7

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(126);

// 2. Custom kick SynthDef with scoop EQ (boost lows, cut 400Hz, boost click)
SynthDef(\cc_kick_scoop, {
    |out = 0, freq = 48, amp = 0.8, attack = 0.001, decay = 0.3, drive = 0.4, pan = 0|
    var sig, env, fenv;
    fenv = EnvGen.kr(Env.perc(0.001, decay, 1, -8)) * freq * 4 + freq;
    env = EnvGen.kr(Env.perc(attack, decay), doneAction: 2);
    sig = SinOsc.ar(fenv) * env * amp;
    sig = (sig * (1 + drive)).tanh;
    sig = BLowShelf.ar(sig, 80, 1, 4);
    sig = BPeakEQ.ar(sig, 400, 0.8, -8);
    sig = BHiShelf.ar(sig, 4000, 1, 3);
    sig = Pan2.ar(sig, pan);
    Out.ar(out, sig);
}).add;

// 3. Kick - four on the floor with scoop EQ
Pdef(\kick, Pbind(
    \instrument, \cc_kick_scoop, \freq, 48,
    \dur, 1,
    \amp, 0.8,
    \attack, 0.001,
    \decay, 0.3,
    \drive, 0.4
));

// 4. Hi-hats - intricate 16ths with ghost notes and open/closed variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.35, 0.1, 0.22, 0.08, 0.3, 0.12, 0.25, 0.08,
                0.33, 0.1, 0.2, 0.08, 0.3, 0.1, 0.38, 0.12], inf),
    \decay, Pseq([0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.06, 0.02,
                  0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.1, 0.02], inf)
));

// 5. Clap - on 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, Pseq([Rest(3), 1], inf),
    \amp, 0.4
));

// 6. Shaker - subtle 16th texture
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.05, 0.12, 0.05, 0.08, 0.06,
                0.1, 0.06, 0.12, 0.05, 0.1, 0.06, 0.08, 0.05], inf)
));

// 7. Rim - syncopated pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([0.75, 0.5, 0.75, 0.5, 0.5, 1], inf),
    \amp, Pseq([0.2, 0.12, 0.18, 0.15, 0.12, 0.2], inf)
));

// 8. Bass - driving acid sequencer with filter sweeps
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
));

// 9. Pad - Fm7 to Bbm7 with open voicings
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [41, 60, 65, 72],
        [41, 60, 65, 72],
        [46, 58, 65, 70],
        [46, 58, 65, 70]
    ], inf),
    \dur, 4,
    \amp, 0.18,
    \attack, 1.5,
    \sustain, 2,
    \release, 2
));

// 10. Stabs - choppy keys
Pdef(\stab, Pbind(
    \instrument, \cc_keys,
    \midinote, Pseq([
        [65, 68, 72], Rest(), Rest(), [65, 68, 72], Rest(), [63, 68, 72], Rest(), Rest(),
        [65, 70, 73], Rest(), Rest(), [65, 70, 73], Rest(), Rest(), [63, 68, 72], Rest()
    ], inf),
    \dur, 0.25,
    \amp, 0.2,
    \sustain, 0.08
));

// 11. Drone - sustained F3
Pdef(\drone, Pbind(
    \instrument, \cc_drone,
    \midinote, 53,
    \dur, 8,
    \amp, 0.4,
    \sustain, 7.5
));

// 12. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.45, \room, 0.7, \damp, 0.6);
~cc.fx.route(\pad, \fx_reverb);

~cc.fx.load(\bpf, \stab_bpf);
~cc.fx.set(\stab_bpf, \freq, 1200, \bw, 0.5);
~cc.fx.route(\stab, \stab_bpf);

// 13. Sidechain - kick ducks bass, pad, and drone
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.route(\pad, \kick_duck);
~cc.fx.route(\drone, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);
