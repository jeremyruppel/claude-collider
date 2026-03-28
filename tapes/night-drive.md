---
name: Night Drive
tempo: 128
key: Em
scd: night-drive.scd
arrangement: night-drive-arrangement.scd
---

> **IMPORTANT:** This tape has a companion script at `night-drive.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Night Drive

EDM jam at 128 BPM in E Dorian with four-on-the-floor drums, gritty distorted percussion, a filter-driven bass with sidechain pumping, lush pad chords, and airy keys lead with octave doubling.

## Chord Progression

Em7 | Em7 | Dm7 | Dm7 (16 bars)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady anchor
- **Hi-hats** (cc_hihat) - Offbeat 16ths with dynamic velocity and open hat accents
- **Clap** (cc_clap) - Backbeat on 2 and 4
- **Rim** (cc_rim) - Syncopated pattern adding swing
- **Shaker** (cc_shaker) - Subtle 16th-note texture layer
- **Tom** (cc_tom) - Fills every couple bars with pitched hits
- **Cowbell** (cc_cowbell) - Sparse rhythmic accents
- **Bass** (cc_bass) - E Dorian roots and 5ths with filter sweeps, sidechained to kick
- **Pad** (cc_pad) - Em7 to Dm7 progression, open Dorian voicings, long attack
- **Lead** (cc_keys) - Airy sparse melody with long decay/release, reverb + delay
- **Lead low** (cc_keys) - Octave-down doubling at 50% volume

## Effects

- **Reverb** on pad, clap, lead, lead_lo (mix 0.35, room 0.7, damp 0.5)
- **Delay** on rim, lead, lead_lo (mix 0.25, time 0.375, feedback 0.4)
- **Distortion** on kick, hat, clap (mix 0.3, drive 0.4)

## Sidechain

- **sc_kick** — threshold 0.1, ratio 6, attack 0.005, release 0.15 -> bass, pad, lead, lead_lo; trigger: kick

## Arrangement (~2:08)

1. **Intro** (8 bars) - Kick + hats emerge
2. **Build 1** (8 bars) - Add clap, rim, shaker
3. **Drop 1** (16 bars) - Add bass, pad, tom, cowbell — full groove
4. **Break** (8 bars) - Strip to pad, rim, shaker only
5. **Build 2** (4 bars) - Kick and hats return
6. **Drop 2** (16 bars) - Everything + lead and lead_lo — peak energy
7. **Outro** (8 bars) - Elements peel away, kick last

## Notes

Hi-hat pattern uses offbeat accents with open hat on beat 3.5. Bass uses root-5th-root-4th movement with per-note filter sweeps. Pad uses stacked Dorian 7th voicings. Lead melody is sparse and airy with lots of space for reverb tails. All drums run through subtle distortion for grit.

## Playback

Execute each block from `night-drive.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns. For the full arrangement, execute `night-drive-arrangement.scd`.
