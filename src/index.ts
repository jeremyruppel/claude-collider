#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { SuperCollider } from "./supercollider.js";

const sc = new SuperCollider();

const server = new Server(
  {
    name: "vibe-music",
    version: "0.1.0",
  },
  {
    capabilities: {
      tools: {},
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
