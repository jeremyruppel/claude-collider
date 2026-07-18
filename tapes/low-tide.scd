// Low Tide - slow ambient / downtempo at 72 BPM in A minor (aeolian)
// Quartal/sus pad drifting Am11 -> G -> Fmaj9, sub anchoring the roots, sidechained to the kick

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
~cc.fx.routeTrigger(\kick, \subduck, true)
