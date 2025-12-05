# ClaudeCollider

A SuperCollider Quark providing a live coding toolkit for MCP/Claude integration. This quark is the SC backend for [claude-collider](https://github.com/jeremyruppel/claude-collider).

## Installation

Symlink or copy the ClaudeCollider folder to your Extensions directory:

```bash
ln -s /path/to/claude-collider/ClaudeCollider ~/Library/Application\ Support/SuperCollider/Extensions/ClaudeCollider
```

Then recompile the class library (Cmd+Shift+L in the IDE).

## Usage

```supercollider
// Boot ClaudeCollider
~cc = CC.boot;

// Or with a specific audio device
~cc = CC.boot(device: "BlackHole 2ch");

// Set tempo
~cc.tempo(120);

// Load and play synths
~cc.synths.play(\kick);
~cc.synths.play(\pad, \freq, 220, \amp, 0.3);
```

## Classes

### CC

Main facade class. Access subsystems via `~cc.synths`, `~cc.fx`, `~cc.midi`, `~cc.state`.

```supercollider
CC.boot(server, device, onComplete)  // Boot and initialize
~cc.tempo(bpm)                        // Get/set tempo (returns BPM)
~cc.device(name)                      // Set audio device (nil for default)
~cc.reboot(device, onComplete)        // Reboot with optional new device
~cc.stop                              // Stop all Pdefs/Ndefs
~cc.clear                             // Stop and clear everything
~cc.status                            // Get current status (booted, tempo, device, synths, cpu, pdefs, ndefs)
```

### CCSynths

Pre-built SynthDefs with `cc_` prefix. All synths are auto-loaded on boot.

```supercollider
~cc.synths.loadAll          // Load all synths (called automatically on boot)
~cc.synths.load(\kick)      // Load specific synth
~cc.synths.play(\kick)      // Load if needed and play
~cc.synths.list             // List available synth names
~cc.synths.describe         // Print all synths with descriptions and params
```

**Available synths**:

| Name | Description | Params |
|------|-------------|--------|
| kick | Punchy kick drum with sub bass | out, freq, amp, decay |
| snare | Snare drum with noise burst | out, freq, amp, decay |
| hihat | Closed hi-hat | out, amp, decay |
| clap | Hand clap with layered noise | out, amp, decay |
| bass | Simple sub bass with harmonics | out, freq, amp, decay, gate |
| acid | Resonant 303-style filter bass | out, freq, amp, cutoff, res, decay, gate |
| lead | Detuned saw lead with filter | out, freq, amp, pan, gate, att, rel, cutoff, res |
| pad | Soft ambient pad | out, freq, amp, attack, release, gate |

**Note**: When using drum synths with Pbind, explicitly set `\freq` to avoid SuperCollider's default pitch conversion overriding the synth's default frequency.

### CCFX

Ndef-based effects system with routing and chaining.

```supercollider
// Load single effect (creates fx_<name> slot)
~cc.fx.load(\reverb)
~cc.fx.load(\delay, \myDelay)  // Custom slot name

// Set parameters
~cc.fx.set(\fx_reverb, \mix, 0.5, \room, 0.8)

// Create effect chain
~cc.fx.chain(\drums, [\distortion, \reverb, \limiter])

// Chain with initial params
~cc.fx.chain(\drums, [\distortion -> [\drive, 4], \reverb, \limiter])

// Route sources to effects
~cc.fx.route(\kickPattern, \fx_reverb)

// Sidechain compression
~cc.fx.sidechain(\bassDuck, threshold: 0.1, ratio: 4, attack: 0.01, release: 0.1)
~cc.fx.route(\bassPattern, \bassDuck)
~cc.fx.routeTrigger(\kickPattern, \bassDuck)

// Bypass/remove
~cc.fx.bypass(\fx_reverb, true)
~cc.fx.remove(\fx_reverb)

// Info
~cc.fx.list                 // List available effect names
~cc.fx.describe             // Print all effects with descriptions and params
~cc.fx.status               // Get loaded effects, chains, sidechains
```

**Available effects**:

| Category | Effects |
|----------|---------|
| Filters | lpf, hpf, bpf |
| Time-based | reverb, delay, pingpong |
| Modulation | chorus, flanger, phaser, tremolo |
| Distortion | distortion, bitcrush, wavefold |
| Dynamics | compressor, limiter, gate |
| Stereo | widener, autopan |

### CCMIDI

MIDI device management and mapping.

```supercollider
~cc.midi.listDevices              // List MIDI devices (inputs and outputs)
~cc.midi.connectAll               // Connect all inputs
~cc.midi.connect("Launchpad", \in)
~cc.midi.connect(0, \out)         // Connect by index
~cc.midi.disconnect(\all)         // Disconnect all

// Map notes to synth (polyphonic by default)
~cc.midi.mapNotes(\pad)
~cc.midi.mapNotes(\pad, velocityToAmp: true)

// Map notes (monophonic, specific channel)
~cc.midi.mapNotes(\lead, channel: 0, mono: true)

// Map CC to control bus (creates ~busName)
~cc.midi.mapCC(1, \cutoff, [200, 8000], \exp)
~cc.midi.mapCC(74, \filter, [0, 1], \lin, channel: 0)

// MIDI learn
~cc.midi.learn(timeout: 10, callback: { |result| result.postln })

// Send MIDI out
~cc.midi.send(\noteOn, 0, 60, 100)
~cc.midi.send(\cc, 0, 1, 64)

// Event logging
~cc.midi.enableLog(true)
~cc.midi.getRecent(20)            // Get last 20 events
~cc.midi.getRecent(type: \noteOn) // Filter by type

// Clear
~cc.midi.clearMappings            // Clear note/CC mappings
~cc.midi.clearAll                 // Clear everything including connections
~cc.midi.status                   // Get MIDI status
```

### CCState

Bus and state management.

```supercollider
~cc.state.bus(\cutoff)             // Get or create control bus (also sets ~cutoff)
~cc.state.bus(\audio, 2, \audio)   // Create audio bus with 2 channels
~cc.state.setBus(\cutoff, 1000)    // Set bus value
~cc.state.getBus(\cutoff)          // Get bus
~cc.state.freeBus(\cutoff)         // Free bus
~cc.state.clear                    // Free all buses
~cc.state.status                   // Get list of buses
```

## License

MIT
