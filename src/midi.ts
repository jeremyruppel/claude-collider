import { SuperCollider } from "./supercollider.js"
import { debug } from "./debug.js"

export interface MIDIDevice {
  index: number
  device: string
  name: string
}

export interface MIDIDeviceList {
  inputs: MIDIDevice[]
  outputs: MIDIDevice[]
}

export interface MIDINoteMapping {
  id: string
  synthName: string
  channel: number | null
  mono: boolean
}

export interface MIDICCMapping {
  id: string
  cc: number
  busName: string
  range: [number, number]
  curve: "linear" | "exponential"
  channel: number | null
}

export interface MIDILearnResult {
  cc: number
  channel: number
  value: number
}

export interface MIDIEvent {
  type: "noteOn" | "noteOff" | "cc"
  note?: number
  velocity?: number
  cc?: number
  value?: number
  channel: number
  time: number
}

export interface MIDIRecentEvents {
  events: MIDIEvent[]
}

export interface MIDIConnections {
  inputs: MIDIDevice[]
  output: MIDIDevice | null
}

export class MIDIManager {
  private sc: SuperCollider
  private initialized = false
  private loggingEnabled = false
  private midiDefCounter = 0
  private noteMappings: MIDINoteMapping[] = []
  private ccMappings: MIDICCMapping[] = []
  private connectedInputs: MIDIDevice[] = []
  private connectedOutput: MIDIDevice | null = null

  constructor(supercollider: SuperCollider) {
    this.sc = supercollider
  }

  async init(): Promise<void> {
    if (this.initialized) return
    debug("MIDI: Initializing MIDIClient")
    await this.sc.execute("MIDIClient.init; MIDIIn.connectAll;")
    this.initialized = true
  }

  async listDevices(): Promise<MIDIDeviceList> {
    await this.init()

    const code = `MIDIClient.sources.collect { |src, i| "IN:" ++ i ++ ":" ++ src.device ++ ":" ++ src.name }.join("\\n") ++ "|" ++ MIDIClient.destinations.collect { |dst, i| "OUT:" ++ i ++ ":" ++ dst.device ++ ":" ++ dst.name }.join("\\n")`
    const output = await this.sc.execute(code)
    return this.parseDeviceList(output)
  }

  private parseDeviceList(output: string): MIDIDeviceList {
    const inputs: MIDIDevice[] = []
    const outputs: MIDIDevice[] = []

    // Split by | to separate inputs from outputs, then by newline for each device
    const allLines = output.split(/[|\n]/)
    for (const line of allLines) {
      if (line.startsWith("IN:")) {
        const parts = line.split(":")
        if (parts.length >= 4) {
          inputs.push({
            index: parseInt(parts[1], 10),
            device: parts[2],
            name: parts.slice(3).join(":"),
          })
        }
      } else if (line.startsWith("OUT:")) {
        const parts = line.split(":")
        if (parts.length >= 4) {
          outputs.push({
            index: parseInt(parts[1], 10),
            device: parts[2],
            name: parts.slice(3).join(":"),
          })
        }
      }
    }

    return { inputs, outputs }
  }

  async connect(device: string, direction: "in" | "out"): Promise<string> {
    await this.init()

    const isIndex = /^\d+$/.test(device)
    const devices = await this.listDevices()

    if (direction === "in") {
      // Find the device info
      let deviceInfo: MIDIDevice | undefined
      if (isIndex) {
        const idx = parseInt(device, 10)
        deviceInfo = devices.inputs.find((d) => d.index === idx)
        await this.sc.execute(
          `MIDIIn.connect(0, MIDIClient.sources[${device}]);`
        )
      } else {
        deviceInfo = devices.inputs.find(
          (d) =>
            d.device.toLowerCase().includes(device.toLowerCase()) ||
            d.name.toLowerCase().includes(device.toLowerCase())
        )
        await this.sc.execute(
          `MIDIIn.connect(0, MIDIClient.sources.detect { |x| x.device.containsi("${device}") or: { x.name.containsi("${device}") } });`
        )
      }

      // Track connection
      if (
        deviceInfo &&
        !this.connectedInputs.find((d) => d.index === deviceInfo!.index)
      ) {
        this.connectedInputs.push(deviceInfo)
      }

      return `Connected to MIDI input: ${deviceInfo?.name ?? device}`
    } else {
      // Find the device info
      let deviceInfo: MIDIDevice | undefined
      if (isIndex) {
        const idx = parseInt(device, 10)
        deviceInfo = devices.outputs.find((d) => d.index === idx)
        await this.sc.execute(
          `~midiOut = MIDIOut(${device}); ~midiOut.connect(MIDIClient.destinations[${device}].uid);`
        )
      } else {
        deviceInfo = devices.outputs.find(
          (d) =>
            d.device.toLowerCase().includes(device.toLowerCase()) ||
            d.name.toLowerCase().includes(device.toLowerCase())
        )
        await this.sc.execute(
          `~midiOut = MIDIOut.newByName("${device}", "${device}");`
        )
      }

      // Track connection (output is single - switches if already connected)
      this.connectedOutput = deviceInfo ?? {
        index: -1,
        device: device,
        name: device,
      }

      return `Connected to MIDI output: ${this.connectedOutput.name}`
    }
  }

  async disconnect(
    direction: "in" | "out" | "all",
    device?: string
  ): Promise<string> {
    await this.init()

    if (direction === "in" || direction === "all") {
      if (device) {
        // Disconnect specific input
        const isIndex = /^\d+$/.test(device)
        if (isIndex) {
          await this.sc.execute(`MIDIIn.disconnect(0, ${device});`)
          this.connectedInputs = this.connectedInputs.filter(
            (d) => d.index !== parseInt(device, 10)
          )
        } else {
          // Find and disconnect by name
          const found = this.connectedInputs.find(
            (d) =>
              d.device.toLowerCase().includes(device.toLowerCase()) ||
              d.name.toLowerCase().includes(device.toLowerCase())
          )
          if (found) {
            await this.sc.execute(`MIDIIn.disconnect(0, ${found.index});`)
            this.connectedInputs = this.connectedInputs.filter(
              (d) => d.index !== found.index
            )
          }
        }
      } else {
        // Disconnect all inputs
        await this.sc.execute(`MIDIIn.disconnectAll;`)
        this.connectedInputs = []
      }
    }

    if (direction === "out" || direction === "all") {
      if (this.connectedOutput) {
        await this.sc.execute(`~midiOut = nil;`)
        this.connectedOutput = null
      }
    }

    if (direction === "all") {
      return "Disconnected all MIDI devices"
    } else if (direction === "in") {
      return device
        ? `Disconnected MIDI input: ${device}`
        : "Disconnected all MIDI inputs"
    } else {
      return "Disconnected MIDI output"
    }
  }

  getConnections(): MIDIConnections {
    return {
      inputs: [...this.connectedInputs],
      output: this.connectedOutput,
    }
  }

  async mapNotes(
    synthName: string,
    options: {
      channel?: number
      velocityToAmp?: boolean
      mono?: boolean
    } = {}
  ): Promise<string> {
    await this.init()

    const channel = options.channel ?? null
    const velocityToAmp = options.velocityToAmp ?? true
    const mono = options.mono ?? false
    const id = `notes_${this.midiDefCounter++}`

    const chanArg = channel === null ? "nil" : channel.toString()
    const ampExpr = velocityToAmp
      ? "vel.linlin(0, 127, 0, 0.8)"
      : "0.5"

    if (mono) {
      await this.sc.execute(
        `~monoSynth_${id} = nil; ~monoNote_${id} = nil; MIDIdef.noteOn(\\noteOn_${id}, { |vel, note, chan| if(~monoSynth_${id}.notNil) { ~monoSynth_${id}.set(\\gate, 0) }; ~monoSynth_${id} = Synth(\\${synthName}, [\\freq, note.midicps, \\amp, ${ampExpr}, \\gate, 1]); ~monoNote_${id} = note; }, chan: ${chanArg}); MIDIdef.noteOff(\\noteOff_${id}, { |vel, note, chan| if(note == ~monoNote_${id}) { ~monoSynth_${id}.!?(_.set(\\gate, 0)); ~monoSynth_${id} = nil; }; }, chan: ${chanArg});`
      )
    } else {
      await this.sc.execute(
        `~midiNotes_${id} = Array.fill(128, { nil }); MIDIdef.noteOn(\\noteOn_${id}, { |vel, note, chan| ~midiNotes_${id}[note] = Synth(\\${synthName}, [\\freq, note.midicps, \\amp, ${ampExpr}, \\gate, 1]); }, chan: ${chanArg}); MIDIdef.noteOff(\\noteOff_${id}, { |vel, note, chan| ~midiNotes_${id}[note].!?(_.set(\\gate, 0)); ~midiNotes_${id}[note] = nil; }, chan: ${chanArg});`
      )
    }

    this.noteMappings.push({ id, synthName, channel, mono })

    const modeStr = mono ? "monophonic" : "polyphonic"
    const chanStr = channel === null ? "all channels" : `channel ${channel}`
    return `Mapped MIDI notes to \\${synthName} (${modeStr}, ${chanStr})`
  }

  async mapCC(
    cc: number,
    busName: string,
    options: {
      range?: [number, number]
      curve?: "linear" | "exponential"
      channel?: number
    } = {}
  ): Promise<string> {
    await this.init()

    const range = options.range ?? [0, 1]
    const curve = options.curve ?? "linear"
    const channel = options.channel ?? null
    const id = `cc_${busName}`

    const chanArg = channel === null ? "nil" : channel.toString()
    const mapFunc = curve === "exponential" ? "linexp" : "linlin"
    const initialValue = (range[0] + range[1]) / 2

    await this.sc.execute(
      `~${busName} = ~${busName} ?? { Bus.control(s, 1) }; ~${busName}.set(${initialValue}); MIDIdef.cc(\\${id}, { |val| ~${busName}.set(val.${mapFunc}(0, 127, ${range[0]}, ${range[1]})); }, ccNum: ${cc}, chan: ${chanArg});`
    )

    this.ccMappings.push({ id, cc, busName, range, curve, channel })

    return `Mapped CC ${cc} to ~${busName} (${range[0]}-${range[1]}, ${curve}). Use ~${busName}.asMap in your synths.`
  }

  async learn(timeout: number = 10): Promise<MIDILearnResult | null> {
    await this.init()

    // Set up the learn listener
    await this.sc.execute(
      `~learnResult = nil; MIDIdef.cc(\\learn, { |val, cc, chan| ~learnResult = [cc, chan, val]; MIDIdef(\\learn).free; });`
    )

    // Poll for result
    const startTime = Date.now()
    const pollInterval = 200

    while (Date.now() - startTime < timeout * 1000) {
      await new Promise((resolve) => setTimeout(resolve, pollInterval))

      const result = await this.sc.execute(
        `if(~learnResult.notNil) { "LEARN:" ++ ~learnResult[0] ++ ":" ++ ~learnResult[1] ++ ":" ++ ~learnResult[2]; } { "LEARN:WAITING"; };`
      )

      if (result.startsWith("LEARN:") && !result.includes("WAITING")) {
        const parts = result.replace("LEARN:", "").split(":")
        return {
          cc: parseInt(parts[0], 10),
          channel: parseInt(parts[1], 10),
          value: parseInt(parts[2], 10),
        }
      }
    }

    // Timeout - clean up
    await this.sc.execute("MIDIdef(\\learn).free;")
    return null
  }

  async send(
    type: "note_on" | "note_off" | "cc" | "program",
    params: {
      channel?: number
      note?: number
      velocity?: number
      cc?: number
      value?: number
    }
  ): Promise<string> {
    const channel = params.channel ?? 0

    switch (type) {
      case "note_on":
        await this.sc.execute(
          `~midiOut.noteOn(${channel}, ${params.note ?? 60}, ${params.velocity ?? 64});`
        )
        return `Sent note on: ${params.note ?? 60}, velocity ${params.velocity ?? 64}`

      case "note_off":
        await this.sc.execute(
          `~midiOut.noteOff(${channel}, ${params.note ?? 60}, 0);`
        )
        return `Sent note off: ${params.note ?? 60}`

      case "cc":
        await this.sc.execute(
          `~midiOut.control(${channel}, ${params.cc ?? 0}, ${params.value ?? 64});`
        )
        return `Sent CC ${params.cc ?? 0}: ${params.value ?? 64}`

      case "program":
        await this.sc.execute(
          `~midiOut.program(${channel}, ${params.value ?? 0});`
        )
        return `Sent program change: ${params.value ?? 0}`

      default:
        throw new Error(`Unknown MIDI message type: ${type}`)
    }
  }

  async enableLogging(): Promise<void> {
    if (this.loggingEnabled) return
    await this.init()

    await this.sc.execute(
      `~midiLog = List[]; ~midiLogMax = 200; MIDIdef.noteOn(\\logNoteOn, { |vel, note, chan| ~midiLog.add((type: \\noteOn, note: note, vel: vel, chan: chan, time: Main.elapsedTime)); if(~midiLog.size > ~midiLogMax) { ~midiLog.removeAt(0) }; }); MIDIdef.noteOff(\\logNoteOff, { |vel, note, chan| ~midiLog.add((type: \\noteOff, note: note, vel: vel, chan: chan, time: Main.elapsedTime)); if(~midiLog.size > ~midiLogMax) { ~midiLog.removeAt(0) }; }); MIDIdef.cc(\\logCC, { |val, cc, chan| ~midiLog.add((type: \\cc, cc: cc, value: val, chan: chan, time: Main.elapsedTime)); if(~midiLog.size > ~midiLogMax) { ~midiLog.removeAt(0) }; });`
    )

    this.loggingEnabled = true
  }

  async getRecent(
    options: {
      count?: number
      type?: "notes" | "cc" | "all"
      since?: number
    } = {}
  ): Promise<MIDIRecentEvents> {
    await this.enableLogging()

    const count = options.count ?? 20
    const type = options.type ?? "all"
    const since = options.since

    let filterCode = ""
    if (type === "notes") {
      filterCode = `.select { |e| (e.type == \\noteOn) or: (e.type == \\noteOff) }`
    } else if (type === "cc") {
      filterCode = `.select { |e| e.type == \\cc }`
    }

    if (since !== undefined) {
      filterCode += `.select { |e| (Main.elapsedTime - e.time) < ${since} }`
    }

    const code = `~midiLog${filterCode}.keep(-${count}).collect { |e| e.type ++ ":" ++ (e.note ? e.cc ? 0) ++ ":" ++ (e.vel ? e.value ? 0) ++ ":" ++ e.chan ++ ":" ++ e.time.round(0.001) }.join("\\n")`

    const output = await this.sc.execute(code)
    return this.parseEvents(output)
  }

  private parseEvents(output: string): MIDIRecentEvents {
    const events: MIDIEvent[] = []
    const lines = output.split("\n")

    for (const line of lines) {
      if (line.trim() === "" || !line.includes(":")) {
        continue
      }

      const parts = line.split(":")
      if (parts.length >= 5) {
        const eventType = parts[0] as "noteOn" | "noteOff" | "cc"
        const event: MIDIEvent = {
          type: eventType,
          channel: parseInt(parts[3], 10),
          time: parseFloat(parts[4]),
        }

        if (eventType === "noteOn" || eventType === "noteOff") {
          event.note = parseInt(parts[1], 10)
          event.velocity = parseInt(parts[2], 10)
        } else if (eventType === "cc") {
          event.cc = parseInt(parts[1], 10)
          event.value = parseInt(parts[2], 10)
        }

        events.push(event)
      }
    }

    return { events }
  }

  async clearMappings(): Promise<string> {
    await this.sc.execute(`MIDIdef.freeAll;`)

    // Re-enable logging if it was on
    if (this.loggingEnabled) {
      this.loggingEnabled = false
      await this.enableLogging()
    }

    this.noteMappings = []
    this.ccMappings = []
    this.midiDefCounter = 0

    return "Cleared all MIDI mappings"
  }

  isInitialized(): boolean {
    return this.initialized
  }

  getNoteMappings(): MIDINoteMapping[] {
    return [...this.noteMappings]
  }

  getCCMappings(): MIDICCMapping[] {
    return [...this.ccMappings]
  }

  async playPattern(
    name: string,
    notes: number[],
    durations: number[],
    velocities: number[],
    channel: number = 0
  ): Promise<string> {
    const notesStr = notes.join(", ")
    const dursStr = durations.join(", ")
    const velsStr = velocities.join(", ")

    await this.sc.execute(
      `Pdef(\\${name}, Pbind(\\type, \\midi, \\midiout, ~midiOut, \\chan, ${channel}, \\midinote, Pseq([${notesStr}], inf), \\dur, Pseq([${dursStr}], inf), \\amp, Pseq([${velsStr}], inf) / 127)).play;`
    )

    return `Playing pattern \\${name} on MIDI channel ${channel}. Stop with: Pdef(\\${name}).stop;`
  }
}
