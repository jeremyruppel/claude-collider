# Claude Collider

An MCP (Model Context Protocol) server that enables Claude to generate and play music through SuperCollider. Describe the sounds you want, and Claude will write and execute SuperCollider code in real-time.

[![YouTube Playlist](https://i.ytimg.com/vi/b2v-pr3SIvY/hqdefault.jpg?sqp=-oaymwExCOADEI4CSFryq4qpAyMIARUAAIhCGAHwAQH4AaIJgALQBYoCDAgAEAEYSyBVKGUwDw==&rs=AOn4CLBxIV7qDHDQUvoZa3EZxFZQlRIbXQ)](https://youtube.com/playlist?list=PLCSMRjzD98qXnSt50X4tvjvftlwIS-s7r&si=zdckQfASr4E81IGg)

## What It Can Do

**9 MCP tools** give Claude control over SuperCollider:

| Tool           | Description                                                                  |
| -------------- | ---------------------------------------------------------------------------- |
| `cc_execute`   | Execute SuperCollider code (auto-boots on first use)                         |
| `cc_status`    | Show status, routing debug, available synths, or available effects           |
| `cc_reboot`    | Reboot the audio server or list audio devices                                |
| `cc_control`   | Control playback: stop all sounds, clear everything, or get/set tempo        |
| `cc_fx`        | Effects operations: load, set params, bypass, remove, wire, sidechain, chain |
| `cc_midi`      | MIDI operations: list devices, connect, play synth, or stop                  |
| `cc_sample`    | Sample operations: inspect, load, play, free, or reload directory            |
| `cc_recording` | Recording operations: start, stop, or check status                           |
| `cc_output`    | Hardware output routing: route to outputs, unroute, or show status           |

**27+ built-in synths** across drums, bass, leads, pads, and utility categories. **18 built-in effects** including filters, reverb, delay, modulation, distortion, dynamics, and stereo processing.

See the [ClaudeCollider API reference](ClaudeCollider/README.md) for the full list of synths, effects, and class documentation.

## Requirements

- [SuperCollider](https://supercollider.github.io/) installed on your system
- Node.js 18+
- Claude Code, Claude Desktop, or any other MCP client

## Installation

```bash
git clone https://github.com/jeremyruppel/claude-collider.git
cd claude-collider
npm install
npm run build
npm test
```

## ClaudeCollider Quark

Symlink or copy the `ClaudeCollider/` folder to your Extensions directory:

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

Restart your MCP client after updating the config.

## Songwriting

The `/songwriting` skill is a built-in music theory reference that Claude loads when composing or improving individual parts. It covers:

- **Bass** — Bass line theory, patterns, and genre conventions
- **Breakbeats** — CCBreakbeat slicing, rearrangement, and genre applications
- **Chords** — Voicing, voice leading, progressions, and extensions
- **Melody** — Contour, motif development, phrasing, tension and resolution
- **Rhythm** — Drum patterns, swing, ghost notes, and genre grooves
- **Scales** — Scales, modes, when to use each, and SuperCollider usage

The skill enforces principles like register separation (keeping bass, chords, and melody in distinct octave ranges), complementary rhythms (if one part is on-beat, another syncopates), and repetition with variation. Ask Claude to use it when you want more musically intentional results:

- "Use the songwriting skill to write a better bass line for this tape"
- "The melody feels flat — can you improve it?"

## Tapes

Tapes are Claude Collider's session format — a paired `.md` and `.scd` file that capture a complete musical idea. The `tapes/` directory contains saved sessions you can play back or use as starting points.

A tape consists of:

- **`<name>.md`** — Metadata and documentation: tempo, key, element descriptions, arrangement overview, and musical notes. Has YAML frontmatter linking to its `.scd` file.
- **`<name>.scd`** — Executable SuperCollider code defining all Pdefs, Ndefs, effects, and routing. Elements are defined but not played — playback is controlled separately.
- **`<name>-arrangement.scd`** (optional) — A `CCArrangement` that orchestrates when elements enter and exit across sections.

### Playing a Tape

Ask Claude to play a tape by name:

- "Play the chopped-break tape"
- "Load up floor-burner"
- "Play night-drive in jam mode"

There are two playback modes:

- **Arrangement mode** (default if an arrangement file exists) — Loads the tape's elements and effects, then runs the arrangement which brings elements in and out over time with section transitions.
- **Jam mode** — Loads everything and starts all elements at once. Good for improvising on top of an existing tape.

### Recording a Tape

After a live session, ask Claude to save it:

- "Record this session as a tape called midnight-groove"
- "Save what we've been playing as a tape"

Claude captures the current tempo, patterns, effects, and routing into a new `.md`/`.scd` pair in `tapes/`.

### Arranging a Tape

You can also ask Claude to compose an arrangement for an existing tape:

- "Arrange the floor-burner tape"
- "Write an arrangement for night-drive with a long build"

This generates a `<name>-arrangement.scd` file with a `CCArrangement` that structures the tape's elements into intro, build, drop, break, and outro sections.

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
