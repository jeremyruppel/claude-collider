---
name: Chopped Break
tempo: 142
key: Fm
scd: chopped-break.scd
---

> **IMPORTANT:** This tape has a companion script at `chopped-break.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Chopped Break

Breakbeat chops with acid bass and stabby lead at 142 BPM in F minor.

## Elements

- **Break** (CCBreakbeat + drum_break_142bpm_4_bar) - 4-bar break sliced into 16, rearranged with reversed slices for a choppy feel
- **Bass** (cc_acid) - Syncopated 2-bar bass in Fm, long root notes with double-time hits and octave jump in bar 2
- **Lead** (cc_keys) - Rhodes melody in Fm, 2-bar phrase with dotted eighth delay

## Arrangement (~0:41)

1. **Intro** (8 bars) - Original break in sequence
2. **Build** (8 bars) - Acid bass enters
3. **Drop** (8 bars) - Full stack, chopped break pattern
4. **Break** (4 bars) - Lead melody solo
5. **Drop 2** (8 bars) - Everything back, second chop pattern
6. **Outro** (8 bars) - Back to original sequence

## Notes

- The break uses `drum_break_142bpm_4_bar` from the repo's `samples/` directory
- Delay on keys (dotted eighth, 0.375s, feedback 0.4)
- No chord progression; all parts are riff-based over Fm

## Playback

Execute each block from `chopped-break.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
