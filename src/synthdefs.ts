export interface SynthDef {
  name: string
  description: string
  code: string
  params: string[]
}

export const synthdefs: Record<string, SynthDef> = {
  kick: {
    name: "kick",
    description: "Punchy kick drum with sub bass",
    params: ["freq", "amp", "decay"],
    code: `
      SynthDef(\\kick, { |out=0, freq=60, amp=0.5, decay=0.3|
        var sig, env;
        env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
        sig = SinOsc.ar(freq * EnvGen.kr(Env([2, 1], [0.02])));
        sig = sig + (SinOsc.ar(freq) * 0.5);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  snare: {
    name: "snare",
    description: "Snare drum with noise burst",
    params: ["freq", "amp", "decay"],
    code: `
      SynthDef(\\snare, { |out=0, freq=180, amp=0.5, decay=0.2|
        var sig, env, noise;
        env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
        sig = SinOsc.ar(freq * EnvGen.kr(Env([1.5, 1], [0.02])));
        noise = HPF.ar(WhiteNoise.ar, 1000) * EnvGen.kr(Env.perc(0.001, decay * 0.5));
        sig = (sig * 0.3) + (noise * 0.7);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  hihat: {
    name: "hihat",
    description: "Closed hi-hat",
    params: ["amp", "decay"],
    code: `
      SynthDef(\\hihat, { |out=0, amp=0.3, decay=0.05|
        var sig, env;
        env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
        sig = HPF.ar(WhiteNoise.ar, 8000);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  clap: {
    name: "clap",
    description: "Hand clap with layered noise bursts",
    params: ["amp", "decay"],
    code: `
      SynthDef(\\clap, { |out=0, amp=0.5, decay=0.15|
        var sig, env;
        env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
        sig = BPF.ar(WhiteNoise.ar, 1500, 0.5);
        sig = sig * Env.perc(0.001, 0.01).ar * 3;
        sig = sig + (BPF.ar(WhiteNoise.ar, 1200, 0.3) * env);
        sig = sig * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  bass: {
    name: "bass",
    description: "Simple sub bass with slight harmonics",
    params: ["freq", "amp", "decay"],
    code: `
      SynthDef(\\bass, { |out=0, freq=55, amp=0.5, decay=0.5, gate=1|
        var sig, env;
        env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.7, 0.1), gate, doneAction: 2);
        sig = SinOsc.ar(freq) + (SinOsc.ar(freq * 2) * 0.3) + (SinOsc.ar(freq * 3) * 0.1);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  acid: {
    name: "acid",
    description: "Resonant filter bass (303-style)",
    params: ["freq", "amp", "cutoff", "res", "decay"],
    code: `
      SynthDef(\\acid, { |out=0, freq=55, amp=0.5, cutoff=1000, res=0.3, decay=0.3, gate=1|
        var sig, env, fenv;
        env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.6, 0.1), gate, doneAction: 2);
        fenv = EnvGen.kr(Env.perc(0.01, decay)) * cutoff * 2;
        sig = Saw.ar(freq);
        sig = RLPF.ar(sig, (cutoff + fenv).clip(20, 18000), res);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },

  pad: {
    name: "pad",
    description: "Soft ambient pad with detuned oscillators",
    params: ["freq", "amp", "attack", "release"],
    code: `
      SynthDef(\\pad, { |out=0, freq=220, amp=0.3, attack=0.5, release=1, gate=1|
        var sig, env;
        env = EnvGen.kr(Env.adsr(attack, 0.2, 0.7, release), gate, doneAction: 2);
        sig = SinOsc.ar(freq * [0.99, 1, 1.01]).sum / 3;
        sig = sig + (Saw.ar(freq * [0.5, 0.501]) * 0.2).sum;
        sig = LPF.ar(sig, 2000);
        sig = sig * env * amp;
        Out.ar(out, sig ! 2);
      }).add;
    `,
  },
}

export function getSynthDefNames(): string[] {
  return Object.keys(synthdefs)
}

export function getSynthDef(name: string): SynthDef | undefined {
  return synthdefs[name]
}

export function getAllSynthDefsCode(): string {
  return Object.values(synthdefs)
    .map((s) => s.code)
    .join("\n\n")
}
