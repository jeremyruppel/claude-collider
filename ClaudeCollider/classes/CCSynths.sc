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
        description: "Punchy kick drum. Sine wave with pitch sweep from 2x to 1x freq over 20ms, plus sub layer. Default 48Hz (~G1), 300ms decay. One-shot.",
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
        description: "Snare drum. 30% sine body (180Hz default, pitch sweep 1.5x-1x) + 70% HPF noise at 1kHz. Noise decays at half the main decay time. Default 200ms decay. One-shot.",
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
        description: "Closed hi-hat. HPF white noise at 8kHz. Very short 50ms default decay for tight sound. Lower amp default (0.3). One-shot.",
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
        description: "Hand clap. Two BPF noise layers: sharp 10ms burst at 1500Hz + sustained body at 1200Hz. Default 150ms decay. One-shot.",
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
        description: "Open hi-hat. HPF noise at 6kHz with BPF peak at 10kHz for metallic shimmer. Slight pitch envelope on attack. Default 500ms decay. One-shot.",
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
        description: "Tom drum. Sine with exponential pitch drop from 2x to target freq over 40ms. LPF noise transient for attack. Default 120Hz, 300ms decay. One-shot.",
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
        description: "Rimshot/sidestick. Tight BPF noise (Q=0.02) at 1700Hz default + 1200Hz sine click on attack. Fixed 30ms decay, very percussive. One-shot.",
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
        description: "Shaker/maraca. HPF noise grain with adjustable color (cutoff, default 5kHz). Short 80ms default decay. Lower amp (0.2). One-shot.",
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
        description: "808-style cowbell. Two detuned square waves (560Hz + 845Hz) through BPF at 700Hz. Steep -4 curve envelope. Default 400ms decay. One-shot.",
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
        description: "Sub bass with harmonics. Fundamental + 2nd harmonic (0.3x) + 3rd harmonic (0.1x). ADSR envelope (10ms att, 100ms dec, 0.7 sus, 100ms rel). Default 55Hz (~A1). Gate-controlled.",
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
        description: "303-style acid bass. Saw through resonant LPF with filter envelope (perc, sweeps up 2x cutoff). Default cutoff 1kHz, res 0.3, decay 300ms controls filter sweep time. Gate-controlled.",
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
        description: "Pure sub bass. Single sine with soft tanh saturation for warmth. Long ADSR (10ms att, 100ms dec, 0.8 sus, decay param controls release). Ideal for layering under other bass sounds. Gate-controlled.",
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
        description: "Reese bass (DnB/dubstep). Three saws detuned by +/- detune param (default 0.5Hz) for phasing movement. LPF at 800Hz default, fixed 0.3 resonance. Gate-controlled.",
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
        description: "FM bass. 2-operator FM: sine modulator at freq*ratio into sine carrier. Index (default 3) controls growl/aggression. Ratio=1 for harmonic, try 0.5 or 2 for different timbres. Gate-controlled.",
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
        description: "Detuned saw lead. Two saws (1x and 1.005x freq) through resonant LPF. Default cutoff 4kHz, res 0.2. Pannable. ADSR with adjustable att/rel (default 10ms/300ms). Gate-controlled.",
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
        description: "Karplus-Strong plucked string. Physical modeling using Pluck UGen. Color (0-1, default 0.5) controls brightness/damping. Decay (default 2s) sets ring time. One-shot.",
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
        description: "FM bell. Inharmonic FM ratios (2.4x, 3.1x, 5.2x) for metallic/glassy timbre. Brightness (default 3) controls FM index. Long 4s default decay. One-shot.",
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
        description: "Electric piano (Rhodes-style). FM with 2:1 ratio + 80ms bark envelope on attack for tine character. Brightness (0-1) controls both sustained FM index and attack bark intensity. Gate-controlled.",
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
        description: "String ensemble. Five detuned saws spread +/- detune*1% around fundamental. LPF at 3kHz tames brightness. Slow 500ms default attack, 1s release. Gate-controlled.",
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
        description: "Soft ambient pad. Three slightly detuned sines (0.99x, 1x, 1.01x) + octave-down saw pair. LPF at 2kHz. Default freq 220Hz, 500ms attack, 1s release. Gate-controlled.",
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
        description: "Filtered noise. White noise through switchable filter: type 0=LPF, 1=HPF, 2=BPF. Cutoff (default 2kHz) and res (default 0.3) shape the sound. Gate-controlled.",
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
        description: "Warm, evolving saw drone (6 stacked harmonics with independent drift, wandering resonant LPF, built-in reverb)",
        def: {
          SynthDef(\cc_drone, { |out=0, freq=55, amp=0.25, spread=0.8, movement=0.5, filterLo=200, filterHi=1200, room=0.9, mix=0.6, gate=1|
              var sigs, stereo, filt, verb, env;

              env = EnvGen.kr(Env.adsr(2, 0.5, 1, 4), gate, doneAction: 2);

              // 6 harmonically-stacked saws with independent slow detuning
              sigs = Array.fill(6, { |i|
                  var lfoRate = movement * (0.1 + (i * 0.02));
                  var detune = LFNoise1.kr(lfoRate).range(-0.5, 0.5);
                  var osc = Saw.ar(freq * (i + 1) * detune.midiratio);
                  osc * (1 / (i + 1).sqrt)  // natural harmonic rolloff
              });

              // True stereo spread
              stereo = Splay.ar(sigs, spread);

              // Wandering resonant lowpass
              filt = RLPF.ar(
                  stereo,
                  LFNoise1.kr(movement * 0.08).exprange(filterLo, filterHi),
                  0.3
              );

              // Built-in reverb
              verb = FreeVerb2.ar(filt[0], filt[1], mix: mix, room: room, damp: 0.4);

              Out.ar(out, verb * env * amp);
          })
        }
      ),

      riser: (
        description: "Tension riser/sweep. Two detuned saws + HPF noise, all sweeping exponentially from startFreq (200Hz) to endFreq (4kHz) over duration (4s). Noise crescendos during sweep. One-shot, self-timed.",
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
        description: "Metronome click. Sine (default 1500Hz) + HPF noise at 4kHz for transient. Very short 20ms fixed decay. Use freq to change pitch for downbeat/offbeat distinction. One-shot.",
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
        description: "Pure sine tone. Single SinOsc, no processing. ADSR (10ms att, 100ms dec, 0.9 sus, 100ms rel). Default 440Hz. Useful for testing, layering, or simple tones. Gate-controlled.",
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
        description: "Sample playback. Plays stereo buffer with rate control (negative=reverse). Start (0-1) sets playback position. Auto-frees when buffer ends. Use with ~cc.samples.at(name) for buf param. One-shot.",
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
        description: "Granular synthesis. GrainBuf with adjustable pos (0-1), posSpeed (movement rate), grainSize (100ms default), grainRate (20/sec), pitch, and stereo spread. Random pan per grain. Gate-controlled.",
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
