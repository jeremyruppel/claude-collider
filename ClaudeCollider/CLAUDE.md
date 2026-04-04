# ClaudeCollider Quark

SuperCollider backend providing the live coding toolkit classes.

## Class Hierarchy

**CC** is the main facade, stored in `~cc`. All subsystems are accessed through it:

| Subsystem | Class | Access |
| --- | --- | --- |
| Synths | CCSynths | `~cc.synths` |
| Effects | CCFX | `~cc.fx` |
| MIDI | CCMIDI | `~cc.midi` |
| Samples | CCSamples | `~cc.samples` |
| Recording | CCRecorder | `~cc.recorder` |
| State/Buses | CCState | `~cc.state` |
| Formatting | CCFormatter | `~cc.formatter` |
| Outputs | CCOutputs | `~cc.outputs` |
| Routing | CCRouter | `~cc.router` |
| Sidechains | CCSidechain | `~cc.sidechains` |

## Classes

- **CC** — Main facade. Manages server boot, tempo, stop/clear operations
- **CCSynths** — SynthDef management with 27+ pre-built instruments organized by category: drums (kick, snare, hihat, clap, openhat, tom, rim, shaker, cowbell), bass (bass, acid, sub, reese, fmbass), leads (lead), melodic (pluck, bell, keys, strings), pads (pad), textural (noise, drone, riser), utility (click, sine, sampler, breakbeat, grains). All synths use `cc_` prefix
- **CCFX** — Ndef-based effects system with 18 effects: filters (lpf, hpf, bpf), time-based (reverb, delay, pingpong), modulation (chorus, flanger, phaser, tremolo), distortion (distortion, bitcrush, wavefold), dynamics (compressor, limiter, gate), stereo (widener, autopan). Supports routing, chaining, and sidechaining
- **CCMIDI** — MIDI device management. Multiple simultaneous synths, polyphonic/monophonic mapping, CC-to-bus mapping with configurable ranges and curves, per-synth stop
- **CCSamples** — Sample management with lazy loading from `~/.claudecollider/samples`. WAV/AIFF, rate control, directory rescanning
- **CCRecorder** — Audio recording to WAV in `~/.claudecollider/recordings` with timestamped filenames
- **CCState** — Bus and session state management, creates and tracks control/audio buses in current environment
- **CCFormatter** — Status formatting for console output
- **CCOutputs** — Hardware output routing with per-output limiters
- **CCOutput** — Single hardware output destination (mono or stereo pair) with limiter
- **CCRouter** — Effect-to-effect connections, named chains, source-to-effect routing
- **CCSidechain** — Sidechain compressor management for ducking effects
- **CCArrangement** — Declarative song arrangement sequencer. Sections as `[name, bars, elements]` arrays, Pdef/Ndef start/stop diffing, drift-free scheduling via `TempoClock.schedAbs`, live `goto` for jumping sections
- **CCBreakbeat** — Breakbeat slice sequencer. Wraps a buffer, divides into N equal slices, returns Pbind patterns with reorderable slices. Negative indices reverse individual slices. Use `bars` to set timing, `pattern` to sequence
