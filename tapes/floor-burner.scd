/*
# Floor Burner

Deep tech house at 122 BPM in F minor: a swung, rolling groove with a pumping offbeat bass, dub Rhodes stabs through echo and reverb, and a sparse bell hook in the breakdown.

## Chord Progression

Fm9 → Dbmaj9 → Bbm9 → C7sus4 (i - VI - iv - V, 1 bar each, 4-bar cycle)

Stabs and pad use rootless upper voicings; the bass supplies the roots (F, Db, Bb, C).

## Elements

- **Kick** (cc_kick) - Four on the floor, deep and round (tune 44)
- **Hat** (cc_hihat) - Swung 16ths, offbeat accents
- **Shaker** (cc_shaker) - Swung 16ths for glue
- **Open hat** (cc_openhat) - Every offbeat
- **Clap** (cc_clap) - 2 and 4, into the reverb
- **Rim** (cc_rim) - Syncopated 2-bar ghost pattern
- **Perc** (cc_fmtom) - Low conga-style FM tom groove
- **Bass** (cc_bass) - Rolling offbeat line following the chord roots, filter swelling over 8 bars
- **Stab** (cc_keys) - Short offbeat Rhodes chords
- **Pad** (cc_pad) - Same voicings as the stabs, soft bed
- **Hook** (cc_bell) - Motif [4, 2, -, 0] reshaped per chord: state, down a step, inverted, reversed

## Effects

- **fx_dub** (delay) - Dotted-8th echo on the stabs (feedback 0.55, mix 0.4) → fx_room
- **fx_pp** (pingpong) - 8th-note ping-pong on the hook → fx_room
- **fx_room** (reverb) - Big room for stabs, pad, clap, hook (mix 0.35, room 0.85)
- **duck** sidechain - Kick pumps the bass (ratio 5, release 0.18)

## Arrangement (96 bars, ~3:09)

1. **Intro** (8) - Kick, hat, shaker, fading in
2. **Groove** (8) - + bass, rim
3. **Build** (8) - + clap, open hat, perc
4. **Drop 1** (16) - + stabs
5. **Break** (8) - Kick and bass out; shaker, pad, stabs, hook; riser over the last 4 bars
6. **Drop 2** (16) - Everything
7. **Drop 3** (8) - Hook and pad out
8. **Outro** (8 + 4 + 4) - Peel away to kick, fading to silence

## Notes

- Every harmonic part (bass, stab, pad, hook) picks its chord from `~chordIdx`, which reads the arrangement's current bar. Parts start and stop at different sections, so per-pattern counters drift out of step; a shared clock keeps them agreeing. All sections are multiples of 4 bars, so each starts on Fm9.
- Swing is a 28 ms `\lag` on every other 16th.
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(122);

// 2. Shared harmony: which chord is this bar? (reads the arrangement's bar)
~chordIdx = {
    var arr = CCArrangement.current;
    if(arr.notNil and: { arr.isPlaying }) {
        (arr.sectionBar - 1) % 4
    } {
        (TempoClock.default.beats / 4).floor.asInteger % 4
    }
};
// Fm9 | Dbmaj9 | Bbm9 | C7sus4 (rootless voicings)
~chords = [[56, 60, 63, 67], [56, 60, 63, 65], [56, 60, 61, 65], [55, 58, 60, 65]];
// Chord roots as F minor degrees: F, Db, Bb, C
~roots = [0, -2, -4, -3];

// Swing: every other 16th sits late
~swing = Pseq([0, 0.028], inf);

// 3. Kick - deep four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \dur, 1, \amp, 0.8,
    \tune, 44, \decay, 0.38, \punch, 0.6, \click, 0.4, \drive, 0.35
));

// 4. Hat - swung 16ths, offbeat accents
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \dur, 0.25, \lag, ~swing,
    \amp, Pseq([0, 0.1, 0.24, 0.12], inf) * Pwhite(0.8, 1.1),
    \decay, Pseq([0.03, 0.03, 0.05, 0.03], inf), \tone, 0.8
));

// 5. Shaker - swung 16ths
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker,
    \dur, 0.25, \lag, ~swing,
    \amp, Pseq([0.08, 0.14, 0.1, 0.16], inf) * Pwhite(0.7, 1.1),
    \decay, 0.06, \color, 6500
));

// 6. Open hat - every offbeat
Pdef(\ohat, Pbind(
    \instrument, \cc_openhat,
    \dur, 0.5, \amp, Pseq([0, 0.17], inf), \decay, 0.22
));

// 7. Clap - 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap,
    \dur, Pseq([Rest(1), 1], inf), \amp, 0.45, \decay, 0.18
));

// 8. Rim - 2-bar syncopated ghost pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim,
    \dur, 0.25, \lag, ~swing, \tune, 1500,
    \amp, Pseq([
        0, 0, 0, 0.22,  0, 0, 0.15, 0,  0, 0.2, 0, 0,  0, 0, 0.25, 0,
        0, 0, 0, 0.22,  0, 0, 0, 0,     0, 0.2, 0, 0.12,  0, 0, 0.25, 0
    ], inf)
));

// 9. Perc - low FM tom, conga-style
Pdef(\perc, Pbind(
    \instrument, \cc_fmtom,
    \dur, 0.25, \lag, ~swing,
    \tune, Pseq([140, 140, 140, 140,  140, 140, 180, 140,  140, 140, 140, 140,  140, 140, 140, 140], inf),
    \amp, Pseq([0, 0, 0, 0,  0, 0, 0.25, 0,  0, 0, 0, 0.2,  0, 0.15, 0, 0], inf),
    \decay, 0.18, \index, 0.8
));

// 10. Bass - rolling offbeat line on the chord roots
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.minor, \root, 5, \octave, 3,
    \mtranspose, Pfunc({ ~roots[~chordIdx.value] }.inEnvir),
    \degree, Pseq([
        Rest(), Rest(), 0, Rest(),  Rest(), Rest(), 0, 7,
        Rest(), Rest(), 0, Rest(),  Rest(), 4, 0, Rest()
    ], inf),
    \amp, Pseq([0, 0, 0.6, 0,  0, 0, 0.5, 0.3,  0, 0, 0.6, 0,  0, 0.35, 0.5, 0], inf),
    \dur, 0.25, \lag, ~swing, \legato, 0.7,
    \cutoff, Pseg(Pseq([350, 900, 350], inf), Pseq([16, 16], inf), \sine),
    \res, 0.5, \fenv, 2.5, \fdecay, 0.12,
    \sub, 0.8, \drive, 0.4, \shape, 0.2
));

// 11. Stab - short offbeat Rhodes chords
Pdef(\stab, Pbind(
    \instrument, \cc_keys,
    \midinote, Pfunc({ ~chords[~chordIdx.value] }.inEnvir),
    \dur, Pseq([Rest(0.75), 0.75, Rest(1), 1.5], inf),
    \amp, Pseq([0.28, 0.2], inf), \vel, Pwhite(0.5, 0.75), \legato, 0.25,
    \decay, 0.9, \brightness, 0.3, \lag, Pwhite(0, 0.015)
));

// 12. Pad - same voicings as the stabs, released before the next chord
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pfunc({ ~chords[~chordIdx.value] }.inEnvir),
    \dur, 4, \legato, 0.85,
    \amp, 0.1, \cutoff, 1400, \attack, 0.8, \release, 0.5, \movement, 0.5
));

// 13. Hook - bell motif reshaped to fit each chord
~hook = CCMotif([4, 2, Rest(), 0]);
~hookBars = [
    ~hook,              // Fm9:    C Ab . F
    ~hook.transpose(-1), // Dbmaj9: Bb G . Eb
    ~hook.invert,       // Bbm9:   C Eb . G
    ~hook.retrograde    // C7sus4: . F Ab C
].collect(_.degrees);
Pdef(\hook, Pbind(
    \instrument, \cc_bell,
    \scale, Scale.minor, \root, 5, \octave, 5,
    \step, Pseq([0, 1, 2, 3], inf),
    \degree, Pfunc({ |e| ~hookBars[~chordIdx.value][e[\step]] }.inEnvir),
    \dur, Pseq([0.75, 0.75, 0.5, 2], inf),
    \amp, Pseq([0.32, 0.25, 0, 0.28], inf) * Pwhite(0.85, 1.05),
    \vel, Pwhite(0.4, 0.7), \decay, 2.5, \metal, 0.1, \lag, Pwhite(0, 0.02)
));

// 14. Effects - dub echo and ping-pong into a shared room
~cc.fx.load(\reverb, \fx_room);
~cc.fx.set(\fx_room, \mix, 0.35, \room, 0.85, \damp, 0.6);

~cc.fx.load(\delay, \fx_dub);
~cc.fx.set(\fx_dub, \time, 60 / 122 * 0.75, \feedback, 0.55, \mix, 0.4);
~cc.fx.route(\stab, \fx_dub);
~cc.fx.connect(\fx_dub, \fx_room);

~cc.fx.load(\pingpong, \fx_pp);
~cc.fx.set(\fx_pp, \time, 60 / 122 * 0.5, \feedback, 0.45, \mix, 0.35);
~cc.fx.route(\hook, \fx_pp);
~cc.fx.connect(\fx_pp, \fx_room);

~cc.fx.route(\pad, \fx_room);
~cc.fx.route(\clap, \fx_room);

// 15. Sidechain - kick pumps the bass
~cc.fx.sidechain(\duck, 0.1, 5, 0.005, 0.18);
~cc.fx.route(\bass, \duck);
~cc.fx.routeTrigger(\kick, \duck, true);

// 16. Arrangement - play with ~arrangement.play
~drums = [\kick, \hat, \shaker, \rim, \clap, \ohat, \perc];
~arrangement = CCArrangement([
    // INTRO - kick, hat, shaker, fading in
    [\intro, 8, [\kick, \hat, \shaker], CCFade.in],

    // GROOVE - +bass +rim
    [\groove, 8, [\kick, \hat, \shaker, \bass, \rim]],

    // BUILD - +clap +ohat +perc
    [\build, 8, [\kick, \hat, \shaker, \bass, \rim, \clap, \ohat, \perc]],

    // DROP 1 - +stabs
    [\drop1, 16, ~drums ++ [\bass, \stab]],

    // BREAK - no kick or bass; pad, stabs, hook; riser over the last 4 bars
    [\break, 8, [\shaker, \pad, \stab, \hook], { |beat|
        if(beat == 16) { ~cc.synths.play(\cc_riser, \duration, 60 / 122 * 16, \amp, 0.2) }
    }.inEnvir],

    // DROP 2 - everything
    [\drop2, 16, ~drums ++ [\bass, \stab, \hook, \pad]],

    // DROP 3 - hook and pad out
    [\drop3, 8, ~drums ++ [\bass, \stab]],

    // OUTRO - peel away, fading to silence
    [\outro1, 8, [\kick, \hat, \shaker, \bass, \rim, \perc], CCFade(1, 0.6, \lin)],
    [\outro2, 4, [\kick, \hat, \shaker], CCFade(0.6, 0.2, \lin)],
    [\outro3, 4, [\kick], CCFade(0.2, 0, \lin)]
]);
