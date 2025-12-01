# Vibe Music Server - Task List

## Phase 1: Core Infrastructure ✅
**Goal**: Get basic boot, execute, and stop working end-to-end.

### 1.1 Project Setup ✅
- [x] Initialize npm project with `npm init`
- [x] Install production dependencies:
  - [x] `@modelcontextprotocol/sdk`
- [x] Install dev dependencies:
  - [x] `typescript`
  - [x] `@types/node`
- [x] Create `tsconfig.json` with:
  - [x] ES module output (`"module": "NodeNext"`)
  - [x] Node target (`"target": "ES2022"`)
  - [x] Strict mode enabled
  - [x] Output to `dist/` directory
- [x] Add npm scripts to `package.json`:
  - [x] `build`: `tsc`
  - [x] `start`: `node dist/index.js`
  - [x] `dev`: `tsc --watch`
- [x] Create `src/` directory

### 1.2 SuperCollider Class ✅
- [x] Create `src/supercollider.ts`
- [x] Implement class structure with private properties:
  - [x] `process: ChildProcess | null`
  - [x] `outputBuffer: string` (via OutputParser)
  - [x] `isServerBooted: boolean` (via ServerState)
- [x] Implement `boot()` method:
  - [x] Spawn sclang process
  - [x] Set up stdout/stderr handlers
  - [x] Wait for "Welcome to SuperCollider" before sending boot command
  - [x] Send boot command: `s.waitForBoot { "SERVER_READY".postln };`
  - [x] Wait for `SERVER_READY` in output
  - [x] Return success message
- [x] Implement `execute(code)` method:
  - [x] Wrap code with `>>>BEGIN>>>` and `<<<END<<<` markers
  - [x] Write wrapped code to sclang stdin
  - [x] Buffer output until `<<<END<<<` detected
  - [x] Extract and return content between markers
- [x] Implement `stop()` method:
  - [x] Execute `CmdPeriod.run`
- [x] Implement `quit()` method:
  - [x] Kill sclang process
  - [x] Reset state
- [x] Implement `isRunning()` getter
- [x] Export class
- [x] Refactor into modules: debug.ts, types.ts, config.ts, parser.ts

### 1.3 MCP Server ✅
- [x] Create `src/index.ts`
- [x] Import MCP SDK components:
  - [x] `Server` from server module
  - [x] `StdioServerTransport` from stdio module
  - [x] Types: `CallToolRequestSchema`, `ListToolsRequestSchema`
- [x] Import `SuperCollider` class
- [x] Create global SuperCollider instance
- [x] Create MCP Server with name and version
- [x] Implement `tools/list` handler returning tool definitions:
  - [x] `sc_boot` tool definition
  - [x] `sc_execute` tool definition with code parameter
  - [x] `sc_stop` tool definition
- [x] Implement `tools/call` handler:
  - [x] Handle `sc_boot`: call `supercollider.boot()`
  - [x] Handle `sc_execute`: call `supercollider.execute(args.code)`
  - [x] Handle `sc_stop`: call `supercollider.stop()`
  - [x] Return results in MCP format
- [x] Set up stdio transport and connect
- [x] Handle process signals for graceful shutdown

### 1.4 Build and Test ✅
- [x] Run `npm run build` - verify no TypeScript errors
- [x] Manual test sequence:
  - [x] Boot server
  - [x] Execute SC code
  - [x] Verify audio output
  - [x] Stop sounds

---

## Phase 2: Robustness ✅
**Goal**: Handle errors gracefully, add timeouts, improve reliability.

### 2.1 Timeout Handling ✅
- [x] Add configurable timeout constants (from env vars or defaults)
- [x] Implement boot timeout in `boot()` method:
  - [x] Reject promise if SERVER_READY not received within timeout
  - [x] Clean up process on timeout
- [x] Implement execution timeout in `execute()` method:
  - [x] Reject promise if <<<END<<< not received within timeout

### 2.2 Error Parsing ✅
- [x] Capture stderr from sclang process
- [x] Detect ERROR patterns in output
- [x] Parse SuperCollider error messages:
  - [x] Extract line numbers
  - [x] Extract error type
- [x] Return user-friendly error messages

### 2.3 Process Recovery ✅
- [x] Handle sclang 'exit' event:
  - [x] Update state flags
  - [x] Log crash information
- [x] Handle sclang 'error' event
- [ ] Implement auto-restart option (optional)
- [x] Add `restart()` method for manual recovery

### 2.4 Graceful Shutdown ✅
- [x] Handle SIGINT/SIGTERM in MCP server
- [x] Call `supercollider.quit()` on shutdown
- [x] Ensure sclang process is terminated

### 2.5 Additional Tools ✅
- [x] Add `sc_free_all` tool:
  - [x] Execute `s.freeAll`
  - [x] Nuclear option for stuck synths
- [x] Add `sc_restart` tool:
  - [x] Quit and reboot SuperCollider
  - [x] Recovery from crashes

---

## Phase 3: Convenience Features ✅
**Goal**: Add synthdef library and helper tools.

### 3.1 SynthDef Library ✅
- [x] Create `src/synthdefs.ts`
- [x] Implement drum synthdefs:
  - [x] `kick` - basic kick drum
  - [x] `snare` - snare with noise
  - [x] `hihat` - closed hi-hat
  - [x] `clap` - clap/snare layer
- [x] Implement bass synthdefs:
  - [x] `bass` - simple sub bass
  - [x] `acid` - resonant filter bass
- [x] Implement pad synthdefs:
  - [x] `pad` - simple ambient pad
- [x] Export synthdef registry with names and code

### 3.2 Load SynthDef Tool ✅
- [x] Add `sc_load_synthdef` tool definition:
  - [x] Input: `name` parameter with enum of available synthdefs
  - [x] Description explaining available synths
- [x] Implement handler:
  - [x] Look up synthdef by name
  - [x] Execute synthdef code
  - [x] Return confirmation

### 3.3 Status Tool ✅
- [x] Add `sc_status` tool definition
- [x] Implement handler:
  - [x] Query `s.numSynths`
  - [x] Query `s.avgCPU`
  - [x] Query `s.peakCPU`
  - [x] Return formatted status

### 3.4 Pattern Helpers (Optional)
- [ ] Create `src/patterns.ts`
- [ ] Add common drum patterns as Pbind templates
- [ ] Add scale/chord helpers

---

## Phase 4: Polish ✅
**Goal**: Add discoverability features and documentation.

### 4.1 MCP Resources ✅
- [x] Add resources capability to server
- [x] Implement `supercollider://synthdefs` resource:
  - [x] List all available synthdefs with descriptions
- [x] Implement `supercollider://examples` resource:
  - [x] Common code snippets for reference

### 4.2 Debug Logging ✅
- [x] Add debug logging to file (/tmp/claude-collider.log)
- [x] Log sclang stdout/stderr when debugging
- [x] Log boot sequence and state transitions
- [x] Make debug conditional on DEBUG=claude-collider env var

### 4.3 Documentation ✅
- [x] Update README.md with:
  - [x] Installation instructions
  - [x] Claude Desktop configuration
  - [x] Available tools reference
  - [x] Example prompts
- [x] Environment variables documented

### 4.4 Final Testing (Manual)
- [ ] Test full workflow in Claude Desktop
- [ ] Test error scenarios
- [ ] Test long-running patterns
- [ ] Test rapid start/stop cycles

---

## Completion Checklist

### Phase 1 Complete ✅
- [x] Can boot SuperCollider from Claude Desktop
- [x] Can execute arbitrary SC code
- [x] Can stop all sounds
- [x] No TypeScript errors
- [x] Basic error messages work

### Phase 2 Complete When:
- [x] Timeouts prevent hangs
- [x] Errors are parsed and readable
- [x] Server survives sclang crashes
- [x] Clean shutdown works

### Phase 3 Complete When:
- [x] Can load pre-built synthdefs
- [x] Can check server status
- [x] At least 6 synthdefs available (7 total: kick, snare, hihat, clap, bass, acid, pad)

### Phase 4 Complete When:
- [x] Resources show in Claude Desktop
- [x] Debug mode works
- [x] README is complete
- [x] Ready for others to use
