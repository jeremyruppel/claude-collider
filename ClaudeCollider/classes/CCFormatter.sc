// CCFormatter - Status formatting for ClaudeCollider

CCFormatter {
  var <cc;

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
  }

  playingPdefs {
    ^Pdef.all.select(_.isPlaying).keys.asArray;
  }

  playingNdefs {
    var result = [];
    var proxySpace = Ndef.all[cc.server];
    if(proxySpace.notNil) {
      proxySpace.keysValuesDo { |key, proxy|
        if(proxy.isPlaying) { result = result.add(key) };
      };
    };
    ^result;
  }

  format {
    var lines = [
      this.formatServer,
      this.formatTempo,
      this.formatOutputs,
      this.formatSamples,
      this.formatPdefs,
      this.formatNdefs
    ];
    ^lines.join("\n");
  }

  print {
    this.format.postln;
  }

  formatServer {
    ^"Server: % | CPU: % | Synths: %".format(
      if(cc.server.serverRunning) { "running" } { "stopped" },
      cc.server.avgCPU.round(0.1).asString ++ "%",
      cc.server.numSynths
    );
  }

  formatTempo {
    ^"Tempo: % BPM | Device: % (% out)".format(
      cc.tempo.round(0.1),
      cc.server.options.device ?? "default",
      cc.server.options.numOutputBusChannels
    );
  }

  formatOutputs {
    var outputs = cc.fx.outputs;
    var mainOutput = outputs[\out_main];
    var otherOutputs;

    if(mainOutput.isNil) {
      ^"Outputs: none";
    };

    otherOutputs = outputs.keys.reject { |k| k == \out_main };
    if(otherOutputs.isEmpty) {
      var hwOut = mainOutput.hwOut ?? 0;
      var playing = mainOutput.ndef.isPlaying;
      ^"Outputs: main %-% (limiter %)".format(
        hwOut + 1, hwOut + 2,
        if(playing) { "on" } { "off" }
      );
    } {
      ^"Outputs: % active".format(outputs.size);
    };
  }

  formatSamples {
    ^"Samples: %/% loaded".format(
      cc.samples.buffers.size,
      cc.samples.paths.size
    );
  }

  formatPdefs {
    var playing = this.playingPdefs;
    ^"Pdefs playing: %".format(
      if(playing.isEmpty) { "none" } { playing.join(", ") }
    );
  }

  formatNdefs {
    var playing = this.playingNdefs;
    ^"Ndefs playing: %".format(
      if(playing.isEmpty) { "none" } { playing.join(", ") }
    );
  }

  formatRoutingDebug {
    var lines = ["=== ROUTING DEBUG ===", ""];
    var fx = cc.fx;
    var warnings = [];
    var chainSlots = this.collectChainSlots;
    var chainInputBuses = this.collectChainInputBuses;

    // Build sections
    lines = this.appendDebugPlayingSources(lines);
    lines = this.appendDebugEffects(lines, chainSlots);
    lines = this.appendDebugChains(lines);
    lines = this.appendDebugConnections(lines);
    lines = this.appendDebugSources(lines, chainInputBuses, warnings);
    lines = this.appendDebugSidechains(lines);
    lines = this.appendDebugOutputs(lines);
    lines = this.appendDebugWarnings(lines, warnings);

    // Add status info at the end
    lines = lines.add("");
    lines = lines.add(this.format);

    ^lines.join("\n");
  }

  collectChainSlots {
    var chainSlots = Set[];
    cc.fx.chains.do { |slots| slots.do { |s| chainSlots.add(s.asSymbol) } };
    ^chainSlots;
  }

  collectChainInputBuses {
    var chainInputBuses = Dictionary[];
    cc.fx.chains.keysValuesDo { |name, slots|
      var firstSlot = slots[0];
      var info = cc.fx.loaded[firstSlot.asSymbol];
      if(info.notNil) {
        chainInputBuses[name] = info.inBus.index;
      };
    };
    ^chainInputBuses;
  }

  appendDebugEffects { |lines, chainSlots|
    var fx = cc.fx;
    var standaloneEffects = fx.loaded.keys.select { |slot| chainSlots.includes(slot).not };

    if(standaloneEffects.size > 0) {
      lines = lines.add("Effects:");
      standaloneEffects.do { |slot|
        var info = fx.loaded[slot];
        var ndef = Ndef(slot);
        var inBus = ndef.bus !? _.index ?? "?";
        var status = if(ndef.isPlaying) { "✓ playing" } { "○ stopped" };
        lines = lines.add("  % (bus %) %".format(slot, inBus, status));
        lines = lines.add("    params: %".format(this.formatEffectParams(info)));
      };
    };
    ^lines;
  }

  appendDebugChains { |lines|
    var fx = cc.fx;

    if(fx.chains.size > 0) {
      if(lines.size > 2) { lines = lines.add("") };
      lines = lines.add("Chains:");
      fx.chains.keysValuesDo { |name, slots|
        lines = lines.add("  %:".format(name));
        lines = this.appendChainSlots(lines, slots);
        lines = lines.add("    → main out");
      };
    };
    ^lines;
  }

  appendChainSlots { |lines, slots|
    var fx = cc.fx;

    slots.do { |slot, i|
      var info = fx.loaded[slot.asSymbol];
      if(info.notNil) {
        var ndef = Ndef(slot.asSymbol);
        var inBus = info.inBus.index;
        var outBus = this.getChainSlotOutBus(slots, i);
        var status = if(ndef.isPlaying) { "✓ playing" } { "○ stopped" };
        lines = lines.add("    → % (in: %, out: %) %".format(slot, inBus, outBus, status));
      } {
        lines = lines.add("    → % (not loaded)".format(slot));
      };
    };
    ^lines;
  }

  getChainSlotOutBus { |slots, index|
    if(index < (slots.size - 1)) {
      var nextInfo = cc.fx.loaded[slots[index + 1].asSymbol];
      ^nextInfo !? { nextInfo.inBus.index } ?? 0;
    };
    ^0;
  }

  appendDebugConnections { |lines|
    var fx = cc.fx;
    var nonChainConnections;

    if(fx.connections.size == 0) { ^lines };

    nonChainConnections = fx.connections.select { |conn, from|
      this.isChainConnection(from, conn.to).not;
    };

    if(nonChainConnections.size > 0) {
      if(lines.size > 2) { lines = lines.add("") };
      lines = lines.add("Connections:");
      nonChainConnections.keysValuesDo { |from, conn|
        lines = lines.add("  % → %".format(from, conn.to));
      };
    };
    ^lines;
  }

  isChainConnection { |from, to|
    cc.fx.chains.do { |slots|
      slots.do { |slot, i|
        if((i < (slots.size - 1)) && (from == slot.asSymbol) && (to == slots[i + 1].asSymbol)) {
          ^true;
        };
      };
    };
    ^false;
  }

  appendDebugSources { |lines, chainInputBuses, warnings|
    var fx = cc.fx;

    if(fx.routes.size == 0) { ^lines };

    if(lines.size > 2) { lines = lines.add("") };
    lines = lines.add("Sources:");

    fx.routes.keysValuesDo { |source, target|
      var result = this.formatSourceRoute(source, target, chainInputBuses);
      lines = lines.add(result[\line]);
      if(result[\warning].notNil) {
        warnings.add(result[\warning]);
      };
    };
    ^lines;
  }

  formatSourceRoute { |source, target, chainInputBuses|
    var fx = cc.fx;
    var pdef = Pdef.all[source];
    var expectedBus = this.getExpectedBus(target, chainInputBuses);
    var actualOut = this.getPdefOutBus(pdef);

    if(actualOut.notNil && expectedBus.notNil) {
      if(actualOut == expectedBus) {
        ^(line: "  % → % (out: %) ✓".format(source, target, actualOut), warning: nil);
      } {
        ^(
          line: "  % → % (out: %) ✗ MISMATCH".format(source, target, actualOut),
          warning: "⚠ %: sending to bus %, but % expects bus %".format(source, actualOut, target, expectedBus)
        );
      };
    };
    ^(line: "  % → %".format(source, target), warning: nil);
  }

  getExpectedBus { |target, chainInputBuses|
    var targetInfo = cc.fx.loaded[target.asSymbol];
    if(targetInfo.notNil) {
      ^targetInfo.inBus.index;
    };
    ^chainInputBuses[target.asSymbol];
  }

  getPdefOutBus { |pdef|
    var out, source;
    if(pdef.isNil) { ^nil };
    // Check pdef.envir first (where Pdef.set stores values)
    if(pdef.envir.notNil) {
      out = pdef.envir[\out];
      if(out.notNil) {
        if(out.isKindOf(Bus)) { ^out.index };
        ^out;
      };
    };
    if(pdef.source.isNil) { ^nil };
    source = pdef.source;
    // Handle Pbind and similar patterns that store pairs
    if(source.respondsTo(\patternpairs)) {
      var pairs = source.patternpairs;
      pairs.pairsDo { |key, val|
        if(key == \out) {
          out = val;
          if(out.isKindOf(Bus)) { ^out.index };
          ^out;
        };
      };
    };
    // Handle Event sources
    if(source.respondsTo(\at)) {
      out = source.at(\out);
      if(out.isKindOf(Bus)) { ^out.index };
      ^out;
    };
    ^nil;
  }

  appendDebugSidechains { |lines|
    var fx = cc.fx;

    if(fx.sidechains.size == 0) { ^lines };

    if(lines.size > 2) { lines = lines.add("") };
    lines = lines.add("Sidechains:");

    fx.sidechains.keysValuesDo { |name, info|
      var ndef = Ndef(info.slot);
      var status = if(ndef.isPlaying) { "✓ playing" } { "○ stopped" };
      lines = lines.add("  % (in: %, trig: %) %".format(name, info.inBus.index, info.triggerBus.index, status));
    };
    ^lines;
  }

  appendDebugPlayingSources { |lines|
    var playingPdefs = this.playingPdefs;
    var playingNdefs = this.playingNdefs.reject { |key| key.asString.beginsWith("out_") };

    if(playingPdefs.size == 0 && playingNdefs.size == 0) { ^lines };

    lines = lines.add("Playing:");
    playingPdefs.do { |name|
      var pdef = Pdef(name);
      var outBus = this.getPdefOutBus(pdef) ?? 0;
      lines = lines.add("  % (Pdef) → bus %".format(name, outBus));
    };
    playingNdefs.do { |name|
      var ndef = Ndef(name);
      var outBus = ndef.bus !? _.index ?? 0;
      lines = lines.add("  % (Ndef) → bus %".format(name, outBus));
    };
    ^lines;
  }

  appendDebugOutputs { |lines|
    var outputs = cc.fx.outputs;
    var outputRoutes = cc.fx.outputRoutes;

    if(lines.size > 2) { lines = lines.add("") };
    lines = lines.add("Hardware Outputs:");

    if(outputs.size == 0) {
      lines = lines.add("  (none configured)");
      ^lines;
    };

    outputs.keysValuesDo { |key, info|
      var channels = info.channels;
      var playing = info.ndef.isPlaying;
      var status = if(playing) { "active" } { "stopped" };
      var channelStr = if(channels.isArray) {
        "%-%" .format(channels[0], channels[1]);
      } {
        channels.asString;
      };
      var inBusStr = info.inBus !? { "bus " ++ info.inBus.index } ?? "bus 0";
      var sources = outputRoutes.select { |o| o == info }.keys.asArray;
      var sourceStr = if(sources.isEmpty) { "" } { " <- " ++ sources.join(", ") };

      lines = lines.add("  % → limiter → hw % (%)%".format(
        inBusStr, channelStr, status, sourceStr
      ));
    };
    ^lines;
  }

  appendDebugWarnings { |lines, warnings|
    if(warnings.size == 0) { ^lines };

    lines = lines.add("");
    lines = lines.add("Warnings:");
    warnings.do { |w| lines = lines.add("  %".format(w)) };
    ^lines;
  }

  formatEffectParams { |info|
    var paramPairs = info.params.clump(2);
    ^paramPairs.collect { |pair|
      var val = info.ndef.get(pair[0]);
      "%=%".format(pair[0], val ?? pair[1])
    }.join(", ")
  }
}
