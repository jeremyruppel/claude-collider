import { SuperCollider } from "./supercollider.js"
import { SynthDefs } from "./synthdefs.js"
import { Effects } from "./effects.js"
import { Samples } from "./samples.js"

export interface ToolResult {
  content: { type: "text"; text: string }[]
  isError?: boolean
  [key: string]: unknown
}

// Helper to format SC params from object
function formatParams(params: Record<string, unknown>): string {
  return Object.entries(params)
    .map(([k, v]) => `\\${k}, ${v}`)
    .join(", ")
}

export class Tools {
  private readonly sc: SuperCollider
  private readonly samples: Samples
  private readonly synthdefs: SynthDefs
  private readonly effects: Effects
  private initialized = false

  constructor(
    sc: SuperCollider,
    samples: Samples,
    synthdefs: SynthDefs,
    effects: Effects
  ) {
    this.sc = sc
    this.samples = samples
    this.synthdefs = synthdefs
    this.effects = effects
  }

  private async ensureReady(): Promise<string | null> {
    if (this.initialized) return null

    // execute() auto-boots sclang if needed
    await this.sc.execute(
      `~cc = CC.boot(device: nil, numOutputs: nil, samplesDir: "${this.sc.getSamplesPath()}", recordingsDir: "${this.sc.getRecordingsPath()}")`
    )
    const serverOutput = await this.sc.waitForCCReady()
    await this.synthdefs.load()
    await this.effects.load()
    await this.samples.load()
    this.initialized = true

    // Return server output + status on first boot
    const status = await this.sc.execute("~cc.status")
    return `${serverOutput}\n\n${status}`
  }

  // ============================================================
  // cc_execute - Run arbitrary SuperCollider code
  // ============================================================
  async cc_execute(args: { code: string }): Promise<ToolResult> {
    const bootStatus = await this.ensureReady()
    const { code } = args
    if (!code) {
      return {
        content: [{ type: "text", text: "Error: code parameter is required" }],
        isError: true,
      }
    }
    const result = await this.sc.execute(code)
    // On first boot, prepend status; otherwise just return result
    const text = bootStatus ? `${bootStatus}\n\n${result}` : result
    return { content: [{ type: "text", text }] }
  }

  // ============================================================
  // cc_status - Show status, routing, synths, or effects info
  // ============================================================
  async cc_status(args: {
    action?: "status" | "routing" | "synths" | "effects"
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action = "status" } = args

    switch (action) {
      case "status": {
        const result = await this.sc.execute("~cc.status")
        return { content: [{ type: "text", text: result }] }
      }

      case "routing": {
        const result = await this.sc.execute("~cc.formatter.formatRoutingDebug")
        return { content: [{ type: "text", text: result }] }
      }

      case "synths": {
        const result = await this.sc.execute("~cc.synths.describe")
        return { content: [{ type: "text", text: result }] }
      }

      case "effects": {
        const result = await this.sc.execute("~cc.fx.describe")
        return { content: [{ type: "text", text: result }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown status action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_reboot - Reboot server or list audio devices
  // ============================================================
  async cc_reboot(args: {
    action?: "reboot" | "devices"
    device?: string
    numOutputs?: number
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action = "reboot", device, numOutputs } = args

    switch (action) {
      case "reboot": {
        const deviceArg = device ? `"${device}"` : "nil"
        const numOutputsArg = numOutputs ?? "nil"
        await this.sc.execute(`~cc.reboot(${deviceArg}, ${numOutputsArg})`)
        const serverOutput = await this.sc.waitForCCReady()
        const status = await this.sc.execute("~cc.status")
        return {
          content: [
            {
              type: "text",
              text: `${serverOutput}\n\n${status}`,
            },
          ],
        }
      }

      case "devices": {
        const result = await this.sc.execute(`
          ServerOptions.devices.collect { |dev, i|
            "DEV:" ++ i ++ ":" ++ dev
          }.join("\\\\n")
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
        return { content: [{ type: "text", text }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown reboot action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_control - Stop, clear, or set tempo
  // ============================================================
  async cc_control(args: {
    action: "stop" | "clear" | "tempo"
    bpm?: number
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, bpm } = args

    switch (action) {
      case "stop": {
        await this.sc.execute("~cc.stop")
        return { content: [{ type: "text", text: "Stopped all sounds" }] }
      }

      case "clear": {
        await this.sc.execute("~cc.clear")
        return {
          content: [
            {
              type: "text",
              text: "Cleared all sounds, patterns, effects, and MIDI mappings",
            },
          ],
        }
      }

      case "tempo": {
        if (bpm !== undefined) {
          await this.sc.execute(`~cc.tempo(${bpm})`)
          return { content: [{ type: "text", text: `Tempo set to ${bpm} BPM` }] }
        } else {
          const result = await this.sc.execute("~cc.tempo")
          return { content: [{ type: "text", text: `Current tempo: ${result} BPM` }] }
        }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown control action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_fx - All effects operations
  // ============================================================
  async cc_fx(args: {
    action: "load" | "set" | "bypass" | "remove" | "wire" | "sidechain" | "chain"
    // For load
    name?: string
    slot?: string
    // For set/bypass/remove
    params?: Record<string, number>
    bypass?: boolean
    // For wire
    source?: string
    target?: string
    type?: "source" | "effect" | "sidechain"
    passthrough?: boolean
    // For sidechain
    threshold?: number
    ratio?: number
    attack?: number
    release?: number
    // For chain
    effects?: Array<string | { name: string; params?: Record<string, number> }>
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action } = args

    switch (action) {
      case "load": {
        const { name: effectName, slot } = args
        if (!effectName) {
          return {
            content: [{ type: "text", text: "Error: name required for load" }],
            isError: true,
          }
        }
        const slotArg = slot ? `\\${slot}` : "nil"
        const result = await this.sc.execute(`~cc.fx.load(\\${effectName}, ${slotArg})`)
        return {
          content: [
            { type: "text", text: `Loaded effect: ${slot || `fx_${effectName}`}\n${result}` },
          ],
        }
      }

      case "set": {
        const { slot, params } = args
        if (!slot) {
          return {
            content: [{ type: "text", text: "Error: slot required for set" }],
            isError: true,
          }
        }
        if (!params) {
          return {
            content: [{ type: "text", text: "Error: params required for set" }],
            isError: true,
          }
        }
        const paramStr = formatParams(params)
        await this.sc.execute(`~cc.fx.set(\\${slot}, ${paramStr})`)
        return {
          content: [{ type: "text", text: `Set ${Object.keys(params).join(", ")} on ${slot}` }],
        }
      }

      case "bypass": {
        const { slot, bypass = true } = args
        if (!slot) {
          return {
            content: [{ type: "text", text: "Error: slot required for bypass" }],
            isError: true,
          }
        }
        await this.sc.execute(`~cc.fx.bypass(\\${slot}, ${bypass})`)
        return {
          content: [{ type: "text", text: bypass ? `Bypassed ${slot}` : `Enabled ${slot}` }],
        }
      }

      case "remove": {
        const { slot } = args
        if (!slot) {
          return {
            content: [{ type: "text", text: "Error: slot required for remove" }],
            isError: true,
          }
        }
        await this.sc.execute(`~cc.fx.remove(\\${slot})`)
        return { content: [{ type: "text", text: `Removed ${slot}` }] }
      }

      case "wire": {
        const { source, target, type = "source", passthrough = true } = args
        if (!source || !target) {
          return {
            content: [{ type: "text", text: "Error: source and target required for wire" }],
            isError: true,
          }
        }

        switch (type) {
          case "source": {
            await this.sc.execute(`~cc.fx.route(\\${source}, \\${target})`)
            return { content: [{ type: "text", text: `Routed ${source} → ${target}` }] }
          }

          case "effect": {
            const result = await this.sc.execute(`~cc.fx.connect(\\${source}, \\${target})`)
            if (result.includes("nil")) {
              return {
                content: [{ type: "text", text: `Failed to connect ${source} → ${target}` }],
                isError: true,
              }
            }
            return { content: [{ type: "text", text: `Connected ${source} → ${target}` }] }
          }

          case "sidechain": {
            await this.sc.execute(`~cc.fx.routeTrigger(\\${source}, \\${target}, ${passthrough})`)
            const passthroughNote = passthrough
              ? " (passthrough enabled - source still audible)"
              : " (passthrough disabled - source redirected to trigger only)"
            return {
              content: [
                { type: "text", text: `Routed ${source} as trigger for sidechain ${target}${passthroughNote}` },
              ],
            }
          }

          default:
            return {
              content: [{ type: "text", text: `Unknown wire type: ${type}` }],
              isError: true,
            }
        }
      }

      case "sidechain": {
        const {
          name: scName,
          threshold = 0.1,
          ratio = 4,
          attack = 0.01,
          release = 0.1,
        } = args
        if (!scName) {
          return {
            content: [{ type: "text", text: "Error: name required for sidechain" }],
            isError: true,
          }
        }
        const result = await this.sc.execute(
          `~cc.fx.sidechain(\\${scName}, ${threshold}, ${ratio}, ${attack}, ${release})`
        )
        return {
          content: [{ type: "text", text: `Created sidechain: ${scName}\n${result}` }],
        }
      }

      case "chain": {
        const { name: chainName, effects: fxList } = args
        if (!chainName) {
          return {
            content: [{ type: "text", text: "Error: name required for chain" }],
            isError: true,
          }
        }
        if (!fxList || fxList.length === 0) {
          return {
            content: [{ type: "text", text: "Error: effects array cannot be empty" }],
            isError: true,
          }
        }

        const slots: string[] = []
        const results: string[] = []

        for (let i = 0; i < fxList.length; i++) {
          const fx = fxList[i]
          const fxName = typeof fx === "string" ? fx : fx.name
          const fxParams = typeof fx === "object" && fx.params ? fx.params : null
          const slotName = `${chainName}_${i}_${fxName}`

          const loadResult = await this.sc.execute(`~cc.fx.load(\\${fxName}, \\${slotName})`)
          if (loadResult.includes("unknown effect")) {
            return {
              content: [
                { type: "text", text: `Error: Unknown effect '${fxName}' at position ${i}` },
              ],
              isError: true,
            }
          }

          if (fxParams) {
            const paramStr = formatParams(fxParams)
            await this.sc.execute(`~cc.fx.set(\\${slotName}, ${paramStr})`)
          }

          slots.push(slotName)
          results.push(`  → ${slotName}`)
        }

        for (let i = 0; i < slots.length - 1; i++) {
          await this.sc.execute(`~cc.fx.connect(\\${slots[i]}, \\${slots[i + 1]})`)
        }

        const slotsArray = slots.map((s) => `\\${s}`).join(", ")
        await this.sc.execute(`~cc.fx.registerChain(\\${chainName}, [${slotsArray}])`)

        results.push("  → main out")
        results.unshift(`Created chain: ${chainName}`)
        results.push(`Route sources to: ${slots[0]}`)

        return { content: [{ type: "text", text: results.join("\n") }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown fx action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_midi - MIDI operations
  // ============================================================
  async cc_midi(args: {
    action: "list" | "connect" | "play" | "stop"
    device?: string
    direction?: "in" | "out" | "all"
    synth?: string
    channel?: number
    mono?: boolean
    velToAmp?: boolean
    cc?: Record<
      string,
      string | { param: string; range?: [number, number]; curve?: "lin" | "exp" }
    >
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, device, direction, synth, channel, mono, velToAmp, cc } = args

    switch (action) {
      case "list": {
        const result = await this.sc.execute("~cc.midi.listDevices")
        return { content: [{ type: "text", text: result }] }
      }

      case "connect": {
        if (!device || !direction) {
          return {
            content: [{ type: "text", text: "Error: device and direction required for connect" }],
            isError: true,
          }
        }
        if (direction === "all") {
          await this.sc.execute("~cc.midi.connectAll")
          return { content: [{ type: "text", text: "Connected all MIDI inputs" }] }
        }
        const isIndex = /^\d+$/.test(device)
        const deviceArg = isIndex ? device : `"${device}"`
        await this.sc.execute(`~cc.midi.connect(${deviceArg}, \\${direction})`)
        return {
          content: [{ type: "text", text: `Connected MIDI ${direction}: ${device}` }],
        }
      }

      case "play": {
        if (!synth) {
          return {
            content: [{ type: "text", text: "Error: synth required for play" }],
            isError: true,
          }
        }
        const chanArg = channel !== undefined ? channel : "nil"
        const monoArg = mono ?? false
        const velArg = velToAmp ?? true

        let ccArg = "nil"
        if (cc && Object.keys(cc).length > 0) {
          const mappings = Object.entries(cc).map(([ccNum, mapping]) => {
            if (typeof mapping === "string") {
              return `${ccNum} -> \\${mapping}`
            } else {
              const range = mapping.range
                ? `[${mapping.range[0]}, ${mapping.range[1]}]`
                : "[0, 1]"
              const curve = mapping.curve ? `\\${mapping.curve}` : "\\lin"
              return `${ccNum} -> (param: \\${mapping.param}, range: ${range}, curve: ${curve})`
            }
          })
          ccArg = `Dictionary[${mappings.join(", ")}]`
        }

        await this.sc.execute(
          `~cc.midi.play(\\${synth}, ${chanArg}, ${monoArg}, ${velArg}, ${ccArg})`
        )

        const modeStr = monoArg ? "mono" : "poly"
        const chanStr = channel !== undefined ? `ch ${channel}` : "all ch"
        const ccStr = cc ? `, CC: ${Object.keys(cc).join(",")}` : ""
        return {
          content: [{ type: "text", text: `MIDI → ${synth} (${modeStr}, ${chanStr}${ccStr})` }],
        }
      }

      case "stop": {
        await this.sc.execute("~cc.midi.stop")
        return { content: [{ type: "text", text: "MIDI synth stopped" }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown MIDI action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_sample - Sample operations
  // ============================================================
  async cc_sample(args: {
    action: "inspect" | "load" | "play" | "free" | "reload"
    name?: string
    rate?: number
    amp?: number
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, name, rate = 1, amp = 0.5 } = args

    switch (action) {
      case "inspect": {
        const result = await this.sc.execute("~cc.samples.describe")
        return { content: [{ type: "text", text: result }] }
      }

      case "load": {
        if (!name) {
          return {
            content: [{ type: "text", text: "Error: name required for load" }],
            isError: true,
          }
        }
        await this.sc.execute(`~cc.samples.load("${name}")`)
        return { content: [{ type: "text", text: `Loading sample "${name}"` }] }
      }

      case "play": {
        if (!name) {
          return {
            content: [{ type: "text", text: "Error: name required for play" }],
            isError: true,
          }
        }
        await this.samples.play(name, rate, amp)
        return {
          content: [{ type: "text", text: `Playing "${name}" (rate: ${rate}, amp: ${amp})` }],
        }
      }

      case "free": {
        if (!name) {
          return {
            content: [{ type: "text", text: "Error: name required for free" }],
            isError: true,
          }
        }
        await this.samples.free(name)
        return { content: [{ type: "text", text: `Freed sample "${name}"` }] }
      }

      case "reload": {
        await this.sc.execute("~cc.samples.reload")
        const sampleList = await this.samples.format()
        return {
          content: [{ type: "text", text: `Reloaded samples directory.\n\n${sampleList}` }],
        }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown sample action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_recording - Recording operations
  // ============================================================
  async cc_recording(args: {
    action: "start" | "stop" | "status"
    filename?: string
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, filename } = args

    switch (action) {
      case "start": {
        const arg = filename ? `"${filename}"` : "nil"
        const result = await this.sc.execute(`~cc.recorder.start(${arg})`)
        return {
          content: [{ type: "text", text: result }],
          isError: result.includes("Already recording"),
        }
      }

      case "stop": {
        const result = await this.sc.execute("~cc.recorder.stop")
        return {
          content: [{ type: "text", text: result }],
          isError: result === "Not recording",
        }
      }

      case "status": {
        const result = await this.sc.execute("~cc.recorder.status")
        return { content: [{ type: "text", text: result }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown recording action: ${action}` }],
          isError: true,
        }
    }
  }

  // ============================================================
  // cc_output - Hardware output routing
  // ============================================================
  async cc_output(args: {
    action: "route" | "unroute" | "status"
    source?: string
    outputs?: number | number[]
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, source, outputs } = args

    switch (action) {
      case "route": {
        if (!source || outputs === undefined) {
          return {
            content: [{ type: "text", text: "Error: source and outputs required for route" }],
            isError: true,
          }
        }
        const outputsArg = Array.isArray(outputs) ? `[${outputs.join(", ")}]` : outputs
        await this.sc.execute(`~cc.fx.routeToOutput(\\${source}, ${outputsArg})`)
        const outputStr = Array.isArray(outputs)
          ? `outputs ${outputs.join("-")}`
          : `output ${outputs}`
        return { content: [{ type: "text", text: `Routed ${source} to ${outputStr}` }] }
      }

      case "unroute": {
        if (!source) {
          return {
            content: [{ type: "text", text: "Error: source required for unroute" }],
            isError: true,
          }
        }
        await this.sc.execute(`~cc.fx.unrouteFromOutput(\\${source})`)
        return {
          content: [{ type: "text", text: `Unrouted ${source}, now using main outputs` }],
        }
      }

      case "status": {
        const result = await this.sc.execute("~cc.fx.outputStatus")
        return { content: [{ type: "text", text: `Output Routing:\n${result}` }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown output action: ${action}` }],
          isError: true,
        }
    }
  }
}
