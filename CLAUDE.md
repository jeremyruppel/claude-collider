# Claude Collider

Claude Collider is an MCP server that lets Claude generate and execute SuperCollider code in real-time for live music synthesis.

## Skills

| Skill | Description |
| --- | --- |
| `/claude-collider` | Live code music with SuperCollider |
| `/play-tape` | Play a tape file (loads patterns, effects, arrangement) |
| `/arrange-tape` | Compose a CCArrangement for a tape |
| `/record-tape` | Record a live session as a tape (md + scd pair) |
| `/songwriting` | Music theory reference for composing patterns and melodies |

## Prefer MCP Tools Over Raw SC Code

Always use the dedicated MCP tools (`cc_fx`, `cc_sample`, `cc_midi`, `cc_control`, `cc_recording`, `cc_output`, `cc_reboot`, `cc_status`) instead of writing equivalent SuperCollider code with `cc_execute`. The tools handle error cases, validate input, and keep the server state consistent.

Use `cc_execute` only for things the tools don't cover: Pdef/Ndef definitions, custom SynthDefs, CCArrangement, and other raw SC code.

## Quick Reference

1. **Drums need `\freq, 48`** — without it, drums sound wrong
2. **Use Pdef for rhythms** — patterns that repeat
3. **Use Ndef for continuous** — pads, drones, textures
4. **Symbols not strings** — `\kick` not `"kick"`
5. **Semicolons between statements** — no trailing semicolon
6. **NEVER Synth() inside Ndef** — causes infinite spawning

---

# CC API Reference

Main entry point stored in `~cc`. Access subsystems via `~cc.synths`, `~cc.fx`, `~cc.midi`, `~cc.samples`, `~cc.recorder`, `~cc.state`.

## CC - Main Class

| Method | Description |
| --- | --- |
| `tempo(bpm)` | Get/set tempo in BPM |
| `stop` | Stop all Pdefs and Ndefs |
| `clear` | Full reset: free all synths, patterns, effects, samples |
| `status` | Get formatted status string |
| `reboot(device, numOutputs, onComplete)` | Restart the server |

## ~cc.synths - Synth Definitions

27+ pre-built synths with `cc_` prefix.

| Method | Description |
| --- | --- |
| `list` | Comma-separated list of synth names |
| `describe` | Detailed descriptions with params |
| `play(name, ...args)` | One-shot synth playback |

```supercollider
~cc.synths.play(\cc_kick, \freq, 48, \amp, 0.8);
```

## ~cc.fx - Effects System

18 built-in effects with routing, chaining, and sidechaining.

| Method | Description |
| --- | --- |
| `load(name, slot)` | Load effect (slot defaults to `fx_<name>`) |
| `set(slot, ...args)` | Set effect parameters |
| `bypass(slot, bool)` | Bypass/re-enable effect |
| `remove(slot)` | Remove effect |
| `route(source, target)` | Route Pdef/Ndef to effect |
| `connect(from, to)` | Chain effect output to another effect |
| `sidechain(name, threshold, ratio, attack, release)` | Create sidechain compressor |
| `routeTrigger(source, sidechainName, passthrough)` | Route trigger to sidechain |
| `routeToOutput(source, channels)` | Route to hardware outputs |
| `list` | Available effect names |
| `describe` | Effect descriptions with params |
| `status` | Current effects and routing |

```supercollider
~cc.fx.load(\reverb);
~cc.fx.route(\bass, \fx_reverb);
~cc.fx.set(\fx_reverb, \mix, 0.5, \room, 0.9);
```

## ~cc.midi - MIDI Control

| Method | Description |
| --- | --- |
| `listDevices` | List available MIDI devices |
| `connect(device, direction)` | Connect device (`\in` or `\out`) |
| `connectAll` | Connect all MIDI inputs |
| `disconnect(direction)` | Disconnect (`\in`, `\out`, or `\all`) |
| `output` | Get the connected MIDIOut instance |
| `play(synthName, channel, mono, velToAmp, ccMappings)` | Play synth via MIDI (multiple allowed) |
| `stop(synthName)` | Stop one synth, or all if nil |
| `status` | Get MIDI status |

```supercollider
~cc.midi.connectAll;
~cc.midi.play(\cc_lead, 1, false, true, (
  1: \cutoff,
  74: (param: \res, range: [0.1, 0.9])
));
~cc.midi.stop(\cc_lead);
```

## CCMIDIClock - MIDI Clock Output

Sends MIDI clock (24 ppqn) synchronized to TempoClock. When enabled, CCArrangement automatically starts/stops the clock on play/stop.

| Method | Description |
| --- | --- |
| `*enable(midiOut)` | Enable clock output with a MIDIOut instance |
| `*disable` | Disable clock output |
| `*start` | Start clock (no-op if not enabled) |
| `*stop` | Stop clock (no-op if not enabled) |

```supercollider
~cc.midi.connect("My Device", \out);
CCMIDIClock.enable(~cc.midi.output);
// Arrangements now auto-send MIDI clock start/stop/ticks
```

## ~cc.samples - Sample Management

Samples from `~/.claudecollider/samples`.

| Method | Description |
| --- | --- |
| `load(name)` | Load sample buffer into memory |
| `at(name)` | Get buffer (nil if not loaded) |
| `play(name, rate, amp)` | One-shot playback |
| `free(name)` | Free buffer |
| `reload` | Rescan directory for new files |
| `names` | Array of sample names |
| `list` | Comma-separated list |

```supercollider
~cc.samples.load(\kick);
~cc.samples.play(\kick, 1, 0.8);
Pdef(\samp, Pbind(\instrument, \cc_sampler, \buf, ~cc.samples.at(\kick), \dur, 1)).play
```

## ~cc.recorder - Audio Recording

Records to `~/.claudecollider/recordings`.

| Method | Description |
| --- | --- |
| `start(filename)` | Start recording (auto-names if nil) |
| `stop` | Stop recording, returns path |
| `status` | Recording status |
| `isRecording` | Boolean |

## ~cc.state - Bus Management

Named control/audio buses stored in current environment.

| Method | Description |
| --- | --- |
| `bus(name, numChannels, rate)` | Get or create bus (also sets `~name`) |
| `setBus(name, value)` | Set bus value |
| `getBus(name)` | Get bus by name |
| `freeBus(name)` | Free bus |
| `clear` | Free all buses |

```supercollider
~cc.state.bus(\cutoff, 1, \control);
~cc.state.setBus(\cutoff, 2000);
Synth(\cc_acid, [\cutoff, ~cutoff.asMap]);
```

## CCBreakbeat - Breakbeat Slice Sequencer

Divides a buffer into equal slices for pattern-based beat rearrangement. Standalone object (like CCMotif), not a CC subsystem.

| Method | Description |
| --- | --- |
| `*new(buffer, numSlices=8)` | Create slicer from a Buffer |
| `bars(numBars, beatsPerBar=4)` | Set slice duration from bar count (chainable) |
| `dur_(beats)` | Set explicit slice duration in beats |
| `sliceStart(index)` | Normalized start position (0-1) |
| `sliceEnd(index)` | Normalized end position (0-1) |
| `pattern(order, rate, amp)` | Returns Pbind for use with Pdef |
| `play(order, name)` | Quick-play via Pdef |
| `stop(name)` | Stop the Pdef |

Negative indices reverse that slice's playback. Per-slice rates via array.

```supercollider
~cc.samples.load(\amen);
b = CCBreakbeat(~cc.samples.at(\amen), 8).bars(1);
Pdef(\break, b.pattern([0, 0, 3, 2, 7, 6, 5, 4]));
Pdef(\break, b.pattern([0, 1, -2, 3, 4, 5, -6, 7]));
```
