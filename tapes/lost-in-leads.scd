/*
# Lost in Leads

Driving D Dorian groove at 124 BPM with a syncopated bass, lush pad progression, and a rippling arp that ties it all together.

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
- **Arp** (cc_pluck) - 16th note arpeggiation across octaves 4-5, running up and down chord tones

## Arrangement (66 bars, ~2:08)

1. **Intro** (8 bars) - Kick + hat establish the groove
2. **Build** (8 bars) - Bass + rim layer in
3. **Drop 1** (16 bars) - Full drums + pad, everything cooking
4. **Break** (8 bars) - Drums strip down, arp enters over pad + bass
5. **Drop 2** (16 bars) - Everything back in full force
6. **Outro** (10 bars) - Peel away: arp/pad/snare/ohat first, then bass/rim, then hat, kick last

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
    \drive, 1.3
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
));

// 10. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    // INTRO - kick + hat
    [\intro, 8, [\kick, \hat]],

    // BUILD - +bass +rim
    [\build, 8, [\kick, \hat, \bass, \rim]],

    // DROP 1 - +snare +ohat +pad
    [\drop1, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad]],

    // BREAK - +arp -kick -snare -ohat
    [\break, 8, [\hat, \bass, \rim, \pad, \arp]],

    // DROP 2 - everything
    [\drop2, 16, [\kick, \hat, \bass, \rim, \snare, \ohat, \pad, \arp]],

    // OUTRO - peel away
    [\outro1, 4, [\kick, \hat, \bass, \rim]],
    [\outro2, 4, [\kick, \hat]],
    [\outro3, 2, [\kick]],
]);
