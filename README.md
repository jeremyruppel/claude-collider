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
| `sc_boot` | Start SuperCollider and boot the audio server |
| `sc_execute` | Execute arbitrary SuperCollider code |
| `sc_stop` | Stop all sounds (Cmd+Period equivalent) |
| `sc_free_all` | Free all synth nodes (nuclear option) |
| `sc_restart` | Restart SuperCollider from scratch |
| `sc_load_synthdef` | Load a pre-built synth (kick, snare, hihat, clap, bass, acid, pad) |
| `sc_status` | Show synth count and CPU usage |

### MIDI Tools

| Tool | Description |
|------|-------------|
| `midi_list_devices` | List available MIDI input and output devices |
| `midi_connect` | Connect to a MIDI device by name or index |
| `midi_map_notes` | Map MIDI notes to trigger a synth (polyphonic or mono) |
| `midi_map_cc` | Map a MIDI CC to a control bus for parameter modulation |
| `midi_learn` | Wait for a CC message to detect which knob/fader to map |
| `midi_send` | Send MIDI messages to external gear |
| `midi_get_recent` | Get recent MIDI events (for "what did I just play?") |
| `midi_clear_mappings` | Remove all MIDI mappings |

### Effects Tools

| Tool | Description |
|------|-------------|
| `fx_load` | Load a pre-built effect (lpf, hpf, reverb, delay, chorus, distortion, etc.) |
| `fx_set` | Set parameters on a loaded effect |
| `fx_chain` | Create a chain of effects in series |
| `fx_route` | Route a sound source to an effect or chain |
| `fx_bypass` | Bypass an effect (pass audio through unchanged) |
| `fx_remove` | Remove an effect and free its resources |
| `fx_list` | List all loaded effects and their parameters |
| `fx_sidechain` | Create a sidechain compressor (e.g., kick ducking bass) |
| `fx_sidechain_set` | Update sidechain compressor parameters |
| `fx_sidechain_remove` | Remove a sidechain compressor |

## Available Resources

- `supercollider://synthdefs` - List of all pre-built synths with parameters
- `supercollider://examples` - Common code snippets for reference

## Pre-built SynthDefs

After booting, load synths with `sc_load_synthdef`:

- **kick** - Punchy kick drum with sub bass
- **snare** - Snare drum with noise burst
- **hihat** - Closed hi-hat
- **clap** - Hand clap with layered noise
- **bass** - Simple sub bass with harmonics
- **acid** - Resonant 303-style filter bass
- **pad** - Soft ambient pad with detuned oscillators

## Example Prompts

- "Boot SuperCollider and play a simple sine wave"
- "Load the kick and snare synths and make a basic beat"
- "Create an ambient pad that slowly evolves"
- "Make a 303-style acid bassline"
- "Show me the server status"
- "Connect my MIDI keyboard and let me play the pad synth"
- "Map my filter knob to control the cutoff frequency"
- "What did I just play on the keyboard?"
- "Turn those notes into a loop"
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
