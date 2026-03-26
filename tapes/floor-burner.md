---
name: Floor Burner
tempo: 132
key: Gm
scd: floor-burner.scd
---

> **IMPORTANT:** This tape has a companion script at `floor-burner.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Floor Burner

Dancy techno loop at 132 BPM in G Dorian with a four-on-the-floor groove, syncopated bass, and funky lead.

## Chord Progression

Gm - Bb - C - F (i - III - IV - bVII, 4-bar cycle)

## Elements

- **Kick** (cc_kick) - Four on the floor, steady quarter notes
- **Hi-hat** (cc_hihat) - 16th notes with ghost note dynamics and open hat variation on the last 16th
- **Clap** (cc_clap) - Backbeat on 2 and 4
- **Open hat** (cc_openhat) - Offbeat accents peeking through
- **Rim** (cc_rim) - Sparse ghost hits for syncopation
- **Shaker** (cc_shaker) - Steady 16ths for rhythmic glue
- **Bass** (cc_bass) - Syncopated root notes following the 4-bar chord progression, each bar with a different rhythm
- **Lead** (cc_lead) - Funky syncopated pattern in octave 5, derived from the bass rhythm

## Arrangement (~2:04)

1. **Intro** (8 bars) - Kick and hat, raw groove
2. **Build** (8 bars) - Full percussion layers in
3. **Drop 1** (16 bars) - Bass locks in, driving energy
4. **Break** (8 bars) - Strip to hat, rim, shaker — tension
5. **Build 2** (4 bars) - Kick and clap return, rebuild
6. **Drop 2** (16 bars) - Everything + lead, peak energy
7. **Outro** (8 bars) - Kick, hat, bass ride out

## Notes

- No effects or sidechain in this tape
- Lead uses the same syncopated rhythm as the original bass pattern, transposed up to octave 5
- Each bar of the bass has a unique syncopation pattern to keep the 4-bar cycle interesting

## Playback

Execute each block from `floor-burner.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns. For the full arrangement, execute `floor-burner-arrangement.scd`.