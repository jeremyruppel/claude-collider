// Chopped Break - breakbeat chops with acid bass and keys at 142 BPM in Fm
// Fm (no progression, riff-based)

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(142);

// 2. Break - chopped breakbeat from 4-bar sample
~cc.samples.load(\drum_break_142bpm_4_bar);
b = CCBreakbeat(\break, ~cc.samples.at(\drum_break_142bpm_4_bar), 16).bars(4);
b.pattern((0..15), amp: 0.6);
Pdef(\break).play(quant: 4);

// 3. Bass - syncopated acid bass in Fm with octave jump
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.minor,
    \root, 5,
    \octave, 3,
    \degree, Pseq([
        0, \rest, 0, 0, 2, \rest, 0, \rest,
        0, \rest, 0, 7, \rest, 2, 0, \rest
    ], inf).collect { |n| if(n == \rest) { Rest() } { n } },
    \dur, Pseq([
        1, 0.5, 0.5, 0.5, 0.5, 0.5, 0.25, 0.25,
        0.5, 0.5, 0.5, 1, 0.5, 0.5, 0.25, 0.25
    ], inf),
    \amp, 0.4,
    \cutoff, 800,
    \res, 0.6,
    \decay, 0.2
)).play(quant: 4);

// 4. Lead - Rhodes melody in Fm
Pdef(\lead, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.minor,
    \root, 5,
    \octave, 5,
    \degree, Pseq([
        0, \rest, 2, 4, \rest, 3, 2, \rest,
        0, -1, \rest, 0, 2, \rest, 3, \rest
    ], inf).collect { |n| if(n == \rest) { Rest() } { n } },
    \dur, 0.5,
    \amp, 0.25,
    \brightness, 0.6,
    \release, 0.3
)).play(quant: 4);

// 5. Effects
~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.3, \time, 0.375, \feedback, 0.4);
~cc.fx.route(\lead, \fx_delay);
