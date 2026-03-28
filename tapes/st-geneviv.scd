// St Geneviv - Deep house / EDM jam at 128 BPM in D Dorian
// Dm7 | C/E (alternating, 4 bars each)

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(128);

// 2. Kick - syncopated four-on-the-floor with velocity variation
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 1, 0.75, 0.25, 1, 1, 0.5, 0.5], inf),
    \amp, Pseq([0.85, 0.7, 0.75, 0.5, 0.85, 0.7, 0.6, 0.55], inf),
    \decay, 0.4
);

// 3. Hats - swung 16ths with ghost notes and open hat on "and" of 4
Pdef(\hats, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([0.3, 0.2], inf),
    \amp, Pseq([0.35, 0.12, 0.25, 0.1, 0.3, 0.12, 0.2, 0.1, 0.35, 0.12, 0.25, 0.1, 0.3, 0.12, 0.38, 0.1], inf),
    \decay, Pseq([0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.08, 0.02], inf)
);

// 4. Clap - on 2 and 4 with ghost hits
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([Rest(), Rest(), Rest(), Rest(), 0.55, 0.07, Rest(), 0.05, Rest(), Rest(), 0.06, Rest(), 0.55, 0.07, 0.05, Rest()], inf),
    \decay, 0.12
);

// 5. Open hat - and of 2 and 4
Pdef(\ohat, Pbind(
    \instrument, \cc_openhat, \freq, 48,
    \dur, Pseq([Rest(0.5), 0.5, Rest(1), Rest(0.5), 0.5, Rest(1)], inf),
    \amp, Pseq([0, 0.3, 0, 0, 0.25, 0], inf),
    \decay, 0.15
);

// 6. Bass - acid line in D Dorian, syncopated with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.dorian, \root, 2, \octave, 3,
    \degree, Pseq([0, 0, Rest(), 4, 0, Rest(), 3, 0, 0, 0, Rest(), 4, 0, Rest(), 0, Rest()], inf),
    \dur, 0.25,
    \amp, Pseq([0.55, 0.25, 0, 0.45, 0.5, 0, 0.35, 0.2, 0.55, 0.25, 0, 0.4, 0.5, 0, 0.3, 0], inf),
    \cutoff, Pseq([400, 600, 400, 1800, 500, 400, 1200, 500, 400, 700, 400, 2200, 500, 400, 800, 400], inf),
    \res, 0.35, \decay, 0.18, \sustain, 0.12
);

// 7. Stabs - pluck fifths on offbeats
Pdef(\stabs, Pbind(
    \instrument, \cc_pluck,
    \midinote, Pseq([Rest(), [69, 74], Rest(), Rest(), Rest(), [67, 72], Rest(), Rest()], inf),
    \dur, Pseq([0.75, 0.25, 1.0, 1.0, 0.75, 0.25, 1.0, 3.0], inf),
    \amp, Pseq([0, 0.45, 0, 0, 0, 0.4, 0, 0], inf),
    \sustain, 0.4, \decay, 0.5, \release, 0.6
);

// 8. Pad - warm evolving pad crossfading Dm7 and C/E
Ndef(\pad, {
    var switch = LFPulse.kr(0.125).lag(2);
    var freqs1 = [50, 62, 65, 72].midicps;
    var freqs2 = [50, 60, 64, 71].midicps;
    var freq = (freqs1 * switch) + (freqs2 * (1 - switch));
    var sig = Mix([
        SinOsc.ar(freq * [1, 1.003], 0, 0.12),
        Saw.ar(freq * 2.001 * [0.999, 1.001], 0.05)
    ]);
    sig = RLPF.ar(sig, LFTri.kr(0.07).range(900, 2800), 0.45);
    sig * \amp.kr(0.35)
});

// 9. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.4, \room, 0.7, \damp, 0.5);
~cc.fx.route(\clap, \fx_reverb);

~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.3, \time, 0.375, \fb, 0.4);
~cc.fx.route(\hats, \fx_delay);

~cc.fx.load(\pingpong);
~cc.fx.set(\fx_pingpong, \mix, 0.3, \time, 0.375, \fb, 0.45);
~cc.fx.route(\stabs, \fx_pingpong);

// 10. Sidechain - kick ducks bass
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);
