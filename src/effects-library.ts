/**
 * Pre-built effects library for the Vibe Music Server
 * Each effect is an Ndef-based audio processor with typed parameters
 */

export interface EffectParam {
  min: number;
  max: number;
  default: number;
  curve: 'lin' | 'exp';
}

export interface EffectDefinition {
  name: string;
  description: string;
  params: Record<string, EffectParam>;
  // Function that generates SC code given an input bus variable name
  generateCode: (inputBus: string) => string;
}

export const effectsLibrary: Record<string, EffectDefinition> = {
  // ============== Filter Effects ==============
  lpf: {
    name: 'lpf',
    description: 'Low Pass Filter with resonance',
    params: {
      cutoff: { min: 20, max: 20000, default: 1000, curve: 'exp' },
      resonance: { min: 0.1, max: 1.0, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |cutoff=1000, resonance=0.5| RLPF.ar(In.ar(${inputBus}, 2), cutoff, resonance) }`,
  },

  hpf: {
    name: 'hpf',
    description: 'High Pass Filter with resonance',
    params: {
      cutoff: { min: 20, max: 20000, default: 200, curve: 'exp' },
      resonance: { min: 0.1, max: 1.0, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |cutoff=200, resonance=0.5| RHPF.ar(In.ar(${inputBus}, 2), cutoff, resonance) }`,
  },

  bpf: {
    name: 'bpf',
    description: 'Band Pass Filter',
    params: {
      freq: { min: 20, max: 20000, default: 1000, curve: 'exp' },
      bw: { min: 0.1, max: 2.0, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |freq=1000, bw=0.5| BPF.ar(In.ar(${inputBus}, 2), freq, bw) }`,
  },

  // ============== Time-Based Effects ==============
  reverb: {
    name: 'reverb',
    description: 'Stereo reverb using FreeVerb2',
    params: {
      mix: { min: 0, max: 1, default: 0.33, curve: 'lin' },
      room: { min: 0, max: 1, default: 0.8, curve: 'lin' },
      damp: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |mix=0.33, room=0.8, damp=0.5| var sig = In.ar(${inputBus}, 2); FreeVerb2.ar(sig[0], sig[1], mix, room, damp) }`,
  },

  delay: {
    name: 'delay',
    description: 'Stereo delay with feedback',
    params: {
      time: { min: 0.01, max: 2.0, default: 0.375, curve: 'lin' },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: 'lin' },
      mix: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |time=0.375, feedback=0.5, mix=0.5| var sig = In.ar(${inputBus}, 2); var delayed = CombL.ar(sig, 2, time, feedback * 4); (sig * (1 - mix)) + (delayed * mix) }`,
  },

  pingpong: {
    name: 'pingpong',
    description: 'Ping pong stereo delay',
    params: {
      time: { min: 0.01, max: 2.0, default: 0.375, curve: 'lin' },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: 'lin' },
      mix: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |time=0.375, feedback=0.5, mix=0.5| var sig = In.ar(${inputBus}, 2); var left = CombL.ar(sig[0], 2, time, feedback * 4); var right = CombL.ar(sig[1] + (left * 0.5), 2, time, feedback * 4); var delayed = [left + (right * 0.5), right]; (sig * (1 - mix)) + (delayed * mix) }`,
  },

  // ============== Modulation Effects ==============
  chorus: {
    name: 'chorus',
    description: 'Stereo chorus effect',
    params: {
      rate: { min: 0.1, max: 10, default: 0.5, curve: 'exp' },
      depth: { min: 0.001, max: 0.02, default: 0.005, curve: 'exp' },
      mix: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |rate=0.5, depth=0.005, mix=0.5| var sig = In.ar(${inputBus}, 2); var mod = SinOsc.kr([rate, rate * 1.01], [0, 0.5]).range(0.001, depth); var wet = DelayL.ar(sig, 0.1, mod); (sig * (1 - mix)) + (wet * mix) }`,
  },

  flanger: {
    name: 'flanger',
    description: 'Flanger with feedback',
    params: {
      rate: { min: 0.05, max: 5, default: 0.2, curve: 'exp' },
      depth: { min: 0.0001, max: 0.01, default: 0.003, curve: 'exp' },
      feedback: { min: 0, max: 0.95, default: 0.5, curve: 'lin' },
      mix: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |rate=0.2, depth=0.003, feedback=0.5, mix=0.5| var sig = In.ar(${inputBus}, 2); var mod = SinOsc.kr(rate).range(0.0001, depth); var wet = DelayL.ar(sig + (LocalIn.ar(2) * feedback), 0.02, mod); LocalOut.ar(wet); (sig * (1 - mix)) + (wet * mix) }`,
  },

  phaser: {
    name: 'phaser',
    description: 'Phaser effect',
    params: {
      rate: { min: 0.05, max: 5, default: 0.3, curve: 'exp' },
      depth: { min: 0.5, max: 4, default: 2, curve: 'lin' },
      mix: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |rate=0.3, depth=2, mix=0.5| var sig = In.ar(${inputBus}, 2); var mod = SinOsc.kr(rate).range(100, 4000); var wet = AllpassL.ar(sig, 0.1, mod.reciprocal, 0); (sig * (1 - mix)) + (wet * mix) }`,
  },

  tremolo: {
    name: 'tremolo',
    description: 'Amplitude modulation tremolo',
    params: {
      rate: { min: 0.5, max: 20, default: 4, curve: 'exp' },
      depth: { min: 0, max: 1, default: 0.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |rate=4, depth=0.5| var sig = In.ar(${inputBus}, 2); var mod = SinOsc.kr(rate).range(1 - depth, 1); sig * mod }`,
  },

  // ============== Distortion Effects ==============
  distortion: {
    name: 'distortion',
    description: 'Soft clip distortion with tone control',
    params: {
      drive: { min: 1, max: 20, default: 2, curve: 'exp' },
      tone: { min: 0, max: 1, default: 0.5, curve: 'lin' },
      mix: { min: 0, max: 1, default: 1, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |drive=2, tone=0.5, mix=1| var sig = In.ar(${inputBus}, 2); var wet = (sig * drive).tanh; wet = LPF.ar(wet, tone.linexp(0, 1, 1000, 12000)); (sig * (1 - mix)) + (wet * mix) }`,
  },

  bitcrush: {
    name: 'bitcrush',
    description: 'Bit crusher with sample rate reduction',
    params: {
      bits: { min: 1, max: 16, default: 8, curve: 'lin' },
      rate: { min: 1000, max: 44100, default: 44100, curve: 'exp' },
      mix: { min: 0, max: 1, default: 1, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |bits=8, rate=44100, mix=1| var sig = In.ar(${inputBus}, 2); var crushed = sig.round(2.pow(1 - bits)); var downsampled = Latch.ar(crushed, Impulse.ar(rate)); (sig * (1 - mix)) + (downsampled * mix) }`,
  },

  wavefold: {
    name: 'wavefold',
    description: 'Wave folder distortion',
    params: {
      amount: { min: 1, max: 10, default: 2, curve: 'exp' },
      mix: { min: 0, max: 1, default: 1, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |amount=2, mix=1| var sig = In.ar(${inputBus}, 2); var wet = (sig * amount).fold(-1, 1); (sig * (1 - mix)) + (wet * mix) }`,
  },

  // ============== Dynamics Effects ==============
  compressor: {
    name: 'compressor',
    description: 'Dynamics compressor',
    params: {
      threshold: { min: 0.01, max: 1, default: 0.5, curve: 'exp' },
      ratio: { min: 1, max: 20, default: 4, curve: 'exp' },
      attack: { min: 0.001, max: 0.5, default: 0.01, curve: 'exp' },
      release: { min: 0.01, max: 2, default: 0.1, curve: 'exp' },
      makeup: { min: 0.5, max: 4, default: 1, curve: 'exp' },
    },
    generateCode: (inputBus) =>
      `{ |threshold=0.5, ratio=4, attack=0.01, release=0.1, makeup=1| var sig = In.ar(${inputBus}, 2); Compander.ar(sig, sig, threshold, 1, ratio.reciprocal, attack, release, makeup) }`,
  },

  limiter: {
    name: 'limiter',
    description: 'Brickwall limiter',
    params: {
      level: { min: 0.1, max: 1, default: 0.95, curve: 'lin' },
      lookahead: { min: 0.001, max: 0.1, default: 0.01, curve: 'exp' },
    },
    generateCode: (inputBus) =>
      `{ |level=0.95, lookahead=0.01| Limiter.ar(In.ar(${inputBus}, 2), level, lookahead) }`,
  },

  gate: {
    name: 'gate',
    description: 'Noise gate',
    params: {
      threshold: { min: 0.001, max: 0.5, default: 0.1, curve: 'exp' },
      attack: { min: 0.001, max: 0.5, default: 0.01, curve: 'exp' },
      release: { min: 0.01, max: 2, default: 0.1, curve: 'exp' },
    },
    generateCode: (inputBus) =>
      `{ |threshold=0.1, attack=0.01, release=0.1| var sig = In.ar(${inputBus}, 2); var env = Amplitude.kr(sig).max > threshold; sig * Lag.kr(env, attack, release) }`,
  },

  // ============== Stereo Effects ==============
  widener: {
    name: 'widener',
    description: 'Stereo width control',
    params: {
      width: { min: 0, max: 3, default: 1.5, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |width=1.5| var sig = In.ar(${inputBus}, 2); var mid = (sig[0] + sig[1]) * 0.5; var side = (sig[0] - sig[1]) * 0.5 * width; [mid + side, mid - side] }`,
  },

  autopan: {
    name: 'autopan',
    description: 'Automatic stereo panning',
    params: {
      rate: { min: 0.1, max: 10, default: 1, curve: 'exp' },
      depth: { min: 0, max: 1, default: 1, curve: 'lin' },
    },
    generateCode: (inputBus) =>
      `{ |rate=1, depth=1| var sig = In.ar(${inputBus}, 2); var pan = SinOsc.kr(rate).range(-1 * depth, depth); Balance2.ar(sig[0], sig[1], pan) }`,
  },
};

export const effectNames = Object.keys(effectsLibrary) as Array<keyof typeof effectsLibrary>;
