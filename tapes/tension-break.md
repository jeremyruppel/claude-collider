---
name: Tension Break
tempo: 118
key: Gm (dorian)
scd: tension-break.scd
arrangement: tension-break-arrangement.scd
---

> **IMPORTANT:** This tape has a companion script at `tension-break.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Tension Break

Chopped breakbeat track at 118 BPM in G dorian with pluck melody and sub bass.

## Elements

- **breakStraight** (CCBreakbeat / drum_break_118bpm_4_bar) - Original 4-bar break played in order, used for intro and outro
- **breakChopped** (CCBreakbeat / drum_break_118bpm_4_bar) - Rearranged slices with reversed hits for the main sections
- **melody** (cc_pluck) - 2-bar CCMotif phrase: states twice, transposes up, then retrograde resolution
- **bass** (cc_fmsub) - 2-bar syncopated CCMotif phrase with ghost notes (amp variation), states then transposes down. Short sustain, medium release, FM index 1.5

## Arrangement (~65s)

1. **Intro** (4 bars) - Straight break only
2. **Build** (4 bars) - Chopped break + sub bass
3. **Drop** (8 bars) - Chopped break + sub bass + melody
4. **Break** (4 bars) - Melody solo
5. **Drop 2** (8 bars) - Full: chopped break + sub bass + melody
6. **Outro** (4 bars) - Straight break only

## Notes

- Both breakbeat patterns use the repo sample `drum_break_118bpm_4_bar` sliced into 16 segments
- No effects or sidechain — raw and direct

## Playback

Execute each block from `tension-break.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
