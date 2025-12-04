// Effect metadata for validation and MCP tool schemas
// Actual effect code lives in ClaudeCollider Quark (CCFX.sc)

export interface EffectParam {
  min: number
  max: number
  default: number
  curve: "lin" | "exp"
}

export interface EffectMeta {
  name: string
  description: string
  params: Record<string, EffectParam>
}

export const effectsLibrary: Record<string, EffectMeta> = {
  // ============== Filter Effects ==============
  lpf: {
    name: "lpf",
    description: "Low Pass Filter with resonance",
    params: {
      cutoff: { min: 20, max: 20000, default: 1000, curve: "exp" },
      resonance: { min: 0.1, max: 1.0, default: 0.5, curve: "lin" },
    },
  },

  hpf: {
    name: "hpf",
    description: "High Pass Filter with resonance",
    params: {
      cutoff: { min: 20, max: 20000, default: 200, curve: "exp" },
      resonance: { min: 0.1, max: 1.0, default: 0.5, curve: "lin" },
    },
  },

  bpf: {
    name: "bpf",
    description: "Band Pass Filter",
    params: {
      freq: { min: 20, max: 20000, default: 1000, curve: "exp" },
      bw: { min: 0.1, max: 2.0, default: 0.5, curve: "lin" },
    },
  },

  // ============== Time-Based Effects ==============
  reverb: {
    name: "reverb",
    description: "Stereo reverb using FreeVerb2",
    params: {
      mix: { min: 0, max: 1, default: 0.33, curve: "lin" },
      room: { min: 0, max: 1, default: 0.8, curve: "lin" },
      damp: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  delay: {
    name: "delay",
    description: "Stereo delay with feedback",
    params: {
      time: { min: 0.01, max: 2.0, default: 0.375, curve: "lin" },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: "lin" },
      mix: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  pingpong: {
    name: "pingpong",
    description: "Ping pong stereo delay",
    params: {
      time: { min: 0.01, max: 2.0, default: 0.375, curve: "lin" },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: "lin" },
      mix: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  // ============== Modulation Effects ==============
  chorus: {
    name: "chorus",
    description: "Stereo chorus effect",
    params: {
      rate: { min: 0.1, max: 10, default: 0.5, curve: "exp" },
      depth: { min: 0.001, max: 0.02, default: 0.005, curve: "exp" },
      mix: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  flanger: {
    name: "flanger",
    description: "Flanger with feedback",
    params: {
      rate: { min: 0.05, max: 5, default: 0.2, curve: "exp" },
      depth: { min: 0.0001, max: 0.01, default: 0.003, curve: "exp" },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: "lin" },
      mix: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  phaser: {
    name: "phaser",
    description: "Phaser effect",
    params: {
      rate: { min: 0.05, max: 5, default: 0.3, curve: "exp" },
      depth: { min: 0.5, max: 4, default: 2, curve: "lin" },
      mix: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  tremolo: {
    name: "tremolo",
    description: "Amplitude modulation tremolo",
    params: {
      rate: { min: 0.5, max: 20, default: 4, curve: "exp" },
      depth: { min: 0, max: 1, default: 0.5, curve: "lin" },
    },
  },

  // ============== Distortion Effects ==============
  distortion: {
    name: "distortion",
    description: "Soft clip distortion with tone control",
    params: {
      drive: { min: 1, max: 20, default: 2, curve: "exp" },
      tone: { min: 0, max: 1, default: 0.5, curve: "lin" },
      mix: { min: 0, max: 1, default: 1, curve: "lin" },
    },
  },

  bitcrush: {
    name: "bitcrush",
    description: "Bit crusher with sample rate reduction",
    params: {
      bits: { min: 1, max: 16, default: 8, curve: "lin" },
      rate: { min: 1000, max: 44100, default: 44100, curve: "exp" },
      mix: { min: 0, max: 1, default: 1, curve: "lin" },
    },
  },

  wavefold: {
    name: "wavefold",
    description: "Wave folder distortion",
    params: {
      amount: { min: 1, max: 10, default: 2, curve: "exp" },
      mix: { min: 0, max: 1, default: 1, curve: "lin" },
    },
  },

  // ============== Dynamics Effects ==============
  compressor: {
    name: "compressor",
    description: "Dynamics compressor",
    params: {
      threshold: { min: 0.01, max: 1, default: 0.5, curve: "exp" },
      ratio: { min: 1, max: 20, default: 4, curve: "exp" },
      attack: { min: 0.001, max: 0.5, default: 0.01, curve: "exp" },
      release: { min: 0.01, max: 2, default: 0.1, curve: "exp" },
      makeup: { min: 0.5, max: 4, default: 1, curve: "exp" },
    },
  },

  limiter: {
    name: "limiter",
    description: "Brickwall limiter",
    params: {
      level: { min: 0.1, max: 1, default: 0.95, curve: "lin" },
      lookahead: { min: 0.001, max: 0.1, default: 0.01, curve: "exp" },
    },
  },

  gate: {
    name: "gate",
    description: "Noise gate",
    params: {
      threshold: { min: 0.001, max: 0.5, default: 0.1, curve: "exp" },
      attack: { min: 0.001, max: 0.5, default: 0.01, curve: "exp" },
      release: { min: 0.01, max: 2, default: 0.1, curve: "exp" },
    },
  },

  // ============== Stereo Effects ==============
  widener: {
    name: "widener",
    description: "Stereo width control",
    params: {
      width: { min: 0, max: 3, default: 1.5, curve: "lin" },
    },
  },

  autopan: {
    name: "autopan",
    description: "Automatic stereo panning",
    params: {
      rate: { min: 0.1, max: 10, default: 1, curve: "exp" },
      depth: { min: 0, max: 1, default: 1, curve: "lin" },
    },
  },
}

export const effectNames = Object.keys(effectsLibrary)

export function getEffectMeta(name: string): EffectMeta | undefined {
  return effectsLibrary[name]
}

export function validateEffectParams(
  effectName: string,
  params: Record<string, number>
): { valid: Record<string, number>; warnings: string[] } {
  const meta = effectsLibrary[effectName]
  if (!meta) {
    throw new Error(`Unknown effect: ${effectName}`)
  }

  const valid: Record<string, number> = {}
  const warnings: string[] = []

  for (const [paramName, value] of Object.entries(params)) {
    const paramMeta = meta.params[paramName]
    if (!paramMeta) {
      throw new Error(
        `Invalid parameter: ${paramName}. Valid params for ${effectName}: ${Object.keys(meta.params).join(", ")}`
      )
    }

    let clampedValue = value
    if (value < paramMeta.min) {
      clampedValue = paramMeta.min
      warnings.push(`${paramName} clamped to min ${paramMeta.min}`)
    } else if (value > paramMeta.max) {
      clampedValue = paramMeta.max
      warnings.push(`${paramName} clamped to max ${paramMeta.max}`)
    }
    valid[paramName] = clampedValue
  }

  return { valid, warnings }
}
