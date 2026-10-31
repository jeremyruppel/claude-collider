/*
# St Geneviv

Deep house / EDM jam at 128 BPM in D Dorian with syncopated drums, acid bass, and pluck stabs.

## Chord Progression

Dm7 | C/E (alternating, 4 bars each)

## Elements

- **Kick** (cc_kick) - Syncopated four-on-the-floor with velocity variation, extra hits on beats 3-4
- **Hats** (cc_hihat) - Swung 16ths with ghost notes and open hat accent on the "and" of 4
- **Clap** (cc_clap) - Backbeat on 2 and 4 with ghost hits for human feel
- **Open Hat** (cc_openhat) - And of 2 and 4, complements closed hats
- **Bass** (cc_acid) - Acid line with root-fifth movement, syncopated against the kick, filter sweeps as expression
- **Stabs** (cc_pluck) - Sparse offbeat open-fifth chord stabs (A+D, G+C)
- **Pad** (Ndef) - Warm evolving pad crossfading between Dm7 and C/E voicings with slow filter movement

## Effects

- **Reverb** on clap (mix 0.4, room 0.7, damp 0.5)
- **Delay** on hats (mix 0.3, time 0.375, fb 0.4)
- **Ping-pong delay** on stabs (mix 0.3, time 0.375, fb 0.45)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005s, release 0.15s -> bass, trigger: kick

## Notes

- The pad uses an Ndef with LFPulse crossfading between two chord voicings
- Hats use long-short duration pairs (0.3, 0.2) for swing feel

## Arrangement (72 bars, ~2:15)

1. **Intro** (8 bars) - Kick and hats only, establishes the groove
2. **Build** (8 bars) - Open hat, clap, and acid bass join, rhythm section locks in
3. **Drop 1** (16 bars) - Stabs arrive, full energy
4. **Break** (8 bars) - Strip to hats and pad, tension and space
5. **Build 2** (8 bars) - Kick, open hat, clap, bass rebuild under the pad
6. **Drop 2** (16 bars) - Everything together, pad adds warmth
7. **Outro** (8 bars) - Elements peel away, kick/hats/pad fade out
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(128);

// 2. Kick - syncopated four-on-the-floor with velocity variation
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1, 1, 0.75, 0.25, 1, 1, 0.5, 0.5], inf),
    \amp, Pseq([0.85, 0.7, 0.75, 0.5, 0.85, 0.7, 0.6, 0.55], inf),
    \decay, 0.4
));

// 3. Hats - swung 16ths with ghost notes and open hat on "and" of 4
Pdef(\hats, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([0.3, 0.2], inf),
    \amp, Pseq([0.35, 0.12, 0.25, 0.1, 0.3, 0.12, 0.2, 0.1, 0.35, 0.12, 0.25, 0.1, 0.3, 0.12, 0.38, 0.1], inf),
    \decay, Pseq([0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.03, 0.02, 0.08, 0.02], inf)
));

// 4. Clap - on 2 and 4 with ghost hits
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([Rest(), Rest(), Rest(), Rest(), 0.55, 0.07, Rest(), 0.05, Rest(), Rest(), 0.06, Rest(), 0.55, 0.07, 0.05, Rest()], inf),
    \decay, 0.12
));

// 5. Open hat - and of 2 and 4
Pdef(\ohat, Pbind(
    \instrument, \cc_openhat, \freq, 48,
    \dur, Pseq([Rest(0.5), 0.5, Rest(1), Rest(0.5), 0.5, Rest(1)], inf),
    \amp, Pseq([0, 0.3, 0, 0, 0.25, 0], inf),
    \decay, 0.15
));

// 6. Bass - acid line in D Dorian, syncopated with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.dorian, \root, 2, \octave, 3,
    \degree, Pseq([0, 0, Rest(), 4, 0, Rest(), 3, 0, 0, 0, Rest(), 4, 0, Rest(), 0, Rest()], inf),
    \dur, 0.25,
    \amp, Pseq([0.55, 0.25, 0, 0.45, 0.5, 0, 0.35, 0.2, 0.55, 0.25, 0, 0.4, 0.5, 0, 0.3, 0], inf),
    \cutoff, Pseq([400, 600, 400, 1800, 500, 400, 1200, 500, 400, 700, 400, 2200, 500, 400, 800, 400], inf),
    \res, 0.35, \decay, 0.18, \sustain, 0.12
));

// 7. Stabs - pluck fifths on offbeats
Pdef(\stabs, Pbind(
    \instrument, \cc_pluck,
    \midinote, Pseq([Rest(), [69, 74], Rest(), Rest(), Rest(), [67, 72], Rest(), Rest()], inf),
    \dur, Pseq([0.75, 0.25, 1.0, 1.0, 0.75, 0.25, 1.0, 3.0], inf),
    \amp, Pseq([0, 0.45, 0, 0, 0, 0.4, 0, 0], inf),
    \sustain, 0.4, \decay, 0.5, \release, 0.6
));

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

// 11. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 8, [\kick, \hats]],
    [\build, 8, [\kick, \hats, \ohat, \clap, \bass]],
    [\drop1, 16, [\kick, \hats, \ohat, \clap, \bass, \stabs]],
    [\break, 8, [\hats, \pad]],
    [\build2, 8, [\kick, \hats, \ohat, \clap, \bass, \pad]],
    [\drop2, 16, [\kick, \hats, \ohat, \clap, \bass, \stabs, \pad]],
    [\outro, 8, [\kick, \hats, \pad]]
]);
