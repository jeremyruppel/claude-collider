# Vibe Music Server - Task List

## Phase 1: Core Infrastructure
**Goal**: Get basic boot, execute, and stop working end-to-end.

### 1.1 Project Setup
- [ ] Initialize npm project with `npm init`
- [ ] Install production dependencies:
  - [ ] `@modelcontextprotocol/sdk`
- [ ] Install dev dependencies:
  - [ ] `typescript`
  - [ ] `@types/node`
- [ ] Create `tsconfig.json` with:
  - [ ] ES module output (`"module": "NodeNext"`)
  - [ ] Node target (`"target": "ES2022"`)
  - [ ] Strict mode enabled
  - [ ] Output to `dist/` directory
- [ ] Add npm scripts to `package.json`:
  - [ ] `build`: `tsc`
  - [ ] `start`: `node dist/index.js`
  - [ ] `dev`: `tsc --watch`
- [ ] Create `src/` directory

### 1.2 SuperCollider Class
- [ ] Create `src/supercollider.ts`
- [ ] Implement class structure with private properties:
  - [ ] `process: ChildProcess | null`
  - [ ] `outputBuffer: string`
  - [ ] `isServerBooted: boolean`
- [ ] Implement `boot()` method:
  - [ ] Spawn sclang process with `-i` flag
  - [ ] Set up stdout/stderr handlers
  - [ ] Send boot command: `s.waitForBoot { "SERVER_READY".postln };`
  - [ ] Wait for `SERVER_READY` in output
  - [ ] Return success message
- [ ] Implement `execute(code)` method:
  - [ ] Wrap code with `>>>BEGIN>>>` and `<<<END<<<` markers
  - [ ] Write wrapped code to sclang stdin
  - [ ] Buffer output until `<<<END<<<` detected
  - [ ] Extract and return content between markers
- [ ] Implement `stop()` method:
  - [ ] Execute `CmdPeriod.run`
- [ ] Implement `quit()` method:
  - [ ] Kill sclang process
  - [ ] Reset state
- [ ] Implement `isRunning()` getter
- [ ] Export class

### 1.3 MCP Server
- [ ] Create `src/index.ts`
- [ ] Import MCP SDK components:
  - [ ] `Server` from server module
  - [ ] `StdioServerTransport` from stdio module
  - [ ] Types: `CallToolRequestSchema`, `ListToolsRequestSchema`
- [ ] Import `SuperCollider` class
- [ ] Create global SuperCollider instance
- [ ] Create MCP Server with name and version
- [ ] Implement `tools/list` handler returning tool definitions:
  - [ ] `sc_boot` tool definition
  - [ ] `sc_execute` tool definition with code parameter
  - [ ] `sc_stop` tool definition
- [ ] Implement `tools/call` handler:
  - [ ] Handle `sc_boot`: call `supercollider.boot()`
  - [ ] Handle `sc_execute`: call `supercollider.execute(args.code)`
  - [ ] Handle `sc_stop`: call `supercollider.stop()`
  - [ ] Return results in MCP format
- [ ] Set up stdio transport and connect
- [ ] Handle process signals for graceful shutdown

### 1.4 Build and Test
- [ ] Run `npm run build` - verify no TypeScript errors
- [ ] Test with MCP Inspector (if available)
- [ ] Manual test sequence:
  - [ ] Boot server
  - [ ] Execute: `{ SinOsc.ar(440, 0, 0.3) ! 2 }.play`
  - [ ] Verify audio output
  - [ ] Stop sounds
  - [ ] Execute another sound
  - [ ] Verify it works after stop

---

## Phase 2: Robustness
**Goal**: Handle errors gracefully, add timeouts, improve reliability.

### 2.1 Timeout Handling
- [ ] Add configurable timeout constants (from env vars or defaults)
- [ ] Implement boot timeout in `boot()` method:
  - [ ] Reject promise if SERVER_READY not received within timeout
  - [ ] Clean up process on timeout
- [ ] Implement execution timeout in `execute()` method:
  - [ ] Reject promise if <<<END<<< not received within timeout
  - [ ] Option to kill and restart sclang on timeout

### 2.2 Error Parsing
- [ ] Capture stderr from sclang process
- [ ] Detect ERROR patterns in output
- [ ] Parse SuperCollider error messages:
  - [ ] Extract line numbers
  - [ ] Extract error type
- [ ] Return user-friendly error messages

### 2.3 Process Recovery
- [ ] Handle sclang 'exit' event:
  - [ ] Update state flags
  - [ ] Log crash information
- [ ] Handle sclang 'error' event
- [ ] Implement auto-restart option (optional, may be too aggressive)
- [ ] Add `restart()` method for manual recovery

### 2.4 Graceful Shutdown
- [ ] Handle SIGINT/SIGTERM in MCP server
- [ ] Call `supercollider.quit()` on shutdown
- [ ] Ensure sclang process is terminated

### 2.5 Additional Tools
- [ ] Add `sc_free_all` tool:
  - [ ] Execute `s.freeAll`
  - [ ] Nuclear option for stuck synths

---

## Phase 3: Convenience Features
**Goal**: Add synthdef library and helper tools.

### 3.1 SynthDef Library
- [ ] Create `src/synthdefs.ts`
- [ ] Implement drum synthdefs:
  - [ ] `kick` - basic kick drum
  - [ ] `snare` - snare with noise
  - [ ] `hihat` - closed hi-hat
  - [ ] `clap` - clap/snare layer
- [ ] Implement bass synthdefs:
  - [ ] `bass` - simple sub bass
  - [ ] `acid` - resonant filter bass
- [ ] Implement pad synthdefs:
  - [ ] `pad` - simple ambient pad
- [ ] Export synthdef registry with names and code

### 3.2 Load SynthDef Tool
- [ ] Add `sc_load_synthdef` tool definition:
  - [ ] Input: `name` parameter with enum of available synthdefs
  - [ ] Description explaining available synths
- [ ] Implement handler:
  - [ ] Look up synthdef by name
  - [ ] Execute synthdef code
  - [ ] Return confirmation

### 3.3 Status Tool
- [ ] Add `sc_status` tool definition
- [ ] Implement handler:
  - [ ] Query `s.numSynths`
  - [ ] Query `s.avgCPU`
  - [ ] Query `s.peakCPU`
  - [ ] Return formatted status

### 3.4 Pattern Helpers (Optional)
- [ ] Create `src/patterns.ts`
- [ ] Add common drum patterns as Pbind templates
- [ ] Add scale/chord helpers

---

## Phase 4: Polish
**Goal**: Add discoverability features and documentation.

### 4.1 MCP Resources
- [ ] Add resources capability to server
- [ ] Implement `supercollider://synthdefs` resource:
  - [ ] List all available synthdefs with descriptions
- [ ] Implement `supercollider://examples` resource:
  - [ ] Common code snippets for reference
- [ ] Implement `supercollider://status` resource:
  - [ ] Live server status

### 4.2 Debug Logging
- [ ] Add DEBUG environment variable support
- [ ] Log sclang stdout/stderr when debugging
- [ ] Log MCP tool calls when debugging
- [ ] Ensure logs go to stderr (not stdout, which is MCP transport)

### 4.3 Documentation
- [ ] Update README.md with:
  - [ ] Installation instructions
  - [ ] Claude Desktop configuration
  - [ ] Available tools reference
  - [ ] Example prompts
- [ ] Add inline code comments where helpful

### 4.4 Final Testing
- [ ] Test full workflow in Claude Desktop
- [ ] Test error scenarios
- [ ] Test long-running patterns
- [ ] Test rapid start/stop cycles

---

## Completion Checklist

### Phase 1 Complete When:
- [ ] Can boot SuperCollider from Claude Desktop
- [ ] Can execute arbitrary SC code
- [ ] Can stop all sounds
- [ ] No TypeScript errors
- [ ] Basic error messages work

### Phase 2 Complete When:
- [ ] Timeouts prevent hangs
- [ ] Errors are parsed and readable
- [ ] Server survives sclang crashes
- [ ] Clean shutdown works

### Phase 3 Complete When:
- [ ] Can load pre-built synthdefs
- [ ] Can check server status
- [ ] At least 6 synthdefs available

### Phase 4 Complete When:
- [ ] Resources show in Claude Desktop
- [ ] Debug mode works
- [ ] README is complete
- [ ] Ready for others to use
