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

const sc = new SuperCollider();

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
        const result = await sc.execute(
          `"Synths: " ++ s.numSynths ++ ", Avg CPU: " ++ s.avgCPU.round(0.1) ++ "%, Peak CPU: " ++ s.peakCPU.round(0.1) ++ "%"`
        );
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
