# Vibe Music Server - Implementation Plan

## Overview

An MCP (Model Context Protocol) server that enables natural language interaction with SuperCollider for live music coding via Claude Desktop. Users describe sounds, beats, or musical ideas to Claude, and the server executes the corresponding SuperCollider code in real-time.

## Architecture

```
┌────────────────┐      MCP (stdio)      ┌──────────────────┐
│ Claude Desktop │◄─────────────────────►│  MCP Server      │
│                │                       │  (Node.js/TS)    │
└────────────────┘                       └────────┬─────────┘
                                                  │
                                         spawn + stdin/stdout
                                                  │
                                                  ▼
                                         ┌──────────────────┐
                                         │     sclang       │
                                         │  (interpreter)   │
                                         └────────┬─────────┘
                                                  │
                                              internal
                                                  │
                                                  ▼
                                         ┌──────────────────┐
                                         │     scsynth      │
                                         │  (audio server)  │
                                         └──────────────────┘
```

## Technology Stack

- **Language**: TypeScript
- **Runtime**: Node.js (ES Modules)
- **Protocol**: MCP via `@modelcontextprotocol/sdk`
- **Audio Engine**: SuperCollider (sclang + scsynth)

## File Structure

```
claude-collider/
├── package.json
├── tsconfig.json
├── PLAN.md
├── TASKS.md
├── src/
│   ├── index.ts              # MCP server entry point
│   ├── supercollider.ts      # SC process management
│   ├── synthdefs.ts          # Pre-built synth definitions (Phase 3)
│   ├── patterns.ts           # Musical pattern helpers (Phase 3)
│   └── utils.ts              # Shared utilities (as needed)
└── dist/                     # Compiled JavaScript output
```

## Core Components

### 1. SuperCollider Class (`src/supercollider.ts`)

Manages the sclang subprocess and all communication with SuperCollider.

**Class Interface:**
```typescript
class SuperCollider {
  private process: ChildProcess | null;
  private outputBuffer: string;
  private isServerBooted: boolean;

  async boot(): Promise<string>           // Start sclang, boot scsynth
  async execute(code: string): Promise<string>  // Run SC code, return output
  async stop(): Promise<void>             // Stop all sounds (CmdPeriod)
  async quit(): Promise<void>             // Terminate sclang process
  isRunning(): boolean                    // Check if server is active
}
```

**Key Implementation Details:**

1. **Process Spawning**
   - Spawn `sclang` with `-i` flag for IDE mode (cleaner output parsing)
   - Capture stdout and stderr separately
   - Handle process exit/error events

2. **Marker-Based Output Protocol**
   - Wrap user code with unique markers to detect execution boundaries:
     ```supercollider
     ">>>BEGIN>>>".postln;
     (
       // user code here
     ).value;
     "<<<END<<<".postln;
     ```
   - Buffer stdout continuously
   - Extract content between markers when `<<<END<<<` is detected
   - Return extracted content as execution result

3. **Server Boot Sequence**
   - Execute boot code that waits for server ready:
     ```supercollider
     s.waitForBoot { "SERVER_READY".postln };
     ```
   - Wait for `SERVER_READY` marker in output
   - Set `isServerBooted` flag

4. **Stop All Sounds**
   - Execute `CmdPeriod.run` to stop all synths/patterns

### 2. MCP Server (`src/index.ts`)

The main entry point implementing MCP protocol over stdio.

**Server Setup:**
```typescript
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const server = new Server({
  name: "vibe-music",
  version: "0.1.0"
}, {
  capabilities: {
    tools: {}
  }
});
```

**Tool Definitions:**

1. **`sc_boot`**
   - Description: Start SuperCollider and boot the audio server
   - Input: None
   - Output: Boot status message
   - Implementation: Call `supercollider.boot()`

2. **`sc_execute`**
   - Description: Execute SuperCollider code
   - Input: `{ code: string }`
   - Output: Execution result or error
   - Implementation: Call `supercollider.execute(code)`

3. **`sc_stop`**
   - Description: Stop all currently playing sounds
   - Input: None
   - Output: Confirmation message
   - Implementation: Call `supercollider.stop()`

4. **`sc_load_synthdef`** (Phase 3)
   - Description: Load a pre-built synth definition
   - Input: `{ name: string }` (from enum of available synthdefs)
   - Output: Confirmation message
   - Implementation: Execute synthdef code from library

5. **`sc_status`** (Phase 3)
   - Description: Get server status (CPU, synth count)
   - Input: None
   - Output: Status object
   - Implementation: Query `s.numSynths`, `s.avgCPU`

6. **`sc_free_all`** (Phase 2)
   - Description: Free all synth nodes
   - Input: None
   - Output: Confirmation
   - Implementation: Execute `s.freeAll`

### 3. SynthDef Library (`src/synthdefs.ts`) - Phase 3

Pre-built synthesizer definitions organized by category:

**Categories:**
- **Drums**: kick, snare, hihat, clap, tom
- **Bass**: sub, acid, reese
- **Pads**: ambient, strings
- **Lead**: pluck, sine, saw

**Format:**
```typescript
export const synthDefs: Record<string, string> = {
  kick: `
    SynthDef(\\kick, { |out=0, freq=60, amp=0.5|
      var sig = SinOsc.ar(freq * EnvGen.kr(Env.perc(0.001, 0.3), doneAction: 2));
      sig = sig * EnvGen.kr(Env.perc(0.001, 0.5));
      Out.ar(out, sig ! 2 * amp);
    }).add;
  `,
  // ... more definitions
};
```

## Configuration

**Environment Variables (optional):**
```bash
SCLANG_PATH=/path/to/sclang    # Custom sclang location (default: 'sclang' in PATH)
SC_BOOT_TIMEOUT=30000          # Server boot timeout in ms (default: 30000)
SC_EXEC_TIMEOUT=5000           # Code execution timeout in ms (default: 5000)
DEBUG=vibe-music               # Enable debug logging
```

**Claude Desktop Configuration:**
Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "vibe-music": {
      "command": "node",
      "args": ["/absolute/path/to/claude-collider/dist/index.js"]
    }
  }
}
```

## Error Handling Strategy

| Error | Detection | Response |
|-------|-----------|----------|
| sclang not found | Spawn error ENOENT | Return helpful install message |
| Server boot timeout | No SERVER_READY within timeout | Return boot failure message |
| Execution timeout | No <<<END<<< within timeout | Kill execution, return timeout error |
| SC syntax error | ERROR in stderr | Parse and return friendly error |
| Process crash | 'exit' event | Set flags, return crash message |

## Example Interactions

**Simple Tone:**
```
User: "Play a sine wave at 440hz"
→ sc_execute: { SinOsc.ar(440, 0, 0.3) ! 2 }.play;
```

**Drum Pattern:**
```
User: "Make a four-on-the-floor kick pattern"
→ sc_load_synthdef: { name: "kick" }
→ sc_execute: Pbind(\instrument, \kick, \dur, 1).play;
```

**Stop:**
```
User: "Stop the music"
→ sc_stop
```

## Future Enhancements (Beyond Phase 4)

- **Recording**: Tool to record audio output to file
- **MIDI**: Tool to list/connect MIDI devices
- **Visualization**: Return scope/spectrum data as base64 images
- **Session State**: Save/load current synths and patterns
- **Buffer Management**: Load and manage audio samples
