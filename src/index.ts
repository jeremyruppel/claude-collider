#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js"
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js"
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js"
import { SuperCollider } from "./supercollider.js"
import { SynthDefs } from "./synthdefs.js"
import { Effects } from "./effects.js"
import { Samples } from "./samples.js"

const sc = new SuperCollider()
const samples = new Samples(sc)
const synthdefs = new SynthDefs(sc)
const effects = new Effects(sc)

async function formatBootReadme(): Promise<string> {
  return `# ClaudeCollider

SuperCollider is ready for sound synthesis and music creation.

## Quick Start

Play a synth:
  Synth(\\cc_kick)
  Synth(\\cc_lead, [freq: 440, amp: 0.3])

Create a pattern:
  Pdef(\\beat, Pbind(\\instrument, \\cc_kick, \\dur, 0.5)).play

Stop all sounds:
  Use the sc_stop tool

## Using sc_execute

The sc_execute tool runs SuperCollider code. Always send code as a single line.
Use semicolons to separate statements: { var x = 1; x + 1 }.value

You can create your own synths and effects using sclang:
  SynthDef(\\mySynth, { |out=0, freq=440| Out.ar(out, SinOsc.ar(freq) * 0.2) }).add

## Available Synths

${synthdefs.format()}

## Available Effects

${effects.format()}

## Samples

${await samples.format()}

Play a sample once (also loads the buffer):
  ~cc.samples.play("sampleName")

Use in a pattern (after playing once to load):
  Pdef(\\beat, Pbind(\\instrument, \\cc_sampler, \\buf, ~cc.samples.at("sampleName"), \\dur, 1)).play

## Effect Routing

Load an effect:
  fx_load with name (e.g. "reverb")

Route a pattern through it:
  fx_route with source and target

Chain multiple effects:
  fx_chain with name and effects array

## Tips

- All built-in synths are prefixed with \\cc_ (e.g. \\cc_kick, \\cc_bass)
- Use sc_tempo to set BPM for patterns
- Use sc_status to see what's playing
- Use sc_clear to reset everything

## Live Coding Conventions

Always use proxy objects for live-updatable sounds:
- Pdef: Patterns (beats, sequences) — crossfades at next loop point
- Ndef: Continuous synths (drones, pads, effects) — crossfades over fadeTime
- Tdef: Tasks (algorithmic sequences) — restarts with new definition

### Drum Patterns and Frequency

Pbind auto-sets \\freq from \\degree (default ~261 Hz), overriding synth defaults.

Fix by explicitly setting freq:
  Pdef(\\kick, Pbind(\\instrument, \\cc_kick, \\dur, 1, \\freq, 48)).play

### Quantization

Set Pdef.defaultQuant = 4 so changes land on bar boundaries.

### Quick Reference

Start:  Pdef(\\x).play / Ndef(\\x).play
Stop:   Pdef(\\x).stop / Ndef(\\x).stop
Tempo:  TempoClock.default.tempo = 120/60`
}

const server = new Server(
  {
    name: "claude-collider",
    version: "0.1.0",
  },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
)

// Helper to format SC params from object
function formatParams(params: Record<string, unknown>): string {
  return Object.entries(params)
    .map(([k, v]) => `\\${k}, ${v}`)
    .join(", ")
}

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "sc_boot",
        description:
          "Boot ClaudeCollider and the SuperCollider audio server. Must be called before playing any sounds.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description:
                "Audio device name (e.g. 'BlackHole 2ch'). Omit for default device.",
            },
          },
          required: [],
        },
      },
      {
        name: "sc_execute",
        description:
          "Execute SuperCollider code. Use for playing sounds, defining synths, creating patterns, etc.",
        inputSchema: {
          type: "object",
          properties: {
            code: {
              type: "string",
              description: "SuperCollider code to execute",
            },
          },
          required: ["code"],
        },
      },
      {
        name: "sc_stop",
        description:
          "Stop all currently playing sounds (equivalent to Cmd+Period in SuperCollider IDE)",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_status",
        description:
          "Get ClaudeCollider status: tempo, synths, CPU, active patterns.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_tempo",
        description: "Get or set the tempo in BPM.",
        inputSchema: {
          type: "object",
          properties: {
            bpm: {
              type: "number",
              description: "Tempo in BPM. Omit to get current tempo.",
            },
          },
          required: [],
        },
      },
      {
        name: "sc_clear",
        description:
          "Stop all sounds and clear all patterns, effects, and MIDI mappings.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_reboot",
        description:
          "Reboot the audio server with optional new device. Use to change audio device at runtime.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description: "Audio device name. Omit to keep current device.",
            },
          },
          required: [],
        },
      },
      {
        name: "sc_audio_devices",
        description: "List available audio input and output devices.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      // MIDI tools
      {
        name: "midi_list_devices",
        description: "List available MIDI input and output devices.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "midi_connect",
        description:
          "Connect to a MIDI device by name or index. Use 'all' for direction to connect all inputs.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description: "Device name or index number",
            },
            direction: {
              type: "string",
              enum: ["in", "out", "all"],
              description: "Input, output, or all inputs",
            },
          },
          required: ["device", "direction"],
        },
      },
      {
        name: "midi_map_notes",
        description:
          "Map MIDI note input to trigger a synth. The synth should accept 'freq', 'amp', and 'gate' arguments.",
        inputSchema: {
          type: "object",
          properties: {
            synthName: {
              type: "string",
              description:
                "Name of the SynthDef to trigger (without cc_ prefix)",
            },
            channel: {
              type: "number",
              description:
                "MIDI channel 0-15. Omit to respond to all channels.",
            },
            velocityToAmp: {
              type: "boolean",
              description: "Map note velocity to amplitude (default: true)",
            },
            mono: {
              type: "boolean",
              description:
                "Monophonic mode - only one note at a time (default: false)",
            },
          },
          required: ["synthName"],
        },
      },
      {
        name: "midi_map_cc",
        description:
          "Map a MIDI CC to a control bus that can modulate synth parameters.",
        inputSchema: {
          type: "object",
          properties: {
            cc: {
              type: "number",
              description: "CC number 0-127",
            },
            busName: {
              type: "string",
              description:
                "Name for the control bus (e.g. 'cutoff'). Will be stored as ~busName.",
            },
            range: {
              type: "array",
              items: { type: "number" },
              description: "Output range [min, max] (default: [0, 1])",
            },
            curve: {
              type: "string",
              enum: ["lin", "exp"],
              description: "Mapping curve (default: lin)",
            },
            channel: {
              type: "number",
              description: "MIDI channel 0-15. Omit for all channels.",
            },
          },
          required: ["cc", "busName"],
        },
      },
      {
        name: "midi_clear",
        description: "Clear all MIDI mappings.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      // Effects tools
      {
        name: "fx_load",
        description:
          "Load a pre-built audio effect. Query supercollider://effects resource for available effects. Returns the effect's input bus for routing audio.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Effect name (e.g. reverb, delay, distortion)",
            },
            slot: {
              type: "string",
              description: "Ndef slot name (default: fx_<name>)",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "fx_set",
        description: "Set parameters on a loaded effect.",
        inputSchema: {
          type: "object",
          properties: {
            slot: {
              type: "string",
              description: "Effect slot name",
            },
            params: {
              type: "object",
              description: "Parameter name/value pairs",
            },
          },
          required: ["slot", "params"],
        },
      },
      {
        name: "fx_chain",
        description:
          "Create a chain of effects in series. Returns the input bus for the chain.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Name for this chain",
            },
            effects: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  name: {
                    type: "string",
                    description: "Effect name",
                  },
                  params: {
                    type: "object",
                    description: "Initial parameters",
                  },
                },
                required: ["name"],
              },
              description: "Ordered list of effects",
            },
          },
          required: ["name", "effects"],
        },
      },
      {
        name: "fx_route",
        description:
          "Route a sound source (Pdef or Ndef) to an effect or chain.",
        inputSchema: {
          type: "object",
          properties: {
            source: {
              type: "string",
              description: "Pdef or Ndef name",
            },
            target: {
              type: "string",
              description: "Effect slot or chain name",
            },
          },
          required: ["source", "target"],
        },
      },
      {
        name: "fx_sidechain",
        description:
          "Create a sidechain compressor. Routes audio through compression triggered by a separate signal (e.g. kick ducking bass).",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Name for this sidechain (e.g. 'bassDuck')",
            },
            threshold: {
              type: "number",
              description: "Compression threshold 0-1 (default: 0.1)",
            },
            ratio: {
              type: "number",
              description: "Compression ratio 1-20 (default: 4)",
            },
            attack: {
              type: "number",
              description: "Attack time in seconds (default: 0.01)",
            },
            release: {
              type: "number",
              description: "Release time in seconds (default: 0.1)",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "fx_route_trigger",
        description: "Route a source to the trigger input of a sidechain.",
        inputSchema: {
          type: "object",
          properties: {
            source: {
              type: "string",
              description: "Source Pdef or Ndef name (e.g. kick pattern)",
            },
            sidechain: {
              type: "string",
              description: "Sidechain name",
            },
          },
          required: ["source", "sidechain"],
        },
      },
      {
        name: "fx_bypass",
        description: "Bypass an effect (pass audio through unchanged).",
        inputSchema: {
          type: "object",
          properties: {
            slot: {
              type: "string",
              description: "Effect slot name",
            },
            bypass: {
              type: "boolean",
              description: "true to bypass, false to re-enable (default: true)",
            },
          },
          required: ["slot"],
        },
      },
      {
        name: "fx_remove",
        description: "Remove an effect and free its resources.",
        inputSchema: {
          type: "object",
          properties: {
            slot: {
              type: "string",
              description: "Effect slot, chain, or sidechain name",
            },
          },
          required: ["slot"],
        },
      },
      {
        name: "fx_list",
        description: "List all loaded effects, chains, and sidechains.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      // Sample tools
      {
        name: "sample_list",
        description: "List all loaded samples with duration and channel info.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sample_play",
        description: "Play a loaded sample once.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Sample name",
            },
            rate: {
              type: "number",
              description: "Playback rate (default: 1, negative for reverse)",
            },
            amp: {
              type: "number",
              description: "Amplitude 0-1 (default: 0.5)",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "sample_free",
        description: "Free a sample buffer and remove it from memory.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Sample name to free",
            },
          },
          required: ["name"],
        },
      },
    ],
  }
})

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params

  try {
    switch (name) {
      case "sc_boot": {
        const { device } = args as { device?: string }
        const deviceArg = device ? `"${device}"` : "nil"
        await sc.boot()
        await sc.execute(`~cc = CC.boot(device: ${deviceArg})`)
        await synthdefs.load()
        await effects.load()
        await samples.load()
        return {
          content: [{ type: "text", text: await formatBootReadme() }],
        }
      }

      case "sc_execute": {
        const code = (args as { code: string }).code
        if (!code) {
          return {
            content: [
              { type: "text", text: "Error: code parameter is required" },
            ],
            isError: true,
          }
        }
        const result = await sc.execute(code)
        return {
          content: [{ type: "text", text: result }],
        }
      }

      case "sc_stop": {
        await sc.execute("~cc.stop")
        return {
          content: [{ type: "text", text: "Stopped all sounds" }],
        }
      }

      case "sc_status": {
        const result = await sc.execute("~cc.status")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      case "sc_tempo": {
        const { bpm } = args as { bpm?: number }
        if (bpm !== undefined) {
          await sc.execute(`~cc.tempo(${bpm})`)
          return {
            content: [{ type: "text", text: `Tempo set to ${bpm} BPM` }],
          }
        } else {
          const result = await sc.execute("~cc.tempo")
          return {
            content: [{ type: "text", text: `Current tempo: ${result} BPM` }],
          }
        }
      }

      case "sc_clear": {
        await sc.execute("~cc.clear")
        return {
          content: [
            {
              type: "text",
              text: "Cleared all sounds, patterns, effects, and MIDI mappings",
            },
          ],
        }
      }

      case "sc_reboot": {
        const { device } = args as { device?: string }
        const deviceArg = device ? `"${device}"` : "nil"
        await sc.execute(`~cc.reboot(${deviceArg})`)
        return {
          content: [
            {
              type: "text",
              text: `Rebooted${device ? ` with device: ${device}` : ""}`,
            },
          ],
        }
      }

      case "sc_audio_devices": {
        const result = await sc.execute(`
          ServerOptions.devices.collect { |dev, i|
            "DEV:" ++ i ++ ":" ++ dev
          }.join("\\n")
        `)
        const lines = result.split("\n")
        const devices: string[] = []
        for (const line of lines) {
          if (line.startsWith("DEV:")) {
            const parts = line.split(":")
            if (parts.length >= 3) {
              devices.push(parts.slice(2).join(":"))
            }
          }
        }
        let text = "Audio Devices:\n"
        if (devices.length === 0) {
          text += "  (none found)\n"
        } else {
          for (const dev of devices) {
            text += `  - ${dev}\n`
          }
        }
        return {
          content: [{ type: "text", text }],
        }
      }

      // MIDI tools
      case "midi_list_devices": {
        const result = await sc.execute("~cc.midi.listDevices")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      case "midi_connect": {
        const { device, direction } = args as {
          device: string
          direction: "in" | "out" | "all"
        }
        if (direction === "all") {
          await sc.execute("~cc.midi.connectAll")
          return {
            content: [{ type: "text", text: "Connected all MIDI inputs" }],
          }
        }
        const isIndex = /^\d+$/.test(device)
        const deviceArg = isIndex ? device : `"${device}"`
        await sc.execute(`~cc.midi.connect(${deviceArg}, \\${direction})`)
        return {
          content: [
            { type: "text", text: `Connected MIDI ${direction}: ${device}` },
          ],
        }
      }

      case "midi_map_notes": {
        const { synthName, channel, velocityToAmp, mono } = args as {
          synthName: string
          channel?: number
          velocityToAmp?: boolean
          mono?: boolean
        }
        const chanArg = channel !== undefined ? channel : "nil"
        const velArg = velocityToAmp !== undefined ? velocityToAmp : true
        const monoArg = mono !== undefined ? mono : false
        await sc.execute(
          `~cc.midi.mapNotes(\\${synthName}, ${chanArg}, ${velArg}, ${monoArg})`
        )
        const modeStr = monoArg ? "monophonic" : "polyphonic"
        const chanStr =
          channel !== undefined ? `channel ${channel}` : "all channels"
        return {
          content: [
            {
              type: "text",
              text: `Mapped MIDI notes to \\cc_${synthName} (${modeStr}, ${chanStr})`,
            },
          ],
        }
      }

      case "midi_map_cc": {
        const { cc, busName, range, curve, channel } = args as {
          cc: number
          busName: string
          range?: [number, number]
          curve?: "lin" | "exp"
          channel?: number
        }
        const rangeArg = range ? `[${range[0]}, ${range[1]}]` : "[0, 1]"
        const curveArg = curve ? `\\${curve}` : "\\lin"
        const chanArg = channel !== undefined ? channel : "nil"
        await sc.execute(
          `~cc.midi.mapCC(${cc}, \\${busName}, ${rangeArg}, ${curveArg}, ${chanArg})`
        )
        return {
          content: [
            {
              type: "text",
              text: `Mapped CC ${cc} to ~${busName} (${range?.[0] ?? 0}-${
                range?.[1] ?? 1
              }, ${curve ?? "lin"})`,
            },
          ],
        }
      }

      case "midi_clear": {
        await sc.execute("~cc.midi.clearMappings")
        return {
          content: [{ type: "text", text: "Cleared all MIDI mappings" }],
        }
      }

      // Effects tools
      case "fx_load": {
        const { name: effectName, slot } = args as {
          name: string
          slot?: string
        }
        const slotArg = slot ? `\\${slot}` : "nil"
        const result = await sc.execute(
          `~cc.fx.load(\\${effectName}, ${slotArg})`
        )
        return {
          content: [
            {
              type: "text",
              text: `Loaded effect: ${slot || `fx_${effectName}`}\n${result}`,
            },
          ],
        }
      }

      case "fx_set": {
        const { slot, params } = args as {
          slot: string
          params: Record<string, number>
        }
        const paramStr = formatParams(params)
        await sc.execute(`~cc.fx.set(\\${slot}, ${paramStr})`)
        return {
          content: [
            {
              type: "text",
              text: `Set ${Object.keys(params).join(", ")} on ${slot}`,
            },
          ],
        }
      }

      case "fx_chain": {
        const { name: chainName, effects: chainEffects } = args as {
          name: string
          effects: Array<{ name: string; params?: Record<string, number> }>
        }
        // Build SC array
        const fxArray = chainEffects
          .map((fx) => {
            if (fx.params && Object.keys(fx.params).length > 0) {
              return `\\${fx.name} -> [${formatParams(fx.params)}]`
            }
            return `\\${fx.name}`
          })
          .join(", ")
        const result = await sc.execute(
          `~cc.fx.chain(\\${chainName}, [${fxArray}])`
        )
        return {
          content: [
            {
              type: "text",
              text: `Created chain: ${chainName}\n${result}\nEffects: ${chainEffects
                .map((e) => e.name)
                .join(" → ")}`,
            },
          ],
        }
      }

      case "fx_route": {
        const { source, target } = args as {
          source: string
          target: string
        }
        await sc.execute(`~cc.fx.route(\\${source}, \\${target})`)
        return {
          content: [{ type: "text", text: `Routed ${source} → ${target}` }],
        }
      }

      case "fx_sidechain": {
        const {
          name: scName,
          threshold = 0.1,
          ratio = 4,
          attack = 0.01,
          release = 0.1,
        } = args as {
          name: string
          threshold?: number
          ratio?: number
          attack?: number
          release?: number
        }
        const result = await sc.execute(
          `~cc.fx.sidechain(\\${scName}, ${threshold}, ${ratio}, ${attack}, ${release})`
        )
        return {
          content: [
            {
              type: "text",
              text: `Created sidechain: ${scName}\n${result}`,
            },
          ],
        }
      }

      case "fx_route_trigger": {
        const { source, sidechain } = args as {
          source: string
          sidechain: string
        }
        await sc.execute(`~cc.fx.routeTrigger(\\${source}, \\${sidechain})`)
        return {
          content: [
            {
              type: "text",
              text: `Routed ${source} as trigger for sidechain ${sidechain}`,
            },
          ],
        }
      }

      case "fx_bypass": {
        const { slot, bypass = true } = args as {
          slot: string
          bypass?: boolean
        }
        await sc.execute(`~cc.fx.bypass(\\${slot}, ${bypass})`)
        return {
          content: [
            {
              type: "text",
              text: bypass ? `Bypassed ${slot}` : `Enabled ${slot}`,
            },
          ],
        }
      }

      case "fx_remove": {
        const { slot } = args as { slot: string }
        await sc.execute(`~cc.fx.remove(\\${slot})`)
        return {
          content: [{ type: "text", text: `Removed ${slot}` }],
        }
      }

      case "fx_list": {
        const result = await sc.execute("~cc.fx.status")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      // Sample tools
      case "sample_list": {
        const sampleList = await samples.format()
        return {
          content: [{ type: "text", text: sampleList }],
        }
      }

      case "sample_play": {
        const {
          name: sampleName,
          rate = 1,
          amp = 0.5,
        } = args as {
          name: string
          rate?: number
          amp?: number
        }
        await samples.play(sampleName, rate, amp)
        return {
          content: [
            {
              type: "text",
              text: `Playing "${sampleName}" (rate: ${rate}, amp: ${amp})`,
            },
          ],
        }
      }

      case "sample_free": {
        const { name: sampleName } = args as { name: string }
        await samples.free(sampleName)
        return {
          content: [{ type: "text", text: `Freed sample "${sampleName}"` }],
        }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown tool: ${name}` }],
          isError: true,
        }
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return {
      content: [{ type: "text", text: `Error: ${message}` }],
      isError: true,
    }
  }
})

// List available resources
server.setRequestHandler(ListResourcesRequestSchema, async () => {
  return {
    resources: [
      {
        uri: "supercollider://synthdefs",
        name: "Available SynthDefs",
        description:
          "List of pre-built synthesizer definitions with parameters",
        mimeType: "text/plain",
      },
      {
        uri: "supercollider://effects",
        name: "Available Effects",
        description: "List of pre-built audio effects with parameters",
        mimeType: "text/plain",
      },
    ],
  }
})

// Read resource content
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const { uri } = request.params

  switch (uri) {
    case "supercollider://synthdefs": {
      const text = `# Available SynthDefs

All synths are pre-loaded and ready to use. Names are prefixed with \\cc_.

${synthdefs.format()}`

      return {
        contents: [{ uri, mimeType: "text/plain", text }],
      }
    }

    case "supercollider://effects": {
      const text = `# Available Effects

Effects must be loaded before use with fx_load. Then route audio to them with fx_route.

${effects.format()}

## Usage

1. Load an effect:
     fx_load with name: "reverb"

2. Route a pattern to it:
     fx_route with source: "myPattern", target: "fx_reverb"

3. Adjust parameters:
     fx_set with slot: "fx_reverb", params: { mix: 0.5, room: 0.9 }`

      return {
        contents: [{ uri, mimeType: "text/plain", text }],
      }
    }

    default:
      throw new Error(`Unknown resource: ${uri}`)
  }
})

// Handle graceful shutdown
process.on("SIGINT", async () => {
  await sc.quit()
  process.exit(0)
})

process.on("SIGTERM", async () => {
  await sc.quit()
  process.exit(0)
})

// Start the server
async function main() {
  const transport = new StdioServerTransport()
  await server.connect(transport)
}

main().catch((error) => {
  console.error("Server error:", error)
  process.exit(1)
})
