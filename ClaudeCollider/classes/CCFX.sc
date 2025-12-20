// CCFX - Effects loading and management for ClaudeCollider

CCFX {
  var <cc;
  var <defs;
  var <loaded;       // slot -> (name, ndef, inBus, params)
  var <outputs;      // CCOutputs instance
  var <sidechains;   // CCSidechain instance
  var <router;       // CCRouter instance

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    loaded = Dictionary[];
    outputs = CCOutputs(cc);
    sidechains = CCSidechain(cc);
    router = CCRouter(this);
    defs = this.defineEffects;
    this.addRoutingSynthDefs;
  }

  addRoutingSynthDefs {
    SynthDef(\cc_bus_copy, { |in, out|
      Out.ar(out, In.ar(in, 2));
    }).add;
  }

  // ========== EFFECT DEFINITIONS ==========

  defineEffects {
    ^(
      // ========== FILTERS ==========
      lpf: (
        description: "Low pass filter with resonance",
        params: [\cutoff, 1000, \resonance, 0.5],
        func: { |in, cutoff=1000, resonance=0.5|
          RLPF.ar(In.ar(in, 2), cutoff, resonance);
        }
      ),

      hpf: (
        description: "High pass filter with resonance",
        params: [\cutoff, 200, \resonance, 0.5],
        func: { |in, cutoff=200, resonance=0.5|
          RHPF.ar(In.ar(in, 2), cutoff, resonance);
        }
      ),

      bpf: (
        description: "Band pass filter",
        params: [\freq, 1000, \bw, 0.5],
        func: { |in, freq=1000, bw=0.5|
          BPF.ar(In.ar(in, 2), freq, bw);
        }
      ),

      // ========== TIME-BASED ==========
      reverb: (
        description: "Stereo reverb",
        params: [\mix, 0.33, \room, 0.8, \damp, 0.5],
        func: { |in, mix=0.33, room=0.8, damp=0.5|
          var sig = In.ar(in, 2);
          FreeVerb2.ar(sig[0], sig[1], mix, room, damp);
        }
      ),

      delay: (
        description: "Stereo delay with feedback",
        params: [\time, 0.375, \feedback, 0.5, \mix, 0.5],
        func: { |in, time=0.375, feedback=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var delayed = CombL.ar(sig, 2, time, feedback * 4);
          (sig * (1 - mix)) + (delayed * mix);
        }
      ),

      pingpong: (
        description: "Ping pong stereo delay",
        params: [\time, 0.375, \feedback, 0.5, \mix, 0.5],
        func: { |in, time=0.375, feedback=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var left = CombL.ar(sig[0], 2, time, feedback * 4);
          var right = CombL.ar(sig[1] + (left * 0.5), 2, time, feedback * 4);
          var delayed = [left + (right * 0.5), right];
          (sig * (1 - mix)) + (delayed * mix);
        }
      ),

      // ========== MODULATION ==========
      chorus: (
        description: "Stereo chorus",
        params: [\rate, 0.5, \depth, 0.005, \mix, 0.5],
        func: { |in, rate=0.5, depth=0.005, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr([rate, rate * 1.01], [0, 0.5]).range(0.001, depth);
          var wet = DelayL.ar(sig, 0.1, mod);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      flanger: (
        description: "Flanger with feedback",
        params: [\rate, 0.2, \depth, 0.003, \feedback, 0.5, \mix, 0.5],
        func: { |in, rate=0.2, depth=0.003, feedback=0.5, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(0.0001, depth);
          var wet = DelayL.ar(sig + (LocalIn.ar(2) * feedback), 0.02, mod);
          LocalOut.ar(wet);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      phaser: (
        description: "Phaser effect",
        params: [\rate, 0.3, \depth, 2, \mix, 0.5],
        func: { |in, rate=0.3, depth=2, mix=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(100, 4000);
          var wet = AllpassL.ar(sig, 0.1, mod.reciprocal, 0);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      tremolo: (
        description: "Amplitude modulation tremolo",
        params: [\rate, 4, \depth, 0.5],
        func: { |in, rate=4, depth=0.5|
          var sig = In.ar(in, 2);
          var mod = SinOsc.kr(rate).range(1 - depth, 1);
          sig * mod;
        }
      ),

      // ========== DISTORTION ==========
      distortion: (
        description: "Soft clip distortion with tone control",
        params: [\drive, 2, \tone, 0.5, \mix, 1],
        func: { |in, drive=2, tone=0.5, mix=1|
          var sig = In.ar(in, 2);
          var wet = (sig * drive).tanh;
          wet = LPF.ar(wet, tone.linexp(0, 1, 1000, 12000));
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      bitcrush: (
        description: "Bit crusher with sample rate reduction",
        params: [\bits, 8, \rate, 44100, \mix, 1],
        func: { |in, bits=8, rate=44100, mix=1|
          var sig = In.ar(in, 2);
          var crushed = sig.round(2.pow(1 - bits));
          var wet = Latch.ar(crushed, Impulse.ar(rate));
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      wavefold: (
        description: "Wave folder distortion",
        params: [\amount, 2, \mix, 1],
        func: { |in, amount=2, mix=1|
          var sig = In.ar(in, 2);
          var wet = (sig * amount).fold(-1, 1);
          (sig * (1 - mix)) + (wet * mix);
        }
      ),

      // ========== DYNAMICS ==========
      compressor: (
        description: "Dynamics compressor",
        params: [\threshold, 0.5, \ratio, 4, \attack, 0.01, \release, 0.1, \makeup, 1],
        func: { |in, threshold=0.5, ratio=4, attack=0.01, release=0.1, makeup=1|
          var sig = In.ar(in, 2);
          Compander.ar(sig, sig, threshold, 1, ratio.reciprocal, attack, release, makeup);
        }
      ),

      limiter: (
        description: "Brickwall limiter",
        params: [\level, 0.95, \lookahead, 0.01],
        func: { |in, level=0.95, lookahead=0.01|
          Limiter.ar(In.ar(in, 2), level, lookahead);
        }
      ),

      gate: (
        description: "Noise gate",
        params: [\threshold, 0.1, \attack, 0.01, \release, 0.1],
        func: { |in, threshold=0.1, attack=0.01, release=0.1|
          var sig = In.ar(in, 2);
          var env = Amplitude.kr(sig).max > threshold;
          sig * EnvGen.kr(Env.asr(attack, 1, release), env);
        }
      ),

      // ========== STEREO ==========
      widener: (
        description: "Stereo width control",
        params: [\width, 1.5],
        func: { |in, width=1.5|
          var sig = In.ar(in, 2);
          var mid = (sig[0] + sig[1]) * 0.5;
          var side = (sig[0] - sig[1]) * 0.5 * width;
          [mid + side, mid - side];
        }
      ),

      autopan: (
        description: "Automatic stereo panning",
        params: [\rate, 1, \depth, 1],
        func: { |in, rate=1, depth=1|
          var sig = In.ar(in, 2);
          var pan = SinOsc.kr(rate).range(-1 * depth, depth);
          Balance2.ar(sig[0], sig[1], pan);
        }
      )
    );
  }

  loadDefs {
    "CCFX: % effects available".format(defs.size).postln;
  }

  // ========== EFFECT LOADING ==========

  load { |name, slot|
    var def = defs[name];
    var slotName, inBus, ndef, func, paramPairs;

    if(def.isNil) {
      "CCFX: unknown effect '%'".format(name).warn;
      ^nil;
    };

    slotName = slot ?? ("fx_" ++ name);
    inBus = Bus.audio(cc.server, 2);
    func = def[\func];
    paramPairs = def[\params].clump(2);

    ndef = Ndef(slotName.asSymbol, { |in|
      var params = paramPairs.collect { |pair|
        NamedControl.kr(pair[0], pair[1]);
      };
      func.valueArray([in] ++ params);
    });
    ndef.set(\in, inBus.index);
    ndef.play;

    loaded[slotName.asSymbol] = (
      name: name,
      ndef: ndef,
      inBus: inBus,
      params: def[\params]
    );

    "Loaded % on bus %".format(slotName, inBus.index).postln;
  }

  set { |slot ...args|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      info.ndef.set(*args);
      ^true;
    } {
      "CCFX: slot '%' not loaded".format(slot).warn;
      ^false;
    };
  }

  bypass { |slot, shouldBypass=true|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      if(shouldBypass) { info.ndef.stop } { info.ndef.play };
      ^true;
    };
    ^false;
  }

  // ========== DELEGATION TO ROUTER ==========

  connect { |from, to|
    ^router.connect(from, to);
  }

  registerChain { |name, slots|
    router.registerChain(name, slots);
  }

  route { |source, target|
    ^router.route(source, target);
  }

  // ========== DELEGATION TO SIDECHAIN ==========

  sidechain { |name, threshold=0.1, ratio=4, attack=0.01, release=0.1|
    ^sidechains.create(name, threshold, ratio, attack, release);
  }

  routeTrigger { |source, sidechainName, passthrough=true|
    ^sidechains.routeTrigger(source, sidechainName, passthrough);
  }

  // ========== DELEGATION TO OUTPUTS ==========

  playMainOutput {
    outputs.playMain;
  }

  setMainOutput { |out|
    outputs.setMainHwOut(out);
  }

  stopMainOutput {
    outputs.stopMain;
  }

  isMainOutputPlaying {
    ^outputs.isMainPlaying;
  }

  routeToOutput { |source, channels|
    ^outputs.route(source, channels);
  }

  unrouteFromOutput { |source|
    ^outputs.unroute(source);
  }

  outputStatus {
    ^outputs.status;
  }

  // ========== EFFECT REMOVAL ==========

  remove { |slot|
    var info = loaded[slot.asSymbol];

    if(info.notNil) {
      router.removeEffect(slot);
      info.ndef.clear;
      info.inBus.free;
      loaded.removeAt(slot.asSymbol);
      ^true;
    };

    // Check if it's a sidechain
    if(sidechains.at(slot.asSymbol).notNil) {
      ^sidechains.remove(slot);
    };

    ^false;
  }

  clearAll {
    var mainWasPlaying = outputs.isMainPlaying;
    var main = outputs.main;
    var mainHwOut = if(main.notNil and: { main.respondsTo(\hwOut) }) { main.hwOut } { 0 };

    router.clear;
    sidechains.clear;

    loaded.keysValuesDo { |slot, info|
      info.ndef.clear;
      info.inBus.free;
    };
    loaded.clear;

    outputs.clear;

    if(mainWasPlaying) {
      outputs.playMain;
      outputs.setMainHwOut(mainHwOut);
    };
  }

  // ========== INSPECTION ==========

  list {
    var names = defs.keys.asArray.sort;
    if(names.isEmpty) { ^"(none)" };
    ^names.join(", ");
  }

  status {
    var lines = [];
    var chainSlots = Set[];

    router.chains.do { |slots| slots.do { |s| chainSlots.add(s.asSymbol) } };

    // Effects section
    loaded.keys.select { |slot| chainSlots.includes(slot).not }.do { |slot|
      var info = loaded[slot];
      if(lines.isEmpty) { lines = lines.add("Effects:") };
      lines = lines.add("  % (%) - bus %".format(slot, info.name, info.inBus.index));
    };

    // Chains section
    if(router.chains.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Chains:");
      router.chains.keysValuesDo { |name, slots|
        lines = lines.add("  %: % -> main".format(name, slots.join(" -> ")));
      };
    };

    // Sidechains section
    if(sidechains.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Sidechains:");
      sidechains.keysValuesDo { |name, info|
        lines = lines.add("  % - in: %, trig: %".format(name, info.inBus.index, info.triggerBus.index));
      };
    };

    if(lines.isEmpty) { ^"No effects loaded" };
    ^lines.join("\n");
  }

  describe {
    var lines = defs.keys.asArray.sort.collect { |name|
      var entry = defs[name];
      var params = entry[\params].clump(2).collect { |pair|
        "% (default: %)".format(pair[0], pair[1]);
      }.join(", ");
      "% - %\n  params: %".format(name, entry[\description], params);
    };
    ^lines.join("\n");
  }
}
