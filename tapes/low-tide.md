---
name: Low Tide
tempo: 72
key: A minor
scd: low-tide.scd
arrangement: low-tide-arrangement.scd
---

> **IMPORTANT:** This tape has a companion script at `low-tide.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

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

## Arrangement (~1:27)

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

## Playback

Execute each block from `low-tide.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns. Then execute `low-tide-arrangement.scd` to perform the piece. Elements in `low-tide.scd` are defined without `.play` — the arrangement controls when each enters and exits.
