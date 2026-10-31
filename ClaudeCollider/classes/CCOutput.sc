// CCOutput - Single hardware output destination (mono or stereo pair)

CCOutput {
  var <cc;
  var <key;          // Symbol: \out_main, \out_3, \out_3_4
  var <ndef;
  var <inBus;        // Bus - mainBus for main output, dedicated bus for aux
  var <channels;     // Integer or Array: 3 or [3, 4] (1-indexed)
  var <hwOut;        // Integer: hardware output index (0-indexed)
  var <routeSynths;  // Dictionary: source -> Synth
  var <group;        // Group the monitor plays in (nil = default group)

  *new { |cc, key, ndef, inBus, channels, hwOut, group|
    ^super.new.init(cc, key, ndef, inBus, channels, hwOut, group);
  }

  init { |argCC, argKey, argNdef, argInBus, argChannels, argHwOut, argGroup|
    cc = argCC;
    key = argKey;
    ndef = argNdef;
    inBus = argInBus;
    channels = argChannels;
    hwOut = argHwOut;
    group = argGroup;
    routeSynths = Dictionary[];
  }

  // ========== Properties ==========

  isMain {
    ^key == \out_main;
  }

  isPlaying {
    ^ndef.isPlaying == true;
  }

  numChannels {
    ^if(channels.isArray) { 2 } { 1 };
  }

  channelString {
    ^if(channels.isArray) {
      "%-%" .format(channels[0], channels[1]);
    } {
      channels.asString;
    };
  }

  inputBusIndex {
    ^inBus.index;
  }

  // ========== Routing ==========

  route { |source|
    var srcSym = source.asSymbol;

    // Handle Pdef
    if(Pdef.all[srcSym].notNil) {
      Pdef(srcSym).set(\out, this.inputBusIndex);
      ^true;
    };

    // Handle Ndef
    if(Ndef.all[cc.server.name].notNil and: { Ndef.all[cc.server.name][srcSym].notNil }) {
      var srcNdef = Ndef(srcSym);
      this.unrouteNdef(srcSym);
      routeSynths[srcSym] = Synth(
        if(srcNdef.numChannels == 1) { \cc_bus_copy_mono } { \cc_bus_copy },
        [\in, srcNdef.bus.index, \out, inBus.index]);
      ^true;
    };

    ^false;
  }

  unroute { |source|
    var srcSym = source.asSymbol;
    this.unrouteNdef(srcSym);
    if(Pdef.all[srcSym].notNil) {
      Pdef(srcSym).set(\out, 0);
      ^true;
    };
    ^false;
  }

  unrouteNdef { |srcSym|
    if(routeSynths[srcSym].notNil) {
      routeSynths[srcSym].free;
      routeSynths.removeAt(srcSym);
    };
  }

  unrouteAll {
    routeSynths.keysValuesDo { |srcSym, synth|
      synth.free;
      if(Pdef.all[srcSym].notNil) {
        Pdef(srcSym).set(\out, 0);
      };
    };
    routeSynths.clear;
  }

  // ========== Control ==========

  // Pass the group every time: NodeProxy.play otherwise moves the monitor back
  // into the default group
  play {
    ndef.play(group: group);
  }

  stop {
    ndef.stop;
  }

  setHwOut { |out|
    hwOut = out;
    ndef.play(out: out, group: group);
  }

  // ========== Cleanup ==========

  free {
    this.unrouteAll;
    ndef.clear;
    if(inBus.notNil) { inBus.free };
  }

  // ========== Status ==========

  sources {
    ^routeSynths.keys.asArray;
  }

  status {
    ^(
      key: key,
      channels: channels,
      hwOut: hwOut,
      isPlaying: this.isPlaying,
      isMain: this.isMain,
      sources: this.sources
    );
  }

  statusString {
    var status = if(this.isPlaying) { "active" } { "stopped" };
    ^"% -> hw % (%)".format(key, this.channelString, status);
  }
}
