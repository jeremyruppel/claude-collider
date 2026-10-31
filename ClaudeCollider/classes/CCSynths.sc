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
      cc_kick: (
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

      cc_snare: (
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

      cc_hihat: (
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

      cc_clap: (
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

      cc_openhat: (
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

      cc_tom: (
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

      cc_fmtom: (
        description: "FM tom. Sine carrier with ratio sweep from ratioStart to ratioEnd over sweep time for harmonic attack. Index controls FM depth. Default 120Hz, 300ms decay. One-shot.",
        def: {
          SynthDef(\cc_fmtom, { |out=0, freq=120, amp=0.5, decay=0.3, index=1.5, ratioStart=2, ratioEnd=1, sweep=0.3|
            var sig, mod, env, ratio;
            env = EnvGen.kr(Env.perc(0.01, decay), doneAction: 2);
            ratio = EnvGen.kr(Env([ratioStart, ratioEnd], [sweep], \exp));
            mod = SinOsc.ar(freq * ratio) * index * freq;
            sig = SinOsc.ar(freq + mod);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      cc_rim: (
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

      cc_shaker: (
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

      cc_cowbell: (
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
      cc_bass: (
        description: "Warm analog bass. Saw-to-square oscillator (shape 0-1) with a detuned second osc into a Moog ladder filter with a plucky, velocity-scaled envelope (cutoff, fenv amount, fdecay time). Res works like RLPF rq: lower = more resonant. Clean sine sub bypasses the filter (sub) so lows stay solid. Pitch blip on attack for punch, gentle saturation (drive). Decay sets amp decay to sustain. Glide for Pmono. Default 55Hz (~A1). Gate-controlled.",
        def: {
          SynthDef(\cc_bass, { |out=0, freq=55, amp=0.5, decay=0.5, gate=1, rel=0.08, vel=0.7,
                              cutoff=700, res=0.6, fenv=2, fdecay=0.15, shape=0.3, sub=0.6, drive=0.3, glide=0|
            var sig, env, f, v, osc, fe, k, cut;
            v = vel.clip(0, 1);
            env = EnvGen.kr(Env.adsr(0.003, decay, 0.6, rel), gate, doneAction: 2);
            // Glide, plus a small pitch blip on the attack for punch
            f = Lag.kr(freq, glide) * EnvGen.kr(Env([1.03, 1], [0.015], -4));
            // Saw-to-square, two slightly detuned oscillators
            osc = [f, f * 1.004].collect { |ff| XFade2.ar(Saw.ar(ff), Pulse.ar(ff, 0.5), shape * 2 - 1) }.sum * 0.5;
            // Plucky filter envelope, scaled by velocity, lightly key tracked
            fe = EnvGen.kr(Env.perc(0.002, fdecay, 1, -4)) * fenv * (v * 0.8 + 0.2);
            cut = (cutoff * ((freq / 55) ** 0.3) * (1 + fe)).clip(30, 12000);
            k = (1 - res).clip(0, 1) * 3.2;
            osc = MoogFF.ar(osc, cut, k) * (1 + (k * 0.9));
            // Clean sub bypasses the filter
            sig = osc + (SinOsc.ar(f) * sub);
            sig = (sig * (1 + (drive * 2))).tanh / (1 + (drive * 0.5));
            sig = LeakDC.ar(sig);
            // Gain roughly matched to the old cc_bass (slightly hotter attack, similar body)
            sig = sig * env * amp * (v * 0.5 + 0.65) * 1.3;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      cc_acid: (
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

      cc_sub: (
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

      cc_reese: (
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

      cc_fmbass: (
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

      cc_fmsub: (
        description: "FM sub bass. Sine carrier with sine modulator for subtle harmonics. Short sustain (0.3), medium release (500ms). Index controls FM depth. Gate-controlled.",
        def: {
          SynthDef(\cc_fmsub, { |out=0, freq=55, amp=0.4, index=1, ratio=1, attack=0.01, sustain=0.3, release=0.5, gate=1|
            var sig, mod, env;
            env = EnvGen.kr(Env.adsr(attack, 0.1, sustain, release), gate, doneAction: 2);
            mod = SinOsc.ar(freq * ratio) * index * freq;
            sig = SinOsc.ar(freq + mod);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      // ========== LEADS ==========
      cc_lead: (
        description: "Analog-style poly lead. Three detuned saws (detune, in semitones) spread in stereo (spread), PWM pulse layer (pulse mix), sub octave (sub). Soft drive into a Moog ladder filter with key tracking and a velocity-scaled filter envelope (fenv amount, fdecay time). Res works like RLPF rq: lower = more resonant. Delayed-onset vibrato (vib semitones, vibRate) plus slow pitch drift. Glide (seconds) for Pmono lines. Vel (0-1) drives filter sweep and level. Gate-controlled.",
        def: {
          SynthDef(\cc_lead, { |out=0, freq=440, amp=0.5, pan=0, gate=1,
                              att=0.01, rel=0.3, cutoff=4000, res=0.2, vel=0.7,
                              detune=0.1, spread=0.6, pulse=0.3, sub=0.25,
                              fenv=1.5, fdecay=0.25, vib=0.12, vibRate=5.5, glide=0, drive=0.3|
            var sig, env, f, v, vibEnv, saws, pw, filtFreq, fe, k;
            v = vel.clip(0, 1);
            env = EnvGen.kr(Env.adsr(att, 0.2, 0.75, rel), gate, doneAction: 2);
            // Pitch: glide, slow analog drift, vibrato that fades in on held notes
            vibEnv = EnvGen.kr(Env([0, 0, 1], [0.3, 0.6], [0, 2]));
            f = Lag.kr(freq, glide);
            f = f * (LFNoise1.kr(0.3).range(-0.03, 0.03)
              + (SinOsc.kr(vibRate * LFNoise1.kr(0.5).range(0.95, 1.05)) * vib * vibEnv)).midiratio;
            // Three detuned saws spread in stereo
            saws = Saw.ar(f * ([-1, 0, 1] * detune).midiratio * Rand(0.999, 1.001 ! 3));
            sig = Splay.ar(saws, spread) * 0.6;
            // Pulse with slow width modulation
            pw = LFTri.kr(Rand(0.2, 0.4)).range(0.2, 0.5);
            sig = sig + (Pulse.ar(f * 1.002, pw) * pulse * 0.5);
            // Sub an octave down, kept centered
            sig = sig + (SinOsc.ar(f * 0.5) * sub);
            // Soft drive into the filter
            sig = (sig * (1 + (drive * 2))).tanh;
            // Filter: key tracked, envelope sweep scaled by velocity
            fe = EnvGen.kr(Env.perc(0.005, fdecay, 1, -4)) * fenv * (v * 0.8 + 0.2);
            filtFreq = (cutoff * ((freq / 440) ** 0.5) * (1 + fe)).clip(40, 18000);
            k = (1 - res).clip(0, 1) * 3.2;
            sig = MoogFF.ar(sig, filtFreq, k);
            sig = LeakDC.ar(sig);
            // Makeup gain: the ladder loses level as resonance rises. Matched to the old cc_lead.
            sig = sig * (1 + (k * 0.9)) * 1.75;
            sig = sig * env * amp * (v * 0.5 + 0.65);
            Out.ar(out, Balance2.ar(sig[0], sig[1], pan));
          })
        }
      ),

      // ========== MELODIC ==========
      cc_pluck: (
        description: "Plucked string (Karplus-Strong). Two slightly detuned strings per note (detune, cents) spread in stereo (spread). Noise excitation gets brighter with vel and is shaped by pick position (pickPos 0-0.5). Color (0-1) sets string damping. Decay (default 2s) sets ring time, shorter for higher notes. Guitar body resonances (body), pick click. Mute (0-1) damps the string when the note is released, like palm muting (release sets damp time). Rings as a one-shot when mute is 0.",
        def: {
          SynthDef(\cc_pluck, { |out=0, freq=440, amp=0.4, decay=2, color=0.5, vel=0.7, pickPos=0.15,
                               detune=4, spread=0.5, body=0.4, mute=0, release=0.1, gate=1, pan=0|
            var sig, env, v, ringTime, exc, strings, click, resonances, damp;
            v = vel.clip(0, 1);
            // Higher notes ring a bit shorter
            ringTime = decay * ((freq / 440) ** -0.25).clip(0.4, 2.5);
            env = EnvGen.kr(Env.linen(0.001, ringTime, 0.05), doneAction: 2);
            // Excitation: brighter when picked harder, comb-shaped by pick position
            exc = LPF.ar(WhiteNoise.ar, (1500 + (v * 12000)).min(18000));
            exc = exc - DelayN.ar(exc, 0.05, freq.reciprocal * pickPos.clip(0.02, 0.5));
            // Two slightly detuned strings
            strings = [-1, 1].collect { |side|
              var f = freq * (side * detune * 0.5 / 100).midiratio;
              Pluck.ar(exc, Impulse.ar(0), 0.05, f.reciprocal, ringTime, color)
            };
            sig = Splay.ar(strings, spread, center: pan);
            // Guitar body resonances
            resonances = sig.collect { |ch|
              BPF.ar(ch, [105, 210, 420, 2500], [0.3, 0.3, 0.4, 0.6], [1, 0.8, 0.5, 0.3]).sum
            };
            sig = (sig * (1 - (body * 0.4))) + (resonances * body);
            // Pick click
            click = HPF.ar(WhiteNoise.ar, 3000) * EnvGen.ar(Env.perc(0.0003, 0.004)) * v * 0.3;
            sig = sig + click;
            // Palm mute: damp and darken the string when the note is released
            damp = 1 - (mute.clip(0, 1) * (1 - EnvGen.kr(Env.asr(0, 1, release), gate)));
            sig = LPF.ar(sig, (freq * 2 + (damp * 15000)).min(18000)) * damp;
            sig = sig * env * amp * (v * 0.5 + 0.65);
            Out.ar(out, sig);
          })
        }
      ),

      cc_bell: (
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

      cc_keys: (
        description: "Electric piano (Rhodes-style). Two FM layers: 1:1 body whose index falls from bark to mellow, plus a 14:1 tine ping on the strike. Notes decay naturally while held (shorter in the upper register). Vel (0-1) drives brightness and bark. Asymmetric pickup saturation (drive) adds growl. Hammer noise, damper thump on release, and suitcase-style stereo tremolo (tremRate, tremDepth). Gate-controlled.",
        def: {
          SynthDef(\cc_keys, { |out=0, freq=440, amp=0.4, vel=0.7, attack=0.001, decay=4, release=0.5,
                              brightness=0.5, drive=0.4, tremRate=4.5, tremDepth=0.3, pan=0, gate=1|
            var sig, env, kt, f, v, decayT, idx, idxEnv, body, ping, hammer, thump, d;
            v = vel.clip(0, 1);
            // Octaves from middle C, for key tracking
            kt = (freq / 261.63).log2.clip(-3, 3);
            // Each tine is slightly out of tune
            f = freq * (Rand(-2.0, 2.0) / 100).midiratio;
            // Higher notes ring shorter
            decayT = decay * (1.6 ** kt.neg);
            // Decays while held; release damps from wherever it is
            env = EnvGen.ar(Env([0, 1, 0, 0], [attack, decayT, release], [0, -5, -4], releaseNode: 2), gate, doneAction: 2);
            // Body: 1:1 phase modulation, index falls from bark to mellow
            idx = (brightness * 1.5 + 0.3) * (v * 0.8 + 0.4) * (1 - (kt * 0.15)).clip(0.3, 1.5);
            idxEnv = EnvGen.kr(Env([2.5, 1, 0.4], [0.06, decayT * 0.6], -4));
            body = SinOsc.ar(f, SinOsc.ar(f) * idx * idxEnv);
            // Tine ping: high-ratio modulator burst on the strike
            ping = SinOsc.ar(f, SinOsc.ar(f * 14) * (v * brightness * 2.5) * EnvGen.kr(Env.perc(0.0005, 0.04)));
            ping = ping * EnvGen.kr(Env.perc(0.001, decayT * 0.15));
            sig = body + (ping * 0.25);
            // Asymmetric pickup saturation: even harmonics, more growl when played hard
            d = drive * (0.5 + v);
            sig = (((sig * (1 + (d * 3))) + (d * 0.3)).tanh - (d * 0.3).tanh) / (1 + d);
            sig = LeakDC.ar(sig);
            sig = LPF.ar(sig, (freq * (6 + (brightness * 20))).clip(800, 16000));
            // Hammer noise on the strike
            hammer = BPF.ar(WhiteNoise.ar, (freq * 3).clip(200, 8000), 0.5) * EnvGen.kr(Env.perc(0.0005, 0.01)) * v * 0.1;
            sig = (sig + hammer) * env * (v * 0.6 + 0.4);
            // Damper thump when the key comes up
            thump = LPF.ar(PinkNoise.ar, 300 + freq) * EnvGen.kr(Env.perc(0.002, 0.06), 1 - gate) * 0.08;
            // Gain matched so vel=0.7 is as loud as the old cc_keys
            sig = (sig + thump) * amp * 1.6;
            // Suitcase-style stereo tremolo
            Out.ar(out, Pan2.ar(sig, (pan + (SinOsc.kr(tremRate) * tremDepth)).clip(-1, 1)));
          })
        }
      ),

      cc_strings: (
        description: "Bowed string section. Four players per note, each with its own vibrato (vib semitones, vibRate) that fades in, spread +/- detune% and across stereo (spread). Brightness follows the bow swell and vel (cutoff). Wooden body resonances (body), bow noise strongest on the attack (bow), string-machine ensemble chorus (ensemble). Slow 500ms default attack, 1s release. Gate-controlled.",
        def: {
          SynthDef(\cc_strings, { |out=0, freq=440, amp=0.3, attack=0.5, release=1, detune=0.3, gate=1, pan=0,
                                 vel=0.7, cutoff=3000, vib=0.15, vibRate=5.5, spread=0.7,
                                 body=0.5, bow=0.15, ensemble=0.5|
            var sig, env, v, vibEnv, players, bright, resonances, noise, chorus;
            v = vel.clip(0, 1);
            env = EnvGen.kr(Env([0, 1, 0], [attack, release], [\sin, -3], 1), gate, doneAction: 2);
            // Players settle into vibrato after the bow starts
            vibEnv = EnvGen.kr(Env([0, 0, 1], [attack * 0.5, attack + 0.5], \sin));
            players = Array.fill(4, { |i|
              var offset = (i / 3 * 2 - 1) * detune * 0.01;
              var vibrato = SinOsc.kr(vibRate * Rand(0.85, 1.15), Rand(0, 2pi)) * vib * vibEnv;
              var drift = LFNoise1.kr(Rand(0.1, 0.3)) * 0.04;
              Saw.ar(freq * (1 + offset) * (vibrato + drift).midiratio);
            });
            sig = Splay.ar(players, spread, center: pan);
            // Brightness follows the bow
            bright = (cutoff * (0.35 + (env * 0.65)) * (v * 0.6 + 0.6) * ((freq / 440) ** 0.3)).clip(200, 16000);
            sig = LPF.ar(sig, bright);
            // Wooden body resonances
            resonances = sig.collect { |ch|
              BPF.ar(ch, [280, 520, 1150, 2800], [0.4, 0.5, 0.6, 0.7], [1, 0.8, 0.6, 0.4]).sum
            };
            sig = (sig * (1 - (body * 0.5))) + (resonances * body);
            // Bow noise, strongest on the attack
            noise = BPF.ar(PinkNoise.ar ! 2, (freq * 2).clip(200, 8000), 0.4)
              * (0.4 + (EnvGen.kr(Env.perc(0.01, 0.3)) * 1.5)) * bow * v;
            // Gain matched to the old cc_strings
            sig = (sig + noise) * env * amp * 0.5;
            // String-machine ensemble: three slow+fast modulated delay taps per side
            chorus = sig.collect { |ch, side|
              Array.fill(3, { |j|
                var t = 0.008 + (SinOsc.kr(0.6 + (j * 0.13), (side * 0.5) + (j * 2pi / 3)) * 0.003)
                  + (SinOsc.kr(6.1 + (j * 0.4), j) * 0.0004);
                DelayC.ar(ch, 0.03, t)
              }).sum / 3
            };
            sig = XFade2.ar(sig, chorus, ensemble * 2 - 1);
            Out.ar(out, sig);
          })
        }
      ),

      // ========== PADS ==========
      cc_pad: (
        description: "Lush evolving pad. Five detuned saws (detune, semitones), each drifting independently, spread in stereo (spread). Sine sub an octave down (sub). Resonant LPF opens as the note swells and breathes with a slow random LFO (cutoff, movement). Octave-up shimmer fades in after the attack (shimmer). Band-passed noise air layer (air). Sine-curve swells. Default 500ms attack, 1s release. Gate-controlled.",
        def: {
          SynthDef(\cc_pad, { |out=0, freq=220, amp=0.3, attack=0.5, release=1, gate=1, pan=0,
                             cutoff=2000, detune=0.15, spread=0.8, movement=0.5,
                             sub=0.3, shimmer=0.2, air=0.15|
            var sig, env, voices, breath, cut, shim, noise;
            env = EnvGen.kr(Env.asr(attack, 1, release, \sin), gate, doneAction: 2);
            // Five detuned saws, each drifting on its own
            voices = Array.fill(5, { |i|
              var offset = (i / 4 * 2 - 1) * detune;
              var drift = LFNoise2.kr(movement * Rand(0.1, 0.3)) * detune * 0.3;
              Saw.ar(freq * (offset + drift).midiratio);
            });
            sig = Splay.ar(voices, spread, center: pan);
            sig = sig + (SinOsc.ar(freq * 0.5) * sub);
            // Filter opens as the note swells and breathes slowly
            breath = LFNoise2.kr(movement * Rand(0.05, 0.15)).range(0.7, 1.4);
            cut = (cutoff * (0.4 + (env * 0.6)) * breath * ((freq / 220) ** 0.3)).clip(80, 16000);
            sig = RLPF.ar(sig, cut, 0.6);
            // Octave-up shimmer, fades in after the attack
            shim = SinOsc.ar(freq * 2 * [1.003, 0.997])
              * EnvGen.kr(Env([0, 0, 1], [attack * 0.5, attack * 1.5 + 1], \sin)) * shimmer * 0.3;
            // Breathy air
            noise = BPF.ar(PinkNoise.ar ! 2, (freq * 4).clip(500, 9000), 0.3) * air * breath;
            sig = (sig + shim + noise) * env * amp;
            Out.ar(out, sig);
          })
        }
      ),

      // ========== TEXTURAL ==========
      cc_noise: (
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

      cc_drone: (
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

      cc_riser: (
        description: "Tension riser/sweep. Two detuned saws + layered noise, all sweeping exponentially from startFreq (200Hz) to endFreq (4kHz) over duration (4s). Noise crescendos during sweep. One-shot, self-timed.",
        def: {
          SynthDef(\cc_riser, { |out=0, amp=0.4, duration=4, startFreq=200, endFreq=4000|
            var sig, env, fenv, noise, noise2, noiseEnv;
            env = EnvGen.kr(Env([0, 1, 0], [duration, 0.01]), doneAction: 2);
            noiseEnv = EnvGen.kr(Env([0.001, 1], [duration], \exp));
            // Frequency sweep
            fenv = EnvGen.kr(Env([startFreq, endFreq], [duration], \exp));
            // Oscillator sweep
            sig = Saw.ar(fenv) * 0.4;
            sig = sig + (Saw.ar(fenv * 1.01) * 0.25);
            // Noise layer 1: HPF white noise following sweep
            noise = HPF.ar(WhiteNoise.ar, fenv) * noiseEnv * 0.5;
            // Noise layer 2: BPF noise centered on sweep freq for resonant screech
            noise2 = BPF.ar(WhiteNoise.ar, fenv * 1.5, 0.1) * noiseEnv * 2;
            // Noise layer 3: broadband crackle
            noise = noise + (HPF.ar(PinkNoise.ar, 2000) * noiseEnv * 0.4);
            sig = sig + noise + noise2;
            // Filter sweep
            sig = RLPF.ar(sig, fenv * 2, 0.3);
            sig = sig * env * amp;
            Out.ar(out, sig ! 2);
          })
        }
      ),

      // ========== UTILITY ==========
      cc_click: (
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

      cc_sine: (
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

      cc_sampler: (
        description: "Sample playback. Plays stereo buffer with rate control (negative=reverse). Start (0-1) sets playback position. Auto-frees when buffer ends. Use with ~cc.samples.at(name) for buf param. One-shot.",
        def: {
          SynthDef(\cc_sampler, { |out=0, buf=0, amp=0.5, rate=1, start=0|
            var sig, frames;
            frames = BufFrames.kr(buf);
            FreeSelf.kr(frames <= 0);
            sig = PlayBuf.ar(2, buf, rate * BufRateScale.kr(buf), startPos: start * frames, doneAction: 2);
            sig = sig * amp;
            Out.ar(out, sig);
          })
        }
      ),

      cc_breakbeat: (
        description: "Breakbeat slice playback. Plays a segment of a stereo buffer from start (0-1) to end (0-1) with rate control. 3ms fade-out prevents clicks. Auto-frees. Use with CCBreakbeat class.",
        def: {
          SynthDef(\cc_breakbeat, { |out=0, buf=0, amp=0.5, rate=1, start=0, end=1|
            var sig, frames, startPos, endPos, dur, env;
            frames = BufFrames.kr(buf);
            FreeSelf.kr(frames <= 0);
            startPos = start * frames;
            endPos = end * frames;
            dur = (endPos - startPos).abs / (BufSampleRate.kr(buf) * rate.abs);
            env = EnvGen.kr(
              Env([1, 1, 0], [dur - 0.003, 0.003]),
              doneAction: 2
            );
            sig = PlayBuf.ar(2, buf, rate * BufRateScale.kr(buf), startPos: startPos, doneAction: 2);
            sig = sig * env * amp;
            Out.ar(out, sig);
          })
        }
      ),

      cc_grains: (
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
    if(loaded.includes(name).not) { this.load(name) };
    ^Synth(name.asSymbol, args);
  }

  list {
    var names = defs.keys.asArray.sort;
    if(names.isEmpty) { ^"(none)" };
    ^names.join(", ");
  }

  describe {
    var lines = defs.keys.asArray.sort.collect { |name|
      var entry = defs[name];
      var synthDef = entry[\def].value;
      var params = synthDef.allControlNames.collect { |c| c.name.asString }.join(", ");
      "% - %\n  params: %".format(name, entry[\description], params);
    };
    ^lines.join("\n");
  }
}
