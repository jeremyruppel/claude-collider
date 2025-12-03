# Effects System — Technical Specification

Implementation guide for reusable audio effects in the Vibe Music Server.

## Overview

Effects are Ndef-based audio processors that can be loaded, chained, and modulated. Sources (Pdefs, Ndefs, MIDI-triggered synths) route audio through effect chains.

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────┐
│   Source    │────►│    LPF      │────►│   Reverb    │────►│   Out   │
│  (Pdef/Ndef)│     │  (effect)   │     │  (effect)   │     │         │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────┘
                           ▲                   ▲
                           │                   │
                    ~cutoff bus          ~reverbMix bus
                           ▲                   ▲
                           │                   │
                      MIDI CC 74          MIDI CC 75
```

## Architecture

### Signal Flow

```supercollider
// 1. Sources write to named buses
~drumBus = Bus.audio(s, 2);
~synthBus = Bus.audio(s, 2);

// 2. Effects read from buses, write to other buses or main out
Ndef(\drumFX, { FreeVerb.ar(In.ar(~drumBus, 2), 0.3, 0.8) }).play;
Ndef(\synthFX, { 
  var sig = In.ar(~synthBus, 2);
  sig = LPF.ar(sig, ~cutoff.kr);
  FreeVerb.ar(sig, 0.5, 0.9);
}).play;

// 3. Patterns send to buses
Pdef(\kick, Pbind(\instrument, \kick, \out, ~drumBus, ...));
Pdef(\bass, Pbind(\instrument, \bass, \out, ~synthBus, ...));
```

### Global Effects

For master bus processing:

```supercollider
// Master effects (always last in signal chain)
Ndef(\master, {
  var sig = In.ar(0, 2);  // read from main bus
  sig = Compander.ar(sig, sig, 0.5, 1, 0.5, 0.01, 0.1);
  sig = Limiter.ar(sig, 0.95);
  ReplaceOut.ar(0, sig);  // replace main output
}).play(addAction: \addToTail);
```

## Pre-Built Effects Library

### Filter Effects

#### `lpf` — Low Pass Filter
```supercollider
(
name: \lpf,
func: { |in, cutoff=1000, resonance=0.5|
  RLPF.ar(In.ar(in, 2), cutoff, resonance)
},
params: (
  cutoff: (min: 20, max: 20000, default: 1000, curve: \exp),
  resonance: (min: 0.1, max: 1.0, default: 0.5, curve: \lin)
)
)
```

#### `hpf` — High Pass Filter
```supercollider
(
name: \hpf,
func: { |in, cutoff=200, resonance=0.5|
  RHPF.ar(In.ar(in, 2), cutoff, resonance)
},
params: (
  cutoff: (min: 20, max: 20000, default: 200, curve: \exp),
  resonance: (min: 0.1, max: 1.0, default: 0.5, curve: \lin)
)
)
```

#### `bpf` — Band Pass Filter
```supercollider
(
name: \bpf,
func: { |in, freq=1000, bw=0.5|
  BPF.ar(In.ar(in, 2), freq, bw)
},
params: (
  freq: (min: 20, max: 20000, default: 1000, curve: \exp),
  bw: (min: 0.1, max: 2.0, default: 0.5, curve: \lin)
)
)
```

### Time-Based Effects

#### `reverb` — Reverb
```supercollider
(
name: \reverb,
func: { |in, mix=0.33, room=0.8, damp=0.5|
  var sig = In.ar(in, 2);
  FreeVerb2.ar(sig[0], sig[1], mix, room, damp)
},
params: (
  mix: (min: 0, max: 1, default: 0.33, curve: \lin),
  room: (min: 0, max: 1, default: 0.8, curve: \lin),
  damp: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

#### `delay` — Stereo Delay
```supercollider
(
name: \delay,
func: { |in, time=0.375, feedback=0.5, mix=0.5|
  var sig = In.ar(in, 2);
  var delayed = CombL.ar(sig, 2, time, feedback * 4);
  (sig * (1 - mix)) + (delayed * mix)
},
params: (
  time: (min: 0.01, max: 2.0, default: 0.375, curve: \lin),
  feedback: (min: 0, max: 0.95, default: 0.5, curve: \lin),
  mix: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

#### `pingpong` — Ping Pong Delay
```supercollider
(
name: \pingpong,
func: { |in, time=0.375, feedback=0.5, mix=0.5|
  var sig = In.ar(in, 2);
  var left = CombL.ar(sig[0], 2, time, feedback * 4);
  var right = CombL.ar(sig[1] + (left * 0.5), 2, time, feedback * 4);
  var delayed = [left + (right * 0.5), right];
  (sig * (1 - mix)) + (delayed * mix)
},
params: (
  time: (min: 0.01, max: 2.0, default: 0.375, curve: \lin),
  feedback: (min: 0, max: 0.95, default: 0.5, curve: \lin),
  mix: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

### Modulation Effects

#### `chorus` — Chorus
```supercollider
(
name: \chorus,
func: { |in, rate=0.5, depth=0.005, mix=0.5|
  var sig = In.ar(in, 2);
  var mod = SinOsc.kr([rate, rate * 1.01], [0, 0.5]).range(0.001, depth);
  var wet = DelayL.ar(sig, 0.1, mod);
  (sig * (1 - mix)) + (wet * mix)
},
params: (
  rate: (min: 0.1, max: 10, default: 0.5, curve: \exp),
  depth: (min: 0.001, max: 0.02, default: 0.005, curve: \exp),
  mix: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

#### `flanger` — Flanger
```supercollider
(
name: \flanger,
func: { |in, rate=0.2, depth=0.003, feedback=0.5, mix=0.5|
  var sig = In.ar(in, 2);
  var mod = SinOsc.kr(rate).range(0.0001, depth);
  var wet = DelayL.ar(sig + (LocalIn.ar(2) * feedback), 0.02, mod);
  LocalOut.ar(wet);
  (sig * (1 - mix)) + (wet * mix)
},
params: (
  rate: (min: 0.05, max: 5, default: 0.2, curve: \exp),
  depth: (min: 0.0001, max: 0.01, default: 0.003, curve: \exp),
  feedback: (min: 0, max: 0.95, default: 0.5, curve: \lin),
  mix: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

#### `phaser` — Phaser
```supercollider
(
name: \phaser,
func: { |in, rate=0.3, depth=2, mix=0.5|
  var sig = In.ar(in, 2);
  var mod = SinOsc.kr(rate).range(100, 4000);
  var wet = AllpassL.ar(sig, 0.1, mod.reciprocal, 0);
  (sig * (1 - mix)) + (wet * mix)
},
params: (
  rate: (min: 0.05, max: 5, default: 0.3, curve: \exp),
  depth: (min: 0.5, max: 4, default: 2, curve: \lin),
  mix: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

#### `tremolo` — Tremolo
```supercollider
(
name: \tremolo,
func: { |in, rate=4, depth=0.5|
  var sig = In.ar(in, 2);
  var mod = SinOsc.kr(rate).range(1 - depth, 1);
  sig * mod
},
params: (
  rate: (min: 0.5, max: 20, default: 4, curve: \exp),
  depth: (min: 0, max: 1, default: 0.5, curve: \lin)
)
)
```

### Distortion Effects

#### `distortion` — Soft Clip Distortion
```supercollider
(
name: \distortion,
func: { |in, drive=2, tone=0.5, mix=1|
  var sig = In.ar(in, 2);
  var wet = (sig * drive).tanh;
  wet = LPF.ar(wet, tone.linexp(0, 1, 1000, 12000));
  (sig * (1 - mix)) + (wet * mix)
},
params: (
  drive: (min: 1, max: 20, default: 2, curve: \exp),
  tone: (min: 0, max: 1, default: 0.5, curve: \lin),
  mix: (min: 0, max: 1, default: 1, curve: \lin)
)
)
```

#### `bitcrush` — Bit Crusher
```supercollider
(
name: \bitcrush,
func: { |in, bits=8, rate=44100, mix=1|
  var sig = In.ar(in, 2);
  var crushed = sig.round(2.pow(1 - bits));
  var downsampled = Latch.ar(crushed, Impulse.ar(rate));
  (sig * (1 - mix)) + (downsampled * mix)
},
params: (
  bits: (min: 1, max: 16, default: 8, curve: \lin),
  rate: (min: 1000, max: 44100, default: 44100, curve: \exp),
  mix: (min: 0, max: 1, default: 1, curve: \lin)
)
)
```

#### `wavefold` — Wave Folder
```supercollider
(
name: \wavefold,
func: { |in, amount=2, mix=1|
  var sig = In.ar(in, 2);
  var wet = (sig * amount).fold(-1, 1);
  (sig * (1 - mix)) + (wet * mix)
},
params: (
  amount: (min: 1, max: 10, default: 2, curve: \exp),
  mix: (min: 0, max: 1, default: 1, curve: \lin)
)
)
```

### Dynamics Effects

#### `compressor` — Compressor
```supercollider
(
name: \compressor,
func: { |in, threshold=0.5, ratio=4, attack=0.01, release=0.1, makeup=1|
  var sig = In.ar(in, 2);
  Compander.ar(sig, sig, threshold, 1, ratio.reciprocal, attack, release, makeup)
},
params: (
  threshold: (min: 0.01, max: 1, default: 0.5, curve: \exp),
  ratio: (min: 1, max: 20, default: 4, curve: \exp),
  attack: (min: 0.001, max: 0.5, default: 0.01, curve: \exp),
  release: (min: 0.01, max: 2, default: 0.1, curve: \exp),
  makeup: (min: 0.5, max: 4, default: 1, curve: \exp)
)
)
```

#### `limiter` — Limiter
```supercollider
(
name: \limiter,
func: { |in, level=0.95, lookahead=0.01|
  Limiter.ar(In.ar(in, 2), level, lookahead)
},
params: (
  level: (min: 0.1, max: 1, default: 0.95, curve: \lin),
  lookahead: (min: 0.001, max: 0.1, default: 0.01, curve: \exp)
)
)
```

#### `gate` — Noise Gate
```supercollider
(
name: \gate,
func: { |in, threshold=0.1, attack=0.01, release=0.1|
  var sig = In.ar(in, 2);
  var env = Amplitude.kr(sig).max > threshold;
  sig * Lag.kr(env, attack, release)
},
params: (
  threshold: (min: 0.001, max: 0.5, default: 0.1, curve: \exp),
  attack: (min: 0.001, max: 0.5, default: 0.01, curve: \exp),
  release: (min: 0.01, max: 2, default: 0.1, curve: \exp)
)
)
```

### Stereo Effects

#### `widener` — Stereo Widener
```supercollider
(
name: \widener,
func: { |in, width=1.5|
  var sig = In.ar(in, 2);
  var mid = (sig[0] + sig[1]) * 0.5;
  var side = (sig[0] - sig[1]) * 0.5 * width;
  [mid + side, mid - side]
},
params: (
  width: (min: 0, max: 3, default: 1.5, curve: \lin)
)
)
```

#### `autopan` — Auto Panner
```supercollider
(
name: \autopan,
func: { |in, rate=1, depth=1|
  var sig = In.ar(in, 2);
  var pan = SinOsc.kr(rate).range(-1 * depth, depth);
  Balance2.ar(sig[0], sig[1], pan)
},
params: (
  rate: (min: 0.1, max: 10, default: 1, curve: \exp),
  depth: (min: 0, max: 1, default: 1, curve: \lin)
)
)
```

## MCP Tools

### `fx_load`

Load a pre-built effect into an Ndef.

**Schema:**
```typescript
{
  name: "fx_load",
  description: "Load a pre-built effect. Returns the effect's input bus.",
  inputSchema: {
    type: "object",
    properties: {
      name: {
        type: "string",
        enum: ["lpf", "hpf", "bpf", "reverb", "delay", "pingpong", 
               "chorus", "flanger", "phaser", "tremolo",
               "distortion", "bitcrush", "wavefold",
               "compressor", "limiter", "gate",
               "widener", "autopan"],
        description: "Effect name"
      },
      slot: {
        type: "string",
        description: "Ndef slot name (default: effect name)"
      }
    },
    required: ["name"]
  }
}
```

**Generated SC code:**
```supercollider
// Create input bus for this effect
~fx_reverb_bus = Bus.audio(s, 2);

// Create the effect Ndef
Ndef(\fx_reverb, { |mix=0.33, room=0.8, damp=0.5|
  var sig = In.ar(~fx_reverb_bus, 2);
  FreeVerb2.ar(sig[0], sig[1], mix, room, damp)
}).play;
```

**Return value:**
```json
{
  "slot": "fx_reverb",
  "inputBus": "~fx_reverb_bus",
  "params": {
    "mix": { "min": 0, "max": 1, "default": 0.33 },
    "room": { "min": 0, "max": 1, "default": 0.8 },
    "damp": { "min": 0, "max": 1, "default": 0.5 }
  },
  "usage": "Send audio with: \\out, ~fx_reverb_bus"
}
```

---

### `fx_set`

Set effect parameters.

**Schema:**
```typescript
{
  name: "fx_set",
  description: "Set parameters on a loaded effect",
  inputSchema: {
    type: "object",
    properties: {
      slot: {
        type: "string",
        description: "Effect slot name"
      },
      params: {
        type: "object",
        description: "Parameter name/value pairs"
      }
    },
    required: ["slot", "params"]
  }
}
```

**Generated SC code:**
```supercollider
Ndef(\fx_reverb).set(\mix, 0.6, \room, 0.95);
```

---

### `fx_chain`

Create a chain of effects in series.

**Schema:**
```typescript
{
  name: "fx_chain",
  description: "Create a chain of effects. Returns the input bus for the chain.",
  inputSchema: {
    type: "object",
    properties: {
      name: {
        type: "string",
        description: "Name for this chain"
      },
      effects: {
        type: "array",
        items: {
          type: "object",
          properties: {
            name: { type: "string", description: "Effect name" },
            params: { type: "object", description: "Initial parameters" }
          },
          required: ["name"]
        },
        description: "Ordered list of effects"
      }
    },
    required: ["name", "effects"]
  }
}
```

**Example call:**
```json
{
  "name": "synthFX",
  "effects": [
    { "name": "lpf", "params": { "cutoff": 2000 } },
    { "name": "chorus", "params": { "rate": 0.5, "mix": 0.3 } },
    { "name": "reverb", "params": { "mix": 0.4, "room": 0.8 } }
  ]
}
```

**Generated SC code:**
```supercollider
// Create chain input bus
~chain_synthFX_in = Bus.audio(s, 2);

// Create inter-effect buses
~chain_synthFX_1 = Bus.audio(s, 2);
~chain_synthFX_2 = Bus.audio(s, 2);

// Create ordered effect Ndefs
Ndef(\chain_synthFX_lpf, { |cutoff=2000, resonance=0.5|
  var sig = In.ar(~chain_synthFX_in, 2);
  Out.ar(~chain_synthFX_1, RLPF.ar(sig, cutoff, resonance));
}).play;

Ndef(\chain_synthFX_chorus, { |rate=0.5, depth=0.005, mix=0.3|
  var sig = In.ar(~chain_synthFX_1, 2);
  var mod = SinOsc.kr([rate, rate * 1.01], [0, 0.5]).range(0.001, depth);
  var wet = DelayL.ar(sig, 0.1, mod);
  Out.ar(~chain_synthFX_2, (sig * (1 - mix)) + (wet * mix));
}).play;

Ndef(\chain_synthFX_reverb, { |mix=0.4, room=0.8, damp=0.5|
  var sig = In.ar(~chain_synthFX_2, 2);
  FreeVerb2.ar(sig[0], sig[1], mix, room, damp)
}).play;
```

**Return value:**
```json
{
  "name": "synthFX",
  "inputBus": "~chain_synthFX_in",
  "effects": [
    { "slot": "chain_synthFX_lpf", "name": "lpf" },
    { "slot": "chain_synthFX_chorus", "name": "chorus" },
    { "slot": "chain_synthFX_reverb", "name": "reverb" }
  ],
  "usage": "Send audio with: \\out, ~chain_synthFX_in"
}
```

---

### `fx_route`

Route a source (Pdef or Ndef) to an effect or chain.

**Schema:**
```typescript
{
  name: "fx_route",
  description: "Route a sound source to an effect or chain",
  inputSchema: {
    type: "object",
    properties: {
      source: {
        type: "string",
        description: "Pdef or Ndef name"
      },
      target: {
        type: "string",
        description: "Effect slot or chain name"
      }
    },
    required: ["source", "target"]
  }
}
```

**Generated SC code (for Pdef):**
```supercollider
// Update the pattern to use the effect bus
Pdef(\bass).set(\out, ~chain_synthFX_in);
```

**Generated SC code (for Ndef):**
```supercollider
// Create a routing Ndef
Ndef(\bass_routed, {
  Out.ar(~chain_synthFX_in, Ndef(\bass).ar)
}).play;
```

---

### `fx_bypass`

Bypass an effect (pass audio through unchanged).

**Schema:**
```typescript
{
  name: "fx_bypass",
  description: "Bypass an effect or entire chain",
  inputSchema: {
    type: "object",
    properties: {
      slot: {
        type: "string",
        description: "Effect slot or chain name"
      },
      bypass: {
        type: "boolean",
        default: true,
        description: "true to bypass, false to re-enable"
      }
    },
    required: ["slot"]
  }
}
```

**Implementation:** Store original function, replace with pass-through, restore on un-bypass.

---

### `fx_remove`

Remove an effect and free its resources.

**Schema:**
```typescript
{
  name: "fx_remove",
  description: "Remove an effect and free its bus",
  inputSchema: {
    type: "object",
    properties: {
      slot: {
        type: "string",
        description: "Effect slot or chain name"
      }
    },
    required: ["slot"]
  }
}
```

**Generated SC code:**
```supercollider
Ndef(\fx_reverb).clear;
~fx_reverb_bus.free;
```

---

### `fx_list`

List loaded effects and their current parameters.

**Schema:**
```typescript
{
  name: "fx_list",
  description: "List all loaded effects and their parameters",
  inputSchema: {
    type: "object",
    properties: {}
  }
}
```

**Return value:**
```json
{
  "effects": [
    {
      "slot": "fx_reverb",
      "type": "reverb",
      "params": { "mix": 0.5, "room": 0.8, "damp": 0.5 },
      "playing": true
    },
    {
      "slot": "chain_synthFX_lpf",
      "type": "lpf",
      "chain": "synthFX",
      "params": { "cutoff": 2000, "resonance": 0.5 },
      "playing": true
    }
  ],
  "chains": [
    {
      "name": "synthFX",
      "inputBus": "~chain_synthFX_in",
      "effects": ["lpf", "chorus", "reverb"]
    }
  ]
}
```

---

## State Management

```javascript
// src/effects.js

class EffectsManager {
  constructor(supercollider) {
    this.sc = supercollider;
    this.effects = new Map();      // slot -> { name, type, bus, params }
    this.chains = new Map();       // chainName -> { inputBus, effects[] }
  }

  async load(name, slot) { ... }
  async set(slot, params) { ... }
  async chain(name, effects) { ... }
  async route(source, target) { ... }
  async bypass(slot, bypass) { ... }
  async remove(slot) { ... }
  async list() { ... }
}
```

## Example Interactions

### Basic effect usage

**User:** "Add some reverb to the drums"

**Claude:**
1. `fx_load({ name: "reverb", slot: "drumReverb" })`
2. `fx_route({ source: "kick", target: "drumReverb" })`
3. `fx_route({ source: "snare", target: "drumReverb" })`

**Response:** "Added reverb to the drums. The kick and snare are now running through it."

---

### Building an effect chain

**User:** "Create a chain with filter, distortion, and delay for the bass"

**Claude:**
1. `fx_chain({ name: "bassFX", effects: [
     { name: "lpf", params: { cutoff: 800 } },
     { name: "distortion", params: { drive: 3 } },
     { name: "delay", params: { time: 0.375, mix: 0.3 } }
   ]})`
2. `fx_route({ source: "bass", target: "bassFX" })`

**Response:** "Created a bass chain: LPF (800 Hz) → Distortion (drive 3) → Delay (375ms). Bass is now routed through it."

---

### Tweaking effects live

**User:** "Open up the filter on the bass"

**Claude:**
1. `fx_set({ slot: "chain_bassFX_lpf", params: { cutoff: 4000 } })`

**Response:** "Opened the bass filter to 4000 Hz."

---

### MIDI control of effects

**User:** "Map my mod wheel to the filter cutoff"

**Claude:**
1. `midi_map_cc({ cc: 1, busName: "bassFilterCutoff", range: [200, 8000], curve: "exponential" })`
2. `sc_execute`:
```supercollider
Ndef(\chain_bassFX_lpf).map(\cutoff, ~bassFilterCutoff);
```

**Response:** "Mod wheel now controls the bass filter (200–8000 Hz)."

---

### Master bus processing

**User:** "Add compression and limiting to the master"

**Claude:**
1. `sc_execute`:
```supercollider
Ndef(\master, {
  var sig = In.ar(0, 2);
  sig = Compander.ar(sig, sig, 0.5, 1, 0.25, 0.01, 0.1, 1.5);
  sig = Limiter.ar(sig, 0.95);
  ReplaceOut.ar(0, sig);
}).play(addAction: \addToTail);
```

**Response:** "Added master compression (4:1 at -6dB) and limiting at -0.5dB."

---

## Integration with Live Coding

Effects work seamlessly with the proxy-based live coding approach:

```supercollider
// Effects can be redefined while playing
Ndef(\fx_reverb).fadeTime = 2;

// Now changing the effect crossfades smoothly
Ndef(\fx_reverb, { |mix=0.5, room=0.95, damp=0.3|
  var sig = In.ar(~fx_reverb_bus, 2);
  // Different reverb algorithm
  GVerb.ar(sig.sum, room * 100, 3, 0.5, 0.5, 15, damp, mix * 100, mix)
});
```

Effects parameters can be controlled by:
- Direct `.set()` calls
- Control buses (for automation)
- MIDI CC mappings
- Patterns (using `.map`)

## File Structure

```
src/
├── effects.js           # Effects tool implementations
├── effects-library.js   # Pre-built effect definitions
```

## Error Handling

| Error | Cause | Response |
|-------|-------|----------|
| `Unknown effect: X` | Invalid effect name | List available effects |
| `Effect not loaded: X` | Operating on non-existent slot | Suggest loading first |
| `Invalid parameter: X` | Unknown param name | List valid params for effect |
| `Parameter out of range` | Value outside min/max | Clamp and warn |
| `Chain not found: X` | Invalid chain name | List available chains |

## Future Enhancements

- **Effect presets**: Save/load parameter snapshots
- **Sidechain compression**: Route sidechain from another source
- **Frequency-dependent effects**: Multiband processing
- **Convolution reverb**: Load IR files for realistic spaces
- **Modulation matrix**: Map LFOs to any effect parameter
- **Parallel processing**: Split signal to multiple chains, mix back
- **Spectrum analyzer**: Return FFT data for visualization
