# Prebuilt Synths

27 synths with `cc_` prefix. Use `~cc.synths.describe` for full param details at runtime.

## Drums & Percussion

| Synth | Type | Description | Key Params |
|---|---|---|---|
| `cc_kick` | one-shot | Punchy kick with pitch sweep | freq(48), decay(300ms) |
| `cc_snare` | one-shot | Sine body + HPF noise | freq(180), decay(200ms) |
| `cc_hihat` | one-shot | Closed hi-hat, HPF noise at 8kHz | decay(50ms) |
| `cc_openhat` | one-shot | Open hi-hat with metallic shimmer | decay(500ms) |
| `cc_clap` | one-shot | Hand clap, two BPF noise layers | decay(150ms) |
| `cc_rim` | one-shot | Rimshot/sidestick, tight BPF noise | freq(1700) |
| `cc_tom` | one-shot | Tom with exponential pitch drop | freq(120), decay(300ms) |
| `cc_cowbell` | one-shot | 808-style, two detuned squares | decay(400ms) |
| `cc_shaker` | one-shot | Shaker/maraca, HPF noise grain | decay(80ms), color(5000) |
| `cc_click` | one-shot | Metronome click, sine + noise | freq(1500) |

## Bass

| Synth | Type | Description | Key Params |
|---|---|---|---|
| `cc_bass` | gate | Sub bass with harmonics (fund + 2nd + 3rd) | freq(55), decay |
| `cc_sub` | gate | Pure sub sine with tanh saturation | freq, decay |
| `cc_acid` | gate | 303-style saw through resonant LPF with filter envelope | cutoff(1000), res(0.3), decay(300ms) |
| `cc_fmbass` | gate | 2-op FM bass, index controls growl | index(3), ratio(1) |
| `cc_reese` | gate | Reese bass, three saws with phasing | detune(0.5), cutoff(800) |

## Melodic / Tonal

| Synth | Type | Description | Key Params |
|---|---|---|---|
| `cc_lead` | gate | Detuned saw lead through resonant LPF | cutoff(4000), res(0.2), att(10ms), rel(300ms) |
| `cc_keys` | gate | Rhodes-style electric piano, FM + bark attack | brightness(0-1) |
| `cc_pluck` | one-shot | Karplus-Strong plucked string | decay(2s), color(0.5) |
| `cc_bell` | one-shot | FM bell, inharmonic ratios for metallic timbre | decay(4s), brightness(3) |
| `cc_sine` | gate | Pure sine tone | freq(440) |

## Pads / Textures

| Synth | Type | Description | Key Params |
|---|---|---|---|
| `cc_pad` | gate | Soft pad, three detuned sines + octave-down saw | attack(500ms), release(1s) |
| `cc_strings` | gate | String ensemble, five detuned saws through LPF | attack(500ms), release(1s), detune |
| `cc_drone` | gate | Evolving saw drone with drift + built-in reverb | spread, movement, filterLo, filterHi, room, mix |
| `cc_noise` | gate | Filtered noise, switchable LPF/HPF/BPF | cutoff(2000), res(0.3), type(0=LPF,1=HPF,2=BPF) |

## Utility / Samplers

| Synth | Type | Description | Key Params |
|---|---|---|---|
| `cc_sampler` | one-shot | Sample playback, auto-frees at buffer end | buf, rate, start(0-1) |
| `cc_breakbeat` | one-shot | Breakbeat slice playback (use with CCBreakbeat) | buf, rate, start, end |
| `cc_grains` | gate | Granular synthesis with adjustable everything | buf, pos, posSpeed, grainSize, grainRate, pitch, spread |
| `cc_riser` | one-shot | Tension riser/sweep, self-timed | duration(4s), startFreq(200), endFreq(4000) |

## Notes

- **one-shot**: plays and self-frees (drums, bells, plucks, risers)
- **gate**: sustains while held, use `\legato` in Pbind to control duration
- All drums should use `\freq, 48` in patterns
- All synths have `out` and `amp` params
