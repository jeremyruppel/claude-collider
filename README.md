# Claude Collider

An MCP (Model Context Protocol) server that enables Claude to generate and play music through SuperCollider. Describe the sounds you want, and Claude will write and execute SuperCollider code in real-time.

## Requirements

- [SuperCollider](https://supercollider.github.io/) installed on your system
- Node.js 18+
- Claude Desktop

## Installation

```bash
git clone https://github.com/jeremyruppel/claude-collider.git
cd claude-collider
npm install
npm run build
npm test
```

## ClaudeCollider Quark

Symlink or copy the ClaudeCollider folder to your Extensions directory:

```bash
ln -s /path/to/claude-collider/ClaudeCollider ~/Library/Application\ Support/SuperCollider/Extensions/ClaudeCollider
```

## Claude Desktop Configuration

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "claude-collider": {
      "command": "node",
      "args": ["/path/to/claude-collider/dist/index.js"],
      "env": {
        "CC_SAMPLES_PATH": "/path/to/your/samples",
        "CC_RECORDINGS_PATH": "/path/to/your/recordings"
      }
    }
  }
}
```

The `env` block is optional - paths default to `~/.claudecollider/samples` and `~/.claudecollider/recordings`.

Restart Claude Desktop after updating the config.

## Available Tools (9 total)

| Tool           | Description                                                              |
| -------------- | ------------------------------------------------------------------------ |
| `cc_execute`   | Execute SuperCollider code (auto-boots on first use)                     |
| `cc_status`    | Show status, routing debug, available synths, or available effects       |
| `cc_reboot`    | Reboot the audio server or list audio devices                            |
| `cc_control`   | Control playback: stop all sounds, clear everything, or get/set tempo    |
| `cc_fx`        | Effects operations: load, set params, bypass, remove, wire, sidechain, chain |
| `cc_midi`      | MIDI operations: list devices, connect, play synth, or stop              |
| `cc_sample`    | Sample operations: inspect, load, play, free, or reload directory        |
| `cc_recording` | Recording operations: start, stop, or check status                       |
| `cc_output`    | Hardware output routing: route to outputs, unroute, or show status       |

## Pre-built SynthDefs

All synths are automatically loaded on first tool use. Use them with the `\cc_` prefix:

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

Load effects with `cc_fx`:

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
- "Use CC 1 (mod wheel) to control the filter cutoff"
- "Show me the server status"
- "Add some reverb to the drums"
- "Create an effect chain with filter, distortion, and delay for the bass"
- "Set up sidechain compression so the kick ducks the bass"
- "Start recording my jam"
- "Stop recording and save the file"

## Debug Mode

Enable debug logging:

```bash
DEBUG=claude-collider node dist/index.js
```

Logs are written to `/tmp/claude-collider.log`.

## Environment Variables

| Variable             | Default                        | Description                             |
| -------------------- | ------------------------------ | --------------------------------------- |
| `SCLANG_PATH`        | Auto-detected                  | Path to sclang executable               |
| `SC_BOOT_TIMEOUT`    | 10000                          | Boot timeout in ms                      |
| `SC_EXEC_TIMEOUT`    | 2000                           | Execution timeout in ms                 |
| `CC_SAMPLES_PATH`    | `~/.claudecollider/samples`    | Directory for audio samples             |
| `CC_RECORDINGS_PATH` | `~/.claudecollider/recordings` | Directory for recorded audio            |
| `DEBUG`              | -                              | Set to `claude-collider` for debug logs |

## License

MIT
