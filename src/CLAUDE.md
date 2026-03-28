# TypeScript MCP Server

## Build & Run

```bash
npm install      # Install dependencies
npm run build    # Compile TypeScript to dist/
npm start        # Run compiled server
npm run dev      # Watch mode for development
npm test         # Run tests
```

Debug mode: `DEBUG=claude-collider node dist/index.js` (logs to `/tmp/claude-collider.log`)

## Architecture

- **index.ts** — Main entry point, registers tools for Claude to call, handles all tool request routing
- **supercollider.ts** — Core class managing SuperCollider process lifecycle (boot, execute, stop, quit). Uses EventEmitter with state machine (Stopped, Booting, Running)
- **process.ts** — Spawns and manages sclang child process, handles stdin/stdout with OutputParser
- **parser.ts** — Parses SuperCollider output, detects execution results using `<<<END<<<` delimiter
- **tokenizer.ts** — Transforms multi-line SC code to single-line format (required by sclang stdin)
- **config.ts** — Configuration with environment variable overrides and auto-detection of sclang path
- **types.ts** — TypeScript types and enums (ServerState, PendingOperation)
- **debug.ts** — Debug logging utility, writes to `/tmp/claude-collider.log` when DEBUG=claude-collider
- **effects.ts** — Effects class that loads effect descriptions dynamically from SuperCollider
- **synthdefs.ts** — SynthDefs class that loads synth descriptions dynamically from SuperCollider
- **samples.ts** — Samples class wrapping sample playback, loading, and freeing operations

## Key Design Patterns

**Single-line protocol**: All SC code sent to sclang must be on a single line. The tokenizer handles this safely, preserving comments and strings.

**Result extraction**: Code is wrapped with unique markers (`<<<END<<<`) for reliable result parsing.

**State machine**: ServerState enum (Stopped, Booting, Running) controls boot sequence and prevents race conditions.

## Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `SCLANG_PATH` | Auto-detected | Path to sclang executable |
| `SC_BOOT_TIMEOUT` | 10000 | Boot timeout in ms |
| `SC_EXEC_TIMEOUT` | 2000 | Execution timeout in ms |
| `CC_SAMPLES_PATH` | `~/.claudecollider/samples` | Directory for audio samples |
| `CC_RECORDINGS_PATH` | `~/.claudecollider/recordings` | Directory for recorded audio |
