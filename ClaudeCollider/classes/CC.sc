// CC - ClaudeCollider Main Facade Class
// The main entry point stored in ~cc by convention

CC {
  classvar <instance;

  var <server;
  var <synths;
  var <fx;
  var <midi;
  var <state;
  var <isBooted;

  *new { |server|
    ^super.new.init(server ?? Server.default);
  }

  *boot { |server, device, onComplete|
    instance = this.new(server);
    instance.boot(device, onComplete);
    ^instance;
  }

  init { |argServer|
    server = argServer;
    synths = CCSynths(this);
    fx = CCFX(this);
    midi = CCMIDI(this);
    state = CCState(this);
    isBooted = false;
  }

  boot { |device, onComplete|
    this.device(device);

    server.waitForBoot {
      this.loadSynthDefs;
      Pdef.defaultQuant = 4;
      isBooted = true;
      "*** ClaudeCollider ready ***".postln;
      onComplete.value(this);
    };
  }

  device { |name|
    if(name.isNil) {
      server.options.device = nil;
      "CC: Using default audio device".postln;
    } {
      server.options.device = name;
      "CC: Using audio device '%'".format(name).postln;
    };
    ^server.options.device;
  }

  reboot { |device, onComplete|
    this.stop;
    server.quit;
    this.boot(device, onComplete);
  }

  loadSynthDefs {
    synths.loadAll;
    fx.loadDefs;
  }

  tempo { |bpm|
    if(bpm.notNil) {
      TempoClock.default.tempo = bpm / 60;
      ^bpm;
    } {
      ^TempoClock.default.tempo * 60;
    };
  }

  stop {
    Pdef.all.do(_.stop);
    Ndef.all.do(_.stop);
  }

  clear {
    this.stop;
    Pdef.all.do(_.clear);
    Ndef.all.do(_.clear);
    fx.clearAll;
    midi.clearAll;
    state.clear;
  }

  status {
    ^(
      booted: isBooted,
      tempo: this.tempo,
      device: server.options.device,
      synths: server.numSynths,
      cpu: server.avgCPU.round(0.1),
      pdefs: Pdef.all.select(_.isPlaying).keys.asArray,
      ndefs: Ndef.all.select(_.isPlaying).keys.asArray
    );
  }
}
