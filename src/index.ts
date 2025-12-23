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

// List available tools (9 consolidated tools)
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      // 1. cc_execute - Run arbitrary SuperCollider code
      {
        name: "cc_execute",
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

      // 2. cc_status - Show status, routing, synths, or effects info
      {
        name: "cc_status",
        description:
          "Show current state: tempo, running patterns, active synths, and server CPU usage.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["status", "routing", "synths", "effects"],
              description:
                "What to show: status (default), routing debug, available synths, or available effects",
            },
          },
          required: [],
        },
      },

      // 3. cc_reboot - Restart audio server or list devices
      {
        name: "cc_reboot",
        description:
          "Restart the audio server, optionally switching to a different audio device.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["reboot", "devices"],
              description: "Operation: reboot server (default) or list audio devices",
            },
            device: {
              type: "string",
              description: "Audio device name (for reboot). Omit to keep current device.",
            },
            numOutputs: {
              type: "number",
              description:
                "Number of output channels (for reboot). Omit to keep current setting.",
            },
          },
          required: [],
        },
      },

      // 4. cc_control - Stop, clear, or set tempo
      {
        name: "cc_control",
        description:
          "Control playback: stop all sounds, clear everything, or get/set tempo.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["stop", "clear", "tempo"],
              description:
                "Operation: stop (silence all), clear (full reset), or tempo (get/set BPM)",
            },
            bpm: {
              type: "number",
              description: "Tempo in BPM (for action=tempo). Omit to get current tempo.",
            },
          },
          required: ["action"],
        },
      },

      // 5. cc_fx - All effects operations
      {
        name: "cc_fx",
        description:
          "Effects operations: load, set params, bypass, remove, wire routing, sidechain, or create chain.",
        inputSchema: {
          type: "object",
          properties: {
            action: {
              type: "string",
              enum: ["load", "set", "bypass", "remove", "wire", "sidechain", "chain"],
              description: "Effect operation to perform",
            },
            // For load
            name: {
              type: "string",
              description:
                "Effect or sidechain name (for load/sidechain/chain). E.g. reverb, delay, distortion",
            },
            slot: {
              type: "string",
              description: "Ndef slot name (for load, default: fx_<name>; for set/bypass/remove)",
            },
            // For set
            params: {
              type: "object",
              description: "Parameter name/value pairs (for action=set)",
            },
            // For bypass
            bypass: {
              type: "boolean",
              description: "true to bypass, false to re-enable (for action=bypass, default: true)",
            },
            // For wire
            source: {
              type: "string",
              description: "Source (Pdef, Ndef, or effect slot) for wire action",
            },
            target: {
              type: "string",
              description: "Destination (effect slot, chain, or sidechain) for wire action",
            },
            type: {
              type: "string",
              enum: ["source", "effect", "sidechain"],
              description:
                "Wire type: source→effect (default), effect→effect, or sidechain trigger",
            },
            passthrough: {
              type: "boolean",
              description: "For sidechain wire: source still audible? (default: true)",
            },
            // For sidechain
            threshold: {
              type: "number",
              description: "Compression threshold 0-1 (for sidechain, default: 0.1)",
            },
            ratio: {
              type: "number",
              description: "Compression ratio 1-20 (for sidechain, default: 4)",
            },
            attack: {
              type: "number",
              description: "Attack time in seconds (for sidechain, default: 0.01)",
            },
            release: {
              type: "number",
              description: "Release time in seconds (for sidechain, default: 0.1)",
            },
            // For chain
            effects: {
              type: "array",
              description:
                "Ordered list of effects for chain. Each item: string or {name, params}.",
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
          required: ["action"],
        },
      },

      // 6. cc_midi - MIDI operations
      {
        name: "cc_midi",
        description: "MIDI operations: list devices, connect, play synth, or stop.",
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

      // 7. cc_sample - Sample operations
      {
        name: "cc_sample",
        description: "Sample operations: inspect, load, play, free, or reload directory.",
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

      // 8. cc_recording - Recording operations
      {
        name: "cc_recording",
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

      // 9. cc_output - Hardware output routing
      {
        name: "cc_output",
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
    ],
  }
})

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params

  try {
    switch (name) {
      case "cc_execute":
        return await tools.cc_execute(args as { code: string })
      case "cc_status":
        return await tools.cc_status(
          args as { action?: "status" | "routing" | "synths" | "effects" }
        )
      case "cc_reboot":
        return await tools.cc_reboot(
          args as { action?: "reboot" | "devices"; device?: string; numOutputs?: number }
        )
      case "cc_control":
        return await tools.cc_control(args as { action: "stop" | "clear" | "tempo"; bpm?: number })
      case "cc_fx":
        return await tools.cc_fx(args as Parameters<typeof tools.cc_fx>[0])
      case "cc_midi":
        return await tools.cc_midi(args as Parameters<typeof tools.cc_midi>[0])
      case "cc_sample":
        return await tools.cc_sample(args as Parameters<typeof tools.cc_sample>[0])
      case "cc_recording":
        return await tools.cc_recording(args as Parameters<typeof tools.cc_recording>[0])
      case "cc_output":
        return await tools.cc_output(args as Parameters<typeof tools.cc_output>[0])
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
