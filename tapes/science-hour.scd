// Science Hour - retro 70s science TV intro at 112 BPM in C Lydian
// Cmaj7 | D7 | Am7 | Gmaj7

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(112);
~sc = Scale.lydian;
~chords = [[0, 2, 4, 6], [1, 3, 5, 7], [-2, 0, 2, 4], [-3, -1, 1, 3]];
~motif = CCMotif([0, 4, 7, 6])

// 2. Drums - funky kick, backbeat snare, 16th hats
Pdef(\drums, Ppar([
    Pbind(\instrument, \cc_kick, \freq, 48, \dur, Pseq([1, 0.75, 0.25, 1, 1], inf), \amp, 0.7),
    Pbind(\instrument, \cc_snare, \freq, 48, \dur, Pseq([Rest(1), 1, Rest(1), 1], inf), \amp, 0.45),
    Pbind(\instrument, \cc_hihat, \freq, 48, \dur, 0.25, \amp, Pseq([0.15, 0.06, 0.25, 0.06], inf))
]))

// 3. Hats - hats alone, for the build
Pdef(\hats, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.15, 0.06, 0.25, 0.06], inf)
))

// 4. Bass - octave-bouncing 8ths on chord roots
Pdef(\bass, Pbind(
    \instrument, \cc_fmbass, \scale, ~sc, \octave, 3,
    \degree, Pseq(~chords.collect { |c| Pseq(c[0] + [0, 7, 0, 0, 7, 0, 7, 0]) }, inf),
    \dur, 0.5, \legato, 0.6, \index, 2, \amp, 0.35
))

// 5. Drone - filtered saw bed with a wobbling "UFO" sine on top
Ndef(\drone).fadeTime = 4;
Ndef(\drone, {
    var cut = SinOsc.kr(0.08).range(300, 1800);
    var sig = Mix(Saw.ar([65.41, 65.7, 130.6, 196.2], 0.08));
    var ufoF = SinOsc.kr(0.2).range(600, 1300) * SinOsc.kr(5.5, 0, 0.015, 1);
    var ufo = SinOsc.ar(ufoF) * 0.035 * SinOsc.kr(0.13).range(0.2, 1);
    sig = RLPF.ar(sig, cut, 0.35) * 0.25;
    [sig + ufo, sig + DelayN.ar(ufo, 0.03, 0.017)]
})

// 6. Beeps - random computer bleeps
Pdef(\beep, Pbind(
    \instrument, \cc_sine, \scale, ~sc, \octave, 6,
    \degree, Pwhite(0, 11), \dur, 0.25,
    \type, Pwrand([\note, \rest], [0.45, 0.55], inf),
    \sustain, 0.04, \amp, 0.12, \pan, Pwhite(-0.7, 0.7)
))

// 7. Arp - plucked 16th-note chord arpeggio
Pdef(\arp, Pbind(
    \instrument, \cc_pluck, \scale, ~sc, \octave, 5,
    \degree, Pseq(~chords.collect { |c| Pn(Pseq(c ++ [c[0] + 7, c[3], c[2], c[1]]), 2) }, inf),
    \dur, 0.25, \amp, Pseq([0.35, 0.2, 0.25, 0.2], inf), \decay, 0.8, \color, 0.6
))

// 8. Pad - string chords, one per bar
Pdef(\pad, Pbind(
    \instrument, \cc_strings, \scale, ~sc, \octave, 4,
    \degree, Pseq(~chords, inf), \dur, 4, \legato, 1.02,
    \attack, 0.6, \release, 1.5, \amp, 0.12
))

// 9. Fanfare - hopeful motif carried through each chord
Pdef(\fanfare, Pbind(
    \instrument, \cc_lead, \scale, ~sc, \octave, 5,
    \degree, CCPhrase(~motif, [\state, \transpose, 1, \transpose, -2, \transpose, -3]),
    \dur, Pseq([0.5, 0.5, 1, 2], inf), \legato, 0.9,
    \cutoff, 2500, \res, 0.15, \amp, 0.22
))

// 10. Stinger - one-shot title card: bleep run, then big chord + bells + hit
Pdef(\stinger, Ppar([
    Pbind(\instrument, \cc_sine, \scale, ~sc, \octave, 5, \degree, Pseq((0..15)), \dur, 0.125, \sustain, 0.08, \amp, 0.15),
    Pbind(\instrument, \cc_strings, \scale, ~sc, \octave, 4, \degree, Pseq([Rest(), [0, 4, 6, 9, 11, 14]]),
        \dur, Pseq([2, 8]), \legato, 0.8, \attack, 0.05, \release, 3, \amp, 0.13),
    Pbind(\instrument, \cc_bell, \scale, ~sc, \octave, 6, \degree, Pseq([Rest(), 7, 11, 14]),
        \dur, Pseq([2, 0.08, 0.08, 6]), \decay, 6, \amp, 0.18),
    Pbind(\instrument, Pseq([\cc_kick, \cc_kick, \cc_openhat]), \freq, 48, \dur, Pseq([Rest(2), 0, 6]), \amp, 0.7),
    Pbind(\instrument, \cc_fmbass, \octave, 2, \degree, Pseq([Rest(), 0]), \dur, Pseq([2, 6]), \legato, 0.9, \amp, 0.4)
]))

// 11. Effects - delay feeds reverb
~cc.fx.load(\reverb);
~cc.fx.load(\delay);
~cc.fx.set(\fx_reverb, \mix, 0.35, \room, 0.85);
~cc.fx.set(\fx_delay, \time, 0.402, \feedback, 0.35, \mix, 0.3);
~cc.fx.connect(\fx_delay, \fx_reverb);
[\drone, \pad, \arp, \stinger].do { |s| ~cc.fx.route(s, \fx_reverb) };
[\beep, \fanfare].do { |s| ~cc.fx.route(s, \fx_delay) }
