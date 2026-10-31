/*
# Downtempo Drift

Brooding downtempo techno jam in E Phrygian at 108 BPM with a hypnotic 2-bar acid bass loop and sparse chord stabs.

## Elements

- **Kick** (cc_kick) - Four on the floor with a ghost 16th at the end of every 5-beat phrase, giving a subtle lurch
- **Hats** (cc_hihat) - Mechanical 16ths with ghost note dynamics and open/closed decay variation
- **Open hat** (cc_hihat) - Sparse offbeat accents with longer decay, filling the role of a rim click
- **Clap** (cc_clap) - On 2 and 4, dry and tight
- **Bass** (cc_acid) - 2-bar loop in E Phrygian with cutoff sweeps; bar 1 is grounded on the root, bar 2 climbs to the minor 3rd before resolving
- **Stabs** (cc_keys) - Single Em chord [E4, G4, B4] hitting twice across 2 bars, minimal and hypnotic
- **Pad** (Ndef) - Detuned saw drone on E with slow LPF modulation and stereo drift
- **Texture** (Ndef) - Dusty comb-filtered particle clouds, ambient background detail

## Effects

- **Reverb** on pad, texture, hats (mix 0.35, room 0.85, damp 0.6)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005s, release 0.15s -> bass, trigger: kick

## Arrangement (72 bars, ~2:40)

1. **Intro** (8 bars) - Pad and texture
2. **Bass enters** (8 bars) - Acid bass joins
3. **Kick drops** (8 bars) - Kick lands under the bass
4. **Hats** (8 bars) - Hats, then open hat layer in
5. **Full groove** (16 bars) - Clap and stabs complete the stack
6. **Breakdown** (8 bars) - Back to bass, pad, texture
7. **Rebuild** (8 bars) - Kick, hats, open hat, then clap and stabs return two bars at a time
8. **Outro** (8 bars) - Elements peel away, pad and texture last

## Notes

- The kick pattern is 5 beats long (1+1+1+0.75+0.25), creating a subtle polymetric drift against the 4/4 grid
- Bass cutoff peaks on tension notes (flat 2nd, minor 3rd) to emphasize the Phrygian darkness
- During the session, drum samples (DxH kick, HSR hats, SPSV2 clap, JPFM Vine open hat) worked well as replacements for the built-in synths
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(108);

// 2. Kick - four on the floor with ghost 16th at phrase end
Pdef(\kick, Pbind(
    \instrument, \cc_kick,
    \dur, Pseq([1, 1, 1, 0.75, 0.25], inf),
    \amp, Pseq([0.7, 0.65, 0.7, 0.6, 0.5], inf)
));

// 3. Hats - mechanical 16ths with ghost dynamics and open/closed variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat,
    \dur, 0.25,
    \amp, Pseq([0.3, 0.07, 0.18, 0.06, 0.25, 0.06, 0.15, 0.07], inf),
    \decay, Pseq([0.04, 0.02, 0.02, 0.02, 0.03, 0.02, 0.08, 0.02], inf)
));

// 4. Open hat - sparse offbeat accents
Pdef(\ohat, Pbind(
    \instrument, \cc_hihat,
    \dur, 0.5,
    \amp, Pseq([Rest(), 0.18, Rest(), 0.14, Rest(), 0.16, Rest(), Rest()], inf),
    \decay, 0.12
));

// 5. Clap - on 2 and 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap,
    \dur, 2,
    \amp, 0.35
));

// 6. Bass - 2-bar acid loop in E Phrygian with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.phrygian, \root, 4,
    \degree, Pseq([
        0, 0, Rest(), 0, 1, 0, Rest(), -1,
        0, Rest(), 0, 3, 1, 0, -1, 0
    ], inf),
    \octave, 3,
    \dur, 0.25,
    \cutoff, Pseq([
        500, 800, 0, 600, 1400, 700, 0, 300,
        600, 0, 900, 1800, 1200, 600, 400, 500
    ], inf),
    \res, 0.4,
    \amp, Pseq([
        0.5, 0.35, 0, 0.4, 0.5, 0.35, 0, 0.3,
        0.45, 0, 0.4, 0.55, 0.45, 0.35, 0.3, 0.4
    ], inf)
));

// 7. Stabs - sparse Em chord hits across 2 bars
Pdef(\stabs, Pbind(
    \instrument, \cc_keys,
    \midinote, Pseq([
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), [64, 67, 71], Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        [64, 67, 71], Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest(),
        Rest(), Rest(), Rest(), Rest()
    ], inf),
    \dur, 0.25,
    \sustain, 0.12,
    \amp, Pseq([
        0, 0, 0, 0,
        0, 0, 0.3, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0.25, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0
    ], inf)
));

// 8. Pad - slow detuned saw drone
Ndef(\pad, {
    var freq = 164.81;
    var sig = LFSaw.ar([freq, freq * 1.003, freq * 2.001, freq * 0.998], 0, 0.08);
    sig = LPF.ar(sig.sum, SinOsc.kr(0.06).range(350, 1600));
    sig = Pan2.ar(sig, SinOsc.kr(0.03).range(-0.4, 0.4));
    sig * 0.2 * EnvGate.new
});

// 9. Texture - dusty comb-filtered particles
Ndef(\texture, {
    var sig = Dust2.ar([3, 4]) * 0.15;
    sig = CombL.ar(sig, 0.2, SinOsc.kr(0.04).range(0.05, 0.18), 3);
    sig = LPF.ar(sig, 2000);
    sig * 0.15 * EnvGate.new
});

// 10. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.35, \room, 0.85, \damp, 0.6);
~cc.fx.route(\pad, \fx_reverb);
~cc.fx.route(\texture, \fx_reverb);
~cc.fx.route(\hat, \fx_reverb);

// 11. Sidechain - bass ducks under kick
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);

// 12. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    // INTRO - pad + texture
    [\intro, 8, [\pad, \texture]],

    // BASS ENTERS
    [\bass_enters, 8, [\pad, \texture, \bass]],

    // KICK DROPS
    [\kick_drops, 8, [\pad, \texture, \bass, \kick]],

    // HATS + OHAT
    [\hats, 4, [\pad, \texture, \bass, \kick, \hat]],
    [\hats2, 4, [\pad, \texture, \bass, \kick, \hat, \ohat]],

    // FULL GROOVE
    [\full, 16, [\pad, \texture, \bass, \kick, \hat, \ohat, \clap, \stabs]],

    // BREAKDOWN - bass + pad + texture
    [\breakdown, 8, [\pad, \texture, \bass]],

    // REBUILD
    [\rebuild1, 2, [\pad, \texture, \bass, \kick]],
    [\rebuild2, 2, [\pad, \texture, \bass, \kick, \hat]],
    [\rebuild3, 2, [\pad, \texture, \bass, \kick, \hat, \ohat]],
    [\rebuild4, 2, [\pad, \texture, \bass, \kick, \hat, \ohat, \clap, \stabs]],

    // OUTRO
    [\outro1, 2, [\pad, \texture, \bass, \kick, \hat, \ohat]],
    [\outro2, 2, [\pad, \texture, \bass, \kick]],
    [\outro3, 2, [\pad, \texture, \bass]],
    [\outro4, 2, [\pad, \texture]],
]);
