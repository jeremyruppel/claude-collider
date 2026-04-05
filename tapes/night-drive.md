---
name: Night Drive
tempo: 128
key: Em
scd: night-drive.scd
---

> **IMPORTANT:** This tape has a companion script at `night-drive.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Night Drive

EDM jam at 128 BPM in E Dorian with four-on-the-floor drums, gritty distorted percussion, straight bass over Em7/Dm7, lush pad chords, and a descending keys melody with harmony a third above and octave doubling.

## Chord Progression

Em7 | Em7 | Dm7 | Dm7 (4 bars)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady anchor
- **Hi-hats** (cc_hihat) - Offbeat 16ths with dynamic velocity and open hat accents
- **Clap** (cc_clap) - Backbeat on 2 and 4
- **Rim** (cc_rim) - Syncopated pattern adding swing
- **Shaker** (cc_shaker) - Subtle 16th-note texture layer
- **Tom** (cc_tom) - Fills every couple bars with pitched hits
- **Cowbell** (cc_cowbell) - Sparse rhythmic accents
- **Bass** (cc_bass) - Straight eighths on root notes, E for 2 bars then D for 2 bars
- **Pad** (cc_pad) - Em7 to Dm7 progression, open Dorian voicings, long attack
- **Lead** (cc_keys) - Descending CCMotif with CCPhrase development (state, transpose, invert), 4-bar cycle with varied third note on second half
- **Lead low** (cc_keys) - Octave-down doubling at 50% volume
- **Harmony** (cc_keys) - Third above the lead, same CCPhrase development

## Effects

- **Reverb** on clap, lead, lead_lo, harmony (mix 0.35, room 0.7, damp 0.5)
- **Delay** on rim, lead, lead_lo, harmony (mix 0.25, time 0.375, feedback 0.4)
- **Distortion** on kick, hat, clap (mix 0.3, drive 0.4)

## Sidechain

- **sc_kick** — threshold 0.1, ratio 6, attack 0.005, release 0.15 -> bass, pad, lead, lead_lo, harmony; trigger: kick

## Notes

Hi-hat pattern uses offbeat accents with open hat on beat 3.5. Bass is straight eighths following the chord roots. Lead uses two CCMotifs — same descending contour but the third note changes from degree 2 to degree 0 on the second 2-bar half, creating a 4-bar melodic cycle. Harmony is derived via transpose(2) from the lead motifs. All drums run through subtle distortion for grit.

## Playback

Execute each block from `night-drive.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
