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
```

Debug mode: `DEBUG=claude-collider node dist/index.js` (logs to `/tmp/claude-collider.log`)

## Architecture

The codebase has two main components:

### 1. TypeScript MCP Server (`/src`)

- **index.ts** - Main entry point, registers 38+ tools for Claude to call, handles all tool request routing
- **supercollider.ts** - Core class managing SuperCollider process lifecycle (boot, execute, stop, quit). Uses EventEmitter with state machine (Stopped, Booting, Running)
- **process.ts** - Spawns and manages sclang child process, handles stdin/stdout with OutputParser
- **parser.ts** - Parses SuperCollider output, detects execution results using `<<<END<<<` delimiter
- **tokenizer.ts** - Transforms multi-line SC code to single-line format (required by sclang stdin)
- **config.ts** - Configuration with environment variable overrides and auto-detection of sclang path

### 2. SuperCollider Quark (`/ClaudeCollider`)

The SC backend providing live coding toolkit classes:

- **CC** - Main facade, access subsystems via `~cc.synths`, `~cc.fx`, `~cc.midi`, `~cc.samples`, `~cc.recorder`
- **CCSynths** - Pre-built SynthDefs (kick, snare, bass, lead, pad, etc.) with `cc_` prefix
- **CCFX** - Ndef-based effects system with routing and chaining
- **CCMIDI** - MIDI device management and mapping
- **CCSamples** - Sample management with lazy loading
- **CCRecorder** - Audio recording to WAV files
- **CCState** - Bus and state management

## Key Design Patterns

**Single-line protocol**: All SC code sent to sclang must be on a single line. The tokenizer handles this safely, preserving comments and strings.

**Result extraction**: Code is wrapped with unique markers (`<<<END<<<`) for reliable result parsing.

**State machine**: ServerState enum (Stopped, Booting, Running) controls boot sequence and prevents race conditions.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SCLANG_PATH` | Auto-detected | Path to sclang executable |
| `SC_BOOT_TIMEOUT` | 10000 | Boot timeout in ms |
| `SC_EXEC_TIMEOUT` | 2000 | Execution timeout in ms |
| `CC_SAMPLES_PATH` | `~/.claudecollider/samples` | Directory for audio samples |
| `CC_RECORDINGS_PATH` | `~/.claudecollider/recordings` | Directory for recorded audio |
