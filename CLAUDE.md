# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Claude Collider is an MCP (Model Context Protocol) server that enables Claude to generate and execute SuperCollider code in real-time for live music synthesis. It bridges Claude's natural language understanding with SuperCollider's audio synthesis capabilities.

## Build & Run Commands

```bash
npm install      # Install dependencies
npm run build    # Compile TypeScript to dist/
npm start        # Run compiled server
npm run dev      # Watch mode for development
npm test         # Run tests
```

Debug mode: `DEBUG=claude-collider node dist/index.js` (logs to `/tmp/claude-collider.log`)

## Architecture

The codebase has two main components:

### 1. TypeScript MCP Server (`/src`)

- **index.ts** - Main entry point, registers 35 tools for Claude to call, handles all tool request routing
- **supercollider.ts** - Core class managing SuperCollider process lifecycle (boot, execute, stop, quit). Uses EventEmitter with state machine (Stopped, Booting, Running)
- **process.ts** - Spawns and manages sclang child process, handles stdin/stdout with OutputParser
- **parser.ts** - Parses SuperCollider output, detects execution results using `<<<END<<<` delimiter
- **tokenizer.ts** - Transforms multi-line SC code to single-line format (required by sclang stdin)
- **config.ts** - Configuration with environment variable overrides and auto-detection of sclang path
- **types.ts** - TypeScript types and enums (ServerState, PendingOperation)
- **debug.ts** - Debug logging utility, writes to `/tmp/claude-collider.log` when DEBUG=claude-collider
- **effects.ts** - Effects class that loads effect descriptions dynamically from SuperCollider
- **synthdefs.ts** - SynthDefs class that loads synth descriptions dynamically from SuperCollider
- **samples.ts** - Samples class wrapping sample playback, loading, and freeing operations

### 2. SuperCollider Quark (`/ClaudeCollider`)

The SC backend providing live coding toolkit classes:

- **CC** - Main facade class, entry point stored in `~cc`. Manages server boot, tempo, stop/clear operations. Access subsystems via `~cc.synths`, `~cc.fx`, `~cc.midi`, `~cc.samples`, `~cc.recorder`, `~cc.state`, `~cc.formatter`, `~cc.outputs`, `~cc.router`, `~cc.sidechains`
- **CCSynths** - SynthDef management with 27+ pre-built instruments organized by category: drums (kick, snare, hihat, clap, openhat, tom, rim, shaker, cowbell), bass (bass, acid, sub, reese, fmbass), leads (lead), melodic (pluck, bell, keys, strings), pads (pad), textural (noise, drone, riser), utility (click, sine, sampler, grains). All synths use `cc_` prefix
- **CCFX** - Ndef-based effects system with 18 effects organized by type: filters (lpf, hpf, bpf), time-based (reverb, delay, pingpong), modulation (chorus, flanger, phaser, tremolo), distortion (distortion, bitcrush, wavefold), dynamics (compressor, limiter, gate), stereo (widener, autopan). Supports effect routing, chaining, and sidechaining
- **CCMIDI** - MIDI device management with polyphonic/monophonic note mapping, CC-to-bus mapping with configurable ranges and curves, event logging, and MIDI learn
- **CCSamples** - Sample management with lazy loading from `~/.claudecollider/samples`. Supports WAV/AIFF, playback with rate control, and directory rescanning
- **CCRecorder** - Audio recording to WAV files in `~/.claudecollider/recordings` with auto-generated timestamped filenames
- **CCState** - Bus and session state management, creates and tracks control/audio buses in the current environment
- **CCFormatter** - Status formatting for console output, generates server status, tempo, sample counts, playing Pdefs/Ndefs, and detailed routing debug visualization
- **CCOutputs** - Hardware output routing manager with per-output limiters. Routes sources to specific outputs or stereo pairs
- **CCOutput** - Single hardware output destination (mono or stereo pair) with limiter protection
- **CCRouter** - Effect-to-effect connections, named effect chains, and source-to-effect routing
- **CCSidechain** - Sidechain compressor management for ducking effects (e.g., kick ducking bass)

## Key Design Patterns

**Single-line protocol**: All SC code sent to sclang must be on a single line. The tokenizer handles this safely, preserving comments and strings.

**Result extraction**: Code is wrapped with unique markers (`<<<END<<<`) for reliable result parsing.

**State machine**: ServerState enum (Stopped, Booting, Running) controls boot sequence and prevents race conditions.

## Environment Variables

| Variable             | Default                        | Description                  |
| -------------------- | ------------------------------ | ---------------------------- |
| `SCLANG_PATH`        | Auto-detected                  | Path to sclang executable    |
| `SC_BOOT_TIMEOUT`    | 10000                          | Boot timeout in ms           |
| `SC_EXEC_TIMEOUT`    | 2000                           | Execution timeout in ms      |
| `CC_SAMPLES_PATH`    | `~/.claudecollider/samples`    | Directory for audio samples  |
| `CC_RECORDINGS_PATH` | `~/.claudecollider/recordings` | Directory for recorded audio |
