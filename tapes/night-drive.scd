/*
# Night Drive

EDM jam at 128 BPM in E Dorian with four-on-the-floor drums, gritty distorted percussion, straight bass over Em7/Dm7, lush pad chords, and a descending keys melody with harmony a third above and octave doubling.

## Chord Progression

Em7 | Em7 | Dm7 | Dm7 (4 bars)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady anchor
- **Hi-hats** (cc_hihat) - Offbeat 16ths with dynamic velocity and open hat accents
- **Clap** (cc_clap) - Backbeat on 2 and 4
- **Rim** (cc_rim) - Syncopated pattern adding swing
- **Shaker** (cc_shaker) - Subtle 16th-note texture layer
- **Tom** (cc_tom) - Fills every couple bars with pitched hits
- **Cowbell** (cc_cowbell) - Sparse rhythmic accents
- **Bass** (cc_bass) - Straight eighths on root notes, E for 2 bars then D for 2 bars
- **Pad** (cc_pad) - Em7 to Dm7 progression, open Dorian voicings, long attack
- **Lead** (cc_keys) - Descending CCMotif with CCPhrase development (state, transpose, invert), 4-bar cycle with varied third note on second half
- **Lead low** (cc_keys) - Octave-down doubling at 50% volume
- **Harmony** (cc_keys) - Third above the lead, same CCPhrase development

## Effects

- **Reverb** on clap, lead, lead_lo, harmony (mix 0.35, room 0.7, damp 0.5)
- **Delay** on rim, lead, lead_lo, harmony (mix 0.25, time 0.375, feedback 0.4)
- **Distortion** on kick, hat, clap (mix 0.3, drive 0.4)

## Sidechain

- **sc_kick** — threshold 0.1, ratio 6, attack 0.005, release 0.15 -> bass, pad, lead, lead_lo, harmony; trigger: kick

## Arrangement (67 bars, ~2:06)

1. **Intro** (8 bars) - Kick and hats emerge
2. **Build 1** (8 bars) - Clap, rim, and shaker layer in
3. **Drop 1a** (8 bars) - Full groove with bass, pad, tom, cowbell; no melody
4. **Drop 1b** (8 bars) - Harmony sneaks in alone
5. **Break** (8 bars) - Harmony floats over rim, shaker, pad
6. **Build 2** (4 bars) - Kick and hats return
7. **Drop 2a** (8 bars) - Lead and lead low enter without harmony
8. **Drop 2b** (8 bars) - Peak energy, everything
9. **Outro** (7 bars) - Elements peel away, kick last

## Notes

Hi-hat pattern uses offbeat accents with open hat on beat 3.5. Bass is straight eighths following the chord roots. Lead uses two CCMotifs — same descending contour but the third note changes from degree 2 to degree 0 on the second 2-bar half, creating a 4-bar melodic cycle. Harmony is derived via transpose(2) from the lead motifs. All drums run through subtle distortion for grit.
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(128);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \dur, 1,
    \amp, 0.65
));

// 3. Hi-hats - offbeat 16ths with dynamics and open hat accents
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \dur, 0.25,
    \amp, Pseq([0, 0.3, 0, 0.2, 0, 0.35, 0, 0.15], inf),
    \decay, Pseq([0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.08, 0.02], inf)
));

// 4. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap,
    \dur, 2,
    \amp, 0.4
));

// 5. Rim - syncopated pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim,
    \dur, Pseq([0.75, 0.25, 0.5, 0.5, 0.75, 0.25, 0.5, 0.5], inf),
    \amp, Pseq([0.2, 0.1, 0.15, 0.12, 0.18, 0.08, 0.15, 0.1], inf)
));

// 6. Shaker - subtle 16th texture
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.05, 0.12, 0.05, 0.08, 0.06], inf)
));

// 7. Tom - fills every couple bars
Pdef(\tom, Pbind(
    \instrument, \cc_tom,
    \dur, Pseq([Rest(3), 0.25, 0.25, 0.5, Rest(4), Rest(3), 0.5, 0.25, 0.25, Rest(4)], inf),
    \amp, Pseq([0, 0.25, 0.2, 0.3, 0, 0, 0.3, 0.2, 0.25, 0], inf),
    \tune, Pseq([0, 7, 5, 0, 0, 0, 0, 5, 7, 0], inf).collect { |n| 120 * n.midiratio }
));

// 8. Cowbell - sparse accents
Pdef(\cowbell, Pbind(
    \instrument, \cc_cowbell,
    \dur, Pseq([Rest(1.5), 0.5, Rest(2), Rest(1.5), 0.25, 0.25, Rest(2)], inf),
    \amp, Pseq([0, 0.12, 0, 0, 0.1, 0.08, 0], inf)
));

// 9. Bass - straight eighths over Em7/Dm7
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.dorian, \root, 4,
    \octave, 3,
    \degree, Pseq([
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1
    ], inf),
    \dur, 0.5,
    \amp, 0.5,
    \legato, 0.8
));

// 10. Pad - Em7 to Dm7 with long attack
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \scale, Scale.dorian, \root, 4,
    \octave, 4,
    \degree, Pseq([
        [0, 2, 4, 6],
        [0, 2, 4, 6],
        [-1, 1, 3, 5],
        [-1, 1, 3, 5]
    ], inf),
    \dur, 4,
    \amp, 0.15,
    \attack, 1.5,
    \release, 3
));

// 11. Lead - descending motif with CCPhrase development
~m1 = CCMotif([6, 4, 2, Rest()]);
~m2 = CCMotif([6, 4, 0, Rest()]);

Pdef(\lead, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 4,
    \octave, 5,
    \degree, Pseq([
        CCPhrase(~m1, [\state, \state, \transpose, 2, \invert]),
        CCPhrase(~m2, [\state, \state, \transpose, 2, \invert])
    ], inf),
    \dur, 0.5,
    \amp, 0.22,
    \release, 3,
    \decay, 2
));

// 12. Lead low - octave-down doubling
Pdef(\lead_lo, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 4,
    \octave, 4,
    \degree, Pseq([
        CCPhrase(~m1, [\state, \state, \transpose, 2, \invert]),
        CCPhrase(~m2, [\state, \state, \transpose, 2, \invert])
    ], inf),
    \dur, 0.5,
    \amp, 0.11,
    \release, 3,
    \decay, 2
));

// 13. Harmony - third above lead
~h1 = ~m1.transpose(2);
~h2 = ~m2.transpose(2);

Pdef(\harmony, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 4,
    \octave, 5,
    \degree, Pseq([
        CCPhrase(~h1, [\state, \state, \transpose, 2, \invert]),
        CCPhrase(~h2, [\state, \state, \transpose, 2, \invert])
    ], inf),
    \dur, 0.5,
    \amp, 0.18,
    \release, 3,
    \decay, 2
));

// 14. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.35, \room, 0.7, \damp, 0.5);
~cc.fx.route(\clap, \fx_reverb);
~cc.fx.route(\lead, \fx_reverb);
~cc.fx.route(\lead_lo, \fx_reverb);
~cc.fx.route(\harmony, \fx_reverb);

~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \mix, 0.25, \time, 0.375, \feedback, 0.4);
~cc.fx.route(\rim, \fx_delay);
~cc.fx.route(\lead, \fx_delay);
~cc.fx.route(\lead_lo, \fx_delay);
~cc.fx.route(\harmony, \fx_delay);

~cc.fx.load(\distortion);
~cc.fx.set(\fx_distortion, \mix, 0.3, \drive, 0.4);
~cc.fx.route(\kick, \fx_distortion);
~cc.fx.route(\hat, \fx_distortion);
~cc.fx.route(\clap, \fx_distortion);

// 15. Sidechain - kick ducks bass, pad, leads, and harmony
~cc.fx.sidechain(\sc_kick, 0.1, 6, 0.005, 0.15);
~cc.fx.route(\bass, \sc_kick);
~cc.fx.route(\pad, \sc_kick);
~cc.fx.route(\lead, \sc_kick);
~cc.fx.route(\lead_lo, \sc_kick);
~cc.fx.route(\harmony, \sc_kick);
~cc.fx.routeTrigger(\kick, \sc_kick, true);

// 16. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    // INTRO - kick and hats emerge
    [\intro, 8, [\kick, \hat]],

    // BUILD 1 - percs layer in
    [\build1, 8, [\kick, \hat, \clap, \rim, \shaker]],

    // DROP 1a - full groove, no melody
    [\drop1a, 8, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \tom, \cowbell]],

    // DROP 1b - harmony sneaks in alone
    [\drop1b, 8, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \tom, \cowbell, \harmony]],

    // BREAK - harmony floats over stripped-back textures
    [\break, 8, [\rim, \shaker, \pad, \harmony]],

    // BUILD 2 - kick and hats return
    [\build2, 4, [\kick, \hat, \rim, \shaker, \pad]],

    // DROP 2a - lead enters without harmony
    [\drop2a, 8, [\kick, \hat, \clap, \bass, \tom, \cowbell, \lead, \lead_lo]],

    // DROP 2b - peak energy, everything
    [\drop2b, 8, [\kick, \hat, \clap, \bass, \tom, \cowbell, \lead, \lead_lo, \harmony]],

    // OUTRO - elements peel away
    [\outro1, 2, [\kick, \hat, \clap, \rim, \shaker, \bass, \pad, \harmony]],
    [\outro2, 2, [\kick, \hat, \clap, \rim, \shaker]],
    [\outro3, 2, [\kick, \hat]],
    [\outro4, 1, [\kick]],
]);
