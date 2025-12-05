# Claude Collider

An MCP (Model Context Protocol) server that enables Claude to generate and play music through SuperCollider. Describe the sounds you want, and Claude will write and execute SuperCollider code in real-time.

## Requirements

- [SuperCollider](https://supercollider.github.io/) installed on your system
- Node.js 18+
- Claude Desktop

## Installation

```bash
git clone https://github.com/yourusername/claude-collider.git
cd claude-collider
npm install
npm run build
```

## Claude Desktop Configuration

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "supercollider": {
      "command": "node",
      "args": ["/path/to/claude-collider/dist/index.js"]
    }
  }
}
```

Restart Claude Desktop after updating the config.

## Available Tools

### SuperCollider Tools

| Tool | Description |
|------|-------------|
| `sc_boot` | Boot ClaudeCollider and the SuperCollider audio server |
| `sc_execute` | Execute SuperCollider code (play sounds, define synths, create patterns) |
| `sc_stop` | Stop all sounds (Cmd+Period equivalent) |
| `sc_status` | Get status: tempo, synths, CPU, active patterns |
| `sc_tempo` | Get or set the tempo in BPM |
| `sc_clear` | Stop all sounds and clear patterns, effects, and MIDI mappings |
| `sc_reboot` | Reboot the audio server with optional new device |
| `sc_audio_devices` | List available audio input and output devices |

### MIDI Tools

| Tool | Description |
|------|-------------|
| `midi_list_devices` | List available MIDI input and output devices |
| `midi_connect` | Connect to a MIDI device by name or index (use 'all' for all inputs) |
| `midi_map_notes` | Map MIDI notes to trigger a synth (polyphonic or mono) |
| `midi_map_cc` | Map a MIDI CC to a control bus for parameter modulation |
| `midi_clear` | Clear all MIDI mappings |

### Effects Tools

| Tool | Description |
|------|-------------|
| `fx_load` | Load a pre-built effect (returns input bus for routing) |
| `fx_set` | Set parameters on a loaded effect |
| `fx_chain` | Create a chain of effects in series |
| `fx_route` | Route a sound source (Pdef/Ndef) to an effect or chain |
| `fx_sidechain` | Create a sidechain compressor (e.g., kick ducking bass) |
| `fx_route_trigger` | Route a source to the trigger input of a sidechain |
| `fx_bypass` | Bypass an effect (pass audio through unchanged) |
| `fx_remove` | Remove an effect and free its resources |
| `fx_list` | List all loaded effects, chains, and sidechains |

## Available Resources

- `supercollider://synthdefs` - List of all pre-built synths with parameters
- `supercollider://effects` - List of all pre-built effects with parameters

## Pre-built SynthDefs

All synths are automatically loaded when you call `sc_boot`. Use them with the `\cc_` prefix:

**Drums**:
- **cc_kick** - Punchy kick drum with sub bass
- **cc_snare** - Snare drum with noise burst
- **cc_hihat** - Closed hi-hat
- **cc_openhat** - Open hi-hat with longer decay
- **cc_clap** - Hand clap with layered noise
- **cc_tom** - Tunable tom drum for fills
- **cc_rim** - Rimshot / sidestick
- **cc_shaker** - Shaker / maraca
- **cc_cowbell** - 808-style cowbell

**Bass**:
- **cc_bass** - Simple sub bass with harmonics
- **cc_acid** - Resonant 303-style filter bass
- **cc_sub** - Pure sub bass for layering
- **cc_reese** - Detuned saw bass (DnB/dubstep)
- **cc_fmbass** - FM bass for growly tones

**Leads & Melodic**:
- **cc_lead** - Detuned saw lead with filter
- **cc_pluck** - Karplus-Strong plucked string
- **cc_bell** - FM bell / glassy tone
- **cc_keys** - Electric piano / Rhodes-ish
- **cc_strings** - String ensemble pad

**Pads & Textural**:
- **cc_pad** - Soft ambient pad with detuned oscillators
- **cc_noise** - Filtered noise source
- **cc_drone** - Evolving ambient texture
- **cc_riser** - Tension building sweep

**Utility**:
- **cc_click** - Metronome click
- **cc_sine** - Pure sine tone
- **cc_sampler** - Basic sample playback
- **cc_grains** - Granular sample playback

## Pre-built Effects

Load effects with `fx_load`:

- **Filters**: lpf, hpf, bpf
- **Time-based**: reverb, delay, pingpong
- **Modulation**: chorus, flanger, phaser, tremolo
- **Distortion**: distortion, bitcrush, wavefold
- **Dynamics**: compressor, limiter, gate
- **Stereo**: widener, autopan

## Example Prompts

- "Boot SuperCollider and play a simple sine wave"
- "Load the kick and snare synths and make a basic beat"
- "Create an ambient pad that slowly evolves"
- "Make a 303-style acid bassline"
- "Set the tempo to 120 BPM"
- "Connect my MIDI keyboard and let me play the pad synth"
- "Map CC 1 to control the filter cutoff"
- "Show me the server status"
- "Add some reverb to the drums"
- "Create an effect chain with filter, distortion, and delay for the bass"
- "Set up sidechain compression so the kick ducks the bass"

## Debug Mode

Enable debug logging:

```bash
DEBUG=claude-collider node dist/index.js
```

Logs are written to `/tmp/claude-collider.log`.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SCLANG_PATH` | Auto-detected | Path to sclang executable |
| `SC_BOOT_TIMEOUT` | 10000 | Boot timeout in ms |
| `SC_EXEC_TIMEOUT` | 2000 | Execution timeout in ms |
| `DEBUG` | - | Set to `claude-collider` for debug logs |

## License

MIT
