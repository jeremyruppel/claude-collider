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

const sc = new SuperCollider()
const samples = new Samples(sc)
const synthdefs = new SynthDefs(sc)
const effects = new Effects(sc)

async function formatBootReadme(): Promise<string> {
  const synthList = await sc.execute("~cc.synths.list")
  const fxList = await sc.execute("~cc.fx.list")
  const sampleList = await sc.execute("~cc.samples.list")

  return `# ClaudeCollider Ready

## sc_execute Notes
- Send code as a single line, semicolons between statements
- System appends trailing semicolon, don't include one at the end

## Quick Reference
  Synth(\\cc_kick)                    # one-shot synth
  Pdef(\\beat, Pbind(...)).play       # pattern (use for beats)
  Ndef(\\pad, { ... }).play           # continuous synth
  Ppar([Pbind(...), Pbind(...)])     # sync multiple patterns

## Output Routing
All audio routes through output Ndefs with limiter protection.
- Default: outputs 1-2 (main output)
- Route to other outputs: output_route (for multi-output setups)
- Example: output_route("kick", 3) routes kick to output 3 (mono)
- Example: output_route("drums", [7, 8]) routes drums to stereo pair 7-8

## Effects & Routing
- fx_load: Load effects (reverb, delay, distortion, etc.)
- fx_route: Route sources to effects
- fx_connect: Chain effects together (effect -> effect)
- fx_chain: Create named effect chains
- fx_sidechain: Create sidechain compressors for ducking

## Subsystems (via sc_execute)
~cc.outputs    # CCOutputs - hardware output routing
~cc.router     # CCRouter - effect connections and chains
~cc.sidechains # CCSidechain - sidechain compressors

## Gotchas
- Pbind auto-sets \\freq ~261Hz. For drums: \\freq, 48
- NEVER call Synth() inside Ndef — infinite spawning

## Synths: ${synthList}
## Effects: ${fxList}
## Samples: ${sampleList}

Use synth_inspect, fx_inspect, sample_inspect for details.
Use sc_status for state, routing_debug for signal flow.`
}

const server = new Server(
  {
    name: "ClaudeCollider",
    version: "0.1.0",
    // TODO branding!
    icons: [],
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
          "Start the SuperCollider audio server. Must be called first before any other tools.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description:
                "Audio device name (e.g. 'BlackHole 2ch'). Omit for default device.",
            },
            numOutputs: {
              type: "number",
              description:
                "Number of output channels (default: 2). Use higher values for multi-channel setups.",
            },
          },
          required: [],
        },
      },
      {
        name: "sc_execute",
        description:
          "Run SuperCollider code directly. For custom synths, patterns, or anything not covered by other tools.",
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
          "Route MIDI keyboard notes to a synth. Synth must accept freq, amp, and gate parameters.",
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
          "Route a MIDI control change (knob/slider) to a named bus for modulating synth parameters.",
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
      {
        name: "midi_thru",
        description:
          "Route MIDI input to MIDI output. Requires midi_connect with 'out' first.",
        inputSchema: {
          type: "object",
          properties: {
            outputChannel: {
              type: "number",
              description:
                "MIDI output channel 1-16. Omit to pass through original channel.",
            },
            inputChannel: {
              type: "number",
              description:
                "MIDI input channel 1-16 to listen on. Omit for all channels.",
            },
          },
          required: [],
        },
      },
      // Effects tools
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
        name: "fx_route_trigger",
        description:
          "Route a source to the trigger input of a sidechain. By default, the source still plays to its original output (passthrough). Set passthrough=false to redirect entirely to the trigger (source becomes inaudible).",
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
            passthrough: {
              type: "boolean",
              description:
                "If true (default), source still plays to main output. If false, source only goes to trigger.",
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
        name: "fx_inspect",
        description:
          "List all available effects with their descriptions and parameters.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "fx_connect",
        description:
          "Connect one effect's output to another effect's input for serial processing.",
        inputSchema: {
          type: "object",
          properties: {
            from: {
              type: "string",
              description: "Source effect slot name (e.g. 'fx_distortion')",
            },
            to: {
              type: "string",
              description: "Destination effect slot name (e.g. 'fx_reverb')",
            },
          },
          required: ["from", "to"],
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
              description:
                "Name for this chain (e.g. 'bass_chain', 'vocal_chain')",
            },
            effects: {
              type: "array",
              description:
                "Ordered list of effects. Each item can be a string (effect name with defaults) or an object with name and params.",
              items: {
                oneOf: [
                  {
                    type: "string",
                    description: "Effect name with default parameters",
                  },
                  {
                    type: "object",
                    properties: {
                      name: {
                        type: "string",
                        description:
                          "Effect name (e.g. 'reverb', 'distortion')",
                      },
                      params: {
                        type: "object",
                        description:
                          "Parameter key/value pairs for this effect",
                      },
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
      // Sample tools
      {
        name: "sample_inspect",
        description:
          "List all samples with duration, channels, and sample rate info.",
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
      {
        name: "sample_reload",
        description:
          "Rescan the samples directory for newly added audio files.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      // Recording tools
      {
        name: "recording_start",
        description: "Begin recording all audio output to a WAV file.",
        inputSchema: {
          type: "object",
          properties: {
            filename: {
              type: "string",
              description:
                "Custom filename (optional). Auto-generates timestamped name if omitted.",
            },
          },
          required: [],
        },
      },
      {
        name: "recording_stop",
        description:
          "Stop recording and save the WAV file. Returns the saved file path.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "recording_status",
        description: "Check if recording is active and get current file path.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      // Output routing tools
      {
        name: "output_route",
        description:
          "Route a source (Pdef or Ndef) to specific hardware outputs. Use for multi-output setups.",
        inputSchema: {
          type: "object",
          properties: {
            source: {
              type: "string",
              description: "Pdef or Ndef name to route",
            },
            outputs: {
              oneOf: [
                { type: "number" },
                { type: "array", items: { type: "number" } },
              ],
              description:
                "Hardware outputs: single number for mono (e.g., 3), or array for stereo pair (e.g., [3, 4]). 1-indexed.",
            },
          },
          required: ["source", "outputs"],
        },
      },
      {
        name: "output_unroute",
        description:
          "Remove output routing for a source, returning it to the main outputs (1-2).",
        inputSchema: {
          type: "object",
          properties: {
            source: {
              type: "string",
              description: "Pdef or Ndef name to unroute",
            },
          },
          required: ["source"],
        },
      },
      {
        name: "output_status",
        description:
          "Show all active output routings and their destinations.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
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
      case "sc_boot": {
        const { device, numOutputs } = args as {
          device?: string
          numOutputs?: number
        }
        const deviceArg = device ? `"${device}"` : "nil"
        const numOutputsArg = numOutputs ?? "nil"
        await sc.boot()
        await sc.execute(
          `~cc = CC.boot(device: ${deviceArg}, numOutputs: ${numOutputsArg}, samplesDir: "${sc.getSamplesPath()}", recordingsDir: "${sc.getRecordingsPath()}")`
        )
        await sc.waitForCCReady()
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
        const { device, numOutputs } = args as {
          device?: string
          numOutputs?: number
        }
        const deviceArg = device ? `"${device}"` : "nil"
        const numOutputsArg = numOutputs ?? "nil"
        await sc.execute(`~cc.reboot(${deviceArg}, ${numOutputsArg})`)
        await sc.waitForCCReady()
        const parts = []
        if (device) parts.push(`device: ${device}`)
        if (numOutputs) parts.push(`outputs: ${numOutputs}`)
        return {
          content: [
            {
              type: "text",
              text: `Rebooted${
                parts.length ? ` with ${parts.join(", ")}` : ""
              }`,
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

      case "midi_thru": {
        const { outputChannel, inputChannel } = args as {
          outputChannel?: number
          inputChannel?: number
        }
        // Convert 1-indexed to 0-indexed for SC
        const outChanArg =
          outputChannel !== undefined ? outputChannel - 1 : "nil"
        const inChanArg =
          inputChannel !== undefined ? inputChannel - 1 : "nil"
        await sc.execute(`~cc.midi.thru(${inChanArg}, ${outChanArg})`)
        const chanInfo =
          outputChannel !== undefined
            ? `channel ${outputChannel}`
            : "original channel"
        return {
          content: [{ type: "text", text: `MIDI thru enabled → ${chanInfo}` }],
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
        const { source, sidechain, passthrough = true } = args as {
          source: string
          sidechain: string
          passthrough?: boolean
        }
        await sc.execute(
          `~cc.fx.routeTrigger(\\${source}, \\${sidechain}, ${passthrough})`
        )
        const passthroughNote = passthrough
          ? " (passthrough enabled - source still audible)"
          : " (passthrough disabled - source redirected to trigger only)"
        return {
          content: [
            {
              type: "text",
              text: `Routed ${source} as trigger for sidechain ${sidechain}${passthroughNote}`,
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

      case "fx_inspect": {
        const result = await sc.execute("~cc.fx.describe")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      case "fx_connect": {
        const { from, to } = args as { from: string; to: string }
        const result = await sc.execute(`~cc.fx.connect(\\${from}, \\${to})`)
        if (result.includes("nil")) {
          return {
            content: [
              { type: "text", text: `Failed to connect ${from} → ${to}` },
            ],
            isError: true,
          }
        }
        return {
          content: [{ type: "text", text: `Connected ${from} → ${to}` }],
        }
      }

      case "fx_chain": {
        const { name: chainName, effects: fxList } = args as {
          name: string
          effects: Array<
            string | { name: string; params?: Record<string, number> }
          >
        }

        if (!fxList || fxList.length === 0) {
          return {
            content: [
              { type: "text", text: "Error: effects array cannot be empty" },
            ],
            isError: true,
          }
        }

        const slots: string[] = []
        const results: string[] = []

        // Load each effect with chain-specific slot names
        for (let i = 0; i < fxList.length; i++) {
          const fx = fxList[i]
          const fxName = typeof fx === "string" ? fx : fx.name
          const fxParams =
            typeof fx === "object" && fx.params ? fx.params : null
          const slotName = `${chainName}_${i}_${fxName}`

          // Load the effect
          const loadResult = await sc.execute(
            `~cc.fx.load(\\${fxName}, \\${slotName})`
          )
          if (loadResult.includes("unknown effect")) {
            return {
              content: [
                {
                  type: "text",
                  text: `Error: Unknown effect '${fxName}' at position ${i}`,
                },
              ],
              isError: true,
            }
          }

          // Set params if provided
          if (fxParams) {
            const paramStr = formatParams(fxParams)
            await sc.execute(`~cc.fx.set(\\${slotName}, ${paramStr})`)
          }

          slots.push(slotName)
          results.push(`  → ${slotName}`)
        }

        // Connect effects in series
        for (let i = 0; i < slots.length - 1; i++) {
          await sc.execute(`~cc.fx.connect(\\${slots[i]}, \\${slots[i + 1]})`)
        }

        // Register the chain for tracking
        const slotsArray = slots.map((s) => `\\${s}`).join(", ")
        await sc.execute(
          `~cc.fx.registerChain(\\${chainName}, [${slotsArray}])`
        )

        results.push("  → main out")
        results.unshift(`Created chain: ${chainName}`)
        results.push(`Route sources to: ${slots[0]}`)

        return {
          content: [{ type: "text", text: results.join("\n") }],
        }
      }

      // Synth tools
      case "synth_inspect": {
        const result = await sc.execute("~cc.synths.describe")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      // Sample tools
      case "sample_inspect": {
        const result = await sc.execute("~cc.samples.describe")
        return {
          content: [{ type: "text", text: result }],
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

      case "sample_reload": {
        await sc.execute("~cc.samples.reload")
        const sampleList = await samples.format()
        return {
          content: [
            {
              type: "text",
              text: `Reloaded samples directory.\n\n${sampleList}`,
            },
          ],
        }
      }

      // Recording tools
      case "recording_start": {
        const { filename } = args as { filename?: string }
        const arg = filename ? `"${filename}"` : "nil"
        const result = await sc.execute(`~cc.recorder.start(${arg})`)
        return {
          content: [{ type: "text", text: result }],
          isError: result.includes("Already recording"),
        }
      }

      case "recording_stop": {
        const result = await sc.execute("~cc.recorder.stop")
        return {
          content: [{ type: "text", text: result }],
          isError: result === "Not recording",
        }
      }

      case "recording_status": {
        const result = await sc.execute("~cc.recorder.status")
        return {
          content: [{ type: "text", text: result }],
        }
      }

      // Output routing tools
      case "output_route": {
        const { source, outputs } = args as {
          source: string
          outputs: number | number[]
        }
        const outputsArg = Array.isArray(outputs)
          ? `[${outputs.join(", ")}]`
          : outputs
        await sc.execute(`~cc.fx.routeToOutput(\\${source}, ${outputsArg})`)
        const outputStr = Array.isArray(outputs)
          ? `outputs ${outputs.join("-")}`
          : `output ${outputs}`
        return {
          content: [{ type: "text", text: `Routed ${source} to ${outputStr}` }],
        }
      }

      case "output_unroute": {
        const { source } = args as { source: string }
        await sc.execute(`~cc.fx.unrouteFromOutput(\\${source})`)
        return {
          content: [
            { type: "text", text: `Unrouted ${source}, now using main outputs` },
          ],
        }
      }

      case "output_status": {
        const result = await sc.execute("~cc.fx.outputStatus")
        return {
          content: [{ type: "text", text: `Output Routing:\n${result}` }],
        }
      }

      // Debug tools
      case "routing_debug": {
        const result = await sc.execute("~cc.formatter.formatRoutingDebug")
        return {
          content: [{ type: "text", text: result }],
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
