// CCSidechain - Sidechain compressor management

CCSidechain {
  classvar <prefix = "sidechain_";

  var <cc;
  var <sidechains;  // Dictionary: name -> (inBus, triggerBus, slot, ndef, triggerRoutes)

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    sidechains = Dictionary[];
  }

  // ========== Sidechain Creation ==========

  create { |name, threshold=0.1, ratio=4, attack=0.01, release=0.1|
    var inBus, triggerBus, slotName;
    var group = cc.tryPerform(\fx) !? { |fx| fx.group };

    // Clean up existing sidechain with same name before recreating
    if(sidechains[name.asSymbol].notNil) {
      this.remove(name);
    };

    inBus = Bus.audio(cc.server, 2);
    triggerBus = Bus.audio(cc.server, 2);
    slotName = (prefix ++ name).asSymbol;

    // Runs with the effects, after every source that writes to its buses
    if(group.notNil) { Ndef(slotName).parentGroup = group };
    Ndef(slotName, { |in, trigger, threshold=0.1, ratio=4, attack=0.01, release=0.1|
      var sig = In.ar(in, 2);
      var trig = In.ar(trigger, 2);
      Compander.ar(sig, trig, threshold, 1, ratio.reciprocal, attack, release);
    });
    Ndef(slotName).set(\in, inBus.index, \trigger, triggerBus.index);
    Ndef(slotName).set(\threshold, threshold, \ratio, ratio, \attack, attack, \release, release);
    Ndef(slotName).play(out: this.mainOut, group: group);

    sidechains[name.asSymbol] = (
      inBus: inBus,
      triggerBus: triggerBus,
      slot: slotName,
      ndef: Ndef(slotName),
      triggerRoutes: Dictionary[]
    );

    ^(name: name, inputBus: inBus, triggerBus: triggerBus);
  }

  // ========== Accessors ==========

  at { |name|
    ^sidechains[name.asSymbol];
  }

  size {
    ^sidechains.size;
  }

  keys {
    ^sidechains.keys.asArray;
  }

  // Main mix bus (goes through the main limiter). Falls back to hardware bus 0
  // when there is no output system, e.g. in unit tests.
  mainOut {
    var outputs = cc.tryPerform(\fx) !? { |fx| fx.outputs };
    if(outputs.isNil) { ^0 };
    outputs.playMain;
    ^outputs.mainBus.index;
  }

  // Where a Pdef currently sends audio: its routed bus, or main if unrouted
  currentOut { |srcSym|
    var out = Pdef(srcSym).envir !? { |e| e[\out] };
    if(out.isNil) { ^this.mainOut };
    ^out.tryPerform(\index) ?? out;
  }

  // ========== Trigger Routing ==========

  routeTrigger { |source, sidechainName, passthrough=true|
    var sc = sidechains[sidechainName.asSymbol];
    var srcSym = source.asSymbol;
    var routeBus, triggerSynth, passSynth, restoreOut, srcNdef;

    if(sc.isNil) {
      "CCSidechain: sidechain '%' not found".format(sidechainName).warn;
      ^false;
    };

    // Clearing first restores any earlier output, so currentOut sees the real destination
    this.clearTriggerRoute(sc, srcSym);

    // Route Pdef source
    if(Pdef.all[srcSym].notNil) {
      restoreOut = this.currentOut(srcSym);
      routeBus = Bus.audio(cc.server, 2);
      Pdef(srcSym).set(\out, routeBus);
      triggerSynth = Synth(\cc_bus_copy, [\in, routeBus.index, \out, sc.triggerBus.index]);

      // Passthrough keeps the source going where it went before (an effect or main)
      if(passthrough) {
        passSynth = Synth.after(triggerSynth, \cc_bus_copy, [\in, routeBus.index, \out, restoreOut]);
      };

      sc.triggerRoutes[srcSym] = (
        routeBus: routeBus,
        triggerSynth: triggerSynth,
        passSynth: passSynth,
        restoreOut: restoreOut
      );
      ^true;
    };

    // Route Ndef source
    if(Ndef.all[cc.server.name].notNil and: { Ndef.all[cc.server.name].at(srcSym).notNil }) {
      srcNdef = Ndef(srcSym);
      if(srcNdef.bus.notNil) {
        triggerSynth = Synth(
          if(srcNdef.numChannels == 1) { \cc_bus_copy_mono } { \cc_bus_copy },
          [\in, srcNdef.bus.index, \out, sc.triggerBus.index]
        );
        sc.triggerRoutes[srcSym] = (triggerSynth: triggerSynth);
        if(passthrough.not) { srcNdef.stop };
        ^true;
      } {
        "CCSidechain: Ndef '%' has no bus".format(source).warn;
        ^false;
      };
    };

    "CCSidechain: trigger source '%' not found".format(source).warn;
    ^false;
  }

  // Free a trigger route's synths and bus, and point a Pdef source back where it
  // went before the trigger was set up
  freeTriggerRoute { |source, routeInfo, restoreOutput=true|
    if(routeInfo.triggerSynth.notNil) { routeInfo.triggerSynth.free };
    if(routeInfo.passSynth.notNil) { routeInfo.passSynth.free };
    if(routeInfo.routeBus.notNil) { routeInfo.routeBus.free };
    if(restoreOutput && Pdef.all[source].notNil && routeInfo.routeBus.notNil) {
      Pdef(source).set(\out, routeInfo.restoreOut ?? { this.mainOut });
    };
  }

  clearTriggerRoute { |sc, srcSym|
    var routeInfo = sc.triggerRoutes !? { |r| r[srcSym] };

    if(routeInfo.notNil) {
      this.freeTriggerRoute(srcSym, routeInfo);
      sc.triggerRoutes.removeAt(srcSym);
    };

    if(Ndef.all[cc.server.name].notNil and: { Ndef.all[cc.server.name].at(srcSym).notNil }) {
      Ndef(srcSym).play;
    };
  }

  cleanupTriggerRoutes { |scInfo, restoreOutput=true|
    if(scInfo.triggerRoutes.notNil) {
      scInfo.triggerRoutes.keysValuesDo { |source, routeInfo|
        this.freeTriggerRoute(source, routeInfo, restoreOutput);
      };
    };
  }

  // ========== Cleanup ==========

  remove { |name|
    var scInfo = sidechains[name.asSymbol];
    if(scInfo.notNil) {
      this.cleanupTriggerRoutes(scInfo, restoreOutput: true);
      Ndef(scInfo.slot).clear;
      scInfo.inBus.free;
      scInfo.triggerBus.free;
      sidechains.removeAt(name.asSymbol);
      ^true;
    };
    ^false;
  }

  clear {
    sidechains.keysValuesDo { |name, info|
      this.cleanupTriggerRoutes(info, restoreOutput: false);
      Ndef(info.slot).clear;
      info.inBus.free;
      info.triggerBus.free;
    };
    sidechains.clear;
  }

  // ========== Status ==========

  status {
    var lines = [];
    sidechains.keysValuesDo { |name, info|
      var ndef = Ndef(info.slot);
      var status = if(ndef.isPlaying) { "active" } { "stopped" };
      lines = lines.add("  % (in: %, trig: %) %".format(name, info.inBus.index, info.triggerBus.index, status));
    };
    if(lines.isEmpty) { ^"  (none)" };
    ^lines.join("\n");
  }

  keysValuesDo { |func|
    sidechains.keysValuesDo(func);
  }
}
