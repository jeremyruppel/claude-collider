// CCSynths - SynthDef management for ClaudeCollider

CCSynths {
  var <cc;
  var <loaded;
  var <defs;

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    loaded = Set[];
    defs = this.defineSynths;
  }

  defineSynths {
    ^(
      // ========== DRUMS ==========
      kick: (
        description: "Punchy kick drum with sub bass",
        def: {
          SynthDef(\cc_kick, { |out=0, freq=60, amp=0.5, decay=0.3|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            sig = SinOsc.ar(freq * EnvGen.kr(Env([2, 1], [0.02])));
            sig = sig + (SinOsc.ar(freq) * 0.5);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      snare: (
        description: "Snare drum with noise burst",
        def: {
          SynthDef(\cc_snare, { |out=0, freq=180, amp=0.5, decay=0.2|
            var sig, env, noise;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            sig = SinOsc.ar(freq * EnvGen.kr(Env([1.5, 1], [0.02])));
            noise = HPF.ar(WhiteNoise.ar, 1000) * EnvGen.kr(Env.perc(0.001, decay * 0.5));
            sig = (sig * 0.3) + (noise * 0.7);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      hihat: (
        description: "Closed hi-hat",
        def: {
          SynthDef(\cc_hihat, { |out=0, amp=0.3, decay=0.05|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            sig = HPF.ar(WhiteNoise.ar, 8000);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      clap: (
        description: "Hand clap with layered noise bursts",
        def: {
          SynthDef(\cc_clap, { |out=0, amp=0.5, decay=0.15|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            sig = BPF.ar(WhiteNoise.ar, 1500, 0.5);
            sig = sig * Env.perc(0.001, 0.01).ar * 3;
            sig = sig + (BPF.ar(WhiteNoise.ar, 1200, 0.3) * env);
            sig = sig * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      // ========== BASS ==========
      bass: (
        description: "Simple sub bass with slight harmonics",
        def: {
          SynthDef(\cc_bass, { |out=0, freq=55, amp=0.5, decay=0.5, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.7, 0.1), gate, doneAction: 2);
            sig = SinOsc.ar(freq) + (SinOsc.ar(freq * 2) * 0.3) + (SinOsc.ar(freq * 3) * 0.1);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      acid: (
        description: "Resonant filter bass (303-style)",
        def: {
          SynthDef(\cc_acid, { |out=0, freq=55, amp=0.5, cutoff=1000, res=0.3, decay=0.3, gate=1|
            var sig, env, fenv;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.6, 0.1), gate, doneAction: 2);
            fenv = EnvGen.kr(Env.perc(0.01, decay)) * cutoff * 2;
            sig = Saw.ar(freq);
            sig = RLPF.ar(sig, (cutoff + fenv).clip(20, 18000), res);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      // ========== LEADS ==========
      lead: (
        description: "Detuned saw lead with filter",
        def: {
          SynthDef(\cc_lead, { |out=0, freq=440, amp=0.5, pan=0, gate=1,
                              att=0.01, rel=0.3, cutoff=4000, res=0.2|
            var sig, env;
            env = EnvGen.kr(Env.adsr(att, 0.1, 0.8, rel), gate, doneAction: 2);
            sig = Saw.ar(freq) + Saw.ar(freq * 1.005);
            sig = RLPF.ar(sig, cutoff, res);
            Out.ar(out, Pan2.ar(sig * env * amp, pan));
          })
        }
      ),

      // ========== PADS ==========
      pad: (
        description: "Soft ambient pad with detuned oscillators",
        def: {
          SynthDef(\cc_pad, { |out=0, freq=220, amp=0.3, attack=0.5, release=1, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(attack, 0.2, 0.7, release), gate, doneAction: 2);
            sig = SinOsc.ar(freq * [0.99, 1, 1.01]).sum / 3;
            sig = sig + (Saw.ar(freq * [0.5, 0.501]) * 0.2).sum;
            sig = LPF.ar(sig, 2000);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      )
    );
  }

  loadAll {
    defs.keysValuesDo { |name, entry|
      entry[\def].value.add;
      loaded.add(name);
    };
    "CCSynths: loaded %".format(loaded.size).postln;
  }

  load { |name|
    var entry = defs[name];
    if(entry.notNil) {
      entry[\def].value.add;
      loaded.add(name);
      ^true;
    } {
      "CCSynths: unknown synth '%'".format(name).warn;
      ^false;
    };
  }

  play { |name ...args|
    var synthName = ("cc_" ++ name).asSymbol;
    if(loaded.includes(name).not) { this.load(name) };
    ^Synth(synthName, args);
  }

  list {
    ^defs.keys.asArray.sort;
  }

  info { |name|
    ^defs[name].notNil;
  }

  describe {
    var lines = defs.keys.asArray.sort.collect { |name|
      var entry = defs[name];
      var synthDef = entry[\def].value;
      var params = synthDef.allControlNames.collect { |c| c.name.asString }.join(",");
      "%|%|%".format(name, entry[\description], params);
    };
    ^lines.join("\n");
  }
}
