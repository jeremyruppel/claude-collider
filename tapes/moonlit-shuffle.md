---
name: Moonlit Shuffle
tempo: 124
key: D Dorian
scd: moonlit-shuffle.scd
---

> **IMPORTANT:** This tape has a companion script at `moonlit-shuffle.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Moonlit Shuffle

A shuffly, broken 2-step / UK-garage groove at 124 BPM in D Dorian — skippy syncopated kick, gliding stereo chord pad, and a syncopated FM sub bass with a call-and-response phrase.

## Chord Progression

Two harmonic "worlds" you can alternate between:

- **A section** — Dm7 · Cmaj7 · G7 · Em7 (2-bar loop, uneven ABAB lengths 3-1-3-1)
- **B section** — Am7 · Bm7♭5 · Cmaj7 · Bm7♭5 (4-bar loop; chord roots A-B-C-B track bassB's 4-5-6-5 walk)

Both are diatonic to D Dorian; the B world lifts the tonal center upward.

## Elements

- **kick** (cc_sampler / `kick_1`) — broken, skippy 2-step kick; hits beat 1 then darts to offbeats, with two different bars. NOT four-on-the-floor.
- **clap** (cc_sampler / `clap_1`) — backbeat on beats 2 & 4, slightly humanized in time; the anchor for the roaming kick.
- **hats** (cc_sampler / `hihat_1`) — swung 16ths, mostly closed with open-hat "lifts" (lower rate) on the on-grid offbeats.
- **bass** (cc_fmbass) — syncopated sub in octave 2 that rests on the downbeats and answers in the gaps. `~bassA` = D-rooted call + retrograde response (4 bars). `~bassB` = 4-5-6-5 walk, roots A→B→C→B (4 bars). Swap via `~mkBass`.
- **lead** (cc_glidechord) — 4-voice paraphonic gliding pad; portamento between chords, stereo-spread voices, slow autopan + filter movement. `~chordsA` / `~chordsB` swap via `~mkChord`.
- **fill_snare** (cc_sampler / `snare_1`) — 16th-note snare roll firing in the last 2 beats of every 8th bar.
- **fill_tom** (cc_tom) — descending tom tumble following the snare roll, resolving on the downbeat.

## Effects

- **reverb** on the pad (`lead`) — restrained: mix 0.25, room 0.7, damp 0.4. Adds space without washing out the articulated chords.

## Notes

- **Custom SynthDef `cc_glidechord`** is defined in the `.scd` (block 3) and must be added before the `lead` Pdef plays. It takes a 4-element `\freqs` array control and glides each voice.
- **A/B swappable parts:** `~mkBass` / `~mkChord` factories guarantee bassA/bassB and chordsA/chordsB are structurally identical (same synth, register, timing), so they can be hot-swapped at phrase boundaries with no jump. bassB and chordsB share roots, so switching sections moves bass and harmony together.
- The drums are sample-based (`cc_sampler`), so they do not use `\freq, 48` (that rule is for the synth drums like `cc_kick`).
- A composed performance of this tape lives in `moonlit-shuffle-arrangement.scd` (intro → groove → A → B → break → drop → outro, ~1:17).

## Playback

Execute each block from `moonlit-shuffle.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.
