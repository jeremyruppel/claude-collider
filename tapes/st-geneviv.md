---
name: St Geneviv
tempo: 128
key: Dm
scd: st-geneviv.scd
---

> **IMPORTANT:** This tape has a companion script at `st-geneviv.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# St Geneviv

Deep house / EDM jam at 128 BPM in D Dorian with syncopated drums, acid bass, and motif-based lead.

## Chord Progression

Dm7 | C/E (alternating, 4 bars each)

## Elements

- **Kick** (cc_kick) - Syncopated four-on-the-floor with velocity variation, extra hits on beats 3-4
- **Hats** (cc_hihat) - Swung 16ths with ghost notes and open hat accent on the "and" of 4
- **Clap** (cc_clap) - Backbeat on 2 and 4 with ghost hits for human feel
- **Bass** (cc_acid) - Acid line with root-fifth movement, syncopated against the kick, filter sweeps as expression
- **Lead** (cc_lead) - 2-bar syncopated melody with rising motif (0-2-4), stepwise descent response, chromatic approach note
- **Stabs** (cc_pluck) - Sparse offbeat open-fifth chord stabs (A+D, G+C)
- **Pad** (Ndef) - Warm evolving pad crossfading between Dm7 and C/E voicings with slow filter movement

## Effects

- **Reverb** on clap (mix 0.4, room 0.7, damp 0.5)
- **Delay** on hats (mix 0.3, time 0.375, fb 0.4)
- **Chorus** on lead (mix 0.4, depth 0.006, rate 0.8) chained into ping-pong delay
- **Ping-pong delay** on lead (via chorus) and stabs (mix 0.3, time 0.375, fb 0.45)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005s, release 0.15s -> bass, trigger: kick

## Notes

- The pad uses an Ndef with LFPulse crossfading between two chord voicings — it needs to output on bus 0 to be heard
- Lead melody is exactly 2 bars (8 beats) with syncopated durations
- Hats use long-short duration pairs (0.3, 0.2) for swing feel

## Arrangement (~2:15)

1. **Intro** (8 bars) - Kick and hats only, establishes the groove
2. **Build** (8 bars) - Clap and acid bass join, rhythm section locks in
3. **Drop 1** (16 bars) - Lead melody and stabs arrive, full energy
4. **Break** (8 bars) - Strip to hats and pad, tension and space
5. **Build 2** (8 bars) - Kick, clap, bass rebuild under the pad
6. **Drop 2** (16 bars) - Everything together, pad adds warmth under the full mix
7. **Outro** (8 bars) - Elements peel away, kick/hats/pad fade out

## Playback

Execute each block from `st-geneviv.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns. To play the arrangement, execute `st-geneviv-arrangement.scd` after all elements are defined.
