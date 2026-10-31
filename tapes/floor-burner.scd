/*
# Floor Burner

Dancy techno loop at 132 BPM in G Dorian with a four-on-the-floor groove, syncopated bass, and funky lead.

## Chord Progression

Gm - Bb - C - F (i - III - IV - bVII, 4-bar cycle)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady quarter notes
- **Hi-hat** (cc_hihat) - 16th notes with ghost note dynamics and open hat variation on the last 16th
- **Clap** (cc_clap) - Backbeat on 2 and 4
- **Open hat** (cc_openhat) - Offbeat accents peeking through
- **Rim** (cc_rim) - Sparse ghost hits for syncopation
- **Shaker** (cc_shaker) - Steady 16ths for rhythmic glue
- **Bass** (cc_bass) - Syncopated root notes following the 4-bar chord progression, each bar with a different rhythm
- **Lead** (cc_keys) - Stepwise motif with retrograde variation, sparse and rhythmic

## Arrangement (68 bars, ~2:04)

1. **Intro** (8 bars) - Kick and hat, raw groove
2. **Build** (8 bars) - Full percussion layers in
3. **Drop 1** (16 bars) - Bass locks in, driving energy
4. **Break** (8 bars) - Strip to hat, rim, shaker — tension
5. **Build 2** (4 bars) - Kick and clap return, rebuild
6. **Drop 2** (16 bars) - Everything + lead, peak energy
7. **Outro** (8 bars) - Kick, hat, bass ride out

## Effects

- **Chorus** on lead (mix 0.4, rate 0.3, depth 0.005)
- **Ping-pong delay** on lead (mix 0.3, time 1.0, feedback 0.4)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005, release 0.15 -> lead, trigger: kick

## Notes

- Lead uses CCMotif/CCPhrase: stepwise motif stated 3x then retrograde on 4th bar
- Each bar of the bass has a unique syncopation pattern to keep the 4-bar cycle interesting
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(132);

// 2. Kick - four on the floor
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \dur, 1,
    \amp, 0.6
));

// 3. Hi-hat - 16ths with ghost notes and open hat variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \dur, 0.25,
    \amp, Pseq([0.05, 0.28, 0.08, 0.22, 0.05, 0.3, 0.08, 0.18], inf),
    \decay, Pseq([0.02, 0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.08], inf)
));

// 4. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap,
    \dur, Pseq([Rest(1), 1, Rest(1), 1], inf),
    \amp, 0.4
));

// 5. Open hat - offbeat accents
Pdef(\openhat, Pbind(
    \instrument, \cc_openhat,
    \dur, Pseq([Rest(0.5), 0.5], inf),
    \amp, Pseq([0, 0.18, 0, 0.15], inf)
));

// 6. Rim - sparse ghost hits
Pdef(\rim, Pbind(
    \instrument, \cc_rim,
    \dur, 0.25,
    \amp, Pseq([
        0, 0, 0.15, 0, 0, 0, 0, 0.12,
        0, 0, 0, 0.15, 0, 0, 0.1, 0
    ], inf)
));

// 7. Shaker - steady 16ths glue
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.06], inf)
));

// 8. Bass - syncopated root notes following chord progression
Pdef(\bass, Pbind(
    \instrument, \cc_bass,
    \scale, Scale.dorian, \root, 7, \octave, 3,
    \degree, Pseq([
        0, Rest(), 0, Rest(), 0, Rest(), Rest(), 0,
        2, Rest(), 2, Rest(), Rest(), 2, Rest(), 2,
        3, Rest(), Rest(), 3, Rest(), 3, 3, Rest(),
        6, Rest(), 6, Rest(), Rest(), Rest(), 6, Rest()
    ], inf),
    \dur, 0.25,
    \amp, Pseq([
        0.5, 0, 0.3, 0, 0.4, 0, 0, 0.35,
        0.5, 0, 0.3, 0, 0, 0.4, 0, 0.3,
        0.5, 0, 0, 0.4, 0, 0.35, 0.3, 0,
        0.5, 0, 0.35, 0, 0, 0, 0.4, 0
    ], inf),
    \sustain, 0.2
));

// 9. Lead - stepwise motif with retrograde variation
Pdef(\lead, Pbind(
    \instrument, \cc_keys,
    \scale, Scale.dorian, \root, 7, \octave, 5,
    \degree, CCPhrase(CCMotif([0, Rest(), 0, 2, Rest(), Rest(), Rest(), Rest(), 3, Rest(), 2, 0, Rest(), Rest(), Rest(), Rest()]), [\state, \state, \state, \retrograde]),
    \dur, 0.25,
    \amp, 0.25,
    \sustain, 0.1
));

// 10. Effects - chorus and ping-pong delay on lead
~cc.fx.load(\chorus);
~cc.fx.set(\fx_chorus, \mix, 0.4, \rate, 0.3, \depth, 0.005);
~cc.fx.route(\lead, \fx_chorus);

~cc.fx.load(\pingpong);
~cc.fx.set(\fx_pingpong, \mix, 0.3, \time, 1.0, \feedback, 0.4);
~cc.fx.route(\lead, \fx_pingpong);

// 11. Sidechain - kick ducks the lead
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\lead, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);

// 12. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 8, [\kick, \hat]],
    [\build, 8, [\kick, \hat, \clap, \openhat, \rim, \shaker]],
    [\drop1, 16, [\kick, \hat, \clap, \openhat, \rim, \shaker, \bass]],
    [\break, 8, [\hat, \rim, \shaker]],
    [\build2, 4, [\kick, \hat, \clap, \rim, \shaker]],
    [\drop2, 16, [\kick, \hat, \clap, \openhat, \rim, \shaker, \bass, \lead]],
    [\outro, 8, [\kick, \hat, \bass]]
]);
