---
name: Pressure Drop
tempo: 126
key: Fm
scd: pressure-drop.scd
arrangement: pressure-drop-arrangement.scd
---

> **IMPORTANT:** This tape has a companion script at `pressure-drop.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Pressure Drop

Tech house jam at 126 BPM in F Dorian with intricate drums, a driving acid sequencer bassline, and layered pads and stabs.

## Chord Progression

Fm7 | Fm7 | Bbm7 | Bbm7 (16 bars)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady anchor
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
- **Delay** on stabs (mix 0.3, time 0.375, feedback 0.35)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005, release 0.15 -> bass, trigger: kick

## Arrangement (~2:17)

1. **Intro** (8 bars) - Hats + shaker only
2. **Build** (8 bars) - Add kick, rim
3. **Groove** (8 bars) - Add clap, bass
4. **Peak** (8 bars) - Add pad, stabs — full mix
5. **Break** (4 bars) - Strip to pad + hats + rim
6. **Drop** (12 bars) - Everything back + drone
7. **Break 2** (4 bars) - Pad + stabs + drone
8. **Final Push** (12 bars) - Full energy + drone
9. **Outro** (8 bars) - Elements peel away, drone fades with hats, kick last

## Notes

Hi-hat pattern uses a 16-step sequence with varied decay times for open/closed feel. Bass uses chromatic approach note (-1 degree) at end of the second bar for forward motion. Stab voicings alternate between Fm and Eb/F inversions.

## Playback

Execute each block from `pressure-drop.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
