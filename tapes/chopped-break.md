---
name: Chopped Break
tempo: 142
key: Em
scd: chopped-break.scd
---

> **IMPORTANT:** This tape has a companion script at `chopped-break.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Chopped Break

Haunting harmonic minor melody over chopped breakbeats at 142 BPM in Em.

## Elements

- **Break** (cc_breakbeat) - 4-bar break sliced into 16, rearranged with reversed slices, half-time repeats, and stutter patterns across sections
- **Bass** (cc_bass) - 2-bar CCMotif riff on root/4th/3rd in Em harmonic minor
- **Melody** (cc_pluck) - Descending pluck motif with CCPhrase development (state, transpose +3, invert), dark and sparse

## Effects

- **Delay** on melody (mix 0.15, dotted eighth 0.375s, decay 2)
- **Reverb** chained from delay (mix 0.55, room 0.9, damp 0.7)

## Arrangement (~3:47)

1. **Intro** (4 bars) - Melody alone with reverb wash
2. **Build** (4 bars) - Straight break enters
3. **Chop 1** (8 bars) - Full stack, rearranged slices with reversed hits
4. **Halftime** (4 bars) - Bass and break only, repeated slices at half rate
5. **Chop 2** (8 bars) - Full stack, fully reversed slice order
6. **Stutter** (4 bars) - Melody and break, stutter pattern at double rate
7. **Outro** (4 bars) - Melody alone

## Notes

- The break uses `drum_break_142bpm_4_bar` from the repo's `samples/` directory
- Delay chains into reverb for extra depth on the pluck melody
- No chord progression; all parts are riff-based over Em harmonic minor
- CCBreakbeat showcase: straight, chopped, halftime, reversed, and stutter patterns

## Playback

Execute each block from `chopped-break.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
