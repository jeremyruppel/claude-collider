/*
# Moonlit Shuffle

A shuffly, broken 2-step / UK-garage groove at 124 BPM in D Dorian — skippy syncopated kick, gliding stereo chord pad, and a syncopated FM sub bass with a call-and-response phrase.

## Chord Progression

Two harmonic "worlds" you can alternate between:

- **A section** — Dm7 · Cmaj7 · G7 · Em7 (2-bar loop, uneven ABAB lengths 3-1-3-1)
- **B section** — Am7 · Bm7♭5 · Cmaj7 · Bm7♭5 (4-bar loop; chord roots A-B-C-B track bassB's 4-5-6-5 walk)

Both are diatonic to D Dorian; the B world lifts the tonal center upward.

## Elements

- **kick** (cc_sampler / `kick_1`) — broken, skippy 2-step kick; hits beat 1 then darts to offbeats, with two different bars. NOT four-on-the-floor.
- **clap** (cc_sampler / `clap_1`) — backbeat on beats 2 & 4, slightly humanized in time; the anchor for the roaming kick.
- **hats** (cc_sampler / `hihat_1`) — swung 16ths, mostly closed with open-hat "lifts" (lower rate) on the on-grid offbeats.
- **bass** (cc_fmbass) — syncopated sub in octave 2 that rests on the downbeats and answers in the gaps. `~bassA` = D-rooted call + retrograde response (4 bars). `~bassB` = 4-5-6-5 walk, roots A→B→C→B (4 bars). Swap via `~mkBass`.
- **lead** (cc_glidechord) — 4-voice paraphonic gliding pad; portamento between chords, stereo-spread voices, slow autopan + filter movement. `~chordsA` / `~chordsB` swap via `~mkChord`.
- **fill_snare** (cc_sampler / `snare_1`) — 16th-note snare roll firing in the last 2 beats of every 8th bar.
- **fill_tom** (cc_tom) — descending tom tumble following the snare roll, resolving on the downbeat.

## Effects

- **reverb** on the pad (`lead`) — restrained: mix 0.25, room 0.7, damp 0.4. Adds space without washing out the articulated chords.

## Arrangement (40 bars, ~1:17)

1. **Intro** (4 bars) - Kick + hats establish the broken beat
2. **Groove** (4 bars) - Clap backbeat locks in
3. **Section A** (8 bars) - Full A world: bass, chords, and fills
4. **Section B** (8 bars) - Swap to the B world for the 4-5-6-5 lift
5. **Break** (4 bars) - Strip to hats + chords; harmony slips home to A
6. **Drop** (8 bars) - Full-energy return in A
7. **Outro** (4 bars) - Strip back to kick + hats and out

## Notes

- **Custom SynthDef `cc_glidechord`** is defined in block 3 and must be added before the `lead` Pdef plays. It takes a 4-element `\freqs` array control and glides each voice.
- **A/B swappable parts:** `~mkBass` / `~mkChord` factories guarantee bassA/bassB and chordsA/chordsB are structurally identical (same synth, register, timing), so they can be hot-swapped at phrase boundaries with no jump. bassB and chordsB share roots, so switching sections moves bass and harmony together. The arrangement swaps worlds via `~setA` / `~setB` section actions.
- The drums are sample-based (`cc_sampler`), so they do not use `\freq, 48` (that rule is for the synth drums like `cc_kick`).
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(124);

// 2. Load repo drum samples
~cc.samples.load(\kick_1);
~cc.samples.load(\clap_1);
~cc.samples.load(\hihat_1);
~cc.samples.load(\snare_1);

// 3. Custom synth: 4-voice paraphonic glide-chord pad (stereo, moving, articulated)
//    Each voice's freq is lagged for portamento; slow LFOs autopan + sweep the filter.
SynthDef(\cc_glidechord, { |out=0, amp=0.2, gate=1, glide=0.05, cutoff=3200, width=0.85, move=0.6|
    var freqs, frq, drift, env, voices, panLfo, base, pans, sig, cut;
    freqs = \freqs.kr([220, 277, 330, 440]);
    drift = 1 + (LFNoise2.kr(0.12 ! 4) * 0.002);         // tiny per-voice pitch drift (chorus shimmer)
    frq = Lag.kr(freqs, glide) * drift;                  // per-voice portamento
    env = EnvGen.kr(Env.adsr(0.06, 0.25, 0.55, 0.35), gate, doneAction: 2);
    voices = (SinOsc.ar(frq) + (SinOsc.ar(frq * 2) * 0.05)) * 0.25;
    base = [-0.7, -0.25, 0.25, 0.7] * width;             // static stereo spread
    panLfo = SinOsc.kr([0.06, 0.08, 0.05, 0.09], [0, 0.5pi, pi, 1.5pi]) * (0.35 * move);
    pans = (base + panLfo).clip(-1, 1);                  // slow independent wandering
    sig = Mix(Pan2.ar(voices, pans));                    // 4 panned voices -> stereo
    cut = cutoff + SinOsc.kr(0.04, 0, cutoff * 0.18);    // slow filter movement
    sig = LPF.ar(sig, cut) * env * amp;
    Out.ar(out, sig);
}).add;

// 4. Part factories + A/B material (bass and chords are drop-in swappable)
~mkBass = { |degrees|
    Pbind(
        \instrument, \cc_fmbass,
        \scale, Scale.dorian, \root, 2, \octave, 2,
        \index, 2.5, \ratio, 1,
        \degree, Pseq(degrees, inf),
        \dur, 0.25,
        \amp, Pwhite(0.5, 0.72)
    )
};
~mkChord = { |chords, durs|
    PmonoArtic(\cc_glidechord,
        \freqs, Pseq(chords, inf),
        \dur, Pseq(durs, inf),
        \legato, 0.6, \glide, 0.05,
        \width, 0.85, \move, 0.6,
        \amp, 0.2, \cutoff, 3200
    )
};

// bassA: D-rooted syncopated call (bars 1-2) + retrograde response (bars 3-4)
~bassA = [
    Rest(), Rest(), 0, Rest(), Rest(), Rest(), 0, 6, Rest(), Rest(), 0, Rest(), 4, Rest(), 2, Rest(),
    Rest(), Rest(), 0, Rest(), Rest(), Rest(), 0, Rest(), Rest(), Rest(), 7, Rest(), 4, Rest(), 2, 6,
    6, 2, Rest(), 4, Rest(), 7, Rest(), Rest(), Rest(), 0, Rest(), Rest(), Rest(), 0, Rest(), Rest(),
    Rest(), 2, Rest(), 4, Rest(), 0, Rest(), Rest(), 6, 0, Rest(), Rest(), Rest(), 0, Rest(), Rest()
];
// bassB: 4-5-6-5 progression (roots A -> B -> C -> B), same syncopated rhythm per bar
~bassB = [
    Rest(), Rest(), 4, Rest(), Rest(), Rest(), 4, 5, Rest(), Rest(), 4, Rest(), Rest(), Rest(), 4, Rest(),
    Rest(), Rest(), 5, Rest(), Rest(), Rest(), 5, 6, Rest(), Rest(), 5, Rest(), Rest(), Rest(), 5, Rest(),
    Rest(), Rest(), 6, Rest(), Rest(), Rest(), 6, 7, Rest(), Rest(), 6, Rest(), Rest(), Rest(), 6, Rest(),
    Rest(), Rest(), 5, Rest(), Rest(), Rest(), 5, 6, Rest(), Rest(), 5, Rest(), Rest(), Rest(), 5, Rest()
];

// chordsA: Dm7 - Cmaj7 - G7 - Em7, spread voicings, uneven ABAB lengths (2 bars)
~chordsA_freqs = [
    [50, 57, 65, 72].midicps,   // Dm7
    [48, 55, 64, 71].midicps,   // Cmaj7
    [55, 59, 65, 74].midicps,   // G7
    [52, 59, 67, 76].midicps    // Em7
];
~chordsA_durs = [3, 1, 3, 1];
// chordsB: Am7 - Bm7b5 - Cmaj7 - Bm7b5 (roots A-B-C-B track bassB, 4 bars)
~chordsB_freqs = [
    [57, 64, 67, 72].midicps,   // Am7    (root A)
    [59, 65, 69, 74].midicps,   // Bm7b5  (root B)
    [60, 67, 71, 76].midicps,   // Cmaj7  (root C)
    [59, 65, 69, 74].midicps    // Bm7b5  (root B)
];
~chordsB_durs = [4, 4, 4, 4];

// 5. Drums - broken 2-step kit (repo samples)
// Kick: skippy/syncopated, NOT four-on-the-floor; two different bars
Pdef(\kick, Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples.at(\kick_1),
    \dur, Pseq([
        0.25, Rest(1.25), 0.25, Rest(0.75), 0.25, Rest(0.75), 0.25, Rest(0.25),
        0.25, Rest(0.5),  0.25, Rest(0.75), 0.25, Rest(0.5),  0.25, Rest(1.25)
    ], inf),
    \amp, Pseq([0.95, 0.82, 0.85, 0.8, 0.95, 0.8, 0.82, 0.85], inf) * Pwhite(0.92, 1.0),
    \rate, Pwhite(0.99, 1.01)
));

// Clap: backbeat on 2 & 4 (anchors the roaming kick)
Pdef(\clap, Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples.at(\clap_1),
    \dur, Pseq([Rest(1), Pn(2, inf)]),
    \amp, Pwhite(0.55, 0.65),
    \timingOffset, Pwhite(-0.005, 0.01)
));

// Hats: swung 16ths, closed with open-hat lifts on the on-grid offbeats
Pdef(\hats, Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples.at(\hihat_1),
    \dur, Pseq([0.30, 0.20], inf),
    \amp,  Pseq([0.26, 0.20, 0.42, 0.24, 0.26, 0.20, 0.44, 0.24], inf) * Pwhite(0.85, 1.0),
    \rate, Pseq([1, 1, 0.76, 1, 1, 1, 0.76, 1], inf) * Pwhite(0.99, 1.01)
));

// 6. Bass - FM sub, syncopated (defaults to bassA; swap to ~bassB for the B section)
Pdef(\bass, ~mkBass.(~bassA));

// 7. Chords - gliding stereo pad (defaults to chordsA; swap to chordsB for the B section)
Pdef(\lead, ~mkChord.(~chordsA_freqs, ~chordsA_durs));

// 8. Drum fill - fires in the last 2 beats of every 8th bar (32-beat loop)
Pdef(\fill_snare, Pbind(
    \instrument, \cc_sampler,
    \buf, ~cc.samples.at(\snare_1),
    \dur, Pseq([Rest(30), 0.25, 0.25, 0.25, 0.25, Rest(1)], inf),
    \amp, Pseq([0, 0.30, 0.40, 0.50, 0.62, 0], inf),
    \rate, Pseq([1, 1.0, 1.05, 1.1, 1.16, 1], inf)
));
Pdef(\fill_snare).quant = 32;
Pdef(\fill_tom, Pbind(
    \instrument, \cc_tom,
    \dur, Pseq([Rest(31), 0.25, 0.25, 0.25, 0.25], inf),
    \freq, Pseq([0, 220, 175, 140, 110], inf),
    \amp, Pseq([0, 0.55, 0.58, 0.62, 0.78], inf),
    \decay, 0.25
));
Pdef(\fill_tom).quant = 32;

// 9. Effects - restrained reverb on the pad
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.25, \room, 0.7, \damp, 0.4);
~cc.fx.route(\lead, \fx_reverb);

// A/B swap helpers - hot-swap the Pdef sources at section boundaries
~setA = { Pdef(\bass, ~mkBass.(~bassA)); Pdef(\lead, ~mkChord.(~chordsA_freqs, ~chordsA_durs)); };
~setB = { Pdef(\bass, ~mkBass.(~bassB)); Pdef(\lead, ~mkChord.(~chordsB_freqs, ~chordsB_durs)); };

// Start from the A world
~setA.value;

// 10. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro,    4, [\kick, \hats]],                                                   // establish the broken beat
    [\groove,   4, [\kick, \hats, \clap]],                                            // backbeat locks in
    [\sectionA, 8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom], ~setA], // full A world
    [\sectionB, 8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom], ~setB], // 4-5-6-5 lift
    [\break,    4, [\hats, \lead], ~setA],                                            // strip down; harmony slips home to A
    [\drop,     8, [\kick, \hats, \clap, \bass, \lead, \fill_snare, \fill_tom]],       // full-energy return in A
    [\outro,    4, [\kick, \hats]],                                                   // strip back to the pulse and out
]);
