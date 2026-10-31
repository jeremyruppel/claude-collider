/*
# Lost in Leads

Driving D Dorian groove at 124 BPM with a syncopated bass, lush pad progression, and a high, loose bell hook that ties it all together.

## Chord Progression

Dm9 → Cmaj7 → Fmaj9 → Dm11 (4-bar cycle)

## Elements

- **Kick** (cc_kick) - Four on the floor, driven
- **Hihat** (cc_hihat) - 16th notes with downbeat accents
- **Snare** (cc_snare) - Backbeat on 2 and 4
- **Open hat** (cc_openhat) - Offbeat accents on the "and" of 2 and 4
- **Rim** (cc_rim) - Syncopated ghost hits for texture
- **Bass** (cc_bass) - Syncopated 16th pattern with root-5th-4th-3rd movement and filter motion
- **Pad** (cc_pad) - Slow 4-bar chord progression with smooth voice leading
- **Arp** (cc_bell) - A leap-up motif in octave 6, reshaped to fit each chord, with a 3+3+2 rhythm and humanized timing, velocity and skipped notes

## Effects

- **Duck** sidechain - The kick pumps the bell (ratio 6, 0.2s release)

## Arrangement (66 bars, ~2:08)

1. **Intro** (8 bars) - Kick + hat establish the groove, fading in from silence
2. **Build** (8 bars) - Bass + rim layer in
3. **Drop 1** (16 bars) - Full drums + pad, everything cooking
4. **Break** (8 bars) - Drums strip down, arp enters over pad + bass
5. **Drop 2** (16 bars) - Everything back in full force
6. **Outro** (10 bars) - Peel away: arp/pad/snare/ohat first, then bass/rim, then hat, kick last, fading out to silence

## Notes

- Session was played through MIDI (bass ch 3, pad ch 2, arp ch 5) with drums on audio outputs 7/8
- Pad voicings use smooth voice leading with common tones between chords
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(124);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \amp, 0.8,
    \dur, 1,
    \drive, 0.4
));

// 3. Hihat - 16th note pattern with accents
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \amp, Pseq([0.3, 0.15, 0.25, 0.15], inf),
    \dur, 0.25,
    \decay, 0.08
));

// 4. Snare - backbeat
Pdef(\snare, Pbind(
    \instrument, \cc_snare,
    \amp, 0.6,
    \dur, Pseq([Rest(1), 1], inf),
    \color, 0.5
));

// 5. Open hat - offbeat accents
Pdef(\ohat, Pbind(
    \instrument, \cc_openhat,
    \amp, Pseq([0, 0, 0.25, 0, 0, 0, 0.2, 0], inf),
    \dur, 0.25,
    \decay, 0.15
));

// 6. Rim - syncopated ghost hits
Pdef(\rim, Pbind(
    \instrument, \cc_rim,
    \amp, Pseq([0, 0, 0, 0.35, 0, 0.25, 0, 0], inf),
    \dur, 0.25,
    \decay, 0.04
));

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
));

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
));

// 9. Arp - bell hook: A leaps up to D, falls back through C to A
~arpMotif = CCMotif([4, 7, 6, Rest(), 4, Rest()]);
Pdef(\arp, Pbind(
    \instrument, \cc_bell,
    \scale, Scale.dorian,
    \root, 2,
    \octave, 6,
    // One bar per chord, the motif reshaped to fit each
    \degree, CCPhrase(~arpMotif, [
        \state,          // Dm9:   A D C . A
        \transpose, -1,  // Cmaj7: G C B . G
        \invert,         // Fmaj9: A E F . A
        \retrograde      // Dm11:  . A . C D A
    ]),
    // A 3+3+2 bar, then a lazier answer
    \dur, Pseq([0.75, 0.75, 0.5, 0.5, 1, 0.5,   0.5, 1, 0.5, 0.75, 0.75, 0.5], inf),
    // Accent shape, a little random wobble, and the odd note left out
    \amp, Pseq([0.5, 0.4, 0.35, 0.3, 0.45, 0.3], inf) * Pwhite(0.8, 1.1) * Pwrand([1, Rest()], [0.92, 0.08], inf),
    \vel, Pwhite(0.45, 0.85),
    // Slightly late, never early, like a player sitting back on the beat
    \lag, Pwhite(0, 0.02),
    \decay, Pwhite(1.4, 2.4)
));

// 10. Effects - the kick ducks the bell
~cc.fx.sidechain(\duck, 0.1, 6, 0.005, 0.2);
~cc.fx.route(\arp, \duck);
~cc.fx.routeTrigger(\kick, \duck, true);

// 11. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    // INTRO - kick + hat, fading in over the 8 bars
    [\intro, 8, [\kick, \hat], CCFade.in],

    // BUILD - +bass +rim
    [\build, 8, [\kick, \hat, \bass, \rim]],

    // DROP 1 - +snare +ohat +pad
    [\drop1, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad]],

    // BREAK - +arp -kick -snare -ohat
    [\break, 8, [\hat, \bass, \rim, \pad, \arp]],

    // DROP 2 - everything
    [\drop2, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad, \arp]],

    // OUTRO - peel away, fading out to silence over the 10 bars
    [\outro1, 4, [\kick, \hat, \bass, \rim], CCFade(1, 0.6, \lin)],
    [\outro2, 4, [\kick, \hat], CCFade(0.6, 0.2, \lin)],
    [\outro3, 2, [\kick], CCFade(0.2, 0, \lin)],
]);
