// CCRouter - Effect connections, chains, and source routing

CCRouter {
  var <fx;           // Reference to parent CCFX
  var <connections;  // Dictionary: from -> (to, routeSynth)
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

  connect { |from, to|
    var fromInfo, toInfo, routeSynth;

    fromInfo = fx.loaded[from.asSymbol];
    if(fromInfo.isNil) {
      "CCRouter: source effect '%' not found".format(from).warn;
      ^nil;
    };

    toInfo = fx.loaded[to.asSymbol];
    if(toInfo.isNil) {
      "CCRouter: destination effect '%' not found".format(to).warn;
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

    routeSynth = Synth(\cc_bus_copy, [\in, fromInfo.ndef.bus.index, \out, toInfo.inBus.index]);

    connections[from.asSymbol] = (
      to: to.asSymbol,
      routeSynth: routeSynth
    );

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

  disconnect { |from|
    var connInfo = connections[from.asSymbol];
    if(connInfo.notNil) {
      connInfo.routeSynth.free;
      connections.removeAt(from.asSymbol);
      ^true;
    };
    ^false;
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

    // Route Pdef source
    if(Pdef.all[source.asSymbol].notNil) {
      Pdef(source.asSymbol).set(\out, targetBus.index);
      routes[source.asSymbol] = (target: target.asSymbol);
      ^true;
    };

    // Route Ndef source
    if(Ndef.all[fx.cc.server].notNil and: { Ndef.all[fx.cc.server].at(source.asSymbol).notNil }) {
      srcNdef = Ndef(source.asSymbol);
      if(srcNdef.bus.notNil) {
        routeSynth = Synth(\cc_bus_copy, [\in, srcNdef.bus.index, \out, targetBus.index]);
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
        Pdef(source.asSymbol).set(\out, 0);
      };
      routes.removeAt(source.asSymbol);
      ^true;
    };
    ^false;
  }

  // ========== Cleanup ==========

  removeEffect { |slot|
    // Remove connections from this effect
    var connInfo = connections[slot.asSymbol];
    if(connInfo.notNil) {
      connInfo.routeSynth.free;
      connections.removeAt(slot.asSymbol);
    };

    // Remove connections to this effect
    connections.keysValuesDo { |from, conn|
      if(conn.to == slot.asSymbol) {
        conn.routeSynth.free;
        connections.removeAt(from);
      };
    };

    // Remove routes to this effect
    routes.keysValuesDo { |source, info|
      if(info.target == slot.asSymbol) {
        if(info.routeSynth.notNil) { info.routeSynth.free };
        if(Pdef.all[source].notNil) { Pdef(source).set(\out, 0) };
        routes.removeAt(source);
      };
    };
  }

  clear {
    connections.keysValuesDo { |from, conn|
      conn.routeSynth.free;
    };
    connections.clear;

    routes.keysValuesDo { |source, info|
      if(info.routeSynth.notNil) { info.routeSynth.free };
      if(Pdef.all[source].notNil) { Pdef(source).set(\out, 0) };
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
