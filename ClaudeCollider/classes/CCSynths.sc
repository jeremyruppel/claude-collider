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
          SynthDef(\cc_kick, { |out=0, freq=48, amp=0.5, decay=0.3|
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

      openhat: (
        description: "Open hi-hat with longer decay",
        def: {
          SynthDef(\cc_openhat, { |out=0, amp=0.3, decay=0.5|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            // Filtered noise with metallic tone
            sig = HPF.ar(WhiteNoise.ar, 6000);
            sig = BPF.ar(sig, 10000, 0.3) + (sig * 0.3);
            // Slight pitch envelope for attack character
            sig = sig * EnvGen.kr(Env([1.2, 1], [0.01]));
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      tom: (
        description: "Tunable tom drum for fills",
        def: {
          SynthDef(\cc_tom, { |out=0, freq=120, amp=0.5, decay=0.3|
            var sig, env, penv, noise;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            // Pitch starts high and drops to target freq
            penv = EnvGen.kr(Env([freq * 2, freq], [0.04], \exp));
            sig = SinOsc.ar(penv);
            // Add noise for attack transient
            noise = LPF.ar(WhiteNoise.ar, 1000) * EnvGen.kr(Env.perc(0.001, 0.02));
            sig = sig + (noise * 0.3);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      rim: (
        description: "Rimshot / sidestick",
        def: {
          SynthDef(\cc_rim, { |out=0, amp=0.4, freq=1700|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, 0.03), doneAction: 2);
            // Short noise burst through resonant bandpass
            sig = WhiteNoise.ar;
            sig = BPF.ar(sig, freq, 0.02);
            // Add click for attack
            sig = sig + (SinOsc.ar(1200) * EnvGen.kr(Env.perc(0.0001, 0.002)) * 0.5);
            sig = sig * env * amp * 2;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      shaker: (
        description: "Shaker / maraca for rhythmic texture",
        def: {
          SynthDef(\cc_shaker, { |out=0, amp=0.2, decay=0.08, color=5000|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            // Filtered noise grain
            sig = HPF.ar(WhiteNoise.ar, color);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      cowbell: (
        description: "808-style cowbell",
        def: {
          SynthDef(\cc_cowbell, { |out=0, amp=0.4, decay=0.4|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, decay, curve: -4), doneAction: 2);
            // Two detuned square waves for metallic character
            sig = Pulse.ar(560, 0.5) + Pulse.ar(845, 0.5);
            // Bandpass filter for classic tone
            sig = BPF.ar(sig, 700, 0.4);
            sig = sig * env * amp;
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

      sub: (
        description: "Pure sub bass for layering",
        def: {
          SynthDef(\cc_sub, { |out=0, freq=55, amp=0.5, decay=1, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.8, decay), gate, doneAction: 2);
            // Pure sine with smooth envelope
            sig = SinOsc.ar(freq);
            // Slight soft saturation for warmth
            sig = (sig * 1.1).tanh;
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      reese: (
        description: "Detuned saw bass (DnB/dubstep style)",
        def: {
          SynthDef(\cc_reese, { |out=0, freq=55, amp=0.4, detune=0.5, cutoff=800, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.8, 0.2), gate, doneAction: 2);
            // 3 detuned sawtooth waves for phasing movement
            sig = Saw.ar(freq - detune) + Saw.ar(freq) + Saw.ar(freq + detune);
            sig = sig / 3;
            // Low-pass filter with slight resonance
            sig = RLPF.ar(sig, cutoff, 0.3);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      fmbass: (
        description: "FM bass for growly/aggressive tones",
        def: {
          SynthDef(\cc_fmbass, { |out=0, freq=55, amp=0.4, index=3, ratio=1, gate=1|
            var sig, mod, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.15, 0.7, 0.2), gate, doneAction: 2);
            // Simple 2-op FM: modulator -> carrier
            mod = SinOsc.ar(freq * ratio) * index * freq;
            sig = SinOsc.ar(freq + mod);
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

      // ========== MELODIC ==========
      pluck: (
        description: "Karplus-Strong plucked string",
        def: {
          SynthDef(\cc_pluck, { |out=0, freq=440, amp=0.4, decay=2, color=0.5|
            var sig, env;
            env = EnvGen.kr(Env.linen(0.001, decay, 0.01), doneAction: 2);
            // Pluck UGen for Karplus-Strong synthesis
            sig = Pluck.ar(
              WhiteNoise.ar,
              Impulse.kr(0),
              freq.reciprocal,
              freq.reciprocal,
              decay,
              color
            );
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      bell: (
        description: "FM bell / glassy tone",
        def: {
          SynthDef(\cc_bell, { |out=0, freq=440, amp=0.3, decay=4, brightness=3|
            var sig, mod1, mod2, env;
            env = EnvGen.kr(Env.perc(0.001, decay), doneAction: 2);
            // FM with inharmonic ratios for bell character
            mod1 = SinOsc.ar(freq * 2.4) * brightness * freq;
            mod2 = SinOsc.ar(freq * 3.1) * brightness * freq * 0.5;
            sig = SinOsc.ar(freq + mod1 + mod2);
            // Add shimmer with higher partial
            sig = sig + (SinOsc.ar(freq * 5.2) * 0.1 * env);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      keys: (
        description: "Electric piano / Rhodes-ish",
        def: {
          SynthDef(\cc_keys, { |out=0, freq=440, amp=0.4, attack=0.01, release=1, brightness=0.5, gate=1|
            var sig, mod, env, bark;
            env = EnvGen.kr(Env.adsr(attack, 0.2, 0.6, release), gate, doneAction: 2);
            // FM with brightness-controlled index
            mod = SinOsc.ar(freq * 2) * freq * (brightness * 3 + 0.5);
            sig = SinOsc.ar(freq + mod);
            // Add slight asymmetry for warmth
            sig = sig + (SinOsc.ar(freq * 3) * 0.05);
            // EP bark on attack (modulator envelope)
            bark = EnvGen.kr(Env.perc(0.001, 0.08)) * brightness * 2;
            sig = sig + (SinOsc.ar(freq + (SinOsc.ar(freq * 2) * freq * bark)) * 0.3 * bark);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      strings: (
        description: "Simple string ensemble pad",
        def: {
          SynthDef(\cc_strings, { |out=0, freq=440, amp=0.3, attack=0.5, release=1, detune=0.3, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(attack, 0.2, 0.8, release), gate, doneAction: 2);
            // Multiple detuned saws for ensemble effect
            sig = Saw.ar(freq * (1 - (detune * 0.01)));
            sig = sig + Saw.ar(freq * (1 - (detune * 0.005)));
            sig = sig + Saw.ar(freq);
            sig = sig + Saw.ar(freq * (1 + (detune * 0.005)));
            sig = sig + Saw.ar(freq * (1 + (detune * 0.01)));
            sig = sig / 5;
            // LPF to tame brightness
            sig = LPF.ar(sig, 3000);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
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
      ),

      // ========== TEXTURAL ==========
      noise: (
        description: "Filtered noise source",
        def: {
          SynthDef(\cc_noise, { |out=0, amp=0.3, cutoff=2000, res=0.3, type=0, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.8, 0.2), gate, doneAction: 2);
            sig = WhiteNoise.ar;
            // Switchable filter type: 0=LP, 1=HP, 2=BP
            sig = Select.ar(type, [
              RLPF.ar(sig, cutoff, res),
              RHPF.ar(sig, cutoff, res),
              BPF.ar(sig, cutoff, res)
            ]);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      drone: (
        description: "Evolving ambient texture",
        def: {
          SynthDef(\cc_drone, { |out=0, freq=55, amp=0.2, spread=0.5, movement=0.1, gate=1|
            var sig, env, lfo1, lfo2, lfo3;
            env = EnvGen.kr(Env.adsr(1, 0.5, 0.9, 2), gate, doneAction: 2);
            // Slow LFOs for modulation
            lfo1 = SinOsc.kr(movement * 0.1).range(0.99, 1.01);
            lfo2 = SinOsc.kr(movement * 0.13).range(0.5, 1);
            lfo3 = SinOsc.kr(movement * 0.07).range(0.98, 1.02);
            // Multiple harmonically-related oscillators
            sig = SinOsc.ar(freq * lfo1);
            sig = sig + (SinOsc.ar(freq * 2 * lfo3) * 0.3 * spread);
            sig = sig + (SinOsc.ar(freq * 3 * lfo1) * 0.15 * spread);
            sig = sig + (Saw.ar(freq * 0.5 * lfo3) * 0.1 * spread);
            // Amplitude modulation for movement
            sig = sig * lfo2;
            sig = LPF.ar(sig, 2000);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      riser: (
        description: "Tension building riser/sweep",
        def: {
          SynthDef(\cc_riser, { |out=0, amp=0.4, duration=4, startFreq=200, endFreq=4000|
            var sig, env, fenv, noise;
            env = EnvGen.kr(Env([0, 1, 0], [duration, 0.01]), doneAction: 2);
            // Frequency sweep
            fenv = EnvGen.kr(Env([startFreq, endFreq], [duration], \exp));
            // Oscillator sweep
            sig = Saw.ar(fenv) * 0.5;
            sig = sig + (Saw.ar(fenv * 1.01) * 0.3);
            // Noise crescendo
            noise = HPF.ar(WhiteNoise.ar, fenv);
            noise = noise * EnvGen.kr(Env([0, 1], [duration], \exp));
            sig = sig + (noise * 0.4);
            // Filter sweep
            sig = RLPF.ar(sig, fenv * 2, 0.3);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      // ========== UTILITY ==========
      click: (
        description: "Metronome click",
        def: {
          SynthDef(\cc_click, { |out=0, amp=0.3, freq=1500|
            var sig, env;
            env = EnvGen.kr(Env.perc(0.001, 0.02), doneAction: 2);
            // Short filtered click
            sig = SinOsc.ar(freq);
            sig = sig + (HPF.ar(WhiteNoise.ar, 4000) * 0.3);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      sine: (
        description: "Pure sine tone",
        def: {
          SynthDef(\cc_sine, { |out=0, freq=440, amp=0.3, gate=1|
            var sig, env;
            env = EnvGen.kr(Env.adsr(0.01, 0.1, 0.9, 0.1), gate, doneAction: 2);
            sig = SinOsc.ar(freq);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      sampler: (
        description: "Basic sample playback",
        def: {
          SynthDef(\cc_sampler, { |out=0, buf=0, amp=0.5, rate=1, start=0|
            var sig;
            sig = PlayBuf.ar(2, buf, rate * BufRateScale.kr(buf), startPos: start * BufFrames.kr(buf), doneAction: 2);
            sig = sig * amp;
            Out.ar(out, sig);
          })
        }
      ),

      grains: (
        description: "Granular sample playback",
        def: {
          SynthDef(\cc_grains, { |out=0, buf=0, amp=0.3, pos=0.5, posSpeed=0, grainSize=0.1, grainRate=20, pitch=1, spread=0.5, gate=1|
            var sig, env, trig, position;
            env = EnvGen.kr(Env.adsr(0.1, 0.1, 0.9, 0.5), gate, doneAction: 2);
            // Trigger for grains
            trig = Impulse.kr(grainRate);
            // Position with optional movement
            position = pos + (Phasor.kr(0, posSpeed / SampleRate.ir) % 1);
            position = position.wrap(0, 1);
            // Granular synthesis
            sig = GrainBuf.ar(
              2,
              trig,
              grainSize,
              buf,
              pitch * BufRateScale.kr(buf),
              position,
              interp: 2,
              pan: TRand.kr(-1, 1, trig) * spread
            );
            sig = sig * env * amp;
            Out.ar(out, sig);
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
      var params = synthDef.allControlNames.collect { |c| c.name.asString }.join(", ");
      "% - %\n  params: %".format(name, entry[\description], params);
    };
    lines.join("\n").postln;
  }
}
