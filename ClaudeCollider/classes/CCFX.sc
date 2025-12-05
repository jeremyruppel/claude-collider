// CCFX - Effects loading, chaining, and routing for ClaudeCollider

CCFX {
  var <cc;
  var <defs;
  var <loaded;      // slot -> (name, ndef, inBus)
  var <chains;      // chainName -> (inBus, slots, buses)
  var <sidechains;  // name -> (inBus, triggerBus)

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    loaded = Dictionary[];
    chains = Dictionary[];
    sidechains = Dictionary[];
    defs = this.defineEffects;
  }

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
    ndef.set(\in, inBus);
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

  chain { |name, effects|
    var buses = [];
    var slots = [];
    var chainIn = Bus.audio(cc.server, 2);

    buses = buses.add(chainIn);

    effects.do { |fx, i|
      var fxName, fxParams, slotName, def, ndef, nextBus;

      if(fx.isKindOf(Association)) {
        fxName = fx.key;
        fxParams = fx.value;
      } {
        fxName = fx;
        fxParams = [];
      };

      def = defs[fxName];
      if(def.isNil) {
        "CCFX: unknown effect '%' in chain".format(fxName).warn;
      } {
        slotName = "chain_%_%".format(name, fxName).asSymbol;

        if(i < (effects.size - 1)) {
          nextBus = Bus.audio(cc.server, 2);
          buses = buses.add(nextBus);
        };

        ndef = Ndef(slotName, { |in|
          var sig = def.func.value(in);
          if(nextBus.notNil) {
            Out.ar(nextBus, sig);
          } {
            sig;
          };
        });
        ndef.set(\in, buses[i]);
        ndef.set(*fxParams);
        ndef.play;

        loaded[slotName] = (
          name: fxName,
          ndef: ndef,
          inBus: buses[i],
          chain: name
        );

        slots = slots.add(slotName);
      };
    };

    chains[name.asSymbol] = (
      inBus: chainIn,
      slots: slots,
      buses: buses
    );

    ^(name: name, bus: chainIn, slots: slots);
  }

  sidechain { |name, threshold=0.1, ratio=4, attack=0.01, release=0.1|
    var inBus = Bus.audio(cc.server, 2);
    var triggerBus = Bus.audio(cc.server, 2);
    var slotName = ("sidechain_" ++ name).asSymbol;

    Ndef(slotName, { |in, trigger, thresh=0.1, rat=4, att=0.01, rel=0.1|
      var sig = In.ar(in, 2);
      var trig = In.ar(trigger, 2);
      Compander.ar(sig, trig, thresh, 1, rat.reciprocal, att, rel);
    });
    Ndef(slotName).set(\in, inBus, \trigger, triggerBus);
    Ndef(slotName).set(\thresh, threshold, \rat, ratio, \att, attack, \rel, release);
    Ndef(slotName).play;

    sidechains[name.asSymbol] = (
      inBus: inBus,
      triggerBus: triggerBus,
      slot: slotName
    );

    ^(name: name, inputBus: inBus, triggerBus: triggerBus);
  }

  route { |source, target|
    var targetInfo, targetBus;

    // Find target bus
    if(chains[target.asSymbol].notNil) {
      targetBus = chains[target.asSymbol].inBus;
    } {
      if(loaded[target.asSymbol].notNil) {
        targetBus = loaded[target.asSymbol].inBus;
      } {
        if(sidechains[target.asSymbol].notNil) {
          targetBus = sidechains[target.asSymbol].inBus;
        };
      };
    };

    if(targetBus.isNil) {
      "CCFX: target '%' not found".format(target).warn;
      ^false;
    };

    // Route the source
    if(Pdef.all[source.asSymbol].notNil) {
      Pdef(source.asSymbol).set(\out, targetBus);
    } {
      if(Ndef.all[source.asSymbol].notNil) {
        Ndef((source ++ "_routed").asSymbol, {
          Out.ar(targetBus, Ndef(source.asSymbol).ar);
        }).play;
      } {
        "CCFX: source '%' not found".format(source).warn;
        ^false;
      };
    };

    ^true;
  }

  routeTrigger { |source, sidechainName|
    var sc = sidechains[sidechainName.asSymbol];
    if(sc.isNil) {
      "CCFX: sidechain '%' not found".format(sidechainName).warn;
      ^false;
    };

    if(Pdef.all[source.asSymbol].notNil) {
      Pdef(source.asSymbol).set(\out, sc.triggerBus);
    } {
      if(Ndef.all[source.asSymbol].notNil) {
        Ndef((source ++ "_trigger").asSymbol, {
          Out.ar(sc.triggerBus, Ndef(source.asSymbol).ar);
        }).play;
      } {
        "CCFX: trigger source '%' not found".format(source).warn;
        ^false;
      };
    };

    ^true;
  }

  bypass { |slot, shouldBypass=true|
    var info = loaded[slot.asSymbol];
    if(info.notNil) {
      if(shouldBypass) {
        info.ndef.stop;
      } {
        info.ndef.play;
      };
      ^true;
    };
    ^false;
  }

  remove { |slot|
    var info, chainInfo, scInfo;

    info = loaded[slot.asSymbol];
    if(info.notNil) {
      info.ndef.clear;
      info.inBus.free;
      loaded.removeAt(slot.asSymbol);
      ^true;
    };

    // Check if it's a chain
    chainInfo = chains[slot.asSymbol];
    if(chainInfo.notNil) {
      chainInfo.slots.do { |s|
        loaded[s].ndef.clear;
        loaded.removeAt(s);
      };
      chainInfo.buses.do(_.free);
      chains.removeAt(slot.asSymbol);
      ^true;
    };

    // Check if it's a sidechain
    scInfo = sidechains[slot.asSymbol];
    if(scInfo.notNil) {
      Ndef(scInfo.slot).clear;
      scInfo.inBus.free;
      scInfo.triggerBus.free;
      sidechains.removeAt(slot.asSymbol);
      ^true;
    };

    ^false;
  }

  clearAll {
    loaded.keysValuesDo { |slot, info|
      info.ndef.clear;
      info.inBus.free;
    };
    chains.keysValuesDo { |name, info|
      info.buses.do(_.free);
    };
    sidechains.keysValuesDo { |name, info|
      Ndef(info.slot).clear;
      info.inBus.free;
      info.triggerBus.free;
    };
    loaded.clear;
    chains.clear;
    sidechains.clear;
  }

  list {
    ^defs.keys.asArray.sort;
  }

  status {
    ^(
      effects: loaded.keys.asArray,
      chains: chains.keys.asArray,
      sidechains: sidechains.keys.asArray
    );
  }

  describe {
    var lines = defs.keys.asArray.sort.collect { |name|
      var entry = defs[name];
      var params = entry[\params].clump(2).collect { |pair|
        "% (default: %)".format(pair[0], pair[1]);
      }.join(", ");
      "% - %\n  params: %".format(name, entry[\description], params);
    };
    lines.join("\n").postln;
  }
}
