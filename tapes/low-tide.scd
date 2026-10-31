/*
# Low Tide

Slow ambient / downtempo in A minor (aeolian) at 72 BPM — a quartal, filter-swept pad drifting over a syncopated trip-hop beat with a sidechained sub.

## Chord Progression

Am11 – G – Fmaj9(no3) (6-bar cycle: each chord held 2 bars / 8 beats)

Voiced quartally with the common tones D3–G3–C4 held across all three chords; only the bass (A2→G2→F2) and top voice move. This is i–VII–VI in A aeolian.

## Elements

- **Kick** (cc_kick) - Syncopated boom-bap on beat 1, the "and of 2", and beat 4; kept dry and doubles as the sidechain trigger
- **Snare** (cc_snare) - Backbeat on 2 & 4, dry and grounded
- **Hats** (cc_hihat) - Active swung 16ths with accent/ghost dynamics and an open hat on the "and of 4"
- **Rim** (cc_rim) - A single offbeat click on the "and of 3", washed through the delay for dub echoes
- **Sub** (cc_sub) - Pure sub bass anchoring the chord roots (A2, G2, F2) and rising to the 5th for gentle motion; ducked by the kick
- **Pad** (cc_pad) - Slow quartal/sus chords with a 4s swell and long tail, run through an auto-swept low-pass filter, chorus, and reverb

## Effects

- **LPF** on pad (resonance 0.3, cutoff auto-swept 350 Hz ↔ 3.2 kHz by a ~28s LFO)
- **Chorus** on pad, after the LPF (rate 0.25, depth 0.008, mix 0.6)
- **Reverb** — shared hall for pad, hats, and rim/delay tail (mix 0.55, room 0.92, damp 0.35)
- **Delay** on rim, feeding the reverb (time 0.75s, feedback 0.55, mix 0.4)

Signal flow: pad → lpf(auto) → chorus → reverb; rim → delay → reverb; hat → reverb. Kick, snare, and sub stay dry (sub via the sidechain).

## Sidechain

- **subduck** — threshold 0.1, ratio 6, attack 0.01s, release 0.18s → sub, trigger: kick

## Arrangement (26 bars, ~1:27)

1. **Emerge** (4 bars) - Just the filtered pad breathing
2. **Texture** (4 bars) - Pad + hats + rim drift in, still no beat
3. **Groove** (6 bars) - Full beat drops; sub + sidechain pump engage
4. **Break** (3 bars) - Strip to pad + sub + hats; tension and breathing room
5. **Return** (5 bars) - Beat returns full-force
6. **Dissolve** (2 bars) - Drums fall away, pad + sub linger
7. **Fade** (2 bars) - Pad alone; reverb tail rings out

## Notes

- The pad's filter automation is the key texture: `Ndef(\padlfo)` is a slow LFO mapped onto `fx_lpf`'s cutoff via `.map` — preserve this or the pad sounds static.
- The kick does double duty: an audible drum *and* the sidechain key. The sub only enters once the kick does, so the pump is always "explained" by an audible trigger.
- Contrast is the mix strategy: dry, punchy rhythm section (kick/snare) in front; soft, wet, filter-swept pad drifting behind. The sub stays dry and centered for a clean low end.
- Odd section lengths (6/3/5 bars) keep the pad's 6-bar chord cycle landing on shifting boundaries so the loop never feels locked.
*/

// 1. Clear and set tempo
~cc.clear;
~cc.tempo(72);

// 2. Kick - syncopated boom-bap: beat 1, the "and of 2", and beat 4 (dry, also the sidechain trigger)
Pdef(\kick, Pbind(
    \instrument, \cc_kick, \freq, 48,
    \dur, Pseq([1.5, 1.5, 1], inf),
    \amp, Pseq([0.6, 0.5, 0.55], inf)
));

// 3. Snare - backbeat on 2 & 4 (Rest(1) offset, then Pn(2) holds the 2-beat gap)
Pdef(\snare, Pbind(
    \instrument, \cc_snare, \freq, 48,
    \dur, Pseq([Rest(1), Pn(2)]),
    \amp, 0.45
));

// 4. Hats - active swung 16ths, accent pattern with an open hat on the "and of 4"
Pdef(\hat, Pbind(
    \instrument, \cc_hihat, \freq, 48,
    \dur, Pseq([0.15, 0.1], inf),
    \amp, Pseq([0.3, 0.1, 0.18, 0.1, 0.28, 0.1, 0.18, 0.12,
                0.3, 0.1, 0.18, 0.1, 0.28, 0.1, 0.35, 0.14], inf),
    \decay, Pseq([0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02,
                  0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.09, 0.02], inf)
));

// 5. Rim - single offbeat click on the "and of 3", washed through the delay
Pdef(\rim, Pbind(
    \instrument, \cc_rim, \freq, 48,
    \dur, Pseq([Rest(2.5), 0.5, Rest(1)], inf),
    \amp, 0.14
));

// 6. Sub - anchors the chord roots (A2, G2, F2), rising to the 5th; ducked by the kick
Pdef(\sub, Pbind(
    \instrument, \cc_sub,
    \midinote, Pseq([
        45, Rest(1), 45, 52,   // A chord: A2 held, A2, up to E (5th)
        43, Rest(1), 43, 50,   // G chord: G2 held, G2, up to D (5th)
        41, Rest(1), 41, 48    // F chord: F2 held, F2, up to C (5th)
    ], inf),
    \dur, Pseq([3, 1, 2, 2, 3, 1, 2, 2, 3, 1, 2, 2], inf),
    \amp, Pseq([0.38, 0, 0.3, 0.26], inf),
    \decay, 1.2
));

// 7. Pad - slow quartal/sus chords, common tones (D3 G3 C4) held while bass & top voice move
Pdef(\pad, Pbind(
    \instrument, \cc_pad,
    \midinote, Pseq([
        [45, 50, 55, 60, 64],  // A2 D3 G3 C4 E4 - Am11 (quartal)
        [43, 50, 55, 60, 65],  // G2 D3 G3 C4 F4 - bass steps down, F on top
        [41, 48, 55, 60, 64]   // F2 C3 G3 C4 E4 - Fmaj9(no3), airy
    ], inf),
    \dur, 8,
    \sustain, 9,   // overlap chords so they bleed together
    \amp, 0.1,
    \attack, 4,    // slow swell in
    \release, 5    // long tail out
));

// 8. Effects - pad runs lpf(auto) -> chorus -> reverb; rim -> delay -> reverb; hat -> reverb
~cc.fx.load(\reverb);
~cc.fx.set(\fx_reverb, \mix, 0.55, \room, 0.92, \damp, 0.35);

~cc.fx.load(\delay);
~cc.fx.set(\fx_delay, \time, 0.75, \feedback, 0.55, \mix, 0.4);

~cc.fx.load(\chorus);
~cc.fx.set(\fx_chorus, \rate, 0.25, \depth, 0.008, \mix, 0.6);

~cc.fx.load(\lpf);
~cc.fx.set(\fx_lpf, \resonance, 0.3);

// 9. Pad filter automation - slow LFO (~28s cycle) sweeps the pad's cutoff 350 Hz <-> 3.2 kHz
Ndef(\padlfo, { SinOsc.kr(0.035).exprange(350, 3200) });
Ndef(\fx_lpf).map(\cutoff, Ndef(\padlfo));

// 10. Routing - serial pad chain, plus reverb/delay sends
~cc.fx.route(\pad, \fx_lpf);
~cc.fx.connect(\fx_lpf, \fx_chorus);
~cc.fx.connect(\fx_chorus, \fx_reverb);
~cc.fx.route(\rim, \fx_delay);
~cc.fx.connect(\fx_delay, \fx_reverb);
~cc.fx.route(\hat, \fx_reverb);

// 11. Sidechain - sub ducks under the kick for a pumping low end
~cc.fx.sidechain(\subduck, 0.1, 6, 0.01, 0.18);
~cc.fx.route(\sub, \subduck);
~cc.fx.routeTrigger(\kick, \subduck, true);

// 12. Arrangement - play with ~arrangement.play
~arrangement = CCArrangement([
    // EMERGE - just the filtered pad breathing
    [\emerge, 4, [\pad]],

    // TEXTURE - wet percussion drifts in, still no beat
    [\texture, 4, [\pad, \hat, \rim]],

    // GROOVE - full beat drops; sub + sidechain pump engage
    [\groove, 6, [\pad, \hat, \rim, \sub, \kick, \snare]],

    // BREAK - strip the drums; tension and breathing room
    [\break, 3, [\pad, \sub, \hat]],

    // RETURN - beat returns full-force
    [\return, 5, [\pad, \hat, \rim, \sub, \kick, \snare]],

    // DISSOLVE - drums fall away, low end lingers
    [\dissolve, 2, [\pad, \sub]],

    // FADE - end on the pad alone; reverb tail rings out
    [\fade, 2, [\pad]],
]);
