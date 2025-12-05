# ClaudeCollider Effect Routing Specification

## Overview

This spec defines two new tools for routing effects into one another:

- **`fx_connect`** — Connect one effect's output to another effect's input
- **`fx_chain`** — Create a named chain of effects in a single call

These complement the existing `fx_route` tool, which routes a source (Pdef/Ndef) to an effect.

---

## Tool 1: `fx_connect`

### Purpose
Connect the output of one effect slot to the input of another effect slot, allowing manual construction of effect chains.

### Schema

```json
{
  "name": "fx_connect",
  "description": "Connect one effect's output to another effect's input for serial processing.",
  "parameters": {
    "type": "object",
    "required": ["from", "to"],
    "properties": {
      "from": {
        "type": "string",
        "description": "Source effect slot name (e.g. 'fx_distortion')"
      },
      "to": {
        "type": "string",
        "description": "Destination effect slot name (e.g. 'fx_reverb')"
      }
    }
  }
}
```

### Behavior

1. Validate that both `from` and `to` slots exist and are loaded effects
2. Route the output of `from` into the input bus of `to`
3. Ensure `to` outputs to main out (bus 0) unless it's already connected to another effect
4. Return confirmation with the connection path

### Return Value

```
Connected fx_distortion → fx_reverb
```

### Error Cases

- `"Error: Source effect 'fx_foo' not found"` — if `from` slot doesn't exist
- `"Error: Destination effect 'fx_bar' not found"` — if `to` slot doesn't exist
- `"Error: Cannot connect effect to itself"` — if `from` === `to`
- `"Error: Circular connection detected"` — if connection would create a loop

### Example Usage

```
// Load two effects
fx_load("distortion", slot: "fx_dist")
fx_load("reverb", slot: "fx_verb")

// Connect them: distortion → reverb
fx_connect(from: "fx_dist", to: "fx_verb")

// Route source to first effect in chain
fx_route(source: "bass", target: "fx_dist")

// Signal flow: bass → fx_dist → fx_verb → main out
```

---

## Tool 2: `fx_chain`

### Purpose
Create a named chain of effects wired in series, with optional parameters for each effect.

### Schema

```json
{
  "name": "fx_chain",
  "description": "Create a named chain of effects wired in series. Returns the chain's input slot for routing sources.",
  "parameters": {
    "type": "object",
    "required": ["name", "effects"],
    "properties": {
      "name": {
        "type": "string",
        "description": "Name for this chain (e.g. 'bass_chain', 'vocal_chain')"
      },
      "effects": {
        "type": "array",
        "description": "Ordered list of effects. Each item can be a string (effect name with defaults) or an object with name and params.",
        "items": {
          "oneOf": [
            {
              "type": "string",
              "description": "Effect name with default parameters"
            },
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": {
                  "type": "string",
                  "description": "Effect name (e.g. 'reverb', 'distortion')"
                },
                "params": {
                  "type": "object",
                  "description": "Parameter key/value pairs for this effect"
                }
              }
            }
          ]
        }
      }
    }
  }
}
```

### Behavior

1. Validate that all effect names in the array are valid/available effects
2. Create each effect with slot name `{chain_name}_{index}_{effect_name}` (e.g. `bass_chain_0_distortion`)
3. Wire them in series: effect[0] → effect[1] → effect[2] → ... → main out
4. Apply any provided params to each effect
5. Store chain metadata for later reference (for `fx_list`, `fx_remove`)
6. Return the chain name and input bus for routing

### Return Value

```
Created chain: bass_chain
  → bass_chain_0_distortion (bus 4)
  → bass_chain_1_chorus (bus 8)
  → bass_chain_2_reverb (bus 12)
  → main out
Route sources to: bass_chain
```

### Error Cases

- `"Error: Chain name 'foo' already exists"` — if a chain with that name is already loaded
- `"Error: Unknown effect 'foo' at position 2"` — if an effect name is invalid
- `"Error: Effects array cannot be empty"` — if no effects provided

### Chain Management

The chain should be treated as a unit for management purposes:

- `fx_route(source: "bass", target: "bass_chain")` — routes to the first effect in the chain
- `fx_set(slot: "bass_chain_1_chorus", params: {...})` — set params on individual effects in chain
- `fx_bypass(slot: "bass_chain")` — bypass the entire chain
- `fx_remove(slot: "bass_chain")` — remove all effects in the chain and free resources

### Example Usage

**Simple (effect names only):**
```
fx_chain(
  name: "drum_bus",
  effects: ["compressor", "distortion", "limiter"]
)
```

**With parameters:**
```
fx_chain(
  name: "bass_chain",
  effects: [
    {"name": "distortion", "params": {"drive": 1.5, "mix": 0.4}},
    {"name": "chorus", "params": {"rate": 0.2, "depth": 0.003}},
    {"name": "reverb", "params": {"room": 0.6, "mix": 0.3}}
  ]
)

fx_route(source: "bass", target: "bass_chain")
```

**Mixed:**
```
fx_chain(
  name: "vocal_chain",
  effects: [
    "compressor",
    {"name": "delay", "params": {"time": 0.375, "feedback": 0.4}},
    "reverb"
  ]
)
```

---

## Implementation Notes

### Bus Allocation

Each effect needs its own input bus. The system should:

1. Track allocated buses to avoid collisions
2. Allocate buses in increments (e.g. 4, 8, 12, 16...)
3. Free buses when effects/chains are removed

### Internal Data Structures

Suggested tracking structures:

```javascript
// Track individual effects
~cc.effects = Dictionary[
  "fx_reverb" -> (bus: 4, ndef: Ndef(\fx_reverb), type: "reverb"),
  "fx_delay" -> (bus: 8, ndef: Ndef(\fx_delay), type: "delay"),
  ...
]

// Track chains
~cc.chains = Dictionary[
  "bass_chain" -> (
    slots: ["bass_chain_0_distortion", "bass_chain_1_reverb"],
    inputBus: 4
  ),
  ...
]

// Track connections (for fx_connect)
~cc.connections = Dictionary[
  "fx_dist" -> "fx_reverb",  // fx_dist outputs to fx_reverb's input
  ...
]
```

### Signal Flow

For a chain `[A, B, C]`:

```
source → bus_A → [Effect A] → bus_B → [Effect B] → bus_C → [Effect C] → bus 0 (main out)
```

Each effect reads from its input bus and writes to the next effect's input bus (or main out for the last effect).

### Circular Detection (for fx_connect)

Before connecting, walk the connection graph from `to` to ensure `from` is not reachable:

```
function wouldCreateCycle(from, to):
  current = to
  while current in connections:
    current = connections[current]
    if current == from:
      return true
  return false
```

---

## Updates to `fx_list`

The `fx_list` tool should be updated to show chains:

```
Effects:
  fx_hats_phaser (phaser) - bus 4
  fx_snare_verb (reverb) - bus 8

Chains:
  bass_chain:
    → bass_chain_0_distortion (bus 12)
    → bass_chain_1_chorus (bus 16)
    → bass_chain_2_reverb (bus 20)
    → main out

Connections:
  fx_delay → fx_reverb
```

---

## Summary

| Tool | Purpose | Key Parameters |
|------|---------|----------------|
| `fx_route` | Source → Effect | `source`, `target` |
| `fx_connect` | Effect → Effect | `from`, `to` |
| `fx_chain` | Create serial chain | `name`, `effects[]` |

These three tools together provide complete flexibility for effect routing:

- **Simple:** `fx_route` a source to a single effect
- **Manual chains:** `fx_load` multiple effects, then `fx_connect` them
- **Quick chains:** `fx_chain` to create a full chain in one call
