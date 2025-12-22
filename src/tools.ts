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
    await this.sc.waitForCCReady()
    await this.synthdefs.load()
    await this.effects.load()
    await this.samples.load()
    this.initialized = true

    return await this.formatBootReadme()
  }

  private async formatBootReadme(): Promise<string> {
    const synthList = await this.sc.execute("~cc.synths.list")
    const fxList = await this.sc.execute("~cc.fx.list")
    const sampleList = await this.sc.execute("~cc.samples.list")

    return `# ClaudeCollider Ready

## Quick Reference
  Synth(\\\\cc_kick)                    # one-shot synth
  Pdef(\\\\beat, Pbind(...)).play       # pattern (use for beats)
  Ndef(\\\\pad, { ... }).play           # continuous synth
  Ppar([Pbind(...), Pbind(...)])     # sync multiple patterns

## Drum Pattern Template
  Pdef(\\\\kick, Pbind(\\\\instrument, \\\\cc_kick, \\\\freq, 48, \\\\dur, 1, \\\\amp, 0.7)).play

  ⚠️  \\\\freq, 48 is REQUIRED for all drum synths!
  Without it, drums play at ~261Hz (middle C) and sound wrong.

## Tools Reference
- fx_load({name}): Load effect, returns slot name
- fx_manage({action, slot, params?, bypass?}): set/bypass/remove effects
- fx_wire({source, target, type?, passthrough?}): source→fx, fx→fx, or sidechain
- fx_chain({name, effects}): Create named effect chain
- fx_sidechain({name, ...}): Create sidechain compressor
- midi({action, ...}): list/connect/play/stop
- sample({action, name?, ...}): inspect/load/play/free/reload
- recording({action, filename?}): start/stop/status
- output({action, source?, outputs?}): route/unroute/status

## Gotchas
- sc_execute: semicolons between statements, no trailing semicolon
- NEVER call Synth() inside Ndef — infinite spawning

## Synths: ${synthList}
## Effects: ${fxList}
## Samples: ${sampleList}

Use synth_inspect, fx_inspect, sample({action: "inspect"}) for details.
Use sample({action: "load", name}) before patterns to avoid latency.`
  }

  // Core SC tools
  async sc_execute(args: { code: string }): Promise<ToolResult> {
    const bootMessage = await this.ensureReady()
    const { code } = args
    if (!code) {
      return {
        content: [{ type: "text", text: "Error: code parameter is required" }],
        isError: true,
      }
    }
    const result = await this.sc.execute(code)
    const text = bootMessage ? `${bootMessage}\n\n---\n\n${result}` : result
    return { content: [{ type: "text", text }] }
  }

  async sc_stop(): Promise<ToolResult> {
    await this.ensureReady()
    await this.sc.execute("~cc.stop")
    return { content: [{ type: "text", text: "Stopped all sounds" }] }
  }

  async sc_status(): Promise<ToolResult> {
    await this.ensureReady()
    const result = await this.sc.execute("~cc.status")
    return { content: [{ type: "text", text: result }] }
  }

  async sc_tempo(args: { bpm?: number }): Promise<ToolResult> {
    await this.ensureReady()
    const { bpm } = args
    if (bpm !== undefined) {
      await this.sc.execute(`~cc.tempo(${bpm})`)
      return { content: [{ type: "text", text: `Tempo set to ${bpm} BPM` }] }
    } else {
      const result = await this.sc.execute("~cc.tempo")
      return { content: [{ type: "text", text: `Current tempo: ${result} BPM` }] }
    }
  }

  async sc_clear(): Promise<ToolResult> {
    await this.ensureReady()
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

  async sc_reboot(args: { device?: string; numOutputs?: number }): Promise<ToolResult> {
    await this.ensureReady()
    const { device, numOutputs } = args
    const deviceArg = device ? `"${device}"` : "nil"
    const numOutputsArg = numOutputs ?? "nil"
    await this.sc.execute(`~cc.reboot(${deviceArg}, ${numOutputsArg})`)
    await this.sc.waitForCCReady()
    const parts = []
    if (device) parts.push(`device: ${device}`)
    if (numOutputs) parts.push(`outputs: ${numOutputs}`)
    return {
      content: [
        {
          type: "text",
          text: `Rebooted${parts.length ? ` with ${parts.join(", ")}` : ""}`,
        },
      ],
    }
  }

  async sc_audio_devices(): Promise<ToolResult> {
    await this.ensureReady()
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

  // Consolidated MIDI tool
  async midi(args: {
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

  // FX tools
  async fx_load(args: { name: string; slot?: string }): Promise<ToolResult> {
    await this.ensureReady()
    const { name: effectName, slot } = args
    const slotArg = slot ? `\\${slot}` : "nil"
    const result = await this.sc.execute(`~cc.fx.load(\\${effectName}, ${slotArg})`)
    return {
      content: [
        { type: "text", text: `Loaded effect: ${slot || `fx_${effectName}`}\n${result}` },
      ],
    }
  }

  async fx_manage(args: {
    action: "set" | "bypass" | "remove"
    slot: string
    params?: Record<string, number>
    bypass?: boolean
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { action, slot, params, bypass = true } = args

    switch (action) {
      case "set": {
        if (!params) {
          return {
            content: [{ type: "text", text: "Error: params required for action=set" }],
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
        await this.sc.execute(`~cc.fx.bypass(\\${slot}, ${bypass})`)
        return {
          content: [{ type: "text", text: bypass ? `Bypassed ${slot}` : `Enabled ${slot}` }],
        }
      }

      case "remove": {
        await this.sc.execute(`~cc.fx.remove(\\${slot})`)
        return { content: [{ type: "text", text: `Removed ${slot}` }] }
      }

      default:
        return {
          content: [{ type: "text", text: `Unknown fx_manage action: ${action}` }],
          isError: true,
        }
    }
  }

  async fx_wire(args: {
    source: string
    target: string
    type?: "source" | "effect" | "sidechain"
    passthrough?: boolean
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { source, target, type = "source", passthrough = true } = args

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
          content: [{ type: "text", text: `Unknown fx_wire type: ${type}` }],
          isError: true,
        }
    }
  }

  async fx_sidechain(args: {
    name: string
    threshold?: number
    ratio?: number
    attack?: number
    release?: number
  }): Promise<ToolResult> {
    await this.ensureReady()
    const {
      name: scName,
      threshold = 0.1,
      ratio = 4,
      attack = 0.01,
      release = 0.1,
    } = args
    const result = await this.sc.execute(
      `~cc.fx.sidechain(\\${scName}, ${threshold}, ${ratio}, ${attack}, ${release})`
    )
    return {
      content: [{ type: "text", text: `Created sidechain: ${scName}\n${result}` }],
    }
  }

  async fx_chain(args: {
    name: string
    effects: Array<string | { name: string; params?: Record<string, number> }>
  }): Promise<ToolResult> {
    await this.ensureReady()
    const { name: chainName, effects: fxList } = args

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

  async fx_inspect(): Promise<ToolResult> {
    await this.ensureReady()
    const result = await this.sc.execute("~cc.fx.describe")
    return { content: [{ type: "text", text: result }] }
  }

  // Synth tools
  async synth_inspect(): Promise<ToolResult> {
    await this.ensureReady()
    const result = await this.sc.execute("~cc.synths.describe")
    return { content: [{ type: "text", text: result }] }
  }

  // Consolidated sample tool
  async sample(args: {
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

  // Consolidated recording tool
  async recording(args: {
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

  // Consolidated output tool
  async output(args: {
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

  // Debug tools
  async routing_debug(): Promise<ToolResult> {
    await this.ensureReady()
    const result = await this.sc.execute("~cc.formatter.formatRoutingDebug")
    return { content: [{ type: "text", text: result }] }
  }
}
