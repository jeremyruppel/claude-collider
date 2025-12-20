// CCOutputs - Manages collection of CCOutput instances

CCOutputs {
  var <cc;
  var <outputs;  // Dictionary: key -> CCOutput
  var <routes;   // Dictionary: source -> CCOutput

  *new { |cc|
    ^super.new.init(cc);
  }

  init { |argCC|
    cc = argCC;
    outputs = Dictionary[];
    routes = Dictionary[];
  }

  // ========== Output Creation ==========

  create { |channels|
    var key, numChannels, hwOut, inBus, ndef, output;

    if(channels.isArray) {
      key = ("out_" ++ channels[0] ++ "_" ++ channels[1]).asSymbol;
      numChannels = 2;
      hwOut = channels[0] - 1;
    } {
      key = ("out_" ++ channels).asSymbol;
      numChannels = 1;
      hwOut = channels - 1;
    };

    if(outputs[key].notNil) { ^outputs[key] };

    inBus = Bus.audio(cc.server, numChannels);

    ndef = Ndef(key, {
      var sig = InFeedback.ar(\in.kr(0), numChannels);
      sig = Limiter.ar(sig, 0.95);
      Out.ar(\hwOut.kr(hwOut), sig);
    });
    ndef.set(\in, inBus.index, \hwOut, hwOut);
    ndef.play;

    output = CCOutput(cc, key, ndef, inBus, channels, hwOut);
    outputs[key] = output;

    ^output;
  }

  createMain {
    var ndef, output;

    if(outputs[\out_main].notNil) { ^outputs[\out_main] };

    ndef = Ndef(\out_main, {
      var sig = InFeedback.ar(0, 2);
      sig = Limiter.ar(sig, 0.95);
      ReplaceOut.ar(0, Silent.ar(2));
      Out.ar(\out.kr(0), sig);
    });
    ndef.play;

    output = CCOutput(cc, \out_main, ndef, nil, [1, 2], 0);
    outputs[\out_main] = output;

    ^output;
  }

  // ========== Accessors ==========

  at { |key|
    ^outputs[key.asSymbol];
  }

  main {
    ^outputs[\out_main];
  }

  isMainPlaying {
    var main = this.main;
    ^(main.notNil and: { main.isPlaying });
  }

  keys {
    ^outputs.keys.asArray;
  }

  size {
    ^outputs.size;
  }

  // ========== Routing ==========

  route { |source, channels|
    var srcSym = source.asSymbol;
    var output, isMain;

    isMain = if(channels.isArray) {
      (channels[0] == 1) && (channels[1] == 2);
    } {
      channels == 1;
    };

    output = if(isMain) {
      this.main ?? { this.createMain };
    } {
      this.create(channels);
    };

    if(routes[srcSym].notNil) {
      routes[srcSym].unroute(srcSym);
    };

    if(output.route(source)) {
      routes[srcSym] = output;
      ^true;
    };

    ^false;
  }

  unroute { |source|
    var srcSym = source.asSymbol;
    var output = routes[srcSym];

    if(output.notNil) {
      output.unroute(srcSym);
      routes.removeAt(srcSym);
      ^true;
    };
    ^false;
  }

  outputFor { |source|
    ^routes[source.asSymbol];
  }

  // ========== Main Output Control ==========

  playMain {
    this.createMain;
  }

  stopMain {
    var main = this.main;
    if(main.notNil) { main.stop };
  }

  setMainHwOut { |out|
    var main = this.main;
    if(main.notNil) { main.setHwOut(out) };
  }

  // ========== Cleanup ==========

  remove { |channels|
    var key = if(channels.isArray) {
      ("out_" ++ channels[0] ++ "_" ++ channels[1]).asSymbol;
    } {
      ("out_" ++ channels).asSymbol;
    };
    var output = outputs[key];

    if(output.notNil) {
      routes.keysValuesDo { |src, out|
        if(out === output) { routes.removeAt(src) };
      };
      output.free;
      outputs.removeAt(key);
      ^true;
    };
    ^false;
  }

  clear {
    outputs.do(_.free);
    outputs.clear;
    routes.clear;
  }

  // ========== Status ==========

  status {
    var lines = [];
    outputs.keysValuesDo { |key, output|
      lines = lines.add(output.statusString);
    };
    if(lines.isEmpty) { ^"  (none)" };
    ^lines.join("\n");
  }

  keysValuesDo { |func|
    outputs.keysValuesDo(func);
  }
}
