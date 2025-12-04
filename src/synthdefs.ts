// SynthDef metadata for validation and MCP tool schemas
// Actual SynthDef code lives in ClaudeCollider Quark (CCSynths.sc)

export interface SynthDefMeta {
  name: string
  description: string
  params: string[]
}

export const synthdefs: Record<string, SynthDefMeta> = {
  kick: {
    name: "kick",
    description: "Punchy kick drum with sub bass",
    params: ["out", "freq", "amp", "decay"],
  },

  snare: {
    name: "snare",
    description: "Snare drum with noise burst",
    params: ["out", "freq", "amp", "decay"],
  },

  hihat: {
    name: "hihat",
    description: "Closed hi-hat",
    params: ["out", "amp", "decay"],
  },

  clap: {
    name: "clap",
    description: "Hand clap with layered noise bursts",
    params: ["out", "amp", "decay"],
  },

  bass: {
    name: "bass",
    description: "Simple sub bass with slight harmonics",
    params: ["out", "freq", "amp", "decay", "gate"],
  },

  acid: {
    name: "acid",
    description: "Resonant filter bass (303-style)",
    params: ["out", "freq", "amp", "cutoff", "res", "decay", "gate"],
  },

  lead: {
    name: "lead",
    description: "Detuned saw lead with filter",
    params: ["out", "freq", "amp", "pan", "gate", "att", "rel", "cutoff", "res"],
  },

  pad: {
    name: "pad",
    description: "Soft ambient pad with detuned oscillators",
    params: ["out", "freq", "amp", "attack", "release", "gate"],
  },
}

export function getSynthDefNames(): string[] {
  return Object.keys(synthdefs)
}

export function getSynthDef(name: string): SynthDefMeta | undefined {
  return synthdefs[name]
}
