---
name: claude-collider
description: Use when live coding music with SuperCollider via ClaudeCollider
MCP server.
---

# SuperCollider Live Coding with ClaudeCollider

This skill enables live music synthesis using SuperCollider via the ClaudeCollider MCP server.
Use this skill to run SuperCollider code, manage synths/effects, control MIDI
devices, handle samples, and record audio.

## Tools Available

| Tool           | Purpose                                         |
| -------------- | ----------------------------------------------- |
| `cc_execute`   | Run SuperCollider code directly                 |
| `cc_status`    | Show status, list synths/effects, debug routing |
| `cc_control`   | Stop, clear, or set tempo                       |
| `cc_fx`        | Load/manage effects and routing                 |
| `cc_midi`      | MIDI device control                             |
| `cc_sample`    | Sample playback and management                  |
| `cc_recording` | Record audio output                             |
| `cc_output`    | Hardware output routing                         |
| `cc_reboot`    | Restart server or list audio devices            |

## Pattern Types

### Pdef - Rhythmic Patterns (drums, sequences)

```supercollider
Pdef(\kick, Pbind(
  \instrument, \cc_kick,
  \freq, 48,              // REQUIRED for drums!
  \dur, 1,
  \amp, 0.7
)).play
```

### Ndef - Continuous Sounds (pads, drones)

```supercollider
Ndef(\pad, {
  var sig = Saw.ar([110, 111], 0.3);
  sig * EnvGen.kr(Env.asr(2, 1, 2), \gate.kr(1))
}).play
```

### Tdef - Timed Tasks (sequencing, automation)

```supercollider
Tdef(\ramp, {
  100.do { |i|
    ~cc.fx.set(\fx_lpf, \cutoff, i.linexp(0, 99, 200, 8000));
    0.05.wait;
  }
}).play
```

## Synths Reference

### Drums (one-shot, need `\freq, 48`)

- `\cc_kick` - Punchy kick with sub bass
- `\cc_snare` - Snappy snare drum
- `\cc_hihat` - Closed hi-hat
- `\cc_openhat` - Open hi-hat with decay
- `\cc_clap` - Layered clap
- `\cc_tom` - Tunable tom
- `\cc_rim` - Rimshot
- `\cc_shaker` - Shaker
- `\cc_cowbell` - Classic cowbell

### Bass (gate-controlled)

- `\cc_bass` - Saw bass with filter
- `\cc_acid` - 303-style acid bass
- `\cc_sub` - Pure sub bass
- `\cc_reese` - Detuned reese bass
- `\cc_fmbass` - FM bass

### Melodic (gate-controlled)

- `\cc_lead` - Mono lead synth
- `\cc_pluck` - Plucked string
- `\cc_bell` - FM bell
- `\cc_keys` - Electric piano
- `\cc_strings` - String ensemble

### Pads (gate-controlled)

- `\cc_pad` - Warm stereo pad

### Utility

- `\cc_sampler` - Sample playback (needs `\buf`)
- `\cc_grains` - Granular synthesis (needs `\buf`)
- `\cc_sine` - Pure sine tone
- `\cc_noise` - Filtered noise

## Effects Routing

```supercollider
// 1. Load an effect
cc_fx({action: "load", name: "reverb"})

// 2. Route a pattern through it
cc_fx({action: "wire", source: "bass", target: "fx_reverb"})

// 3. Adjust parameters
cc_fx({action: "set", slot: "fx_reverb", params: {mix: 0.4, room: 0.8}})

// 4. Chain effects
cc_fx({action: "chain", name: "bass_chain", effects: ["distortion", "delay", "reverb"]})
cc_fx({action: "wire", source: "bass", target: "bass_chain_0_distortion"})
```

### Available Effects

- **Filters**: `lpf`, `hpf`, `bpf`
- **Time**: `reverb`, `delay`, `pingpong`
- **Modulation**: `chorus`, `flanger`, `phaser`, `tremolo`
- **Distortion**: `distortion`, `bitcrush`, `wavefold`
- **Dynamics**: `compressor`, `limiter`, `gate`
- **Stereo**: `widener`, `autopan`

## Samples

```supercollider
// Pre-load before using in patterns
cc_sample({action: "load", name: "kick"})

// One-shot playback
cc_sample({action: "play", name: "kick", rate: 1, amp: 0.8})

// In a pattern
Pdef(\samp, Pbind(
  \instrument, \cc_sampler,
  \buf, ~cc.samples.at(\kick),
  \dur, 1,
  \rate, 1
)).play

// Granular
Synth(\cc_grains, [\buf, ~cc.samples.at(\ambient), \pos, 0.5, \grainSize, 0.1])
```

## Common Patterns

### Basic Beat

```supercollider
Pdef(\beat, Ppar([
  Pbind(\instrument, \cc_kick, \freq, 48, \dur, 1, \amp, 0.8),
  Pbind(\instrument, \cc_hihat, \freq, 48, \dur, 0.25, \amp, 0.3),
  Pbind(\instrument, \cc_snare, \freq, 48, \dur, 2, \amp, 0.6, \lag, 1)
])).play
```

### Melodic Sequence

```supercollider
Pdef(\melody, Pbind(
  \instrument, \cc_pluck,
  \freq, Pseq([60, 63, 67, 70].midicps, inf),
  \dur, Pseq([0.5, 0.5, 0.25, 0.75], inf),
  \amp, 0.5
)).play
```

### Evolving Pad

```supercollider
Ndef(\evolve, {
  var sig = \cc_pad.ar(\freq, 220, \amp, 0.3);
  sig * SinOsc.kr(0.1).range(0.3, 1)
}).play
```

## Key Rules

1. **Drums need `\freq, 48`** - Without it, drums sound wrong (default is middle C)
2. **Use Pdef for rhythms** - Patterns that repeat
3. **Use Ndef for continuous** - Pads, drones, textures
4. **Symbols not strings** - `\kick` not `"kick"`
5. **Semicolons between statements** - No trailing semicolon
6. **NEVER Synth() inside Ndef** - Causes infinite spawning
7. **Load samples first** - Before using in patterns

## Updating Live

```supercollider
// Redefine to update (takes effect on next loop)
Pdef(\kick, Pbind(\instrument, \cc_kick, \freq, 48, \dur, 0.5, \amp, 0.9)).play

// Quantized update (musical timing)
Pdef(\kick).quant = 4;  // Update on next 4-beat boundary

// Crossfade Ndef changes
Ndef(\pad).fadeTime = 2;
Ndef(\pad, { ... new sound ... })
```

## Control Flow

```supercollider
// Stop one pattern
Pdef(\kick).stop

// Stop all sounds
cc_control({action: "stop"})

// Full reset
cc_control({action: "clear"})

// Set tempo
cc_control({action: "tempo", bpm: 128})
```

## Discovery

```supercollider
// List all synths with parameters
cc_status({action: "synths"})

// List all effects with parameters
cc_status({action: "effects"})

// Show current routing
cc_status({action: "routing"})

// Show server status
cc_status()
```
