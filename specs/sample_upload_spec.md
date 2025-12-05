# ClaudeCollider Sample Upload — Tech Spec

## Problem

User uploads audio file to Claude → file lives in Claude's container at `/mnt/user-data/uploads/` → SuperCollider runs on user's machine → SC can't access the file.

```
Claude's Container                    User's Machine
┌─────────────────────┐              ┌─────────────────────┐
│ /mnt/user-data/     │              │ SuperCollider       │
│   uploads/          │     ╳        │ (can only read      │
│     snare.wav       │  ← can't →   │  local files)       │
└─────────────────────┘              └─────────────────────┘
```

## Solution

New MCP tool `sample_upload` that:
1. Accepts base64-encoded audio data from Claude
2. Decodes and writes to local temp folder
3. Loads into SuperCollider Buffer
4. Registers in `~cc.samples` dictionary

```
Claude                    MCP Server                SuperCollider
  │                           │                          │
  │ sample_upload(            │                          │
  │   name: "snare",          │                          │
  │   data: "UklGR..."        │                          │
  │ )                         │                          │
  │ ─────────────────────────>│                          │
  │                           │ decode base64            │
  │                           │ write to temp file       │
  │                           │ ─────────────────────────>
  │                           │ Buffer.read(path)        │
  │                           │ ~cc.samples[\snare] = buf│
  │                           │<─────────────────────────│
  │<──────────────────────────│                          │
  │ "Loaded snare (0.45s)"    │                          │
```

---

## MCP Tool: `sample_upload`

### Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | yes | Name for the sample (e.g. "kick", "break1") |
| data | string | yes | Base64-encoded audio data |
| format | string | no | File format: "wav", "aiff" (default: "wav") |

### Returns

```json
{
  "success": true,
  "name": "snare",
  "duration": 0.45,
  "channels": 2,
  "sampleRate": 44100,
  "frames": 19845
}
```

### Errors

```json
{
  "error": "Invalid audio data",
  "details": "Could not decode base64 or unsupported format"
}
```

```json
{
  "error": "Sample too large",
  "details": "Maximum size is 10MB"
}
```

### Implementation (TypeScript)

```typescript
{
  name: "sample_upload",
  description: "Upload an audio sample for use in SuperCollider",
  inputSchema: {
    type: "object",
    properties: {
      name: {
        type: "string",
        description: "Name for the sample (used as ~cc.samples[\\name])"
      },
      data: {
        type: "string", 
        description: "Base64-encoded audio data"
      },
      format: {
        type: "string",
        enum: ["wav", "aiff"],
        default: "wav"
      }
    },
    required: ["name", "data"]
  }
}
```

```typescript
async function handleSampleUpload(params: { 
  name: string, 
  data: string, 
  format?: string 
}) {
  const { name, data, format = "wav" } = params;
  
  // Validate name (alphanumeric + underscore only)
  if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)) {
    throw new Error("Invalid sample name. Use letters, numbers, underscore.");
  }
  
  // Decode base64
  const buffer = Buffer.from(data, 'base64');
  
  // Size check (10MB limit)
  if (buffer.length > 10 * 1024 * 1024) {
    throw new Error("Sample too large. Maximum size is 10MB.");
  }
  
  // Write to temp folder
  const tempDir = path.join(os.homedir(), '.claudecollider', 'samples');
  await fs.mkdir(tempDir, { recursive: true });
  
  const filePath = path.join(tempDir, `${name}.${format}`);
  await fs.writeFile(filePath, buffer);
  
  // Load into SuperCollider
  const scCode = `
    Buffer.read(s, "${filePath}", action: { |buf|
      ~cc.samples.put(\\${name}, buf);
      "Loaded ${name}: % channels, % frames, % sec".format(
        buf.numChannels, buf.numFrames, buf.duration.round(0.01)
      ).postln;
    });
  `;
  
  const result = await executeSupercollider(scCode);
  
  // Get buffer info (need to wait for async load)
  // Could use a sync approach or return immediately with path
  
  return {
    success: true,
    name,
    path: filePath,
    message: `Loaded ${name}`
  };
}
```

---

## Claude's Workflow

When user uploads an audio file:

```python
# 1. Read the uploaded file
file_path = "/mnt/user-data/uploads/snare.wav"

# 2. Base64 encode it (via bash tool)
# base64 -w 0 /mnt/user-data/uploads/snare.wav

# 3. Call sample_upload tool
sample_upload(name="snare", data="UklGRi4A...")

# 4. Now usable in patterns
sc_execute('Pdef(\\beat, Pbind(\\instrument, \\cc_sampler, \\buf, ~cc.samples[\\snare], \\dur, 1)).play')
```

---

## MCP Tool: `sample_list`

List all loaded samples.

### Parameters

None.

### Returns

```json
{
  "samples": [
    {
      "name": "kick",
      "duration": 0.32,
      "channels": 2
    },
    {
      "name": "snare", 
      "duration": 0.45,
      "channels": 2
    }
  ]
}
```

---

## MCP Tool: `sample_play`

Quick one-shot playback of a sample.

### Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | yes | Sample name |
| rate | number | no | Playback rate (default: 1) |
| amp | number | no | Amplitude (default: 0.5) |

### Returns

```json
{
  "playing": "snare",
  "rate": 1,
  "amp": 0.5
}
```

---

## MCP Tool: `sample_free`

Free a sample buffer.

### Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | yes | Sample name to free |

### Returns

```json
{
  "freed": "snare"
}
```

---

## SuperCollider Side

### cc_sampler SynthDef

```supercollider
SynthDef(\cc_sampler, { |out=0, buf=0, amp=0.5, rate=1, start=0, loop=0, attack=0.001, release=0.01, pan=0|
    var sig, env, startFrame, playRate;
    
    startFrame = start * BufFrames.kr(buf);
    playRate = rate * BufRateScale.kr(buf);
    
    sig = PlayBuf.ar(
        numChannels: 2,
        bufnum: buf,
        rate: playRate,
        startPos: startFrame,
        loop: loop,
        doneAction: (1 - loop) * 2  // free if not looping
    );
    
    // Handle mono buffers
    sig = if(BufChannels.kr(buf) == 1, sig ! 2, sig);
    
    env = EnvGen.kr(
        Env.asr(attack, 1, release),
        gate: 1,  // could add gate param for sustained playback
        doneAction: 0
    );
    
    sig = Balance2.ar(sig[0], sig[1], pan);
    
    Out.ar(out, sig * env * amp);
}).add;
```

### ~cc.samples Dictionary

```supercollider
// Initialize in CC.boot
~cc.samples = IdentityDictionary.new;

// After sample_upload writes file and loads buffer:
~cc.samples[\snare] = loadedBuffer;

// Usage in patterns
Pbind(\instrument, \cc_sampler, \buf, ~cc.samples[\snare], \dur, 1)
```

---

## Size Considerations

| Sample Type | Typical Size | Base64 Size | OK? |
|-------------|--------------|-------------|-----|
| Drum one-shot | 50-200 KB | 67-267 KB | ✓ |
| Vocal chop | 200-500 KB | 267-667 KB | ✓ |
| Short loop (2 bar) | 500 KB - 2 MB | 667 KB - 2.7 MB | ✓ |
| Long loop (8 bar) | 2-8 MB | 2.7-10.7 MB | ⚠️ |
| Full song stem | 20+ MB | 27+ MB | ✗ |

**Recommendation:** 10MB limit for now. For larger files, suggest user provides local path instead.

---

## Error Handling

### Invalid format
```json
{
  "error": "Unsupported format",
  "details": "Only WAV and AIFF supported. Got: mp3",
  "suggestion": "Convert to WAV first"
}
```

### Corrupted data
```json
{
  "error": "Invalid audio data", 
  "details": "Base64 decoded but not valid WAV/AIFF header"
}
```

### Buffer load failed
```json
{
  "error": "SuperCollider load failed",
  "details": "Buffer.read returned nil"
}
```

---

## Future Enhancements

- **Chunked upload** for large files
- **Format conversion** (mp3 → wav via ffmpeg)
- **Sample slicing** tool
- **Waveform preview** (return peak data for visualization)
- **Normalize** option on upload
