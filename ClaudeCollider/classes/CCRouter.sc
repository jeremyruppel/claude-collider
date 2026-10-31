// CCRouter - Effect connections, chains, and source routing

CCRouter {
  var <fx;           // Reference to parent CCFX
  var <connections;  // Dictionary: from -> (to)
  var <chains;       // Dictionary: name -> [slots]
  var <routes;       // Dictionary: source -> (target, routeSynth)

  *new { |fx|
    ^super.new.init(fx);
  }

  init { |argFX|
    fx = argFX;
    connections = Dictionary[];
    chains = Dictionary[];
    routes = Dictionary[];
  }

  // ========== Effect-to-Effect Connections ==========

  // Send an effect's output to another effect or a sidechain instead of main
  connect { |from, to|
    var fromInfo, toBus;

    fromInfo = fx.loaded[from.asSymbol];
    if(fromInfo.isNil) {
      "CCRouter: source effect '%' not found".format(from).warn;
      ^nil;
    };

    toBus = this.inputBus(to);
    if(toBus.isNil) {
      "CCRouter: destination '%' not found".format(to).warn;
      ^nil;
    };

    if(from.asSymbol == to.asSymbol) {
      "CCRouter: cannot connect effect to itself".warn;
      ^nil;
    };

    if(this.wouldCreateCycle(from.asSymbol, to.asSymbol)) {
      "CCRouter: circular connection detected".warn;
      ^nil;
    };

    fromInfo.ndef.play(out: toBus.index);
    connections[from.asSymbol] = (to: to.asSymbol);

    ^true;
  }

  // Input bus of a loaded effect or a sidechain
  inputBus { |name|
    var info = fx.loaded[name.asSymbol] ?? { fx.sidechains.at(name.asSymbol) };
    ^info !? { info.inBus };
  }

  wouldCreateCycle { |from, to|
    var current = to;
    while { connections[current].notNil } {
      current = connections[current].to;
      if(current == from) { ^true };
    };
    ^false;
  }

  disconnect { |from|
    var connInfo = connections[from.asSymbol];
    if(connInfo.notNil) {
      connections.removeAt(from.asSymbol);
      this.playToMain(from);
      ^true;
    };
    ^false;
  }

  // Main mix bus (goes through the main limiter). Falls back to hardware bus 0
  // when there is no output system, e.g. in unit tests.
  mainOut {
    var outputs = fx.tryPerform(\outputs);
    if(outputs.isNil) { ^0 };
    outputs.playMain;
    ^outputs.mainBus.index;
  }

  // Point an effect's output back at main
  playToMain { |slot|
    fx.loaded[slot.asSymbol] !? { |info| info.ndef.play(out: fx.outputs.mainBus.index) };
  }

  // ========== Chains ==========

  registerChain { |name, slots|
    chains[name.asSymbol] = slots;
  }

  unregisterChain { |name|
    chains.removeAt(name.asSymbol);
  }

  chainSlots { |name|
    ^chains[name.asSymbol];
  }

  // ========== Source-to-Effect Routing ==========

  route { |source, target|
    var targetBus, srcNdef, routeSynth;

    // Find target bus from loaded effects
    if(fx.loaded[target.asSymbol].notNil) {
      targetBus = fx.loaded[target.asSymbol].inBus;
    } {
      // Check sidechains
      if(fx.sidechains.at(target.asSymbol).notNil) {
        targetBus = fx.sidechains.at(target.asSymbol).inBus;
      };
    };

    if(targetBus.isNil) {
      "CCRouter: target '%' not found".format(target).warn;
      ^false;
    };

    // Clean up existing route before creating new one
    if(routes[source.asSymbol].notNil) {
      this.unroute(source);
    };

    // Route Pdef source
    if(Pdef.all[source.asSymbol].notNil) {
      Pdef(source.asSymbol).set(\out, targetBus.index);
      routes[source.asSymbol] = (target: target.asSymbol);
      ^true;
    };

    // Route Ndef source
    if(Ndef.all[fx.cc.server.name].notNil and: { Ndef.all[fx.cc.server.name].at(source.asSymbol).notNil }) {
      srcNdef = Ndef(source.asSymbol);
      if(srcNdef.bus.notNil) {
        routeSynth = Synth(
          if(srcNdef.numChannels == 1) { \cc_bus_copy_mono } { \cc_bus_copy },
          [\in, srcNdef.bus.index, \out, targetBus.index]
        );
        routes[source.asSymbol] = (target: target.asSymbol, routeSynth: routeSynth);
        ^true;
      } {
        "CCRouter: Ndef '%' has no bus".format(source).warn;
        ^false;
      };
    };

    "CCRouter: source '%' not found".format(source).warn;
    ^false;
  }

  unroute { |source|
    var routeInfo = routes[source.asSymbol];
    if(routeInfo.notNil) {
      if(routeInfo.routeSynth.notNil) { routeInfo.routeSynth.free };
      if(Pdef.all[source.asSymbol].notNil) {
        Pdef(source.asSymbol).set(\out, this.mainOut);
      };
      routes.removeAt(source.asSymbol);
      ^true;
    };
    ^false;
  }

  // ========== Cleanup ==========

  removeEffect { |slot|
    // Remove connections from this effect
    connections.removeAt(slot.asSymbol);

    // Effects feeding this one go back to main (snapshot keys to avoid mutation during iteration)
    connections.keys.do { |from|
      if(connections[from].to == slot.asSymbol) {
        connections.removeAt(from);
        this.playToMain(from);
      };
    };

    // Remove routes to this effect (snapshot keys to avoid mutation during iteration)
    routes.keys.do { |source|
      var info = routes[source];
      if(info.target == slot.asSymbol) {
        if(info.routeSynth.notNil) { info.routeSynth.free };
        if(Pdef.all[source].notNil) { Pdef(source).set(\out, this.mainOut) };
        routes.removeAt(source);
      };
    };
  }

  clear {
    connections.clear;

    routes.keysValuesDo { |source, info|
      if(info.routeSynth.notNil) { info.routeSynth.free };
      if(Pdef.all[source].notNil) { Pdef(source).set(\out, this.mainOut) };
    };
    routes.clear;

    chains.clear;
  }

  // ========== Status ==========

  status {
    var lines = [];

    if(connections.size > 0) {
      lines = lines.add("Connections:");
      connections.keysValuesDo { |from, conn|
        lines = lines.add("  % -> %".format(from, conn.to));
      };
    };

    if(chains.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Chains:");
      chains.keysValuesDo { |name, slots|
        lines = lines.add("  %: %".format(name, slots.join(" -> ")));
      };
    };

    if(routes.size > 0) {
      if(lines.size > 0) { lines = lines.add("") };
      lines = lines.add("Routes:");
      routes.keysValuesDo { |source, info|
        lines = lines.add("  % -> %".format(source, info.target));
      };
    };

    if(lines.isEmpty) { ^"  (none)" };
    ^lines.join("\n");
  }

  keysValuesDo { |func|
    routes.keysValuesDo(func);
  }
}
