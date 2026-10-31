/*
# Pressure Drop

Tech house jam at 126 BPM in F Dorian with intricate drums, a punchy scoop-EQ kick, driving acid bass, bandpass-filtered stabs, and layered pads and drone.

## Chord Progression

Fm7 | Fm7 | Bbm7 | Bbm7 (16 bars)

## Elements

- **Kick** (cc_kick_scoop) - Four on the floor, custom SynthDef with scoop EQ: boosted sub at 80Hz, -8dB cut at 400Hz, boosted click at 4kHz, with drive
- **Hi-hats** (cc_hihat) - Intricate 16ths with ghost notes, open hat on the "and" of 4
- **Clap** (cc_clap) - Backbeat on 4
- **Shaker** (cc_shaker) - Subtle 16th-note texture layer
- **Rim** (cc_rim) - Syncopated pattern adding swing and movement
- **Bass** (cc_acid) - Driving 16th-note sequencer with filter sweeps, roots and 4ths in F Dorian
- **Pad** (cc_pad) - Slow Fm7 to Bbm7 progression, open voicings, long attack
- **Stabs** (cc_keys) - Choppy minor chord stabs, rhythmic and punchy
- **Drone** (cc_drone) - Sustained F3, enters at the drop

## Effects

- **Reverb** on pad (mix 0.45, room 0.7, damp 0.6)
- **BPF** on stabs (freq 1200, bw 0.5)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005, release 0.15 -> bass, pad, drone; trigger: kick

## Notes

Hi-hat pattern uses a 16-step sequence with varied decay times for open/closed feel. Bass uses chromatic approach note (-1 degree) at end of the second bar for forward motion. Stab voicings alternate between Fm and Eb/F inversions. The kick uses a custom SynthDef (cc_kick_scoop) that bakes in a 3-band scoop EQ for a punchy, scooped sound. Sidechain ducks bass, pad, and drone against the kick.

## Arrangement (72 bars, ~2:17)

1. **Intro** (8 bars) - Hats + shaker only
2. **Build** (8 bars) - Add kick, rim
3. **Groove** (8 bars) - Add clap, bass
4. **Peak** (8 bars) - Add pad, stabs — full mix
5. **Break** (4 bars) - Strip to pad + hats + rim
6. **Drop** (12 bars) - Everything back + drone
7. **Break 2** (4 bars) - Pad + stabs + drone
8. **Final Push** (12 bars) - Full energy + drone
9. **Outro** (8 bars) - Elements peel away, drone fades with hats, kick last
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(126);

// 2. Custom kick SynthDef with scoop EQ (boost lows, cut 400Hz, boost click)
SynthDef(\cc_kick_scoop, {
    |out = 0, freq = 48, amp = 0.8, attack = 0.001, decay = 0.3, drive = 0.4, pan = 0|
    var sig, env, fenv;
    fenv = EnvGen.kr(Env.perc(0.001, decay, 1, -8)) * freq * 4 + freq;
    env = EnvGen.kr(Env.perc(attack, decay), doneAction: 2);
    sig = SinOsc.ar(fenv) * env * amp;
    sig = (sig * (1 + drive)).tanh;
    sig = BLowShelf.ar(sig, 80, 1, 4);
    sig = BPeakEQ.ar(sig, 400, 0.8, -8);
    sig = BHiShelf.ar(sig, 4000, 1, 3);
    sig = Pan2.ar(sig, pan);
    Out.ar(out, sig);
}).add;

// 3. Kick - four on the floor with scoop EQ
Pdef(\kick, Pbind(
    \instrument, \cc_kick_scoop, \freq, 48,
    \dur, 1,
    \amp, 0.8,
    \attack, 0.001,
    \decay, 0.3,
    \drive, 0.4
));

// 4. Hi-hats - intricate 16ths with ghost notes and open/closed variation
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.35, 0.1, 0.22, 0.08, 0.3, 0.12, 0.25, 0.08,
                0.33, 0.1, 0.2, 0.08, 0.3, 0.1, 0.38, 0.12], inf),
    \decay, Pseq([0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.06, 0.02,
                  0.03, 0.02, 0.02, 0.02, 0.03, 0.02, 0.1, 0.02], inf)
));

// 5. Clap - on 4
Pdef(\clap, Pbind(
    \instrument, \cc_clap, \freq, 48,
    \dur, Pseq([Rest(3), 1], inf),
    \amp, 0.4
));

// 6. Shaker - subtle 16th texture
Pdef(\shaker, Pbind(
    \instrument, \cc_shaker, \freq, 48,
    \dur, 0.25,
    \amp, Pseq([0.12, 0.06, 0.1, 0.05, 0.12, 0.05, 0.08, 0.06,
                0.1, 0.06, 0.12, 0.05, 0.1, 0.06, 0.08, 0.05], inf)
));

// 7. Rim - syncopated pattern
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([0.75, 0.5, 0.75, 0.5, 0.5, 1], inf),
    \amp, Pseq([0.2, 0.12, 0.18, 0.15, 0.12, 0.2], inf)
));

// 8. Bass - driving acid sequencer with filter sweeps
Pdef(\bass, Pbind(
    \instrument, \cc_acid,
    \scale, Scale.dorian, \root, 5,
    \octave, 3,
    \degree, Pseq([0, 0, Rest(), 0, 3, 0, Rest(), 0,
                   0, 0, 4, 0, Rest(), 0, -1, 0], inf),
    \dur, 0.25,
    \amp, Pseq([0.5, 0.25, 0, 0.4, 0.45, 0.3, 0, 0.35,
                0.5, 0.25, 0.4, 0.3, 0, 0.35, 0.45, 0.25], inf),
    \cutoff, Pseq([400, 500, 400, 800, 1400, 600, 400, 500,
                   400, 600, 1800, 700, 400, 500, 1200, 500], inf),
    \resonance, 0.35,
    \sustain, 0.1
));

// 9. Pad - Fm7 to Bbm7 with open voicings
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [41, 60, 65, 72],
        [41, 60, 65, 72],
        [46, 58, 65, 70],
        [46, 58, 65, 70]
    ], inf),
    \dur, 4,
    \amp, 0.18,
    \attack, 1.5,
    \sustain, 2,
    \release, 2
));

// 10. Stabs - choppy keys
Pdef(\stab, Pbind(
    \instrument, \cc_keys,
    \midinote, Pseq([
        [65, 68, 72], Rest(), Rest(), [65, 68, 72], Rest(), [63, 68, 72], Rest(), Rest(),
        [65, 70, 73], Rest(), Rest(), [65, 70, 73], Rest(), Rest(), [63, 68, 72], Rest()
    ], inf),
    \dur, 0.25,
    \amp, 0.2,
    \sustain, 0.08
));

// 11. Drone - sustained F3
Pdef(\drone, Pbind(
    \instrument, \cc_drone,
    \midinote, 53,
    \dur, 8,
    \amp, 0.4,
    \sustain, 7.5
));

// 12. Effects
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.45, \room, 0.7, \damp, 0.6);
~cc.fx.route(\pad, \fx_reverb);

~cc.fx.load(\bpf, \stab_bpf);
~cc.fx.set(\stab_bpf, \freq, 1200, \bw, 0.5);
~cc.fx.route(\stab, \stab_bpf);

// 13. Sidechain - kick ducks bass, pad, and drone
~cc.fx.sidechain(\kick_duck, 0.08, 6, 0.005, 0.15);
~cc.fx.route(\bass, \kick_duck);
~cc.fx.route(\pad, \kick_duck);
~cc.fx.route(\drone, \kick_duck);
~cc.fx.routeTrigger(\kick, \kick_duck, true);

// 14. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    [\intro, 8, [\hat, \shaker]],
    [\build, 8, [\hat, \shaker, \kick, \rim]],
    [\groove, 8, [\hat, \shaker, \kick, \rim, \clap, \bass]],
    [\peak, 8, [\hat, \shaker, \kick, \rim, \clap, \bass, \pad, \stab]],
    [\break1, 4, [\hat, \rim, \pad]],
    [\drop, 12, [\hat, \shaker, \kick, \rim, \clap, \bass, \pad, \stab, \drone]],
    [\break2, 4, [\pad, \stab, \drone]],
    [\final_push, 12, [\hat, \shaker, \kick, \rim, \clap, \bass, \pad, \stab, \drone]],
    [\outro1, 2, [\hat, \shaker, \kick, \rim, \clap, \bass, \drone]],
    [\outro2, 2, [\hat, \shaker, \kick, \rim, \drone]],
    [\outro3, 2, [\hat, \kick, \drone]],
    [\outro4, 2, [\kick]],
]);
