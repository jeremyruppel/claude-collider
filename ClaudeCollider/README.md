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

Main facade class. Access subsystems via `~cc.synths`, `~cc.fx`, `~cc.midi`, `~cc.samples`, `~cc.recorder`, `~cc.state`, `~cc.formatter`, `~cc.outputs`, `~cc.router`, `~cc.sidechains`.

```supercollider
CC.boot(server, device, numOutputs, onComplete)  // Boot and initialize
~cc.tempo(bpm)                        // Get/set tempo (returns BPM)
~cc.device(name)                      // Set audio device (nil for default)
~cc.numOutputs(num)                   // Get/set output channels (for multi-channel)
~cc.reboot(device, numOutputs, onComplete)  // Reboot with optional new device
~cc.stop                              // Stop all Pdefs/Ndefs
~cc.clear                             // Stop and clear everything
~cc.status                            // Get formatted status string

// Convenience accessors
~cc.outputs                           // CCOutputs instance (via fx.outputs)
~cc.router                            // CCRouter instance (via fx.router)
~cc.sidechains                        // CCSidechain instance (via fx.sidechains)
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

_Drums_:
| Name | Description | Params |
|------|-------------|--------|
| kick | Punchy kick drum with sub bass | out, freq, amp, decay |
| snare | Snare drum with noise burst | out, freq, amp, decay |
| hihat | Closed hi-hat | out, amp, decay |
| openhat | Open hi-hat with longer decay | out, amp, decay |
| clap | Hand clap with layered noise | out, amp, decay |
| tom | Tunable tom drum for fills | out, freq, amp, decay |
| rim | Rimshot / sidestick | out, amp, freq |
| shaker | Shaker / maraca | out, amp, decay, color |
| cowbell | 808-style cowbell | out, amp, decay |

_Bass_:
| Name | Description | Params |
|------|-------------|--------|
| bass | Simple sub bass with harmonics | out, freq, amp, decay, gate |
| acid | Resonant 303-style filter bass | out, freq, amp, cutoff, res, decay, gate |
| sub | Pure sub bass for layering | out, freq, amp, decay, gate |
| reese | Detuned saw bass (DnB/dubstep) | out, freq, amp, detune, cutoff, gate |
| fmbass | FM bass for growly tones | out, freq, amp, index, ratio, gate |

_Leads & Melodic_:
| Name | Description | Params |
|------|-------------|--------|
| lead | Detuned saw lead with filter | out, freq, amp, pan, gate, att, rel, cutoff, res |
| pluck | Karplus-Strong plucked string | out, freq, amp, decay, color |
| bell | FM bell / glassy tone | out, freq, amp, decay, brightness |
| keys | Electric piano / Rhodes-ish | out, freq, amp, attack, release, brightness, gate |
| strings | String ensemble pad | out, freq, amp, attack, release, detune, gate |

_Pads & Textural_:
| Name | Description | Params |
|------|-------------|--------|
| pad | Soft ambient pad | out, freq, amp, attack, release, gate |
| noise | Filtered noise source | out, amp, cutoff, res, type, gate |
| drone | Evolving ambient texture | out, freq, amp, spread, movement, gate |
| riser | Tension building sweep | out, amp, duration, startFreq, endFreq |

_Utility_:
| Name | Description | Params |
|------|-------------|--------|
| click | Metronome click | out, amp, freq |
| sine | Pure sine tone | out, freq, amp, gate |
| sampler | Basic sample playback | out, buf, amp, rate, start |
| grains | Granular sample playback | out, buf, amp, pos, posSpeed, grainSize, grainRate, pitch, spread, gate |

**Note**: When using drum synths with Pbind, explicitly set `\freq` to avoid SuperCollider's default pitch conversion overriding the synth's default frequency.

### CCSamples

Sample management with lazy loading from `~/.claudecollider/samples`.

Place WAV or AIFF files in the samples directory. On boot, file paths are scanned but buffers are only loaded on first use.

```supercollider
// Access via ~cc.samples

~cc.samples.list              // List available sample names
~cc.samples.at(\kick)         // Get buffer by name (nil if not loaded)
~cc.samples.load(\kick)       // Explicitly load buffer (for use in patterns)
~cc.samples.play(\kick)       // One-shot playback (lazy loads buffer)
~cc.samples.play(\snare, 0.5, 0.8)  // With rate and amp

// Use in patterns (load first to avoid latency on first trigger)
~cc.samples.load(\kick);
Pbind(\instrument, \cc_sampler, \buf, ~cc.samples.at(\kick), \dur, 1).play

// Memory management
~cc.samples.free(\kick)       // Free single sample buffer
~cc.samples.freeAll           // Free all loaded buffers
~cc.samples.reload            // Rescan directory for new samples
~cc.samples.describe          // Print sample status (loaded/unloaded)
```

**Note**: Call `~cc.samples.load(\name)` to pre-load a buffer before using `at(\name)` in Pbind.

### CCFX

Ndef-based effects system with routing and chaining.

```supercollider
// Load single effect (creates fx_<name> slot)
~cc.fx.load(\reverb)
~cc.fx.load(\delay, \myDelay)  // Custom slot name

// Set parameters
~cc.fx.set(\fx_reverb, \mix, 0.5, \room, 0.8)

// Route sources to effects
~cc.fx.route(\kickPattern, \fx_reverb)

// Connect effects in series (effect → effect)
~cc.fx.load(\distortion, \fx_dist)
~cc.fx.load(\reverb, \fx_verb)
~cc.fx.connect(\fx_dist, \fx_verb)  // distortion → reverb → main out
~cc.fx.route(\bass, \fx_dist)       // bass → distortion → reverb → main out

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
~cc.fx.status               // Get loaded effects, sidechains, connections
```

**Available effects**:

| Category   | Effects                          |
| ---------- | -------------------------------- |
| Filters    | lpf, hpf, bpf                    |
| Time-based | reverb, delay, pingpong          |
| Modulation | chorus, flanger, phaser, tremolo |
| Distortion | distortion, bitcrush, wavefold   |
| Dynamics   | compressor, limiter, gate        |
| Stereo     | widener, autopan                 |

### CCOutputs

Manages hardware output routing with per-output limiters. Sources can be routed to specific outputs or stereo pairs.

```supercollider
// Access via ~cc.outputs or ~cc.fx.outputs

// Route sources to specific hardware outputs
~cc.fx.routeToOutput(\drums, [7, 8])    // Route drums to stereo outputs 7-8
~cc.fx.routeToOutput(\kick, 1)          // Route kick to mono output 1
~cc.fx.routeToOutput(\bass, [1, 2])     // Route to main (1-2)

// Unroute sources
~cc.fx.unrouteFromOutput(\drums)

// Main output control (outputs 1-2 with limiter)
~cc.fx.playMainOutput                   // Start main output
~cc.fx.stopMainOutput                   // Stop main output
~cc.fx.setMainOutput(2)                 // Move main to outputs 3-4 (0-indexed: 2)
~cc.fx.isMainOutputPlaying              // Check if main output is active

// Status
~cc.fx.outputStatus                     // Get output routing status
```

### CCOutput

Represents a single hardware output destination (mono or stereo pair). Each output includes a limiter for protection.

```supercollider
// Usually accessed via CCOutputs, not directly
var output = ~cc.outputs.at(\out_3_4)

output.isMain                           // true if this is the main output
output.isPlaying                        // true if output Ndef is playing
output.channels                         // 3 or [3, 4] (1-indexed)
output.hwOut                            // Hardware output index (0-indexed)
output.inputBusIndex                    // Bus index for routing audio to this output
output.sources                          // Array of routed source names

output.route(\myPattern)                // Route a source to this output
output.unroute(\myPattern)              // Remove source from this output
output.setHwOut(4)                      // Change hardware output destination
output.statusString                     // "out_3_4 -> hw 3-4 (active)"
```

### CCRouter

Manages effect-to-effect connections, effect chains, and source-to-effect routing.

```supercollider
// Access via ~cc.router or ~cc.fx.router

// Effect connections (effect → effect)
~cc.fx.connect(\fx_dist, \fx_reverb)    // Connect distortion output to reverb input

// Effect chains (named groups)
~cc.fx.registerChain(\drums, [\fx_comp, \fx_reverb])

// Source routing (pattern/Ndef → effect)
~cc.fx.route(\kickPattern, \fx_comp)

// Status
~cc.router.connections                  // Dictionary of effect connections
~cc.router.chains                       // Dictionary of named chains
~cc.router.routes                       // Dictionary of source routes
~cc.router.status                       // Formatted status string
```

### CCSidechain

Manages sidechain compressors for ducking effects.

```supercollider
// Access via ~cc.sidechains or ~cc.fx.sidechains

// Create sidechain compressor
~cc.fx.sidechain(\bassDuck, threshold: 0.1, ratio: 4, attack: 0.01, release: 0.1)

// Route audio through sidechain
~cc.fx.route(\bassPattern, \bassDuck)

// Route trigger source (the signal that triggers compression)
~cc.fx.routeTrigger(\kickPattern, \bassDuck)
~cc.fx.routeTrigger(\kickPattern, \bassDuck, passthrough: false)  // Mute trigger

// Status
~cc.sidechains.size                     // Number of sidechains
~cc.sidechains.keys                     // Array of sidechain names
~cc.sidechains.at(\bassDuck)            // Get sidechain info
~cc.sidechains.status                   // Formatted status string
```

### CCMIDI

MIDI device connection and synth playback.

```supercollider
// Device connection
~cc.midi.listDevices              // List MIDI devices (inputs and outputs)
~cc.midi.connectAll               // Connect all inputs
~cc.midi.connect("Launchpad", \in)
~cc.midi.connect(0, \out)         // Connect by index
~cc.midi.disconnect(\all)         // Disconnect all

// Play a synth via MIDI (simple)
~cc.midi.play(\pad)               // Polyphonic, all channels

// Play with options
~cc.midi.play(\lead, channel: 0, mono: true)

// Play with CC mappings
~cc.midi.play(\acid, ccMappings: Dictionary[
  1 -> \cutoff,                   // Simple: CC 1 → cutoff (0-1)
  74 -> (param: \res, range: [0.1, 0.9], curve: \lin)  // Full spec
])

// Stop MIDI playback
~cc.midi.stop

// Clear everything
~cc.midi.clear                    // Stop synth and disconnect devices
~cc.midi.status                   // Get MIDI status
```

### CCRecorder

Audio recording to WAV files in `~/.claudecollider/recordings/`.

```supercollider
// Access via ~cc.recorder

~cc.recorder.start                    // Start recording with auto-generated filename
~cc.recorder.start("mysong.wav")      // Start with custom filename
~cc.recorder.stop                     // Stop recording and save file
~cc.recorder.status                   // Check recording state
~cc.recorder.isRecording              // Boolean: currently recording?
~cc.recorder.currentPath              // Path to current recording (nil if not recording)
```

Recordings are saved as 16-bit WAV files. Auto-generated filenames use the format `recording_YYYY_MM_DD_HHMMSS.wav`.

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

### CCFormatter

Status formatting and routing debug visualization.

```supercollider
~cc.formatter.format               // Get formatted status string
~cc.formatter.print                // Print status to post window
~cc.formatter.formatRoutingDebug   // Get detailed routing debug info

// Individual sections
~cc.formatter.formatServer         // "Server: running | CPU: 5.2% | Synths: 12"
~cc.formatter.formatTempo          // "Tempo: 120.0 BPM | Device: BlackHole 2ch"
~cc.formatter.formatSamples        // "Samples: 3/10 loaded"
~cc.formatter.formatPdefs          // "Pdefs playing: kick, bass"
~cc.formatter.formatNdefs          // "Ndefs playing: fx_reverb"
~cc.formatter.playingPdefs         // Array of playing Pdef names
~cc.formatter.playingNdefs         // Array of playing Ndef names
```

## Tests

ClaudeCollider includes a test suite using SuperCollider's UnitTest framework.

### Running Tests

From the project root:

```bash
npm test
```

Or directly with sclang:

```bash
sclang ClaudeCollider/tests/run_tests.scd
```

### Test Files

| File | Description |
|------|-------------|
| CCTest.sc | Core CC class tests (initialization, tempo, device, stop/clear) |
| CCSynthsTest.sc | SynthDef loading and description tests |
| CCFXTest.sc | Effects system tests (describe, list formatting) |
| CCSamplesTest.sc | Sample management tests |
| CCFormatterTest.sc | Status formatter tests |
| CCOutputTest.sc | Single output destination tests |
| CCOutputsTest.sc | Output collection manager tests |
| CCRouterTest.sc | Effect routing and chain tests |
| CCSidechainTest.sc | Sidechain compressor tests |

### Writing Tests

Tests extend `UnitTest` and follow SuperCollider conventions:

```supercollider
CCMyTest : UnitTest {
  setUp {
    // Setup before each test
  }

  tearDown {
    // Cleanup after each test
  }

  test_featureName_expectedBehavior {
    this.assert(condition, "description");
    this.assertEquals(actual, expected, "description");
  }
}
```

## License

MIT
