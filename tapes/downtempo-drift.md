---
name: Downtempo Drift
tempo: 108
key: E Phrygian
scd: downtempo-drift.scd
arrangement: downtempo-drift-arrangement.scd
---

> **IMPORTANT:** This tape has a companion script at `downtempo-drift.scd`. Execute each block from that file verbatim via `cc_execute` — do not improvise the patterns.

# Downtempo Drift

Brooding downtempo techno jam in E Phrygian at 108 BPM with a hypnotic 2-bar acid bass loop and sparse chord stabs.

## Elements

- **Kick** (cc_kick) - Four on the floor with a ghost 16th at the end of every 5-beat phrase, giving a subtle lurch
- **Hats** (cc_hihat) - Mechanical 16ths with ghost note dynamics and open/closed decay variation
- **Open hat** (cc_hihat) - Sparse offbeat accents with longer decay, filling the role of a rim click
- **Clap** (cc_clap) - On 2 and 4, dry and tight
- **Bass** (cc_acid) - 2-bar loop in E Phrygian with cutoff sweeps; bar 1 is grounded on the root, bar 2 climbs to the minor 3rd before resolving
- **Stabs** (cc_keys) - Single Em chord [E4, G4, B4] hitting twice across 2 bars, minimal and hypnotic
- **Pad** (Ndef) - Detuned saw drone on E with slow LPF modulation and stereo drift
- **Texture** (Ndef) - Dusty comb-filtered particle clouds, ambient background detail

## Effects

- **Reverb** on pad, texture, hats (mix 0.35, room 0.85, damp 0.6)

## Sidechain

- **kick_duck** — threshold 0.08, ratio 6, attack 0.005s, release 0.15s -> bass, trigger: kick

## Notes

- The kick pattern is 5 beats long (1+1+1+0.75+0.25), creating a subtle polymetric drift against the 4/4 grid
- Bass cutoff peaks on tension notes (flat 2nd, minor 3rd) to emphasize the Phrygian darkness
- During the session, drum samples (DxH kick, HSR hats, SPSV2 clap, JPFM Vine open hat) worked well as replacements for the built-in synths

## Playback

Execute each block from `downtempo-drift.scd` verbatim via `cc_execute`. Run blocks in order — do not rewrite or improvise the patterns.