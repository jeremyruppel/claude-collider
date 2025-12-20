// CCSidechain - Sidechain compressor management

CCSidechain {
  var <cc;
  var <sidechains;  // Dictionary: name -> (inBus, triggerBus, slot, triggerRoutes)

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    sidechains = Dictionary[];
  }

  // ========== Sidechain Creation ==========

  create { |name, threshold=0.1, ratio=4, attack=0.01, release=0.1|
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
      slot: slotName,
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

  // ========== Trigger Routing ==========

  routeTrigger { |source, sidechainName, passthrough=true|
    var sc = sidechains[sidechainName.asSymbol];
    var srcSym = source.asSymbol;
    var routeBus, triggerSynth, srcNdef;

    if(sc.isNil) {
      "CCSidechain: sidechain '%' not found".format(sidechainName).warn;
      ^false;
    };

    this.clearTriggerRoute(sc, srcSym);

    // Route Pdef source
    if(Pdef.all[srcSym].notNil) {
      routeBus = Bus.audio(cc.server, 2);
      Pdef(srcSym).set(\out, routeBus);
      triggerSynth = Synth(\cc_bus_copy, [\in, routeBus.index, \out, sc.triggerBus.index]);

      if(passthrough) {
        Ndef((source ++ "_passthrough").asSymbol, { |in|
          In.ar(in, 2);
        });
        Ndef((source ++ "_passthrough").asSymbol).set(\in, routeBus.index);
        Ndef((source ++ "_passthrough").asSymbol).play(0);
      };

      sc.triggerRoutes[srcSym] = (routeBus: routeBus, triggerSynth: triggerSynth);
      ^true;
    };

    // Route Ndef source
    if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server].at(srcSym).notNil }) {
      srcNdef = Ndef(srcSym);
      if(srcNdef.bus.notNil) {
        triggerSynth = Synth(\cc_bus_copy, [\in, srcNdef.bus.index, \out, sc.triggerBus.index]);
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

  clearTriggerRoute { |sc, srcSym|
    var routeInfo;

    Ndef((srcSym ++ "_passthrough").asSymbol).clear;

    if(sc.triggerRoutes.notNil) {
      routeInfo = sc.triggerRoutes[srcSym];
      if(routeInfo.notNil) {
        if(routeInfo.triggerSynth.notNil) { routeInfo.triggerSynth.free };
        if(routeInfo.routeBus.notNil) { routeInfo.routeBus.free };
        sc.triggerRoutes.removeAt(srcSym);
      };
    };

    if(Pdef.all[srcSym].notNil) {
      Pdef(srcSym).set(\out, 0);
    };
    if(Ndef.all[cc.server].notNil and: { Ndef.all[cc.server].at(srcSym).notNil }) {
      Ndef(srcSym).play;
    };
  }

  cleanupTriggerRoutes { |scInfo, restoreOutput=true|
    if(scInfo.triggerRoutes.notNil) {
      scInfo.triggerRoutes.keysValuesDo { |source, routeInfo|
        Ndef((source ++ "_passthrough").asSymbol).clear;
        if(routeInfo.triggerSynth.notNil) { routeInfo.triggerSynth.free };
        if(routeInfo.routeBus.notNil) { routeInfo.routeBus.free };
        if(restoreOutput && Pdef.all[source].notNil) {
          Pdef(source).set(\out, 0);
        };
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
