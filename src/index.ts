#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { SuperCollider } from "./supercollider.js";
import { getSynthDef, getSynthDefNames, synthdefs } from "./synthdefs.js";
import { MIDIManager } from "./midi.js";
import { EffectsManager } from "./effects.js";
import { effectsLibrary } from "./effects-library.js";

const sc = new SuperCollider();
const midi = new MIDIManager(sc);
const effects = new EffectsManager(sc);

const effectNames = Object.keys(effectsLibrary);

const server = new Server(
  {
    name: "vibe-music",
    version: "0.1.0",
  },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
);

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "sc_boot",
        description:
          "Start SuperCollider and boot the audio server. Must be called before playing any sounds.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_setup",
        description:
          "Boot SuperCollider with audio configuration in one step. Boots, applies config, and restarts with new settings.",
        inputSchema: {
          type: "object",
          properties: {
            outputDevice: {
              type: "string",
              description: "Output device by name",
            },
            inputDevice: {
              type: "string",
              description: "Input device by name",
            },
            sampleRate: {
              type: "number",
              description: "Sample rate (e.g. 44100, 48000, 96000)",
            },
            blockSize: {
              type: "number",
              description:
                "Hardware buffer size (e.g. 64, 128, 256, 512, 1024). Lower = less latency but more CPU",
            },
            numOutputs: {
              type: "number",
              description: "Number of output channels",
            },
            numInputs: {
              type: "number",
              description: "Number of input channels",
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
        name: "sc_free_all",
        description:
          "Free all synth nodes on the server. Nuclear option for stuck sounds that sc_stop doesn't fix.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_restart",
        description:
          "Restart SuperCollider. Quits the current session and boots fresh. Use for recovery after crashes.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_quit",
        description:
          "Quit SuperCollider completely. Kills sclang and scsynth processes.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "sc_load_synthdef",
        description: `Load a pre-built SynthDef. Available: ${getSynthDefNames().join(", ")}. After loading, play with Synth(\\name, [args]).`,
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Name of the synthdef to load",
              enum: getSynthDefNames(),
            },
          },
          required: ["name"],
        },
      },
      {
        name: "sc_status",
        description: "Get SuperCollider server status: number of synths, CPU usage.",
        inputSchema: {
          type: "object",
          properties: {},
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
      {
        name: "sc_audio_config",
        description:
          "Get or set audio device configuration. Changes require a server reboot to take effect.",
        inputSchema: {
          type: "object",
          properties: {
            outputDevice: {
              type: "string",
              description: "Set output device by name",
            },
            inputDevice: {
              type: "string",
              description: "Set input device by name",
            },
            sampleRate: {
              type: "number",
              description: "Set sample rate (e.g. 44100, 48000, 96000)",
            },
            blockSize: {
              type: "number",
              description:
                "Set hardware buffer size (e.g. 64, 128, 256, 512, 1024). Lower = less latency but more CPU",
            },
            numOutputs: {
              type: "number",
              description: "Number of output channels",
            },
            numInputs: {
              type: "number",
              description: "Number of input channels",
            },
          },
          required: [],
        },
      },
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
          "Connect to a MIDI device by name or index. Multiple inputs can be connected simultaneously. Connecting to a new output replaces the previous output connection.",
        inputSchema: {
          type: "object",
          properties: {
            device: {
              type: "string",
              description: "Device name or index number",
            },
            direction: {
              type: "string",
              enum: ["in", "out"],
              description: "Input or output",
            },
          },
          required: ["device", "direction"],
        },
      },
      {
        name: "midi_disconnect",
        description:
          "Disconnect from MIDI devices. Can disconnect specific devices or all at once.",
        inputSchema: {
          type: "object",
          properties: {
            direction: {
              type: "string",
              enum: ["in", "out", "all"],
              description: "Disconnect inputs, output, or all",
            },
            device: {
              type: "string",
              description:
                "Device name or index to disconnect (optional - omit to disconnect all in that direction)",
            },
          },
          required: ["direction"],
        },
      },
      {
        name: "midi_get_connections",
        description:
          "Get currently connected MIDI devices. Shows which inputs and output are active.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "midi_map_notes",
        description:
          "Map MIDI note input to trigger a synth. The synth should accept 'freq', 'amp', and 'gate' arguments for proper note-on/note-off handling.",
        inputSchema: {
          type: "object",
          properties: {
            synthName: {
              type: "string",
              description: "Name of the SynthDef to trigger",
            },
            channel: {
              type: "number",
              description: "MIDI channel 0-15. Omit to respond to all channels.",
            },
            velocityToAmp: {
              type: "boolean",
              description: "Map note velocity to amplitude (default: true)",
            },
            mono: {
              type: "boolean",
              description: "Monophonic mode - only one note at a time (default: false)",
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
                "Name for the control bus (e.g. 'cutoff', 'resonance'). Will be stored as ~busName.",
            },
            range: {
              type: "array",
              items: { type: "number" },
              description: "Output range [min, max] (default: [0, 1])",
            },
            curve: {
              type: "string",
              enum: ["linear", "exponential"],
              description: "Mapping curve (default: linear)",
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
        name: "midi_learn",
        description:
          "Wait for a MIDI CC message and return which CC number was received. Useful for mapping unknown knobs/faders.",
        inputSchema: {
          type: "object",
          properties: {
            timeout: {
              type: "number",
              description: "Seconds to wait before giving up (default: 10)",
            },
          },
          required: [],
        },
      },
      {
        name: "midi_send",
        description: "Send a MIDI message to connected output device.",
        inputSchema: {
          type: "object",
          properties: {
            type: {
              type: "string",
              enum: ["note_on", "note_off", "cc", "program"],
              description: "Message type",
            },
            channel: {
              type: "number",
              description: "MIDI channel 0-15 (default: 0)",
            },
            note: {
              type: "number",
              description: "Note number 0-127 (for note_on/note_off)",
            },
            velocity: {
              type: "number",
              description: "Velocity 0-127 (for note_on/note_off, default: 64)",
            },
            cc: {
              type: "number",
              description: "CC number 0-127 (for cc type)",
            },
            value: {
              type: "number",
              description: "Value 0-127 (for cc/program type)",
            },
          },
          required: ["type"],
        },
      },
      {
        name: "midi_get_recent",
        description:
          "Get recent MIDI input events. Useful for 'what did I just play?' or building patterns from played notes.",
        inputSchema: {
          type: "object",
          properties: {
            count: {
              type: "number",
              description: "Maximum number of events to return (default: 20)",
            },
            type: {
              type: "string",
              enum: ["notes", "cc", "all"],
              description: "Filter by event type (default: all)",
            },
            since: {
              type: "number",
              description: "Only return events from the last N seconds",
            },
          },
          required: [],
        },
      },
      {
        name: "midi_clear_mappings",
        description: "Clear all MIDI note and CC mappings.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
        },
      },
      {
        name: "midi_play_pattern",
        description:
          "Play a pattern on connected MIDI output device. Loops infinitely until stopped.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Pdef name for this pattern (used to stop it later)",
            },
            notes: {
              type: "array",
              items: { type: "number" },
              description: "MIDI note numbers (0-127)",
            },
            durations: {
              type: "array",
              items: { type: "number" },
              description: "Note durations in beats",
            },
            velocities: {
              type: "array",
              items: { type: "number" },
              description: "Note velocities (0-127)",
            },
            channel: {
              type: "number",
              description: "MIDI channel 0-15 (default: 0)",
            },
          },
          required: ["name", "notes", "durations", "velocities"],
        },
      },
      // Effects tools
      {
        name: "fx_load",
        description: `Load a pre-built audio effect. Available: ${effectNames.join(", ")}. Returns the effect's input bus for routing audio.`,
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              enum: effectNames,
              description: "Effect name",
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
                    enum: effectNames,
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
        description: "Route a sound source (Pdef or Ndef) to an effect or chain.",
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
        name: "fx_bypass",
        description: "Bypass an effect (pass audio through unchanged).",
        inputSchema: {
          type: "object",
          properties: {
            slot: {
              type: "string",
              description: "Effect slot or chain name",
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
              description: "Effect slot or chain name",
            },
          },
          required: ["slot"],
        },
      },
      {
        name: "fx_list",
        description: "List all loaded effects and their current parameters.",
        inputSchema: {
          type: "object",
          properties: {},
          required: [],
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
        name: "fx_sidechain_set",
        description: "Update parameters on a sidechain compressor.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Sidechain name",
            },
            threshold: {
              type: "number",
              description: "Compression threshold 0-1",
            },
            ratio: {
              type: "number",
              description: "Compression ratio 1-20",
            },
            attack: {
              type: "number",
              description: "Attack time in seconds",
            },
            release: {
              type: "number",
              description: "Release time in seconds",
            },
          },
          required: ["name"],
        },
      },
      {
        name: "fx_sidechain_remove",
        description: "Remove a sidechain compressor and free its resources.",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "Sidechain name to remove",
            },
          },
          required: ["name"],
        },
      },
    ],
  };
});

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "sc_boot": {
        const result = await sc.boot();
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "sc_setup": {
        const {
          outputDevice,
          inputDevice,
          sampleRate,
          blockSize,
          numOutputs,
          numInputs,
        } = args as {
          outputDevice?: string;
          inputDevice?: string;
          sampleRate?: number;
          blockSize?: number;
          numOutputs?: number;
          numInputs?: number;
        };

        const steps: string[] = [];

        // Step 1: Boot
        steps.push("Booting SuperCollider...");
        await sc.boot();
        steps.push("Booted");

        // Step 2: Apply config if any options provided
        const hasConfig =
          outputDevice !== undefined ||
          inputDevice !== undefined ||
          sampleRate !== undefined ||
          blockSize !== undefined ||
          numOutputs !== undefined ||
          numInputs !== undefined;

        if (hasConfig) {
          const configChanges: string[] = [];

          if (outputDevice !== undefined) {
            await sc.execute(`s.options.outDevice = "${outputDevice}";`);
            configChanges.push(`output: ${outputDevice}`);
          }
          if (inputDevice !== undefined) {
            await sc.execute(`s.options.inDevice = "${inputDevice}";`);
            configChanges.push(`input: ${inputDevice}`);
          }
          if (sampleRate !== undefined) {
            await sc.execute(`s.options.sampleRate = ${sampleRate};`);
            configChanges.push(`sampleRate: ${sampleRate}`);
          }
          if (blockSize !== undefined) {
            await sc.execute(`s.options.hardwareBufferSize = ${blockSize};`);
            configChanges.push(`blockSize: ${blockSize}`);
          }
          if (numOutputs !== undefined) {
            await sc.execute(`s.options.numOutputBusChannels = ${numOutputs};`);
            configChanges.push(`outputs: ${numOutputs}`);
          }
          if (numInputs !== undefined) {
            await sc.execute(`s.options.numInputBusChannels = ${numInputs};`);
            configChanges.push(`inputs: ${numInputs}`);
          }

          steps.push(`Configured: ${configChanges.join(", ")}`);

          // Step 3: Restart to apply config
          steps.push("Restarting with new config...");
          await sc.restart();
          steps.push("Ready");
        } else {
          steps.push("Ready (no config changes)");
        }

        return {
          content: [{ type: "text", text: steps.join("\n") }],
        };
      }

      case "sc_execute": {
        const code = (args as { code: string }).code;
        if (!code) {
          return {
            content: [{ type: "text", text: "Error: code parameter is required" }],
            isError: true,
          };
        }
        const result = await sc.execute(code);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "sc_stop": {
        await sc.stop();
        return {
          content: [{ type: "text", text: "Stopped all sounds" }],
        };
      }

      case "sc_free_all": {
        await sc.freeAll();
        return {
          content: [{ type: "text", text: "Freed all synth nodes" }],
        };
      }

      case "sc_restart": {
        const result = await sc.restart();
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "sc_quit": {
        await sc.quit();
        return {
          content: [{ type: "text", text: "SuperCollider quit" }],
        };
      }

      case "sc_load_synthdef": {
        const synthName = (args as { name: string }).name;
        const synthdef = getSynthDef(synthName);
        if (!synthdef) {
          return {
            content: [
              {
                type: "text",
                text: `Unknown synthdef: ${synthName}. Available: ${getSynthDefNames().join(", ")}`,
              },
            ],
            isError: true,
          };
        }
        await sc.execute(synthdef.code);
        return {
          content: [
            {
              type: "text",
              text: `Loaded \\${synthdef.name}: ${synthdef.description}\nParams: ${synthdef.params.join(", ")}\nPlay with: Synth(\\${synthdef.name}, [freq: 440, amp: 0.5])`,
            },
          ],
        };
      }

      case "sc_status": {
        const result = await sc.execute(`
          "Synths: " ++ s.numSynths ++
          ", Avg CPU: " ++ s.avgCPU.round(0.1) ++ "%" ++
          ", Peak CPU: " ++ s.peakCPU.round(0.1) ++ "%"
        `);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "sc_audio_devices": {
        const result = await sc.execute(`
          ServerOptions.devices.collect { |dev, i|
            "DEV:" ++ i ++ ":" ++ dev
          }.join("\\n")
        `);
        const lines = result.split("\n");
        const devices: string[] = [];
        for (const line of lines) {
          if (line.startsWith("DEV:")) {
            const parts = line.split(":");
            if (parts.length >= 3) {
              devices.push(parts.slice(2).join(":"));
            }
          }
        }
        let text = "Audio Devices:\n";
        if (devices.length === 0) {
          text += "  (none found)\n";
        } else {
          for (const dev of devices) {
            text += `  - ${dev}\n`;
          }
        }
        return {
          content: [{ type: "text", text }],
        };
      }

      case "sc_audio_config": {
        const {
          outputDevice,
          inputDevice,
          sampleRate,
          blockSize,
          numOutputs,
          numInputs,
        } = args as {
          outputDevice?: string;
          inputDevice?: string;
          sampleRate?: number;
          blockSize?: number;
          numOutputs?: number;
          numInputs?: number;
        };

        const changes: string[] = [];

        // Apply settings if provided
        if (outputDevice !== undefined) {
          await sc.execute(`s.options.outDevice = "${outputDevice}";`);
          changes.push(`Output device: ${outputDevice}`);
        }
        if (inputDevice !== undefined) {
          await sc.execute(`s.options.inDevice = "${inputDevice}";`);
          changes.push(`Input device: ${inputDevice}`);
        }
        if (sampleRate !== undefined) {
          await sc.execute(`s.options.sampleRate = ${sampleRate};`);
          changes.push(`Sample rate: ${sampleRate}`);
        }
        if (blockSize !== undefined) {
          await sc.execute(`s.options.hardwareBufferSize = ${blockSize};`);
          changes.push(`Block size: ${blockSize}`);
        }
        if (numOutputs !== undefined) {
          await sc.execute(`s.options.numOutputBusChannels = ${numOutputs};`);
          changes.push(`Output channels: ${numOutputs}`);
        }
        if (numInputs !== undefined) {
          await sc.execute(`s.options.numInputBusChannels = ${numInputs};`);
          changes.push(`Input channels: ${numInputs}`);
        }

        // Get current config
        const config = await sc.execute(`
          "outDevice:" ++ (s.options.outDevice ? "default") ++
          "|inDevice:" ++ (s.options.inDevice ? "default") ++
          "|sampleRate:" ++ s.options.sampleRate ++
          "|blockSize:" ++ s.options.hardwareBufferSize ++
          "|numOutputs:" ++ s.options.numOutputBusChannels ++
          "|numInputs:" ++ s.options.numInputBusChannels
        `);

        const settings: Record<string, string> = {};
        for (const part of config.split("|")) {
          const [key, ...rest] = part.split(":");
          if (key) {
            settings[key.trim()] = rest.join(":").trim();
          }
        }

        let text = "";
        if (changes.length > 0) {
          text += "Changed:\n";
          for (const change of changes) {
            text += `  ✓ ${change}\n`;
          }
          text += "\nNote: Restart server (sc_restart) for changes to take effect.\n\n";
        }

        text += "Current Audio Config:\n";
        text += `  Output device: ${settings["outDevice"] || "default"}\n`;
        text += `  Input device: ${settings["inDevice"] || "default"}\n`;
        text += `  Sample rate: ${settings["sampleRate"] || "?"}\n`;
        text += `  Block size: ${settings["blockSize"] || "?"}\n`;
        text += `  Output channels: ${settings["numOutputs"] || "?"}\n`;
        text += `  Input channels: ${settings["numInputs"] || "?"}\n`;

        return {
          content: [{ type: "text", text }],
        };
      }

      case "midi_list_devices": {
        const devices = await midi.listDevices();
        let text = "MIDI Devices:\n\nInputs:\n";
        if (devices.inputs.length === 0) {
          text += "  (none)\n";
        } else {
          for (const d of devices.inputs) {
            text += `  ${d.index}: ${d.device} - ${d.name}\n`;
          }
        }
        text += "\nOutputs:\n";
        if (devices.outputs.length === 0) {
          text += "  (none)\n";
        } else {
          for (const d of devices.outputs) {
            text += `  ${d.index}: ${d.device} - ${d.name}\n`;
          }
        }
        return {
          content: [{ type: "text", text }],
        };
      }

      case "midi_connect": {
        const { device, direction } = args as {
          device: string;
          direction: "in" | "out";
        };
        const result = await midi.connect(device, direction);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_disconnect": {
        const { direction, device } = args as {
          direction: "in" | "out" | "all";
          device?: string;
        };
        const result = await midi.disconnect(direction, device);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_get_connections": {
        const connections = midi.getConnections();
        let text = "MIDI Connections:\n\nInputs:\n";
        if (connections.inputs.length === 0) {
          text += "  (none connected)\n";
        } else {
          for (const d of connections.inputs) {
            text += `  ${d.index}: ${d.device} - ${d.name}\n`;
          }
        }
        text += "\nOutput:\n";
        if (connections.output === null) {
          text += "  (none connected)\n";
        } else {
          text += `  ${connections.output.index}: ${connections.output.device} - ${connections.output.name}\n`;
        }
        return {
          content: [{ type: "text", text }],
        };
      }

      case "midi_map_notes": {
        const { synthName, channel, velocityToAmp, mono } = args as {
          synthName: string;
          channel?: number;
          velocityToAmp?: boolean;
          mono?: boolean;
        };
        const result = await midi.mapNotes(synthName, {
          channel,
          velocityToAmp,
          mono,
        });
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_map_cc": {
        const { cc, busName, range, curve, channel } = args as {
          cc: number;
          busName: string;
          range?: [number, number];
          curve?: "linear" | "exponential";
          channel?: number;
        };
        const result = await midi.mapCC(cc, busName, { range, curve, channel });
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_learn": {
        const { timeout } = args as { timeout?: number };
        const result = await midi.learn(timeout);
        if (result === null) {
          return {
            content: [
              {
                type: "text",
                text: "MIDI learn timed out. No CC was detected.",
              },
            ],
            isError: true,
          };
        }
        return {
          content: [
            {
              type: "text",
              text: `Detected CC ${result.cc} on channel ${result.channel} (value: ${result.value})`,
            },
          ],
        };
      }

      case "midi_send": {
        const { type: msgType, channel, note, velocity, cc, value } = args as {
          type: "note_on" | "note_off" | "cc" | "program";
          channel?: number;
          note?: number;
          velocity?: number;
          cc?: number;
          value?: number;
        };
        const result = await midi.send(msgType, {
          channel,
          note,
          velocity,
          cc,
          value,
        });
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_get_recent": {
        const { count, type: eventType, since } = args as {
          count?: number;
          type?: "notes" | "cc" | "all";
          since?: number;
        };
        const result = await midi.getRecent({ count, type: eventType, since });
        if (result.events.length === 0) {
          return {
            content: [{ type: "text", text: "No recent MIDI events." }],
          };
        }
        let text = `Recent MIDI events (${result.events.length}):\n`;
        for (const e of result.events) {
          if (e.type === "noteOn" || e.type === "noteOff") {
            text += `  ${e.type}: note ${e.note}, vel ${e.velocity}, ch ${e.channel}\n`;
          } else if (e.type === "cc") {
            text += `  cc: ${e.cc} = ${e.value}, ch ${e.channel}\n`;
          }
        }
        return {
          content: [{ type: "text", text }],
        };
      }

      case "midi_clear_mappings": {
        const result = await midi.clearMappings();
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "midi_play_pattern": {
        const { name, notes, durations, velocities, channel } = args as {
          name: string;
          notes: number[];
          durations: number[];
          velocities: number[];
          channel?: number;
        };
        const result = await midi.playPattern(
          name,
          notes,
          durations,
          velocities,
          channel ?? 0
        );
        return {
          content: [{ type: "text", text: result }],
        };
      }

      // Effects tools
      case "fx_load": {
        const { name: effectName, slot } = args as {
          name: string;
          slot?: string;
        };
        const result = await effects.load(effectName, slot);
        return {
          content: [
            {
              type: "text",
              text: `Loaded effect: ${result.slot}\nInput bus: ${result.inputBus}\nParams: ${Object.keys(result.params).join(", ")}\n${result.usage}`,
            },
          ],
        };
      }

      case "fx_set": {
        const { slot, params } = args as {
          slot: string;
          params: Record<string, number>;
        };
        const result = await effects.set(slot, params);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "fx_chain": {
        const { name: chainName, effects: chainEffects } = args as {
          name: string;
          effects: Array<{ name: string; params?: Record<string, number> }>;
        };
        const result = await effects.chain(chainName, chainEffects);
        let text = `Created chain: ${result.name}\nInput bus: ${result.inputBus}\nEffects: ${result.effects.map((e) => e.name).join(" → ")}\n${result.usage}`;
        return {
          content: [{ type: "text", text }],
        };
      }

      case "fx_route": {
        const { source, target } = args as {
          source: string;
          target: string;
        };
        const result = await effects.route(source, target);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "fx_bypass": {
        const { slot, bypass = true } = args as {
          slot: string;
          bypass?: boolean;
        };
        const result = await effects.bypass(slot, bypass);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "fx_remove": {
        const { slot } = args as { slot: string };
        const result = await effects.remove(slot);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "fx_list": {
        const result = await effects.list();
        let text = "Loaded Effects:\n";
        if (result.effects.length === 0) {
          text += "  (none)\n";
        } else {
          for (const effect of result.effects) {
            const chainInfo = effect.chain ? ` (chain: ${effect.chain})` : "";
            const bypassInfo = effect.bypassed ? " [BYPASSED]" : "";
            text += `  ${effect.slot} (${effect.type})${chainInfo}${bypassInfo}\n`;
            text += `    Params: ${Object.entries(effect.params)
              .map(([k, v]) => `${k}=${v.default}`)
              .join(", ")}\n`;
          }
        }
        text += "\nChains:\n";
        if (result.chains.length === 0) {
          text += "  (none)\n";
        } else {
          for (const chain of result.chains) {
            text += `  ${chain.name}: ${chain.effects.join(" → ")}\n`;
            text += `    Input bus: ${chain.inputBus}\n`;
          }
        }
        text += "\nSidechains:\n";
        if (result.sidechains.length === 0) {
          text += "  (none)\n";
        } else {
          for (const sc of result.sidechains) {
            text += `  ${sc.name}\n`;
            text += `    Audio input: ${sc.inputBus}\n`;
            text += `    Trigger input: ${sc.sidechainBus}\n`;
          }
        }
        return {
          content: [{ type: "text", text }],
        };
      }

      case "fx_sidechain": {
        const { name: scName, threshold, ratio, attack, release } = args as {
          name: string;
          threshold?: number;
          ratio?: number;
          attack?: number;
          release?: number;
        };
        const result = await effects.sidechain(scName, {
          threshold,
          ratio,
          attack,
          release,
        });
        return {
          content: [
            {
              type: "text",
              text: `Created sidechain: ${result.name}\nAudio input: ${result.inputBus}\nTrigger input: ${result.sidechainBus}\n${result.usage}`,
            },
          ],
        };
      }

      case "fx_sidechain_set": {
        const { name: scName, threshold, ratio, attack, release } = args as {
          name: string;
          threshold?: number;
          ratio?: number;
          attack?: number;
          release?: number;
        };
        const result = await effects.setSidechain(scName, {
          threshold,
          ratio,
          attack,
          release,
        });
        return {
          content: [{ type: "text", text: result }],
        };
      }

      case "fx_sidechain_remove": {
        const { name: scName } = args as { name: string };
        const result = await effects.removeSidechain(scName);
        return {
          content: [{ type: "text", text: result }],
        };
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown tool: ${name}` }],
          isError: true,
        };
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return {
      content: [{ type: "text", text: `Error: ${message}` }],
      isError: true,
    };
  }
});

// List available resources
server.setRequestHandler(ListResourcesRequestSchema, async () => {
  return {
    resources: [
      {
        uri: "supercollider://synthdefs",
        name: "Available SynthDefs",
        description: "List of pre-built synthesizer definitions with parameters",
        mimeType: "text/plain",
      },
      {
        uri: "supercollider://examples",
        name: "Code Examples",
        description: "Common SuperCollider code snippets for reference",
        mimeType: "text/plain",
      },
    ],
  };
});

// Read resource content
server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const { uri } = request.params;

  switch (uri) {
    case "supercollider://synthdefs": {
      const content = Object.values(synthdefs)
        .map((s) => {
          return `\\${s.name} - ${s.description}\n  Params: ${s.params.join(", ")}\n  Usage: Synth(\\${s.name}, [freq: 440, amp: 0.5])`;
        })
        .join("\n\n");
      return {
        contents: [
          {
            uri,
            mimeType: "text/plain",
            text: `Available SynthDefs:\n\n${content}`,
          },
        ],
      };
    }

    case "supercollider://examples": {
      const examples = `SuperCollider Code Examples:

// Play a simple sine wave
{ SinOsc.ar(440) * 0.3 }.play;

// Play a chord
{ SinOsc.ar([261, 329, 392]) * 0.2 }.play;

// Use a built-in synthdef (after loading with sc_load_synthdef)
Synth(\\kick);
Synth(\\snare, [amp: 0.4]);
Synth(\\acid, [freq: 110, cutoff: 2000, res: 0.2]);

// Simple drum pattern with Pbind
Pbind(
  \\instrument, \\kick,
  \\dur, 1,
  \\amp, 0.5
).play;

// Layered pattern
Ppar([
  Pbind(\\instrument, \\kick, \\dur, 1),
  Pbind(\\instrument, \\hihat, \\dur, 0.25, \\amp, 0.2),
]).play;

// Stop all sounds
CmdPeriod.run;
`;
      return {
        contents: [
          {
            uri,
            mimeType: "text/plain",
            text: examples,
          },
        ],
      };
    }

    default:
      throw new Error(`Unknown resource: ${uri}`);
  }
});

// Handle graceful shutdown
process.on("SIGINT", async () => {
  await sc.quit();
  process.exit(0);
});

process.on("SIGTERM", async () => {
  await sc.quit();
  process.exit(0);
});

// Start the server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  console.error("Server error:", error);
  process.exit(1);
});
