// CCFX - Effects loading, chaining, and routing for ClaudeCollider

CCFX {
  var <cc;
  var <defs;
  var <loaded;      // slot -> (name, ndef, inBus)
  var <sidechains;  // name -> (inBus, triggerBus)
  var <connections; // from -> to (effect-to-effect connections)
  var <chains;      // chainName -> [slot1, slot2, ...]
  var <routes;        // source -> target
  var <outputs;       // outputKey -> (ndef, inBus, channels, hwOut)
  var <outputRoutes;  // source -> outputKey

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    loaded = Dictionary[];
    sidechains = Dictionary[];
    connections = Dictionary[];
    chains = Dictionary[];
    routes = Dictionary[];
    outputs = Dictionary[];
    outputRoutes = Dictionary[];
    defs = this.defineEffects;
    this.addRoutingSynthDefs;
  }

  addRoutingSynthDefs {
    SynthDef(\cc_bus_copy, { |in, out|
      Out.ar(out, In.ar(in, 2));
    }).add;
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

  connect { |from, to|
    var fromInfo, toInfo, routeSlot, routeSynth;

    // Validate from slot exists
    fromInfo = loaded[from.asSymbol];
    if(fromInfo.isNil) {
      "CCFX: source effect '%' not found".format(from).warn;
      ^nil;
    };

    // Validate to slot exists
    toInfo = loaded[to.asSymbol];
    if(toInfo.isNil) {
      "CCFX: destination effect '%' not found".format(to).warn;
      ^nil;
    };

    // Check for self-connection
    if(from.asSymbol == to.asSymbol) {
      "CCFX: cannot connect effect to itself".format(from).warn;
      ^nil;
    };

    // Check for circular connection
    if(this.wouldCreateCycle(from.asSymbol, to.asSymbol)) {
      "CCFX: circular connection detected".warn;
      ^nil;
    };

    // Create a routing Synth that copies from `from`'s bus to `to`'s input bus
    routeSlot = (from ++ "_to_" ++ to).asSymbol;
    routeSynth = Synth(\cc_bus_copy, [\in, fromInfo.ndef.bus.index, \out, toInfo.inBus.index]);

    // Store the connection
    connections[from.asSymbol] = (
      to: to.asSymbol,
      routeSynth: routeSynth
    );

    "Connected % → %".format(from, to).postln;
    ^true;
  }

  wouldCreateCycle { |from, to|
    var current = to;
    while { connections[current].notNil } {
      current = connections[current].to;
      if(current == from) { ^true };
    };
    ^false;
  }

  registerChain { |name, slots|
    chains[name.asSymbol] = slots;
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
    Ndef(slotName).set(\in, inBus.index, \trigger, triggerBus.index);
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
    var targetInfo, targetBus, srcNdef, routeSynth;

    // Find target bus
    if(loaded[target.asSymbol].notNil) {
      targetBus = loaded[target.asSymbol].inBus;
    } {
      if(sidechains[target.asSymbol].notNil) {
        targetBus = sidechains[target.asSymbol].inBus;
      };
    };

    if(targetBus.isNil) {
      "CCFX: target '%' not found".format(target).warn;
      ^false;
    };

    // Route the source
    if(Pdef.all[source.asSymbol].notNil) {
      Pdef(source.asSymbol).set(\out, targetBus.index);
      routes[source.asSymbol] = (target: target.asSymbol);
    } {
      if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server].at(source.asSymbol).notNil }) {
        srcNdef = Ndef(source.asSymbol);
        if(srcNdef.bus.notNil) {
          routeSynth = Synth(\cc_bus_copy, [\in, srcNdef.bus.index, \out, targetBus.index]);
          routes[source.asSymbol] = (target: target.asSymbol, routeSynth: routeSynth);
        } {
          "CCFX: Ndef '%' has no bus".format(source).warn;
          ^false;
        };
      } {
        "CCFX: source '%' not found".format(source).warn;
        ^false;
      };
    };

    ^true;
  }

  routeTrigger { |source, sidechainName, passthrough=true|
    var sc = sidechains[sidechainName.asSymbol];
    var srcSym = source.asSymbol;
    var routeBus, triggerSynth, srcNdef;

    if(sc.isNil) {
      "CCFX: sidechain '%' not found".format(sidechainName).warn;
      ^false;
    };

    // Clean up any existing trigger routing for this source
    this.clearTriggerRoute(sc, srcSym);

    // Route Pdef source
    if(Pdef.all[srcSym].notNil) {
      // Always use the same signal path for consistent volume:
      // Pdef → routeBus → triggerSynth → triggerBus
      // passthrough just adds _passthrough → main out
      routeBus = Bus.audio(cc.server, 2);
      Pdef(srcSym).set(\out, routeBus);

      // Copy to trigger bus using Synth (not Ndef - avoids monitor issues)
      triggerSynth = Synth(\cc_bus_copy, [\in, routeBus.index, \out, sc.triggerBus.index]);

      // Passthrough to main output (only when requested)
      if(passthrough) {
        Ndef((source ++ "_passthrough").asSymbol, { |in|
          In.ar(in, 2);
        });
        Ndef((source ++ "_passthrough").asSymbol).set(\in, routeBus.index);
        Ndef((source ++ "_passthrough").asSymbol).play(0);
      };

      // Store for cleanup
      sc[\triggerRoutes] = sc[\triggerRoutes] ?? Dictionary[];
      sc[\triggerRoutes][srcSym] = (routeBus: routeBus, triggerSynth: triggerSynth);
      ^true;
    };

    // Route Ndef source
    if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server].at(srcSym).notNil }) {
      srcNdef = Ndef(srcSym);
      if(srcNdef.bus.notNil) {
        // Copy from Ndef's bus to trigger bus
        triggerSynth = Synth(\cc_bus_copy, [\in, srcNdef.bus.index, \out, sc.triggerBus.index]);
        sc[\triggerRoutes] = sc[\triggerRoutes] ?? Dictionary[];
        sc[\triggerRoutes][srcSym] = (triggerSynth: triggerSynth);

        if(passthrough.not) {
          srcNdef.stop;
        };
        ^true;
      } {
        "CCFX: Ndef '%' has no bus (not playing?)".format(source).warn;
        ^false;
      };
    };

    "CCFX: trigger source '%' not found".format(source).warn;
    ^false;
  }

  clearTriggerRoute { |sc, srcSym|
    var routeInfo;

    // Clear passthrough Ndef if exists
    Ndef((srcSym ++ "_passthrough").asSymbol).clear;

    // Free stored synth and bus
    if(sc[\triggerRoutes].notNil) {
      routeInfo = sc[\triggerRoutes][srcSym];
      if(routeInfo.notNil) {
        if(routeInfo.triggerSynth.notNil) { routeInfo.triggerSynth.free };
        if(routeInfo.routeBus.notNil) { routeInfo.routeBus.free };
        sc[\triggerRoutes].removeAt(srcSym);
      };
    };

    // Reset source output to main
    if(Pdef.all[srcSym].notNil) {
      Pdef(srcSym).set(\out, 0);
    };
    if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server].at(srcSym).notNil }) {
      Ndef(srcSym).play;
    };
  }

  cleanupTriggerRoutes { |scInfo, restoreOutput=true|
    if(scInfo[\triggerRoutes].notNil) {
      scInfo[\triggerRoutes].keysValuesDo { |source, routeInfo|
        Ndef((source ++ "_passthrough").asSymbol).clear;
        if(routeInfo.triggerSynth.notNil) { routeInfo.triggerSynth.free };
        if(routeInfo.routeBus.notNil) { routeInfo.routeBus.free };
        if(restoreOutput && Pdef.all[source].notNil) {
          Pdef(source).set(\out, 0);
        };
      };
    };
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

  // ========== OUTPUT MANAGEMENT ==========

  // Create or get an output Ndef for specific hardware channels
  // channels: integer for mono (e.g., 3), array for stereo (e.g., [3,4])
  createOutput { |channels|
    var key, numChannels, hwOut, inBus, ndef;

    // Determine key and parameters
    if(channels.isArray) {
      key = ("out_" ++ channels[0] ++ "_" ++ channels[1]).asSymbol;
      numChannels = 2;
      hwOut = channels[0] - 1;  // 1-indexed to 0-indexed
    } {
      key = ("out_" ++ channels).asSymbol;
      numChannels = 1;
      hwOut = channels - 1;
    };

    // Return existing if already created
    if(outputs[key].notNil) { ^outputs[key] };

    // Create private input bus for this output
    inBus = Bus.audio(cc.server, numChannels);

    // Create Ndef with limiter
    ndef = Ndef(key, {
      var sig = InFeedback.ar(\in.kr(0), numChannels);
      sig = Limiter.ar(sig, 0.95);
      Out.ar(\hwOut.kr(hwOut), sig);
    });
    ndef.set(\in, inBus.index, \hwOut, hwOut);
    ndef.play;

    outputs[key] = (
      ndef: ndef,
      inBus: inBus,
      channels: channels,
      hwOut: hwOut,
      routeSynths: Dictionary[]
    );

    ^outputs[key];
  }

  // Play main output that captures bus 0 (default for all synths)
  playMainOutput {
    Ndef(\out_main, {
      var sig = InFeedback.ar(0, 2);
      sig = Limiter.ar(sig, 0.95);
      ReplaceOut.ar(0, Silent.ar(2));
      Out.ar(\out.kr(0), sig);
    }).play;

    outputs[\out_main] = (
      ndef: Ndef(\out_main),
      inBus: nil,  // Uses bus 0 directly
      channels: [1, 2],
      hwOut: 0,
      routeSynths: Dictionary[]
    );
  }

  // Change main output destination
  setMainOutput { |out|
    if(outputs[\out_main].notNil) {
      outputs[\out_main].hwOut = out;
      Ndef(\out_main).set(\out, out);
    };
  }

  // Stop main output
  stopMainOutput {
    if(outputs[\out_main].notNil) {
      outputs[\out_main].ndef.stop;
    };
  }

  // Check if main output is playing
  isMainOutputPlaying {
    ^(outputs[\out_main].notNil and: { outputs[\out_main].ndef.isPlaying });
  }

  // Route a source (Pdef/Ndef name) to specific output channels
  routeToOutput { |source, channels|
    var output, srcSym, isMainOutput;
    srcSym = source.asSymbol;

    // Check if routing to main output (1-2 or [1, 2])
    isMainOutput = channels.isArray.if(
      { (channels[0] == 1) && (channels[1] == 2) },
      { channels == 1 }
    );

    // For main output, use bus 0 directly (no separate output Ndef needed)
    if(isMainOutput) {
      output = outputs[\out_main];
      // Check if source is a Pdef
      if(Pdef.all[srcSym].notNil) {
        Pdef(srcSym).set(\out, 0);
        outputRoutes[srcSym] = output;
        ^true;
      };
      // Check if source is an Ndef
      if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server][srcSym].notNil }) {
        var srcNdef = Ndef(srcSym);
        srcNdef.set(\out, 0);
        outputRoutes[srcSym] = output;
        ^true;
      };
      ^false;
    };

    // For other outputs, create dedicated output Ndef
    output = this.createOutput(channels);

    // Check if source is a Pdef
    if(Pdef.all[srcSym].notNil) {
      Pdef(srcSym).set(\out, output.inBus.index);
      outputRoutes[srcSym] = output;
      ^true;
    };

    // Check if source is an Ndef
    if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server][srcSym].notNil }) {
      var srcNdef = Ndef(srcSym);
      var routeSynth;
      // Free existing route synth if any
      if(output.routeSynths[srcSym].notNil) {
        output.routeSynths[srcSym].free;
      };
      // Create routing synth
      routeSynth = Synth(\cc_bus_copy, [\in, srcNdef.bus.index, \out, output.inBus.index]);
      output.routeSynths[srcSym] = routeSynth;
      outputRoutes[srcSym] = output;
      ^true;
    };

    ^false;
  }

  // Remove source routing, return to main output
  unrouteFromOutput { |source|
    var srcSym = source.asSymbol;
    var output = outputRoutes[srcSym];

    if(output.notNil) {
      // Clean up routing synth if exists
      if(output.routeSynths[srcSym].notNil) {
        output.routeSynths[srcSym].free;
        output.routeSynths.removeAt(srcSym);
      };
      // Reset Pdef to main output bus
      if(Pdef.all[srcSym].notNil) {
        Pdef(srcSym).set(\out, 0);
      };
      outputRoutes.removeAt(srcSym);
      ^true;
    };
    ^false;
  }

  // Get status of all outputs
  outputStatus {
    var lines = [];
    outputs.keysValuesDo { |key, info|
      var channels = info.channels;
      var status = if(info.ndef.isPlaying) { "active" } { "stopped" };
      var channelStr = if(channels.isArray) {
        "%-%" .format(channels[0], channels[1]);
      } {
        channels.asString;
      };
      lines = lines.add("  % -> hw % (%)".format(key, channelStr, status));
    };
    if(lines.isEmpty) { ^"  (none)" };
    ^lines.join("\n");
  }

  // Remove an output (when no longer needed)
  removeOutput { |channels|
    var key = if(channels.isArray) {
      ("out_" ++ channels[0] ++ "_" ++ channels[1]).asSymbol;
    } {
      ("out_" ++ channels).asSymbol;
    };
    var output = outputs[key];

    if(output.notNil) {
      // Clean up all routing synths
      output.routeSynths.do(_.free);
      // Free resources
      output.ndef.clear;
      if(output.inBus.notNil) { output.inBus.free };
      outputs.removeAt(key);
      ^true;
    };
    ^false;
  }

  // Clear all outputs
  clearOutputs {
    outputs.keysValuesDo { |key, output|
      output.routeSynths.do(_.free);
      output.ndef.clear;
      if(output.inBus.notNil) { output.inBus.free };
    };
    outputs.clear;
    outputRoutes.clear;
  }

  remove { |slot|
    var info, scInfo, connInfo;

    info = loaded[slot.asSymbol];
    if(info.notNil) {
      // Remove any connections from this effect
      connInfo = connections[slot.asSymbol];
      if(connInfo.notNil) {
        connInfo.routeSynth.free;
        connections.removeAt(slot.asSymbol);
      };
      // Remove any connections to this effect
      connections.keysValuesDo { |from, conn|
        if(conn.to == slot.asSymbol) {
          conn.routeSynth.free;
          connections.removeAt(from);
        };
      };
      info.ndef.clear;
      info.inBus.free;
      loaded.removeAt(slot.asSymbol);
      ^true;
    };

    // Check if it's a sidechain
    scInfo = sidechains[slot.asSymbol];
    if(scInfo.notNil) {
      this.cleanupTriggerRoutes(scInfo, restoreOutput: true);
      Ndef(scInfo.slot).clear;
      scInfo.inBus.free;
      scInfo.triggerBus.free;
      sidechains.removeAt(slot.asSymbol);
      ^true;
    };

    ^false;
  }

  clearAll {
    var mainOutput = outputs[\out_main];
    var mainWasPlaying = mainOutput.notNil and: { mainOutput.ndef.isPlaying };
    var mainHwOut = mainOutput !? { mainOutput.hwOut } ?? 0;

    // Clear connections first
    connections.keysValuesDo { |from, conn|
      conn.routeSynth.free;
    };
    connections.clear;
    // Clear effects
    loaded.keysValuesDo { |slot, info|
      info.ndef.clear;
      info.inBus.free;
    };
    loaded.clear;
    // Clear sidechains
    sidechains.keysValuesDo { |name, info|
      this.cleanupTriggerRoutes(info, restoreOutput: false);
      Ndef(info.slot).clear;
      info.inBus.free;
      info.triggerBus.free;
    };
    sidechains.clear;
    // Clear chains and routes
    chains.clear;
    routes.clear;
    // Clear all outputs
    this.clearOutputs;
    // Restart main output if it was playing
    if(mainWasPlaying) {
      this.playMainOutput;
      this.setMainOutput(mainHwOut);
    };
  }

  list {
    var names = defs.keys.asArray.sort;
    if(names.isEmpty) { ^"(none)" };
    ^names.join(", ");
  }

  status {
    var lines = [];
    var standaloneEffects, chainSlots;

    // Collect all slots that are part of chains
    chainSlots = Set[];
    chains.do { |slots| slots.do { |s| chainSlots.add(s.asSymbol) } };

    // Effects section (standalone effects not in chains)
    standaloneEffects = loaded.keys.select { |slot| chainSlots.includes(slot).not };
    if(standaloneEffects.size > 0) {
      lines = lines.add("Effects:");
      standaloneEffects.do { |slot|
        var info = loaded[slot];
        lines = lines.add("  % (%) - bus %".format(slot, info.name, info.inBus.index));
      };
    };

    // Chains section
    if(chains.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Chains:");
      chains.keysValuesDo { |name, slots|
        lines = lines.add("  %:".format(name));
        slots.do { |slot|
          lines = lines.add("    → %".format(slot));
        };
        lines = lines.add("    → main out");
      };
    };

    // Connections section (non-chain connections)
    if(connections.size > 0) {
      // Filter out chain connections
      var nonChainConnections = connections.select { |conn, from|
        var isChainConn = false;
        chains.do { |slots|
          slots.do { |slot, i|
            if((i < (slots.size - 1)) && (from == slot.asSymbol)) {
              if(conn.to == slots[i + 1].asSymbol) {
                isChainConn = true;
              };
            };
          };
        };
        isChainConn.not;
      };
      if(nonChainConnections.size > 0) {
        if(lines.size > 0) { lines = lines.add("") };
        lines = lines.add("Connections:");
        nonChainConnections.keysValuesDo { |from, conn|
          lines = lines.add("  % → %".format(from, conn.to));
        };
      };
    };

    // Routes section
    if(routes.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Routes:");
      routes.keysValuesDo { |source, target|
        lines = lines.add("  % → %".format(source, target));
      };
    };

    // Sidechains section
    if(sidechains.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Sidechains:");
      sidechains.keysValuesDo { |name, info|
        lines = lines.add("  % - input bus %, trigger bus %".format(name, info.inBus.index, info.triggerBus.index));
      };
    };

    if(lines.size == 0) {
      ^"No effects loaded";
    } {
      ^lines.join("\n");
    };
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
