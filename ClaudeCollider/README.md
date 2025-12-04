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
~cc.tempo(bpm)                        // Get/set tempo
~cc.device(name)                      // Switch audio device
~cc.reboot(device)                    // Reboot with new device
~cc.stop                              // Stop all Pdefs/Ndefs
~cc.clear                             // Stop and clear everything
~cc.status                            // Get current status
```

### CCSynths

Pre-built SynthDefs with `cc_` prefix.

```supercollider
~cc.synths.loadAll        // Load all synths
~cc.synths.load(\kick)    // Load specific synth
~cc.synths.play(\kick)    // Load and play
~cc.synths.list           // List available synths
```

**Available synths**: kick, snare, hihat, clap, bass, acid, lead, pad

### CCFX

Ndef-based effects system with routing and chaining.

```supercollider
// Load single effect
~cc.fx.load(\reverb)
~cc.fx.load(\delay, \myDelay)  // Custom slot name

// Set parameters
~cc.fx.set(\fx_reverb, \mix, 0.5, \room, 0.8)

// Create effect chain
~cc.fx.chain(\drums, [\distortion, \reverb, \limiter])

// Route sources to effects
~cc.fx.route(\kickPattern, \fx_reverb)

// Sidechain compression
~cc.fx.sidechain(\bassDuck, threshold: 0.1, ratio: 4)
~cc.fx.route(\bassPattern, \bassDuck)
~cc.fx.routeTrigger(\kickPattern, \bassDuck)

// Bypass/remove
~cc.fx.bypass(\fx_reverb, true)
~cc.fx.remove(\fx_reverb)
```

**Available effects**:
- Filters: lpf, hpf, bpf
- Time-based: reverb, delay, pingpong
- Modulation: chorus, flanger, phaser, tremolo
- Distortion: distortion, bitcrush, wavefold
- Dynamics: compressor, limiter, gate
- Stereo: widener, autopan

### CCMIDI

MIDI device management and mapping.

```supercollider
~cc.midi.listDevices              // List MIDI devices
~cc.midi.connectAll               // Connect all inputs
~cc.midi.connect("Launchpad", \in)

// Map notes to synth (polyphonic)
~cc.midi.mapNotes(\pad)

// Map notes (monophonic, specific channel)
~cc.midi.mapNotes(\lead, channel: 0, mono: true)

// Map CC to control bus
~cc.midi.mapCC(1, \cutoff, [200, 8000], \exp)

// Clear mappings
~cc.midi.clearMappings
```

### CCState

Bus and state management.

```supercollider
~cc.state.bus(\cutoff)           // Get or create control bus
~cc.state.bus(\audio, 2, \audio) // Create audio bus
~cc.state.setBus(\cutoff, 1000)  // Set bus value
~cc.state.freeBus(\cutoff)       // Free bus
~cc.state.clear                  // Free all buses
```

## License

MIT
