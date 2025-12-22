#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js"
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js"
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js"
import { SuperCollider } from "./supercollider.js"
import { SynthDefs } from "./synthdefs.js"
import { Effects } from "./effects.js"
import { Samples } from "./samples.js"
import { Tools } from "./tools.js"

const sc = new SuperCollider()
const samples = new Samples(sc)
const synthdefs = new SynthDefs(sc)
const effects = new Effects(sc)
const tools = new Tools(sc, samples, synthdefs, effects)

const server = new Server(
  {
    name: "ClaudeCollider",
    version: "0.1.0",
    icons: [],
  },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
)

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      // Core SC tools (7)
      {
        name: "sc_execute",
        description:
          "Run SuperCollider code directly. For custom synths, patterns, or anything not covered by other tools. Auto-boots if needed.",
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
          "Stop all currently playing sounds immediately (Cmd+Period equivalent)",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_status",
        description:
          "Show current state: tempo, running patterns, active synths, and server CPU usage.",
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
          "Full reset: stop all sounds, clear patterns, remove effects, and disconnect MIDI.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_reboot",
        description:
          "Restart the audio server, optionally switching to a different audio device.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description: "Audio device name. Omit to keep current device.",
            },
            numOutputs: {
              type: "number",
              description:
                "Number of output channels. Omit to keep current setting.",
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

      // Consolidated MIDI tool
      {
        name: "midi",
        description:
          "MIDI operations: list devices, connect, play synth, or stop.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["list", "connect", "play", "stop"],
              description: "MIDI operation to perform",
            },
            device: {
              type: "string",
              description: "Device name or index (for connect)",
            },
            direction: {
              type: "string",
              enum: ["in", "out", "all"],
              description: "Input, output, or all inputs (for connect)",
            },
            synth: {
              type: "string",
              description:
                "Synth name without cc_ prefix (for play). Must have freq, amp, gate params.",
            },
            channel: {
              type: "number",
              description: "MIDI channel 0-15 (for play). Omit for all channels.",
            },
            mono: {
              type: "boolean",
              description: "Monophonic mode (for play, default: false)",
            },
            velToAmp: {
              type: "boolean",
              description: "Map velocity to amplitude (for play, default: true)",
            },
            cc: {
              type: "object",
              description:
                "CC mappings (for play): {ccNumber: 'paramName'} or {ccNumber: {param: 'name', range: [min, max], curve: 'lin'|'exp'}}",
              additionalProperties: true,
            },
          },
          required: ["action"],
        },
      },

      // FX tools (7)
      {
        name: "fx_load",
        description:
          "Load an effect (reverb, delay, distortion, etc). Returns the slot name for routing.",
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
        name: "fx_manage",
        description: "Manage effects: set parameters, bypass, or remove.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["set", "bypass", "remove"],
              description: "Operation to perform",
            },
            slot: {
              type: "string",
              description: "Effect slot name",
            },
            params: {
              type: "object",
              description: "Parameter name/value pairs (for action=set)",
            },
            bypass: {
              type: "boolean",
              description: "true to bypass, false to re-enable (for action=bypass, default: true)",
            },
          },
          required: ["action", "slot"],
        },
      },
      {
        name: "fx_wire",
        description:
          "Route audio: source to effect, effect to effect, or to sidechain trigger.",
        inputSchema: {
          type: "object",
          properties: {
            source: {
              type: "string",
              description: "Source (Pdef, Ndef, or effect slot)",
            },
            target: {
              type: "string",
              description: "Destination (effect slot, chain, or sidechain)",
            },
            type: {
              type: "string",
              enum: ["source", "effect", "sidechain"],
              description:
                "Routing type: source→effect (default), effect→effect, or sidechain trigger",
            },
            passthrough: {
              type: "boolean",
              description:
                "For sidechain: source still audible? (default: true)",
            },
          },
          required: ["source", "target"],
        },
      },
      {
        name: "fx_sidechain",
        description:
          "Create a sidechain compressor for ducking effects (e.g., kick ducking bass).",
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
        name: "fx_chain",
        description:
          "Create a named series of effects (e.g., distortion → delay → reverb). Route sources to the first slot.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Name for this chain (e.g. 'bass_chain')",
            },
            effects: {
              type: "array",
              description:
                "Ordered list of effects. Each item can be a string (effect name) or an object with name and params.",
              items: {
                oneOf: [
                  { type: "string" },
                  {
                    type: "object",
                    properties: {
                      name: { type: "string" },
                      params: { type: "object" },
                    },
                    required: ["name"],
                  },
                ],
              },
            },
          },
          required: ["name", "effects"],
        },
      },
      {
        name: "fx_inspect",
        description:
          "List all available effects with their descriptions and parameters.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },

      // Synth tools
      {
        name: "synth_inspect",
        description:
          "List all available synths with their descriptions and parameters.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },

      // Consolidated sample tool
      {
        name: "sample",
        description:
          "Sample operations: inspect, load, play, free, or reload directory.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["inspect", "load", "play", "free", "reload"],
              description: "Sample operation to perform",
            },
            name: {
              type: "string",
              description: "Sample name (for load/play/free)",
            },
            rate: {
              type: "number",
              description: "Playback rate (for play, default: 1, negative for reverse)",
            },
            amp: {
              type: "number",
              description: "Amplitude 0-1 (for play, default: 0.5)",
            },
          },
          required: ["action"],
        },
      },

      // Consolidated recording tool
      {
        name: "recording",
        description: "Recording operations: start, stop, or check status.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["start", "stop", "status"],
              description: "Recording operation to perform",
            },
            filename: {
              type: "string",
              description:
                "Custom filename (for start). Auto-generates timestamped name if omitted.",
            },
          },
          required: ["action"],
        },
      },

      // Consolidated output tool
      {
        name: "output",
        description:
          "Hardware output routing: route source to outputs, unroute, or show status.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["route", "unroute", "status"],
              description: "Output routing operation",
            },
            source: {
              type: "string",
              description: "Pdef or Ndef name (for route/unroute)",
            },
            outputs: {
              oneOf: [
                { type: "number" },
                { type: "array", items: { type: "number" } },
              ],
              description:
                "Output number for mono, or array for stereo pair [3, 4]. 1-indexed. (for route)",
            },
          },
          required: ["action"],
        },
      },

      // Debug tools
      {
        name: "routing_debug",
        description:
          "Debug the current audio routing: shows signal flow, bus indices, effect parameters, and active sources.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
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
      case "sc_execute":
        return await tools.sc_execute(args as { code: string })
      case "sc_stop":
        return await tools.sc_stop()
      case "sc_status":
        return await tools.sc_status()
      case "sc_tempo":
        return await tools.sc_tempo(args as { bpm?: number })
      case "sc_clear":
        return await tools.sc_clear()
      case "sc_reboot":
        return await tools.sc_reboot(args as { device?: string; numOutputs?: number })
      case "sc_audio_devices":
        return await tools.sc_audio_devices()
      case "midi":
        return await tools.midi(args as Parameters<typeof tools.midi>[0])
      case "fx_load":
        return await tools.fx_load(args as { name: string; slot?: string })
      case "fx_manage":
        return await tools.fx_manage(args as Parameters<typeof tools.fx_manage>[0])
      case "fx_wire":
        return await tools.fx_wire(args as Parameters<typeof tools.fx_wire>[0])
      case "fx_sidechain":
        return await tools.fx_sidechain(args as Parameters<typeof tools.fx_sidechain>[0])
      case "fx_chain":
        return await tools.fx_chain(args as Parameters<typeof tools.fx_chain>[0])
      case "fx_inspect":
        return await tools.fx_inspect()
      case "synth_inspect":
        return await tools.synth_inspect()
      case "sample":
        return await tools.sample(args as Parameters<typeof tools.sample>[0])
      case "recording":
        return await tools.recording(args as Parameters<typeof tools.recording>[0])
      case "output":
        return await tools.output(args as Parameters<typeof tools.output>[0])
      case "routing_debug":
        return await tools.routing_debug()
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
